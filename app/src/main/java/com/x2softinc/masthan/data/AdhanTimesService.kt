package com.x2softinc.masthan.data

import com.x2softinc.masthan.util.getFullDate
import com.x2softinc.masthan.util.today
import java.time.ZonedDateTime

/** Calculates the day's adhan times locally (port of Services/AdhanTimesService.swift). */
class AdhanTimesService {

    fun calcAdhanTimes(
        dayShift: Int,
        latitude: Double,
        longitude: Double,
        @Suppress("UNUSED_PARAMETER") school: Int,
        method: String,
    ): AdhanTime {
        if (latitude == 0.0 || longitude == 0.0) return AdhanTime()

        // Attach the computed clock times to the requested day, not always to "today".
        val gDate: ZonedDateTime = today().plusDays(dayShift.toLong())

        val calculator = PrayTimesCalculator(method)
        val times = calculator.getTimes(gDate.toLocalDate(), listOf(latitude, longitude), gDate.zone)

        fun at(key: String) = getFullDate(gDate, times[key] ?: "")

        val dhuhar = at("dhuhar")
        return AdhanTime(
            imsak = at("imsak"),
            fajr = at("fajr"),
            sunrise = at("sunrise"),
            dhuhar = dhuhar,
            zawal = dhuhar?.minusSeconds(600),
            asar = at("asar"),
            asarHanafi = at("asarHanafi"),
            sunset = at("sunset"),
            maghrib = at("maghrib"),
            isha = at("isha"),
            midnight = at("midnight"),
        )
    }
}
