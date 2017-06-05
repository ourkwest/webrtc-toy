(ns server.routes
  (:require [com.stuartsierra.component :as c]
            [server.log :as log]
            [yada.yada :as yada]
            [clojure.java.io :as io]
            [clojure.core.async :as async]
            [clojure.string :as string])
  (:import (java.util UUID)))


(def sessions (atom {}))

(defn get-routes [env]
  [""
   [
    ["/favicon.ico" (yada/handler (io/resource "public/favicon.ico"))]
    ["/test" (yada/handler
               (yada/resource
                 {:methods {:get {:produces "test/plain"
                                  :response "Hello"}}}))]

    ; map creates session
    ["/session" (yada/handler
                  (yada/resource
                    {:access-control
                     {:allow-origin "*"}
                     :methods
                     {:get {:produces "text/event-stream"
                            :parameters {:query {:session-id String}}
                            :response (fn [ctx]
                                        (let [to-map (async/chan)
                                              session-id (-> ctx :parameters :query :session-id)]

                                          (println "/session" session-id)
                                          (future
                                            (swap! sessions assoc session-id {:to-map to-map})
                                            (async/>!! to-map "open...")
                                            ;(async/close! to-map)
                                            )
                                          to-map))}}}))]

    ; player asks to join
    ["/offer"
     (yada/handler
       (yada/resource
         {:access-control
          {:allow-origin "*"}
          :methods
          {:post
           {:consumes
            "text/plain"
            :response
            (fn [ctx]

              (let [body (:body ctx)
                    split (.indexOf body "|")
                    session-id (subs body 0 split)
                    offer (subs body (inc split))]

                (println "/offer" body)

                (if-let [to-map (get-in @sessions [session-id :to-map])]

                  (let [player-id (UUID/randomUUID)
                        to-player (async/chan)]

                    (println "about to send offer to map")
                    (async/>!! to-map (str player-id "|" offer))
                    (println "sent offer to map")
                    (swap! sessions assoc-in [session-id player-id] to-player)

                    (if-let [answer (async/alts!! [to-player (async/timeout 5000)])]
                      (do (println "ANSWER: " answer)
                          (async/close! to-player)
                          {:status 200
                           :body answer})
                      (do (println "no answer")
                          {:status 504
                           :body   "Gateway Timeout"}))

                    {:status 200
                     :body   "okay"}
                    )

                  {:status 404
                   :body   "Room not found."})))}}
          ; params: offer
          ; generate player-id
          ; add player channel to sessions/session-id/player-id
          ; send player-id and offer down sessions/session-id/channel to map
          ; wait for player channel to recieve answer and then send that back
          }
         ))]

    ; map responds
    ["/answer"
     (yada/handler
       (yada/resource
         ; params: answer player-id
         ; send answer down channel at sessions/session-id/player-id
         {:access-control
          {:allow-origin "*"}
          :methods
          {:post {:consumes
                  "text/plain"
                  :response
                  (fn [ctx]
                    (println "/answer" (:body ctx))
                    (let [[_ session-id player-id answer] (re-find #"([^|]+)\|([^|]+)\|(.*)" (:body ctx))]
                      (if-let [to-player (get-in @sessions [session-id player-id])]
                        (do (async/>!! to-player answer)
                            {:status 200
                             :body "okay"})
                        {:status 404
                         :body   "Room/player not found."})))}}}))]]])

(defrecord Routes [env]
  c/Lifecycle
  (start [routes]
    (log/info "Starting" routes)
    (assoc routes :routing (get-routes env)))
  (stop [routes]
    (log/info "Stopping" routes)
    (assoc routes :routing nil)))
