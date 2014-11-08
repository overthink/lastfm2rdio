(ns lastfm2rdio.lastfm-test
  "These tests use the network and are slow and all the other bad-for-tests
  stuff.  OTOH they're useful."
  (:require
    [clojure.test :refer [deftest is]]
    [lastfm2rdio.test-utils :refer [with-system]]
    [lastfm2rdio.lastfm :as lastfm]))

(deftest loved
  (with-system system
    (let [lastfm (:lastfm system)
          tracks (lastfm/loved lastfm "overthink" 1 10)]
      (is (= 0 @(:req-count lastfm)) "we're lazy")
      (is (= 20 (count (take 20 tracks))) "paging works")
      (is (= 2 @(:req-count lastfm)) "further requests are also lazy"))))

