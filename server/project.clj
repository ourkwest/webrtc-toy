(defproject server "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [yada "1.2.1"]
                 [yada "1.2.1"]
                 [com.stuartsierra/component "0.3.2"]]


  :profiles {:dev {:source-paths   ["dev" "src/clj"]
                   :dependencies   [[org.clojure/tools.namespace "0.2.11"]
                                    [org.clojure/java.classpath "0.2.3"]]
                   :repl-options   {:init-ns user}}})


