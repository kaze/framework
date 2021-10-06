(ns xiana.route-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [xiana.config :as config]
    [xiana.context :as context]
    [xiana.core :as xiana]
    [xiana.route :as route]))

(def sample-request
  {:uri "/" :request-method :get})

(def sample-not-found-request
  {:uri "/not-found" :request-method :get})

(def sample-routes
  "Sample routes structure."
  [["/" {:action :action}]])

(def sample-routes-with-handler
  "Sample routes structure."
  [["/" {:handler :handler}]])

(def sample-routes-without-action
  "Sample routes structure (without action or handler)."
  [["/" {}]])

(defn test-handler
  "Sample test handler function for the tests."
  [_]
  {:status 200 :body "Ok"})

(def test-ctx
  "Sample test ctx."
  {:request {}
   :request-data {:handler test-handler}})

(deftest routes-with-sample-routes
  (config/load-config! {:routes sample-routes})
  (route/reset-routes!)
  (testing "test if sample routes was registered correctly"
    (is (= sample-routes (route/routes))))
  (testing "test route match update request-data (ctx) functionality"
    (let [ctx (-> (context/make sample-request)
                  (route/match)
                  (xiana/extract))
          ;; expected request data
          expected {:method :get
                    :match  #reitit.core.Match{:template    "/"
                                               :data        {:action :action}
                                               :result      nil
                                               :path-params {}
                                               :path        "/"}
                    :action :action}]
      ;; verify if updated request-data
      ;; is equal to the expected value
      (is (= expected (:request-data ctx)))))
  (testing "test if the updated request-data (context) data handles the"
    (let [action (-> (context/make sample-not-found-request)
                     (route/match)
                     (xiana/extract)
                     (:request-data)
                     (:action))
          expected route/not-found]
      (is (= action expected))))
  (testing "test if the updated request-data contains the right action"
    ;; (re)set routes
    (config/load-config! {:routes sample-routes-with-handler})
    (route/reset-routes!)
    ;; get action from the updated context/match (micro) flow computation
    (let [action (-> (context/make sample-request)
                     (route/match)
                     (xiana/extract)
                     (:request-data)
                     (:action))
          ;; expected action
          expected route/default-action]
      ;; verify if action has the expected value
      (is (= action expected))))
  (testing "test if the route/match flow handles a route without a handler or action"
    ;; (re)set routes
    (config/load-config! {:routes sample-routes-without-action})
    (route/reset-routes!)
    ;; get action from the updated context/match (micro) flow computation
    (let [action (-> (context/make sample-request)
                     (route/match)
                     (xiana/extract)
                     (:request-data)
                     (:action))
          ;; expected action? TODO: research
          expected route/not-found]
      ;; verify if action has the expected value
      (is (= action expected))))
  (testing "test default not-found handler response"
    (let [response (:response (xiana/extract
                                (route/not-found {})))
          expected {:status 404, :body "Not Found"}]
      ;; verify if the response and expected value are equal
      (is (= response expected))))
  (testing "test default action handler: error response"
    (let [response (:response (xiana/extract
                                (route/default-action {})))
          expected {:status 500 :body "Internal Server error"}]
      ;; verify if the response and expected value are equal
      (is (= response expected))))
  (testing "test default action handler: ok response"
    (let [response (:response (xiana/extract
                                (route/default-action test-ctx)))
          expected {:status 200, :body "Ok"}]
      ;; verify if the response and expected value are equal
      (is (= response expected)))))
