(ns xiana.config-test
  (:require
    [clojure.test :refer [deftest is]]
    [xiana.config :as config]))

(def config-map (config/load-config!))

;; test if the configuration map is not empty
(deftest config-map-not-empty
  (is (= (empty? config-map) false)))

;; test if contains an arbitrary key that should not be present
;; if equal to nil -> true, otherwise false
(deftest wrong-key-not-present
  (is (= (:non-existent-key config-map) nil)))

;; test if the default keys are present
(deftest default-keys-are-present
  (is (not (= (and
                (:xiana.app/web-server config-map)
                (:xiana.app/postgresql config-map)
                (:xiana.app/migration config-map)
                (:xiana.app/emails config-map)
                (:xiana.app/auth config-map))
              nil))))

;; test if the web-server map is not empty
(deftest web-server-map-not-empty
  (let [web-server-map (:xiana.app/web-server config-map)]
    (is (= (empty? web-server-map) false))))

;; test if the web-server map contains the expected keys (:port/:join?)
(deftest contain-expected-web-server-map-keys
  (let [web-server-map (:xiana.app/web-server config-map)]
    (is (not (= (and (map? web-server-map)
                     (:port web-server-map)
                     (:join? web-server-map))
                nil)))))

(deftest apply-argument
  (config/load-config! {:xiana_edn_config "test/resources/xiana_test.edn"})
  (is (= "test/resources/xiana_test.edn" (config/get-spec :xiana_edn_config)))
  (is (= "This is a test text" (config/get-spec :test-data))))

(deftest same-result-for-keys-provided-with-name-space
  (config/load-config! {:xiana.app/test-data "This is a test text"
                        :test-data-2          "Second test data"})
  (is (= "This is a test text" (config/get-spec :test-data)))
  (is (= "This is a test text" (config/get-spec :xiana.app/test-data)))
  (is (= "Second test data" (config/get-spec :test-data-2)))
  (is (nil? (config/get-spec :xiana.app/test-data-2))))
