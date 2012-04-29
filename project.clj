(defproject thingy "0.1.0-SNAPSHOT"
  :description "Data abstraction of things"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies []
  :warn-on-reflection true
  :profiles {:1.2 {:dependencies [[org.clojure/clojure "1.2.1"]]}
             :1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}}
  :aliases  {"all" ["with-profile" "1.2:1.3:1.4"]})
