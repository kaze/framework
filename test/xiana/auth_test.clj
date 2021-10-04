(ns xiana.auth-test
  (:require
    [clojure.test :refer [deftest is]]
    [xiana.auth :as auth]))

(def password "myPersonalPassword!")

(defn testing-ok
  [settings]
  (let [encrypted (auth/make settings password)]
    (is (true? (auth/check settings password encrypted)))))

(defn testing-mistake
  [settings]
  (let [encrypted (auth/make settings password)]
    (is (false? (auth/check settings "myWrongPassword!" encrypted)))))

(deftest test-full-functionality-bcrypt
  (let [fragment {:deps {:auth {:hash-algorithm :bcrypt}}}]
    (testing-mistake fragment)
    (testing-ok fragment)))

(deftest test-full-functionality-script
  (let [fragment {:deps {:auth {:hash-algorithm :scrypt}}}]
    (testing-mistake fragment)
    (testing-ok fragment)))

(deftest test-full-functionality-pbkdf2
  (let [fragment {:deps {:auth {:hash-algorithm :pbkdf2}}}]
    (testing-mistake fragment)
    (testing-ok fragment)))

(deftest test-assert-functionality
  (let [fragment {:deps {:auth {:hash-algorithm :argon2}}}]
    (is (thrown? java.lang.AssertionError (auth/make fragment password)))))