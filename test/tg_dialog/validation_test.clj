(ns tg-dialog.validation-test
  (:require
   [tg-dialog.validation :as v]
   [clojure.test :refer [deftest is]]))

(deftest validate-label
  (is  (= true (v/validate-label {:label "Next"})))

  (is  (= true (v/validate-label
                {:label "Я не хочу регистрироваться" :-> :end})))

  (is  (= true (v/validate-label
                {:label "Я не хочу регистрироваться" :-> "someid"})))

  (is  (= true (v/validate-label
                {:label "Group1" :save-as [:group] :value "group1"})))

  (is  (= true (v/validate-label {:label "str" :value 2})))
  (is  (= false (v/validate-label {:label 1 :value 2})))

  (is  (= false (v/validate-label {:name :start
                                   :message "hello"}))))

(deftest validate-step
  (is (= true (v/validate-step
               {:id "ask-group"
                :message "Выберите вашу группу:"
                :menu [{:label "Group1" :save-as [:group] :value "group1"}
                       {:label "Group2" :save-as [:group] :value "group2"}
                       {:label "Group3" :save-as [:group] :value "group3"}]})))

  (is (= true (v/validate-step
               {:id "start-register"
                :message "Добро пожаловать! Давайте начнем регистрацию."
                :menu [{:label "Next"}
                       {:label "Я не хочу регистрироваться" :-> :end}]})))

  (is (= true (v/validate-step {:id "register-done"
                                :message (fn [_]
                                           (str "Привет! "))})))

  (is (= false (v/validate-step {:id "register-done"
                                 :message 1}))))

(deftest validate-command

  (is (= false (v/validate-command {:name :start
                                    :message "hello"})))

  (is (= true (v/validate-command {:message "hello"})))

  (is (= false (v/validate-command {})))

  (is (= false (v/validate-command [])))

  (is (= false (v/validate-command [{}])))

  (is (= false (v/validate-command [{:a 1}])))

  (is (= true (v/validate-command [{:message "hi"}])))
  (is (= true (v/validate-command [{:message "hi"} {:message "hi"}]))))

(deftest validate-commands

  (is (= true (v/validate-commands {:help {:message "hi"}})))
  (is (= false (v/validate-commands {"help" [{:message "hi"}]})))

  (is (= true (v/validate-commands {:help {:message "hi"}
                                    :start [{:message "a"}
                                            {:message "b"}]})))

  (is (= false (v/validate-commands {:help :a}))))


