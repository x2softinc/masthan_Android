package com.x2softinc.masthan.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.x2softinc.masthan.MainActivity
import com.x2softinc.masthan.R
import com.x2softinc.masthan.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Posts the Adhan/Iqama notification, then schedules the next occurrence. */
class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Prefs.init(context)

        val kind = intent.getStringExtra(NotificationScheduler.EXTRA_KIND) ?: return
        val prayer = intent.getStringExtra(NotificationScheduler.EXTRA_PRAYER) ?: return
        val city = intent.getStringExtra(NotificationScheduler.EXTRA_CITY).orEmpty()

        val isAdhan = kind == "adhan"
        val title = if (isAdhan) "Adhan" else "Iqama"
        NotificationScheduler.ensureChannels(context)
        val channelId = if (isAdhan) {
            NotificationScheduler.adhanChannelId(NotificationScheduler.selectedAdhanSound())
        } else {
            NotificationScheduler.IQAMA_CHANNEL_ID
        }

        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("$title time for $prayer prayer.")
            .apply { if (city.isNotEmpty()) setSubText(city) }
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()

        val canPost = android.os.Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (canPost) {
            val id = (kind.hashCode() * 31 + prayer.hashCode())
            NotificationManagerCompat.from(context).notify(id, notification)
        }

        // Schedule tomorrow's occurrence (and refresh everything else).
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NotificationScheduler.syncNotifications(context)
            } finally {
                pending.finish()
            }
        }
    }
}
