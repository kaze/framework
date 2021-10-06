(ns xiana.core
  (:require
    [cats.core :as m]
    [cats.monad.either :as me]))

;; context record definition
(defrecord Context
  [request request-data response session-data])

;; monad.either/right container alias
;; don't stop the sequence of executions, continue! (implicit)
(def ok me/right)

;; monad.either/left container alias
;; and stop the sequence of executions (implicit)
;; used by >>= function
(def error me/left)

;; monad.extract alias
;; unwrap monad container
(def extract m/extract)

(defmacro apply-flow->
  "Simple macro that applies Haskell-style left associative bind to a
  queue of functions."
  [ctx & queue]
  `(apply m/>>= (ok ~ctx) ~@queue))

(defmacro flow->
  "Expand a single form to (form) or the sequence of forms to:
  (lambda (x) (form1 x), (lambda (x) (form2 x)) ...
  and perform Haskell-style left-associative bind using the
  monad.either/right context (wrapped)."
  [ctx & forms]
  `(m/>>=
     (ok ~ctx)
     ~@(for [form forms]
         (if (seq? form)
           `(fn [~'x] (~(first form) ~'x ~@(rest form)))
           form))))
