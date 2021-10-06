(ns xiana.webserver-test
  (:require
    [clojure.test :refer [deftest function? is]]
    [xiana.config :as config]
    [xiana.core :as xiana]
    [xiana.route :as route]
    [xiana.webserver :as webserver])
  (:import
    (org.eclipse.jetty.server
      Server)))

(def sample-request
  {:uri "/" :request-method :get})

(def sample-routes
  "Sample routes structure."
  [["/" {:action
         #(xiana/ok
            (assoc % :response {:status 200, :body ":action"}))}]])

(deftest handler-fn-creation
  ;; test if handler-fn return
  (is (function? webserver/handler-fn)))

(deftest start-webserver
  ;; verify if initial instance is clean
  (is (or (empty? @webserver/-webserver)
          (.isStopped (:server @webserver/-webserver))))
  ;; start the server and fetch it
  (is (= (type (:server (webserver/start)))
         Server)))

;; test jetty handler function call
(deftest call-jetty-handler-fn
  ;; set sample routes
  (config/load-config! {:routes sample-routes})
  (route/reset-routes!)
  (is (= (webserver/handler-fn sample-request) {:status 200, :body ":action"})))

(deftest stop-webserver
  (webserver/stop)
  (is (or (empty? @webserver/-webserver)
          (.isStopped (:server @webserver/-webserver)))))
