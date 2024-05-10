(ns tg-dialog.example.telega-dsl-example
  (:require [malli.core :as m]
            [malli.error :as me]
            [clojure.string :as str]))

(set! *warn-on-reflection* true)

(defn student-exists? [name-s] true)

(def name-schema
  #_[:re {:error/message "Имя и фамилия должны содержать пробел."} #".+\s+.+"]
  [:and
   [:re {:error/message "Имя и фамилия должны содержать пробел."} #".+\s+.+"]
   [:fn
    {:error/fn (fn [_ _] "Имя и фамилия должны начинаться с заглавной буквы")}
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
  {:start [{:message "Добро пожаловать! Давайте начнем регистрацию."}

           {:message "Выберите вашу группу:"
            :validate {:menu-values true}
            :error "Выберите правильную группу!"
            :menu [{:label "P34111" :save-as [:group]}
                   {:label "P34112" :save-as [:group]}
                   {:label "P34113" :save-as [:group]}
                   {:label "Я не хочу регистрироваться"
                    :save-as [:skip-registration]
                    :value true
                    :-> :end}]}

           {:message "Введите ваше имя и фамилию:"
            :save-as [:name]
            :validate name-schema
            :back true}

           {:message "Хотите рассказать о себе?"
            :menu [{:label "Да"  :value true}
                   {:label "Нет" :value false}]
            :save-as [:want-bio]
            :validate {:menu-values true}
            :back true}

           {:when (fn [ctx] (= true (:want-bio ctx)))
            :message "Расскажите о своих увлечениях в IT:"
            :validate (fn [bio]
                        (when (<= (count bio) 50)
                          "Описание должно быть длиннее 50 символов."))
            :save-as [:bio]
            :back true}

           {:message (fn [ctx]
                       (if-let [bio (:bio ctx)]
                         (str "Ваше описание: " bio)
                         "Описание не введено"))
            :menu [{:label "Готово"}]
            :validate {:menu-values true}
            :back "ask-bio"}

           {:message
            (fn [ctx]
              (if (:skip-registration ctx)
                "Регистрация пропущена."
                (if (:bio? ctx)
                  "Регистрация пройдена успешно. Спасибо за описание!"
                  "Регистрация пройдена успешно.")))}]

   :whoami [{:message (fn [ctx]
                        (format "Группа: %s\nИмя: %s\nО вас: %s"
                                (:group ctx) (:name ctx) (:bio ctx)))}]

   :help [{:message "Этот бот помогает студентам пройти регистрацию в системе обучения."}]})
