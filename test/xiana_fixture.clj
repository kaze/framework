(ns xiana-fixture
  (:require
    [clj-test-containers.core :as tc]
    [migratus.core :as migratus]
    [next.jdbc :as jdbc]
    [xiana.config :as config]
    [xiana.db :as db-core]
    [xiana.route :as routes]
    [xiana.session :as session-backend]
    [xiana.webserver :as ws]))

(defn system
  [config]
  (let [deps {:webserver               (:xiana.app/web-server config)
              :routes                  (:routes config)
              :role-set                (:role-set config)
              :router-interceptors     (:router-interceptors config)
              :controller-interceptors (:controller-interceptors config)}]
    (assoc deps :web-server (ws/start deps))))

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
        (config/load-config!)
        migrate!
        system)
    (f)
    (finally
      (ws/stop))))
