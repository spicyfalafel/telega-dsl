# DSL для телеграма

## Current design
Пример.
Сценарии:

1. Студент начинает регистрацию:
   - Бот приветствует студента и предлагает выбрать группу.
   - На любом шаге студент может нажать "Назад" и вернуться к предыдущему шагу, удаляя введенные данные.
   - Студент выбирает группу или решает не регистрироваться.
   - Если студент выбрал группу, бот просит ввести имя и фамилию.
   - Студент вводит имя и фамилию.
   Если формат верный и студент найден в базе данных, бот переходит к следующему шагу.
   - Бот предлагает рассказать о себе.
   - Если студент выбрал "Да", бот просит написать описание увлечений в IT.
   - Студент вводит описание. Если описание длиннее 50 символов, бот показывает его и предлагает отредактировать или подтвердить.
   - Если студент подтвердил описание, бот сохраняет данные студента и завершает регистрацию с успешным сообщением.
   - Если студент выбрал "Нет" на шаге о рассказе о себе, бот завершает регистрацию с успешным сообщением.
   - Если описание короче 50 символов, бот выводит сообщение об ошибке и просит ввести описание заново.

2. Студент не хочет регистрироваться:
   - Бот приветствует студента и предлагает выбрать группу.
   - Студент выбирает "Я не хочу регистрироваться".
   - Бот завершает регистрацию с сообщением "Регистрация пропущена".

```clojure
(ns telega-dsl-example
  (:require [malli.core :as m]
            [clojure.string :as str]))

(set! *warn-on-reflection* true)

(defn dbget [path]
  (println "Getting stub " path))

(defn dbdelete [path]
  (println "Deleting stub " path))

(defn student-exists? [name-s]
  (println "student exists stub" name-s))

(def name-schema
  [:and
   [:re {:error/message "Имя и фамилия должны содержать пробел."} #".+\s+.+"]
   [:fn
    {:error/fn (fn [_] "Имя и фамилия должны начинаться с заглавной буквы")}
    (fn [name-s]
      (let [[name surname] (str/split name-s #"\s+")]
        (and (Character/isUpperCase ^Character (first name))
             (Character/isUpperCase ^Character (first surname)))))]])

(defn validate-name [name-s]
  (if (m/validate name-schema name-s)
    (when-not (student-exists? name-s)
      {:error/message "Студент с таким именем и фамилией не найден в базе данных."})
    {:error/message "Неверный формат имени и фамилией."}))

(def bot-commands
  [{:command "/start"
    :steps
    [{:message "Добро пожаловать! Давайте начнем регистрацию."}
     {:id "ask-group"
      :message "Выберите вашу группу:"
      :menu (conj
             (for [group (dbget [:groups])]
               {:label (str "Группа " group) :save-as [:group] :value group})
             {:label "Я не хочу регистрироваться" :save-as [:skip-registration] :value true :end true})}

     {:id "ask-name"
      :message "Введите ваше имя и фамилию:"
      :validate validate-name
      :save-as [:name]
      :on-back {:action (fn [] (dbdelete [:name]))
                :message "Имя и фамилия удалены."}
      :back true}

     {:id "ask-bio"
      :message "Хотите рассказать о себе?"
      :menu [{:label "Да" :save-as [:bio?] :value true}
             {:label "Нет" :save-as [:bio?] :value false}]
      :on-back {:action (fn [] (dbdelete [:bio?]))
                :message "Информация о биографии удалена."}
      :back true}

     {:id "write-bio"
      :message "Расскажите о своих увлечениях в IT:"
      :validate (fn [bio]
                  (when (<= (count bio) 50)
                    {:error/message "Описание должно быть длиннее 50 символов."}))
      :save-as [:bio]
      :back true}

     {:id "confirm-bio"
      :save-as [:bio]
      :message (fn [] (if-let [bio (dbget [:bio])]
                        (str "Ваше описание: " bio)
                        "Введите ваше описание"))
      :menu [{:label "Готово"}]
      :back "ask-bio"}

     {:id "register-done"
      :message (fn [] (if (dbget [:skip-registration])
                        "Регистрация пропущена."
                        (if (dbget [:bio?])
                          "Регистрация пройдена успешно. Спасибо за описание!"
                          "Регистрация пройдена успешно.")))}]}

   {:command "/help"
    :message "Этот бот помогает студентам пройти регистрацию в системе обучения."}])
```

