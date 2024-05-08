(ns tg-dialog.steps-test
  (:require
   [matcho.core :as matcho]
   [tg-dialog.steps :as steps]
   [clojure.test :refer [deftest]]))

(def me 202476208)

(deftest steps-test

  (matcho/match
   (steps/find-by-id [{:id 1} {:id 2}] 2)
    {:id 2})

  (matcho/match
   (steps/find-by-id [{:id 1} {:id 2}] 3)
    nil)

  (matcho/match
   (steps/next-by-id [{:id 1} {:id 2}] 1)
    {:id 2})

  (matcho/match
   (steps/next-by-id [{:id 1} {:id 2} {:id 3}] 2)
    {:id 3})

  (matcho/match
   (steps/next-by-id [{:id 1} {:id 2} {:id 3}] 3)
    nil)

  (def steps-atom (atom {me {}}))

  (matcho/match
   (steps/next-step!
    steps-atom [{:id 1} {:id 2} {:id 3}]
    me
    {})
    {:id 1})

  (matcho/match
   (steps/next-step!
    steps-atom [{:id 1} {:id 2} {:id 3}]
    me
    {})
    {:id 2})

  (matcho/match
   (steps/next-step!
    steps-atom [{:id 1 :-> 3} {:id 2} {:id 3 :-> 1}]
    me
    {})
    {:id 3 :-> 1})

  (matcho/match
   (steps/next-step!
    steps-atom [{:id 1 :-> 3} {:id 2} {:id 3 :-> 1}]
    me
    {})
    {:id 1})

  (matcho/match
   (steps/goto-aliases [{:id 1 :-> :end} {:id 2} {:id 3}] :end)
    3)

  (matcho/match
   (steps/goto-aliases [{:id 1 :-> :end} {:id 2} {:id 3}] 3)
    3)

  (matcho/match
   (steps/menu-clicked-goto
    {:menu [{:label 1 :-> :somestep} {:label 2} {:label 3}]}
    1)
    :somestep)

  (matcho/match
   (steps/menu-clicked-goto
    {:menu [{:label 1 :-> :somestep} {:label 2} {:label 3}]}
    2)
    nil)

  (matcho/match
   (steps/goto-without-input?
    {:message 1})
    true)

  (matcho/match
   (steps/goto-without-input?
    {:menu [{}]})
    false)

  (matcho/match
   (steps/goto-without-input?
    {:save-as [:something]})
    false)

  (matcho/match
   (steps/find-by-id
    [{:id 1} {:id 2} {:id 3}]
    :end)
    {:id 3})

  (matcho/match
   (steps/menu-clicked-no-goto
    {:message "m-1", :menu [{:label "menu-1"}
                            {:label "menu-2", :-> :end}]} "menu-1")
    {:label "menu-1"})

  (matcho/match
   (steps/prev-step-by-index [1 2 3] 3)
    2)

  (matcho/match
   (steps/prev-step-by-index [1 2 3] 1)
    nil))
