(ns hello.core
  (:require
    [malli.core :as m]
    [clojure.string :as str]
    [malli.generator :as mg]))

(defn send-message [user-id text]
  [user-id text])

(def hello-step
  {:name :hello-step
   :step-type :message
   :message "Welcome to register command!"})

(def group-step
  {:name :group-step
   :step-type :question
   :question "Which group are you in?"
   :answer [:enum {:error/message "should be valid group"}
            "P34110" "P34111" "P34112"]})

(defn fullname? [name-str]
  (contains?
      #{2 3}
      (count (str/split name-str #" "))))

(def name-step
  {:name :name-step
   :step-type :question
   :question "What is your full name?"
   :back :group-step
   :answer [:and string? [:fn fullname?]]})

(def commands
  [{:type :command
    :name :register
    :steps [hello-step group-step name-step]}])

(defmulti handle-step (fn [_ctx step] (:step-type step)))

(defmethod handle-step :message
  [{:keys [id]} step]
  #_"todo validation"
  (send-message id (:message step)))

(defn save-wait-step
  "Saves step in internal db."
  [_step]
  #_"TODO: implement"
  nil)

(defmethod handle-step :question
  [{:keys [id]} step]
  #_"todo validation"
  (send-message id (:question step))
  (save-wait-step step))





