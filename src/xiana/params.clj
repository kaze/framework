(ns xiana.params
  (:require
    [clojure.walk :refer [keywordize-keys]]
    [ring.middleware.params :as middleware.params]
    [xiana.core :as xiana]))

(def interceptor
  "Update the request map with parsed url-encoded parameters.
  Adds the following keys to the request map:

  :query-params - a map of parameters from the query string
  :form-params  - a map of parameters from the body
  :params       - a merged map of all types of parameter

  Enter: TODO.
  Leave: nil."
  {:enter (fn [state]
            (let [f #(keywordize-keys
                       ((middleware.params/wrap-params identity) %))]
              (xiana/ok
                (update state :request f))))})
