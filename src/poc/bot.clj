(ns poc.bot
  (:require
    [telegrambot-lib.core :as tbot]
    [malli.core :as m]
    [clojure.string :as str]
    [malli.generator :as mg]))

(def mybot (tbot/create))
