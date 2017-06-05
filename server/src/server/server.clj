(ns server.server
  (:require [server.log :as log]
            [yada.yada :as yada]
            [bidi.vhosts :refer [vhosts-model]]
            [com.stuartsierra.component :as c]))


(defrecord Server [stop-function env routes]
  c/Lifecycle
  (start [server]
    (log/info "Starting" server)
    (if stop-function
      server
      (let [port (env "PORT")
            vhosts-model (vhosts-model [:* ["" [(:routing routes)
                                                [true (yada/handler nil)]]]])
            listener (yada/listener vhosts-model {:port port})]
        (log/info "Server on port: " port)
        (assoc server :stop-function (:close listener)))))
  (stop [server]
    (log/info "Stopping" server)
    (try
      (when-let [stop-fn (:stop-function server)]
        (stop-fn))
      (catch Exception e
        (log/warn "Possible failure to shutdown server: " (.getMessage e))))
    (assoc server :stop-function nil)))

