(ns client.webrtc)


(defprotocol EventLogger
  (failure [eh msg])
  (success [eh msg]))

(def noop-event-logger
  (reify
    EventLogger
    (failure [eh msg])
    (success [eh msg])))

(extend-protocol EventLogger
  nil
  (failure [eh msg]
    (println "FAILURE:" msg))
  (success [eh msg]
    (println "Success:" msg)))

(defrecord Channel [peer-connection data-channel])

(defn offering-channel []
  (let [pc (js/RTCPeerConnection.)
        dc (.createDataChannel pc "channel")]
    (->Channel pc dc)))

(defn make-offer [{pc :peer-connection} on-offer event-logger]
  (-> (.createOffer pc)
      (.then #(.setLocalDescription pc %))
      (.then #(success event-logger "peer connection : create offer")
             #(failure event-logger "peer connection : create offer")))
  (doto pc
    (aset "onicecandidate"
          (fn [event]
            (when (nil? (.-candidate event))
              (on-offer (.stringify js/JSON (.-localDescription pc))))))
    (aset "onconnection" #(success event-logger "peer connection : connect"))))

(defn set-remote-description [pc description-str]
  (->> description-str
       (.parse js/JSON)
       (js/RTCSessionDescription.)
       (.setRemoteDescription pc)))

(defn offer->answer [offer
                     on-answer
                     on-channel
                     event-logger]

  (let [pc (js/RTCPeerConnection.)]
    (aset pc "ondatachannel"
          (fn [event]
            (let [dc (or (.-channel event) event)]
              (on-channel (->Channel pc dc)))))

    (-> (set-remote-description pc offer)
        (.then #(success event-logger "peer connection : set remote description")
               #(failure event-logger "peer connection : set remote description")))

    (-> (.createAnswer pc)
        (.then #(.setLocalDescription pc %))
        (.then #(success event-logger "peer connection : create answer")
               #(failure event-logger "peer connection : create answer")))

    (aset pc "onicecandidate"
          (fn [event]
            (when (nil? (.-candidate event))
              (on-answer (.stringify js/JSON (.-localDescription pc))))))))

(defn accept-answer [{pc :peer-connection} answer]
  (set-remote-description pc answer))

(defn send [{dc :data-channel} msg]
  (.send dc msg))

(defn on-message [channel on-event]
  (aset (:data-channel channel) "onmessage" on-event))

(defn on-open [channel on-open]
  (aset (:data-channel channel) "onopen" on-open))
