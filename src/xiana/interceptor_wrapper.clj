(ns xiana.interceptor-wrapper
  (:require
    [xiana.core :refer [ok error]]))

(defn interceptor
  "Interceptor wrapper to use xiana monad."
  [in]
  (cond-> {}
    (:enter in) (assoc :enter (fn [ctx] (ok ((:enter in) ctx))))
    (:leave in) (assoc :leave (fn [ctx] (ok ((:leave in) ctx))))
    (:error in) (assoc :error (fn [ctx] (error ((:error in) ctx))))))

(defn- middleware-fn
  "Simple enter/leave middleware function generator."
  [m k]
  (fn [{r k :as ctx}]
    (ok (-> ctx (assoc k (m r))))))

(defn middleware->enter
  "Parse middleware function to interceptor enter lambda function."
  ([middleware]
   (middleware->enter {} middleware))
  ([interceptor middleware]
   (let [m (middleware identity)
         f (middleware-fn m :request)]
     (-> interceptor (assoc :enter f)))))

(defn middleware->leave
  "Parse middleware function to interceptor leave lambda function."
  ([middleware]
   (middleware->leave {} middleware))
  ([interceptor middleware]
   (let [m (middleware identity)
         f (middleware-fn m :response)]
     (-> interceptor (assoc :leave f)))))
