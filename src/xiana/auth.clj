(ns xiana.auth
  (:require
    [crypto.password.bcrypt :as hash-b]
    [crypto.password.pbkdf2 :as hash-p]
    [crypto.password.scrypt :as hash-s]
    [xiana.config :as config]))

(defn dispatch
  [& _]
  (get (config/get-spec :auth) :hash-algorithm))

(defmulti make dispatch)

(defmethod make :bcrypt
  [password]
  (let [{:keys [bcrypt-settings]
         :or   {bcrypt-settings {:work-factor 11}}} (config/get-spec :auth)]
    (hash-b/encrypt password (:work-factor bcrypt-settings))))

(defmethod make :scrypt
  [password]
  (let [{:keys [scrypt-settings]
         :or   {scrypt-settings {:cpu-cost        32768
                                 :memory-cost     8
                                 :parallelization 1}}} (config/get-spec :auth)]

    (hash-s/encrypt
      password
      (:cpu-cost scrypt-settings)
      (:memory-cost scrypt-settings)
      (:parallelization scrypt-settings))))

(defmethod make :pbkdf2
  [password]
  (let [{:keys [pbkdf2-settings]
         :or   {pbkdf2-settings {:type       :sha1
                                 :iterations 100000}}} (config/get-spec :auth)]
    (hash-p/encrypt
      password
      (:iterations pbkdf2-settings)
      (if (= :sha1 (:type pbkdf2-settings))
        "HMAC-SHA1" "HMAC-SHA256"))))

(defmethod make :default
  [_]
  (throw (ex-info "not supported hashing algorithm found"
                  {:hash-algorithm (dispatch)})))

(defmulti check dispatch)

(defmethod check :bcrypt
  [password encrypted]
  (hash-b/check password encrypted))

(defmethod check :scrypt
  [password encrypted]
  (hash-s/check password encrypted))

(defmethod check :pbkdf2
  [password encrypted]
  (hash-p/check password encrypted))

(defmethod check :default
  [_ _]
  (throw (ex-info "Not supported hashing algorithm found"
                  {:hash-algorithm (get (config/get-spec :auth) :hash-algorithm)})))
