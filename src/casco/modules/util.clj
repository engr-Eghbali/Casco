(ns casco.modules.util
  (:require  [clojure.pprint :as pretty]
             [clojure.string :as str]))

(defn unicode-to-string
  "Turns a hex unicode symbol into a string.
  Deals with such long numbers as 0x1F535 for example."
  [code]
  (-> code Character/toChars String.))

(def bird (unicode-to-string 0x1F426))
(def redball (unicode-to-string 0x1F534))
(def blueball (unicode-to-string 0x1F535))
(def rocket (unicode-to-string 0x1F680))
(def green-tick (unicode-to-string 0x2705))
(def blue-ok (unicode-to-string 0x1F197))
(def up! (unicode-to-string 0x1F199))
(def seeding (unicode-to-string 0x1F331))
(def failed (unicode-to-string 0x274C))
(def no-enter (unicode-to-string 0x26D4))
(def caution (unicode-to-string 0x2757))
(def fire (unicode-to-string 0x1F525))
(def sand-clock (unicode-to-string 0x23F3))
(def vendeta (unicode-to-string 0x1F44C))
(def roadblock (unicode-to-string 0x1F6A7))
(def siren (unicode-to-string 0x1F6A8))
(def magnifier (unicode-to-string 0x1F50E))
(def shower (unicode-to-string 0x1F6BF))

(defn pr-help
  []
  (do (prn (str bird "Casco"))
      (prn (str magnifier "Concepts:"))
      (prn (str "-" up! "up: building up your database(s) from /up dir in your migration source"))
      (println "\tExamples: lein casco up")
      (println "\t          lein casco up --force")
      (println "\t          lein casco up :test --strict")
      (println "\t          lein casco up :test --strict \"./my-confs/casco.edn\"")
      (println "\t          lein casco <operation> <:profile-key> <--mode> <config/file/path>")
      (prn (str "-" fire "down: revert your database(s) state from /down dir in your migration source"))
      (println "\tExamples: lein casco down")
      (println "\t          lein casco down --force")
      (println "\t          lein casco down :dev --force")
      (println "\t          lein casco down :dev --force \"./my-confs/casco.edn\"")
      (println "\t          lein casco <operation> <:profile-key> <--mode> <config/file/path>")
      (prn (str "-" seeding "seed: filling up your database(s) from /seed dir in your migration source with mentioned profile."))
      (println "\tExamples: lein casco seed :dev")
      (println "\t          lein casco seed :dev \"./my-confs/casco.edn\"")
      (prn (str "-" failed "forget: forgeting the migration history of mentioned profile."))
      (println "\tExample:  lein casco forget :dev")
      (println "\t          lein casco forget :dev \"./my-confs/casco.edn\"")))

(defn catch-profile-arg [args]
  (let [profile (first (filter #(str/includes? % ":") args))]
    (if profile
      (str/replace-first profile ":" "")
      nil)))

(defn catch-mode-arg [args]
  (let [mode (first (filter #(str/includes? % "--") args))]
    (if mode
      (or (keyword (str/replace-first mode "--" "")) :strict)
      :strict)))

(defn catch-path-arg [args]
  (let [path (first (filter #(str/includes? % "/") args))]
    (if path
      (str/replace path "\"" "")
      "./casco.edn")))

(defn cast-jtime [migration-history]
  (if migration-history
    (map #(update % :applied_at (partial str)) migration-history)
    migration-history))

;;TODO do the default migrations src