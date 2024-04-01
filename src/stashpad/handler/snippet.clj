(ns stashpad.handler.snippet
  (:require [stashpad.db.core :as db]
            [buddy.sign.jwt :as jwt]
            [environ.core :refer [env]]
            [clojure.string :as str]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(def jwt-secret (env :jwt-secret))

(defn submit-snippet
  [request]
  (let [params-map (:params request)
        snippet-from-direct-access (:snippet params-map)
        ; Extract the JWT token from the Authorization header, assuming it's formatted as "Bearer <token>"
        token (-> request :headers :authorization
                  (str/split #" ")
                  (second)
                  ; The split function returns a vector, where the second element is the token
                  )
        ; Use jwt/unsign to verify the token and extract the payload, which includes the userid
        payload (try
                  (jwt/unsign token jwt-secret {:alg :hs256})
                  (catch Exception e
                    nil))
        userid (:userid payload)
        ; Proceed with saving the snippet if userid is available
        id (when userid (db/save-snippet snippet-from-direct-access userid))]
    (if id
      (str "Saved snippet with ID: " id " <a href='/snippets/" id "'>View</a>")
      "Unauthorized: You must be logged in to submit snippets.")))

(defn get-snippet
  [id]
  (db/get-snippet id))

(defn render-home
  []
  (let [csrf-token (anti-forgery-field)]
    (str "<!DOCTYPE html><html><head><title>Login/Register</title></head><body>"
         "<h2>Login</h2><form action='/login' method='POST'>" csrf-token
         "Username: <input type='text' name='username'/><br>"
         "Password: <input type='password' name='password'/><br>"
         "<input type='submit' value='Login'/></form>"
         "<h2>Register</h2><form action='/register' method='POST'>" csrf-token
         "Username: <input type='text' name='username'/><br>"
         "Password: <input type='password' name='password'/><br>"
         "<input type='submit' value='Register'/></form>"
         "</body></html>")))