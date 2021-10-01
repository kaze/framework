(ns xiana.message-test
  (:require
    [clojure.test :refer [deftest is]]
    [xiana.core :as xiana]
    [xiana.message :as message]))

(deftest log-interceptor-execution
  (let [interceptor message/log
        state {}
        enter ((:enter interceptor) state)
        leave ((:leave interceptor) state)]
    ;; verify log execution
    (is (and (= enter (xiana/ok state))
             (= leave (xiana/ok state))))))

(deftest msg-interceptor-execution
  (let [interceptor (message/message "")
        state {}
        enter ((:enter interceptor) state)
        leave ((:leave interceptor) state)]
    ;; verify msg execution
    (is (and (= enter (xiana/ok state))
             (= leave (xiana/ok state))))))
