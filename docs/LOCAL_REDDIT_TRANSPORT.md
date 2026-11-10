# Local Reddit transport

The app fetches Reddit data directly. It does not depend on a public Redlib or Teddit instance.

The local adapter now prefers Reddit's persisted Android GraphQL operations for the broken or unreliable paths:
- Post, subreddit and people search use SearchPosts, SearchCommunities and SearchPeople.
- Subreddit listings use SubredditFeedElements.
- Post metadata uses /by_id/t3_<id>.json, while comments use the Android PostComments operation on gql-fed.reddit.com.
- Comment continuation cursors are adapted to Stealth's existing MoreChild model, preserving the app's current load-more UI. Comment bodies are rendered from Reddit's `content.richtext` RTJSON document (with Markdown fallback), rather than looking for the absent `content.html` field.
- Transient network errors, HTTP 429 and HTTP 5xx responses get a bounded retry. The older JSON source remains a fallback for operation drift and other endpoints. The post-list paging boundary also converts unexpected transport/parser exceptions into a recoverable Paging error state (while preserving coroutine cancellation), rather than allowing an exception to escape and crash the search screen. Community search colours are restricted to Android-compatible hex values before entering the legacy subreddit mapper, which uses `Color.parseColor()`.

Operation hashes are based on the public Android operation catalogue at https://github.com/Amqx/reddit-gql, checked 2026-11-10. Reddit's persisted operations are undocumented implementation details and can change without notice. The comment conversion is implemented in this project; Redlib PR https://github.com/redlib-org/redlib/pull/603 was used as a protocol reference. No Redlib Rust source is embedded.

The token flow uses Reddit's unofficial Android app-only OAuth endpoint and sends an installation identifier to Reddit. Reddit may change or revoke these endpoints, operation hashes or required headers. This remains account-free and read-only; it does not add login, voting, posting or messaging. Reddit, CDN and media hosts are still contacted. CI verifies compilation and local path tests, not live availability on every network.

The pinned Stealth Android codebase is GPLv3. Redlib is a separate AGPL-3.0-only project and is not copied into the APK.
