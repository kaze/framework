(ns xiana.side-effect
  (:require
    [xiana.core :as xiana]))

(def interceptor
  "Side-effect interceptor.
  Enter: nil.
  Leave: Fetch and execute the state registered
  side-effect procedure, if none was found execute: `xiana/ok`."
  {:leave
   (fn [{side-effect :side-effect :as state}]
     (let [f (or side-effect xiana/ok)]
       (f state)))})
