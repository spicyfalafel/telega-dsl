(ns tg-dialog.db.mongo
  (:require
   [clojure.string :as str]
   [monger.collection :as mc]
   [monger.core :as mg]
   [monger.credentials :as mcr]
   [monger.operators :refer :all]))

(def collection "tg_dialog")

(defn connect [{:keys [host db username password]}]
  (let [cred (mcr/create username "admin" (.toCharArray password))]
    (mg/connect-with-credentials host cred)))

(defn get-db [{:keys [db] :as db-spec}]
  (let [conn (connect db-spec)]
    (mg/get-db conn db)))

(defn get-user [ctx id]
  (mc/find-one-as-map
    (:db @ctx)
    collection {:id id}))

(defn set-command! [ctx id command-key]
  (mc/update (:db @ctx) collection
             {:id id}
             {$set {:CURRENT_COMMAND command-key}}
             {:upsert true}))

(defn get-current-command [ctx id]
  (let [command
        (keyword (:CURRENT_COMMAND
                   (mc/find-one-as-map
                     (:db @ctx) collection
                     {:id id})))]
    (get (:commands @ctx) command)))

(defn get-current-step-id [ctx id]
  (:CURRENT_STEP (get-user ctx id)))

(defn change-current-step! [ctx id step-id]
  (mc/update (:db @ctx) collection
             {:id id}
             {$set {:CURRENT_STEP step-id}}
             {:upsert true}))

(defn remove-current-step! [ctx id]
  (mc/update (:db @ctx) collection
             {:id id}
             {$unset {:CURRENT_STEP ""}}
             {:upsert true}))


(defn get-dialog-data [ctx id]
  (:DIALOG_DATA (get-user ctx id)))

(defn add-dialog-data! [ctx id k data]
  (when k
    (let [data (cond-> data (some? (parse-boolean data)) (parse-boolean))]
      (mc/update (:db @ctx) collection
                 {:id id}
                 {$set {(str "DIALOG_DATA." (str/join "." (mapv name k))) data}}
                 {:upsert true}))))

(defn change-dialog-data! [ctx id data]
  (mc/update (:db @ctx) collection
             {:id id}
             {$set {:DIALOG_DATA data}}
             {:upsert true}))
