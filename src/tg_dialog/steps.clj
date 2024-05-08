(ns tg-dialog.steps
  (:require
   [telegrambot-lib.core :as tbot]
   [tg-dialog.misc :as misc]
   [tg-dialog.bot :as bot]))

(set! *warn-on-reflection* true)

(def no-id-step-prefix "no-id-step")

(defn add-ids [steps]
  (if (vector? steps)
    (vec (map-indexed
          (fn [idx s]
            (if (:id s)
              s
              (assoc s :id (str no-id-step-prefix idx))))
          steps))
    (when (map? steps)
      (if (:id steps)
        steps
        (assoc steps :id "0")))))

(defn menu-item->tg [{:keys [label value]}]
  {:text label :callback_data (or value label)})

(defn menu->tg [menu]
  [(mapv menu-item->tg menu)])

(defn handle-send [ctx step id message]
  (when step
    (let [params (cond-> {}
                   (:menu step)
                   (merge {:reply_markup
                           {:inline_keyboard
                            (menu->tg (:menu step))}})

                   (true? (:reply step))
                   (merge {:reply_parameters
                           {:message_id (:message_id message)}}))
          text (cond
                 (fn? (:message step))
                 ((:message step) (misc/get-dialog-data ctx id))

                 (string? (:message step))
                 (:message step))]
      (tbot/send-message bot/mybot id text params))))

(defn goto-aliases [all-steps goto]
  (cond
    (= (keyword goto) :end)
    (:id (last all-steps))

    :else
    goto))

(defn find-by-id [steps-vec step-id]
  (first (filterv #(= (:id %) (goto-aliases steps-vec step-id))
                  steps-vec)))

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

(defn find-next-step [all-steps step telegram-data]
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

(defn next-step [ctx all-steps chat-id telegram-data]
  (let [last-step (misc/get-current-step ctx chat-id)]
    (if last-step
      (find-next-step all-steps last-step telegram-data)
      (first all-steps))))

(defn next-step! [ctx all-steps chat-id telegram-data]
  (let [last-step (next-step ctx all-steps chat-id telegram-data)]
    (when last-step
      (misc/change-current-step! ctx chat-id last-step)
      (misc/get-current-step ctx chat-id))))

(defn save-menu-item [ctx id menu-item telegram-data]
  (when (and (= telegram-data (:value menu-item)) (:save-as menu-item))
    (misc/add-dialog-data! ctx id
                           (:save-as menu-item)
                           (or (:value menu-item)
                               (:label menu-item)))))

(defn handle-save [ctx id last-step telegram-data]
  (when (:menu last-step)
    (doseq [menu-item (:menu last-step)]
      (save-menu-item ctx id menu-item telegram-data)))

  (when (:save-as last-step)
    (misc/add-dialog-data! ctx id (:save-as last-step) telegram-data)))

(defn continue-after-step? [{:keys [message save-as menu]}]
  (and message (not save-as) (not menu)))

(defn handle-current-step [ctx steps chat-id message]
  (let [last-step (misc/get-current-step ctx chat-id)
        _ (handle-save ctx chat-id last-step (:text message))
        next-step (next-step! ctx steps chat-id (:text message))
        result (handle-send ctx next-step chat-id message)]
    (when (continue-after-step? next-step)
      (handle-current-step ctx steps chat-id message))
    result))
