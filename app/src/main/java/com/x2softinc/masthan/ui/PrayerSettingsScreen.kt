package com.x2softinc.masthan.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.x2softinc.masthan.PRAYER_SETTINGS
import com.x2softinc.masthan.data.AppJson
import com.x2softinc.masthan.data.PrayerSetting
import com.x2softinc.masthan.data.PrayerSettingsService
import com.x2softinc.masthan.data.UserFriendlyError
import com.x2softinc.masthan.util.displayTime
import kotlinx.coroutines.CancellationException

/** Server-synced prayer settings (port of Views/PrayerSettingsView.swift). */
@Composable
fun PrayerSettingsScreen() {
    val settingsJson by rememberStringPref(PRAYER_SETTINGS)
    val settings = remember(settingsJson) {
        if (settingsJson.isEmpty()) null else runCatching { AppJson.decodeFromString<PrayerSetting>(settingsJson) }.getOrNull()
    }
    var isLoading by remember { mutableStateOf(false) }
    var currentError by remember { mutableStateOf<UserFriendlyError?>(null) }

    suspend fun loadSettings() {
        isLoading = true
        currentError = null
        try {
            PrayerSettingsService().fetchPrayerSettings()
        } catch (e: CancellationException) {
            throw e
        } catch (e: UserFriendlyError) {
            currentError = e
        } catch (e: Exception) {
            currentError = UserFriendlyError.dataLoading("Prayer Settings") { loadSettings() }
        }
        isLoading = false
    }

    LaunchedEffect(Unit) { loadSettings() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text("Loading prayer settings…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            SettingsCard(settings)
        }
    }

    UserFriendlyErrorDialog(currentError) { currentError = null }
}

@Composable
private fun SettingsCard(settings: PrayerSetting?) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Settings, null, tint = MaterialTheme.colorScheme.primary)
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Prayer Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Synced from server", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider()

            ValueRow("Calculation Method", settings?.calcMethod ?: "MWL")
            ValueRow("Maintenance Method", (settings?.maintMethod ?: 0).toString())
            ValueRow("Jumma Time", displayTime(settings?.jumma))
            ValueRow("Jumma 2 Time", displayTime(settings?.jumma2))
            ValueRow("Asr Method", (settings?.asrMethod ?: 0).toString())
            ValueRow("Hijri Adjustment", (settings?.hijriAdjustment ?: 0).toString())
            ValueRow("Round Minutes", (settings?.roundMinutes ?: 0).toString())

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Maghrib Delays (minutes)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ValueRow("Maghrib Delay (min)", (settings?.maghribDelay ?: 3).toString())
            }
        }
    }
}
