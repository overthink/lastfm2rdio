(ns lastfm2rdio.echonest
  "Code for working with The Echo Nest's API."
  (:require
    [clojure.walk :refer [keywordize-keys]]
    [lastfm2rdio.util :as util]
    [com.stuartsierra.component :as component]
    [cheshire.core :as json]
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
  "Create a taste profile with name tpname. Returns the created tp.  Throws if
  tp already exists."
  [client tpname]
  (let [resp (do-post client
                      "tasteprofile/create"
                      {:form-params {:name tpname
                                     :type "song"}})
        data (get-in resp [:body "response"])
        status (get data "status")]
    (when-not (zero? (get status "code"))
      (throw (Exception. (str "Echonest API error: " (get status "message")))))
    (keywordize-keys (dissoc data "status"))))

(defn delete-taste-profile!
  "Delete taste profile with given id.  Returns nil."
  [client id]
  (assert (not (nil? id)))
  (do-post client
           "tasteprofile/delete"
           {:form-params {:id id}
            :throw-exceptions? false})
  nil)

(defn update-taste-profile!
  "Update tasteprofile identified by id with data. data is a vector of items
  described here:
  http://developer.echonest.com/docs/v4/tasteprofile.html#update"
  [client id data]
  (assert (not (nil? id)))
  (assert (seq data))
  (let [resp (do-post
               client
               "tasteprofile/update"
               {:form-params {:id id
                              :data_type "json"
                              :format "json"
                              :data (json/generate-string data)}
                :throw-exceptions? false})
        ticket (get-in resp [:body "response" "ticket"])]
    ticket))

(defn update-status
  "Check the status of a taste profile update.  See
  http://developer.echonest.com/docs/v4/tasteprofile.html#status Returns a map
  with various info. ticket_status will be \"complete\" when done.  Throws un
  unknown ticket."
  [client ticket]
  (let [resp (do-get client
                     "tasteprofile/status"
                     {:query-params {:ticket ticket}})]
    (get-in resp [:body "response"])))


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


