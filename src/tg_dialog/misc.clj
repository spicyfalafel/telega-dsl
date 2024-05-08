(ns tg-dialog.misc
  (:require
   [clojure.set :as set]))

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

(defn change-dialog-data! [ctx id data]
  (swap! ctx (fn [m] (assoc-in m [id :DIALOG_DATA] data))))

(defn get-data-paths-from-menu [menu]
  (when menu
    (reduce (fn  [acc menu-item]
              (if (:save-as menu-item)
                (conj acc (:save-as menu-item))
                acc))
            #{}
            menu)))

(defn get-data-paths-from-step [step]
  (conj (get-data-paths-from-menu (:menu step)) (:save-as step)))

(defn get-all-data-paths [command]
  (set/difference (apply set/union (mapv get-data-paths-from-step command)) #{nil}))

(defn dissoc-path [m path]
  (assert (vector? path))
  (if (= 1 (count path))
    (dissoc m (first path))
    (update-in m (pop path) dissoc (peek path))))

(defn remove-data-paths! [ctx id command-key]
  (let [command (command-key (:commands ctx))
        data-from-command (get-all-data-paths command)]
    (change-dialog-data!
      ctx id
      (reduce
        (fn [acc path]
          (dissoc-path acc path))
        (get-dialog-data ctx id)
        data-from-command))))

(defn index-of [x coll]
  (let [idx? (fn [i a] (when (= x a) i))]
  (first (keep-indexed idx? coll))))
