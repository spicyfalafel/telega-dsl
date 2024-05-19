(ns tg-dialog.example.register-example
  (:require [clojure.string :as str]
            [tg-dialog.core :as tg-dialog]))

(def name-schema
  [:and
   [:re {:error/message "Имя и фамилия должны содержать пробел."} #".+\s+.+"]
   [:fn
    {:error/fn (fn [_ _] "Имя и фамилия должны начинаться с заглавной буквы")}
    (fn [name-s]
      (let [[name surname] (str/split name-s #"\s+")]
        (and (Character/isUpperCase ^Character (first name))
             (Character/isUpperCase ^Character (first surname)))))]])

(def bot-commands
  {:start
   [{:message "Добро пожаловать! Давайте начнем регистрацию."}

    {:id "name-step"
     :message "Введите ваше имя и фамилию:"
     :save-as [:name]
     :validate name-schema}

    {:message "Кто вы?"
     :menu [{:label "Студент"     :value "student"}
            {:label "Уже работаю" :value "work" :-> "work-step"}]
     :save-as [:type]
     :validate {:menu-values true}}

    {:message "Введите наименование вашего учебного заведения"
     :save-as [:university-name]}

    {:message "Введите наименование вашей кафедры"
     :save-as [:department]}

    {:message "Введите вашу группу"
     :save-as [:group]
     :-> "confirm-step"}

    {:id "work-step"
     :message "Введите ваше место работы"
     :save-as [:work-place]}

    {:message "Введите вашу должность"
     :save-as [:job-title]}

    {:id "confirm-step"
     :message (fn [{:keys [name type
                           work-place job-title
                           university-name department group]}]
                (str (format "Имя: %s\n" name)
                     (if (= "student" type)
                       (format "Учебное заведение: %s\nКафедра: %s\nГруппа: %s\n"
                               university-name department group)
                       (format "Место работы: %s\nДолжность: %s\n"
                               work-place job-title))
                     "Все верно?"))

     :menu [{:label "Готово"}
            {:label "Нет, исправить" :-> "name-step"}]
     :validate {:menu-values true}}

    {:message "Вы зарегистрированы!"}]})

(defn -main []
  (tg-dialog/start-bot bot-commands
                       {:type :polling
                        :token (System/getenv "BOT_TOKEN")
                        :poll-timeout 10
                        :sleep 1000
                        :db {:type :mongo
                             :username "admin"
                             :password "admin"
                             :host "127.0.0.1"
                             :db "telegram"}}))



(deftest test-my-bot
  (tg-dialog/start-bot bot-commands opts)

  (matcho/match
    (tg-dialog-test/send id "/start")
    {:user-data {}
     ;; все переходы
     :steps ["start-step" "name-step" nil]
     ;; telegram http response
     :result [{:result {:text "Привет!"}}
              {:result {:text "Как тебя зовут?"}}
              nil]})

  (matcho/match
    (tg-dialog-test/send id "John")
    {:user-data {id {:name "John"}}
     :steps ["start-step" "name-step" nil]
     :result [{:result {:text "Имя и фамилия должны содержать пробел."}}
              nil]})

  (matcho/match
    (tg-dialog-test/send id "John Doe")
    {:user-data {id {:name "John Doe"}}
     :steps ["start-step" "name-step" "end-step" nil]
     :result [{:result {:text "Спасибо"}} nil]}))
