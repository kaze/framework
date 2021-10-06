(ns xiana.context
  (:require
    [xiana.core :as xiana]))

(defn make
  "Create an empty context structure."
  [request]
  (->
    {:request  request
     :response {}}
    ;; return a context container
    xiana/map->Context (conj {})))
