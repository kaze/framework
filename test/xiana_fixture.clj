(ns xiana-fixture
  (:require
    [clj-test-containers.core :as tc]
    [migratus.core :as migratus]
    [xiana.app :as app]
    [xiana.config :as config]
    [xiana.webserver :as ws]))

(defn docker-postgres!
  [cfg]
  (let [postgres-cfg (config/get-spec :postgresql)
        container (-> (tc/create {:image-name    "postgres:11.5-alpine"
                                  :env-vars      {"POSTGRES_DB" (:dbname postgres-cfg)}
                                  :exposed-ports [5432]})
                      (tc/start!))
        port (get (:mapped-ports container) 5432)
        db-config (assoc postgres-cfg
                         :port port
                         :embedded container
                         :subname (str "//localhost:" port "/frankie"))]
    (assoc cfg :postgresql db-config)))

(defn migrate!
  [cfg]
  (let [db (:postgresql cfg)
        mig-config (assoc (config/get-spec :migration) :db db)]
    (migratus/migrate mig-config))
  cfg)

(defn std-system-fixture
  [config f]
  (try
    (-> (config/load-config! config)
        docker-postgres!
        (assoc-in [:xiana.app/web-server :port] 3333)
        migrate!
        app/start)
    (f)
    (finally
      (ws/stop))))
