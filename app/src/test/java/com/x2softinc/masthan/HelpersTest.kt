package com.x2softinc.masthan

import com.x2softinc.masthan.data.MasjidProApiPaths
import com.x2softinc.masthan.data.isNewerVersion
import com.x2softinc.masthan.util.displayTime
import com.x2softinc.masthan.util.formatInterval
import com.x2softinc.masthan.util.getFullDate
import com.x2softinc.masthan.util.hijriDateText
import com.x2softinc.masthan.util.parseClockTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class HelpersTest {
    private val day = ZonedDateTime.of(2026, 3, 4, 0, 0, 0, 0, ZoneId.of("America/New_York"))

    @Test
    fun versionComparison() {
        assertTrue(isNewerVersion("2.1", "2.0"))
        assertTrue(isNewerVersion("2.0.1", "2.0"))
        assertFalse(isNewerVersion("2.0", "2.0.0"))
        assertFalse(isNewerVersion("1.9", "2.0"))
        assertFalse(isNewerVersion("2.0-beta", "2.0"))
    }

    @Test
    fun clockTimeParsing() {
        assertEquals(5, getFullDate(day, "05:30")?.hour)
        assertNull(getFullDate(day, "-----"))
        assertEquals(30, parseClockTime("05:30:00", day)?.minute)
        assertEquals(15, parseClockTime("13:15", day)?.minute)
        assertNull(parseClockTime("  ", day))
        assertNull(parseClockTime("25:00", day))
    }

    @Test
    fun displayFormats() {
        assertEquals("01:05 PM", displayTime("13:05"))
        assertEquals("—", displayTime(""))
        assertEquals("noon", displayTime("noon"))
        assertEquals("1:05:07", formatInterval(3907))
        assertEquals("05:07", formatInterval(307))
    }

    @Test
    fun hijriDate() {
        assertTrue(hijriDateText(day).endsWith("Ramadan 1447"))
    }

    @Test
    fun apiPaths() {
        assertEquals("/api/masjidInfo/masjid/26277142", MasjidProApiPaths.Resources.byDeviceCode("masjidInfo", "26277142"))
        assertEquals("/api/device/a%20b/validate", MasjidProApiPaths.Device.validate("a b"))
    }
}
