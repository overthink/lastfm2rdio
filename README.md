# lastfm2rdio

Sync [last.fm](http://last.fm) favourites to an [rdio](http://rdio.com) playlist.

## Usage

There are no niceities at the moment.  Here's what you need to do:

Setup the following accounts:

- last.fm developer account (http://www.last.fm/api/account/create)
- The Echo Nest developer account (https://developer.echonest.com)
- Have an Rdio account to create playlists in (http://www.rdio.com)

Create `~/.lastfm2rdio` that looks like this:

    {:app-creds
       {:rdio
         {:consumer-key "..."
          :shared-secret "..."}
        :lastfm
          {:api-key "..."}
        :echonest
          {:consumer-key "..."
           :shared-secret "..."
           :api-key "..."}}
     :user-creds
       {:rdio
         {:oauth_token "..."
          :oauth_token_secret "..."}}}

To get the `oauth_token` keys, run `(lastfm2rdio.rdio/authorize-rdio!
consumer-key shared-secret)`.  The resulting token and secret give lastfm2rdio
access to write data in whatever rdio account you authenticated.

Finally, run:

`lein run -m lastfm2rdio.main lastfm-username`

Takes < 1 min to sync ~1500 last.fm favs over to an 1100 song playlist at rdio
(not all tracks are available or matched well).

## License

Copyright © 2014 Mark Feeney

Distributed under the [MIT License](http://opensource.org/licenses/MIT).

