(ns tg-dialog.example.quiz
  (:require
   [clojure.string :as str]
   [tg-dialog.core :as tg-dialog]))

(def questions
  [{:q "В чем основная цель практик архитектурного проектирования и системной инженерии?"
    :answers ["1. Сокращение времени на разработку"
              "2. Сокращение затрат на разработку"
              "3. Улучшение характеристик разрабатываемой системы"
              "4. Сокращение проектных рисков"]
    :right 4}
   {:q "Почему большинство современных компьютерных систем считаются системами с преобладающей программной составляющей?"
    :answers ["1. Программная составляющая является частью системы."
              "2. Значительная частью бюджета уходит на разработку программного обеспечения."
              "3. Система может распространяться без аппаратного обеспечения."
              "4. Разработка системы включает создание программы испытаний."]
    :right 2}
   {:q "На какой стадии жизненного цикла системы определяется операционное окружение?"
    :answers ["1. Замысел"
              "2. Разработка"
              "3. Производство"
              "4. Применение"
              "5. Поддержка"
              "6. Списание"]
    :right 1}
   {:q "Что такое 'обеспечивающая система'?"
    :answers ["1. Элемент разрабатываемой системы."
              "2. Система из операционного окружения."
              "3. Система энергоснабжения."
              "4. Инвесторы и инвестиционные фонды."
              "5. Система, позволяющая продвигать систему между стадиями жизненного цикла."]
    :right 5}
   {:q "Вы согласны с утверждением: архитектура определяет то, как система будет развиваться в будущем?"
    :answers ["1. Да"
              "2. Нет"]
    :right 1}
   {:q "Вы согласны с утверждением: архитектура затрагивает все вопросы и аспекты устройства системы?"
    :answers ["1. Да"
              "2. Нет"]
    :right 2}])

(defn question-steps [questions]
  (map-indexed
    (fn [idx q]
      (cond-> {:id (str "q" idx)
               :message (:q q)
               :save-as [(keyword (str "q" (inc idx)))]
               :menu (map-indexed
                       (fn [i ans]
                         {:label ans  :value (inc i)})
                       (:answers q))}
        (not= 0 idx)
        (assoc :back (str "q" (dec idx)))))
    questions))

(defn questions-with-messages [questions]
  (->> (question-steps questions)
       (mapv
         (fn [step]
           [step {:message
                  (fn [ctx] (str "Запомнили ваш ответ: "
                                 (get-in ctx (:save-as step))))}]))
       flatten
       vec))

(def bot-commands-1
  {:quiz
   (vec (flatten
          [{:message "Хотите начать тест 'Лекция 1-2'?"
            :menu [{:label "Да"}
                   {:label "Нет" :-> :end :save-as [:skip]}]}

           {:message "Ваш первый вопрос"}

           (questions-with-messages questions)

           {:message (fn [{:keys [q1 q2 q3 q4 q5 q6]}]
                       (let [answers (mapv = [4 2 1 5 1 2] [q1 q2 q3 q4 q5 q6])
                             count-right (count (remove false? answers))]
                         (format "Ваш результат: %s/%s(%s)"
                                 count-right
                                 (count answers)
                                 (str/join "," answers))))}
           {:when (fn [ctx] (:skip ctx))
            :message "Вы пропустили тест."}]))})

(def bot-commands
  {:quiz
   [{:message "Хотите начать тест 'Лекция 1-2'?"
     :menu [{:label "Да"}
            {:label "Нет" :-> :end :save-as [:skip]}]}

    {:message "Ваш первый вопрос"}

    {:id "q1"
     :message "В чем основная цель практик архитектурного проектирования и системной инженерии?"
     :save-as [:q1]
     :menu [{:label "1. Сокращение времени на разработку" :value 1}
            {:label "2. Сокращение затрат на разработку" :value 2}
            {:label "3. Улучшение характеристик разрабатываемой системы" :value 3}
            {:label "4. Сокращение проектных рисков" :value 4}]}

    {:message (fn [ctx] (str "Запомнили ваш ответ: " (:q1 ctx)))}

    {:id "q2"
     :message "Почему большинство современных компьютерных систем считаются системами с преобладающей программной составляющей?"
     :back "q1"
     :save-as [:q2]
     :menu [{:label "1. Программная составляющая является частью системы." :value 1}
            {:label "2. Значительная частью бюджета уходит на разработку программного обеспечения." :value 2}
            {:label "3. Система может распространяться без аппаратного обеспечения." :value 3}
            {:label "4. Разработка системы включает создание программы испытаний." :value 4}]}

    {:message (fn [ctx] (str "Запомнили ваш ответ: " (:q2 ctx)))}

    {:id "q3"
     :message "На какой стадии жизненного цикла системы определяется операционное окружение?"
     :back "q2"
     :save-as [:q3]
     :menu [{:label "1. Замысел" :value 1}
            {:label "2. Разработка" :value 2}
            {:label "3. Производство" :value 3}
            {:label "4. Применение" :value 4}
            {:label "5. Поддержка" :value 5}
            {:label "6. Списание" :value 6}]}

    {:message (fn [ctx] (str "Запомнили ваш ответ: " (:q3 ctx)))}

    {:id "q4"
     :message "Что такое 'обеспечивающая система'?"
     :save-as [:q4]
     :back "q3"
     :menu [{:label "1. Элемент разрабатываемой системы." :value 1}
            {:label "2. Система из операционного окружения." :value 2}
            {:label "3. Система энергоснабжения." :value 3}
            {:label "4. Инвесторы и инвестиционные фонды." :value 4}
            {:label "5. Система, позволяющая продвигать систему между стадиями жизненного цикла." :value 5}]}
    {:message (fn [ctx] (str "Запомнили ваш ответ: " (:q4 ctx)))}

    {:id "q5"
     :message "Вы согласны с утверждением: архитектура определяет то, как система будет развиваться в будущем?"
     :save-as [:q5]
     :back "q4"
     :menu [{:label "1. Да"  :value 1}
            {:label "2. Нет" :value 2}]}

    {:message (fn [ctx] (str "Запомнили ваш ответ: " (:q5 ctx)))}

    {:message "Вы согласны с утверждением: архитектура затрагивает все вопросы и аспекты устройства системы?"
     :save-as [:q6]
     :back "q5"
     :menu [{:label "1. Да"  :value 1}
            {:label "2. Нет" :value 2}]}
    {:message (fn [ctx] (str "Запомнили ваш ответ: " (:q6 ctx)))}

    {:message (fn [{:keys [q1 q2 q3 q4 q5 q6]}]
                (let [answers (mapv = [4 2 1 5 1 2] [q1 q2 q3 q4 q5 q6])
                      count-right (count (remove false? answers))]
                  (format "Ваш результат: %s/%s(%s)"
                          count-right
                          (count answers)
                          (str/join "," answers))))}
    {:when (fn [ctx] (:skip ctx))
     :message "Вы пропустили тест."}]})

#_(tg-dialog/start-bot bot-commands-1 {:type :webhook
                                     :url "https://f3c4-188-243-183-57.ngrok-free.app"
                                     :token (System/getenv "BOT_TOKEN")
                                     :port 8080})
