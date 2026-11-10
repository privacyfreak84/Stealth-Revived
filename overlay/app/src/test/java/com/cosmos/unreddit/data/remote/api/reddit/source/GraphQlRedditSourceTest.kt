package com.cosmos.unreddit.data.remote.api.reddit.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GraphQlRedditSourceTest {
    @Test
    fun parsesFullAndShortPostPermalinks() {
        assertEquals(
            GraphQlRedditSource.PostReference("1abc", null),
            GraphQlRedditSource.parsePostReference("/r/android/comments/1abc/a_title/")
        )
        assertEquals(
            GraphQlRedditSource.PostReference("1abc", "xy99"),
            GraphQlRedditSource.parsePostReference("/r/android/comments/1abc/a_title/xy99")
        )
        assertEquals(
            GraphQlRedditSource.PostReference("1abc", null),
            GraphQlRedditSource.parsePostReference("t3_1abc")
        )
    }

    @Test
    fun cursorTokensRoundTripOpaqueGraphQlCursor() {
        val raw = "opaque:+/= cursor with spaces"
        assertEquals(raw, GraphQlRedditSource.decodeCursor(GraphQlRedditSource.encodeCursor(raw)))
        assertNull(GraphQlRedditSource.decodeCursor("not-a-valid-base64%%%"))
    }
}
