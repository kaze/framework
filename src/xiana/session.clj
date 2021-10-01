(ns xiana.session
  (:require
    [xiana.core :as xiana])
  (:import
    (java.util
      UUID)))

;; define session protocol
(defprotocol Session
  ;; fetch an element (no side effect)
  (fetch [_ k])
  ;; fetch all elements (no side effect)
  (dump [_])
  ;; add an element (side effect)
  (add! [_ k v])
  ;; delete an element (side effect)
  (delete! [_ k])
  ;; erase all elements (side effect)
  (erase! [_]))

(defn init-in-memory
  "Initialize session in memory."
  ([] (init-in-memory (atom {})))
  ([m]
   ;; implement the Session protocol
   (reify Session
     ;; fetch session key:element
     (fetch [_ k] (get @m k))
     ;; fetch all elements (no side effect)
     (dump [_] @m)
     ;; add session key:element
     (add!
       [_ k v]
       (let [k (or k (UUID/randomUUID))]
         (swap! m assoc k v)))
     ;; delete session key:element
     (delete! [_ k] (swap! m dissoc k))
     ;; erase session
     (erase! [_] (reset! m {})))))

(defn interceptor
  "Session interceptor."
  ([] (interceptor (init-in-memory)))
  ([session-instance]
   {:enter
    (fn [{request :request :as state}]
      (let [session-id (try (UUID/fromString
                              (get-in request [:headers :session-id]))
                            (catch Exception _ nil))
            session-data (when session-id
                           (fetch session-instance
                                  session-id))]

        (if session-id
          ;; associate session in state
          (xiana/ok (assoc state :session-data session-data))
          ;; new session
          (xiana/error {:response {:status 403 :body "Session expired"}}))))
    :leave
    (fn [state]
      (let [session-id (get-in state [:session-data :session-id])]
        ;; dissociate session data
        (add! session-instance
              session-id
              (dissoc (:session-data state) :new-session))
        ;; associate the session id
        (xiana/ok
          (assoc-in state
                    [:response :headers "Session-id"]
                    (str session-id)))))}))

(defn user-id-interceptor
  "This interceptor handles the session user id management.
  Enter: Get the session id from the request header, if
  that operation doesn't succeeds a new session is created an associated to the
  current state, otherwise the cached session data is used.
  Leave: Verify if the state has a session id, if so add it to
  the session instance and remove the new session property of the current state.
  The final step is the association of the session id to the response header."
  ([] (user-id-interceptor (init-in-memory)))
  ([session-instance]
   {:enter
    (fn [{request :request :as state}]

      (let [session-id (try (UUID/fromString
                              (get-in request [:headers :session-id]))
                            (catch Exception _ nil))
            session-data (when session-id
                           (fetch session-instance
                                  session-id))]
        (xiana/ok
          (if session-data
            ;; associate session data into state
            (assoc state :session-data session-data)
            ;; else, associate a new session
            (-> (assoc-in state [:session-data :session-id] (UUID/randomUUID))
                (assoc-in [:session-data :new-session] true))))))
    :leave
    (fn [state]
      (let [session-id (get-in state [:session-data :session-id])]
        ;; add the session id to the session instance and
        ;; dissociate the new-session from the current state
        (add! session-instance
              session-id
              (dissoc (:session-data state) :new-session))
        ;; associate the session id
        (xiana/ok
          (assoc-in state
                    [:response :headers :session-id]
                    (str session-id)))))}))

(defn -user-role
  "Update the user role."
  [state role]
  (assoc-in state [:session-data :user] {:role role}))

(defn user-role-interceptor
  "This interceptor updates session data user role:authorization
  from the given request header.
  Enter: Fetch the authorization from its request/state
  if succeeds update the current state with that information,
  also update the user role with a custom value or the default :guest.
  Leave: nil."
  ([]
   (user-role-interceptor -user-role :guest))
  ([f role]
   {:enter
    (fn [{request :request :as state}]
      (let [auth (get-in request [:headers :authorization])]
        (xiana/ok
          (->
            ;; f: function to update/associate the user role
            (f state role)
            ;; associate authorization into session-data
            (assoc-in [:session-data :authorization] auth)))))}))
