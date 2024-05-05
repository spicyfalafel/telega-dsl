(ns tg-dialog.steps
  (:require
   [telegrambot-lib.core :as tbot]
   [malli.core :as m]
   [clojure.string :as str]
   [jsonista.core :as json]
   [tg-dialog.validation :as validation]
   [tg-dialog.example-group :as example]
   [tg-dialog.bot :as bot]
   [org.httpkit.client :as client]
   [org.httpkit.server :as hk-server]
   [malli.generator :as mg]))

(set! *warn-on-reflection* true)

(def no-id-step-prefix "no-id-step")

(defn add-ids [steps]
  (let [steps-vec (vec (map-indexed
                        (fn [idx s]
                          (if (:id s)
                            s
                            (assoc s :id (str no-id-step-prefix idx))))
                        steps))]
    (reduce
     (fn [acc step]
       (assoc acc (keyword (str (:id step))) step))
     {}
     steps-vec)))

^:rct/test
(comment
  (add-ids [{:id 1 :hello 2} {:id 2 :hello 3} {:hello 4}])
;=> {:1 {:id 1, :hello 2},
;    :2 {:id 2, :hello 3},
;    :no-id-step2 {:hello 4, :id "no-id-step2"}}
  (add-ids []) ;=> {}
  )

(defn send-menu [id data menu]
  (tbot/send-message bot/mybot id "todo send menu"))

(defn call-message-fn [id data message]
  (tbot/send-message bot/mybot id "todo send message fn"))

(defn handle-step [step id data]
  (let [fns (cond-> []
              (string? (:message step))
              (conj #(tbot/send-message bot/mybot id (:message step)))

              (:message step)
              (conj #(call-message-fn id data (:message step)))

              (:menu step)
              (conj #(send-menu id data (:menu step))))]
    (doseq [f fns]
      (f))))

(def CURRENT_STEP (atom {}))

(defn current-step [current-step-atom steps id]
  (let [all-steps (keys steps)
        last-step (get @current-step-atom id)]
    (if last-step
      (get steps (second (drop-while #(not= % last-step) all-steps)))
      (get steps (first all-steps)))))

(defn change-current-step [current-step-atom step id]
  (swap! current-step-atom (fn [m] (assoc m id (:id step)))))

^:rct/test
(comment
  ;; (def stp1 (atom {}))
  ;; (def stps {:step1 {:hui 1 :id :step1}
  ;;            :step2 {:hui 2 :id :step2}})
  ;; (current-step stp1 stps 123) ;=> {:hui 1, :id :step1}
  ;; (change-current-step stp1 {:hui 1, :id :step1} 123)
  ;; (= @stp {123 :step1}) ;=> true
  ;; (current-step stp stps 123) ;=> {:hui 2, :id :step2}
  ;; (change-current-step stp {:hui 2, :id :step2} 123)
  ;; (= @stp {123 :step2}) ;=> true
  )

(defn handle-current-step [steps id data]
  (let [steps (add-ids steps)
        current-step (current-step @CURRENT_STEP steps id)]
    (println "current-step " current-step)
    (handle-step current-step id data)
    (println "handled current step")
    (change-current-step CURRENT_STEP current-step id)))

(com.mjdowney.rich-comment-tests/run-ns-tests! *ns*)
