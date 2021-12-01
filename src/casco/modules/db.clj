(ns casco.modules.db
  (:require [qbits.alia :as alia]
            [qbits.hayt :refer [insert values
                                select where delete]]
            [casco.modules.dir-manager :refer [split-filename]]
            [casco.modules.util :refer [bird up! fire sand-clock
                                        green-tick failed]]))

(defn auto-create-keyspace [db]
  (if (:auto-create db)
    (let [query (str "CREATE KEYSPACE IF NOT EXISTS " (:session-keyspace db) " WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 3};")
          session-conf (dissoc db :auto-create :session-keyspace)]
      (with-open [session (alia/session session-conf)]
        (alia/execute session query)
        db))
    db))

(defn create-migration-table
  [db]
  (with-open [session (alia/session (dissoc db :auto-create))]
    (alia/execute session "CREATE TABLE IF NOT EXISTS migration_map (version varchar,
                                              checksum int,
                                              path varchar,
                                              applied_at timestamp,
                                              up Boolean,
                                              PRIMARY KEY (version));")))

(defn purge-migration-history
  [db]
  (with-open [session (alia/session (dissoc db :auto-create))]
    (prn (str fire "Purging Migration Histories!" fire))
    (alia/execute session "DROP TABLE IF EXISTS migration_map;")
    (alia/execute session "CREATE TABLE IF NOT EXISTS migration_map (version varchar,
                                              checksum int,
                                              path varchar,
                                              applied_at timestamp,
                                              up Boolean,
                                              PRIMARY KEY (version));")
    (prn (str green-tick " Done."))))

(defn delete-records-by-id
  [db ids]
  (let [session (alia/session (dissoc db :auto-create))]
    (dorun (map #(alia/execute session
                               (delete :migration_map (where {:version %})))
                ids))))

(defn get-migration-table
  [db]
  (with-open [session (alia/session (dissoc db :auto-create))]
    (sort-by :version (alia/execute session "SELECT * FROM migration_map")))) ;;TODO force sort by version

(defn do-migrate [db migration-maps up?]
  (do
    (prn (str  (if up? (str up! "Building UP!" up!) (str fire "Purging Down!" fire))))
    (let [session (alia/session (dissoc db :auto-create))]
      (dorun (map #(do
                     (prn (str sand-clock " Migrating " (split-filename (:path %)) "..."))
                     (try
                       (alia/execute session (slurp (:path %)))
                       (alia/execute session (insert :migration_map
                                                     (values (assoc % :applied_at (System/currentTimeMillis)
                                                                    :up up?))))
                       (prn (str green-tick " Done."))
                       (catch Exception e
                         (prn failed "Migration failed :")
                         (prn e))))
                  migration-maps)))))

(defn do-rebase
  [delete-records rebase-maps db]
  (do-migrate db rebase-maps false)
  (delete-records-by-id db delete-records)) ;;TODO in progress...