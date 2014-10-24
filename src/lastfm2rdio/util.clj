(ns lastfm2rdio.util)

(defn deep-merge
  [& xs]
  (if (every? map? xs)
    (apply merge-with deep-merge xs)
    (last xs)))

