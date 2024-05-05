(ns tg-dialog.core
  (:require
   [telegrambot-lib.core :as tbot]
   #_[malli.core :as m]
   [clojure.string :as str]
   [jsonista.core :as json]
   [tg-dialog.validation :as validation]
   [tg-dialog.example.example-group :as example]
   [tg-dialog.bot :as bot]
   [tg-dialog.steps :as steps]
   [org.httpkit.client :as client]
   [org.httpkit.server :as hk-server]
   #_[malli.generator :as mg]))

(set! *warn-on-reflection* true)

(defn dbget [path]
  (println "Getting stub " path))

(defn dbdelete [path]
  (println "Deleting stub " path))

#_(defmacro menu [items]
    `(for [item# ~items]
       {:label (:label item#)
        :on-select (fn [] (dbsave (:save-as item#) (:value item#)))}))

(defn handle-command [command id data]
  (assert (= true (validation/validate-command command)) "wrong command")
  (let [message (:message command)
        steps (:steps command)]
    (println "message" message)
    (println "seps" steps)
    (cond
      message
      (tbot/send-message bot/mybot id message)

      steps
      (steps/handle-current-step steps id data))))

(defn app [commands]
  (assert (map? commands) "expected commands to be map")
  (println "server is starting")
    (fn [req]
      (let [body (json/read-value (:body req) json/keyword-keys-object-mapper)
            data (-> body :message :text)
            id (-> body :message :chat :id)
            command-key
            (when (str/starts-with? data "/")
              (first (filterv #(= % (keyword (subs data 1)))
                              (keys commands))))]
        (println "command key" command-key)
        (if command-key
          (handle-command
            (assoc (command-key commands) :name command-key)
            id data)
          (tbot/send-message bot/mybot id "no such command"))
        (println "\nid " id " data " data "\n")
        {:status  200
         :headers {"Content-Type" "text/html"}
         :body   "ok"})))

(defonce SERVER (atom nil))

(defn start-bot [commands & [opts]]
  (let [port (:port opts)]
    (if @SERVER
      (do (@SERVER)
          (reset! SERVER nil)
          :down)
      (let [server (hk-server/run-server (app commands)
                                         {:port (or port 8080)})]
        (when server
          (reset! SERVER server))
        :up))))

(start-bot example/bot-commands)

(comment
  (start-bot {:help {} :a {}})

  (def ngrok-url "https://106e-188-243-183-57.ngrok-free.app")
  (def token (System/getenv "BOT_TOKEN"))

  ;;https://api.telegram.org/bot{my_bot_token}/setWebhook?url={url_to_send_updates_to}
  (let [token token
        hook-url ngrok-url]
    [@(client/request {:url (str "https://api.telegram.org/bot" token "/deleteWebhook")
                       :query-params {:drop_pending_updates true}})

     @(client/request {:url (str "https://api.telegram.org/bot" token "/setWebhook")
                       :query-params {:url (str hook-url)}})]))
