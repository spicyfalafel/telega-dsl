# DSL для телеграма

## Проблемы
todo
1.

## Vision


## Возможный код
```clojure

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
  (send (registered-commands)))

```


## Примеры

### Бот в стиле re-frame фреймворка
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
