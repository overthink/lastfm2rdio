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

(defn rdio-access-keys
  "Retrieve the saved oauth access token and secret for the current user.
  Returns a map with the following keys:
    :oauth_token
    :oauth_token_secret"
  []
  (read-config (str HOME "/.lastfm2rdio-user")))

(defn authorize-rdio!
  "Authorize this app with Rdio for a single user.  Asks user to go to a
  particular rdio.com URL to authorize the app and get a PIN code.  After
  entering the PIN the user's authorized access token is written to a file in
  their homedir.  Returns nil."
  []
  (let [oauth-keys (rdio-consumer-keys)
        consumer (oa/make-consumer
                   (:consumer-key oauth-keys)
                   (:shared-secret oauth-keys)
                   "http://api.rdio.com/oauth/request_token"
                   "http://api.rdio.com/oauth/access_token"
                   "https://www.rdio.com/oauth/authorize"
                   :hmac-sha1)
        request-token (oa/request-token consumer)
        approval-uri (oa/user-approval-uri consumer (:oauth_token request-token))
        _ (println (str "Visit the following to authorize: " approval-uri))
        _ (print "Enter PIN and press enter: ")
        _ (flush)
        pin (read-line)
        access-token-resp (oa/access-token consumer request-token pin)
        credsfile (str (.get (System/getenv) "HOME") "/.lastfm2rdio-user")]
    (spit credsfile access-token-resp)
    (println (format "Wrote access token to '%s'" credsfile))))


; Learning more:
; - need Echo Nest acct
; - create taste profile for user at EN
; - from taste profile can get rdio track IDs
; - use rdio API to create playlist with appropriate IDs

