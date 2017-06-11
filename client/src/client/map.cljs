(ns client.map
  ;(:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [client.webrtc :as webrtc]
            [client.session :as session]
            [client.server :as server]
            [client.config :as config]
            [cljs.core.async :as async]
            [clojure.string :as string]))

(enable-console-print!)

(println "MAP MAP MAP ")

(defonce state (atom {:phase         :startup
                      :session-id    ""
                      :players {}}))

; send welcome message to players
; start game button
;    close SSE
;    send messages to players


(defn render-app []
  (let [{:keys [phase session-id players]} @state]
    [:div
     [:h1 "Map"]
     [:div
      [:span (str "Session: " session-id)]]
     (if (= :startup phase)
       [:div {}
        [:span "Waiting for players to join..."]
        [:br]
        [:span (str "Players: " (count (keys players)))]])
     [:div
      (for [[player-id {:keys [in name]}] players]
        [:div {:key (str "player-" player-id)
               :style {:border "1px solid black"}}
         [:div name]
         (for [[index msg] (map-indexed vector in)]
           [:div {:key (str "msg-" index)} msg])])]]))


(def conjv (fnil conj []))

(defn receive [player-id message-type message]
  (condp = message-type
    "HELLO" (do (println message)
                (swap! state assoc-in [:players player-id :name] message)
                ((get-in @state [:players player-id :out]) "HELLO" (str "Welcome " message "!")))
    "MOVE" (swap! state update-in [:players player-id :in] conjv message))
  )

(defn initialise [container]

  (reagent/render-component [render-app] container)

  (let [session-id (if (config/test?)
                     (session/schelling-session-id)
                     (session/random-session-id))
        sse-url (str (:server config/config) "/session?session-id=" (session/remove-spaces session-id))
        source (js/EventSource. sse-url)]

    (swap! state assoc :session-id session-id)

    (aset source "onmessage"
          (fn [event]
            (when (not= "<PING>" (.-data event))
              (let [[_ player-id offer] (re-find #"([^|]+)\|(.*)" (.-data event))
                    c (async/chan)]
                (println "source/onmessage" player-id offer)
                (webrtc/offer->answer
                  offer
                  (fn [answer]
                    (println "source/onmessage/answer-cb" answer)
                    (server/http-post
                      (str (:server config/config) "/answer")
                      (str (session/remove-spaces session-id) "|" player-id "|" answer)
                      println))
                  (fn [channel]
                    (webrtc/on-message channel
                                       (fn [event]
                                         (println "RECEIVED:" (.-data event))
                                         (let [[_ message-type message] (re-find #"(.*?):(.*)" (.-data event))]
                                           (receive player-id message-type message))))
                    (swap! state assoc-in [:players player-id :out]
                           (fn [msg-type outbound-message]
                             (webrtc/send channel (str msg-type ":" outbound-message)))))
                  nil)
                )))))

  )