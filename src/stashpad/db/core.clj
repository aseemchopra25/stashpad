; ns is a macro that declares the namespace for a file, namespaces
; are a way to organize and group functions and prevent naming conflicts
(ns stashpad.db.core
  ; jdbc is java database connectivity
  (:require [clojure.java.jdbc :as jdbc]))

(def db-spec 
  "creates a global variable db spec which is a map;
   it has two keys dbtype and dbname"
  {:dbtype "sqlite" :dbname "stashpad.db"})

(defn initialize-db 
  "initializes the db;
   defn is a macro that defines the new function named initialize-db
   [] is a parameter vector for the function, as this function takes no arguments" 
  [] 
  ; this is a macro that sets up the db connection and assigns it to the symbol conn;
  ; [conn db-spec] is a binding vector that takes the database specification from db-spec and creates a connection object
  ; which is referred by the symbol conn within the body of the macro
  (jdbc/with-db-connection [conn db-spec] 
    ; jdbc/execute! runs the sql command provided to it on the database connection conn
    (jdbc/execute! conn
                   ; this command tells the database to create a new table called snippets
                   ; with two columns id and content if it already doesn't exist
                   ["CREATE TABLE IF NOT EXISTS snippets (
          id TEXT PRIMARY KEY,
          content TEXT NOT NULL
        );"])))

(defn save-snippet
  "saves a new snippet to the database and returns its id"
  [snippet-content]
  ; let is a special form that binds values to symbols within the local scope
  ; here it creates a local symbol id and binds it to a string representation of a random UUID
  (let [id (str (java.util.UUID/randomUUID))]
    (jdbc/with-db-connection [conn db-spec]
      (jdbc/execute! conn
      ["INSERT INTO snippets (id, content) VALUES (?, ?)"
       id snippet-content])) 
    ; id is the return value of this function
  id))

(defn get-snippet
  "retrieves the content of a snippet by its id from the database"
  [id]
  ; first is a clojure function tha returns the first item from a collection
  (first (jdbc/query db-spec
                     ; clojure replaces the ? with the value of id when it runs the command
                     ["SELECT content FROM snippets WHERE id = ?" id] 
                     {:row-fn :content})))