## Существующие DSL для Telegram-ботов
1. [aiogram_dialog](https://github.com/Tishka17/aiogram_dialog/). Библиотека поверх aiogram FSM - Python-библиотеки для Telegram API.

Пример на моем DSL:
```clojure
(ns poc.example-group
  (:require
   [poc.core :as poc]))

(def bot-commands
  [{:command "/start"
    :steps
    [{:id "start-register"
      :message "Добро пожаловать! Давайте начнем регистрацию."
      :menu [{:label "Next"}
             {:label "Я не хочу регистрироваться" :end true}]}

     {:id "ask-group"
      :message "Выберите вашу группу:"
      :menu [{:label "Group1" :save-as [:group] :value "group1"}
             {:label "Group2" :save-as [:group] :value "group2"}
             {:label "Group3" :save-as [:group] :value "group3"}]}

     {:id "register-done"
      :message (fn []
                 (str "Привет! Группа " (if-let [group (poc/dbget [:group])]
                                          group
                                          "не задана")))}]}])

(defn -main []
  (poc/start-bot bot-commands {:token (System/getenv "BOT_TOKEN")}))
```

Пример на aiogram_dialog:
```python
import asyncio
import logging
import os

from aiogram import Bot, Dispatcher
from aiogram.filters import CommandStart
from aiogram.fsm.state import State, StatesGroup
from aiogram.types import CallbackQuery, Message

from aiogram_dialog import (Dialog, DialogManager, StartMode, Window,
                            setup_dialogs)
from aiogram_dialog.widgets.kbd import Button, Next, SwitchTo
from aiogram_dialog.widgets.text import Const, Format


class Wizard(StatesGroup):
    start = State()
    group = State()
    end = State()


async def button1_clicked(
    callback: CallbackQuery, button: Button, manager: DialogManager
):
    manager.dialog_data["group"] = button.widget_id
    await manager.next()


async def cnl_edt_clicked(
    callback: CallbackQuery, button: Button, manager: DialogManager
):
    manager.dialog_data["group"] = "не задана"
    await manager.next()


dialog = Dialog(
    Window(
        Const("Добро пожаловать! Давайте начнем регистрацию"),
        Next(),
        SwitchTo(
            Const("Я не хочу регистрироваться"),
            id="cnl_edt",
            state=Wizard.end,
            on_click=cnl_edt_clicked,
        ),
        state=Wizard.start,
    ),
    Window(
        Const("Выберите вашу группу"),
        Button(Const("Group1"), id="Group1", on_click=button1_clicked),
        Button(Const("Group2"), id="Group2", on_click=button1_clicked),
        Button(Const("Group3"), id="Group3", on_click=button1_clicked),
        state=Wizard.group,
    ),
    Window(
        Format("Привет! группа {dialog_data[group]}"),
        state=Wizard.end,
    ),
)


async def start(message: Message, dialog_manager: DialogManager):
    await dialog_manager.start(Wizard.start, mode=StartMode.RESET_STACK)


async def main():
    # real main
    logging.basicConfig(level=logging.INFO)
    bot = Bot(token=os.getenv("BOT_TOKEN"))
    dp = Dispatcher()
    dp.include_router(dialog)
    dp.message.register(start, CommandStart())
    setup_dialogs(dp)

    await dp.start_polling(bot)


if __name__ == "__main__":
    asyncio.run(main())
```


## Проблема
При разработке диалогов в чат-ботах (телеграм), нужно:
  1) хранить стэйт пользователей, на каком шаге диалога пользователь
  2) в зависимости от этого стэйта при получении сообщения от пользователя переходить на нужный шаг.
  3) на каждом шагу нужно писать код для валидации
Для этого нужно больше времени, чем если бы был SDK для описания диалогов. Кода больше, больше ошибок.

При этом в случае команды попроще, например, /help, программировать легко - нужно отправить один ответ, всего один шаг, нет input'а от пользователя.

## Цель
Создать библиотеку для описания диалогов компактно с помощью DSL.
Диалоги разбиты на шаги, которые можно переиспользовать. Ввод данных пользователя легче валидировать.

## DSL в Clojure

DSL - предметно-ориентированный язык. Обычно встроенные в общий язык, реже бывают внешние. Основная задача - снизить сложность программирования, используя предметный язык, который транслируется в язык общего назначения.
В идеале, если бы на этом языке мог писать не программист, а человек той специальности, который разбирается в предметно-ориентированном языке.
DSL хорошо прижился и используется в querying languages: SQL, GraphQL
- Data Driven DSL - если всё можно представить данными, ими легче манипулировать. Данные можно передать в другую систему. На одни данные можно сделать несколько интерпретаторов.
- Traditional LISP DSL - функции, макросы. Менее гибкие, чем Data Driven DSL, сложнее дебажить, сложнее генерировать.

### Пример Data DSL (honeysql)

```clojure
(def sqlmap {:select [:a :b :c]
             :from   [:foo]
             :where  [:= :foo.a "baz"]})
(sql/format sqlmap) ;; => ["SELECT a, b, c FROM foo WHERE foo.a = ?" "baz"]
```

### Пример Traditional LISP DSL (overtone, создание музыки в REPL)
```clojure
user=> (demo (sin-osc))

;; or something more interesting...
user=> (demo 7 (lpf (mix (saw [50 (line 100 1600 5) 101 100.5]))
                    (lin-lin (lf-tri (line 2 20 5)) -1 1 400 4000)))
```

