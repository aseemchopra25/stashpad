; ns declares the napespace for this file
; stashpad.core is the name of this namespace
; this organises my code and makes these functions acccessible under this namespace
(ns stashpad.core
  
  ; :require brings other namespaces in the the current namespace, making their functions or macros avilable for use
  ; :as gives an aslias to the imported namespaces for convenience
  ; :refer imports specific functions or macros directly into the current namespace so you don't need to prefix them with the namespace
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [stashpad.db.core :as db]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [buddy.sign.jwt :as jwt]
            [environ.core :refer [env]]
            [clojure.string :as str]))

;; (def snippets
;; removed as sqlite is being used now
;;   "def defines a new symbol called 'snippets' in the namespace, 
;;    atom creates an atom with an initial value of an empty map,
;;    atoms provide a way to manage state that can be safely mutated from different threads essentially preventing race conditions"

;;   (atom {})) 


(defn save-snippet
  "defn defines a new function that takes a single argument snippet"
  [snippet-content userid]
  (db/save-snippet snippet-content userid)

  ; let binds the result of str to the symbol id within the scope of the let block
  ; it generates a unique identifier for each snippet
  ;; (let [id (str (java.util.UUID/randomUUID))]

    ; swap! applies a function to the value of the atom and updates the atom with the return value of the function 
    ; here it associates the unique id with the snippet in the snippet atom
    ;; (swap! snippets assoc id snippet)

    ; outputs a message to the console, useful for debugging
    ;; (println "Saved snippet with ID:" id "Content:" snippet) id)
)


(defn get-snippet
  "retrieves a snippet by id from the snippets atom
   @ is a deref operator used to get the current value of the atom"
  [id]
  ;; (@snippets id)
  (db/get-snippet id))

(def jwt-secret (env :jwt-secret))

(defn submit-snippet
  "Handles form submissions for snippets, associating them with a user"
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



(defn generate-jwt [userid]
  (let [jwt-secret (env :jwt-secret)]
    (jwt/sign {:userid userid} jwt-secret {:alg :hs256})))

(defn login-handler [request]
  (let [username (:username (:params request))
        password (:password (:params request))
        user (db/validate-user-credentials username password)]
    (if user
      (let [jwt-token (generate-jwt (:userid user))]
        ;; Removed println statement that logs JWT token
        {:status 200
         :headers {"Content-Type" "application/json"
                   "Set-Cookie" (str "Authorization=" jwt-token "; HttpOnly")}
         :body "Login Successful"}) ;; Changed response body to not include JWT
      {:status 401
       :headers {"Content-Type" "application/json"}
       :body "Invalid Credentials"})))

(defn register-handler [request] 
  (let [username (:username (:params request))
        password (:password (:params request))] 
    (if (and username password)
      (try
        (db/create-user username password)
        {:status 200 :headers {"Content-Type" "application/json"} :body "User Successfully Registered"}
        (catch Exception e
          {:status 500 :headers {"Content-Type" "application/json"} :body "An error occurred during registration"}))
      {:status 400 :headers {"Content-Type" "application/json"} :body "Missing username or password"})))


; defroutes is a macro from Compojure that defines URL routes for my web application
(defroutes app-routes
  
  "defines a route for the http get method on the root path. it renders an html form
  defines a route for the http post method on /submit which handles form submissions using submit-snippet function"

  (GET "/" []
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
  
  (POST "/submit" request (submit-snippet request)) 
  (GET "/snippets/:id" [id] (str "<h2>Snippet:</h2><p>" (get-snippet id) "</p>")) 
  (POST "/login" req (login-handler req)) 
  (POST "/register" req (register-handler req)) 
  (route/not-found "Not Found"))

(def app
  "defines the web application with middleware
   -> is a thread first macro that passes the result of each expression as the first argument to the next expression
   wrap-defaults applies default middleware settings for common web-application needs
   wrap-params parses request parameters"
  (-> (wrap-defaults app-routes site-defaults)
       (wrap-params)
       (wrap-json-body {:types ["application/json"]})
       (wrap-json-response)))



(defn -main 
  "defines the main entry point of the application,
  initialises database, 
  jetty web server is started to serve app on port 3000, 
  join? false means the current thread won't block on the server thread,"
  []
  (db/initialize-db) 
  (jetty/run-jetty app {:port 3000 :join? false}))