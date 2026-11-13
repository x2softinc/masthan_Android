package com.x2softinc.masthan.data

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.sqrt

/** Kotlin port of PrayTimes.js v2.3 (helper/PrayerTimesCalculator.swift). */
class PrayTimesCalculator(method: String) {

    private companion object {
        val methods: Map<String, Map<String, String>> = mapOf(
            "MWL" to mapOf("fajr" to "18", "isha" to "17"),
            "ISNA" to mapOf("fajr" to "15", "isha" to "15"),
            "EGAS" to mapOf("fajr" to "19.5", "isha" to "17.5"),
            "UQUM" to mapOf("fajr" to "18.5", "isha" to "90 min"),
            "UISK" to mapOf("fajr" to "18", "isha" to "18"),
            "MUIS" to mapOf("fajr" to "20", "isha" to "18"),
            "TEH" to mapOf("fajr" to "17.7", "isha" to "14", "maghrib" to "4.5", "midnight" to "Jafari"),
            "JAF" to mapOf("fajr" to "16", "isha" to "14", "maghrib" to "4", "midnight" to "Jafari"),
        )

        val defaultParams = mapOf("maghrib" to "0 min", "midnight" to "Standard")

        val timeNames = listOf(
            "imsak", "fajr", "sunrise", "dhuhar", "asar", "asarHanafi",
            "sunset", "maghrib", "isha", "midnight",
        )

        const val INVALID_TIME = "-----"
        const val NUM_ITERATIONS = 1

        fun gmtOffset(date: LocalDate, zone: ZoneId): Double {
            val noon = date.atTime(12, 0).atZone(zone)
            return noon.offset.totalSeconds / 3600.0
        }

        fun standardTimeZone(date: LocalDate, zone: ZoneId): Double {
            val january = LocalDate.of(date.year, 1, 1)
            val july = LocalDate.of(date.year, 7, 1)
            return minOf(gmtOffset(january, zone), gmtOffset(july, zone))
        }

        fun julian(year: Int, month: Int, day: Int): Double {
            var y = year
            var m = month
            if (m <= 2) {
                y -= 1
                m += 12
            }
            val a = floor(y / 100.0)
            val b = 2.0 - a + floor(a / 4.0)
            return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
        }
    }

    private val setting = mutableMapOf(
        "imsak" to "10 min",
        "dhuhar" to "0 min",
        "asar" to "Standard",
        "highLats" to "NightMiddle",
    )
    private var timeFormat = "24h"
    private val offset = mutableMapOf<String, Double>()

    private var lat = 0.0
    private var lng = 0.0
    private var elv = 0.0
    private var timeZone = 0.0
    private var jDate = 0.0

    init {
        val methodKey = if (methods.containsKey(method)) method else "MWL"
        val params = (methods[methodKey] ?: emptyMap()).toMutableMap()
        for ((key, value) in defaultParams) {
            if (params[key] == null) params[key] = value
        }
        setting.putAll(params)
        timeNames.forEach { offset[it] = 0.0 }
    }

    // region Public

    fun getTimes(
        date: LocalDate,
        coordinates: List<Double>,
        zone: ZoneId = ZoneId.systemDefault(),
        timeZone: Double? = null,
        dst: Boolean? = null,
        format: String = "24h",
    ): Map<String, String> {
        lat = coordinates[0]
        lng = coordinates[1]
        elv = if (coordinates.size > 2) coordinates[2] else 0.0
        timeFormat = format

        val standard = standardTimeZone(date, zone)
        val resolvedTimeZone = timeZone ?: standard
        val resolvedDst = dst ?: (gmtOffset(date, zone) != standard)
        this.timeZone = resolvedTimeZone + if (resolvedDst) 1 else 0

        jDate = julian(date.year, date.monthValue, date.dayOfMonth) - lng / (15.0 * 24.0)

        return modifyFormats(computeTimes())
    }

    // endregion

    // region Core calculations

    private fun computeTimes(): Map<String, Double> {
        var times = mutableMapOf(
            "imsak" to 5.0, "fajr" to 5.0, "sunrise" to 6.0, "dhuhar" to 12.0,
            "asar" to 13.0, "sunset" to 18.0, "maghrib" to 18.0, "isha" to 18.0,
        ).toMap()

        repeat(NUM_ITERATIONS) { times = computePrayerTimes(times) }

        val adjusted = adjustTimes(times).toMutableMap()

        val sunset = adjusted["sunset"] ?: 18.0
        val sunrise = adjusted["sunrise"] ?: 6.0
        val fajr = adjusted["fajr"] ?: 5.0

        adjusted["midnight"] = if (setting["midnight"] == "Jafari") {
            sunset + timeDiff(sunset, fajr) / 2
        } else {
            sunset + timeDiff(sunset, sunrise) / 2
        }

        return tuneTimes(adjusted)
    }

