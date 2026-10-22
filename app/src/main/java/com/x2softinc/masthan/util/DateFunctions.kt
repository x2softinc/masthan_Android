package com.x2softinc.masthan.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.Locale

// Port of Tools+Items/Dates+Functions.swift and the date formatting used by the views.
// All formatting uses Locale.US to match the iOS `en_US_POSIX` formatters.

private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
private val gregorianLongFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US)
private val longDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.US)

private val hijriMonthNames = listOf(
    "Muharram", "Safar", "Rabiʻ I", "Rabiʻ II", "Jumada I", "Jumada II",
    "Rajab", "Shaʻban", "Ramadan", "Shawwal", "Dhuʻl-Qiʻdah", "Dhuʻl-Hijjah",
)

fun startOfDay(date: ZonedDateTime): ZonedDateTime = date.toLocalDate().atStartOfDay(date.zone)

fun today(zone: ZoneId = ZoneId.systemDefault()): ZonedDateTime = LocalDate.now(zone).atStartOfDay(zone)


/** Port of getFullDate(refDate:timeString:) - applies an "HH:mm" string to a day. Null if unparseable. */
fun getFullDate(refDate: ZonedDateTime, timeString: String): ZonedDateTime? {
    val match = Regex("""^(\d{1,2}):(\d{2})""").find(timeString.trim()) ?: return null
    val hour = match.groupValues[1].toInt()
    val minute = match.groupValues[2].toInt()
    if (hour !in 0..23 || minute !in 0..59) return null
    return ZonedDateTime.of(refDate.toLocalDate(), LocalTime.of(hour, minute), refDate.zone)
}

/** Parses "HH:mm" or "HH:mm:ss" onto [day]; null when blank or invalid. */
fun parseClockTime(raw: String?, day: ZonedDateTime): ZonedDateTime? {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty()) return null
    val match = Regex("""^(\d{1,2}):(\d{2})(?::(\d{2}))?$""").find(trimmed) ?: return null
    val hour = match.groupValues[1].toInt()
    val minute = match.groupValues[2].toInt()
    val second = match.groupValues[3].ifEmpty { "0" }.toInt()
    if (hour !in 0..23 || minute !in 0..59 || second !in 0..59) return null
    return ZonedDateTime.of(day.toLocalDate(), LocalTime.of(hour, minute, second), day.zone)
}

/** "h:mm a" or an em dash when missing (Dashboard `formatTime`). */
fun formatTime(date: ZonedDateTime?): String = date?.format(timeFormatter) ?: "—"

/** Dashboard `parseTime` / `displayTime` for jumma settings ("HH:mm" or "h:mm a"). */
fun parseSettingTime(raw: String?, day: ZonedDateTime): ZonedDateTime? {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty()) return null
    parseClockTime(trimmed, day)?.let { return it }
    return runCatching {
        ZonedDateTime.of(day.toLocalDate(), LocalTime.parse(trimmed.uppercase(), timeFormatter), day.zone)
    }.getOrNull()
}

/** "hh:mm a" display of an "HH:mm" string; returns the raw string if it cannot be parsed. */
fun displayTime(raw: String?): String {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty()) return "—"
    val match = Regex("""^(\d{1,2}):(\d{2})$""").find(trimmed) ?: return trimmed
    val hour = match.groupValues[1].toInt()
    val minute = match.groupValues[2].toInt()
    if (hour !in 0..23 || minute !in 0..59) return trimmed
    return LocalTime.of(hour, minute).format(DateTimeFormatter.ofPattern("hh:mm a", Locale.US))
}

fun getLongDate(date: ZonedDateTime): String = date.format(longDateFormatter)

fun gregorianDateText(date: ZonedDateTime): String = date.format(gregorianLongFormatter)

/** Umm al-Qura Hijri date, e.g. "12 Ramadan 1447" (or "12 Ram 1447" when [shortMonth]). */
fun hijriDateText(date: ZonedDateTime, shortMonth: Boolean = false): String {
    val hijri = HijrahDate.from(date.toLocalDate())
    val month = hijriMonthNames[hijri.get(ChronoField.MONTH_OF_YEAR) - 1]
    val monthText = if (shortMonth) month.take(3) else month
    return "${hijri.get(ChronoField.DAY_OF_MONTH)} $monthText ${hijri.get(ChronoField.YEAR)}"
}

fun isFriday(date: ZonedDateTime): Boolean = date.dayOfWeek == DayOfWeek.FRIDAY

/** Row label in the iPad/tablet date list: "Today • Mar 4" or "Wed, Mar 4". */
fun dateSelectionText(date: ZonedDateTime, isToday: Boolean): String {
    val pattern = if (isToday) "'Today' • MMM d" else "EEE, MMM d"
    return date.format(DateTimeFormatter.ofPattern(pattern, Locale.US))
}

/** Port of convertGregtoJD / convertJDtoGreg. */
private const val JD_JAN_1_1970_0000GMT = 2440587.5

fun convertGregToJD(date: ZonedDateTime): Double = JD_JAN_1_1970_0000GMT + date.toEpochSecond() / 86400.0

fun convertJDToGreg(jd: Double, zone: ZoneId = ZoneId.systemDefault()): ZonedDateTime =
    java.time.Instant.ofEpochSecond(((jd - JD_JAN_1_1970_0000GMT) * 86400).toLong()).atZone(zone)

/** "HH:mm:ss" duration text used by countdown helpers: "mm:ss" under an hour, else "h:mm:ss". */
fun formatInterval(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
}

