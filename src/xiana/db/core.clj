(ns xiana.db.core
  (:require
    [next.jdbc :as jdbc]
    [xiana.config.core :as config]))

(defn start
  "Start database instance.
  Get the database specification from
  the 'edn' configuration file in case of
  db-spec isn't set."
  ([] (start nil))
  ([db-spec]
   (when-let [spec (or db-spec (config/get-spec :database))]
     {:datasource (jdbc/get-datasource spec)})))
