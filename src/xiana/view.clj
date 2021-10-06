(ns xiana.view
  (:require
    [xiana.core :refer [ok]]))

(def interceptor
  "View interceptor.
  Enter: nil.
  Leave: Fetch and execute the context view registered
  procedure, if none was found execute: `xiana/ok`."
  {:leave
   (fn [{view :view :as ctx}]
     (let [f (or view ok)]
       (f ctx)))})
