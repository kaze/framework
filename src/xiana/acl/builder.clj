(ns xiana.acl.builder
  (:require
    [xiana.acl.builder.permissions :as abp]
    [xiana.acl.builder.roles :as abr]
    [xiana.core :as xiana]))

(defn init
  [this config]
  (xiana/flow->
    this
    (abp/init (:acl/permissions config))
    (abr/init (:acl/roles config))))
