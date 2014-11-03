(ns lastfm2rdio.echonest-test
  "Echonest tests. Calls the real API (i.e. rate limited, slow, uses network)."
  (:require
    [clojure.test :refer [deftest is]]
    [lastfm2rdio.test-utils :refer [with-system]]
    [lastfm2rdio.echonest :as en]
    clojure.pprint))

(defn run-with-tp
  "Create a taste profile TP with name tpname, then call f with TP. Delete TP
  after f runs."
  [echonest tpname f]
  (let [tp (en/create-taste-profile echonest tpname)]
    (try
      (f tp)
      (finally
        (en/delete-taste-profile! echonest (:id tp))))))

(defmacro with-new-tp
  [echonest tpname tpsym & body]
  `(run-with-tp ~echonest ~tpname (fn [~tpsym] ~@body)))

(defn rand-tpname
  "Generate a randomish taste profile name."
  []
  (str "test-taste-profile-" (rand-int 1000000)))

(deftest taste-profile-crud
  (with-system system
    (let [en (:echonest system)
          tpname (rand-tpname)]
      (with-new-tp en tpname tp
        (is (= tpname (:name tp)))
        (is (= "song" (:type tp)))
        (is (:id tp))))))

(deftest taste-profile-no-double-service
  (with-system system
    (let [en (:echonest system)
          tpname (rand-tpname)]
      (with-new-tp en tpname tp
        (is (thrown-with-msg?
              Exception
              #"A catalog with this name is already owned"
              (en/create-taste-profile en tpname))
            "Create with existing name throws")))))

(deftest list-taste-profiles
  (with-system system
    (let [en (:echonest system)
          n (count (en/list-taste-profiles en))]
      (with-new-tp en (rand-tpname) _
        (is (= (inc n) (count (en/list-taste-profiles en))))))))

