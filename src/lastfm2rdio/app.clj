(ns lastfm2rdio.app
  (:refer-clojure :exclude [sync])
  (:require
    [lastfm2rdio.lastfm :as lastfm]
    [lastfm2rdio.echonest :as en]
    [lastfm2rdio.rdio :as rdio])
  (:import
    java.util.UUID))

(defn empty-tp
  "Return a new empty taste profile named tpname.  If an existing tp exists
  wiht that name, it's deleted."
  [echonest tpname]
  (when-let [tp (en/taste-profile echonest tpname)]
    (en/delete-taste-profile! echonest (get tp "id")))
  (en/create-taste-profile echonest tpname))

(defn lastfm->en
  "Transform data from lastfm into data that echcnest's taste profile update
  api understands."
  [lastfm-track]
    {:action "update"
     :item {:item_id (str (UUID/randomUUID))
            :song_name (get lastfm-track "name")
            :artist_name (get-in lastfm-track ["artist" "name"])}})

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
  (let [{:keys [lastfm echonest rdio]} app
        tp (empty-tp echonest lastfm-user)
        loved (lastfm/loved lastfm lastfm-user)]
    (prn tp)
    (en/update-taste-profile!
      echonest
      (get tp "id")
      (map lastfm->en loved))
    ))


