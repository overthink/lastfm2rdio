(ns lastfm2rdio.lastfm
  "Functionality for working wiht last.fm's API."
  (:require
    [clj-http.client :as http]
    [clj-http.conn-mgr :as cm]))

(def ^:const ENDPOINT "http://ws.audioscrobbler.com/2.0/")

(defn client
  "Return a new lastfm client that can be used to make requests to their API.
  Be sure to call shutdown when done."
  [api-key]
  {:api-key api-key
   :conn-mgr (cm/make-reusable-conn-manager
               {:timeout 5  ; keep connections open for this long
                :threads 1  ; I don't expect multiple threads using the same client
                })})

(defn shutdown!
  [client]
  (cm/shutdown-manager (:conn-mgr client)))

(defn do-get
  [client opts]
  (let [merged (merge-with
                 merge
                 {:as :json
                  :connection-manager (:conn-mgr client)
                  :query-params {:api_key (:api-key client)
                                 :format "json"}}
                 opts)]
    (http/get "http://ws.audioscrobbler.com/2.0/" merged)))

(defn loved
  "Return a seq of username's loved tracks."
  [client username]
  (let [resp (do-get
               client
               {:query-params {:method "user.getlovedtracks"
                               :user username
                               :limit 2000}})]
    (get-in resp [:body :lovedtracks :track])))

