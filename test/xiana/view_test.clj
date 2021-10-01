(ns xiana.view-test
  (:require
    [clojure.test :refer [deftest is]]
    [xiana.test-helper :as th]
    [xiana.view :as view]))

(deftest view-execution
  (let [response (th/fetch-execute {:request :view}
                                   view/interceptor
                                   :leave)
        expected {:request :view}]
    (is (= response expected))))
