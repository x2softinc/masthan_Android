package com.x2softinc.masthan.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.x2softinc.masthan.ANNUAL_TIMES
import com.x2softinc.masthan.CALC_METH
import com.x2softinc.masthan.JURISDICTION
import com.x2softinc.masthan.LATT
import com.x2softinc.masthan.LONG
import com.x2softinc.masthan.PRAYER_SETTINGS
import com.x2softinc.masthan.data.AdhanTime
import com.x2softinc.masthan.data.AdhanTimesService
import com.x2softinc.masthan.data.AnnualTime
import com.x2softinc.masthan.data.AppJson
import com.x2softinc.masthan.data.IqamaCalculator
import com.x2softinc.masthan.data.IqamaTime
import com.x2softinc.masthan.data.IqamaTimesService
import com.x2softinc.masthan.data.PrayerSetting
import com.x2softinc.masthan.data.PrayerSettingsService
import com.x2softinc.masthan.data.Prefs
import com.x2softinc.masthan.data.TimeDelay
import com.x2softinc.masthan.data.TimeDelayService
import com.x2softinc.masthan.util.formatInterval
import com.x2softinc.masthan.util.formatTime
import com.x2softinc.masthan.util.parseClockTime
import com.x2softinc.masthan.util.today
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.builtins.ListSerializer
import java.time.Duration
import java.time.ZonedDateTime

/** The prayer window the current time falls in. */
data class CurrentPrayer(val name: String, val adhan: ZonedDateTime, val iqama: ZonedDateTime)

data class Countdown(val countdown: String, val label: String)

/** Port of ViewModels/TimesViewModel.swift. */
class TimesViewModel : ViewModel() {

    /** Calculated prayer entry times (for the "Enters" column). */
    var adhanTimes by mutableStateOf(AdhanTime())
        private set

    /** Adhan times from the masjid API (stored as the raw annual-times row). */
    var masjidAdhanTimesString by mutableStateOf<AnnualTime?>(null)
        private set
    var masjidAdhanTimes by mutableStateOf<IqamaTime?>(null)
        private set

    /** Calculated iqama times (adhan + delay). */
    var iqamaTimes by mutableStateOf<IqamaTime?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set
    var displayedDayOffset by mutableStateOf(0)
        private set
    var hasLoaded by mutableStateOf(false)
        private set
    var settings by mutableStateOf<PrayerSetting?>(null)
        private set
    var timeDelays by mutableStateOf<TimeDelay?>(null)
        private set

    private fun cachedSettings(): PrayerSetting? {
        val raw = Prefs.getString(PRAYER_SETTINGS)
        if (raw.isEmpty()) return null
        return runCatching { AppJson.decodeFromString<PrayerSetting>(raw) }.getOrNull()
    }

    private val maghribDelayMinutes: Int get() = cachedSettings()?.maghribDelay ?: 3 // fallback default

    private val roundMinutes: Int
        get() = cachedSettings()?.roundMinutes?.takeIf { it > 0 } ?: 5 // fallback default to 5 minutes

    suspend fun load(dayOffset: Int = 0) {
        isLoading = true
        displayedDayOffset = dayOffset

        fetchAdhanTimes(dayOffset) // Calculated prayer entry times
        // Settings drive rounding and the maghrib delay, so they are fetched before use.
        fetchPrayerSettings()
        fetchAnnualTimes() // Masjid adhan times from API
        extractMasjidAdhanTimes(dayOffset) // Extract adhan times from API
        fetchTimeDelays() // Fetch time delays for each prayer
        calculateIqamaTimes() // Calculate iqama = adhan + delay
        isLoading = false
        hasLoaded = true
    }

    fun fetchAdhanTimes(dayShift: Int = 0) {
        val latitude = Prefs.getDouble(LATT)
        val longitude = Prefs.getDouble(LONG)
        val school = Prefs.getInt(JURISDICTION)
        val method = Prefs.getString(CALC_METH, "ISNA")
        adhanTimes = AdhanTimesService().calcAdhanTimes(dayShift, latitude, longitude, school, method)
    }

    suspend fun fetchPrayerSettings() {
        settings = swallow { PrayerSettingsService().fetchPrayerSettings() }
    }

    suspend fun fetchTimeDelays() {
        timeDelays = swallow { TimeDelayService().fetchTimeDelay() }
    }

    /** Refreshes the cached annual times (the service itself caches once per day). */
    private suspend fun fetchAnnualTimes() {
        swallow { IqamaTimesService().fetchIqamaTimes() }
    }

