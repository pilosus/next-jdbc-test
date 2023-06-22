(ns org.pilosus.next-jdbc-test-test
  (:require [clojure.test :refer :all]
            [org.pilosus.next-jdbc-test :as m]
            [next.jdbc.connection :as connection]
            [mount.core :as mount :refer [defstate]]
            [next.jdbc :as jdbc])
  (:import (com.zaxxer.hikari HikariDataSource)))

;; Fixtures

(defstate db-test
  :start (connection/->pool HikariDataSource m/db-spec)
  :stop (-> db-test .close))

(defn fix-server-run
  "Manage DB"
  [test]
  (println "====== Run server fixture started  ======")
  (mount/start)
  (m/db-create-table db-test)
  (test)
  (m/db-drop-table db-test)
  (mount/stop)
  (println "====== Run server fixture stopped  ======"))

(defn fix-db-rollback
  "Rollback nested transactions

  If it's the only one fixture, then db-test component must be started before the test and stopped after. Otherwise, rely on mount/start and mount/stop in the run-once fixture."
  [test]
  (println "====== Rollback fixture started  ======")
  (binding [next.jdbc.transaction/*nested-tx* :ignore]
    (jdbc/with-transaction [conn db-test {:auto-commit false}]
      ;; system component must be substituted with the test one
      ;; so that a single connection pool is used
      ;; and a tranasction can be rolled back
      (-> (mount/only [#'m/db])
          (mount/swap {#'m/db conn})
          (mount/start))
      (test)
      (.rollback conn)))
  (println "====== Rollback fixture stopped  ======"))

;; Run fixtures

(use-fixtures :once fix-server-run)
(use-fixtures :each fix-db-rollback)

;; Tests

(deftest insert-two-addresses-1-test
  (testing "insert-two-addresses-1-test"
    (let [result (m/insert-two-addresses)]
      (is (= 2 (count result))))))

(deftest insert-two-addresses-2-test
  (testing "insert-two-addresses-2-test"
    (let [result (m/insert-two-addresses)]
      (is (= 2 (count result))))))
