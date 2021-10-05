(ns xiana.rbac
  (:require
    [tiny-rbac.builder :as b]
    [tiny-rbac.core :as c]
    [xiana.config :as config]
    [xiana.core :as xiana]
    [xiana.session :as session]))

(defonce -role-set (atom nil))

(defn reset-role-set!
  []
  (reset! -role-set nil))

(defn role-set
  []
  (or @-role-set
      (reset! -role-set (b/init (config/get-spec :role-set)))))

(defn permissions
  [ctx]
  (let [rs (role-set)
        session-id (get-in ctx [:request :headers "session-id"])
        session-backend (-> ctx :deps :session-backend)
        user (session/fetch session-backend session-id)
        role (:users/role user)
        permit (get-in ctx [:request-data :permission])
        resource (keyword (namespace permit))
        action (keyword (name permit))
        permissions (c/permissions rs role resource action)]
    (into #{} (map #(keyword (str (name resource) "/" (name %))) permissions))))

(def interceptor
  {:enter (fn [ctx]
            (let [operation-restricted (get-in ctx [:request-data :permission])
                  permits (and operation-restricted (permissions ctx))]
              (cond
                (and operation-restricted (empty? permits)) (xiana/error {:response {:status 403 :body "Forbidden"}})
                operation-restricted (xiana/ok (assoc-in ctx [:request-data :user-permissions] permits))
                :else (xiana/ok ctx))))
   :leave (fn [ctx]
            (if-let [restriction-fn (get-in ctx [:request-data :restriction-fn])]
              (restriction-fn ctx)
              (xiana/ok ctx)))})
