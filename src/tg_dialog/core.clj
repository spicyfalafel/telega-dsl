(ns tg-dialog.core
  (:require
    [telegrambot-lib.core :as tbot]
    #_[malli.core :as m]
    [clojure.string :as str]
    [jsonista.core :as json]
    [tg-dialog.misc :as misc]
    [tg-dialog.example.example-group :as example]
    [tg-dialog.bot :as bot]
    [tg-dialog.steps :as steps]
    [org.httpkit.client :as client]
    [org.httpkit.server :as hk-server]
    #_[malli.generator :as mg]))

(set! *warn-on-reflection* true)

#_(defmacro menu [items]
    `(for [item# ~items]
       {:label (:label item#)
        :on-select (fn [] (dbsave (:save-as item#) (:value item#)))}))

(def send-message (partial tbot/send-message bot/mybot))

(defn set-command! [ctx id command-key]
  (swap! ctx (fn [m] (assoc-in m [id :CURRENT_COMMAND] command-key))))

(defn handle-command
  [ctx command-key chat-id data]
  (misc/remove-current-step! ctx chat-id)
  (set-command! ctx chat-id command-key)

  #_(assert (= true (validation/validate-command command)) "wrong command")
  (let [command (command-key (:commands @ctx))
        message (:message command)
        steps (vector? command)]
    (cond
      message
      (tbot/send-message bot/mybot chat-id message)

      steps
      (steps/handle-current-step ctx command chat-id data))))

(defn parse-body [body]
  (json/read-value body json/keyword-keys-object-mapper))

(defn parse-command [commands user-text]
  (when (str/starts-with? user-text "/")
    (first (filterv #(= % (keyword (subs user-text 1)))
                    (keys commands)))))

(def CTX (atom {
                ;; 1234 {:CURRENT_STEP {} :DIALOG_DATA {} }
                }))

(defn in-dialog? [ctx id]
  (misc/get-current-step ctx id))

(defn current-command [ctx id]
  (get (:commands @ctx) (-> ctx deref (get id) :CURRENT_COMMAND)))

(defn process-message [ctx id telegram-data]
  (let [commands (:commands @ctx)
        command-key (parse-command commands telegram-data)]
    (if command-key
      (handle-command ctx command-key id telegram-data)
      (if (in-dialog? ctx id)
        (steps/handle-current-step
            ctx
            (current-command ctx id)
            id telegram-data)
        (send-message id "no such command")))))

(defn handle-callback [ctx id telegram-data]
  (tbot/answer-callback-query
    bot/mybot
    (:id telegram-data) {:text (:data telegram-data)})
  (process-message ctx id (:data telegram-data)))

(defn app [ctx]
  (assert (map? (:commands @ctx)) "expected commands to be a map")
  (println "server is starting1")
  (fn [req]
    (def r req)
    (let [body (parse-body (:body req))
          data (-> body :message :text)
          chat-id (-> body :message :chat :id)
          chat-id-from-callback (-> body :callback_query :message :chat :id)
          callback-data (-> body :callback_query)]
      (def b body)
      (if callback-data
        (handle-callback ctx chat-id-from-callback callback-data)
        (process-message ctx chat-id data))
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body   "ok"})))

(defonce SERVER (atom nil))

(defn prepare-commands [commands]
  (into {} (mapv (fn [[command-name command]]
                   [command-name (steps/add-ids command)]) commands)))

(defn start-bot [commands & [opts]]
  (let [port (:port opts)]
    (if @SERVER
      (do (@SERVER)
          (reset! SERVER nil)
          :down)
      (let [_ (reset! CTX {:commands (prepare-commands commands)})
            server (hk-server/run-server (app CTX)
                                         {:port (or port 8080)})]
        (when server
          (reset! SERVER server))
        :up))))

#_(start-bot example/bot-commands {})

(comment

  (def ngrok-url "https://106e-188-243-183-57.ngrok-free.app")
  (def token (System/getenv "BOT_TOKEN"))

  ;;https://api.telegram.org/bot{my_bot_token}/setWebhook?url={url_to_send_updates_to}
  (let [token token
        hook-url ngrok-url]
    [@(client/request {:url (str "https://api.telegram.org/bot" token "/deleteWebhook")
                       :query-params {:drop_pending_updates true}})

     @(client/request {:url (str "https://api.telegram.org/bot" token "/setWebhook")
                       :query-params {:url (str hook-url)}})]))
