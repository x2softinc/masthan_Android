package com.x2softinc.masthan.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.x2softinc.masthan.ANNUAL_TIMES
import com.x2softinc.masthan.ASAR
import com.x2softinc.masthan.CALC_METH
import com.x2softinc.masthan.DHUHAR
import com.x2softinc.masthan.FAJR
import com.x2softinc.masthan.ISHA
import com.x2softinc.masthan.JURISDICTION
import com.x2softinc.masthan.LATT
import com.x2softinc.masthan.LONG
import com.x2softinc.masthan.MAGHRIB
import com.x2softinc.masthan.NOTIFICATION_ENABLED
import com.x2softinc.masthan.NOTIFY_ADHAN
import com.x2softinc.masthan.NOTIFY_IQAMA
import com.x2softinc.masthan.CITY
import com.x2softinc.masthan.data.AdhanSoundOption
import com.x2softinc.masthan.data.AdhanSoundPreference
import com.x2softinc.masthan.data.AdhanTimesService
import com.x2softinc.masthan.data.AnnualTime
import com.x2softinc.masthan.data.AppJson
import com.x2softinc.masthan.data.IqamaTime
import com.x2softinc.masthan.data.IqamaTimesService
import com.x2softinc.masthan.data.Prefs
import com.x2softinc.masthan.util.parseClockTime
import com.x2softinc.masthan.util.today
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.builtins.ListSerializer
import java.time.ZonedDateTime

/**
 * Schedules Adhan / Iqama reminders (port of Managers/NotificationScheduler.swift).
 *
 * iOS uses repeating calendar triggers. Android has no equivalent, so each prayer gets a
 * one-shot exact alarm for its next occurrence; [PrayerAlarmReceiver] re-runs
 * [syncNotifications] when one fires, which schedules the following day with that day's
 * recalculated time.
 */
object NotificationScheduler {

    private const val ADHAN = "adhan"
    private const val IQAMA = "iqama"
    private const val ADHAN_CHANNEL_PREFIX = "adhan_"
    const val IQAMA_CHANNEL_ID = "iqama"

    const val EXTRA_KIND = "kind"
    const val EXTRA_PRAYER = "prayer"
    const val EXTRA_CITY = "city"

    private val prayerNames = listOf(FAJR, DHUHAR, ASAR, MAGHRIB, ISHA)

    private fun requestCode(kind: String, prayer: String): Int =
        (if (kind == ADHAN) 1000 else 2000) + prayerNames.indexOf(prayer)

    /** Rebuilds alarms based on current settings. */
    suspend fun syncNotifications(context: Context) {
        val app = context.applicationContext
        Prefs.init(app)

        if (!Prefs.getBoolean(NOTIFICATION_ENABLED) || !notificationsAllowed(app)) {
            cancelAll(app, ADHAN)
            cancelAll(app, IQAMA)
            return
        }

        ensureChannels(app)

        if (Prefs.getBoolean(NOTIFY_ADHAN)) scheduleAdhan(app) else cancelAll(app, ADHAN)
        if (Prefs.getBoolean(NOTIFY_IQAMA)) scheduleIqama(app) else cancelAll(app, IQAMA)
    }

    fun notificationsAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    // region Adhan

    private fun scheduleAdhan(context: Context) {
        cancelAll(context, ADHAN)

        val latitude = Prefs.getDouble(LATT)
        val longitude = Prefs.getDouble(LONG)
        if (latitude == 0.0 || longitude == 0.0) return

        val method = Prefs.getString(CALC_METH, "ISNA")
        val school = Prefs.getInt(JURISDICTION)
        val service = AdhanTimesService()
        val days = (0..1).map { service.calcAdhanTimes(it, latitude, longitude, school, method) }

        val city = Prefs.getString(CITY)
        val now = System.currentTimeMillis()
        for (prayer in prayerNames) {
            val next = days
                .mapNotNull {
                    when (prayer) {
                        FAJR -> it.fajr
                        DHUHAR -> it.dhuhar
                        ASAR -> it.asar
                        MAGHRIB -> it.maghrib
                        else -> it.isha
                    }
                }
                .firstOrNull { it.toInstant().toEpochMilli() > now + FIRE_GRACE_MS } ?: continue
            schedule(context, ADHAN, prayer, next, city)
        }
    }

    // endregion

    // region Iqama

