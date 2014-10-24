(ns lastfm2rdio.main
  "We'll jam stuff into core until the proper ns organization reveals itself."
  (:require
    [lastfm2rdio.lastfm :as lastfm]
    [oauth.client :as oa]))

(def HOME (.get (System/getenv) "HOME"))

(defn read-config
  [filename]
  (binding [*read-eval* false]
    (read-string (slurp filename))))

(defn rdio-consumer-keys
  "Returns Rdio app key and shared secret for lastfmrdio. \"Consumer\" is oauth
  jargon.  These are the values managed by http://rdio.mashery.com/.  Returns a
  map with two keys:
    :consumer-key
    :shared-secret"
  []
  (read-config (str HOME "/.lastfm2rdio-consumer")))


; Learning more:
; - need Echo Nest acct
; - create taste profile for user at EN
; - from taste profile can get rdio track IDs
; - use rdio API to create playlist with appropriate IDs

