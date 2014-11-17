(ns lastfm2rdio.rdio
  "Component for working with Rdio's API."
  (:require
    [clojure.string :as s]
    [clj-http.client :as http]
    [oauth.client :as oa]))

(defn- make-consumer
  "Return a new oauth consumer object."
  [consumer-key shared-secret]
  (oa/make-consumer
    consumer-key
    shared-secret
    "http://api.rdio.com/oauth/request_token"
    "http://api.rdio.com/oauth/access_token"
    "https://www.rdio.com/oauth/authorize"
    :hmac-sha1))

(defrecord Rdio [consumer user-token user-secret])

(defn client [consumer-key shared-secret user-token user-secret]
  "Create an authenticted Rdio client that can make requests on behalf of the
  user corresponding to user-token and user-secret.  TODO: This is a broken
  design since it makes the client usable for only a single user, but will work
  for my extremely limited use case."
  (map->Rdio
    {:consumer (make-consumer consumer-key shared-secret)
     :user-token user-token
     :user-secret user-secret}))

(defn- do-request
  "Do all the oauth crap and send request.  Returns response."
  [client params]
  (let [url "http://api.rdio.com/1/"
        {:keys [consumer user-token user-secret]} client
        creds (oa/credentials
                consumer
                user-token
                user-secret
                :POST
                url
                params)
        req {:as :json
             :throw-exceptions? false
             :url "http://api.rdio.com/1/"
             :method :post
             :form-params params
             :query-params creds}
        resp (http/request req)]
    resp))

(defn owned-playlists
  "Return a seq of owned playlists for current user."
  [client]
  (let [resp (do-request client {:method "getPlaylists"})]
    (get-in resp [:body :result :owned])))

(defn create-playlist
  "Create a new empty playlist with given name for current user.  The new
  playlist is non-collaborative, and public. Returns the new playlist
  object."
  [client playlist-name description]
  (let [resp (do-request client {:method "createPlaylist"
                                 :name playlist-name
                                 :description description
                                 :tracks nil
                                 :isPublished true})]
    (get-in resp [:body :result])))

(defn delete-playlist
  "Delete a playlist by key for current user."
  [client playlist-key]
  (let [resp (do-request client {:method "deletePlaylist"
                                 :playlist playlist-key})]
    (get-in resp [:body :result])))

(defn add-to-playlist
  "Add a bunch of tracks to a playlist.  track-ids is a seq.  Returns the
  updated playlist."
  [client playlist-key track-ids]
  (let [csv (s/join "," track-ids)
        resp (do-request client {:method "addToPlaylist"
                                 :playlist playlist-key
                                 :tracks csv})]
    (get-in resp [:body :result])))

(defn authorize-rdio!
  "Authorize this app with Rdio for a single user.  Asks user to go to a
  particular rdio.com URL to authorize the app and get a PIN code.  After
  entering the PIN, return the response.
  their homedir.  Returns nil."
  [consumer-key shared-secret]
  (let [consumer (make-consumer consumer-key shared-secret)
        request-token (oa/request-token consumer)
        approval-uri (oa/user-approval-uri consumer (:oauth_token request-token))
        _ (println (str "Visit the following to authorize: " approval-uri))
        _ (print "Enter PIN and press enter: ")
        _ (flush)
        pin (read-line)]
    (oa/access-token consumer request-token pin)))

