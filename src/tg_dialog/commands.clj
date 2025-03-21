(ns tg-dialog.commands
  (:require
    [clojure.pprint :as pprint]
    [telegrambot-lib.core :as tbot]
    [tg-dialog.validation :as validation]
    [tg-dialog.state :as state]))

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

(defn add-menu-values [step]
  (cond-> step

    (:menu step)
    (update
     :menu
     (fn [menu]
       (mapv
        (fn [menu-item]
          (if (some? (:value menu-item))
            (update menu-item :value str)
            (assoc menu-item :value (:label menu-item)))) menu)))))

(defn add-back-button [steps step]
  (let [back (:back step)]
    (if back
      (update step :menu
              conj
              {:label "Back"
               :-> (cond
                     (true? back)
                     (:id (state/prev-step-by-index steps step))

                     (string? back)
                     back)})
      step)))

(defn prepare-steps [steps]
  (if (vector? steps)
    (mapv (comp add-menu-values (partial add-back-button steps)) steps)
    steps))

(defn prepare-commands [bot commands]
  (when (seq (validation/validate-commands commands))
    (throw (Exception.
             (str
               "Errors in commands:\n"
               (with-out-str
                 (pprint/pprint (validation/validate-commands commands)))))))

  (validation/check-malli-schemas commands)

  (let [commands (->> commands
                      (mapv (fn [[command-name command]]
                              [command-name (-> command
                                                (add-ids)
                                                (add-menu-values)
                                                (prepare-steps))]))
                      (into {}))]

    (tbot/set-chat-menu-button bot)
    (tbot/set-my-commands bot (mapv (fn [[command _]]
                                      {:command command :description command})
                                    commands))
    commands))

