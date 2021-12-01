(ns casco.modules.dir-manager
  (:require [clojure.java.io :refer [file as-relative-path]]
            [clojure.string :as str]))

(defn split-filename
  [file-path] (last (str/split file-path #"/")))

(defn split-version
  [file-name] (first (str/split file-name #"__")))

(defn map-version-paths
  [file-path] (let [file-name (split-filename file-path)]
                {:version (split-version file-name)
                 :path file-path}))

(defn calc-checksums
  [versioned-map] (let [file (slurp (:path versioned-map))]
                    (assoc versioned-map :checksum (hash file))))

(defn get-migration-map [path]
  (let [dir (file path)
        files (rest (file-seq dir))
        map-migrations (partial map (comp
                                     calc-checksums
                                     map-version-paths
                                     as-relative-path))]
    (sort-by :version (map-migrations files))))
