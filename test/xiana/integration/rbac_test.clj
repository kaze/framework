(ns xiana.integration.rbac-test
  (:require
    [clj-http.client :as http]
    [clojure.test :refer [deftest is use-fixtures]]
    [tiny-rbac.builder :as b]
    [xiana-fixture :as fixture]
    [xiana.integration.integration-helpers :as test-routes]
    [xiana.rbac :as rbac]
    [xiana.route :as x-routes]
    [xiana.session :as session])
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
      (b/add-permission :member :image :delete :own)))

(def system-config
  {:routes                  test-routes/routes
   :role-set                role-set
   :controller-interceptors [session/interceptor
                             rbac/interceptor]})

(use-fixtures :once (partial fixture/std-system-fixture system-config))

(def guest
  {:users/role :guest
   :users/id   (str (UUID/randomUUID))})

(def member
  {:users/role :member
   :users/id   (str (UUID/randomUUID))})

(deftest delete-request-by-member
  (let [session-id (UUID/randomUUID)]
    (session/add! session/on-demand session-id member)
    (x-routes/reset-routes!)
    (is (= 200
           (:status (http/delete "http://localhost:3333/api/rbac"
                                 {:throw-exceptions false
                                  :headers          {"Session-id" session-id}}))))))

(deftest delete-request-by-guest
  (let [session-id (UUID/randomUUID)]
    (session/add! session/on-demand session-id guest)
    (x-routes/reset-routes!)
    (is (= [403 "Forbidden"]
           (-> (http/delete "http://localhost:3333/api/rbac"
                            {:throw-exceptions false
                             :headers          {"Session-id" session-id}})
               ((juxt :status :body)))))))
