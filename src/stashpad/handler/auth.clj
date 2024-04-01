(ns stashpad.handler.auth
  (:require [stashpad.db.core :as db]
            [environ.core :refer [env]]
            [buddy.sign.jwt :as jwt]))

(def jwt-secret (env :jwt-secret))

(defn generate-jwt [userid]
  (jwt/sign {:userid userid} jwt-secret {:alg :hs256}))

(defn login-handler [request]
  (let [username (:username (:params request))
        password (:password (:params request))
        user (db/validate-user-credentials username password)]
    (if user
      (let [jwt-token (generate-jwt (:userid user))]
        {:status 200
         :headers {"Content-Type" "application/json"
                   "Set-Cookie" (str "Authorization=" jwt-token "; HttpOnly")}
         :body "Login Successful"})
      {:status 401 :headers {"Content-Type" "application/json"} :body "Invalid Credentials"})))

(defn register-handler [request]
  (println "Entire request:" request)
  (let [username (:username (:params request))
        password (:password (:params request))]
    (println "username: " username "password: password")
    (if (and username password) 
      (try
        (db/create-user username password)
        {:status 200 :headers {"Content-Type" "application/json"} :body "User Successfully Registered"}
        (catch Exception e
          {:status 500 :headers {"Content-Type" "application/json"} :body "An error occurred during registration"}))
      {:status 400 :headers {"Content-Type" "application/json"} :body "Missing username or password"})))