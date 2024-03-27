(ns poc.telega-dsl-example
  (:require [malli.core :as m]
            [poc.core :as poc]
            [clojure.string :as str]))

(set! *warn-on-reflection* true)

(defn student-exists? [name-s]
  (println "student exists stub" name-s))

(def name-schema
  [:and
   [:re {:error/message "Имя и фамилия должны содержать пробел."} #".+\s+.+"]
   [:fn
    {:error/fn (fn [_] "Имя и фамилия должны начинаться с заглавной буквы")}
    (fn [name-s]
      (let [[name surname] (str/split name-s #"\s+")]
        (and (Character/isUpperCase ^Character (first name))
             (Character/isUpperCase ^Character (first surname)))))]])

(defn validate-name [name-s]
  (if (m/validate name-schema name-s)
    (when-not (student-exists? name-s)
      {:error/message "Студент с таким именем и фамилией не найден в базе данных."})
    {:error/message "Неверный формат имени и фамилией."}))

(def bot-commands
  [{:command "/start"
    :steps
    [{:message "Добро пожаловать! Давайте начнем регистрацию."}

     {:id "ask-group"
      :message "Выберите вашу группу:"
      :menu (conj
             (for [group (poc/dbget [:groups])]
               {:label (str "Группа " group) :save-as [:group] :value group})
             {:label "Я не хочу регистрироваться" :save-as [:skip-registration] :value true :end true})}

     {:id "ask-name"
      :message "Введите ваше имя и фамилию:"
      :validate validate-name
      :save-as [:name]
      :on-back {:action (fn [] (poc/dbdelete [:name]))
                :message "Имя и фамилия удалены."}
      :back true}

     {:id "ask-bio"
      :message "Хотите рассказать о себе?"
      :menu [{:label "Да" :save-as [:bio?] :value true}
             {:label "Нет" :save-as [:bio?] :value false}]
      :on-back {:action (fn [] (poc/dbdelete [:bio?]))
                :message "Информация о биографии удалена."}
      :back true}

     {:id "write-bio"
      :message "Расскажите о своих увлечениях в IT:"
      :validate (fn [bio]
                  (when (<= (count bio) 50)
                    {:error/message "Описание должно быть длиннее 50 символов."}))
      :save-as [:bio]
      :back true}

     {:id "confirm-bio"
      :save-as [:bio]
      :message (fn [] (if-let [bio (poc/dbget [:bio])]
                        (str "Ваше описание: " bio)
                        "Введите ваше описание"))
      :menu [{:label "Готово"}]
      :back "ask-bio"}

     {:id "register-done"
      :message (fn [] (if (poc/dbget [:skip-registration])
                        "Регистрация пропущена."
                        (if (poc/dbget [:bio?])
                          "Регистрация пройдена успешно. Спасибо за описание!"
                          "Регистрация пройдена успешно.")))}]}

   {:command "/help"
    :message "Этот бот помогает студентам пройти регистрацию в системе обучения."}])

(defn -main []
  (poc/start-bot bot-commands {:token (System/getenv "BOT_TOKEN")
                               :type :polling}))
