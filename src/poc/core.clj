(ns poc.core
  (:require
   [telegrambot-lib.core :as tbot]
   [malli.core :as m]
   [clojure.string :as str]
   [jsonista.core :as json]
   [poc.validation :as validation]
   [poc.example-group :as example]
   [poc.bot :as bot]
   [org.httpkit.client :as client]
   [org.httpkit.server :as hk-server]
   [malli.generator :as mg]))

(set! *warn-on-reflection* true)

(defn dbget [path]
  (println "Getting stub " path))

(defn dbdelete [path]
  (println "Deleting stub " path))

(def config
  {:timeout 10
   :sleep 10000}) ;thread/sleep is in milliseconds

#_(defmacro menu [items]
    `(for [item# ~items]
       {:label (:label item#)
        :on-select (fn [] (dbsave (:save-as item#) (:value item#)))}))

(def no-id-step-prefix "no-id-step")


(defn get-ids [steps]
  (vec (map-indexed
        (fn [idx s]
          (if-let [id (:id s)]
            id
            (str no-id-step-prefix idx)))
        steps)))

(defn send-menu [id data menu]
  (tbot/send-message bot/mybot id "todo send menu"))

(defn call-message-fn [id data message]
  (tbot/send-message bot/mybot id "todo send message fn")
  )

(defn handle-step [step id data]
  (cond-> []
    (string? (:message step))
    (conj #(tbot/send-message bot/mybot id (:message step)))

    (:message step)
    (conj #(call-message-fn id data (:message step)))

    (:menu step)
    (conj #(send-menu id data (:menu step)))))

(defn handle-command [command id data]
  (assert (= true (validation/validate-command command)) "wrong command")
  (let [steps (:steps command)
        _ (println "steps " steps)
        ids (get-ids steps)
        message (:message command)
        steps (:steps command)]
    (cond
      message
      (tbot/send-message bot/mybot id message)

      steps
      (doseq [step steps]
        (doseq [f (handle-step step id data)]
          (f))))
    ids))

(defn app [commands]
  (assert (map? commands) "expected commands to be map")
  (println "server is starting")
    (fn [req]
      (let [body (json/read-value (:body req) json/keyword-keys-object-mapper)
            data (-> body :message :text)
            id (-> body :message :chat :id)
            ; TODO: find current step
            commands-handled (mapv
                               (fn [[c-name v]]
                                 (handle-command (assoc v :name c-name) id data))
                               commands)]
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

  (def ngrok-url "https://e97a-188-243-183-57.ngrok-free.app")
  (def token (System/getenv "BOT_TOKEN"))

  ;;https://api.telegram.org/bot{my_bot_token}/setWebhook?url={url_to_send_updates_to}
  (let [token token
        hook-url ngrok-url]
    [@(client/request {:url (str "https://api.telegram.org/bot" token "/deleteWebhook")
                       :query-params {:drop_pending_updates true}})

     @(client/request {:url (str "https://api.telegram.org/bot" token "/setWebhook")
                       :query-params {:url (str hook-url)}})]))
