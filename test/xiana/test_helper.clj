(ns xiana.test-helper
  (:require
    [xiana.core :as xiana]))

;; auxiliary function
(defn fetch-execute
  "Fetch and execute the interceptor function"
  [ctx interceptor branch]
  (->> (list ctx)
       (apply (branch interceptor))
       (xiana/extract)))
