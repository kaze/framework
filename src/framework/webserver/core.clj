(ns framework.webserver.core
  "Lifecycle management of the webserver"
  (:require
    [clojure.tools.logging :as log]
    [framework.config.core :as config]
    [framework.handler.core :refer [handler-fn]]
    [org.httpkit.server :as server]))

;; web server reference
(defonce -webserver (atom {}))

(defn- make
  "Web server instance."
  [options dependencies]
  {:options options
   :server  (server/run-server (handler-fn dependencies) options)})

(defn stop
  "Stop web server."
  []
  ;; stop the server if necessary
  (when (not (empty? @-webserver))
    (when-let [-stop-server (get @-webserver :server)]
      (-stop-server))))

(defn start
  "Start web server."
  [dependencies]
  ;; stop the server
  (stop)
  ;; get server options
  (when-let [options (merge (config/get-spec :webserver) (:webserver dependencies))]
    (when-let [server (make options dependencies)]
      (log/info "Server started with options: " options)
      (swap! -webserver merge server))))