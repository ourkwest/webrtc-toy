(ns server.system
  (:require [com.stuartsierra.component :as c]
            [server.server :refer [map->Server]]
            [server.routes :refer [map->Routes]]))


(defn new-system [environment]
  (c/system-map
    :env environment
    :routes (c/using (map->Routes {}) [:env])
    :server (c/using (map->Server {}) [:env :routes])))