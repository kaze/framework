(ns xiana.session-test
  (:require
    [clojure.test :refer [deftest is]]
    [xiana.session :as session]
    [xiana.test-helper :as th])
  (:import
    (java.util
      UUID)))

;; initial session-instance instance
(def session-instance (session/init-in-memory))

;; test add and fetch reify implementations
(deftest session-protocol-add!-fetch
  (let [user-id (UUID/randomUUID)]
    ;; add user id
    (session/add! session-instance :user-id {:id user-id})
    ;; verify if user ids are equal
    (is (= {:id user-id}
           (session/fetch session-instance :user-id)))))

;; test delete! reify implementation
(deftest session-protocol-delete!
  (let [_ (session/fetch session-instance :user-id)]
    ;; remove user identification
    (session/delete! session-instance :user-id)
    ;; verify if was removed
    (is (nil? (session/fetch session-instance :user-id)))))

;; test erase reify implementation
(deftest session-protocol-add!-erase!
  (let [user-id (UUID/randomUUID)
        session-id (UUID/randomUUID)]
    ;; add session instance values
    (session/add! session-instance :user-id {:id user-id})
    (session/add! session-instance :session-id {:id session-id})
    ;; verify if the values exists
    (is (= {:id user-id} (session/fetch session-instance :user-id)))
    (is (= {:id session-id} (session/fetch session-instance :session-id)))
    ;; erase and verify if session instance is empty
    (is (empty? (session/erase! session-instance)))))

(def simple-request
  "Simple/minimal request example."
  {:uri "/" :request-method :get})

(def sample-session-id
  "Sample session id."
  "21f0d6e6-3782-465a-b903-ca84f6f581a0")

(def sample-authorization
  "Sample authorization data."
  "auth")

(def sample-request
  "Sample request example."
  {:uri "/"
   ;; method example: GET
   :request-method :get
   ;; header example with session id and authorization
   :headers {:session-id sample-session-id
             :authorization sample-authorization}})

(def sample-state
  "State with the sample request."
  {:request sample-request})

(def simple-state
  "State with the simple/minimal request."
  {:request simple-request})

(def session-user-role
  "Instance of the session user role interceptor."
  (session/user-role-interceptor))

(deftest contains-session-user-role
  ;; compute a single interceptor semi cycle (enter-leave-enter)
  (let [sample-resp (th/fetch-execute sample-state session-user-role :enter)
        simple-resp (th/fetch-execute simple-state session-user-role :enter)]
    ;; verify if has the user and the right authorization strings
    (is (and (= (get-in sample-resp [:session-data :user :role]) :guest)
             (= (get-in sample-resp [:session-data :authorization]) "auth")))
    ;; verify if has the user and the right authorization is nil
    (is (and (= (get-in simple-resp [:session-data :user :role]) :guest)
             (nil? (get-in simple-resp [:session-data :authorization]))))))

(def session-user-id
  "Instance of the session user id interceptor."
  (session/user-id-interceptor))

;; test if the session-user-id handles new sessions
(deftest contains-new-session
  (let [session-data (-> {}
                         (th/fetch-execute session-user-id :enter)
                         (:session-data))]
    ;; verify if session-id was registered
    (is (= (:new-session session-data) true))))

(deftest persiste-session-id
  ;; compute a single interceptor semi cycle (enter-leave-enter)
  (let [enter-resp (th/fetch-execute sample-state session-user-id :enter)
        leave-resp (th/fetch-execute enter-resp session-user-id :leave)
        header     (get-in leave-resp [:response :headers])
        new-state  (assoc-in sample-state [:request :headers] header)
        last-resp  (th/fetch-execute new-state session-user-id :enter)]
    ;; verify if the uuid strings are equal
    (is (= (get-in last-resp [:request :headers :session-id])
           (.toString (get-in last-resp [:session-data :session-id]))))))

(deftest on-demand-session-protocol-add!-dump-delete!-erase!
  (let [user-id (UUID/randomUUID)
        session-id (UUID/randomUUID)]
    ;; add session instance values
    (session/add! session/on-demand :user-id {:id user-id})
    (session/add! session/on-demand :session-id {:id session-id})
    ;; verify if the values exists
    (is (= {:id user-id} (session/fetch session/on-demand :user-id)))
    (is (= {:id session-id} (session/fetch session/on-demand :session-id)))
    (is (= {:session-id {:id session-id}
            :user-id    {:id user-id}}
           (session/dump session/on-demand)))
    (is (= {:user-id {:id user-id}}
           (session/delete! session/on-demand :session-id)))
    ;; erase and verify if session instance is empty
    (is (empty? (session/erase! session/on-demand)))))