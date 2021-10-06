(ns xiana.message-test
  (:require
    [clojure.test :refer [deftest is]]
    [xiana.core :as xiana]
    [xiana.message :as message]))

(deftest log-interceptor-execution
  (let [interceptor message/log
        ctx {}
        enter ((:enter interceptor) ctx)
        leave ((:leave interceptor) ctx)]
    ;; verify log execution
    (is (and (= enter (xiana/ok ctx))
             (= leave (xiana/ok ctx))))))

(deftest msg-interceptor-execution
  (let [interceptor (message/message "")
        ctx {}
        enter ((:enter interceptor) ctx)
        leave ((:leave interceptor) ctx)]
    ;; verify msg execution
    (is (and (= enter (xiana/ok ctx))
             (= leave (xiana/ok ctx))))))
