(ns xiana.state
  (:require
    [xiana.core :as xiana]))

(defn make
  "Create an empty state structure."
  [request]
  (->
    {:request  request
     :response {}}
    ;; return a state container
    xiana/map->State (conj {})))
