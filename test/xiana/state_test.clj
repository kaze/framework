(ns xiana.state-test
  (:require
    [clojure.test :refer [deftest is]]
    [xiana.context :as context]
    [xiana.core :as xiana]))

(def context-initial-map
  {:request  {}
   :response {}})

;; test empty context creation
(deftest initial-context
  (let [result (context/make {})
        expected (xiana/map->Context context-initial-map)]
    ;; verify if the response and expected value are equal
    (is (= result expected))))
