(ns tg-dialog.mongo-test
  (:require
   [matcho.core :as matcho]
   [tg-dialog.test-data :refer [bot-commands start-command]]
   [monger.collection :as mc]
   [clojure.test :refer [deftest is]]
   [tg-dialog.mongo :as mongo]))

(deftest mongo-test

  (def d (mongo/get-db {:type :mongo
                        :username "admin"
                        :password "admin"
                        :host "127.0.0.1"
                        :db "monger-test"}))
  (def ctx (atom {:commands {:help 1 :start 2}
                  :db d}))

  (mongo/set-command! ctx 285574 :help)

  (is (= {:commands {:help 1 :start 2} :db d}
         @ctx))

  (matcho/match
    (mc/find-maps d mongo/collection {})
    [{:id 285574 :CURRENT_COMMAND "help"} nil])

  (mongo/set-command! ctx 285574 :start)

  (matcho/match (mc/find-maps d mongo/collection {})
                [{:id 285574 :CURRENT_COMMAND "start"} nil])

  ;; (mc/remove d "documents")
  (matcho/match
    (mongo/get-current-command ctx 285574)
    2)

  (mongo/change-current-step! ctx 285574 {:message "a"})

  (matcho/match
    (mc/find-maps d mongo/collection {})
    [{:id 285574 :CURRENT_COMMAND "start"
      :CURRENT_STEP {:message "a"}} nil])


  (mongo/remove-current-step! ctx 285574)

  (matcho/match
    (mc/find-maps d mongo/collection {})
    [{:id 285574 :CURRENT_COMMAND "start"
      :CURRENT_STEP nil} nil])

  (mongo/add-dialog-data! ctx 285574 [:name] "slava")

  (matcho/match
    (mc/find-maps d mongo/collection {})
    [{:id 285574 :CURRENT_COMMAND "start"
      :CURRENT_STEP nil
      :DIALOG_DATA {:name "slava"}} nil])

  (matcho/match
    (mongo/get-dialog-data ctx 285574)
    {:name "slava"})


  (mongo/add-dialog-data! ctx 285574 [:name1] "slava")
  (mongo/change-dialog-data! ctx 285574 {:name 1 :age 2})

  (matcho/match
    (mc/find-maps d mongo/collection {})
    [{:id 285574 :CURRENT_COMMAND "start"
      :CURRENT_STEP nil
      :DIALOG_DATA {:name 1 :age 2 nil nil}} nil])


  (mc/remove d mongo/collection)
  )

