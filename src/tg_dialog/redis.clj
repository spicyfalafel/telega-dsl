(ns tg-dialog.redis
  (:require [taoensso.carmine :as car :refer [wcar]]))

(defonce my-conn-pool (car/connection-pool {})) ; Create a new stateful pool
(def     my-conn-spec {:uri "redis://localhost:6379"})
(def     my-wcar-opts {:pool my-conn-pool, :spec my-conn-spec})

(wcar my-wcar-opts
      (car/ping)
      (car/set "myid" {:step "step1" :command "start"})
      (car/get "myid"))

