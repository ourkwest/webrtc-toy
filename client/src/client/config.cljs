(ns client.config
  (:require [client.session :as session]))


(def config
  {:server "http://localhost:3210"
   :test true})

(defn test? []
  (:test config))

(defn new-session-id []
  (if (test?)
    (session/schelling-session-id)
    (session/random-session-id)))
