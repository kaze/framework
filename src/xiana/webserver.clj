(ns xiana.webserver
  (:require
    [ring.adapter.jetty :as jetty]
    [xiana.config :as config]
    [xiana.core :as xiana]
    [xiana.interceptor-queue :as interceptor.queue]
    [xiana.route :as route]
    [xiana.state :as state]))

;; web server reference
(defonce -webserver (atom nil))

(def handler-fn
  "Return jetty server handler function."
  (fn handle*
    ([http-request]
     (let [state (state/make http-request)
           queue (list #(interceptor.queue/execute % (config/get-spec :router-interceptors))
                       #(route/match %)
                       #(interceptor.queue/execute % (config/get-spec :controller-interceptors)))]
       (-> (xiana/apply-flow-> state queue)
           ;; extract
           (xiana/extract)
           ;; get the response
           (get :response))))
    ([request respond _]
     (respond (handle* request)))))

(defn stop
  "Stop web server."
  []
  ;; stop the server if necessary
  (when @-webserver
    (.stop @-webserver)))

(defn start
  "Start web server."
  []
  ;; stop the server
  (stop)
  ;; get server options
  (let [options (config/get-spec :web-server)]
    (reset! -webserver (jetty/run-jetty handler-fn options))))
