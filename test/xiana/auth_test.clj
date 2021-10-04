(ns xiana.auth-test
  (:require
    [clojure.test :refer [deftest is]]
    [xiana.auth :as auth]
    [xiana.config :as config])
  (:import
    (clojure.lang
      ExceptionInfo)))

(def password "myPersonalPassword!")

(defn testing-ok
  []
  (let [encrypted (auth/make password)]
    (is (true? (auth/check password encrypted)))))

(defn testing-mistake
  []
  (let [encrypted (auth/make password)]
    (is (false? (auth/check "myWrongPassword!" encrypted)))))

(deftest test-full-functionality-bcrypt
  (config/load-config {:xiana.app/auth {:hash-algorithm :bcrypt}})
  (testing-mistake)
  (testing-ok))

(deftest test-full-functionality-script
  (config/load-config {:xiana.app/auth {:hash-algorithm :scrypt}})
  (testing-mistake)
  (testing-ok))

(deftest test-full-functionality-pbkdf2
  (testing-mistake)
  (testing-ok))

(deftest test-not-supported-hash-algorithm
  (config/load-config {:xiana.app/auth {:hash-algorithm :argon2}})
  (is (thrown? ExceptionInfo #"Not supported hashing algorithm found"
               {:hash-algorithm :argon2}
               (auth/make password))))
