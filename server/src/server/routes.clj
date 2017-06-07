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
                 {:methods {:get {:produces "text/plain"
                                  :response (fn [{:keys [response]}]

                                              (assoc response :status 201
                                                              :body "created")
                                              )}}}))]

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
                                            ;(async/>!! to-map "open...")
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
            (fn [{:keys [response body]}]

              (let [split (.indexOf body "|")
                    session-id (subs body 0 split)
                    offer (subs body (inc split))]

                (println "/offer" body)

                (if-let [to-map (get-in @sessions [session-id :to-map])]

                  (let [player-id (str (UUID/randomUUID))
                        to-player (async/chan)]

                    (println "about to send offer to map:" (str player-id "|" offer))
                    (async/>!! to-map (str player-id "|" offer))
                    (println "sent offer to map")
                    (swap! sessions assoc-in [session-id player-id] to-player)

                    (let [taken (async/alts!! [to-player (async/timeout 15000)])]
                      (println "TAKEN:" taken)
                      (if-let [answer (first taken)]
                        (do (println "ANSWER: " answer)
                            (async/close! to-player)
                            (assoc response :status 200
                                            :body answer))
                        (do (println "no answer")
                            (assoc response :status 504
                                            :body "Gateway Timeout")))))
                  (assoc response :status 404
                                  :body "Room not found."))))}}
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
                  (fn [{:keys [response body]}]
                    (println "/answer" body)
                    (let [[_ session-id player-id answer] (re-find #"([^|]+)\|([^|]+)\|(.*)" body)]
                      (if-let [to-player (get-in @sessions [session-id player-id])]
                        (do (println "Player found in session... answering...")
                            (async/>!! to-player answer)
                            (assoc response :status 200
                                            :body "okay"))
                        (do
                          (println "Player missing from session!")
                          (assoc response :status 404
                                          :body "Room/player not found.")))))}}}))]]])

(defrecord Routes [env]
  c/Lifecycle
  (start [routes]
    (log/info "Starting" routes)
    (assoc routes :routing (get-routes env)))
  (stop [routes]
    (log/info "Stopping" routes)
    (assoc routes :routing nil)))
