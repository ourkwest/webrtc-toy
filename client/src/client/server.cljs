(ns client.server)

(defn http-post [url data-str cb]

  (let [xhr (js/XMLHttpRequest.)]
    (.open xhr "POST" url, true)
    ;(.setRequestHeader xhr "" "")
    (aset xhr "onload" (fn []
                         (println ">>" (.-responseText xhr))
                         (cb (.-responseText xhr))))
    (.send xhr data-str)))