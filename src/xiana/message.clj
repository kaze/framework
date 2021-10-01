(ns xiana.message
  (:require
    [clojure.pprint :refer [pprint]]
    [xiana.core :as xiana]))

(def log
  "Log interceptor.
  Enter: Print 'Enter:' followed by the complete state map.
  Leave: Print 'Leave:' followed by the complete state map."
  {:enter (fn [state] (pprint ["Enter: " state]) (xiana/ok state))
   :leave (fn [state] (pprint ["Leave: " state]) (xiana/ok state))})

(defn message
  "This interceptor creates a function that prints predefined message.
  Enter: Print an arbitrary message.
  Leave: Print an arbitrary message."
  [msg]
  {:enter (fn [state] (println msg) (xiana/ok state))
   :leave (fn [state] (println msg) (xiana/ok state))})
