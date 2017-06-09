(ns client.webrtc)


; API

; I want to open a channel
;    channel    new-data-channel    data-handler
;               make-offer          channel    offer-handler
;               accept-answer       channel    answer
;               send                channel    message

; I want to respond
;    channel    offer->answer       offer    answer-handler

(defprotocol EventHandler
  (failure [eh msg])
  (success [eh msg]))

(def noop-event-handler
  (reify
    EventHandler
    (failure [eh msg])
    (success [eh msg])))

(extend-protocol EventHandler
  nil
  (failure [eh msg]
    (println "FAILURE:" msg))
  (success [eh msg]
    (println "Success:" msg)))

(defrecord Channel [peer-connection data-channel])

(defn offering-channel [data-handler event-handler]
  (let [pc (js/RTCPeerConnection.)
        dc (.createDataChannel pc "channel")]
    (aset dc "onopen" #(success event-handler "data channel : open"))
    (aset dc "onmessage" data-handler)
    (->Channel pc dc)))

(defn make-offer [{pc :peer-connection} offer-handler event-handler]
  (.createOffer pc (fn [description]
                     (success event-handler "peer connection : create offer")
                     (.setLocalDescription pc description
                                           #(success event-handler "peer connection : set local description")
                                           #(failure event-handler "peer connection : set local description")))
                #(failure event-handler "peer connection : create offer"))
  (aset pc "onicecandidate"
        (fn [event]
          (when (nil? (.-candidate event))
            (offer-handler (.stringify js/JSON (.-localDescription pc))))))
  (aset pc "onconnection" #(success event-handler "peer connection : connect")))

(defn set-remote-description [pc description-str]
  (->> description-str
       (.parse js/JSON)
       (js/RTCSessionDescription.)
       (.setRemoteDescription pc)))

(defn offer->answer [offer
                     answer-handler
                     data-handler
                     channel-handler
                     event-handler]

  (let [pc (js/RTCPeerConnection.)]
    (aset pc "ondatachannel"
          (fn [event]
            (let [dc (or (.-channel event) event)]
              (aset dc "onopen" #(success event-handler "data channel : open"))
              (aset dc "onmessage" data-handler)
              (channel-handler (->Channel pc dc)))))

    (set-remote-description pc offer)

    (.createAnswer pc (fn [description]
                        (success event-handler "peer connection : create answer")
                        ;TODO: use promises, as callbacks are deprecated?
                        ; https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection/setLocalDescription
                        (.setLocalDescription pc description
                                              #(success event-handler "peer connection : set local description")
                                              #(failure event-handler "peer connection : set local description")))
                   #(failure event-handler "peer connection : create answer"))

    (aset pc "onicecandidate"
          (fn [event]
            (when (nil? (.-candidate event))
              (answer-handler (.stringify js/JSON (.-localDescription pc))))))))

(defn accept-answer [{pc :peer-connection} answer]
  (set-remote-description pc answer))

(defn send [{dc :data-channel} msg]
  (.send dc msg))
