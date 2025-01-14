(ns controllers.users
  (:require
    [models.data-ownership :as owner]
    [models.users :as model]
    [views.users :as views]
    [xiana.core :as xiana]))

(defn fetch
  [state]
  (xiana/flow->
    (assoc state :view views/fetch-users)
    model/fetch-query))

(defn add
  [state]
  (xiana/flow->
    (assoc state :view views/fetch-users)
    model/add-query))

(defn update-user
  [state]
  (xiana/flow->
    (assoc state :view views/fetch-users)
    model/update-query
    owner/owner-fn))

(defn delete-user
  [state]
  (xiana/flow->
    (assoc state :view views/fetch-users)
    model/delete-query
    owner/owner-fn))

(defn fetch-with-posts-comments
  [state]
  (xiana/flow->
    (assoc state :view views/fetch-posts-comments)
    model/fetch-with-post-comments-query
    owner/owner-fn))

(defn fetch-with-posts
  [state]
  (xiana/flow->
    (assoc state :view views/fetch-posts)
    model/fetch-with-post-query
    owner/owner-fn))
