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
