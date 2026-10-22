package com.x2softinc.masthan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.x2softinc.masthan.data.LocationTracker
import com.x2softinc.masthan.ui.AppVersionGate
import com.x2softinc.masthan.ui.MainScreen
import com.x2softinc.masthan.ui.MasthanTheme

/** Port of MasthanApp.swift: the version gate wraps the main content. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MasthanTheme {
                AppVersionGate(storeUrl = APP_URL) {
                    MainScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The user may have changed the location permission in system settings.
        LocationTracker.refreshAuthorization()
    }
}
