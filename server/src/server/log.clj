(ns server.log)



(defn info [& strs]
  (apply println "INFO:" strs))

(defn warn [& strs]
  (apply println "WARN:" strs))
