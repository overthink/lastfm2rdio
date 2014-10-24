(ns lastfm2rdio.rdio
  "Code for working with Rdio's API."
  (:require
    [oauth.client :as oa]))

(defn authorize-rdio!
  "Authorize this app with Rdio for a single user.  Asks user to go to a
  particular rdio.com URL to authorize the app and get a PIN code.  After
  entering the PIN, return the response.
  their homedir.  Returns nil."
  [consumer-key shared-secret]
  (let [consumer (oa/make-consumer
                   consumer-key
                   shared-secret
                   "http://api.rdio.com/oauth/request_token"
                   "http://api.rdio.com/oauth/access_token"
                   "https://www.rdio.com/oauth/authorize"
                   :hmac-sha1)
        request-token (oa/request-token consumer)
        approval-uri (oa/user-approval-uri consumer (:oauth_token request-token))
        _ (println (str "Visit the following to authorize: " approval-uri))
        _ (print "Enter PIN and press enter: ")
        _ (flush)
        pin (read-line)]
    (oa/access-token consumer request-token pin)))

