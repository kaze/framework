(ns xiana.integration.interceptor-test
  (:require
    [clj-http.client :as http]
    [clojure.test :refer [deftest is use-fixtures]]
    [xiana-fixture :as fixture]
    [xiana.config :as config]
    [xiana.core :as xiana]
    [xiana.integration.integration-helpers :as test-routes]
    [xiana.route :as route]
    [xiana.webserver :as ws]))

(def ok-interceptor
  {:leave #(xiana/ok
             (assoc % :response {:status 200, :body "ok interceptor"}))})

(def error-interceptor
  {:enter (fn [_] (throw (Exception. "enter-exception")))
   :leave (fn [_] (throw (Exception. "leave-exception")))
   :error (fn [state] (xiana/error (assoc state :response {:body   "Error"
                                                           :status 500})))})

(def system-config
  {:routes test-routes/routes})

(use-fixtures :once (partial fixture/std-system-fixture system-config))

(deftest ok-without-interceptors
  (config/load-config! system-config)
  (ws/reset-interceptors!)
  (route/reset-routes!)
  (is (= [200 "ok"]
         (-> (http/get "http://localhost:3333/api/interceptor"
                       {:throw-exceptions false})
             ((juxt :status :body))))))

(deftest error-in-router-interceptors
  (config/load-config! (assoc system-config :router-interceptors [error-interceptor]))
  (ws/reset-interceptors!)
  (route/reset-routes!)
  (is (= [500 "Error"]
         (-> (http/get "http://localhost:3333/api/interceptor"
                       {:throw-exceptions false})
             ((juxt :status :body))))))

(deftest action-overrides-router-interceptors
  (config/load-config! (assoc system-config :router-interceptors [ok-interceptor]))
  (ws/reset-interceptors!)
  (is (= [200 "ok"]
         (-> (http/get "http://localhost:3333/api/interceptor"
                       {:throw-exceptions false})
             ((juxt :status :body))))))

(deftest controller-interceptor-overrides-action
  (config/load-config! (assoc system-config :controller-interceptors [ok-interceptor]))
  (ws/reset-interceptors!)
  (is (= [200 "ok interceptor"]
         (-> (http/get "http://localhost:3333/api/interceptor"
                       {:throw-exceptions false})
             ((juxt :status :body))))))
