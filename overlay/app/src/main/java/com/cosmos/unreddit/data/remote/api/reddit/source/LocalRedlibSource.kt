package com.cosmos.unreddit.data.remote.api.reddit.source

import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device Reddit adapter. It prefers the current Android GraphQL operations for search,
 * subreddit feeds and comments, then falls back to Stealth's native JSON source.
 */
@Singleton
class LocalRedlibSource @Inject constructor(
    private val graphQlRedditSource: GraphQlRedditSource
) : BaseRedditSource by graphQlRedditSource
