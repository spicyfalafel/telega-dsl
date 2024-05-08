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
   [taoensso.telemere :as t]
   #_[malli.generator :as mg]))

(set! *warn-on-reflection* true)

(def send-message (partial tbot/send-message bot/mybot))

(defn set-command! [ctx id command-key]
  (swap! ctx (fn [m] (assoc-in m [id :CURRENT_COMMAND] command-key))))

(defn handle-command
  [ctx command-key chat-id data]
  (misc/remove-current-step! ctx chat-id)
  (misc/remove-data-paths! ctx chat-id command-key)
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
  (when (and user-text (str/starts-with? user-text "/"))
    (first (filterv #(= % (keyword (subs user-text 1)))
                    (keys commands)))))

(def CTX (atom {;; 1234 {:CURRENT_STEP {} :DIALOG_DATA {} }
                }))

(defn in-dialog? [ctx id]
  (misc/get-current-step ctx id))

(defn current-command [ctx id]
  (get (:commands @ctx) (-> ctx deref (get id) :CURRENT_COMMAND)))

(defn process-message [ctx id message]
  (let [commands (:commands @ctx)
        command-key (parse-command commands (:text message))]
    (if command-key
      (handle-command ctx command-key id (:text message))
      (if (in-dialog? ctx id)
        (steps/handle-current-step
         ctx
         (current-command ctx id)
         id message)
        (send-message id "no such command")))))

(defn handle-callback [ctx id telegram-data]
  (tbot/answer-callback-query
   bot/mybot
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
  (t/log! "Server is starting")
  (fn [req]
    (let [body (parse-body (:body req))]
      (app* ctx body)
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body   "ok"})))

(defonce SERVER (atom nil))

(defn add-menu-values [step]
  (cond-> step

    (:menu step)
    (update
     :menu
     (fn [menu]
       (mapv
        (fn [menu-item]
          (if (some? (:value menu-item))
            (update menu-item :value str)
            (assoc menu-item :value (:label menu-item)))) menu)))))

(defn add-back-button [steps step]
  (if (:back step)
    (update step :menu conj
            {:label "Back"
             :-> (:id (steps/prev-step-by-index steps step))})
    step))

(defn prepare-steps [steps]
  (if (vector? steps)
    (mapv (comp add-menu-values (partial add-back-button steps)) steps)
    steps))

(defn prepare-commands [commands]
  (->> commands
       (mapv (fn [[command-name command]]
               [command-name (-> command
                                 (steps/add-ids)
                                 (add-menu-values)
                                 (prepare-steps))]))
       (into {})))

(defn poll-updates
  "Long poll for recent chat messages from Telegram."
  [bot offset timeout]
  (let [resp (tbot/get-updates bot {:offset offset
                                    :timeout timeout})]
    (println "resp " resp)
    (if (contains? resp :error)
      (println "error")
      ;; (t/error! (str "get-updates error:" (:error resp)))
      resp)))

(defonce update-id (atom nil))

(defn set-id!
  "Sets the update id to process next as the the passed in `id`."
  [id]
  (reset! update-id id))

(def BOT (atom nil))

(defn polling [ctx bot {:keys [poll-timeout sleep]}]
  (loop []
    ;; (t/log! "checking for chat updates.")
    (let [updates (poll-updates bot @update-id poll-timeout)
          messages (:result updates)]

      ;; Check all messages, if any, for commands/keywords.
      (doseq [msg messages]
        (app* ctx msg)

        ;; Increment the next update-id to process.
        (-> msg
            :update_id
            inc
            set-id!))

      ;; Wait a while before checking for updates again.
      (Thread/sleep sleep))
    (recur)))

(defn webhook [ctx bot {:keys [port url]}]
  (let [server (hk-server/run-server (app ctx) {:port (or port 8080)})]
    (tbot/set-webhook bot {:url url
                           :content-type :multipart})
    (t/log! "Server ready")
    (when server
      (reset! SERVER server))))

(defn start-bot [commands & [{:keys [type token] :as opts}]]
  (if @SERVER
    (do (@SERVER)
        (reset! SERVER nil)
        :down)
    (let [_ (reset! CTX {:commands (prepare-commands commands)})
          _ (reset! BOT (tbot/create token))]
      (tbot/delete-webhook @BOT)
      (if (= :polling type)
        (polling CTX @BOT opts)
        (webhook CTX @BOT opts))
      :up)))

#_(start-bot example/bot-commands {})

#_(start-bot example/bot-commands {:type :webhook
                                   :url "https://f3c4-188-243-183-57.ngrok-free.app"
                                   :token (System/getenv "BOT_TOKEN")
                                   :port 8080})

#_(start-bot example/bot-commands {:type :polling
                                   :token (System/getenv "BOT_TOKEN")
                                   :poll-timeout 10
                                   :sleep 1000})
(comment

  (def me 202476208)
  (def ngrok-url "https://f3c4-188-243-183-57.ngrok-free.app")
  (def token (System/getenv "BOT_TOKEN"))

  ;;https://api.telegram.org/bot{my_bot_token}/setWebhook?url={url_to_send_updates_to}
  [@(client/request {:url (str "https://api.telegram.org/bot" token "/deleteWebhook")
                     :query-params {:drop_pending_updates true}})

   @(client/request {:url (str "https://api.telegram.org/bot" token "/setWebhook")
                     :query-params
                     {:url (str ngrok-url)
                      :allowed_updates []}})

   @(client/request {:url (str  "https://api.telegram.org/bot" token "/getWebhookInfo")})])