### Пример Data DSL (роутинг reitit)

```clojure
(def app
  (ring/ring-handler
    (ring/router
      ["/api"
       ["/math" {:get {:parameters {:query {:x int?, :y int?}}
                       :responses  {200 {:body {:total int?}}}
                       :handler    (fn [{{{:keys [x y]} :query} :parameters}]
                                     {:status 200
                                      :body   {:total (+ x y)}})}}]]
      ;; router data affecting all routes
      {:data {:middleware [wrap-cors ...]}})))

(app {:request-method :get
      :uri "/api/math"
      :query-params {:x "1", :y "2"}})
; {:status 200
;  :body {:total 3}}
```

### Пример Traditional LISP DSL (роутинг compojure)
```clojure
(defroutes app-routes
  (context "/products" []
    (POST "/" [body]
      (db/insert! body))

    (GET "/:id" [id]
      (db/find-by-id! id))))
```


## Возможный код
```clojure

(step "sendinfo"
  (db/get ::current-user))

(command
  "start"
  {:cancel-button true
   :back-button true
   :description "Команда для старта бота. Сохраняет имя."}
  (send "Привет, я бот вашего курса. Я помогу тебе с твоей работой. Как тебя зовут (как в реестре)?")
  (db/store-answer ::name)

  (send ask-group)

  (cond-answer
    #(#{"gr1" "gr2"} %)
    send-info
    :else
    (do (send "Группа не найдена")
        back))
  (db/store-answer ::group))

(command
  "help"
  (send "..."))

```

### Бот в стиле re-frame - фреймворка для FrontEnd на ClojureScript
У команд есть шаги (ивенты). Команды и ивенты возвращают мапы-эффекты. На входе указывают нужные коэффекты.

```clojure

;; регистрируем команду
(defmethod command :start
  ;; cofx       payload - то, что пришло от телеграма
  [{:keys [db]} {{:keys [id username] :as payload}}]
  {:db (assoc-in db [id :username] username)
   :tg/api
   {:text "Привет, я бот вашего курса. Как тебя зовут?"}
   ;; переход на ивент
   :dispatch {:id ::set-username :payload payload}})

(event ::set-username
;; возможные коэффекты
  [
   ;; (inject-cfx :sqlite)
   ;; (inject-cfx :env)
   ;; (inject-cfx :kafka)
   ;; ...
   ;; коэффект, который добавляет кнопку "назад"
   undoable]
   ;;              payload - то, что пришло от телеграма + от функции, которая вызвала
  (fn [{:keys [db]} {user-text :text id :id :as payload}]
    (if (not-empty user-text)
      {:db (assoc-in db [:user id :name] user-text)
       :dispatch {:id ::set-group :payload (assoc payload :student-name user-text)}}
      {:tg/api {:text "Пустое имя"}})))

(event ::set-group
  [undoable]
  (fn [{:keys [db]} {user-text :text id :id :as payload}]
    (if (get (db/get-groups) user-text)
      {:db (assoc-in db [:user id :group] user-text)
       :dispatch {:id ::finish-start :payload (assoc payload :student-group user-text)}}
      {:tg/api {:text "Группа не найдена"}})))

(event ::finish-start
  [undoable]
  (fn [_ {:keys [id student-group student-name]}]
    {:tg/api [{:text (format "Привет, %s!" student-name)}
              {:text (format "Имя: %s; Группа: %s; Telegram ID: %s"
                             student-name student-group id)} ]}))
```

## Литература
### DSL
- https://doi.org/10.1016/S0164-1212(00)00089-3
- https://doi.org/10.1093/bib/bbw130
- https://www.scitepress.org/papers/2010/29254/29254.pdf
- https://cyberleninka.ru/article/n/obzor-sovremennyh-sredstv-sozdaniya-i-podderzhki-predmetno-orientirovannyh-yazykov-programmirovaniya/viewer
- https://cyberleninka.ru/article/n/sozdanie-predmetno-orientirovannyh-yaykov/viewer
- https://cyberleninka.ru/article/n/ob-ispolzovanii-v-programmirovanii-problemno-orientirovannyh-yazykov/viewer
- https://cyberleninka.ru/article/n/razrabotka-yazyka-programmirovaniya-na-racket/viewer

- https://www.youtube.com/watch?v=j382BLptxCc
- https://www.youtube.com/playlist?list=PLJQ_tjFEDMB-C1Op_L91gMTCtz2Cepe-3

### Чат-боты
- http://dx.doi.org/10.1007/978-3-030-62522-1_15
- http://dx.doi.org/10.5381/jot.2019.18.2.a5


## Вопросы
1. Кложу используют три калеки. Как правильно обосновать необходимость дсл именно в лиспе?
2. Стоит ли поддерживать только телеграм?
