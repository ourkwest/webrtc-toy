(ns user
  (:require [com.stuartsierra.component :as c]
            [clojure.tools.namespace.repl :refer [refresh refresh-all set-refresh-dirs]]))


(defn environment []
  {"PORT" 3210})

(def system nil)

(defn init
  "Constructs the current development system."
  []
  (alter-var-root #'system
                  (constantly ((resolve 'server.system/new-system) (environment)))))

(defn start
  "Starts the current development system."
  []
  (alter-var-root #'system c/start))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (alter-var-root #'system
                  (fn [s] (when s (c/stop s)))))

(defn go
  "Initializes the current development system and starts it running."
  []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))

