(ns tg-dialog.example.aiogram-example
 (:require [tg-dialog.core :as tg-dialog]))

(def bot-commands
  {:start ; if command is start +1
   [{:message "Hi there! What's your name?"
     :save-as [:name]}

    {:message "Do you like to write bots?"
     :save-as [:like_bots]
     :menu [{:label "Yes"}
            {:label "No" :-> :end}] ; go to end if pressed no +1 (is it nested because of start?)
     :reply true
     :validate {:menu-values true}
     :error "I don't understand you :("}

    {:message "Cool, I'm too!\nWhat programming language did you use for it?"
     :save-as [:language]}

    {:when (fn [ctx] (= "Python" (:language ctx))) ; +1
     :message "Python, you say? That's the language that makes my circuits light up! 😉"
     :reply true}

    {:message
     (fn [ctx]
       (cond-> (format "I'll keep you in mind, %s," (:name ctx))

         (and (:language ctx) (= "Yes" (:like_bots ctx))) ; if + && => +2
         (str (format " you like to write bots with %s" (:language ctx)))

         (= "No" (:like_bots ctx)) ;; if +1
         (str " you don't like to write bots, so sad...")))}]})

(def opts
  {:type :webhook
   :url "https://b212-95-164-88-155.ngrok-free.app"
   :token (System/getenv "BOT_TOKEN")
   :port 8080
   :db {:type :mongo
        :username "admin"
        :password "admin"
        :host "127.0.0.1"
        :db "telegram"}})

(defn -main []
 (tg-dialog/start-bot bot-commands opts))

 ;; Cognitive Complexity: 6
