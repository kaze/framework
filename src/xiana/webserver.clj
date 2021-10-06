(ns xiana.webserver
  (:require
    [ring.adapter.jetty :as jetty]
    [xiana.config :as config]
    [xiana.context :as context]
    [xiana.core :as xiana]
    [xiana.interceptor-queue :as interceptor.queue]
    [xiana.route :as route]))

;; web server reference
(defonce -webserver (atom {}))

(def handler-fn
  "Return jetty server handler function."
  (fn handle*
    ([http-request]
     (let [ctx (context/make http-request)
           queue (list #(interceptor.queue/execute % (:router-interceptors @-webserver) false)
                       #(route/match %)
                       #(interceptor.queue/execute % (:controller-interceptors @-webserver)))]
       (-> (xiana/apply-flow-> ctx queue)
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
  (when (:server @-webserver)
    (.stop (:server @-webserver)))
  (reset! -webserver {}))

(defn reset-interceptors!
  []
  (swap! -webserver assoc
         :router-interceptors (config/get-spec :router-interceptors)
         :controller-interceptors (config/get-spec :controller-interceptors)))

(defn start
  "Start web server."
  []
  ;; stop the server
  (stop)
  ;; get server options
  (let [options (config/get-spec :web-server)]
    (reset! -webserver {:server                  (jetty/run-jetty handler-fn options)
                        :options                 options
                        :router-interceptors     (config/get-spec :router-interceptors)
                        :controller-interceptors (config/get-spec :controller-interceptors)})))
