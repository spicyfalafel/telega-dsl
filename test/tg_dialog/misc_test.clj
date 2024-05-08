(ns tg-dialog.misc-test
  (:require
   [tg-dialog.misc :as misc]
   [clojure.test :refer [is deftest]]))

(deftest misc-test

  (is (= (misc/dissoc-path {:a {:b 1}} [:a :b])
         {:a {}}))

  (is (= {} (misc/dissoc-path {:a 1} [:a]))))
