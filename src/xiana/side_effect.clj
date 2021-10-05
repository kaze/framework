(ns xiana.side-effect
  (:require
    [xiana.core :refer [ok]]))

(def interceptor
  "Side-effect interceptor.
  Enter: nil.
  Leave: Fetch and execute the state registered
  side-effect procedure, if none was found execute: `xiana/ok`."
  {:leave
   (fn [{side-effect :side-effect :as ctx}]
     (let [f (or side-effect ok)]
       (f ctx)))})
