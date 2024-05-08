(ns tg-dialog.state
  (:require
   [tg-dialog.misc :as misc]
   [clojure.set :as set]))

(defn get-current-command [ctx id]
  (get (:commands @ctx) (-> ctx deref (get id) :CURRENT_COMMAND)))

(defn set-command! [ctx id command-key]
  (swap! ctx (fn [m] (assoc-in m [id :CURRENT_COMMAND] command-key))))

(defn get-current-step [ctx id]
  (-> ctx deref (get id) :CURRENT_STEP))

(defn change-current-step! [ctx id step]
  (swap! ctx (fn [m] (assoc-in m [id :CURRENT_STEP] step))))

(defn remove-current-step! [ctx id]
  (swap! ctx (fn [m] (update m id dissoc :CURRENT_STEP))))

(defn get-dialog-data [ctx id]
  (get-in @ctx [id :DIALOG_DATA]))

(defn add-dialog-data! [ctx id k data]
  (let [data (cond-> data (some? (parse-boolean data)) (parse-boolean))]
    (swap! ctx (fn [m] (assoc-in m (into [id :DIALOG_DATA] k) data)))))

(defn- change-dialog-data! [ctx id data]
  (swap! ctx (fn [m]
               (-> m
                   (update id dissoc :DIALOG_DATA)
                   (assoc-in [id :DIALOG_DATA] data)))))

(defn get-data-paths-from-menu [menu]
  (if menu
    (reduce (fn  [acc menu-item]
              (if (:save-as menu-item)
                (conj acc (:save-as menu-item))
                acc))
            #{}
            menu)
    #{}))

(defn get-data-paths-from-step [step]
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
