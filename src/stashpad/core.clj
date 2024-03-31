(ns stashpad.core
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.params :refer [wrap-params]]))

(def snippets (atom {})) ;; atoms would prevent race conditions

(defn save-snippet [snippet]
  (let [id (str (java.util.UUID/randomUUID))]
    (swap! snippets assoc id snippet)
    (println "Saved snippet with ID:" id "Content:" snippet) ;; content keeps showing nil
    id))

(defn get-snippet [id]
  (@snippets id))


(defn submit-snippet [request]
  (let [params-map (:params request)
        snippet-from-direct-access (:snippet params-map)]
    (println "Params Map:" params-map)
    (println "Snippet from Direct Access:" snippet-from-direct-access)
    (let [id (save-snippet snippet-from-direct-access)]
      (println "Before response, Snippet:" snippet-from-direct-access) ;; Confirm it's still not nil.
      (str "Saved snippet with ID: " id " <a href='/snippets/" id "'>View</a>"))))



(defroutes app-routes
  (GET "/" request
    (let [token (:anti-forgery-token request)]
      (str "<form method='POST' action='/submit'>"
           "<input type='hidden' name='__anti-forgery-token' value='" token "'/>"
           "<input type='text' name='snippet'/>"
           "<input type='submit' value='Submit'/>"
           "</form>")))
  (POST "/submit" request (submit-snippet request))
  (GET "/snippets/:id" [id] (str "<h2>Snippet:</h2><p>" (get-snippet id) "</p>"))
  (route/not-found "Not Found"))

(def app
  (-> (wrap-defaults app-routes site-defaults)
      (wrap-params)))

(defn -main []
  (jetty/run-jetty app {:port 3000 :join? false}))