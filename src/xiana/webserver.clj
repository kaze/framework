(ns xiana.webserver
  (:require
    [ring.adapter.jetty :as jetty]
    [xiana.config :as config]
    [xiana.core :as xiana]
    [xiana.interceptor-queue :as interceptor.queue]
    [xiana.route :as route]
    [xiana.state :as state]))

;; web server reference
(defonce -webserver (atom {}))

(defn handler-fn
  "Return jetty server handler function."
  [deps]
  (fn handle*
    ([http-request]
     (let [state (state/make deps http-request)
           queue (list #(interceptor.queue/execute % (:router-interceptors deps))
                       #(route/match %)
                       #(interceptor.queue/execute % (:controller-interceptors deps)))]
       (-> (xiana/apply-flow-> state queue)
           ;; extract
           (xiana/extract)
           ;; get the response
           (get :response))))
    ([request respond _]
     (respond (handle* request)))))

(defn- make
  "Web server instance."
  [options dependencies]
  {:options options
   :server  (jetty/run-jetty (handler-fn dependencies) options)})

(defn stop
  "Stop web server."
  []
  ;; stop the server if necessary
  (when (not (empty? @-webserver))
    (.stop (get @-webserver :server))))

(defn start
  "Start web server."
  [dependencies]
  ;; stop the server
  (stop)
  ;; get server options
  (when-let [options (merge (config/get-spec :webserver) (:webserver dependencies))]
    ;; tries to initialize the web-server if we have the
    ;; server specification (its options)
    (swap! -webserver merge (make options dependencies))))