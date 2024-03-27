(ns poc.example-group
  (:require
   [poc.core :as poc]))

(def bot-commands
  {:start
   {:steps
    [{:id "start-register"
      :message "Добро пожаловать! Давайте начнем регистрацию."
      :menu [{:label "Next"}
             {:label "Я не хочу регистрироваться" :end true}]}

     {:id "ask-group"
      :message "Выберите вашу группу:"
      :menu [{:label "Group1" :save-as [:group] :value "group1"}
             {:label "Group2" :save-as [:group] :value "group2"}
             {:label "Group3" :save-as [:group] :value "group3"}]}

     {:id "register-done"
      :message (fn []
                 #_(str "Привет! Группа "
                      (if-let [group (poc/dbget [:group])]
                        group
                        "не задана")))}]}
   :help {:message "Hello"}})

(defn -main []
  (poc/start-bot bot-commands {:token (System/getenv "BOT_TOKEN")
                               :type :webhook
                               :port 8080}))