    private fun computePrayerTimes(times: Map<String, Double>): Map<String, Double> {
        val portions = dayPortion(times)
        val params = setting

        val imsak = sunAngleTime(eval(params["imsak"] ?: "10 min"), portions["imsak"] ?: 5.0, "ccw")
        val fajr = sunAngleTime(eval(params["fajr"] ?: "15"), portions["fajr"] ?: 5.0, "ccw")
        val sunrise = sunAngleTime(riseSetAngle(), portions["sunrise"] ?: 6.0, "ccw")
        val dhuhar = midDay(portions["dhuhar"] ?: 12.0)
        val asar = asrTime(asarFactor(params["asar"] ?: "Standard"), portions["asar"] ?: 13.0)
        val asarHanafi = asrTime(asarFactor("Hanafi"), portions["asar"] ?: 13.0)
        val sunset = sunAngleTime(riseSetAngle(), portions["sunset"] ?: 18.0)
        val maghrib = sunAngleTime(eval(params["maghrib"] ?: "0 min"), portions["maghrib"] ?: 18.0)
        val isha = sunAngleTime(eval(params["isha"] ?: "15"), portions["isha"] ?: 18.0)

        return mapOf(
            "imsak" to imsak,
            "fajr" to fajr,
            "sunrise" to sunrise,
            "dhuhar" to dhuhar,
            "asar" to asar,
            "asarHanafi" to asarHanafi,
            "sunset" to sunset,
            "maghrib" to maghrib,
            "isha" to isha,
        )
    }

    private fun midDay(time: Double): Double = DMath.fixHour(12 - sunPosition(jDate + time).equation)

    private fun sunAngleTime(angle: Double, time: Double, direction: String = "cw"): Double {
        val decl = sunPosition(jDate + time).declination
        val noon = midDay(time)
        val t = 1.0 / 15.0 * DMath.arccos(
            (-DMath.sin(angle) - DMath.sin(decl) * DMath.sin(lat)) /
                (DMath.cos(decl) * DMath.cos(lat))
        )
        return if (direction == "ccw") noon - t else noon + t
    }

    private fun asrTime(factor: Double, time: Double): Double {
        val decl = sunPosition(jDate + time).declination
        val angle = -DMath.arccot(factor + DMath.tan(abs(lat - decl)))
        return sunAngleTime(angle, time)
    }

    private class SunPosition(val declination: Double, val equation: Double)

    private fun sunPosition(jd: Double): SunPosition {
        val d = jd - 2451545.0
        val g = DMath.fixAngle(357.529 + 0.98560028 * d)
        val q = DMath.fixAngle(280.459 + 0.98564736 * d)
        val l = DMath.fixAngle(q + 1.915 * DMath.sin(g) + 0.020 * DMath.sin(2 * g))
        val e = 23.439 - 0.00000036 * d

        val ra = DMath.arctan2(DMath.cos(e) * DMath.sin(l), DMath.cos(l)) / 15.0
        val equation = q / 15.0 - DMath.fixHour(ra)
        val declination = DMath.arcsin(DMath.sin(e) * DMath.sin(l))
        return SunPosition(declination, equation)
    }

    // endregion

    // region Adjustments

    private fun adjustTimes(times: Map<String, Double>): Map<String, Double> {
        var result = times.toMutableMap()
        val params = setting

        for (key in result.keys.toList()) {
            result[key] = result.getValue(key) + timeZone - lng / 15.0
        }

        if (params["highLats"] != "None") {
            result = adjustHighLats(result).toMutableMap()
        }

        if (isMin(params["imsak"] ?: "")) {
            result["imsak"] = (result["fajr"] ?: 0.0) - eval(params["imsak"] ?: "10 min") / 60.0
        }
        if (isMin(params["maghrib"] ?: "")) {
            result["maghrib"] = (result["sunset"] ?: 0.0) + eval(params["maghrib"] ?: "0 min") / 60.0
        }
        if (isMin(params["isha"] ?: "")) {
            result["isha"] = (result["maghrib"] ?: 0.0) + eval(params["isha"] ?: "15") / 60.0
        }
        result["dhuhar"] = (result["dhuhar"] ?: 0.0) + eval(params["dhuhar"] ?: "0 min") / 60.0

        return result
    }

