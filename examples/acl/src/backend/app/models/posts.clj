(ns models.posts
  (:require
    [honeysql.helpers :refer [select
                              from
                              where
                              insert-into
                              values
                              left-join
                              sset
                              delete-from]
     :as helpers]
    [xiana.core :as xiana])
  (:import
    (java.util
      UUID)))

(defn fetch-query
  [{{{id :id} :query-params} :request
    :as                      state}]
  (xiana/ok (assoc state :query (cond-> (-> (select :*)
                                            (from :posts))
                                  id (where [:= :id (UUID/fromString id)])))))

(defn add-query
  [{{{user-id :id} :user}             :session-data
    {{content :content} :body-params} :request
    :as                               state}]
  (xiana/ok (assoc state :query (-> (insert-into :posts)
                                    (values [{:content content :user_id user-id}])))))

(defn update-query
  [{{{id :id} :query-params}          :request
    {{content :content} :body-params} :request
    :as                               state}]
  (xiana/ok (assoc state :query (-> (helpers/update :posts)
                                    (where [:= :id (UUID/fromString id)])
                                    (sset {:content content})))))

(defn delete-query
  [{{{id :id} :query-params} :request
    :as                      state}]
  (xiana/ok (assoc state :query (cond-> (delete-from :posts)
                                  id (where [:= :id (UUID/fromString id)])))))

(defn fetch-by-ids-query
  [{{{ids :ids} :body-params} :request
    :as                       state}]
  (xiana/ok (assoc state :query (cond-> (-> (select :*)
                                            (from :posts))
                                  ids (where [:in :id (map #(UUID/fromString %) [ids])])
                                  (coll? ids) (where [:in :id (map #(UUID/fromString %) ids)])))))

(defn fetch-with-comments-query
  [{{{id :id} :query-params} :request
    :as                      state}]
  (xiana/ok (assoc state :query (cond-> (-> (select :*)
                                            (from :posts)
                                            (left-join :comments [:= :posts.id :comments.post_id]))
                                  id (where [:= :posts.id (UUID/fromString id)])))))
