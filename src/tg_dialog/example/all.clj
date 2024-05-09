(ns tg-dialog.example.all
  (:require
    [tg-dialog.example.aiogram-example :as aiogram]
    [tg-dialog.example.example-group :as group]
    [tg-dialog.example.telega-dsl-example :as telega-dsl]
    [tg-dialog.example.quiz :as quiz]))

(def quiz quiz/bot-commands-generated)
(def aiogram aiogram/bot-commands)
(def group group/bot-commands)
(def telega-dsl telega-dsl/bot-commands)
