package com.cosmos.unreddit.data.remote.api.reddit

import org.junit.Assert.assertEquals
import org.junit.Test

class RedditPathInterceptorTest {
    private val interceptor = RedditPathInterceptor()

    @Test fun encodesCommentThreadRoute() {
        assertEquals("/r/android%2fcomments/abc123/title",
            interceptor.encodePath("/r/android/comments/abc123/title"))
    }

    @Test fun encodesMoreChildrenRoute() {
        assertEquals("/api%2fmorechildren", interceptor.encodePath("/api/morechildren"))
    }

    @Test fun encodesInfoRoute() {
        assertEquals("/api%2finfo", interceptor.encodePath("/api/info"))
    }

    @Test fun encodesMultiSubredditSeparator() {
        assertEquals("/r/android%2Bprivacy/new",
            interceptor.encodePath("/r/android+privacy/new"))
    }

    @Test fun leavesUnrelatedRoutesUntouched() {
        assertEquals("/r/android/top", interceptor.encodePath("/r/android/top"))
    }
}
