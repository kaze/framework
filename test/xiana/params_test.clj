(ns xiana.params-test
  (:require
    [clojure.test :refer [deftest is]]
    [xiana.params :as params]
    [xiana.test-helper :as th]))

(deftest params-execution
  (let [ctx {}
        request (-> ctx (th/fetch-execute params/interceptor :enter))
        expected {:request {:form-params {},
                            :params {},
                            :query-params {}}}]
           ;; expected request value?
    (is (= request expected))))
