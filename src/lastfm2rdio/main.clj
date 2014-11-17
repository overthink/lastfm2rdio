(ns lastfm2rdio.main
  "Entry point for app.  Sets up system, calls main app, little else."
  (:require
    [com.stuartsierra.component :as component]
    [lastfm2rdio.app :as app]
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
  ([] (system (config)))
  ([config]
   (component/system-map
     :rdio (let [cfg (get-in config [:app-creds :rdio])]
             (rdio/client (:consumer-key cfg)
                          (:shared-secret cfg)
                          (get-in config [:user-creds :rdio :oauth_token])
                          (get-in config [:user-creds :rdio :oauth_token_secret])))
     :echonest (let [cfg (get-in config [:app-creds :echonest])]
                 (echonest/client (:consumer-key cfg)
                                  (:shared-secret cfg)
                                  (:api-key cfg)))
     :lastfm (lastfm/client (get-in config [:app-creds :lastfm :api-key]))
     :app (component/using
            {}
            [:lastfm :echonest :rdio]))))

(defn -main [& args]
  (let [system (component/start (system))]
    (try
      (let [lastfm-user (first args)
            result (app/update-playlist (:app system) lastfm-user)]
        (println (format "Created playlist with %s items" (:length result))))
      (finally
        (component/stop system)))))

