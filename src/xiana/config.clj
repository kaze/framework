(ns xiana.config
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [config.core :refer [env]])
  (:import
    (java.io
      PushbackReader)))

(defonce -config (atom nil))

(defn app-keyword
  [k]
  (keyword "xiana.app" (name k)))

(defn read-edn-file
  "Read edn configuration file."
  [edn-file]
  (when edn-file
    (with-open [r (io/reader edn-file)]
      (edn/read (PushbackReader. r)))))

(defn load-config!
  "Loads configuration from environment variables, config edn file, and passed parameter map"
  ([]
   (load-config! {}))
  ([args]
   (let [c (merge env args)]
     (reset! -config
             (merge env
                    (read-edn-file (:xiana_edn_config c))
                    args)))))

(defn get-spec
  "Select configuration spec using 'k' identifier."
  [k]
  (when (nil? @-config) (load-config!))
  (get @-config k (get @-config (app-keyword k))))
