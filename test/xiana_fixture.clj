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
  (let [session-backend (:session-backend config (session-backend/init-in-memory))
        deps {:webserver               (:xiana.app/web-server config)
              :routes                  (routes/reset (:routes config))
              :role-set                (:role-set config)
              :auth                    (:xiana.app/auth config)
              :session-backend         session-backend
              :router-interceptors     (:router-interceptors config)
              :controller-interceptors (:controller-interceptors config)
              :db                      (db-core/start
                                         (:database-connection config))}]
    (assoc deps :web-server (ws/start deps))))

(defn docker-postgres!
  [config]
  (let [container (-> (tc/create {:image-name    "postgres:11.5-alpine"
                                  :exposed-ports [5432]})
                      (tc/start!))
        port (get (:mapped-ports container) 5432)

        db-config (-> config
                      :database-connection
                      (assoc
                        :port port
                        :dbtype "postgresql"
                        :dbname "xiana_test"
                        :embedded container
                        :user "postgres"
                        :subname (str "//localhost:" port "/frankie")))]
    (jdbc/execute! (dissoc db-config :dbname) ["CREATE DATABASE xiana_test;"])
    (jdbc/execute! db-config ["GRANT ALL PRIVILEGES ON DATABASE xiana_test TO postgres;"])
    (assoc config :database-connection db-config)))

(defn migrate!
  [config]
  (let [db (:database-connection config)
        mig-config (assoc (:xiana.app/migration config) :db db)]
    (migratus/migrate mig-config))
  config)

(defn std-system-fixture
  [config f]
  (try
    (-> (config/load-config config)
        docker-postgres!
        (assoc-in [:xiana.app/web-server :port] 3333)
        migrate!
        system)
    (f)
    (finally
      (ws/stop))))
