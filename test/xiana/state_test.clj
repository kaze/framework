(ns xiana.state-test
  (:require
    [clojure.test :refer [deftest is]]
    [xiana.core :as xiana]
    [xiana.state :as state]))

(def state-initial-map
  {:request  {}
   :response {}})

;; test empty state creation
(deftest initial-state
  (let [result (state/make {})
        expected (xiana/map->State state-initial-map)]
    ;; verify if the response and expected value are equal
    (is (= result expected))))
