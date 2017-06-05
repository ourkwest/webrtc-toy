(ns client.core
  (:require [reagent.core :as reagent :refer [atom]]
            [client.map :as m]
            [client.player :as p]))

(enable-console-print!)

(println "This text is printed from src/client/core.cljs. Go ahead and edit it and see reloading in action.")

;; define your app data so that it doesn't get over-written on reload

;(defonce app-state (atom {:text "Hello world!"}))


(when-let [container (. js/document (getElementById "map"))]
  (m/initialise container))

(when-let [container (. js/document (getElementById "player"))]
  (p/initialise container))

;(defn on-js-reload []
  ; optionally touch your app-state to force rerendering depending on
  ; your application
  ; (swap! app-state update-in [:__figwheel_counter] inc)
;)