    /** Runs [block], returning null on failure (like Swift's `try?`) but never swallowing cancellation. */
    private suspend fun <T> swallow(block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    // region Extract masjid adhan times from API

    fun extractMasjidAdhanTimes(dayOffset: Int = 0) {
        val targetDay = today().plusDays(dayOffset.toLong())
        extractMasjidAdhanTimes(targetDay)
    }

    private fun extractMasjidAdhanTimes(date: ZonedDateTime) {
        masjidAdhanTimes = IqamaTime()

        val raw = Prefs.getString(ANNUAL_TIMES)
        val annual = runCatching {
            AppJson.decodeFromString(ListSerializer(AnnualTime.serializer()), raw)
        }.getOrNull()
        if (annual == null) {
            masjidAdhanTimes = IqamaTime()
            return
        }

        val month = date.monthValue
        val day = date.dayOfMonth
        val row = annual.firstOrNull {
            it.month == month && (it.fromDay ?: 0) <= day && (it.toDay ?: 0) >= day
        }
        masjidAdhanTimesString = row

        // These are ADHAN times from the API (not iqama) - rounded up per the round-minutes setting.
        masjidAdhanTimes = IqamaTime(
            fajr = IqamaCalculator.roundUp(parseClockTime(row?.fajrTime, date)),
            dhuhar = IqamaCalculator.roundUp(parseClockTime(row?.dhuharTime, date)),
            asar = IqamaCalculator.roundUp(parseClockTime(row?.asrTime, date)),
            maghrib = IqamaCalculator.roundUp(parseClockTime(row?.maghribTime, date)),
            isha = IqamaCalculator.roundUp(parseClockTime(row?.ishaTime, date)),
        )
    }

    // endregion

    // region Calculate iqama times (adhan + delay from TimeDelayService)

    private fun calculateIqamaTimes() {
        iqamaTimes = IqamaTime()
        val adhan = masjidAdhanTimes ?: return
        iqamaTimes = IqamaCalculator.iqamaFrom(adhan, timeDelays)
    }

    // endregion

    // region Iqama helpers

    private fun iqamaDate(prayerName: String): ZonedDateTime? {
        val times = iqamaTimes ?: return null
        return when (prayerName) {
            "Fajr" -> times.fajr
            "Dhuhar", "Dhuhr" -> times.dhuhar
            "Asar", "Asr" -> times.asar
            "Maghrib" -> times.maghrib
            "Isha" -> times.isha
            else -> null
        }
    }

    fun iqamaTime(name: String): String = iqamaDate(name)?.let { formatTime(it) } ?: ""

    fun currentPrayerInfo(now: ZonedDateTime = ZonedDateTime.now()): CurrentPrayer? {
        val times = adhanTimes

        // The midnight time is stored as a time-of-day on today's date. If it occurs after
        // sunset and before Fajr, it belongs to the next day, so use tomorrow's midnight
        // as the end of the Isha window.
        var ishaEnd = times.midnight
        val isha = times.isha
        if (isha != null && ishaEnd != null && ishaEnd < isha) {
            ishaEnd = ishaEnd.plusDays(1)
        }

        data class Window(val name: String, val start: ZonedDateTime?, val end: ZonedDateTime?)

        val windows = listOf(
            Window("Fajr", times.fajr, times.sunrise),
            Window("Dhuhar", times.dhuhar, times.asar),
            Window("Asar", times.asar, times.maghrib),
            Window("Maghrib", times.maghrib, times.isha),
            Window("Isha", times.isha, ishaEnd),
        )

        for (window in windows) {
            val start = window.start ?: continue
            val end = window.end ?: continue
            if (now >= start && now < end) {
                return CurrentPrayer(window.name, start, iqamaDate(window.name) ?: start)
            }
        }
        return null
    }

    fun countdownInfo(now: ZonedDateTime, adhan: ZonedDateTime, iqama: ZonedDateTime, nextAdhan: ZonedDateTime?): Countdown =
        when {
            now < adhan -> Countdown(formatSeconds(now, adhan), "Until Adhan")
            now < iqama -> Countdown(formatSeconds(now, iqama), "Until Iqama")
            nextAdhan != null && now < nextAdhan -> Countdown(formatSeconds(now, nextAdhan), "Until Next Adhan")
            else -> Countdown(formatSeconds(iqama, now), "Since Iqama")
        }

    private fun formatSeconds(from: ZonedDateTime, to: ZonedDateTime) =
        formatInterval(Duration.between(from, to).seconds)

    // endregion
}
