(ns tg-dialog.handlers
  (:require
   [tg-dialog.validation :as validation]
   [tg-dialog.tg :as tg]
   [tg-dialog.state :as state]))

(defn save-menu-item [ctx id menu-item telegram-data]
  (when (and (or (= telegram-data (:value menu-item))
                 (= telegram-data (:label menu-item))) (:save-as menu-item))
    (state/add-dialog-data! ctx id
                            (:save-as menu-item)
                            (if (some? (:value menu-item))
                              (:value menu-item)
                              (:label menu-item)))))

(defn text->value [menu text]
  (->> menu
       (filterv #(= (:label %) text))
       first
       :value))

(defn send-error [ctx id step error]
  (let [message (or error (:error step))]
    (when (string? message)
      (tg/send-message ctx id message))))

(defn handle-save [ctx id last-step text]
  (when (:menu last-step)
    (doseq [menu-item (:menu last-step)]
      (save-menu-item ctx id menu-item text)))

  (cond
    (and (:menu last-step) (:save-as last-step))
    (state/add-dialog-data! ctx id (:save-as last-step)
                            (or (text->value (:menu last-step) text) text))
    (:save-as last-step)
    (state/add-dialog-data! ctx id (:save-as last-step) text)))

(defn handle-input [ctx id last-step text]
  (when text
    (let [error (validation/user-validate-step last-step text)]
      (cond
        (or (nil? error) (true? error) (= :ok error))
        (do (handle-save ctx id last-step text)
            nil)

        :else
        (do
          (send-error ctx id last-step error)
          :error)))))

(defn continue-after-step? [{:keys [message save-as menu]} when-skipped?]
  (or when-skipped? (and message (not save-as) (not menu))))

(defn handle-when [ctx id step]
  (if (:when step)
    (let [f (:when step)]
      (f (state/get-dialog-data ctx id)))
    true))

(defn menu-item->tg [menu-item]
  [{:text (:label menu-item) :callback_data
    (str
     (if (nil? (:value menu-item))
       (:label menu-item)
       (:value menu-item)))}])

(defn menu->tg [menu]
  (mapv menu-item->tg menu))

(defn handle-send [ctx step id message]
  (when (and step (handle-when ctx id step))
    (let [params (cond-> {}
                   (:menu step)
                   (merge {:reply_markup
                           {:inline_keyboard
                            (menu->tg (:menu step))}})

                   (and (true? (:reply step)) (:message_id message))
                   (merge {:reply_parameters
                           {:message_id (:message_id message)}}))
          text (cond
                 (fn? (:message step))
                 ((:message step) (state/get-dialog-data ctx id))

                 (string? (:message step))
                 (:message step))]
      (tg/send-message ctx id text params))))

(defn handle-current-step [ctx steps chat-id message]
  (let [current-step (state/get-current-step ctx chat-id)
        validation-fail? (= :error (handle-input ctx chat-id current-step (:text message)))]
    (when-not validation-fail?
      (let [next-step (state/next-step! ctx steps chat-id (:text message))
            result (handle-send ctx next-step chat-id message)
            when-skipped? (and (:when next-step) (not result))]
        (if (continue-after-step? next-step when-skipped?)
          (vec (remove nil? (concat [result] (handle-current-step ctx steps chat-id message))))
          [result])))))

(defn handle-command
  [ctx command-key chat-id data]
  (state/remove-current-step! ctx chat-id)
  (state/remove-data-paths! ctx chat-id command-key)
  (state/set-command! ctx chat-id command-key)

  (let [command (command-key (:commands @ctx))
        message (:message command)
        steps (vector? command)]
    (cond
      message
      (tg/send-message ctx chat-id message)

      steps
      (handle-current-step ctx command chat-id data))))
