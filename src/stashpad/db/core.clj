(ns stashpad.db.core
  (:require [clojure.java.jdbc :as jdbc]))

(def db-spec {:dbtype "sqlite" :dbname "stashpad.db"})

(defn initialize-db 
  "initializes the db" 
  [] 
  (jdbc/with-db-connection [conn db-spec]
    (jdbc/execute! conn
                   ["CREATE TABLE IF NOT EXISTS snippets (
          id TEXT PRIMARY KEY,
          content TEXT NOT NULL
        );"])))

(defn save-snippet
  "saves a new snippet to the database and returns its id"
  [snippet-content]
  (let [id (str (java.util.UUID/randomUUID))]
    (jdbc/with-db-connection [conn db-spec]
      (jdbc/execute! conn
      ["INSERT INTO snippets (id, content) VALUES (?, ?)"
       id snippet-content]))
  id))

(defn get-snippet
  "retrieves the content of a snippet by its id from the database"
  [id]
  (first (jdbc/query db-spec
                     ["SELECT content FROM snippets WHERE id = ?" id]
                     {:row-fn :content})))
