(ns tg-dialog.commands
  (:require
   [tg-dialog.steps :as steps]))

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
  (let [back  (:back step)]
    (if back
      (update step :menu
              conj
              {:label "Back"
               :-> (cond
                     (true? back)
                     (:id (steps/prev-step-by-index steps step))

                     (string? back)
                     back)})
      step)))

(defn prepare-steps [steps]
  (if (vector? steps)
    (mapv (comp add-menu-values (partial add-back-button steps)) steps)
    steps))

(defn prepare-commands [commands]
  (->> commands
       (mapv (fn [[command-name command]]
               [command-name (-> command
                                 (add-ids)
                                 (add-menu-values)
                                 (prepare-steps))]))
       (into {})))

