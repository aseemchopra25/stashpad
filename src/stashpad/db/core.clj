(ns stashpad.db.core
  (:require [clojure.java.jdbc :as jdbc]
            [buddy.hashers :as hashers]))

(def db-spec
  {:dbtype "sqlite" :dbname "stashpad.db"})

(defn initialize-db []
  (jdbc/with-db-connection [conn db-spec]
    (jdbc/execute! conn ["CREATE TABLE IF NOT EXISTS snippets (id TEXT PRIMARY KEY, content TEXT NOT NULL, userid TEXT, FOREIGN KEY(userid) REFERENCES users(userid));"])
    (jdbc/execute! conn ["CREATE TABLE IF NOT EXISTS users (userid TEXT PRIMARY KEY, username TEXT UNIQUE, password_hash TEXT NOT NULL);"])))

(defn save-snippet
  [snippet-content userid]
  (let [id (str (java.util.UUID/randomUUID))]
    (jdbc/with-db-connection [conn db-spec]
      (jdbc/execute! conn ["INSERT INTO snippets (id, content, userid) VALUES (?, ?, ?)" id snippet-content userid]))
    id))

(defn get-snippet [id]
  (first (jdbc/query db-spec ["SELECT content FROM snippets WHERE id = ?" id] {:row-fn :content})))

(defn create-user [username password]
  (let [userid (str (java.util.UUID/randomUUID))
        password-hash (hashers/derive password)]
    (try
      (jdbc/with-db-connection [conn db-spec]
        (jdbc/execute! conn ["INSERT INTO users (userid, username, password_hash) VALUES (?, ?, ?)" userid username password-hash]))
      (catch Exception e
        (throw (Exception. "Failed to create user"))))))

(defn find-user-by-username [username]
  (jdbc/query db-spec ["SELECT * FROM users WHERE username = ? LIMIT 1" username] {:result-set-fn first}))

(defn validate-user-credentials [username submitted-password]
  (let [user (find-user-by-username username)]
    (when (and user (hashers/check submitted-password (:password_hash user)))
      user)))