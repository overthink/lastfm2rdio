(ns lastfm2rdio.test-utils
  (:require
    [com.stuartsierra.component :as component]
    [lastfm2rdio.main :as main]))

(defn run-with-system
  "Create a new system and call f with it as an arg."
  [f]
  (let [system (component/start (main/system))]
    (try
      (f system)
      (finally
        (component/stop system)))))

(defmacro with-system
  [sysname & body]
  `(run-with-system
     (fn [~sysname] ~@body)))

