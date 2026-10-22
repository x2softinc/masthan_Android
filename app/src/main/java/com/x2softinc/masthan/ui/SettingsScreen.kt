package com.x2softinc.masthan.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.x2softinc.masthan.APP_URL
import com.x2softinc.masthan.AUTH_TOKEN
import com.x2softinc.masthan.AppMetadata
import com.x2softinc.masthan.CALC_METH
import com.x2softinc.masthan.CITY
import com.x2softinc.masthan.JURISDICTION
import com.x2softinc.masthan.LATT
import com.x2softinc.masthan.LONG
import com.x2softinc.masthan.NOTIFICATION_ENABLED
import com.x2softinc.masthan.NOTIFY_ADHAN
import com.x2softinc.masthan.NOTIFY_IQAMA
import com.x2softinc.masthan.TOKEN_EXPIRES_AT
import com.x2softinc.masthan.data.AdhanSoundOption
import com.x2softinc.masthan.data.AdhanSoundPreference
import com.x2softinc.masthan.data.AuthService
import com.x2softinc.masthan.data.getDeviceCode
import com.x2softinc.masthan.notifications.NotificationScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private sealed interface SettingsAlert {
    data object NotificationsDenied : SettingsAlert
    data class ReconnectFailed(val message: String) : SettingsAlert
}

private val calculationMethods = mapOf(
    "ISNA" to "Islamic Society of North America",
    "MWL" to "Muslim World League",
    "EGAS" to "Egiptian General Authority of Survey",
    "UQUM" to "Ummul Qura - Makkah",
    "UISK" to "United Islamic Science, Karachi",
    "MUIS" to "Majilis Ugama Islam Singapura",
    "TEH" to "Institute of Geophysics, University of Tehran",
    "JAF" to "Jafari",
)

