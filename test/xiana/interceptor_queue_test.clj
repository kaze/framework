(ns xiana.interceptor-queue-test
  (:require
    [clojure.test :refer [deftest is]]
    [xiana.core :as xiana]
    [xiana.interceptor-queue :as queue]))

(def A-interceptor
  {:enter (fn [ctx] (xiana/ok (assoc ctx :enter "A-enter")))
   :leave (fn [ctx] (xiana/ok (assoc ctx :leave "A-leave")))})

(def B-interceptor
  {:enter (fn [ctx] (xiana/ok (assoc ctx :enter "B-enter")))
   :leave (fn [ctx] (xiana/ok (assoc ctx :leave "B-leave")))})

(def C-interceptor
  {:enter (fn [ctx] (xiana/ok (assoc ctx :enter "C-enter")))
   :leave (fn [ctx] (xiana/ok (assoc ctx :leave "C-leave")))})

;; Exception
(def D-interceptor
  {:enter (fn [_] (throw (Exception. "enter-exception")))})

;; Error/Exception
(def E-interceptor
  {:enter (fn [_] (throw (Exception. "enter-exception")))
   :leave (fn [_] (throw (Exception. "leave-exception")))
   :error (fn [ctx] (xiana/ok (assoc ctx :error "Error")))})

(def F-interceptor
  {:enter (fn [ctx] (xiana/ok (assoc ctx :enter "F-enter")))
   :leave (fn [ctx] (xiana/ok (assoc ctx :leave "F-leave")))})

(def default-interceptors
  "Default interceptors."
  [A-interceptor])

(def inside-interceptors
  {:inside [B-interceptor]})

(def around-interceptors
  {:around [C-interceptor]})

(def both-interceptors
  {:inside [B-interceptor]
   :around [C-interceptor]})

(def override-interceptors
  [F-interceptor])

(def ok-action
  "Auxiliary ok container function."
  #(xiana/ok
     (assoc % :response {:status 200, :body "ok"})))

(def error-action
  "Auxiliary error container function."
  #(xiana/error
     (assoc % :response {:status 500 :body "Internal Server error"})))

(defn make-context
  "Return a simple context request data."
  [action interceptors]
  {:request-data
   {:action action
    :interceptors interceptors}})

;; test a simple interceptor queue ok execution
(deftest queue-simple-ok-execution
  ;; construct a simple request data context
  (let [ctx (make-context ok-action [])
        ;; get response using a simple micro flow
        response (-> ctx
                     (queue/execute [])
                     (xiana/extract)
                     (:response))
        expected {:status 200, :body "ok"}]
    ;; verify if response is equal to the expected
    (is (= response expected))))

;; test a simple interceptor queue execution
(deftest queue-simple-error-execution
  ;; construct a simple request data context
  (let [ctx (make-context error-action [])
        ;; get response using a simple micro flow
        response (-> ctx
                     (queue/execute [])
                     (xiana/extract)
                     (:response))
        expected {:status 500 :body "Internal Server error"}]
    ;; verify if response is equal to the expected
    (is (= response expected))))

;; test default interceptors queue execution
(deftest queue-default-interceptors-execution
  ;; construct a simple request data context
  (let [ctx (make-context ok-action nil)
        ;; get response using a simple micro flow
        result (-> ctx
                   (queue/execute default-interceptors)
                   (xiana/extract))
        response (:response result)
        expected {:status 200 :body "ok"}
        enter (:enter result)
        leave (:leave result)]
    ;; verify if response is equal to the expected
    (is (and
          (= enter "A-enter")
          (= leave "A-leave")
          (= response expected)))))

;; test a simple interceptor error queue execution
(deftest queue-interceptor-exception-default-execution
  ;; construct a simple request data context
  (let [ctx (make-context ok-action [D-interceptor])
        ;; get error response using a simple micro flow
        cause (-> ctx
                  (queue/execute [])
                  (xiana/extract)
                  (:response)
                  (:body)
                  (:cause))
        expected "enter-exception"]
    ;; verify if cause is equal to the expected
    (is (= cause expected))))

;; test a simple interceptor error queue execution
(deftest queue-interceptor-error-exception-execution
  ;; construct a simple request data context
  (let [ctx (make-context ok-action [E-interceptor])
        ;; get error response using a simple micro flow
        error (-> ctx
                  (queue/execute [])
                  (xiana/extract)
                  (:error))
        expected "Error"]
    ;; verify if error is equal to the expected
    (is (= error expected))))

;; test inside interceptors queue execution
(deftest queue-inside-interceptors-execution
  ;; construct a simple request data context
  (let [ctx (make-context ok-action inside-interceptors)
        ;; get response using a simple micro flow
        result (-> ctx
                   (queue/execute default-interceptors)
                   (xiana/extract))
        response (:response result)
        expected {:status 200 :body "ok"}
        last-enter (:enter result)
        last-leave (:leave result)]
    ;; verify if response is equal to the expected
    (is (and
          (= last-enter "B-enter")
          (= last-leave "A-leave")
          (= response expected)))))

;; test around interceptors queue execution
(deftest queue-around-interceptors-execution
  ;; construct a simple request data context
  (let [ctx (make-context ok-action around-interceptors)
        ;; get response using a simple micro flow
        result (-> ctx
                   (queue/execute default-interceptors)
                   (xiana/extract))
        response (:response result)
        expected {:status 200 :body "ok"}
        last-enter (:enter result)
        last-leave (:leave result)]
    ;; verify if response is equal to the expected
    (is (and
          (= last-enter "A-enter")
          (= last-leave "C-leave")
          (= response expected)))))

;; test inside/around interceptors queue execution
(deftest queue-both-interceptors-execution
  ;; construct a simple request data context
  (let [ctx (make-context ok-action both-interceptors)
        ;; get response using a simple micro flow
        result (-> ctx
                   (queue/execute default-interceptors)
                   (xiana/extract))
        response (:response result)
        expected {:status 200 :body "ok"}
        last-enter (:enter result)
        last-leave (:leave result)]
    ;; verify if response is equal to the expected
    (is (and
          (= last-enter "B-enter")
          (= last-leave "C-leave")
          (= response expected)))))

;; test override interceptors queue execution
(deftest queue-override-interceptors-execution
  ;; construct a simple request data context
  (let [ctx (make-context ok-action override-interceptors)
        ;; get response using a simple micro flow
        result (-> ctx
                   (queue/execute default-interceptors)
                   (xiana/extract))
        response (:response result)
        expected {:status 200 :body "ok"}
        last-enter (:enter result)
        last-leave (:leave result)]
    ;; verify if response is equal to the expected
    (is (and
          (= last-enter "F-enter")
          (= last-leave "F-leave")
          (= response expected)))))
