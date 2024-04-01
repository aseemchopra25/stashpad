(ns stashpad.routes
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [stashpad.handler.auth :as auth]
            [stashpad.handler.snippet :as snippet]))

(defroutes app-routes
  (GET "/" [] (snippet/render-home))
  (POST "/submit" request (snippet/submit-snippet request))
  (GET "/snippets/:id" [id] (snippet/get-snippet id))
  (POST "/login" request (auth/login-handler request))
  (POST "/register" request (auth/register-handler request))
  (route/not-found "Not Found"))

;; (defn wrap-debug [handler]
;;   (fn [req]
;;     (println "Request headers:" (:headers req))
;;     (println "Query params:" (:query-params req))
;;     (println "Form params:" (:form-params req))
;;     (println "Entire Request: " req)
;;     (handler req)))

(def app
  "defines the web application with middleware
   -> is a thread first macro that passes the result of each expression as the first argument to the next expression
   wrap-defaults applies default middleware settings for common web-application needs
   wrap-params parses request parameters"
  (-> (wrap-defaults app-routes site-defaults)
      (wrap-params)
      (wrap-json-body {:types ["application/json"]})
      (wrap-json-response)))