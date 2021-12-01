(ns casco.modules.agent
  (:require [casco.modules.dir-manager :refer [get-migration-map]]
            [casco.modules.db :refer [get-migration-table
                                      do-migrate
                                      do-rebase
                                      auto-create-keyspace
                                      create-migration-table]]
            [clojure.set :refer [difference]]
            [casco.modules.util :refer [bird no-enter
                                        siren vendeta
                                        sand-clock
                                        roadblock
                                        failed
                                        seeding
                                        shower
                                        cast-jtime]]
            [clojure.pprint :as pretty]))



(defn get-conflict-maps
  [migration-map migration-history up?]
  (let [version-checksum-map #(select-keys % [:version :checksum])
        history-map (cast-jtime migration-history)]

    (sort-by :version (filter (complement nil?)
                              (map #(if (and (not= (version-checksum-map %1) (version-checksum-map %2)) (= up? (:up %2)))
                                      {:migration %1
                                       :history   %2}
                                      nil)
                                   (take (count history-map) migration-map)
                                   history-map)))))

(defn take-diff [migration-map migration-history up?]
  (let [migration-status-map #(with-meta (assoc (select-keys % [:version]) :up up?)
                                {:path (:path %) :checksum (:checksum %)})
        history-status-map #(select-keys % [:version :up])
        meta-to-data #(merge (meta %) %)]
    (sort-by :version (map meta-to-data (difference (into #{} (map migration-status-map migration-map))
                                                    (into #{} (map history-status-map migration-history))))))) ;;TODO sort by version

(defn get-rebase-map
  [migration-src conflict-maps]
  (let [first-conflict (Float/parseFloat (get-in (first conflict-maps) [:history :version]))
        down-migration-list (get-migration-map (str migration-src "/down"))]

    (if (empty? down-migration-list)
      (do
        (prn (str roadblock "There is no down type migration file available for rebase strategy." roadblock))
        (prn (str failed "Aborting."))
        (System/exit 0))
      (sort-by :version
               (filter #(<= first-conflict
                            (Float/parseFloat (:version %)))
                       down-migration-list)))))

(defn rebase [migration-src up? db]
  (let [migration-map (get-migration-map (str migration-src (if up? "/up" "/down")))
        migration-history (get-migration-table db)
        conflict-maps (get-conflict-maps migration-map migration-history up?)
        rebase-maps (get-rebase-map migration-src conflict-maps)
        delete-records (map #(get-in % [:history :version]) conflict-maps)
        migration-maps (take-diff
                        (get-migration-map (str migration-src (if up? "/up" "/down")))
                        (get-migration-table db) up?)]

    (do-rebase delete-records rebase-maps db)
    (do-migrate db migration-maps up?)))

(defn migrate-all
  [migration-src up? mode db-destination]
  (auto-create-keyspace db-destination)
  (create-migration-table db-destination)
  (let [migration-map (get-migration-map (str migration-src (if up? "/up" "/down")))
        migration-history (get-migration-table db-destination)
        conflict-maps (get-conflict-maps migration-map migration-history up?)
        mode (if (nil? mode) :strict mode)]

    (if ((or nil empty?) conflict-maps)
      (if (= (count migration-map) (count (filter #(= (:up %) up?) migration-history)))
        (prn (str bird "CASCO: All migrations are already applied,have fun!" vendeta))
        (do-migrate db-destination (take-diff migration-map migration-history up?) up?))

      (cond
        (= mode :strict) (do
                           (prn (str siren " There is conflicts:"))
                           (pretty/pprint conflict-maps)
                           (prn (str no-enter "Cannot move further since you are in strict mode."))
                           (System/exit 0))
        (= mode :rebase) (do
                           (prn (str siren " There is conflicts:"))
                           (pretty/pprint conflict-maps)
                           (prn (str sand-clock "Trying to apply rebase strategy..."))
                           (rebase migration-src up? db-destination))
        (= mode :force) (do
                          (prn (str siren " There is conflicts:"))
                          (pretty/pprint conflict-maps)
                          (prn (str siren " Use force mode only for independent migration states."))
                          (do-migrate db-destination migration-map up?))))))

(defn farm-seeds [migration-src db-destination]
  (let [migration-map (get-migration-map (str migration-src "/seed"))]
    (prn (str seeding shower "Farming your seeds...!"))
    (if ((or nil empty?) migration-map)
      (do
        (prn (str failed "You have no seeds to grow!"))
        (prn "Put them in " migration-src "/seed"))
      (do-migrate db-destination (map #(dissoc % :checksum) migration-map) true))))

;;TODO check if directory is ok and not empty