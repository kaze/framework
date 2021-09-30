(ns xiana.config.core
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [config.core :refer [load-env]])
  (:import
    (java.io
      File
      PushbackReader)))

;; set configuration environment variable name
(def env-edn-file "XIANA_EDN_CONFIG")

;; set default edn file
(def default-edn-file
  (when-let [edn-file (System/getenv env-edn-file)]
    (.getAbsolutePath (File. edn-file))))

;; default config map: util wrapper/translator
(def default-config-map
  {:acl       :xiana.app/acl
   :auth      :xiana.app/auth
   :emails    :xiana.app/emails
   :webserver :xiana.app/web-server
   :migration :xiana.db.storage/migration
   :database  :xiana.db.storage/postgresql})

(defn read-edn-file
  "Read edn configuration file."
  [edn-file]
  (if edn-file (let [edn-file (or edn-file default-edn-file)]
                 (with-open [r (io/reader edn-file)]
                   (edn/read (PushbackReader. r))))
      (load-env)))

(defn get-spec
  "Select configuration spec using 'k' identifier."
  ([k] (get-spec k nil))
  ([k edn-file]
   (get (read-edn-file edn-file)
        (-> k default-config-map))))

(defn env
  []
  (load-env))
