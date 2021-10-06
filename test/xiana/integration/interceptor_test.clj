(ns xiana.integration.interceptor-test
  (:require
    [clj-http.client :as http]
    [clojure.test :refer [deftest is use-fixtures]]
    [xiana-fixture :as fixture]
    [xiana.config :as config]
    [xiana.core :as xiana]
    [xiana.webserver :as ws]))

(def ok-action
  "Auxiliary ok container function."
  #(xiana/ok
     (assoc % :response {:status 200, :body "ok"})))

(def ok-interceptor
  {:leave #(xiana/ok
             (assoc % :response {:status 200, :body "ok interceptor"}))})

(def error-interceptor
  {:enter (fn [_] (throw (Exception. "enter-exception")))
   :leave (fn [_] (throw (Exception. "leave-exception")))
   :error (fn [state] (xiana/error (assoc state :response {:body "Error"
                                                           :status 500})))})

(def routes
  [["/api" {:handler ws/handler-fn}
    ["/image" {:get {:action ok-action}}]]])

(def system-config
  {:routes                  routes})

(use-fixtures :once (partial fixture/std-system-fixture system-config))

(deftest ok-without-interceptors
  (config/load-config! system-config)
  (ws/reset-interceptors!)
  (is (= [200 "ok"]
         (-> (http/get "http://localhost:3333/api/image"
                       {:throw-exceptions false})
             ((juxt :status :body))))))

(deftest error-in-router-interceptors
  (config/load-config! (assoc system-config :router-interceptors [error-interceptor]))
  (ws/reset-interceptors!)
  (is (= [500 "Error"]
         (-> (http/get "http://localhost:3333/api/image"
                       {:throw-exceptions false})
             ((juxt :status :body))))))

(deftest action-overrides-router-interceptors
  (config/load-config! (assoc system-config :router-interceptors [ok-interceptor]))
  (ws/reset-interceptors!)
  (is (= [200 "ok"]
         (-> (http/get "http://localhost:3333/api/image"
                       {:throw-exceptions false})
             ((juxt :status :body))))))

(deftest controller-interceptor-overrides-action
  (config/load-config! (assoc system-config :controller-interceptors [ok-interceptor]))
  (ws/reset-interceptors!)
  (is (= [200 "ok interceptor"]
         (-> (http/get "http://localhost:3333/api/image"
                       {:throw-exceptions false})
             ((juxt :status :body))))))
