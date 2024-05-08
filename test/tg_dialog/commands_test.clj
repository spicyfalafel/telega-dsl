(ns tg-dialog.commands-test
  (:require
   [matcho.core :as matcho]
   [tg-dialog.commands :as commands]
   [clojure.test :refer [deftest]]))

(deftest commands-test
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

  (matcho/match (commands/add-ids start-command)
    [{:id "no-id-step0"} {:id "no-id-step1"} {:id "no-id-step2"} nil])

  (matcho/match
   (commands/prepare-commands {:help {:id "hello"}
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
     :start [{:id "world"} {:id "no-id-step1"} {:id "no-id-step2"}]}))
