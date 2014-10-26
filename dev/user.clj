(ns user
  (:require
    [clojure.repl :refer :all]
    [clojure.pprint :refer [pprint pp]]
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

