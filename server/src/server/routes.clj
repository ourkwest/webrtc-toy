(ns server.routes
  (:require [com.stuartsierra.component :as c]
            [server.log :as log]
            [yada.yada :as yada]
            [manifold.stream :as s]
            [manifold.deferred :as d]
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


    ;      humans          map (client)          player (client)          server
    ;        |                 |                        |                   |
    ;        |---------------->|                        |                   |
    ;        |----------------------------------------->|                   |
    ;        |                 |                        |                   |
    ;        |            GET session id ---------------------------------->|
    ;        |                 |<-------------------------------------- SSE: begin
    ;        |                 |                        |                   |
    ;        |<------- display session id               |                   |
    ; enter session id -------------------------------->|                   |
    ;        |                 |                 POST session id            |
    ;        |                 |                 & WebRTC Offer ----------->|
    ;        |                 |<--------------------------------- SSE: send WebRTC Offer
    ;        |         POST WebRTC Answer---------------------------------->|
    ;        |                 |                        |<------- Respond WebRTC Answer
    ;        |                 |                        |                   |
    ;        |                 |<---- WebRTC Magic ---->|                   |
    ;        |                 |                        |                   |
    ;        |                 |                        |               SSE: end?
    ;        |                 |                        |                   |

    ; map creates session
    ["/session" (yada/handler
                  (yada/resource
                    {:access-control
                     {:allow-origin "*"}
                     :methods
                     {:get {:produces   "text/event-stream"
                            :parameters {:query {:session-id String}}
                            :response   (fn [ctx]

                                          (let [to-map (s/stream)
                                                session-id (-> ctx :parameters :query :session-id)]
                                            (println "CREATED SESSION: " session-id)

                                            (swap! sessions assoc session-id {:to-map to-map})

                                            (s/connect (s/periodically 60000 (fn [] "<TEST>")) to-map)

                                            (s/on-closed to-map
                                                         #(do
                                                            (swap! sessions dissoc session-id)
                                                            (println "CLOSED SESSION: " session-id)))

                                            to-map)

                                          ;(let [to-map (async/chan)
                                          ;      session-id (-> ctx :parameters :query :session-id)]
                                          ;  (println "/session" session-id)
                                          ;  (swap! sessions assoc session-id {:to-map to-map})
                                          ;
                                          ;  ; asynchronously send test messages down the channel,
                                          ;  ; if they block, kill the session.
                                          ;
                                          ;  to-map)
                                          )}}}))]

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
                        to-player (s/stream)]

                    (swap! sessions assoc-in [session-id player-id] to-player)

                    (println "about to send offer to map:" (str player-id "|" offer))
                    ;(async/>!! to-map (str player-id "|" offer))
                    (s/put! to-map (str "data: " player-id "|" offer"\n\n"))
                    (println "sent offer to map")

                    @(d/chain (s/try-take! to-player 15000)
                              #(if-let [answer %]
                                 (do (println "ANSWER: " answer)
                                     (s/close! to-player)
                                     (assoc response :status 200
                                                     :body answer))
                                 (do (println "no answer")
                                     (assoc response :status 504
                                                     :body "Gateway Timeout"))))

                    ;(let [taken (async/alts!! [to-player (async/timeout 15000)])]
                    ;  (println "TAKEN:" taken)
                    ;  (if-let [answer (first taken)]
                    ;    (do (println "ANSWER: " answer)
                    ;        (async/close! to-player)
                    ;        (assoc response :status 200
                    ;                        :body answer))
                    ;    (do (println "no answer")
                    ;        (assoc response :status 504
                    ;                        :body "Gateway Timeout"))))
                    )
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
                            (s/put! to-player answer)
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
