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

(defn menu-item->tg [menu-item]
  {:text (:label menu-item) :callback_data
   (str
    (if (nil? (:value menu-item))
      (:label menu-item)
      (:value menu-item)))})

(defn menu->tg [menu]
  [(mapv menu-item->tg menu)])

(defn handle-when [ctx id step]
  (if (:when step)
    (let [f (:when step)]
      (f (misc/get-dialog-data ctx id)))
    true))

(defn handle-send [ctx step id message]
  (when (and step
             (handle-when ctx id step))
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

(defn prev-step-by-index [steps step]
  (let [idx (misc/index-of step steps)]
    (when (> idx 0)
      (nth steps (dec idx)))))

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
  (when (and (or (= telegram-data (:value menu-item))
                 (= telegram-data (:label menu-item))) (:save-as menu-item))
    (misc/add-dialog-data! ctx id
                           (:save-as menu-item)
                           (if (some? (:value menu-item))
                             (:value menu-item)
                             (:label menu-item)))))

(defn text->value [menu text]
  (->> menu
       (filterv #(= (:label %) text))
       first
       :value))

(defn handle-save [ctx id last-step text]
  (when (:menu last-step)
    (doseq [menu-item (:menu last-step)]
      (save-menu-item ctx id menu-item text)))

  (cond
    (and (:menu last-step) (:save-as last-step))
    (misc/add-dialog-data! ctx id (:save-as last-step)
                           (or (text->value (:menu last-step) text) text))
    (:save-as last-step)
    (misc/add-dialog-data! ctx id (:save-as last-step) text)))

(defn continue-after-step? [{:keys [message save-as menu]} when-skipped?]
  (or when-skipped? (and message (not save-as) (not menu))))

(defn handle-current-step [ctx steps chat-id message]
  (let [last-step (misc/get-current-step ctx chat-id)
        _ (handle-save ctx chat-id last-step (:text message))
        next-step (next-step! ctx steps chat-id (:text message))
        result (handle-send ctx next-step chat-id message)
        when-skipped? (and (:when next-step) (not result))]
    (if (continue-after-step? next-step when-skipped?)
      (vec (remove nil? (concat [result] (handle-current-step ctx steps chat-id message))))
      [result])))

