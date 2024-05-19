(ns tg-dialog.validation
  (:require
   [malli.core :as m]
   [malli.error :as me]
   [malli.dev.pretty :as pretty])
  (:import
   [clojure.lang ArityException]))

(def label-schema
  (m/schema [:map {:closed true}
             [:label :string]
             [:save-as
              {:optional true}
              [:vector :keyword]]
             [:value
              {:optional true}
              :any]
             [:->
              {:optional true}
              [:or [:string]
               [:enum :end]]]]))

(defn validate-label [label]
  (m/validate (m/schema label-schema) label))

(def truefalse-schema
  (m/schema [:enum
             {:error/message "should be: true|false"}
             true false]))

(def step-schema
  (m/schema
   [:map
    {:closed true}
    [:id {:optional true} :string]

    [:message [:or :string fn?]]

    [:menu
     {:optional true}
     [:vector
      {:min 1
       :error/message "Menu must contain at least one option"}
      #'label-schema]]

    [:error
     {:optional true
      :error/message "shuld be string"}
     :string]

    [:reply
     {:optional true}
     #'truefalse-schema]

    [:back
     {:optional true}
     #'truefalse-schema]

    [:when
     {:optional true}
     fn?]

    [:validate
     {:optional true}
     [:or
      fn?
      [:map
       [:menu-values #'truefalse-schema]]
      [:and
       [:any]
       [:fn (fn [x] (= java.util.regex.Pattern (type x)))]]]]

    [:-> {:optional true}
     [:or [:string]
      [:enum :end]]]

    [:save-as
     {:optional true}
     [:vector {:min 1
               :error/message "should be keyword vector with at least one item"}
      :keyword]]]))

(defn validate-step [command]
  (m/validate (m/schema step-schema)
              command))

(def message-command
  (m/schema
   [:map
    {:closed true
     :error/message "message key is required"}
    [:message [:or :string fn?]]]))

(def command-schema
  (m/schema
   [:vector {:min 1
             :error/message "Command must contain at least one step"}
    #'step-schema]))

(defn validate-command [command]
  (m/validate command-schema command))

(defn menu-options [menu]
  (into #{} (mapv
             #(or (:value %) (:label %))
             menu)))

(defn validate-commands [commands]
  (remove
    (fn [c] (or (empty? c)
                (and (map? c) (every? empty? (vals c)))))
    (if (map? commands)
      (mapv
        (fn [[command-name steps]]
          (cond
            (not (keyword? command-name))
            {command-name ["should be keyword"]}

            (vector? steps)
            {command-name (vec (remove nil? (-> (m/schema #'command-schema)
                                                (m/explain steps)
                                                (me/humanize))))}
            :else
            (vec (remove nil? (-> (m/schema #'message-command)
                                  (m/explain steps)
                                  (me/humanize))))))
        commands)
      "Commands should be a map")))

(defmulti user-validate-step
  (fn [step _]
    (type (:validate step))))

(defmethod user-validate-step
  :default
  [_step _text]
  :ok)

(defmethod user-validate-step
  java.util.regex.Pattern
  [step text]
  (re-matches (:validate step) text))

(defmethod user-validate-step
  clojure.lang.AFunction
  [step text]
  ((:validate step) text))

(defmethod user-validate-step
  clojure.lang.IPersistentMap
  [{{menu-values :menu-values} :validate menu :menu :as _step} text]
  (cond
    ;; check if text is one of menu optiosn
    (and menu-values ((menu-options menu) text))
    true

    menu-values
    false

    :else true))

(defmethod user-validate-step
  clojure.lang.PersistentVector
  [{malli-vector :validate} text]
  (if-let [errors (me/humanize (m/explain (m/schema malli-vector) text))]
    (first errors)
    true))

(defn check-malli-schemas [commands]
  (mapv
   (fn [[_ steps]]
     (when (vector? steps)
       (mapv
        (fn [step]
            ;; malli
          (when (vector? (:validate step))
            (try
              (me/humanize (m/explain (m/schema (:validate step)) "check"))
              (catch ArityException _
                (throw (Exception. (str "Wrong malli schema " (:validate step))))))))

        steps))) commands))

(comment
  #_[:fn {:error/message "steps & message are mutually exclusive"
          :error/path [:message]}
     (fn [{:keys [message steps]}]
       (not (and message steps)))])
