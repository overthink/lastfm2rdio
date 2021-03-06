(ns lastfm2rdio.lastfm
  "Functionality for working wiht last.fm's API."
  (:require
    [lastfm2rdio.util :as util]
    [com.stuartsierra.component :as component]
    [clj-http.client :as http]
    [clj-http.conn-mgr :as cm]))

(defrecord LastfmClient [api-key conn-mgr req-count]
  component/Lifecycle

  (start [this]
    (let [cm (cm/make-reusable-conn-manager
                 {:timeout 5  ; keep connections open for this long
                  :threads 1  ; I don't expect multiple threads using the same client
                  })]
      (assoc this :conn-mgr cm)))

  (stop [this]
    (cm/shutdown-manager conn-mgr)
    this))

(defn client
  "Return a new lastfm client that can be used to make requests to their API.
  Lazy results might need the client \"later\" so be careful with scoping, or
  be sure to force results."
  [api-key]
  (map->LastfmClient
    {:api-key api-key
     :req-count (atom 0)}))

(defn- do-get
  [client opts]
  (let [merged (util/deep-merge
                 {:as :json-string-keys ; lastfm json keys aren't keyword friendly
                  :connection-manager (:conn-mgr client)
                  :query-params {:api_key (:api-key client)
                                 :format "json"}}
                 opts)
        resp (http/get "http://ws.audioscrobbler.com/2.0/" merged)
        error (get-in resp [:body "error"])
        message (get-in resp [:body "message"])]
    ;; Why use http status when you can just return 200 always and embed the
    ;; actal status code arbitrarily in the body?
    (when error
      (throw (Exception. (format "lastfm error %s: %s" error message))))
    resp))

(defn loved
  "Return a lazy seq of username's loved tracks."
  ([client username]
   (loved client username 1 1000))
  ([client username page limit-per-page]
   (lazy-seq
     (let [resp (do-get
                  client
                  {:query-params {:method "user.getlovedtracks"
                                  :user username
                                  :page page
                                  :limit limit-per-page}})
           tracks (get-in resp [:body "lovedtracks" "track"])
           info (get-in resp [:body "lovedtracks" "@attr"])
           total-pages (Integer/valueOf ^String (get info "totalPages"))]
       (swap! (:req-count client) inc) ; count requests made; largely for testing
       (if (= page total-pages)
         tracks
         (concat tracks (loved client username (inc page) limit-per-page)))))))

