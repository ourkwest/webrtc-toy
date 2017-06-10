(ns client.map
  ;(:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [client.webrtc :as webrtc]
            [client.session :as session]
            [client.server :as server]
            [cljs.core.async :as async]))

(enable-console-print!)

(println "MAP MAP MAP ")

(defonce state (atom {:phase         :startup
                      :session-id    ""

                      ;:data-channels []
                      ;:msgs []

                      ;:player-count 0
                      :players {}}))


(defn setup-webrtc []


  ;(let [pc (webrtc/peer-connection)]
  ;
  ;  (println pc)
  ;  )

  ; create offer
  ; post offer and wait for SSE

  ; listen to SSE - recieve answers until start clicked / 4 players received


  )



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
        [:span (str "Players: " (count (keys players)))]]

       )

     [:div


      (for [[player-id {:keys [in]}] players]

        [:div {:key (str "player-" player-id)
               :style {:border "1px solid black"}}

         (for [[index msg] (map-indexed vector in)]
           [:div {:key (str "msg-" index)} msg])

         ]

        )


      ]

     ]

    )

  )


(def conjv (fnil conj []))

(defn initialise [container]

  (reagent/render-component [render-app] container)

  (let [session-id (session/random-session-id)
        sse-url (str "http://localhost:3210/session?session-id=" (session/remove-spaces session-id))
        source (js/EventSource. sse-url)]

    (swap! state assoc :session-id session-id)

    (aset source "onmessage"
          (fn [event]

            (when (not= "<TEST>" (.-data event))
              (let [[_ player-id offer] (re-find #"([^|]+)\|(.*)" (.-data event))
                    c (async/chan)]
                (println "source/onmessage" player-id offer)
                (webrtc/offer->answer
                  offer
                  (fn [answer]
                    (println "source/onmessage/answer-cb" answer)
                    (server/http-post
                      "http://localhost:3210/answer"
                      (str (session/remove-spaces session-id) "|" player-id "|" answer)
                      println))
                  (fn [event]
                    (println "RECEIVED:" (.-data event))
                    (swap! state update-in [:players player-id :in] conjv (.-data event))
                    )
                  (fn [channel]

                    (swap! state assoc-in [:players player-id :out] (fn [outbound-message]
                                                                      (webrtc/send channel outbound-message)))

                    )
                  nil)
                )))))

  )