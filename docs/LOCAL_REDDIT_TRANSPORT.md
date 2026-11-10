# Local Reddit transport

## Goal and data flow

The app does not need a public Redlib or Teddit instance. Requests originate inside the Android app and go directly to Reddit.

"Local" describes the frontend and transport implementation, not offline storage of Reddit itself. Posts, comments and media are fetched over the network from Reddit and the relevant media hosts.

```text
Screens/ViewModels -> CurrentSource -> LocalRedlibSource
                                     -> RedditSource
                                     -> Retrofit + Moshi
                                     -> OkHttp interceptors
                                     -> Reddit OAuth / JSON endpoints
```

The first-party `LocalRedlibSource` adapter delegates to Stealth's native `RedditSource`. The existing source parses subreddit feeds and metadata, post details, comments, additional comment pages, user pages and search into Stealth's existing models.

## Request handling

The pinned Android baseline contains these mechanisms:

- An app-only token manager obtains a token from Reddit's Android OAuth endpoint and caches it for the returned lifetime.
- The OAuth interceptor attaches the bearer token and associated Android client headers. On HTTP 401 it invalidates the cached token, refreshes, and retries once.
- The path interceptor encodes routes Reddit rejects literally for app-only requests: comment-thread routes, `/api/morechildren`, `/api/info` and multi-subreddit separators.
- Moshi adapters decode the JSON into Stealth's existing models. The overlay adds regression tests for the URL rewrites.

The overlay fixes the app's runtime source to this on-device Reddit path. Legacy saved preferences that selected Teddit or old.reddit scraping are interpreted as the local source, and the source-selection dialog no longer exposes instance choices.

## Limitations and privacy

- Reddit's app-only token endpoint and Android client identifier/headers are unofficial implementation details. They may change or be revoked, may be subject to Reddit's terms and policies, and are not guaranteed to remain available.
- The token flow generates an installation identifier and sends it to Reddit as part of the request.
- This is an account-free, read-only path. It does not add login, voting, posting or messaging.
- Reddit/CDN and external media hosts are still contacted for content. This is not a network-isolated app.
- Unit tests validate local path rewriting, not live Reddit availability. Live request behaviour still needs device/emulator testing.

## Redlib and licensing

Redlib's Rust server, templates and proxy are not embedded in the APK. Redlib is an architectural reference; the implementation here is native Kotlin using the Stealth-derived GPLv3 codebase. No Redlib source is copied.

Redlib is AGPL-3.0-only. The selected Stealth Android baseline is GPLv3, with its original license text and notices retained. This project note is provenance information, not legal advice.
