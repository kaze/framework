(ns xiana.app
  (:require
    [xiana.config :as config]
    [xiana.webserver :as ws]))

(defn start
  [config]
  (config/load-config! config)
  (ws/start))
