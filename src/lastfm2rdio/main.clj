(ns lastfm2rdio.main
  "We'll jam stuff into core until the proper ns organization reveals itself."
  (:require
    [com.stuartsierra.component :as component]
    [lastfm2rdio.lastfm :as lastfm]
    [lastfm2rdio.echonest :as echonest]
    [lastfm2rdio.rdio :as rdio]))

(defn read-config
  [filename]
  (binding [*read-eval* false]
    (read-string (slurp filename))))

(defn config
  "Return a config map for the app.  Should look like this:
    {:app-creds
       {:rdio
         {:consumer-key \"foo\"
          :shared-secret \"foo\"}
        :lastfm
          {:api-key \"foo\"}
        :echonest
          {:consumer-key \"foo\"
           :shared-secret \"foo\"
           :api-key \"foo\"}}
     :user-creds
       {:rdio
         {:oauth_token \"foo\"
          :oauth_token_secret \"foo\"}}}
  TODO: Switch to command line args at some point"
  []
  (read-config
    (str (.get (System/getenv) "HOME") "/.lastfm2rdio")))

(defn system
  [config]
  (component/system-map
    :rdio (rdio/client
            (get-in config [:user-creds :rdio :oauth_token])
            (get-in config [:user-creds :rdio :oauth_token_secret]))
    :echonest (echonest/client
                (get-in config [:app-creds :echonest :consumer-key])
                (get-in config [:app-creds :echonest :shared-secret])
                (get-in config [:app-creds :echonest :api-key]))
    :lastfm (lastfm/client (get-in config [:app-creds :lastfm :api-key]))))

(defn -main [& args]
  :ok
  )

; Learning more:
; - need Echo Nest acct
; - create taste profile for user at EN
; - from taste profile can get rdio track IDs
; - use rdio API to create playlist with appropriate IDs

