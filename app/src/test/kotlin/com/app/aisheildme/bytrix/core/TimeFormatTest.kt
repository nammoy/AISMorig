package com.app.aisheildme.bytrix.core

import com.app.aisheildme.bytrix.core.util.IsoParser
import com.app.aisheildme.bytrix.core.util.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.TimeUnit

class TimeFormatTest {

    private val now = 1_700_000_000_000L

    @Test
    fun `timeAgo matches the Dart singular and plural wording`() {
        assertEquals("just now", TimeFormat.timeAgo(now - 5_000, now))
        assertEquals("1 min ago", TimeFormat.timeAgo(now - TimeUnit.MINUTES.toMillis(1), now))
        assertEquals("5 mins ago", TimeFormat.timeAgo(now - TimeUnit.MINUTES.toMillis(5), now))
        assertEquals("1 hour ago", TimeFormat.timeAgo(now - TimeUnit.HOURS.toMillis(1), now))
        assertEquals("3 hours ago", TimeFormat.timeAgo(now - TimeUnit.HOURS.toMillis(3), now))
        assertEquals("1 day ago", TimeFormat.timeAgo(now - TimeUnit.DAYS.toMillis(1), now))
        assertEquals("1 week ago", TimeFormat.timeAgo(now - TimeUnit.DAYS.toMillis(8), now))
        assertEquals("3 weeks ago", TimeFormat.timeAgo(now - TimeUnit.DAYS.toMillis(21), now))
    }

    @Test
    fun `timeAgo of null is blank`() {
        assertEquals("", TimeFormat.timeAgo(null, now))
    }

    @Test
    fun `iso parser accepts the shapes the backend sends`() {
        assertNotNull(IsoParser.toEpochMs("2026-07-20"))
        assertNotNull(IsoParser.toEpochMs("2026-07-20T14:30:00"))
        assertNotNull(IsoParser.toEpochMs("2026-07-20T14:30:00Z"))
        assertNotNull(IsoParser.toEpochMs("2026-07-20 14:30:00"))
    }

    @Test
    fun `iso parser returns null rather than throwing on junk`() {
        assertNull(IsoParser.toEpochMs("not a date"))
        assertNull(IsoParser.toEpochMs(""))
        assertNull(IsoParser.toEpochMs(null))
    }

    @Test
    fun `isoDate formats a date-picker value as its UTC day`() {
        val pickerMs = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2026, java.util.Calendar.JULY, 20)
        }.timeInMillis
        assertEquals("2026-07-20", TimeFormat.isoDate(pickerMs))
    }
}
