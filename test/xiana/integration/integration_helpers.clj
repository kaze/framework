(ns xiana.integration.integration-helpers
  (:require
    [honeysql.helpers :as sql]
    [xiana.core :as xiana]
    [xiana.session :as session]
    [xiana.webserver :as ws]))

(def ok-action
  "Auxiliary ok container function."
  #(xiana/ok
     (assoc % :response {:status 200, :body "ok"})))

(defn restriction-fn
  [ctx]
  (let [user-permissions (get-in ctx [:request-data :user-permissions])]
    (cond
      (user-permissions :image/all) (xiana/ok ctx)
      (user-permissions :image/own) (xiana/ok
                                      (let [session-id (get-in ctx [:request :headers "session-id"])
                                            user-id (:users/id (session/fetch session/on-demand session-id))]
                                        (update ctx :query sql/merge-where [:= :owner.id user-id]))))))

(defn delete-action [ctx]
  (xiana/ok
    (-> ctx
        (assoc :query {:delete [:*]
                       :from   [:images]
                       :where  [:= :id (get-in ctx [:params :image-id])]})
        (assoc-in [:request-data :restriction-fn] restriction-fn))))

(def routes
  [["/api" {:handler ws/handler-fn}
    ["/interceptor" {:get {:action ok-action}}]
    ["/rbac" {:delete {:action     delete-action
                       :permission :image/delete}}]]])
