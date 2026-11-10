package com.cosmos.unreddit.data.remote.api.reddit.source

import javax.inject.Inject
import javax.inject.Singleton

/** All data operations use the local, direct-to-Reddit Android transport. */
@Singleton
class CurrentSource @Inject constructor(
    private val localRedlibSource: LocalRedlibSource
) : BaseRedditSource by localRedlibSource {

    @Suppress("UNUSED_PARAMETER")
    suspend fun setRedditSource(value: Int) {
        // Retain the legacy API for callers; the backend is intentionally fixed.
    }
}