    private fun adjustHighLats(times: Map<String, Double>): Map<String, Double> {
        val result = times.toMutableMap()
        val params = setting
        val nightTime = timeDiff(result["sunset"] ?: 18.0, result["sunrise"] ?: 6.0)

        result["imsak"] = adjustHLTime(
            result["imsak"] ?: 5.0, result["sunrise"] ?: 6.0,
            eval(params["imsak"] ?: "10 min"), nightTime, "ccw",
        )
        result["fajr"] = adjustHLTime(
            result["fajr"] ?: 5.0, result["sunrise"] ?: 6.0,
            eval(params["fajr"] ?: "15"), nightTime, "ccw",
        )
        result["isha"] = adjustHLTime(
            result["isha"] ?: 18.0, result["sunset"] ?: 18.0,
            eval(params["isha"] ?: "15"), nightTime,
        )
        result["maghrib"] = adjustHLTime(
            result["maghrib"] ?: 18.0, result["sunset"] ?: 18.0,
            eval(params["maghrib"] ?: "0 min"), nightTime,
        )
        return result
    }

    private fun adjustHLTime(time: Double, base: Double, angle: Double, night: Double, direction: String = "cw"): Double {
        val portion = nightPortion(angle, night)
        val diff = if (direction == "ccw") timeDiff(time, base) else timeDiff(base, time)
        if (time.isNaN() || diff > portion) {
            return base + if (direction == "ccw") -portion else portion
        }
        return time
    }

    private fun nightPortion(angle: Double, night: Double): Double {
        val portion = when (setting["highLats"]) {
            "AngleBased" -> 1.0 / 60.0 * angle
            "OneSeventh" -> 1.0 / 7.0
            else -> 1.0 / 2.0
        }
        return portion * night
    }

    // endregion

    // region Formatting

    private fun dayPortion(times: Map<String, Double>): Map<String, Double> =
        times.mapValues { it.value / 24.0 }

    private fun tuneTimes(times: Map<String, Double>): Map<String, Double> {
        val result = times.toMutableMap()
        for ((key, value) in offset) {
            result[key]?.let { result[key] = it + value / 60.0 }
        }
        return result
    }

    private fun modifyFormats(times: Map<String, Double>): Map<String, String> =
        times.mapValues { getFormattedTime(it.value, timeFormat) }

    fun getFormattedTime(time: Double, format: String = "24h"): String {
        if (time.isNaN()) return INVALID_TIME
        if (format == "Float") return time.toString()

        val rounded = DMath.fixHour(time + 0.5 / 60.0)
        val hours = floor(rounded).toInt()
        val minutes = floor((rounded - hours) * 60.0).toInt()

        val suffix = if (format == "12h") (if (hours < 12) "AM" else "PM") else ""
        val hour = if (format == "24h") hours else (hours + 12 - 1) % 12 + 1

        val hourString = if (format == "24h") "%02d".format(hour) else hour.toString()
        val minuteString = "%02d".format(minutes)
        return if (suffix.isEmpty()) "$hourString:$minuteString" else "$hourString:$minuteString $suffix"
    }

    // endregion

    // region Utility

    private fun asarFactor(param: String): Double = when (param) {
        "Standard" -> 1.0
        "Hanafi" -> 2.0
        else -> eval(param)
    }

    private fun riseSetAngle(): Double = 0.833 + 0.0347 * sqrt(elv)

    /** Parses the leading numeric part of a string, e.g. "90 min" -> 90.0. */
    private fun eval(str: String): Double = str.takeWhile { it in "0123456789.+-" }.toDoubleOrNull() ?: 0.0

    private fun isMin(arg: String) = arg.contains("min")

    private fun timeDiff(time1: Double, time2: Double): Double = DMath.fixHour(time2 - time1)

    // endregion
}

/** Degree-based math helpers. */
private object DMath {
    fun dtr(d: Double) = Math.toRadians(d)
    fun rtd(r: Double) = Math.toDegrees(r)

    fun sin(d: Double) = kotlin.math.sin(dtr(d))
    fun cos(d: Double) = kotlin.math.cos(dtr(d))
    fun tan(d: Double) = kotlin.math.tan(dtr(d))

    fun arcsin(d: Double) = rtd(asin(d))
    fun arccos(d: Double) = rtd(acos(d))

    fun arccot(x: Double) = rtd(atan(1.0 / x))
    fun arctan2(y: Double, x: Double) = rtd(atan2(y, x))

    fun fixAngle(a: Double) = fix(a, 360.0)
    fun fixHour(a: Double) = fix(a, 24.0)

    fun fix(a: Double, b: Double): Double {
        var result = a - b * floor(a / b)
        if (result < 0) result += b
        return result
    }
}
