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

(defn set-command! [ctx id command-key]
  (swap! ctx (fn [m] (assoc-in m [id :CURRENT_COMMAND] command-key))))

(defn handle-command
  [ctx command-key chat-id data]
  (set-command! ctx chat-id command-key)
  (steps/change-current-step!
    ctx chat-id
    (first (command-key (:commands @ctx))))
  (println "after changing step " (get @ctx 202476208) )

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
  (println "!!!!!!!" (-> ctx deref (get id) :CURRENT_COMMAND))
  (println "!!!!!!!!!!!!!!!!!!!!!!!!"
           (get (:commands @ctx) (-> ctx deref (get id) :CURRENT_COMMAND)))

  (get (:commands @ctx) (-> ctx deref (get id) :CURRENT_COMMAND))
  )

;; (defn current-step [ctx id]
;;   (let [commands (:commands @ctx)] (get commands (current-command ctx commands))))

(defn process-message [ctx id telegram-data]
  (let [commands (:commands @ctx)
        command-key (parse-command commands telegram-data)]
    (if command-key
      (handle-command ctx command-key id telegram-data)
      (if (in-dialog? ctx id)
        (steps/handle-current-step
          ctx
          (current-command ctx commands)
          id telegram-data)
        (tbot/send-message bot/mybot id "no such command")))))

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
          id (or (-> body :message :chat :id)
                 (-> body :callback_query :message :chat :id))
          callback-data (-> body :callback_query)]
      (def b body)
      (if callback-data
        (handle-callback ctx id callback-data)
        (process-message ctx id data))
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body   "ok"})))

(defonce SERVER (atom nil))

(defn configure-commands [commands]
  (into {} (mapv (fn [[command-name command]]
                   [command-name (steps/add-ids command)]) commands)))

(defn start-bot [commands & [opts]]
  (let [port (:port opts)]
    (if @SERVER
      (do (@SERVER)
          (reset! SERVER nil)
          :down)
      (let [_ (reset! CTX {:commands (configure-commands commands)})
            server (hk-server/run-server (app CTX)
                                         {:port (or port 8080)})]
        (when server
          (reset! SERVER server))
        :up))))

(start-bot example/bot-commands {})

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
