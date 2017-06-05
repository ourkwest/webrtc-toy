(ns client.player
  (:require [reagent.core :as reagent :refer [atom]]
            [client.webrtc :as webrtc]
            [client.session :as session]
            [clojure.string :as string]))

(enable-console-print!)

(println "player ...")

(defonce state (atom {:phase :init}))


(defn post-offer [session-id offer cb]

  (let [xhr (js/XMLHttpRequest.)]

    (.open xhr "POST" "http://localhost:3210/offer", true)
    ;(.setRequestHeader xhr "" "")
    (aset xhr "onload" (fn []
                         (println ">>" (.-responseText xhr))
                         (cb (.-responseText xhr))))
    (.send xhr (str (session/remove-spaces session-id) "|" offer))

    )

  ;var xhr = new XMLHttpRequest();
  ;xhr.open('POST', 'somewhere', true);
  ;xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
  ;xhr.onload = function () {
  ;                          // do something to response
  ;                             console.log(this.responseText);
  ;                          };
  ;xhr.send('user=person&pwd=password&organization=place&requiredkey=key');

  )


(defn join-room []

  (let [session-id (->> (range 0 4)
                        (map #(.-value (.getElementById js/document (str "room-key-" %))))
                        (string/join " "))
        channel (webrtc/offering-channel println nil)]

    (swap! state assoc :phase :joining)

      (webrtc/make-offer channel
                         (fn [offer]
                           ;; post offer AND session id
                           ;; get answer as response

                           (post-offer session-id offer
                                       (fn [answer]

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