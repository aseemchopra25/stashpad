(defproject stashpad "0.1.0-SNAPSHOT"
  :description "A simple Clojure-based pastebin"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [ring "1.8.2"]
                 [ring/ring-defaults "0.3.3"]
                 [compojure "1.6.1"]]
  :main ^:skip-aot stashpad.core)