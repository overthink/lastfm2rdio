(ns lastfm2rdio.lastfm
  "Functionality for working wiht last.fm's API."
  (:require
    [lastfm2rdio.util :as util]
    [com.stuartsierra.component :as component]
    [clj-http.client :as http]
    [clj-http.conn-mgr :as cm]))

(defrecord LastfmClient [api-key conn-mgr]
  component/Lifecycle
  (start [this]
    this)
  (stop [this]
    (cm/shutdown-manager conn-mgr)))

(defn client
  "Return a new lastfm client that can be used to make requests to their API.
  Be sure to call shutdown when done."
  [api-key]
  (map->LastfmClient
    {:api-key api-key
     :conn-mgr (cm/make-reusable-conn-manager
                 {:timeout 5  ; keep connections open for this long
                  :threads 1  ; I don't expect multiple threads using the same client
                  })}))

(defn- do-get
  [client opts]
  (let [merged (util/deep-merge
                 {:as :json-string-keys ; lastfm json keys aren't keyword friendly
                  :connection-manager (:conn-mgr client)
                  :query-params {:api_key (:api-key client)
                                 :format "json"}}
                 opts)]
    (http/get "http://ws.audioscrobbler.com/2.0/" merged)))

(defn loved
  "Return a seq of username's loved tracks."
  ([client username]
   (loved client username 1 1000))
  ([client username page limit]
   (let [resp (do-get
                client
                {:query-params {:method "user.getlovedtracks"
                                :user username
                                :page page
                                :limit limit}})
         tracks (get-in resp [:body "lovedtracks" "track"])
         info (get-in resp [:body "lovedtracks" "@attr"])
         total-pages (Integer/valueOf ^String (get info "totalPages"))]
     ;(prn page total-pages)
     (if (= page total-pages)
       tracks
       (concat tracks (loved client username (inc page) limit))))))

