(ns tg-dialog.example.example-group)

(def bot-commands
  {:start
   {:steps
    [{:id "start-register"
      :message "Добро пожаловать! Давайте начнем регистрацию."
      :menu [{:label "Next"}
             {:label "Я не хочу регистрироваться" :-> :end}]}

     {:id "ask-group"
      :message "Выберите вашу группу:"
      :menu [{:label "Group1" :save-as [:group] :value "group1"}
             {:label "Group2" :save-as [:group]}
             {:label "Group3" :save-as [:group]}]}

     {:id "register-done"
      :message (fn [ctx]
                 (str "Привет! Группа "
                      (if-let [group (:group ctx)]
                        group
                        "не задана")))}]}
   :help {:message "Hello"}})

;; (defn -main []
;;   (poc/start-bot bot-commands {:token (System/getenv "BOT_TOKEN")
;;                                :type :webhook
;;                                :port 8080}))
