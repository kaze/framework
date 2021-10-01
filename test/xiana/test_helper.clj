(ns xiana.test-helper
  (:require
    [xiana.core :as xiana]))

;; auxiliary function
(defn fetch-execute
  "Fetch and execute the interceptor function"
  [state interceptor branch]
  (->> (list state)
       (apply (branch interceptor))
       (xiana/extract)))
