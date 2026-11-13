package com.x2softinc.masthan.data

import com.x2softinc.masthan.PRAYER_SETTINGS
import java.time.ZonedDateTime
import kotlin.math.ceil

/**
 * Shared Iqama rules so the Dashboard and notifications agree: the masjid's adhan time is
 * rounded up per the round-minutes setting, then the per-prayer delay is added.
 */
object IqamaCalculator {
    private const val DEFAULT_DELAY_MINUTES = 20
    private const val DEFAULT_ROUND_MINUTES = 5
    private const val DEFAULT_MAGHRIB_DELAY_MINUTES = 3

    private fun cachedSettings(): PrayerSetting? {
        val raw = Prefs.getString(PRAYER_SETTINGS)
        if (raw.isEmpty()) return null
        return runCatching { AppJson.decodeFromString<PrayerSetting>(raw) }.getOrNull()
    }

    val maghribDelayMinutes: Int get() = cachedSettings()?.maghribDelay ?: DEFAULT_MAGHRIB_DELAY_MINUTES

    private val roundMinutes: Int
        get() = cachedSettings()?.roundMinutes?.takeIf { it > 0 } ?: DEFAULT_ROUND_MINUTES

    /** Rounds a time UP (ceil) to the next interval based on the round-minutes setting. */
    fun roundUp(time: ZonedDateTime?): ZonedDateTime? {
        time ?: return null
        val interval = roundMinutes
        if (interval <= 0) return time // No rounding if 0

        val minute = time.minute
        val roundedMinute = (ceil(minute.toDouble() / interval) * interval).toInt()
        val base = time.withMinute(0).withSecond(0).withNano(0)
        // Handle hour overflow if the minute was rounded to 60 or higher.
        return if (roundedMinute >= 60) base.plusHours(1) else base.withMinute(roundedMinute)
    }

    /** Iqama = adhan + delay; [delays] falls back to 20 minutes when not available. */
    fun iqamaFrom(adhan: IqamaTime, delays: TimeDelay?): IqamaTime = IqamaTime(
        fajr = adhan.fajr?.plusMinutes((delays?.fajr ?: DEFAULT_DELAY_MINUTES).toLong()),
        dhuhar = adhan.dhuhar?.plusMinutes((delays?.dhuhar ?: DEFAULT_DELAY_MINUTES).toLong()),
        asar = adhan.asar?.plusMinutes((delays?.asr ?: DEFAULT_DELAY_MINUTES).toLong()),
        // Maghrib: use the special delay from settings
        maghrib = adhan.maghrib?.plusMinutes(maghribDelayMinutes.toLong()),
        isha = adhan.isha?.plusMinutes((delays?.isha ?: DEFAULT_DELAY_MINUTES).toLong()),
    )
}
