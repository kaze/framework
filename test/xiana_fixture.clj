(ns xiana-fixture
  (:require
    [clj-test-containers.core :as tc]
    [migratus.core :as migratus]
    [next.jdbc :as jdbc]
    [xiana.config.core :as config]
    [xiana.db.core :as db-core]
    [xiana.route.core :as routes]
    [xiana.session.core :as session-backend]
    [xiana.webserver.core :as ws]))

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
        mig-config (assoc (:xiana.db.storage/migration config) :db db)]
    (migratus/migrate mig-config))
  config)

(defn std-system-fixture
  [config f]
  (try
    (-> (config/env)
        (merge config)
        docker-postgres!
        (assoc-in [:xiana.app/web-server :port] 3333)
        migrate!
        system)
    (f)
    (finally
      (ws/stop))))
