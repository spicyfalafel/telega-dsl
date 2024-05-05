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

(defn menu-item->tg [{:keys [label value] :as menu-item}]
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

(defn call-message-fn [ctx id data message]
  (let [message-fn-res (message (misc/get-dialog-data ctx id))]
    (when (string? message-fn-res)
      (tbot/send-message bot/mybot id message-fn-res))))

(defn handle-message [ctx step id data]
  (cond
    (string? (:message step))
    [:string #(tbot/send-message bot/mybot id (:message step))]

    (fn? (:message step))
    [:fn #(call-message-fn ctx id data (:message step))]
    :else nil))

(defn handle-menu [id text step]
  [:menu #(send-menu id text (:menu step))])

(defn handle-step [ctx step id data]
  (let [fns (cond-> []
              (:menu step)
              (conj (handle-menu id (:message step) step))

              (and (not (:menu step)) (:message step))
              (conj (handle-message ctx step id data)))]
    (mapv (fn [f] ((second f))) fns)))

(defn current-step [ctx steps chat-id]
  (let [last-step (misc/get-current-step ctx chat-id)]
    (if last-step
      last-step
      (first steps))))

(defn next-step [ctx steps chat-id]
  (let [last-step (misc/get-current-step ctx chat-id)]
    (if last-step
      (second (drop-while #(not= % last-step) steps))
      (first steps))))

(defn change-current-step! [ctx id step]
  (swap! ctx (fn [m] (assoc-in m [id :CURRENT_STEP] step))))

(defn goto-aliases [all-steps goto]
  (cond
    (= (keyword goto) :end)
    (:id (last all-steps))

    :else
    goto))

(defn find-by-id [steps-vec step-id]
  (println "findbyid " step-id)
  (println "fbi steps " steps-vec)
  (println "a" (goto-aliases steps-vec step-id))
  (println "b"
           (first (filterv #(= (:id %) (goto-aliases steps-vec step-id))
                  steps-vec))
           )
  (first (filterv #(= (:id %) (goto-aliases steps-vec step-id))
                  steps-vec)))

(defn next-by-id [steps-vec step-id]
  (second (drop-while #(not= (:id %) step-id) steps-vec)))

(defn menu-clicked-goto [step telegram-data]
  (when (:menu step)
    (:-> (first (filter
             (fn [menu-item]
               (= telegram-data (:label menu-item)))
             (:menu step))))))


(defn goto-without-input? [current-step]
  (and (not (:menu current-step))
       (not (:save-as current-step))))

(defn find-next-step [all-steps step telegram-data]
  (println "CURRENT STEP " step)
  (println "ALL STEPS" all-steps)
  (cond

    (menu-clicked-goto step telegram-data)
    (do
      (println
        "!!!!!!!!!!!!!"
        (find-by-id all-steps (menu-clicked-goto step telegram-data)))
      (find-by-id all-steps (menu-clicked-goto step telegram-data)))

    (:-> step)
    (find-by-id all-steps (goto-aliases all-steps (:-> step)))

    (goto-without-input? step)
    (next-by-id all-steps (:id step))))

(defn next-step! [ctx all-steps chat-id current-step telegram-data]
  (let [step (find-next-step all-steps current-step telegram-data)]
    (when step (change-current-step! ctx chat-id step))))

(defn handle-current-step [ctx steps chat-id telegram-data]
  (let [current-step (current-step
                       ctx
                       steps chat-id)
        result (handle-step ctx current-step chat-id telegram-data)]
    (println "telegram data " telegram-data)

    (println "handle-c-s step id1 " (:id current-step))
    {:result result
     :next (next-step! ctx steps chat-id current-step telegram-data)}
    ))
