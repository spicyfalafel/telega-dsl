(ns tg-dialog.core-test
  (:require
   [tg-dialog.core :as sut]
   [matcho.core :as matcho]
   [tg-dialog.commands :as commands]
   [tg-dialog.test-data :refer [bot-commands start-command]]
   [clojure.test :refer [deftest]]))

(def me 202476208)

(deftest core-test
  (matcho/match
   (sut/process-message (atom {:commands bot-commands}) me {:text "/command"})
    {:result {:text "no such command"}})

  (matcho/match
   (sut/process-message (atom {:commands bot-commands}) me {:text "/help"})
    {:result {:text "Hello"}, :ok true})

  (matcho/match
   (sut/process-message (atom {:commands bot-commands}) me {:text "/start"})
    [{:result {:text "m-1"}, :ok true}]))

(deftest whole
  (def ctx (atom {:commands bot-commands}))

  (matcho/match
   (sut/process-message ctx me {:text "/start"})
    [{:result
      {:reply_markup
       {:inline_keyboard
        [[{:callback_data "menu-1", :text "menu-1"}
          {:callback_data "menu-2", :text "menu-2"}]]},
       :text "m-1"}}])

  (matcho/match
   @ctx
    {me {:CURRENT_COMMAND :start
         :CURRENT_STEP (first start-command)}})

  (matcho/match
   (sut/process-message
    ctx
    me
    {:text "menu-1"})
    [{:result
      {:reply_markup
       {:inline_keyboard
        [[{:callback_data "value-2-1", :text "menu-2-1"}
          {:callback_data "menu-2-2", :text "menu-2-2"}
          {:callback_data "menu-2-3", :text "menu-2-3"}]]},
       :text "m-2"},
      :ok true}])

  (matcho/match
   (sut/process-message
    ctx
    me
    {:text "menu-2-2"})
    [{:result
      {:text "hello! menu menu-2-2"},
      :ok true}])

  (matcho/match
   (sut/process-message
    ctx
    me
    {:text "/start"})
    [{:result
      {:reply_markup
       {:inline_keyboard
        [[{:callback_data "menu-1", :text "menu-1"}
          {:callback_data "menu-2", :text "menu-2"}]]},
       :text "m-1"},
      :ok true}]))

(deftest corner
  (def steps
    [{:message "a"}
     {:message "b"
      :menu [{:label "n" :value false}
             {:label "y" :value true}]
      :save-as [:bio?]}
     {:when (fn [ctx] (= true (:bio? ctx)))
      :message "c"}
     {:message "end"}])

  (def ctx (atom {:commands (commands/prepare-commands {:start steps})}))

  (matcho/match
   (sut/process-message ctx me {:text "/start"})
    [{:result {:text "a"}}
     {:result {:reply_markup
               {:inline_keyboard
                [[{:callback_data "false", :text "n"}
                  {:callback_data "true", :text "y"}]]},
               :text "b"}} nil])

  (matcho/match
   (sut/process-message ctx me {:text "n"})
    [{:result {:text "end"}, :ok true} nil])

  (matcho/match
   @ctx
    {me {:DIALOG_DATA {:bio? false}}})

  (matcho/match
   (sut/process-message ctx me {:text "/start"})
    [{:result {:text "a"}}
     {:result {:reply_markup
               {:inline_keyboard
                [[{:callback_data "false", :text "n"}
                  {:callback_data "true", :text "y"}]]},
               :text "b"}} nil])

  (matcho/match
   (sut/process-message ctx me {:text "y"})
    [{:result {:text "c"}}
     {:result {:text "end"}}
     nil]))

(deftest back-test
  (def steps
    [{:message "a"}
     {:message "b"
      :menu [{:label "n"}
             {:label "y"}]
      :back true
      :save-as [:b]}
     {:message "c"
      :back true}
     {:message "end"}])

  (def ctx (atom {:commands (commands/prepare-commands {:start steps})}))

  (matcho/match
   @ctx
    {:commands {:start
                [{:id "no-id-step0"}
                 {:id "no-id-step1"
                  :menu
                  [{} {}
                   {:label "Back" :value "Back" :-> "no-id-step0"} nil]}
                 {:menu
                  [{:label "Back",
                    :-> "no-id-step1",
                    :value "Back"} nil]}
                 {}]}})

  (matcho/match
   (sut/process-message ctx me {:text "/start"})
    [{:result {:text "a"}}
     {:result {:reply_markup
               {:inline_keyboard
                [[{:callback_data "n", :text "n"}
                  {:callback_data "y", :text "y"}
                  {:callback_data "Back", :text "Back"}]]},
               :text "b"}} nil])

  (matcho/match
   (sut/process-message ctx me {:text "Back"})
    [{:result {:text "a"}}
     {:result {:reply_markup
               {:inline_keyboard
                [[{:callback_data "n", :text "n"}
                  {:callback_data "y", :text "y"}]]},
               :text "b"}} nil])

  (matcho/match
   (sut/process-message ctx me {:text "y"})
    [{:result {:reply_markup
               {:inline_keyboard
                [[{:callback_data "Back", :text "Back"} nil]]},
               :text "c"}} nil]))
