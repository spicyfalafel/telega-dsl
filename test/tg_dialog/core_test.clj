(ns tg-dialog.core-test
  (:require
   [tg-dialog.core :as sut]
   [matcho.core :as matcho]
   [tg-dialog.steps :as steps]
   [tg-dialog.misc :as misc]
   [clojure.test :refer [is testing deftest]]))

(def help-command  {:message "Hello"})

(def start-command
  [{:message "m-1"
    :menu [{:label "menu-1"}
           {:label "menu-2" :-> :end}]}

   {:message "m-2"
    :menu [{:label "menu-2-1" :save-as [:menu] :value "value-2-1"}
           {:label "menu-2-2" :save-as [:menu]}
           {:label "menu-2-3" :save-as [:menu]}]}

   {:message (fn [ctx]
               (str "hello! menu "
                    (if-let [menu (:menu ctx)]
                      menu
                      "not chosen")))}])

(def bot-commands
  (sut/prepare-commands
   {:start start-command
    :help help-command}))

(def me 202476208)

(deftest core-test
  (matcho/match
   (sut/handle-command (atom {:commands bot-commands}) :help me {})
    {:result
     {:chat
      {:id 202476208},
      :text "Hello"},
     :ok true})

  (matcho/match (steps/add-ids start-command)
    [{:id "no-id-step0"} {:id "no-id-step1"} {:id "no-id-step2"} nil])

  (def abc (atom {}))
  (def steps [{:id 1} {:id 2} {:id 3}])

  (matcho/match
   (steps/next-step abc steps me nil)
    {:id 1})

  (matcho/match
   (steps/next-step abc steps me nil)
    {:id 1})

  (matcho/match
   (steps/next-step (atom {me {:CURRENT_STEP {:id 1}}}) steps me nil)
    {:id 2})

  (matcho/match
   (steps/next-step (atom {me {:CURRENT_STEP {:id 2}}}) steps me nil)
    {:id 3})

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

  (matcho/match
   (steps/find-next-step [{:id 1} {:id 2} {:id 3}] {:id 3} {})
    nil)

  (matcho/match
   (steps/find-next-step [{:id 1} {:id 2} {:id 3}] {:id 2} {})
    {:id 3})

  (matcho/match
   (steps/find-next-step [{:id 1 :-> 3} {:id 2} {:id 3}]
                         {:id 1 :-> 3} {})
    {:id 3})

  (matcho/match
   (steps/find-next-step [{:id 1} {:id 2} {:id 3 :-> 1}]
                         {:id 3 :-> 1} {})
    {:id 1})

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
   (sut/handle-command (atom {:commands bot-commands})
                       :start
                       me {})
    [{:result
      {:text "m-1"
       :reply_markup
       {:inline_keyboard
        [[{:callback_data "menu-1", :text "menu-1"}
          {:callback_data "menu-2", :text "menu-2"}]]}}}])

  (matcho/match
   (sut/process-message (atom {:commands bot-commands}) me {:text "/command"})
    {:result {:text "no such command"}})

  (matcho/match
   (sut/process-message (atom {:commands bot-commands}) me {:text "/help"})
    {:result {:text "Hello"}, :ok true})

  (matcho/match
   (sut/process-message (atom {:commands bot-commands}) me {:text "/start"})
    [{:result {:text "m-1"}, :ok true}])

  ;; /start -> step1
  ;; send text & menu => wait, step1
  ;; click on button => callback, step2

  (matcho/match
   (sut/prepare-commands {:help {:id "hello"}
                          :help1 [{:message "a"
                                   :save-as [:abc]
                                   :menu [{:label "a"}
                                          {:label "b" :value false}]}]
                          :start [{:id "world"}
                                  {}
                                  {}]})

    {:help {:id "hello"},
     :help1 [{:id string?
              :save-as [:abc]
              :menu [{:label "a" :value "a"}
                     {:label "b" :value "false"}]}]
     :start [{:id "world"} {:id "no-id-step1"} {:id "no-id-step2"}]})

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
   (sut/current-command (atom {me {:CURRENT_COMMAND :help}
                               :commands {:help [{:id 1}]}})
                        me)
    [{:id 1}])

  ;; command -> first step
  ;; step needs input -> stay on step
  ;; step do not needs input -> go further
  ;; has step and callback -> go further
  ;; has step and new message -> go further
  )

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

  (is (some? (sut/in-dialog? ctx me)))

  (matcho/match
   (steps/menu-clicked-no-goto
    {:message "m-1", :menu [{:label "menu-1"}
                            {:label "menu-2", :-> :end}]} "menu-1")
    {:label "menu-1"})

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
      :ok true}])

  (matcho/match
   (misc/get-all-data-paths
    [{:save-as [:a :b]}
     {:menu [{:save-as [:d :e]}
             {:save-as [:x]}
             {:label [:g :v]}]}

     {:menu [{} {}]}
     {}])
    #{[:a :b] [:d :e] [:x]})

  (is (= (misc/dissoc-path {:a {:b 1}} [:a :b])
         {:a {}}))

  (is (= {} (misc/dissoc-path {:a 1} [:a])))

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
  (misc/remove-data-paths! a 1 :start)

  (matcho/match
   @a
    {:commands
     {}
     1 {:DIALOG_DATA {:c 5 :z {:y 6}}}})

  (matcho/match
   (steps/menu->tg [{:label "Да"  :value true}
                    {:label "Нет" :value false}])
    [[{:text "Да", :callback_data "true"}
      {:text "Нет", :callback_data "false"}]]))

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

  (def ctx (atom {:commands (sut/prepare-commands {:start steps})}))

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
     nil])

  (matcho/match
   (steps/text->value
    [{:label "a" :value "b"}
     {:label "c" :value "false"}]
    "c")
    "false"))

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

  (def ctx (atom {:commands (sut/prepare-commands {:start steps})}))

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
               :text "c"}} nil])

  (matcho/match
   (steps/prev-step-by-index [1 2 3] 3)
    2)

  (matcho/match
   (steps/prev-step-by-index [1 2 3] 1)
    nil))
