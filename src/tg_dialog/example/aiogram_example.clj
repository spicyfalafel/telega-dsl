(ns tg-dialog.example.aiogram-example
  (:require
   [tg-dialog.core :as tg-dialog]))

(def bot-commands
  {:start
   [{:message "Hi there! What's your name?"
     :save-as [:name]}

    {:message "Do you like to write bots?"
     :save-as [:like_bots]
     :menu [{:label "Yes"}
            {:label "No" :-> :end}]
     :reply true
     :validate {:menu-values true}
     :error {:message "I don't understand you :("}}

    {:message "Cool, I'm too!\nWhat programming language did you use for it?"
     :save-as [:language]}

    {:when (fn [ctx] (= "Python" (:language ctx)))
     :message "Python, you say? That's the language that makes my circuits light up! 😉"
     :reply true}

    {:message
     (fn [ctx]
       (cond-> (format "I'll keep you in mind, %s," (:name ctx))

         (and (:language ctx) (= "Yes" (:like_bots ctx)))
         (str (format " you like to write bots with %s" (:language ctx)))

         (= "No" (:like_bots ctx))
         (str " you don't like to write bots, so sad...")))}]})


(defn -main []
 (tg-dialog/start-bot bot-commands {:type :webhook
                                    :url "https://f3c4-188-243-183-57.ngrok-free.app"
                                    :token (System/getenv "BOT_TOKEN")
                                    :port 8080}))
