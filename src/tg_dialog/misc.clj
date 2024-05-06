(ns tg-dialog.misc)

(defn get-current-step [ctx id]
  (-> ctx deref (get id) :CURRENT_STEP))

(defn change-current-step! [ctx id step]
  (swap! ctx (fn [m] (assoc-in m [id :CURRENT_STEP] step))))

(defn remove-current-step! [ctx id]
  (swap! ctx (fn [m] (dissoc m id))))

(defn get-dialog-data [ctx id]
  (get-in @ctx [id :DIALOG_DATA]))
