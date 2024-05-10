(ns tg-dialog.state
  (:require
   [tg-dialog.misc :as misc]
   [tg-dialog.db.mongo :as mongo]
   [clojure.set :as set]))

(defn goto-aliases [all-steps goto]
  (cond
    (= (keyword goto) :end)
    (:id (last all-steps))

    :else
    goto))

(defn find-by-id [steps-vec step-id]
  (first (filterv #(= (:id %) (goto-aliases steps-vec step-id))
                  steps-vec)))

(defmulti get-current-command (fn [ctx id] (-> @ctx :dbtype)))

(defmethod get-current-command
  :default
  [ctx id]
  (get (:commands @ctx) (-> ctx deref (get id) :CURRENT_COMMAND)))

(defmethod get-current-command :mongo
  [ctx id]
  (mongo/get-current-command ctx id))

(defmulti set-command! (fn [ctx _id _command-key] (-> @ctx :dbtype)))

(defmethod set-command! :default
  [ctx id command-key]
  (swap! ctx (fn [m] (assoc-in m [id :CURRENT_COMMAND] command-key))))

(defmethod set-command! :mongo
  [ctx id command-key]
  (mongo/set-command! ctx id command-key))

(defmulti get-current-step-id (fn [ctx _id] (:dbtype @ctx)))

(defmethod get-current-step-id
  :default
  [ctx id]
  (-> ctx deref (get id) :CURRENT_STEP))

(defmethod get-current-step-id
  :mongo
  [ctx id]
  (mongo/get-current-step-id ctx id))

(defmulti get-current-step (fn [ctx _id] (-> @ctx :dbtype)))

(defmethod get-current-step
  :default
  [ctx id]
  (let [m (-> ctx deref (get id))
        command-key (:CURRENT_COMMAND m)
        command (get-in @ctx [:commands command-key])
        step-id (:CURRENT_STEP m)]
    (find-by-id command step-id)))

(defmethod get-current-step
  :mongo
  [ctx id]
  (let [user (mongo/get-user ctx id)
        command (keyword (:CURRENT_COMMAND user))
        step-id (:CURRENT_STEP user)
        steps (get (:commands @ctx) command)]
    (find-by-id steps step-id)))

(defmulti change-current-step! (fn [ctx _id _step-id] (-> @ctx :dbtype)))

(defmethod change-current-step!
  :default
  [ctx id step-id]
  (swap! ctx (fn [m] (assoc-in m [id :CURRENT_STEP] step-id))))

(defmethod change-current-step!
  :mongo
  [ctx id step-id]
  (mongo/change-current-step! ctx id step-id)
  (get-current-step ctx step-id))

(defmulti remove-current-step! (fn [ctx _id] (-> @ctx :dbtype)))

(defmethod remove-current-step!
  :default
  [ctx id]
  (swap! ctx (fn [m] (update m id dissoc :CURRENT_STEP))))

(defmethod remove-current-step!
  :mongo
  [ctx id]
  (mongo/remove-current-step! ctx id))

(defmulti get-dialog-data (fn [ctx _id] (-> @ctx :dbtype)))

(defmethod get-dialog-data
  :default
  [ctx id]
  (get-in @ctx [id :DIALOG_DATA]))

(defmethod get-dialog-data
  :mongo
  [ctx id]
  (mongo/get-dialog-data ctx id))

(defmulti add-dialog-data! (fn [ctx _id _k _data] (-> @ctx :dbtype)))

(defmethod add-dialog-data!
  :default
  [ctx id k data]
  (let [data (cond-> data (some? (parse-boolean data)) (parse-boolean))]
    (swap! ctx (fn [m] (assoc-in m (into [id :DIALOG_DATA] k) data)))))

(defmethod add-dialog-data!
  :mongo
  [ctx id k data]
  (mongo/add-dialog-data! ctx id k data))

(defmulti change-dialog-data!
  (fn [ctx _id _data]
    (-> @ctx :dbtype)))

(defmethod change-dialog-data!
  :default
  [ctx id data]
  (swap! ctx (fn [m]
               (-> m
                   (update id dissoc :DIALOG_DATA)
                   (assoc-in [id :DIALOG_DATA] data)))))

(defmethod change-dialog-data!
  :mongo
  [ctx id data]
  (mongo/change-dialog-data! ctx id data))

(defn- get-data-paths-from-menu [menu]
  (if menu
    (reduce (fn  [acc menu-item]
              (if (:save-as menu-item)
                (conj acc (:save-as menu-item))
                acc))
            #{}
            menu)
    #{}))

(defn- get-data-paths-from-step [step]
  (conj (get-data-paths-from-menu (:menu step)) (:save-as step)))

(defn get-all-data-paths [command]
  (set/difference (apply set/union (mapv get-data-paths-from-step command)) #{nil}))

(defn remove-data-paths! [ctx id command-key]
  (let [command (command-key (:commands @ctx))
        steps? (vector? command)
        data-from-command (when steps? (get-all-data-paths command))
        data (when steps?
               (reduce
                (fn [acc path]
                  (misc/dissoc-path acc path))
                (get-dialog-data ctx id)
                data-from-command))]
    (when data (change-dialog-data! ctx id data))))

(defn next-by-id [steps-vec step-id]
  (second (drop-while #(not= (:id %) step-id) steps-vec)))

(defn menu-clicked-goto [step telegram-data]
  (when (:menu step)
    (:-> (first (filter
                 (fn [menu-item]
                   (or (= telegram-data (:value menu-item))
                       (= telegram-data (:label menu-item))))
                 (:menu step))))))

(defn menu-clicked-no-goto [step telegram-data]
  (when (:menu step)
    (first (filter
            (fn [menu-item]
              (or (= telegram-data (:value menu-item))
                  (= telegram-data (:label menu-item))))
            (:menu step)))))

(defn goto-without-input? [current-step]
  (and (not (:menu current-step))
       (not (:save-as current-step))))

(defn prev-step-by-index [steps step]
  (let [idx (misc/index-of step steps)]
    (when (> idx 0)
      (nth steps (dec idx)))))

(defn- find-next-step [all-steps step telegram-data]
  (cond

    (menu-clicked-goto step telegram-data)
    (find-by-id all-steps (menu-clicked-goto step telegram-data))

    (:-> step)
    (find-by-id all-steps (goto-aliases all-steps (:-> step)))

    (or (goto-without-input? step)
        (menu-clicked-no-goto step telegram-data))
    (next-by-id all-steps (:id step))

    telegram-data
    (next-by-id all-steps (:id step))))

(defn- next-step [ctx all-steps chat-id telegram-data]
  (let [last-step (get-current-step ctx chat-id)]
    (if last-step
      (find-next-step all-steps last-step telegram-data)
      (first all-steps))))

(defn next-step! [ctx all-steps chat-id telegram-data]
  (let [last-step (next-step ctx all-steps chat-id telegram-data)]
    (when last-step
      (change-current-step! ctx chat-id (:id last-step))
      (get-current-step ctx chat-id))))

(defn get-db [db]
  (mongo/get-db db))
