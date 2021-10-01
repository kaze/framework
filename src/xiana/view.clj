(ns xiana.view
  (:require
    [xiana.core :as xiana]))

(def interceptor
  "View interceptor.
  Enter: nil.
  Leave: Fetch and execute the state view registered
  procedure, if none was found execute: `xiana/ok`."
  {:leave
   (fn [{view :view :as state}]
     (let [f (or view xiana/ok)]
       (f state)))})
