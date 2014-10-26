(defproject lastfm2rdio "0.1.0-SNAPSHOT"
  :description "Library for syncing last.fm loved tracks to rdio playlists"
  :url "http://github.com/overthink/lastfm2rdio"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.stuartsierra/component "0.2.2"]
                 [clj-http "1.0.0" :exclusions [cheshire
                                                crouton
                                                org.clojure/tools.reader
                                                com.cognitect/transit-clj]]
                 [cheshire "5.3.1"]
                 [clj-oauth "1.5.1"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.7"]]}})

