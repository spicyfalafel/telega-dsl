(ns poc.validation
  (:require
   [malli.core :as m]
   [com.mjdowney.rich-comment-tests]))


(def label-schema
  [:map {:closed true}
   [:label :string]
   [:save-as
    {:optional true}
    [:vector :keyword]]
   [:value
    {:optional true}
    :any]
   [:end
    {:optional true}
    [:enum true]]])

(defn validate-label [label]
  (m/validate (m/schema label-schema) label))

^:rct/test
(comment
  (validate-label {:label "Next"}) ;=> true

  (validate-label
    {:label "Я не хочу регистрироваться" :end true}) ;=> true

  (validate-label
    {:label "Group1" :save-as [:group] :value "group1"}) ;=> true

  (validate-label {:label 1 :value 2}) ;=> false

  (validate-label {:name :start
                   :steps [{}]
                   :message "hello"}) ;=> false
  )

(def step-schema
  [:map
   [:id {:optional true} :string]
   [:message {:optional true} [:or :string fn?]]
   [:menu {:optional true} [:vector #'label-schema]]]
  )

(defn validate-step [command]
  (m/validate (m/schema step-schema)
              command))


^:rct/test
(comment
  (validate-step
    {:id "ask-group"
     :message "Выберите вашу группу:"
     :menu [{:label "Group1" :save-as [:group] :value "group1"}
            {:label "Group2" :save-as [:group] :value "group2"}
            {:label "Group3" :save-as [:group] :value "group3"}]}) ;=> true

  (validate-step
    {:id "start-register"
     :message "Добро пожаловать! Давайте начнем регистрацию."
     :menu [{:label "Next"}
            {:label "Я не хочу регистрироваться" :end true}]}) ;=> true

  (validate-step {:id "register-done"
                  :message (fn []
                             (str "Привет! "))}) ;=> true

  (validate-step {:id "register-done"
                  :message 1}) ;=> false

  )


(defn validate-command [command]
  (m/validate (m/schema [:and
                          [:map {:closed true}
                           [:name :keyword]
                           [:steps {:optional true} [:vector #'step-schema]]
                           [:message {:optional true} :string]]
                          [:fn {:error/message "steps & message are mutually exclusive"
                                :error/path [:message]}
                           (fn [{:keys [message steps]}]
                             (not (and message steps)))]]) command))

^:rct/test
(comment
  (validate-command {:name :start
                     :message "hello"}) ;=> true

  (validate-command {:name :start
                     :steps [{}]}) ;=> true

  (validate-command {:steps [{}]}) ;=> false

  (validate-command {:name :start
                     :steps ["hello"]}) ;=> false

  (validate-command {:name :start
                     :steps [{}]
                     :message "hello"}) ;=> false
  )



(com.mjdowney.rich-comment-tests/run-ns-tests! *ns*)
