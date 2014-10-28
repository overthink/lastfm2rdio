(ns lastfm2rdio.app
  (:refer-clojure :exclude [sync])
  (:require
    [lastfm2rdio.lastfm :as lastfm]
    [lastfm2rdio.echonest :as en]
    [lastfm2rdio.rdio :as rdio]))

(defn ensure-empty-tp
  "Ensure we have an empty taste profile named tpname. i.e. drop existing tp,
  create a new one. Returns the new, empty taste profile."
  [echonest tpname]
  (when-let [tp (en/taste-profile echonest tpname)]
    (en/delete-taste-profile echonest (get tp "id")))
  (en/create-taste-profile echonest tpname))

(defn sync
  "Update 'lastfm favs' playlist at rdio for user lastfm-user. 'sync' is highly
  misleading since what we actually do is blow away any existing state and
  recreate it from scratch."
  [app lastfm-user]
  ; fetch all the user's favs
  ; delete any existing en tp
  ; create new en tp
  ; update tp with all the favs
  ; wait for ticket to be done
  ; delete any existing rdio playlist
  ; create new rdio playlist
  (let [{:keys [lastfm echonest rdio]} app]
    (ensure-empty-tp echonest lastfm-user)))


