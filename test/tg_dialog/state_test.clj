(ns tg-dialog.state-test
  (:require
   [matcho.core :as matcho]
   [tg-dialog.state :as state]
   [clojure.test :refer [is deftest]]))

(deftest state-test

  (def me 202476208)

  (matcho/match
   (state/get-current-command (atom {me {:CURRENT_COMMAND :help}
                                     :commands {:help [{:id 1}]}})
                              me)
    [{:id 1}])


  (matcho/match
   (state/get-all-data-paths
    [{:save-as [:a :b]}
     {:menu [{:save-as [:d :e]}
             {:save-as [:x]}
             {:label [:g :v]}]}

     {:menu [{} {}]}
     {}])
   #{[:a :b] [:d :e] [:x]})

  (def a (atom {:commands {:start [{:save-as [:a :b]}
                                   {:menu [{:save-as [:d :e]}
                                           {:save-as [:x]}
                                           {:label [:g :v]}]}

                                   {:menu [{} {}]}
                                   {}]}
                1
                {:DIALOG_DATA {:a {:b 10}
                               :c 5
                               :x 100
                               :z {:y 6}
                               :d {:e 1000}}}}))
  (state/remove-data-paths! a 1 :start)

  (is (= {:DIALOG_DATA {:a {}, :c 5, :z {:y 6}, :d {}}}
         (get @a 1)))

  )

(deftest steps-test

  (matcho/match
   (state/find-by-id [{:id 1} {:id 2}] 2)
    {:id 2})

  (matcho/match
   (state/find-by-id [{:id 1} {:id 2}] 3)
    nil)

  (matcho/match
   (state/next-by-id [{:id 1} {:id 2}] 1)
    {:id 2})

  (matcho/match
   (state/next-by-id [{:id 1} {:id 2} {:id 3}] 2)
    {:id 3})

  (matcho/match
   (state/next-by-id [{:id 1} {:id 2} {:id 3}] 3)
    nil)

  (def steps-atom (atom {:commands {:help [{:id 1} {:id 2} {:id 3}]}
                         me {:CURRENT_COMMAND :help}}))

  (matcho/match
   (state/next-step!
    steps-atom [{:id 1} {:id 2} {:id 3}]
    me
    {})
    {:id 1})

  (matcho/match
   (state/next-step!
    steps-atom [{:id 1} {:id 2} {:id 3}]
    me
    {})
    {:id 2})

  ;; (matcho/match
  ;;  (state/next-step!
  ;;   steps-atom [{:id 1 :-> 3} {:id 2} {:id 3 :-> 1}]
  ;;   me
  ;;   {})
  ;;   {:id 3 :-> 1})

  ;; (matcho/match
  ;;  (state/next-step!
  ;;   steps-atom [{:id 1 :-> 3} {:id 2} {:id 3 :-> 1}]
  ;;   me
  ;;   {})
  ;;   {:id 1})

  (matcho/match
   (state/goto-aliases [{:id 1 :-> :end} {:id 2} {:id 3}] :end)
    3)

  (matcho/match
   (state/goto-aliases [{:id 1 :-> :end} {:id 2} {:id 3}] 3)
    3)

  (matcho/match
   (state/menu-clicked-goto
    {:menu [{:label 1 :-> :somestep} {:label 2} {:label 3}]}
    1)
    :somestep)

  (matcho/match
   (state/menu-clicked-goto
    {:menu [{:label 1 :-> :somestep} {:label 2} {:label 3}]}
    2)
    nil)

  (matcho/match
   (state/goto-without-input?
    {:message 1})
    true)

  (matcho/match
   (state/goto-without-input?
    {:menu [{}]})
    false)

  (matcho/match
   (state/goto-without-input?
    {:save-as [:something]})
    false)

  (matcho/match
   (state/find-by-id
    [{:id 1} {:id 2} {:id 3}]
    :end)
    {:id 3})

  (matcho/match
   (state/menu-clicked-no-goto
    {:message "m-1", :menu [{:label "menu-1"}
                            {:label "menu-2", :-> :end}]} "menu-1")
    {:label "menu-1"})

  (matcho/match
   (state/prev-step-by-index [1 2 3] 3)
    2)

  (matcho/match
   (state/prev-step-by-index [1 2 3] 1)
    nil))
