package com.cosmos.unreddit.ui.mediaviewer

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatDurationTest {
    @Test
    fun formatsMinutesAndHours() {
        assertEquals("0:00", formatDuration(-5))
        assertEquals("0:09", formatDuration(9_400))
        assertEquals("12:05", formatDuration(725_000))
        assertEquals("1:02:03", formatDuration(3_723_000))
    }
}