/** App settings (port of Views/SettingsView.swift). */
@Composable
fun SettingsScreen(onOpenAdhanSound: () -> Unit, onOpenTerms: () -> Unit, onOpenPrivacy: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Prayer-time calculation preferences stored locally on this device
    var calculationMethod by rememberStringPref(CALC_METH, "ISNA")
    var jurisdiction by rememberIntPref(JURISDICTION)

    // Current location preferences
    val latitude by rememberDoublePref(LATT)
    val longitude by rememberDoublePref(LONG)
    val city by rememberStringPref(CITY)

    // App access / authentication state
    val authToken by rememberStringPref(AUTH_TOKEN)
    val tokenExpiry by rememberStringPref(TOKEN_EXPIRES_AT)

    // Notification preferences
    var notificationsEnabled by rememberBooleanPref(NOTIFICATION_ENABLED)
    var notifyAdhan by rememberBooleanPref(NOTIFY_ADHAN)
    var notifyIqama by rememberBooleanPref(NOTIFY_IQAMA)
    val selectedAdhanSound by rememberStringPref(AdhanSoundPreference.STORAGE_KEY, AdhanSoundOption.SYSTEM_DEFAULT.rawValue)

    var isReconnecting by remember { mutableStateOf(false) }
    var activeAlert by remember { mutableStateOf<SettingsAlert?>(null) }

    fun sync() {
        scope.launch { NotificationScheduler.syncNotifications(context) }
    }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            notificationsEnabled = false
            activeAlert = SettingsAlert.NotificationsDenied
        }
        sync()
    }

    fun requestNotificationAuthorizationIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (!NotificationScheduler.notificationsAllowed(context)) {
            notificationsEnabled = false
            activeAlert = SettingsAlert.NotificationsDenied
        }
    }

    var exactAlarmsAllowed by remember { mutableStateOf(NotificationScheduler.canScheduleExactAlarms(context)) }

    // Also covers returning from the system "Alarms & reminders" page.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        exactAlarmsAllowed = NotificationScheduler.canScheduleExactAlarms(context)
        sync()
    }

    val isConnected = authToken.isNotEmpty() && tokenExpiry.isNotEmpty() && !AuthService().isAuthTokenExpired()
    val appName = AppMetadata.appName(context)
    val appVersion = AppMetadata.appVersion(context)
    val buildNumber = AppMetadata.buildNumber(context)

    LazyColumn(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
        // Version info
        item { SectionHeader(Icons.Filled.Info, "Version Info") }
        item {
            SectionCard {
                ValueRow("App", appName, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                HorizontalDivider()
                ValueRow("Version / Build", "$appVersion / $buildNumber", Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                HorizontalDivider()
                ActionRow(Icons.Filled.Download, "Check for Updates") { openUrl(context, APP_URL) }
            }
        }

        // Adhan settings
        item { SectionHeader(Icons.Filled.Schedule, "Adhan Settings") }
        item {
            SectionCard {
                DropdownRow(
                    title = "Calculation Method",
                    selectedLabel = calculationMethods[calculationMethod] ?: calculationMethod,
                    options = calculationMethods.entries.sortedBy { it.value }.map { it.key to it.value },
                    onSelect = { calculationMethod = it },
                )
                HorizontalDivider()
                DropdownRow(
                    title = "Asr Jurisdiction",
                    selectedLabel = if (jurisdiction == 0) "Standard" else "Hanafi",
                    options = listOf("0" to "Standard", "1" to "Hanafi"),
                    onSelect = { jurisdiction = it.toInt() },
                )
                HorizontalDivider()
                ValueRow("Location", formattedLocation(city, latitude, longitude), Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
        item { SectionFooter("These adhan calculation preferences are stored locally on this device.") }

        // Notifications
        item { SectionHeader(Icons.Filled.NotificationsActive, "Notifications") }
        item {
            SectionCard {
                ToggleRow(Icons.Filled.Notifications, "Enable Notifications", notificationsEnabled, enabled = true) {
                    notificationsEnabled = it
                    if (it) requestNotificationAuthorizationIfNeeded()
                    sync()
                }
                HorizontalDivider()
                ToggleRow(null, "Notify Adhan", notifyAdhan, enabled = notificationsEnabled) {
                    notifyAdhan = it
                    sync()
                }
                HorizontalDivider()
                ToggleRow(null, "Notify Iqama", notifyIqama, enabled = notificationsEnabled) {
                    notifyIqama = it
                    sync()
                }
                if (notificationsEnabled && !exactAlarmsAllowed) {
                    HorizontalDivider()
                    ActionRow(Icons.Filled.Alarm, "Allow Exact Alarms") { openExactAlarmSettings(context) }
                }
                HorizontalDivider()
                val soundEnabled = notificationsEnabled && notifyAdhan
                Row(
                    Modifier.fillMaxWidth().clickable(enabled = soundEnabled, onClick = onOpenAdhanSound).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val tint = if (soundEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    Icon(Icons.Filled.VolumeUp, null, tint = tint, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Adhan Sound", Modifier.weight(1f), color = if (soundEnabled) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                    Text(
                        AdhanSoundOption.fromRawValue(selectedAdhanSound)?.displayName ?: AdhanSoundOption.SYSTEM_DEFAULT.displayName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item { SectionFooter("The selected sound is used only for Adhan notifications. Iqama notifications keep the default system sound. Exact alarms keep reminders on time; without them Android may delay them by several minutes.") }

        // App access
        item { SectionHeader(Icons.Filled.Lock, "App Access") }
        item {
            SectionCard {
                ValueRow(
                    "Connection",
                    if (isConnected) "Connected" else "Reconnect Needed",
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    valueColor = if (isConnected) Color(0xFF2E7D32) else Color(0xFFEF6C00),
                )
                if (!isConnected) {
                    HorizontalDivider()
                    if (isReconnecting) {
                        Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) }
                    } else {
                        ActionRow(Icons.Filled.Autorenew, "Reconnect Now") {
                            isReconnecting = true
                            scope.launch {
                                try {
                                    AuthService().getDeviceToken()
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    activeAlert = SettingsAlert.ReconnectFailed(e.message ?: "Unknown error")
                                }
                                isReconnecting = false
                            }
                        }
                    }
                }
            }
        }
        item { SectionFooter("Use this device code to connect the app to your Masjid Pro account.") }

        // Support
        item { SectionHeader(Icons.Filled.HelpOutline, "Support") }
        item {
            SectionCard {
                ActionRow(Icons.Filled.ContentCopy, "Copy Support Information") {
                    copySupportInformation(context, appName, appVersion, buildNumber)
                }
                HorizontalDivider()
                ActionRow(Icons.Filled.Email, "Contact Us") { openUrl(context, "mailto:support@x2softinc.com") }
                HorizontalDivider()
                ActionRow(Icons.Filled.Star, "Rate on Google Play") { openUrl(context, APP_URL) }
            }
        }

        // Legal
        item { SectionHeader(Icons.Filled.Security, "Legal") }
        item {
            SectionCard {
                ActionRow(Icons.Filled.Description, AppMetadata.TERMS_OF_USE_TITLE, onClick = onOpenTerms)
                HorizontalDivider()
                ActionRow(Icons.Filled.PanTool, AppMetadata.PRIVACY_POLICY_TITLE, onClick = onOpenPrivacy)
            }
        }
        item { SectionFooter("The Privacy Policy is hosted online and may be updated from time to time.") }
    }

    when (val alert = activeAlert) {
        SettingsAlert.NotificationsDenied -> AlertDialog(
            onDismissRequest = { activeAlert = null },
            title = { Text("Notifications Not Authorized") },
            text = { Text("Allow notifications in System Settings to receive prayer reminders.") },
            confirmButton = { TextButton(onClick = { openAppSettings(context); activeAlert = null }) { Text("Open Settings") } },
            dismissButton = { TextButton(onClick = { activeAlert = null }) { Text("Cancel") } },
        )
        is SettingsAlert.ReconnectFailed -> AlertDialog(
            onDismissRequest = { activeAlert = null },
            title = { Text("Unable to Reconnect") },
            text = { Text(alert.message) },
            confirmButton = { TextButton(onClick = { activeAlert = null }) { Text("OK") } },
        )
        null -> Unit
    }
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < 31) return
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }.onFailure { openAppSettings(context) }
}

private fun formattedLocation(city: String, latitude: Double, longitude: Double): String = when {
    city.isNotEmpty() -> city
    latitude != 0.0 || longitude != 0.0 -> "%.4f, %.4f".format(latitude, longitude)
    else -> "Not Set"
}

private fun copySupportInformation(context: Context, appName: String, version: String, build: String) {
    val info = "$appName\nVersion: $version ($build)\nDevice Code: ${getDeviceCode()}"
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Support information", info))
    // Android 13+ shows its own confirmation.
    if (Build.VERSION.SDK_INT < 33) Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}

// region Building blocks

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionFooter(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
    )
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) { Column { content() } }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ToggleRow(icon: ImageVector?, title: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(title, Modifier.weight(1f), color = if (enabled) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

/** Picker row that opens a menu of (key, label) options. */
@Composable
private fun DropdownRow(title: String, selectedLabel: String, options: List<Pair<String, String>>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = true }.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                Spacer(Modifier.width(12.dp))
                Text(selectedLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                Icon(Icons.Filled.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { onSelect(key); expanded = false })
            }
        }
    }
}

// endregion
