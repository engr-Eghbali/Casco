(ns casco.casco
  (:require
   [casco.modules.core :refer [migrate seed purge-migration-table]]
   [casco.modules.util :refer [failed bird up!
                               fire seeding pr-help
                               magnifier catch-profile-arg
                               catch-path-arg catch-mode-arg]]))

(defn- up-factory [args]
  (let [profile (catch-profile-arg args)
        mode (catch-mode-arg args)
        path (catch-path-arg args)]

    (if profile
      (migrate path profile mode true)
      (migrate path true))))

(defn- down-factory [args]
  (let [profile (catch-profile-arg args)
        mode (catch-mode-arg args)
        path (catch-path-arg args)]

    (if profile
      (migrate path profile mode false)
      (migrate path false))))

(defn- seed-factory [args]
  (let [profile (catch-profile-arg args)
        path (catch-path-arg args)]
    (if profile
      (seed path profile)
      (seed path))))

(defn- forget-factory [args]
  (let [path (catch-path-arg args)
        profile (catch-profile-arg args)]
    (if profile
      (purge-migration-table path profile)
      (prn (str failed "Profile is required for purging history.")))))

(defn- done [] (prn (str bird "Casco finished process" bird)) (System/exit 0))

(defn -main [& args]
  (condp = (first args)
    "up" ((up-factory args) (done))
    "down" ((down-factory args) (done))
    "seed" ((seed-factory args) (done))
    "forget" ((forget-factory args) (done))
    "help" ((pr-help) (done))))

;;TODO down mode must work without profile too