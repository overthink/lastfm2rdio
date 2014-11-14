(ns lastfm2rdio.app
  "The actual app logic.  Uses lastfm and echonest to create a playlist of
  favourites at rdio."
  (:require
    [clojure.string :as s]
    [lastfm2rdio.lastfm :as lastfm]
    [lastfm2rdio.echonest :as en]
    [lastfm2rdio.rdio :as rdio]))

(defn empty-tp
  "Return a new empty taste profile named tpname.  If an existing tp exists
  wiht that name, it's deleted."
  [echonest tpname]
  (when-let [tp (en/taste-profile echonest tpname)]
    (en/delete-taste-profile! echonest (:id tp)))
  (en/create-taste-profile echonest tpname))

(defn lastfm->en
  "Transform data from lastfm into data that echcnest's taste profile update
  api understands."
  [lastfm-track]
    {:action "update"
     :item {:item_id (str (get-in lastfm-track ["artist" "name"]) " - "
                          (get lastfm-track "name") " - "
                          (rand-int 1e7))
            ;; above id just needs to be unique, but it's helpful if it's readable too
            :song_name (get lastfm-track "name")
            :artist_name (get-in lastfm-track ["artist" "name"])}})

(defn ensure-playlist
  "Ensure there is an empty rdio playstlist ready to accept our lastfm favs.
  Any existing playlist with this ame name is blown away.  Returns the rdio playlist object."
  [rdio playlist-name]
  (let [playlists (rdio/owned-playlists rdio)
        existing (first (filter #(= playlist-name (:name %)) playlists))]
    (when existing
      ;; TODO: don't delete old playlist, just empty it -- easier to share the
      ;; playlist if it doesn't disappear all the time.
      (rdio/delete-playlist rdio (:key existing)))
    (rdio/create-playlist rdio playlist-name "lastfm2rdio - Favourites from last.fm")))

(defn update-playlist
  "Update 'lastfm favs' playlist at rdio for user lastfm-user."
  [app lastfm-user]
  (let [{:keys [lastfm echonest rdio]} app
        tp (empty-tp echonest lastfm-user)
        loved (lastfm/loved lastfm lastfm-user)
        ticket (en/update-taste-profile!
                 echonest
                 (:id tp)
                 (map lastfm->en loved))
        _ (en/wait-for-update! echonest ticket)
        tracks (en/rdio-tracks echonest (:id tp))
        rdio-ids (->> tracks
                      (map #(first (:tracks %)))
                      (remove #(nil? %))
                      (map :foreign_id)
                      (map #(last (s/split % #":")))) ; rdio-CA:track:t123 -> t123
        pl (ensure-playlist rdio "last.fm favs")]
    (rdio/add-to-playlist rdio (:key pl) rdio-ids)))

