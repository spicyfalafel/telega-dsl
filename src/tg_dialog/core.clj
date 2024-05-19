(ns tg-dialog.core
  (:require
   [telegrambot-lib.core :as tbot]
   [clojure.string :as str]
   [jsonista.core :as json]
   [clojure.test :as test]
   [tg-dialog.commands :as commands]
   [org.httpkit.client :as client]
   [org.httpkit.server :as hk-server]
   [tg-dialog.tg :as tg]
   [tg-dialog.state :as state]
   [tg-dialog.handlers :as handlers]
   #_[taoensso.telemere :as t]))

(set! *warn-on-reflection* true)

(defonce CTX (atom {}))

(defonce SERVER (atom nil))

(defn- parse-body [body]
  (json/read-value body json/keyword-keys-object-mapper))

(defn- parse-command [commands user-text]
  (when (and user-text (str/starts-with? user-text "/"))
    (first (filterv #(= % (keyword (subs user-text 1)))
                    (keys commands)))))

(defn- in-dialog? [ctx id]
  (state/get-current-step-id ctx id))

(defn process-message [ctx id message]
  (let [commands (:commands @ctx)
        command-key (parse-command commands (:text message))]
    (if command-key
      (handlers/handle-command ctx command-key id (:text message))
      (if (in-dialog? ctx id)
        (handlers/handle-current-step
         ctx
         (state/get-current-command ctx id)
         id message)
        (tg/send-message ctx id "no such command")))))

(defn handle-callback [ctx id telegram-data]
  (tbot/answer-callback-query
   (:bot @ctx)
   (:id telegram-data) {:text "ok" #_(:data telegram-data)})
  (process-message ctx id {:text (:data telegram-data)}))

(defn app* [ctx update]
  (let [message (:message update)
        chat-id (-> update :message :chat :id)
        chat-id-from-callback (-> update :callback_query :message :chat :id)
        callback-data (-> update :callback_query)]
    (if callback-data
      (handle-callback ctx chat-id-from-callback callback-data)
      (process-message ctx chat-id message))))

(defn app [ctx]
  ;; (t/log! "Server is starting")
  (fn [req]
    (let [body (parse-body (:body req))]
      (app* ctx body)
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body   "ok"})))

(defn- poll-updates
  "Long poll for recent chat messages from Telegram."
  [bot offset timeout]
  (let [resp (tbot/get-updates bot {:offset offset
                                    :timeout timeout})]
    (if (contains? resp :error)
      (println (str "Get-updates error:" (:error resp)))
      #_(t/error! (str "Get-updates error:" (:error resp)))
      resp)))

(defonce update-id (atom nil))

(defn- set-id!
  "Sets the update id to process next as the the passed in `id`."
  [id]
  (reset! update-id id))

(defn- polling [ctx {:keys [poll-timeout sleep]}]
  (loop []
    ;; (t/log! "Checking for chat updates.")
    (let [updates (poll-updates (:bot @ctx) @update-id poll-timeout)
          messages (:result updates)]
      (doseq [msg messages]
        (app* ctx msg)

        ;; Increment the next update-id to process.
        (-> msg :update_id inc set-id!))

      ;; Wait a while before checking for updates again.
      (Thread/sleep ^long sleep))
    (recur)))

(defn- webhook [ctx {:keys [port url]}]
  (let [server (hk-server/run-server (app ctx) {:port (or port 8080)})]
    (when url
      (tbot/set-webhook (:bot @ctx) {:url url
                                     :content-type :multipart}))
    ;; (t/log! "Server ready!")
    (when server
      (reset! SERVER server))))

(defn start-bot [commands & [{:keys [type token url db] :as opts}]]
  (if @SERVER
    (do (@SERVER)
        (reset! SERVER nil)
        :down)
    (let [bot (tbot/create token)]
      (reset! CTX {:commands (commands/prepare-commands bot commands)
                   :bot bot
                   :dbtype (:type db)
                   :db (when db (state/get-db db))})
      @(client/request {:url (str "https://api.telegram.org/bot" token "/deleteWebhook")
                        :query-params {:drop_pending_updates true}})
      (if (= :polling type)
        (polling CTX opts)
        (webhook CTX opts))
      :up)))

(defn main [])


#_(start-bot tg-dialog.example.quiz/bot-commands-hardcoded
             {:type :polling
              :token (System/getenv "BOT_TOKEN")
              :poll-timeout 10
              :sleep 1000})

(def bot-commands
  {:whoami
   {:message (fn [ctx]
               (format "Name: %s\nLike bots: %s\nLanguage: %s"
                       (:name ctx) (:like_bots ctx) (:language ctx)))}
   :start
   [{:message "Hi there! What's your name?"
     :save-as [:name]}

    {:message "Do you like to write bots?"
     :save-as [:like_bots]
     :menu [{:label "Yes"}
            {:label "No" :-> :end}]
     :reply true
     :validate {:menu-values true}
     :error "I don't understand you :("}

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

#_(start-bot
  bot-commands
  {
   ;; :type :polling
   ;; :poll-timeout 10
   ;; :sleep 500
   :type :webhook
   :url "https://75de-188-243-183-57.ngrok-free.app"
   :port 8080
   :token (System/getenv "BOT_TOKEN")
   :db {:type :mongo
        :username "admin"
        :password "admin"
        :host "127.0.0.1"
        :db "telegram"}})


(comment


  (def me 202476208)
  (def ngrok-url "https://75de-188-243-183-57.ngrok-free.app")
  (def token "6964934897:AAHi1MnW7u8gr3iLPeb3BqOebB1x4ZXPmK8")

  ;;https://api.telegram.org/bot{my_bot_token}/setWebhook?url={url_to_send_updates_to}
  [@(client/request {:url (str "https://api.telegram.org/bot" token "/deleteWebhook")
                     :query-params {:drop_pending_updates true}})

   @(client/request {:url (str "https://api.telegram.org/bot" token "/setWebhook")
                     :query-params
                     {:url (str ngrok-url)
                      :allowed_updates ["message", "edited_channel_post", "callback_query"]}})

   @(client/request {:url (str "https://api.telegram.org/bot" token "/getWebhookInfo")})]


  )
