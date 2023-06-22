(ns org.pilosus.next-jdbc-test
  (:gen-class)
  (:require [next.jdbc :as jdbc]
            [next.jdbc.connection :as connection])
  (:import (com.zaxxer.hikari HikariDataSource)))

;; FIXME
(set! *warn-on-reflection* false)

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

;;(def ds (jdbc/get-datasource db-spec))
;; connection pool
;; use (.close ds) to close resources explicitly
;; or create cp with (with-open [...])
;;(def ds ^HikariDataSource (connection/->pool HikariDataSource db-spec))


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

(defn db-insert-address
  [datasource address]
  "Insert addressee into the table"
  (let [{:keys [name email]} address]
    (jdbc/execute!
     datasource
     ["insert into address (name, email)
     values (?, ?)" name email])))

(defn db-get-all-addresses
  [datasource]
  "Get all rows from `address` table"
  (jdbc/execute!
   datasource
   ["select * from address"]))

(defn db-insert-and-update
  [datasource]
  (jdbc/with-transaction [tx datasource]
    (jdbc/execute!
     tx
     ["insert into address (name, email)
     values (?, ?)" "Vitaly Samigullin" "vrs@pilosus.org"])
    (jdbc/execute!
     tx
     ["insert into address (name, email)
     values (?, ?)" "Timur Samigullin" "timur.samigullin@gmail.com"])))

(defn db-do
  [datasource]
  (jdbc/with-transaction [conn datasource {:auto-commit false}]
    (db-insert-and-update conn) ;; nested transaction
    (db-insert-address conn {:name "Olga Kajarskaia" :email "info@dogfriend.org"})
    (.rollback conn) ;; rollback previous line, but not the nested transaction!
    )
  )

(comment
  ;; Nested transactions
  ;; https://cljdoc.org/d/seancorfield/next.jdbc/1.2.659/doc/getting-started/transactions#nesting-transactions

  ;; make nested transactions ignored and only the outermost transaction takes effect
  ;; set dynamic var globally, use per-thread override with binding if necessary
  ;; https://stackoverflow.com/a/10987054/4241180
  (alter-var-root #'next.jdbc.transaction/*nested-tx* (constantly :ignore))

  ;; start connection pool
  (def ds ^HikariDataSource (connection/->pool HikariDataSource db-spec))

  ;; prep data
  (db-truncate-table ds)
  (db-create-table ds)

  ;; do nesting transactions, make sure none of the inserts persisted
  ;; after the outter transaction has
  (jdbc/with-transaction [conn ds {:auto-commit false}]
    (db-insert-and-update conn) ;; nested transaction with two inserts
    (db-insert-address conn {:name "Olga Kajarskaia" :email "info@dogfriend.org"})
    (.rollback conn) ;; rollback on the outermost transaction
  )
  ;; no rows found
  (db-get-all-addresses ds)
)

(defn -main
  "Entrypoint"
  [& args]
  (println "DB connection pull is running...")
  (let [ds ^HikariDataSource (connection/->pool HikariDataSource db-spec)]
    (println "Is DB connection pool running? " (.isRunning ds))
    (db-create-table ds)
    (db-insert-address ds {:name "Vitaly Samigullin" :email "vrs@pilosus.org"})
    (db-insert-address ds {:name "Timur Samigullin" :email "timur.samigullin@gmail.com"})
    (db-drop-table ds)
    (println "Closing DB connection pool...")
    (.close ds)
    (println "Is DB connection pool running? " (.isRunning ds))
    )
  )
