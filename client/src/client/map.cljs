(ns client.map
  (:require [reagent.core :as reagent :refer [atom]]
            [client.webrtc :as webrtc]
            [client.session :as session]))

(enable-console-print!)

(println "MAP MAP MAP ")

(defonce state (atom {:phase             :startup
                          :session-id    ""
                          :data-channels []}))


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


  (let [{:keys [phase session-id]} @state]


    [:div
     [:h1 "Map"]

     [:div [:span (str "Session: " session-id)]]

     (if (= :startup phase)

       [:div {}
        [:span "Initialising..."]]

       )]

    )

  )



(defn initialise [container]

  (reagent/render-component [render-app] container)

  ;(setup-webrtc)


  ;var source = new EventSource("demo_sse.php");
  ;source.onmessage = function(event) {
  ;                                    document.getElementById("result").innerHTML += event.data + "<br>";
  ;                                    };

  (println "initialising")

  (let [session-id (session/random-session-id)
        sse-url (str "http://localhost:3210/session?session-id=" (session/remove-spaces session-id))
        source (js/EventSource. sse-url)]

    (println session-id)
    (swap! state assoc :session-id session-id)

    (aset source "onmessage" (fn [event]
                               (let [[player-id offer] (re-find #"([^|]+)\|(.*)" (.-data event))]
                                 (println "SSE:" player-id offer)
                                 (webrtc/offer->answer
                                   offer
                                   (fn [answer]
                                     ; post to server
                                     )
                                   (fn [data]
                                     ; recieve
                                     )
                                   (fn [channel]
                                     ; send
                                     )
                                   nil)
                                 ))))

  ; get session from server
  ; wait for SSE session id
  ; wait for SSE offers
  ; on offer
  ;(webrtc/offer->answer offer
  ;                      (fn [answer]
  ;                        ; immediately post session id and answer to server
  ;                        )
  ;                      (fn [data]
  ;                        (println data))
  ;                      (fn [channel]
  ;                        (println "got channel"))
  ;                      nil)


  )