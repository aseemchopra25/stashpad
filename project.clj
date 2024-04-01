(defproject stashpad "0.1.0"
  :description "A simple Clojure-based pastebin"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [ring "1.8.2"]
                 [ring/ring-defaults "0.3.3"]
                 [compojure "1.6.1"]
                 [org.xerial/sqlite-jdbc "3.34.0"]
                 [org.clojure/java.jdbc "0.7.11"]
                 [buddy "2.0.0"]
                 [environ "1.2.0"]
                 [ring/ring-json "0.5.0"]]
  :plugins [[lein-marginalia "0.9.2"]
            [lein-environ "1.2.0"]]
  :main ^:skip-aot stashpad.core)