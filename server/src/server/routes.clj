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

(defn session [yada-request-context]
  (let [to-party-A (s/stream)
        session-id (-> yada-request-context :parameters :query :session-id)]
    (println "CREATED SESSION: " session-id)
    (swap! sessions assoc session-id {:to-party-A to-party-A})
    (s/connect (s/periodically 3000 (fn [] "data: <PING>\n\n")) to-party-A)
    (s/on-closed to-party-A
                 #(do
                    (swap! sessions dissoc session-id)
                    (println "CLOSED SESSION: " session-id)))
    to-party-A))

(defn offer [{:keys [response body]}]
  (let [[_ session-id offer] (re-find #"([^|]+)\|(.*)" body)]
    (if-let [to-party-A (get-in @sessions [session-id :to-party-A])]
      (let [party-id (str (UUID/randomUUID))
            to-party-B (s/stream)]
        (swap! sessions assoc-in [session-id party-id] to-party-B)
        (s/put! to-party-A (str "data: " party-id "|" offer"\n\n"))
        @(d/chain (s/try-take! to-party-B 15000)
                  #(if-let [answer %]
                     (do (println "ANSWER: " answer)
                         (s/close! to-party-B)
                         (assoc response :status 200
                                         :body answer))
                     (do (println "no answer")
                         (assoc response :status 504
                                         :body "Gateway Timeout")))))
      (assoc response :status 404
                      :body "Room not found."))))

(defn answer [{:keys [response body]}]
  (let [[_ session-id player-id answer] (re-find #"([^|]+)\|([^|]+)\|(.*)" body)]
    (if-let [to-party-B (get-in @sessions [session-id player-id])]
      (do (println "Player found in session... answering...")
          (s/put! to-party-B answer)
          (assoc response :status 200
                          :body "okay"))
      (do (println "Player missing from session!")
          (assoc response :status 404
                          :body "Room/player not found.")))))

(defn get-routes [env]
  [""
   [

    ;      humans        Party A (client)         Party B (client)        server
    ;        |                 |                        |                   |
    ;  open web page --------->|                        |                   |
    ;  open web page ---------------------------------->|                   |
    ;        |                 |                        |                   |
    ;        |            GET session id                |                   |
    ;        |                 └------------------------------------------->|
    ;        |                 ┌<-------------------------------------- SSE: begin
    ;        |                 |                        |                   |
    ;        |<------- display session id               |                   |
    ; enter session id -------------------------------->|                   |
    ;        |                 |         POST session id & WebRTC Offer     |
    ;        |                 |                        └------------------>|
    ;        |                 |                                            |
    ;        |                 |<--------------------------------- SSE: send WebRTC Offer
    ;        |         POST WebRTC Answer --------------------------------->|
    ;        |                 |                                            |
    ;        |                 |                        ┌<----- POST Response WebRTC Answer
    ;        |                 |                        |                   |
    ;        |                 |<---- WebRTC Magic ---->|                   |
    ;        |                 |                        |                   |
    ;        |                 |                        |               SSE: end
    ;        |                 |                        |                   |
    ;        v                 v                        v                   v
    ;
    ; N.B. Multiple instances of Party B are supported for each instance of Party A.

    ; Party A creates session
    ["/session" (yada/handler
                  (yada/resource
                    {:access-control {:allow-origin "*"}
                     :methods {:get {:produces   "text/event-stream"
                                     :parameters {:query {:session-id String}}
                                     :response   session}}}))]

    ; Party B asks to join
    ["/offer"
     (yada/handler
       (yada/resource
         {:access-control {:allow-origin "*"}
          :methods        {:post {:consumes "text/plain"
                                  :response offer}}}))]

    ; Party A responds
    ["/answer"
     (yada/handler
       (yada/resource
         {:access-control {:allow-origin "*"}
          :methods        {:post {:consumes "text/plain"
                                  :response answer}}}))]]])

(defrecord Routes [env]
  c/Lifecycle
  (start [routes]
    (log/info "Starting" routes)
    (assoc routes :routing (get-routes env)))
  (stop [routes]
    (log/info "Stopping" routes)
    (assoc routes :routing nil)))
