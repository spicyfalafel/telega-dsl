(ns tg-dialog.misc)

(defn get-current-step [ctx id]
  (-> ctx deref (get id) :CURRENT_STEP))

(defn get-dialog-data [ctx id]
  (get-in @ctx [id :DIALOG_DATA]))
