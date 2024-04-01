(ns stashpad.core
  (:require [ring.adapter.jetty :as jetty]
            [stashpad.routes :as routes]
            [stashpad.db.core :as db]))

(defn -main []
  (db/initialize-db)
  (jetty/run-jetty routes/app {:port 3000 :join? false}))