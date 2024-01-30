# DSL для телеграма

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
