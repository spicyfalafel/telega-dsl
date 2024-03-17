(ns poc.core
  (:require
    [telegrambot-lib.core :as tbot]
    [malli.core :as m]
    [clojure.string :as str]
    [jsonista.core :as json]
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

(defonce update-id (atom nil))

(defn set-id!
  "Sets the update id to process next as the the passed in `id`."
  [id]
  (reset! update-id id))

(defn poll-updates
  "Long poll for recent chat messages from Telegram."
  ([bot]
   (poll-updates bot nil))

  ([bot offset]
   (let [resp (tbot/get-updates bot {:offset offset
                                     :timeout (:timeout config)})]
     (if (contains? resp :error)
       (println "tbot/get-updates error:" (:error resp))
       resp))))

(defn app
  "Retrieve and process chat messages."
  [bot]
  (println "bot service started.")

  (loop []
    (println "checking for chat updates.")
    (let [updates (poll-updates bot @update-id)
          messages (:result updates)]

      ;; Check all messages, if any, for commands/keywords.
      (doseq [msg messages]
        ;(some-handle-msg-fn bot msg) ; your fn that decides what to do with each message.

        ;; Increment the next update-id to process.
        (-> msg
            :update_id
            inc
            set-id!))

      ;; Wait a while before checking for updates again.
      (Thread/sleep (:sleep config)))
    (recur)))

(defn start-bot [commands & [_opts]]
  (hk-server/run-server app {:port 8080}))


#_(defmacro menu [items]
  `(for [item# ~items]
     {:label (:label item#)
      :on-select (fn [] (dbsave (:save-as item#) (:value item#)))}))

;; (defn app [req]
;;   (def r req)
;;   (def body (json/read-value (:body r) json/keyword-keys-object-mapper))
;;   (def data (-> body :message :text))
;;   (def id (-> body :message :chat :id))
;;   (println "\nid " id " data " data "\n")
;;   (tbot/send-message bot/mybot id data)
;;   {:status  200
;;    :headers {"Content-Type" "text/html"}
;;    :body   "ok" })
;;
;;
;;
;; (my-server)
;; (def my-server (hk-server/run-server app {:port 8080}))
;;
(comment

  (def ngrok-url "https://6779-188-243-183-57.ngrok-free.app")
  (def token "")

  ;;https://api.telegram.org/bot{my_bot_token}/setWebhook?url={url_to_send_updates_to}
  (let [token token
        hook-url ngrok-url]
    [@(client/request {:url (str "https://api.telegram.org/bot" token "/deleteWebhook")
                        :query-params {:drop_pending_updates true}})

     #_@(client/request {:url (str "https://api.telegram.org/bot" token "/setWebhook")
                       :query-params {:url (str hook-url)}})])

  )
