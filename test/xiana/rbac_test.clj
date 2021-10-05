(ns xiana.rbac-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [honeysql.helpers :as sql]
    [tiny-rbac.builder :as b]
    [xiana.config :as config]
    [xiana.core :as xiana]
    [xiana.rbac :refer [interceptor reset-role-set!]])
  (:import
    (java.util
      UUID)))

(def role-set
  (-> (b/add-resource {} :image)
      (b/add-action :image [:upload :download :delete])
      (b/add-role :guest)
      (b/add-inheritance :member :guest)
      (b/add-permission :guest :image :download :all)
      (b/add-permission :member :image :upload :all)
      (b/add-permission :member :image :delete :own)
      (b/add-inheritance :mixed :member)
      (b/add-permission :mixed :image :delete :all)))

(def guest
  {:users/role :guest
   :users/id   (str (UUID/randomUUID))})

(def member
  {:users/role :member
   :users/id   (str (UUID/randomUUID))})

(def mixed
  {:users/role :mixed
   :users/id   (str (UUID/randomUUID))})

(defn state
  [user permission]
  (let [session-id (str (UUID/randomUUID))]
    (config/load-config! {:role-set role-set})
    (reset-role-set!)
    (-> (assoc-in {} [:request-data :permission] permission)
        (assoc :session-data user)
        (assoc-in [:request :headers "session-id"] session-id))))

(deftest user-permissions
  (is (= #{:image/all}
         (-> ((:enter interceptor) (state guest :image/download))
             xiana/extract
             :request-data
             :user-permissions)))
  (is (= #{:image/all}
         (-> ((:enter interceptor) (state member :image/download))
             xiana/extract
             :request-data
             :user-permissions)))
  (is (= #{:image/all}
         (-> ((:enter interceptor) (state member :image/upload))
             xiana/extract
             :request-data
             :user-permissions)))
  (is (= #{:image/own}
         (-> ((:enter interceptor) (state member :image/delete))
             xiana/extract
             :request-data
             :user-permissions)))
  (is (= {:status 403 :body "Forbidden"}
         (-> ((:enter interceptor) (state guest :image/upload))
             xiana/extract
             :response))))

(defn restriction-fn
  [ctx]
  (let [user-permissions (get-in ctx [:request-data :user-permissions])]
    (cond
      (user-permissions :image/all) (xiana/ok ctx)
      (user-permissions :image/own) (xiana/ok
                                      (let [user-id (get-in ctx [:session-data :users/id])]
                                        (update ctx :query sql/merge-where [:= :owner.id user-id])))
      :else (xiana/error (assoc ctx :response {:status 403 :body "Invalid permission request"})))))

(defn action [state image-id]
  (xiana/ok (-> state
                (assoc :query {:delete [:*]
                               :from   [:images]
                               :where  [:= :id image-id]})
                (assoc-in [:request-data :restriction-fn] restriction-fn))))

(deftest restrictions
  (config/load-config! {:role-set role-set})
  (testing "restriction for own images"
    (let [user member
          image-id (str (UUID/randomUUID))]
      (is (= {:delete [:*],
              :from   [:images],
              :where  [:and
                       [:= :id image-id]
                       [:= :owner.id (:users/id user)]]}
             (-> (xiana/flow-> (state user :image/delete)
                               ((:enter interceptor))
                               (action image-id)
                               ((:leave interceptor)))
                 xiana/extract
                 :query))
          "Add filter if user has restricted to ':own'")))
  (testing "forbidden action"
    (let [user guest
          image-id (str (UUID/randomUUID))]
      (is (= {:body   "Forbidden"
              :status 403}
             (-> (xiana/flow-> (state user :image/delete)
                               ((:enter interceptor))
                               (action image-id)
                               ((:leave interceptor)))
                 xiana/extract
                 :response))
          "Returns 403: Forbidden if action is forbidden")))
  (testing "multiple ownership"
    (let [user mixed
          image-id (str (UUID/randomUUID))]
      (is (= {:delete [:*],
              :from   [:images],
              :where  [:= :id image-id]}
             (-> (xiana/flow-> (state user :image/delete)
                               ((:enter interceptor))
                               (action image-id)
                               ((:leave interceptor)))
                 xiana/extract
                 :query))
          "Processing multiple restrictions in order")))
  (testing "not defined permission"
    (let [state (-> (state {} :image/delete)
                    (assoc-in [:request-data :user-permissions] #{:image/none})
                    (assoc-in [:request-data :restriction-fn] restriction-fn))]
      (is (= {:status 403, :body "Invalid permission request"}
             (-> (xiana/flow-> state
                               ((:leave interceptor)))
                 xiana/extract
                 :response))
          "Returns xiana/error when user-permission missing"))))

