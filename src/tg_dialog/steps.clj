(ns tg-dialog.steps
  (:require
   [tg-dialog.state :as state]
   [tg-dialog.misc :as misc]))

(set! *warn-on-reflection* true)

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
  (let [last-step (state/get-current-step ctx chat-id)]
    (if last-step
      (find-next-step all-steps last-step telegram-data)
      (first all-steps))))

(defn next-step! [ctx all-steps chat-id telegram-data]
  (let [last-step (next-step ctx all-steps chat-id telegram-data)]
    (when last-step
      (state/change-current-step! ctx chat-id last-step)
      (state/get-current-step ctx chat-id))))
