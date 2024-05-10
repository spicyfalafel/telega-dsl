(ns tg-dialog.validation
  (:require
   [malli.core :as m]))

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

(defn validate-commands [commands]
  (m/validate (m/schema [:map
                         [::m/default [:map-of :keyword #'command-schema]]]) commands))

(comment
  #_[:fn {:error/message "steps & message are mutually exclusive"
          :error/path [:message]}
     (fn [{:keys [message steps]}]
       (not (and message steps)))])
