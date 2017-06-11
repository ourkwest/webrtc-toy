(ns client.player
  (:require [reagent.core :as reagent :refer [atom]]
            [client.webrtc :as webrtc]
            [client.session :as session]
            [client.server :as server]
            [client.config :as config]
            [clojure.string :as string]))

(enable-console-print!)

(println "player ...")

(defonce state (atom {:phase :init
                      :name ""
                      :channel nil
                      :msgs []}))


(defn post-offer [session-id offer cb]
  (println "POSTING OFFER")
  (server/http-post
    (str (:server config/config) "/offer")
    (str (session/remove-spaces session-id) "|" offer)
    cb))


(defn receive-msg [event]
  (swap! state update :msgs conj (.-data event))
  (println "RECEIVED: " (.-data event)))

(defn join-room []

  (println "join-room")

  (let [session-id (if (config/test?)
                     (session/schelling-session-id)
                     (->> (range 0 4)
                          (map #(.-value (.getElementById js/document (str "room-key-" %))))
                          (string/join " ")))
        channel (webrtc/offering-channel)]

    (webrtc/on-message channel receive-msg)
    (webrtc/on-open channel
                    #(->> @state
                          :name
                          (str "HELLO:")
                          (webrtc/send channel)))

    (swap! state assoc :phase :joining)
    (swap! state assoc :channel channel)

      (webrtc/make-offer channel
                         (fn [offer]
                           ;; post offer AND session id
                           ;; get answer as response

                           (println "make-offer/cb" session-id offer)

                           (post-offer session-id offer
                                       (fn [answer]

                                         (println "make-offer/cb/post-offer/cb" session-id answer)

                                         (webrtc/accept-answer channel answer)
                                         (swap! state assoc :phase :joined) ;TODO, this in promise on accept answer
                                         )))
                         nil)
    ))


(defn render-init []
  [:div
   [:h1 "player"]
   [:div
    [:span "Enter your name: " [:input {:type      "text"
                                        :on-change #(swap! state assoc :name (.-value (.-target %)))}]]
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

(defn send [msg-type msg]
  (-> @state :channel (webrtc/send (str msg-type ":" msg))))

(defn render-joined []
  [:div

   [:div
    (for [[index msg] (map-indexed vector (:msgs @state))]
      [:div {:key (str "msg-" index)} msg])]

   [:input {:type "button" :value "Up" :on-click #(send "MOVE" "Up")}]
   [:input {:type "button" :value "Down" :on-click #(send "MOVE" "Down")}]

   ])

(defn render-app []
  [:div
   (let [{:keys [phase]} @state]
     (condp = phase
       :init [render-init]
       :joining [render-waiting]
       :joined [render-joined]))])


(defn initialise [container]
  (reagent/render-component [render-app] container))