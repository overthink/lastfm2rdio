(ns lastfm2rdio.echonest
  "Code for working with The Echo Nest's API."
  (:require
    [clojure.walk :refer [keywordize-keys]]
    [lastfm2rdio.util :as util]
    [cheshire.core :as json]
    [clj-http.client :as http]
    [clj-http.conn-mgr :as cm]))

(defrecord EchoNest [consumer-key secret-key api-key])

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
                :throw-exceptions? false
                :url (str "http://developer.echonest.com/api/v4/" path)
                :query-params {:api_key (:api-key client)}}
               req)
        resp (http/request req')
        status (get-in resp [:body "response" "status"])
        code (get status "code")
        message (get status "message")]
    (if (= 429 (:status resp))
      (do
        (println "Rate limited. Retryin 30 s ...")
        (Thread/sleep 30000)
        (recur client path req))
      resp)))

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
           {:form-params {:id id}})
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
                              :data (json/generate-string data)}})
        ticket (get-in resp [:body "response" "ticket"])]
    (keywordize-keys ticket)))

(defn taste-profile-status
  "Check the status of a taste profile update.  See
  http://developer.echonest.com/docs/v4/tasteprofile.html#status Returns a map
  with various info. ticket_status will be \"complete\" when done.  Throws un
  unknown ticket."
  [client ticket]
  (let [resp (do-get client
                     "tasteprofile/status"
                     {:query-params {:ticket ticket}})]
    (keywordize-keys
      (get-in resp [:body "response"]))))


(defn taste-profile
  "Get basic info about a taste-profile.  Returns nil if no taste profile found
  matching name."
  [client tpname]
  (let [resp (do-get client
                     "tasteprofile/profile"
                     {:query-params {:name tpname}})]
    (when (= 200 (:status resp))
      (keywordize-keys
        (get-in resp [:body "response" "catalog"])))))

(defn rdio-tracks
  "Get Rdio Canada tracks for all the songs in the taste profile with id tp-id.
  Returns a lazy seq of maps.  The interesting key is :tracks."
  ([client tp-id]
   (rdio-tracks client tp-id 0))
  ([client tp-id start]
   (lazy-seq
     (let [batch-size 1000
           resp (do-get client
                        "tasteprofile/read"
                        {:query-params {:id tp-id
                                        :bucket ["tracks" "id:rdio-CA"]
                                        :start start
                                        :results batch-size}})
           results (when (= 200 (:status resp))
                     (keywordize-keys
                       (get-in resp [:body "response" "catalog" "items"])))]
       (if (= (count results) batch-size)
         ;; Got exactly as many as we asked for, might be more
         (concat results (rdio-tracks client tp-id (+ start batch-size)))
         results)))))

(defn list-taste-profiles
  "List all profiles associated with current api key."
  [client]
  (let [resp (do-get client
                     "tasteprofile/list"
                     {})]
    (keywordize-keys
      (get-in resp [:body "response" "catalogs"]))))

(defn delete-test-taste-profiles!
  "Clean up crap I keep creating during test."
  [client]
  (let [victims (->> (list-taste-profiles client)
                     (filter #(re-find #"^test-taste-profile-" (:name %))))]
    (doseq [tp victims]
      (println (format "Deleting taste profile %s (%s)" (:name tp) (:id tp)))
      (delete-taste-profile! client (:id tp)))))

(defn wait-for-update!
  "Block until Echonest reports that ticket is fully processed.  Returns the
  resulting update status. ticket is the value of the ticket key in the
  update-taste-profile! response."
  ([client ticket]
   (wait-for-update! client ticket 1 2))
  ([client ticket call-num wait-sec]
   (assert (string? ticket))
   (let [result (taste-profile-status client ticket)]
     (cond
       (= "complete" (:ticket_status result))
       result
       (< 5 call-num)
       (throw (Exception. (str "Too long waiting for ticket completion: " result)))
       :else
       (do
         (Thread/sleep (* 1000 wait-sec))
         (recur client ticket (inc call-num) (* 2 wait-sec)))))))

