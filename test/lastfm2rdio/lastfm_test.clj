(ns lastfm2rdio.lastfm-test
  "These tests use the network and are slow and all the other bad-for-tests
  stuff.  OTOH they're useful."
  (:require
    [com.stuartsierra.component :as component]
    [clojure.test :refer [deftest is]]
    [lastfm2rdio.main :as main]
    [lastfm2rdio.lastfm :as lastfm]))

(deftest lifecycle
  (let [client (component/start (lastfm/client "fake-key"))]
    (is (:conn-mgr client))
    (component/stop client)))

(deftest errors
  (let [client (component/start (lastfm/client "fake-key"))]
    (is (thrown-with-msg?
          Exception
          #"(?i)Invalid API key"
          (lastfm/loved client "overthink")))))

(deftest loved
  (let [system (component/start (main/system (main/config)))
        lastfm (:lastfm system)]
    (try
      (let [tracks (lastfm/loved lastfm "overthink" 1 10)]
        (is (= 1 @(:req-count lastfm)) "first page request is eager")
        (is (= 20 (count (take 20 tracks))) "paging works"))
        (is (= 2 @(:req-count lastfm)) "further requests are lazy")
      (finally
        (component/stop system)))))

