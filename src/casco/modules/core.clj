(ns casco.modules.core
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [casco.modules.util :refer [bird siren
                                        failed magnifier
                                        no-enter
                                        rocket]]
            [casco.modules.agent :refer [migrate-all
                                         farm-seeds]]
            [casco.modules.db :refer [purge-migration-history]]
            [clojure.pprint :as pretty]))

(defn- contains?+ [k m] (contains? m k))

(defn- load-edn
  "source: filename or io/resource"
  [source]
  (with-open [r (io/reader source)]
    (edn/read (java.io.PushbackReader. r))))

(defn- profile-conf-map-is-required [config]
  (if (some (or nil? empty?) (map val config))
    (do
      (prn (str no-enter "The configuration map is required for the specified profile."))
      (prn "Example:") ;;TODO add example
      (pretty/pprint {:dev  {:src  "./resources/dev/migrations"
                             :dbs  [{:contact-points ["localhost:9042"]
                                     :session-keyspace "dev"
                                     :load-balancing-local-datacenter "datacenter1"
                                     :auto-create    true}]
                             :mode :rebase}

                      :test {:src  "./resources/test/migrations"
                             :dbs  [{:contact-points ["localhost:9042"]
                                     :session-keyspace "test"
                                     :load-balancing-local-datacenter "datacenter1"
                                     :auto-create    false}]
                             :mode :force}

                      :prod {:src  "./resources/prod/migrations"
                             :dbs  [{:contact-points ["localhost:9042"]
                                     :session-keyspace "prod"}]
                             :mode :strict}})
      (System/exit 0))
    config))

(defn- profile-src-is-required [config]
  (if (not (every? (partial contains?+ :src) (map val config)))
    (do
      (prn (str failed "The source migration directory is required, searching for default..."))
      (prn (str magnifier "Locating /resources/migrations..."))
      (if (.isDirectory (io/file "/resources/migrations"))
        config
        (do
          (prn no-enter "Migration resource does not exist.")
          (System/exit 0))))
    config))

(defn- profile-db-is-required [config]
  (if (not (every? (partial contains?+ :dbs) (map val config)))
    (do
      (prn no-enter "The database destination is required for specified profile.")
      (System/exit 0))
    config))

(defn conf-file-is-required
  [conf-path]
  (let [conf-map (load-edn conf-path)]
    (if (not conf-map)
      (do
        (prn no-enter "Could not load the config file content.")
        (System/exit 0))
      conf-map)))

(defn- validate-conf-map-file
  [config-file-path]
  (-> config-file-path
      conf-file-is-required
      profile-conf-map-is-required
      profile-src-is-required
      profile-db-is-required))

(defn migrator [conf-path profile mode up?] (let [conf-map (load-edn conf-path)
                                                  is-profile-presented? (contains?+ (keyword profile) conf-map)
                                                  is-mode-presented? (contains?+ mode #{:strict :rebase :force})
                                                  profile-map ((keyword profile) conf-map)
                                                  migration-src (:src profile-map)
                                                  migration-src (or migration-src "./resources/migrations")
                                                  dbs (:dbs profile-map)
                                                  mode (or mode :strict)
                                                  migration-func (partial migrate-all migration-src up? mode)]

                                              (if (not is-profile-presented?)
                                                (do
                                                  (prn (str no-enter "Profile not presented, did you had a typo?! List:"))
                                                  (println (keys conf-map)))
                                                (if (not is-mode-presented?)
                                                  (do
                                                    (prn siren "Conflict strategy mode is not presented, did you had a typo?! List:")
                                                    (println #{:strict :rebase :force})
                                                    (prn siren "Switch to default: strict mode.")
                                                    (prn (str bird "Casco started migrating..."))
                                                    (prn (str rocket "Buckle up!"))
                                                    (dorun (map migration-func dbs))) ;;TODO
                                                  (do
                                                    (prn (str bird "Casco started migrating..."))
                                                    (prn (str rocket "Buckle up!"))
                                                    (dorun (map migration-func dbs)))))))

(defn migrate
  ([conf-path]
   (let [conf-map (validate-conf-map-file conf-path)
         first-profile (first conf-map)
         profile (key first-profile)
         mode (or (:mode (val first-profile)) :strict)]
     (migrator conf-path profile mode true)))

  ([conf-path up?] (let [conf-map (validate-conf-map-file conf-path)
                         first-profile (first conf-map)
                         profile (key first-profile)
                         mode (or (:mode (val first-profile)) :strict)]
                     (migrator conf-path profile mode up?))) ;;TODO do migrate here

  ([conf-path profile up?] (let [conf-map (validate-conf-map-file conf-path)
                                 profile-map ((keyword profile) conf-map)
                                 mode (or (:mode profile-map) :strict)]
                             (migrator conf-path profile mode up?))) ;;TODO do migrate

  ([conf-path profile mode up?] (let [mode (or mode :strict)]
                                  (migrator conf-path profile mode up?)))) ;;TODO do migrate

(defn seed
  ([conf-path] (let [conf-map (validate-conf-map-file conf-path)
                     first-profile (first conf-map)
                     profile (key first-profile)
                     migration-src (:src (val first-profile))
                     dbs (:dbs (val first-profile))
                     migration-func (partial farm-seeds migration-src)]
                 (prn (str bird "Casco started migrating..."))
                 (prn (str rocket "Buckle up!"))
                 (prn (str "Didn't mentioned the profile, picking:" profile))
                 (dorun (map migration-func dbs))))

  ([conf-path profile] (let [conf-map (validate-conf-map-file conf-path)
                             is-profile-presented? (contains?+ (keyword profile) conf-map)
                             profile-map ((keyword profile) conf-map)
                             migration-src (:src profile-map)
                             dbs (:dbs profile-map)
                             migration-func (partial farm-seeds migration-src)]

                         (if (not is-profile-presented?)
                           (do
                             (prn (str no-enter "Profile not presented, did you had a typo?! List:"))
                             (println (keys conf-map)))
                           (do
                             (prn (str bird "Casco started migrating..."))
                             (prn (str rocket "Buckle up!"))
                             (dorun (map migration-func dbs)))))))

(defn purge-migration-table
  [conf-path profile]
  (let [conf-map (validate-conf-map-file conf-path)
        is-profile-presented? (contains?+ (keyword profile) conf-map)
        profile-map ((keyword profile) conf-map)
        dbs (:dbs profile-map)]

    (if (not is-profile-presented?)
      (do
        (prn (str no-enter "Profile not presented, did you had a typo?! List:"))
        (println (keys conf-map)))
      (do
        (prn (str bird "Casco started migrating..."))
        (prn (str rocket "Buckle up!"))
        (dorun (map purge-migration-history dbs))))))
