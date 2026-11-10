package com.cosmos.unreddit.data.remote.api.reddit.source

import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device Reddit frontend adapter backed by Stealth's native OAuth,
 * Retrofit, OkHttp and Moshi pipeline. It does not use a public instance.
 */
@Singleton
class LocalRedlibSource @Inject constructor(
    private val redditSource: RedditSource
) : BaseRedditSource by redditSource
