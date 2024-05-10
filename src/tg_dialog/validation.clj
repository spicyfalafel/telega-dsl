(ns tg-dialog.validation
  (:require
   [malli.core :as m]
   [malli.error :as me])
  (:import
   [clojure.lang ArityException]))

(def label-schema
  [:map {:closed true}
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
     [:enum :end]]]])

(defn validate-label [label]
  (m/validate (m/schema label-schema) label))

(def step-schema
  [:map
   [:id {:optional true} :string]
   [:message [:or :string fn?]]
   [:menu {:optional true} [:vector #'label-schema]]])

(defn validate-step [command]
  (m/validate (m/schema step-schema)
              command))

(def command-schema
  (m/schema
   [:or
    [:vector {:min 1} #'step-schema]
    [:map
     {:closed true}
     [:message [:or :string fn?]]]]))

(defn validate-command [command]
  (m/validate command-schema command))

(defn menu-options [menu]
  (into #{} (mapv
             #(or (:value %) (:label %))
             menu)))

(defn validate-commands [commands]
  (m/explain (m/schema [:map
                        [::m/default [:map-of :keyword #'command-schema]]]) commands))

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
