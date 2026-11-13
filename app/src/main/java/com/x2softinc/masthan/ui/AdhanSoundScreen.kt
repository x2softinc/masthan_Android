package com.x2softinc.masthan.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.x2softinc.masthan.data.AdhanSoundOption
import com.x2softinc.masthan.data.AdhanSoundPreference
import com.x2softinc.masthan.notifications.NotificationScheduler
import kotlinx.coroutines.launch

/** Picker for the Adhan notification sound with preview (port of Views/AdhanSoundSelectionView.swift). */
@Composable
fun AdhanSoundScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedAdhanSound by rememberStringPref(AdhanSoundPreference.STORAGE_KEY, AdhanSoundOption.SYSTEM_DEFAULT.rawValue)

    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var previewing by remember { mutableStateOf<AdhanSoundOption?>(null) }

    fun stopSound() {
        player?.runCatching { stop(); release() }
        player = null
        previewing = null
    }

    fun playSound(option: AdhanSoundOption) {
        val resourceName = option.resourceName ?: return
        val id = context.resources.getIdentifier(resourceName, "raw", context.packageName)
        if (id == 0) return
        stopSound()
        runCatching {
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            context.resources.openRawResourceFd(id).use { mp.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            mp.setOnCompletionListener { stopSound() }
            mp.prepare()
            mp.start()
            player = mp
            previewing = option
        }.onFailure { stopSound() }
    }

    DisposableEffect(Unit) { onDispose { stopSound() } }

    // Selection changes stop any preview and rebuild the notification schedule.
    LaunchedEffect(selectedAdhanSound) {
        stopSound()
        NotificationScheduler.syncNotifications(context)
    }

    LazyColumn(Modifier.fillMaxWidth()) {
        item {
            Text(
                "Select a sound",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
            )
        }
        items(AdhanSoundOption.entries.toList()) { option ->
            val isSelected = selectedAdhanSound == option.rawValue
            val isPreviewing = previewing == option && player?.isPlaying == true

            Row(
                Modifier.fillMaxWidth().clickable { selectedAdhanSound = option.rawValue }.padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(option.displayName, Modifier.weight(1f).padding(vertical = 14.dp))
                if (isSelected) {
                    Icon(Icons.Filled.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                if (option.resourceName != null) {
                    IconButton(
                        onClick = { if (isPreviewing) stopSound() else playSound(option) },
                        modifier = Modifier.semantics {
                            contentDescription = if (isPreviewing) "Stop ${option.displayName}" else "Play ${option.displayName}"
                        },
                    ) {
                        Icon(
                            if (isPreviewing) Icons.Filled.StopCircle else Icons.Filled.PlayCircle, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp),
                        )
                    }
                } else {
                    Row(Modifier.width(48.dp)) {}
                }
            }
            HorizontalDivider()
        }
        item {
            Text(
                "The selected sound is used only for Adhan notifications. Iqama notifications keep the default system sound.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}
