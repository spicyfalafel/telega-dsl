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

(defn send-menu [id text menu]
  (tbot/send-message
   bot/mybot
   id
   text
   {:reply_markup
    {:inline_keyboard
     (menu->tg menu)}}))

(defn call-message-fn [ctx id _data message]
  (let [message-fn-res (message (misc/get-dialog-data ctx id))]
    (when (string? message-fn-res)
      (tbot/send-message bot/mybot id message-fn-res))))

(defn handle-message [ctx step id data]
  (cond
    (string? (:message step))
    #(tbot/send-message bot/mybot id (:message step))

    (fn? (:message step))
    #(call-message-fn ctx id data (:message step))
    :else nil))

(defn handle-menu [id text step]
  #(send-menu id text (:menu step)))

(defn handle-step [ctx step id data]
  (let [fns (cond-> []
              (:menu step)
              (conj (handle-menu id (:message step) step))

              (and (not (:menu step)) (:message step))
              (conj (handle-message ctx step id data)))]
    (mapv (fn [f] (f)) fns)))

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

(defn handle-current-step [ctx steps chat-id telegram-data]
  (let [last-step (misc/get-current-step ctx chat-id)
        _ (handle-save ctx chat-id last-step telegram-data)
        next-step (next-step! ctx steps chat-id telegram-data)
        result (handle-step ctx next-step chat-id telegram-data)]
    {:result result}))
