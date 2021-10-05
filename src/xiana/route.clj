(ns xiana.route
  (:require
    [reitit.core :as r]
    [xiana.commons :refer [?assoc-in]]
    [xiana.config :as config]
    [xiana.core :as xiana]))

;; routes reference
(defonce -routes (atom nil))

(defn not-found
  "Default not-found response handler helper."
  [state]
  (xiana/error
    (-> state
        (assoc :response {:status 404 :body "Not Found"}))))

(defn default-action
  "Default action response handler helper."
  [{request :request {handler :handler} :request-data :as state}]
  (try
    (xiana/ok
      (assoc state :response (handler request)))
    (catch Exception e
      (xiana/error
        (-> state
            (assoc :error e)
            (assoc :response
                   {:status 500 :body "Internal Server error"}))))))

(defn routes
  "Update routes."
  []
  (or @-routes
      (reset! -routes (config/get-spec :routes))))

(defmacro -get-in-template
  "Simple macro to get the values from the match template."
  [t m k p]
  `(or (-> ~t :data ~p)
       (-> ~t ~k ~m ~p)))

(defn- -update
  "Update state with router match template data."
  [match {request :request :as state}]
  (let [method (:request-method request)
        handler (-get-in-template match method :result :handler)
        action (-get-in-template match method :data :action)
        permission (-get-in-template match method :data :permission)
        interceptors (-get-in-template match method :data :interceptors)]
    ;; associate the necessary route match information
    (xiana/ok
      (-> state
          (?assoc-in [:request-data :method] method)
          (?assoc-in [:request-data :handler] handler)
          (?assoc-in [:request-data :interceptors] interceptors)
          (?assoc-in [:request-data :match] match)
          (?assoc-in [:request-data :permission] permission)
          (assoc-in [:request-data :action]
                    (or action
                        (if handler
                          default-action
                          not-found)))))))

(defn match
  "Associate router match template data into the state.
  Return the wrapped state container."
  [{request :request :as ctx}]
  (let [match (r/match-by-path (r/router (routes)) (:uri request))]
    (-update match ctx)))

(defn reset-routes! []
  (reset! -routes nil))
