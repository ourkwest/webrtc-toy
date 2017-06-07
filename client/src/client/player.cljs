(ns client.player
  (:require [reagent.core :as reagent :refer [atom]]
            [client.webrtc :as webrtc]
            [client.session :as session]
            [client.server :as server]
            [clojure.string :as string]))

(enable-console-print!)

(println "player ...")

(defonce state (atom {:phase :init}))


(defn post-offer [session-id offer cb]
  (println "POSTING OFFER")
  (server/http-post
    "http://localhost:3210/offer"
    (str (session/remove-spaces session-id) "|" offer)
    cb))


(defn receive-msg [& stuff]
  (println "RECEIVED: " stuff))

(defn join-room []

  (println "join-room")

  (let [session-id (->> (range 0 4)
                        (map #(.-value (.getElementById js/document (str "room-key-" %))))
                        (string/join " "))
        channel (webrtc/offering-channel receive-msg nil)]

    (swap! state assoc :phase :joining)

      (webrtc/make-offer channel
                         (fn [offer]
                           ;; post offer AND session id
                           ;; get answer as response

                           (println "make-offer/cb" session-id offer)

                           (post-offer session-id offer
                                       (fn [answer]

                                         (println "make-offer/cb/post-offer/cb" session-id answer)

                                         (webrtc/accept-answer channel answer))

                                          ))


                         nil)

    ))


(defn render-init []
  [:div
   [:h1 "player"]
   [:div
    [:div "Choose a room: "]
    [:div
     (for [[k words] (map-indexed vector session/options)]
       [:select {:key k :id (str "room-key-" k)}
        (cons [:option {:key "default" :value "placeholder"} ""]
              (for [option words]
                [:option {:key option :value option} option]))])]
    [:div
     [:input {:type    "button"
              :value   "Join room!"
              :on-click join-room}]]]])

(defn render-waiting []
  [:span "Please wait..."])

(defn render-app []

  [:div
   (let [{:keys [phase]} @state]

     (condp = phase
       :init [render-init]
       :joining [render-waiting]))])


(defn initialise [container]

  (reagent/render-component [render-app] container)



  ; TODO: this when the session id is known!
  ;(let [channel (webrtc/offering-channel println nil)]
  ;  (webrtc/make-offer channel
  ;                     (fn [offer]
  ;                       ;; post offer AND session id
  ;                       ;; get answer as response
  ;                       (webrtc/accept-answer channel answer))
  ;                     nil))
  )