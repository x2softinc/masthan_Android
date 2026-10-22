package com.x2softinc.masthan.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.x2softinc.masthan.data.ErrorIcon
import com.x2softinc.masthan.data.RecoveryAction
import com.x2softinc.masthan.data.UserFriendlyError
import kotlinx.coroutines.launch

fun ErrorIcon.vector(): ImageVector = when (this) {
    ErrorIcon.WIFI_OFF -> Icons.Filled.WifiOff
    ErrorIcon.LOCK -> Icons.Filled.Lock
    ErrorIcon.LOCATION_OFF -> Icons.Filled.LocationOff
    ErrorIcon.NOTIFICATIONS_OFF -> Icons.Filled.NotificationsOff
    ErrorIcon.WARNING -> Icons.Filled.Warning
    ErrorIcon.SERVER -> Icons.Filled.Storage
    ErrorIcon.TIMEOUT -> Icons.Filled.Timer
}

fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

/** Alert for a [UserFriendlyError] (port of the `.userFriendlyAlert` view modifier). */
@Composable
fun UserFriendlyErrorDialog(error: UserFriendlyError?, onDismiss: () -> Unit) {
    if (error == null) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val action = error.recoveryAction

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(error.icon.vector(), contentDescription = null) },
        title = { Text(error.title) },
        text = { Text(error.message) },
        confirmButton = {
            if (action != null) {
                TextButton(onClick = {
                    when (action) {
                        is RecoveryAction.Retry -> scope.launch {
                            onDismiss()
                            action.action()
                        }
                        RecoveryAction.OpenSettings -> {
                            openAppSettings(context)
                            onDismiss()
                        }
                        RecoveryAction.Contact -> {
                            openUrl(context, "mailto:support@alburoojiqama.com")
                            onDismiss()
                        }
                        RecoveryAction.None -> onDismiss()
                    }
                }) { Text(action.buttonTitle) }
            } else {
                TextButton(onClick = onDismiss) { Text("OK") }
            }
        },
        dismissButton = {
            if (action != null) TextButton(onClick = onDismiss) { Text("Dismiss") }
        },
    )
}

/** Two-column "title ... value" row (Settings / Prayer Settings). */
@Composable
fun ValueRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    monospaced: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            value,
            color = valueColor,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = if (monospaced) androidx.compose.ui.text.font.FontFamily.Monospace else null,
        )
    }
}
