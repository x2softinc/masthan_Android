package com.x2softinc.masthan

import com.x2softinc.masthan.data.PrayTimesCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Reference values come from the PrayTimes.js algorithm (the `praytimes` npm package; its
 * "Makkah" method is our "UQUM") for the
 * same method, coordinates, time zone and date. The Swift calculator is a port of the same
 * algorithm, so agreement here means the Kotlin port matches the iOS results.
 */
class PrayTimesCalculatorTest {

    private fun minutes(hhmm: String): Int {
        val (h, m) = hhmm.split(":").map { it.toInt() }
        return h * 60 + m
    }

    private fun assertTimes(
        method: String, lat: Double, lng: Double, tz: Double, dst: Boolean, date: LocalDate,
        expected: Map<String, String>,
    ) {
        val actual = PrayTimesCalculator(method)
            .getTimes(date, listOf(lat, lng), ZoneId.of("UTC"), timeZone = tz, dst = dst)
        for ((key, want) in expected) {
            val got = actual[key]
            assertTrue("$method $key missing", got != null)
            // Tolerate one minute of rounding difference between implementations.
            assertEquals("$method $key (want $want, got $got)", minutes(want).toDouble(), minutes(got!!).toDouble(), 1.0)
        }
    }

    @Test
    fun isnaNewYorkSummer() = assertTimes(
        "ISNA", 40.7128, -74.0060, -5.0, true, LocalDate.of(2026, 7, 15),
        mapOf("imsak" to "03:53", "fajr" to "04:03", "sunrise" to "05:38", "dhuhar" to "13:02",
            "asar" to "17:01", "sunset" to "20:26", "maghrib" to "20:26", "isha" to "22:01", "midnight" to "01:02"),
    )

    @Test
    fun mwlLondonWinter() = assertTimes(
        "MWL", 51.5074, -0.1278, 0.0, true, LocalDate.of(2026, 12, 21),
        mapOf("imsak" to "06:49", "fajr" to "06:59", "sunrise" to "09:04", "dhuhar" to "12:59",
            "asar" to "14:38", "sunset" to "16:53", "maghrib" to "16:53", "isha" to "18:51", "midnight" to "00:59"),
    )

    @Test
    fun ummAlQuraMakkah() = assertTimes(
        "UQUM", 21.4225, 39.8262, 3.0, false, LocalDate.of(2026, 3, 4),
        mapOf("imsak" to "05:13", "fajr" to "05:23", "sunrise" to "06:39", "dhuhar" to "12:32",
            "asar" to "15:54", "sunset" to "18:26", "maghrib" to "18:26", "isha" to "19:56", "midnight" to "00:33"),
    )

    @Test
    fun hanafiAsrIsLaterThanStandard() {
        val times = PrayTimesCalculator("ISNA").getTimes(LocalDate.of(2026, 7, 15), listOf(40.7128, -74.0060), ZoneId.of("UTC"), -5.0, true)
        assertTrue(minutes(times.getValue("asarHanafi")) > minutes(times.getValue("asar")))
    }

    @Test
    fun unknownMethodFallsBackToMwl() {
        val a = PrayTimesCalculator("nope").getTimes(LocalDate.of(2026, 1, 10), listOf(51.5, -0.12), ZoneId.of("UTC"), 0.0, false)
        val b = PrayTimesCalculator("MWL").getTimes(LocalDate.of(2026, 1, 10), listOf(51.5, -0.12), ZoneId.of("UTC"), 0.0, false)
        assertEquals(b, a)
    }
}