    private suspend fun scheduleIqama(context: Context) {
        cancelAll(context, IQAMA)

        val today = today()
        val tomorrow = today.plusDays(1)
        val days = listOf(todaysIqamaTimes(today), decodeIqamaTimes(tomorrow))

        val now = System.currentTimeMillis()
        // Maghrib Iqama is intentionally left out of notifications.
        for (prayer in listOf(FAJR, DHUHAR, ASAR, ISHA)) {
            val next = days
                .mapNotNull {
                    when (prayer) {
                        FAJR -> it?.fajr
                        DHUHAR -> it?.dhuhar
                        ASAR -> it?.asar
                        else -> it?.isha
                    }
                }
                .firstOrNull { it.toInstant().toEpochMilli() > now + FIRE_GRACE_MS } ?: continue
            schedule(context, IQAMA, prayer, next, "")
        }
    }

    private suspend fun todaysIqamaTimes(today: ZonedDateTime): IqamaTime? {
        decodeIqamaTimes(today)?.let { return it }

        // Try refreshing from the server only if today's schedule is not cached.
        try {
            IqamaTimesService().fetchIqamaTimes()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Fall back to whatever annual times are already stored.
        }
        return decodeIqamaTimes(today)
    }

    private fun decodeIqamaTimes(day: ZonedDateTime): IqamaTime? {
        val raw = Prefs.getString(ANNUAL_TIMES)
        if (raw.isEmpty()) return null
        val records = runCatching {
            AppJson.decodeFromString(ListSerializer(AnnualTime.serializer()), raw)
        }.getOrNull() ?: return null

        val month = day.monthValue
        val dom = day.dayOfMonth
        val record = records.firstOrNull {
            it.month == month && (it.fromDay ?: 0) <= dom && (it.toDay ?: 0) >= dom
        } ?: return null

        return IqamaTime(
            fajr = parseClockTime(record.fajrTime, day),
            dhuhar = parseClockTime(record.dhuharTime, day),
            asar = parseClockTime(record.asrTime, day),
            // Maghrib is intentionally not used for Iqama notifications.
            isha = parseClockTime(record.ishaTime, day),
        )
    }

    // endregion

    // region Alarms

    private const val FIRE_GRACE_MS = 30_000L

    private fun pendingIntent(context: Context, kind: String, prayer: String, city: String): PendingIntent {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            putExtra(EXTRA_KIND, kind)
            putExtra(EXTRA_PRAYER, prayer)
            putExtra(EXTRA_CITY, city)
        }
        return PendingIntent.getBroadcast(
            context, requestCode(kind, prayer), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun schedule(context: Context, kind: String, prayer: String, at: ZonedDateTime, city: String) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = pendingIntent(context, kind, prayer, city)
        val triggerAt = at.toInstant().toEpochMilli()
        if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
        }
    }

    private fun cancelAll(context: Context, kind: String) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (prayer in prayerNames) {
            val intent = pendingIntent(context, kind, prayer, "")
            alarms.cancel(intent)
            intent.cancel()
        }
    }

    // endregion

    // region Channels

    fun adhanChannelId(sound: AdhanSoundOption) = ADHAN_CHANNEL_PREFIX + sound.rawValue.lowercase()

    fun selectedAdhanSound(): AdhanSoundOption =
        AdhanSoundOption.fromRawValue(Prefs.getString(AdhanSoundPreference.STORAGE_KEY, AdhanSoundOption.SYSTEM_DEFAULT.rawValue))
            ?: AdhanSoundOption.SYSTEM_DEFAULT

    /**
     * Android channel sounds are immutable once created, so every Adhan sound gets its own
     * channel; channels for sounds no longer selected are removed.
     */
    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val selected = selectedAdhanSound()
        val selectedId = adhanChannelId(selected)

        manager.notificationChannels
            .filter { it.id.startsWith(ADHAN_CHANNEL_PREFIX) && it.id != selectedId }
            .forEach { manager.deleteNotificationChannel(it.id) }

        if (manager.getNotificationChannel(selectedId) == null) {
            val channel = NotificationChannel(selectedId, "Adhan", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Adhan prayer-time reminders"
                selected.soundUri(context)?.let {
                    setSound(
                        it,
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                }
            }
            manager.createNotificationChannel(channel)
        }

        if (manager.getNotificationChannel(IQAMA_CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(IQAMA_CHANNEL_ID, "Iqama", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Iqama prayer-time reminders (default system sound)"
                }
            )
        }
    }

    // endregion
}
