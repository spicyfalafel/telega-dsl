(ns tg-dialog.handlers-test
  (:require
   [matcho.core :as matcho]
   [tg-dialog.handlers :as handlers]
   [tg-dialog.test-data :refer [bot-commands]]
   [clojure.test :refer [deftest]]))

(def me 202476208)

(deftest handlers-test

  (matcho/match
   (handlers/handle-command (atom {:commands bot-commands}) :help me {})
    {:result
     {:text "Hello"},
     :ok true})

  (def abc (atom {}))
  (def steps [{:id 1} {:id 2} {:id 3}])

  (matcho/match
   (handlers/handle-command (atom {:commands bot-commands})
                            :start
                            me {})
    [{:result
      {:text "m-1"
       :reply_markup
       {:inline_keyboard
        [[{:callback_data "menu-1", :text "menu-1"}
          {:callback_data "menu-2", :text "menu-2"}]]}}}])

  (matcho/match
   (handlers/menu->tg [{:label "Да"  :value true}
                       {:label "Нет" :value false}])
    [[{:text "Да", :callback_data "true"}
      {:text "Нет", :callback_data "false"}]])

(matcho/match
   (handlers/text->value
    [{:label "a" :value "b"}
     {:label "c" :value "false"}]
    "c")
    "false")
  )
