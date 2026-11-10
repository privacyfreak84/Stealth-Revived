package com.cosmos.unreddit.data.remote.api.reddit.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GraphQlRedditSourceTest {
    @Test
    fun parsesFullAndShortPostPermalinks() {
        val full = GraphQlRedditSource.parsePostReference("/r/android/comments/1abc/a_title/")
        assertEquals("1abc", full.postId)
        assertNull(full.commentId)

        val focused = GraphQlRedditSource.parsePostReference("/r/android/comments/1abc/a_title/xy99")
        assertEquals("1abc", focused.postId)
        assertEquals("xy99", focused.commentId)

        val shortened = GraphQlRedditSource.parsePostReference("t3_1abc")
        assertEquals("1abc", shortened.postId)
        assertNull(shortened.commentId)

        val absolute = GraphQlRedditSource.parsePostReference(
            "https://www.reddit.com/r/android/comments/1abc/title/"
        )
        assertEquals("1abc", absolute.postId)
        assertNull(absolute.commentId)
    }
    @Test
    fun onlyPassesColoursThatAndroidCanParse() {
        assertEquals("#1a2b3c", GraphQlRedditSource.sanitizeColor("#1a2b3c"))
        assertEquals("#FF1a2b3c", GraphQlRedditSource.sanitizeColor("#FF1a2b3c"))
        assertNull(GraphQlRedditSource.sanitizeColor("var(--newCommunityTheme-button)"))
        assertNull(GraphQlRedditSource.sanitizeColor("rgba(12, 34, 56, 0.5)"))
        assertNull(GraphQlRedditSource.sanitizeColor("not-a-colour"))
        assertNull(GraphQlRedditSource.sanitizeColor(null))
    }
}
