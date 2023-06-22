(ns org.pilosus.next-jdbc-test
  (:gen-class)
  (:require [next.jdbc :as jdbc]
            [next.jdbc.connection :as connection]
            [mount.core :as mount :refer [defstate]])
  (:import (com.zaxxer.hikari HikariDataSource)))

(set! *warn-on-reflection* true)

;; DB connection pool set up as a system component

(def db-spec
  {:dbtype "postgres"
   :dbname "test"
   :user "test"
   :password "test"
   :host "localhost"
   :port 25432
   ;; HikariCP specific
   :username "test"
   :maximumPoolSize 5
   })

(defstate db
  :start (connection/->pool HikariDataSource db-spec)
  :stop (-> db ^HikariDataSource .close)) ;; FIXME reflection warning

;; Run the system
(mount/start)

;; Table prep

(defn db-create-table
  [datasource]
  "Create `address` table"
  (jdbc/execute!
   datasource
   ["create table if not exists address (
     id serial primary key,
     name text,
     email varchar(255))"]))

(defn db-drop-table
  [datasource]
  "Drop `address` table"
  (jdbc/execute!
   datasource
   ["drop table address"]))

(defn db-truncate-table
  [datasource]
  (jdbc/execute!
   datasource
   ["truncate address restart identity"]))

;; Data prep

(defn db-get-all-addresses
  "Get all rows from the `address` table"
  [datasource]
  (jdbc/with-transaction [tx datasource]
    (jdbc/execute!
     tx
     ["select * from address"])))

(defn db-insert-single-row
  "Insert a single row into the `address` table"
  [datasource address]
  (let [{:keys [name email]} address]
    (jdbc/execute!
     datasource
     ["insert into address (name, email)
     values (?, ?)" name email])))

(defn db-insert-two-rows
  "Insert two predefined rows into the `address` table"
  [datasource]
  (jdbc/with-transaction [tx datasource]
    (println (jdbc/execute!
              tx
              ["insert into address (name, email)
     values (?, ?)" "John Doe" "john@example.com"]))
    (println (jdbc/execute!
              tx
              ["insert into address (name, email)
     values (?, ?)" "Joe Doe" "joe@example.com"]))))

;; Functions to run tests against

(defn insert-two-addresses
  "Function to check nested transactions with the test fixtures"
  []
  (jdbc/with-transaction [conn db]
    (db-insert-two-rows conn)
    (db-get-all-addresses conn)))

;; Main

(defn -main
  "Entrypoint"
  [& args]
  (println "I don't do much"))

;; Code snippets to play around in REPL

(comment
  ;; Nested transactions
  ;; Ignore nested transactions, so that the outermost transaction's rollback will rollback all the nested ones (used in tests fixtures)
  ;; Set dynamic var globally, or use (binding [...]) to reduce the scope
  (alter-var-root #'next.jdbc.transaction/*nested-tx* (constantly :ignore))

  ;; Start connection pool
  (def ds ^HikariDataSource (connection/->pool HikariDataSource db-spec))

  ;; Prep data
  (db-truncate-table ds)
  (db-create-table ds)

  ;; Do nesting transactions, make sure none of the inserts persisted
  (jdbc/with-transaction [conn ds {:auto-commit false}]
    ;; nested transaction with two inserts
    (db-insert-two-rows conn)
    ;; one more insert
    (db-insert-single-row conn {:name "Max Musterman" :email "max@example.com"})
    ;; rollback on the outermost transaction
    (.rollback conn)
  )
  ;; outermost transaction's rollback also rolled back all the nested transactions
  (db-get-all-addresses ds)
)
