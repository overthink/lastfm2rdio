(ns user
  (:require
    [clojure.repl :refer :all]
    [clojure.pprint :refer [pprint pp]]
    [clojure.test :refer [test-ns]]
    [com.stuartsierra.component :as component]
    [clojure.tools.namespace.repl :refer (refresh)]
    [lastfm2rdio.main :as main]))

(def system nil)

(defn init []
  (alter-var-root #'system
    (constantly (main/system (main/config)))))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system
    (fn [s] (when s (component/stop s)))))

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))

(defn test-all
  []
  (refresh)
  (->> (all-ns)
       (filter #(re-find #"^lastfm2rdio.*-test$" (name (ns-name %))))
       (map test-ns)
       (apply merge-with +)))

