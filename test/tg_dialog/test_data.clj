(ns tg-dialog.test-data
  (:require [tg-dialog.commands :as commands]))

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
  (commands/prepare-commands
   {:start start-command
    :help help-command}))

