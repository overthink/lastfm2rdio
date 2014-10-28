(ns lastfm2rdio.echonest
  "Code for working with The Echo Nest's API."
  (:require
    [lastfm2rdio.util :as util]
    [com.stuartsierra.component :as component]
    [clj-http.client :as http]
    [clj-http.conn-mgr :as cm]))

(defrecord EchoNest [consumer-key secret-key api-key]
  component/Lifecycle
  (start [this] this)
  (stop [this] this))

(defn client [consumer-key secret-key api-key]
  (map->EchoNest
    {:consumer-key consumer-key
     :secret-key secret-key
     :api-key api-key}))

(defn- do-request
  "Wrapper on http/request that adds some standard stuff every echonest API
  request requires.  Returns response."
  [client path req]
  (let [req' (util/deep-merge
                 {:as :json-string-keys
                  :url (str "http://developer.echonest.com/api/v4/" path)
                  :query-params {:api_key (:api-key client)}}
                 req)
        resp (http/request req')
        status (get-in resp [:body "response" "status"])
        code (get status "code")
        message (get status "message")]
    ;; And more not-using-http-status shenanigans
    (when (and (:throw-exceptions req) (not (zero? code)))
      (throw (Exception. (format "echonest error code %s: %s" code message))))
    resp))

(defn- do-get
  [client path req]
  (do-request client path (merge req {:method :get})))

(defn- do-post
  [client path req]
  (do-request client path (merge req {:method :post})))

(defn create-taste-profile
  "Create a taste profile with name tpname."
  [client tpname]
  (do-post client
           "tasteprofile/create"
           {:form-params {:name tpname}}))

(defn delete-taste-profile
  [client id]
  (do-post client
           "tasteprofile/delete"
           {:form-params {:id id}}))

(defn taste-profile
  "Get basic info about a taste-profile.  Returns nil if no taste profile found
  matching name."
  [client tpname]
  (let [resp (do-get client
                     "tasteprofile/profile"
                     {:query-params {:name tpname}
                      :throw-exceptions false})]
    (when (= 200 (:status resp))
      (get-in resp [:body "response" "catalog"]))))

(defn list-taste-profiles
  "List all profiles associated with current api key."
  [client]
  (let [resp (do-get client
                     "tasteprofile/list"
                     {})]
    (get-in resp [:body "response" "catalogs"])))

(defn song-profile
  "Get info about a song.
  http://developer.echonest.com/docs/v4/song.html#profile
  e.g. id: \"musicbrainz:song:c56e730b-8e7d-44ee-9bdc-2c7e9d2bb879\"
  "
  [client song-id]
  (let [resp (do-get client
                     "song/profile"
                     {:query-params {:id song-id}})]
    (get-in resp [:body "response" "songs"])))


