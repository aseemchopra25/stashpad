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
            [stashpad.db.core :as db]))

(def snippets
  "def defines a new symbol called 'snippets' in the namespace, 
   atom creates an atom with an initial value of an empty map,
   atoms provide a way to manage state that can be safely mutated from different threads essentially preventing race conditions"

  (atom {})) 


(defn save-snippet
  "defn defines a new function that takes a single argument snippet"
  [snippet-content]
  (db/save-snippet snippet-content)

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


(defn submit-snippet
  "defines a function to handle form submissions
  request is the incoming http request from the user"
  [request]

  ; extracts submitted form parameters into params-map
  (let [params-map (:params request)

        ; retrieves :snippet's key's value from this map 
        snippet-from-direct-access (:snippet params-map)
        id (save-snippet snippet-from-direct-access)]
    (str "Saved snippet with ID: " id " <a href='/snippets/" id "'>View</a>")))


; defroutes is a macro from Compojure that defines URL routes for my web application
(defroutes app-routes
  
  "defines a route for the http get method on the root path. it renders an html form
  defines a route for the http post method on /submit which handles form submissions using submit-snippet function"

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
  "defines the web application with middleware
   -> is a thread first macro that passes the result of each expression as the first argument to the next expression
   wrap-defaults applies default middleware settings for common web-application needs
   wrap-params parses request parameters"
  (-> (wrap-defaults app-routes site-defaults)
      (wrap-params)))


(defn -main 
  "defines the main entry point of the application,
  initialises database, 
  jetty web server is started to serve app on port 3000, 
  join? false means the current thread won't block on the server thread,"
  []
  (db/initialize-db) 
  (jetty/run-jetty app {:port 3000 :join? false}))