(ns xiana.side-effect-test
  (:require
    [clojure.test :refer [deftest is]]
    [xiana.core :as xiana]
    [xiana.side-effect :as side-effect]
    [xiana.test-helper :as th]))

(def ok-fn
  "Ok response function."
  #(xiana/ok
     (assoc % :response {:status 200, :body "ok"})))

(deftest side-effect-execution
  (let [state {:request     {:uri "/"}
               :side-effect ok-fn}
        ;; bind response using the simulated micro/flow
        response (-> state
                     (th/fetch-execute side-effect/interceptor :leave)
                     (:response))
        ;; expected response
        expected {:status 200, :body "ok"}]
    ;; verify if the response is equal to the expected
    (is (= response expected))))
