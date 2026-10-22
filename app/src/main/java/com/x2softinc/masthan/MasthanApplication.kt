package com.x2softinc.masthan

import android.app.Application
import com.x2softinc.masthan.data.LocationTracker
import com.x2softinc.masthan.data.Prefs
import com.x2softinc.masthan.notifications.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MasthanApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        LocationTracker.init(this)
        // Keep alarms in step with the stored settings on every launch.
        appScope.launch { NotificationScheduler.syncNotifications(this@MasthanApplication) }
    }
}
