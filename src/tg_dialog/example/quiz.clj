(ns tg-dialog.example.quiz
  (:require
   [clojure.string :as str]))

;; Для 6 конкретных вопросов
(def bot-commands-hardcoded
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

;; любое количество вопросов!
(def questions
  [{:q "q1"
    :answers ["a1" "a2" "a3" "a4"]
    :right 4}
   {:q "q2"
    :answers ["a1" "a2" "a3" "a4"]
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

(defn questions-ids [questions]
  (map-indexed
    (fn [idx _] (keyword (str "q" (inc idx))))
        questions))

(defn answers [ctx]
  (vals (select-keys ctx (questions-ids ctx))))

(def bot-commands-generated
  {:quiz
   (vec (flatten
          [{:message "Хотите начать тест 'Лекция 1-2'?"
            :menu [{:label "Да"}
                   {:label "Нет" :-> :end :save-as [:skip]}]}

           {:message "Ваш первый вопрос"}

           (questions-with-messages questions)

           {:message (fn [ctx]
                       (let [answers (mapv = (answers ctx) (mapv :right questions))
                             count-right (count (remove false? answers))]
                         (format "Ваш результат: %s/%s(%s)"
                                 count-right
                                 (count answers)
                                 (str/join "," answers))))}
           {:when (fn [ctx] (:skip ctx))
            :message "Вы пропустили тест."}]))})


(def maybe
  {:quiz
   [{:message "Хотите начать тест 'Лекция 1-2'?"
     :menu [{:label "Да"}
            {:label "Нет" :-> :end :save-as [:skip]}]}

    {:message "Ваш первый вопрос"}

    {:id "q1"
     :message ""
     :save-as [:q1]
     :menu [{:label "1. Сокращение времени на разработку" :value 1}
            {:label "2. Сокращение затрат на разработку" :value 2}
            {:label "3. Улучшение характеристик разрабатываемой системы" :value 3}
            {:label "4. Сокращение проектных рисков" :value 4}]}

    {:message (fn [ctx] (str "Запомнили ваш ответ: " (:q1 ctx)))}

    {:message (fn [{:keys [q1 q2 q3 q4 q5 q6]}]
                (let [answers (mapv = [4 2 1 5 1 2] [q1 q2 q3 q4 q5 q6])
                      count-right (count (remove false? answers))]
                  (format "Ваш результат: %s/%s(%s)"
                          count-right
                          (count answers)
                          (str/join "," answers))))}
    {:when (fn [ctx] (:skip ctx))
     :message "Вы пропустили тест."}]})
