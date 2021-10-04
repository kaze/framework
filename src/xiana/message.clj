(ns xiana.message
  (:require
    [clojure.pprint :refer [pprint]]
    [xiana.config :as config]
    [xiana.core :refer [ok]]))

(def log
  "Log interceptor.
  Enter: Print 'Enter:' followed by the complete ctx map.
  Leave: Print 'Leave:' followed by the complete ctx map."
  {:enter (fn [ctx] (pprint ["Enter: " ctx]) (ok ctx))
   :leave (fn [ctx] (pprint ["Leave: " ctx]) (ok ctx))})

(defn message
  "This interceptor creates a function that prints predefined message.
  Enter: Print an arbitrary message.
  Leave: Print an arbitrary message."
  [msg]
  {:enter (fn [ctx] (println msg) (ok ctx))
   :leave (fn [ctx] (println msg) (ok ctx))})

(def config
  "Config log interceptor.
  Enter: Print 'Enter:' followed by the complete config map.
  Leave: Print 'Leave:' followed by the complete config map."
  {:enter (fn [ctx] (pprint ["Enter: " @config/-config]) (ok ctx))
   :leave (fn [ctx] (pprint ["Leave: " @config/-config]) (ok ctx))})
