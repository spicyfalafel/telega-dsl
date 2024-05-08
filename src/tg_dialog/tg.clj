(ns tg-dialog.tg
  (:require [telegrambot-lib.core :as tbot]))

(defn send-message
  ([ctx chat-id message]
   (tbot/send-message (if (:bot @ctx)
                        (:bot @ctx)
                        (tbot/create))
                      chat-id message))
  ([ctx chat-id message params]
   (tbot/send-message (if (:bot @ctx)
                        (:bot @ctx)
                        (tbot/create))
                      chat-id message params)))
