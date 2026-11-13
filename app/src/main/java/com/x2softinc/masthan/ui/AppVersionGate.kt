package com.x2softinc.masthan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.x2softinc.masthan.data.VersionService
import kotlinx.coroutines.CancellationException

/** Blocks the app behind an "Update Required" card when the server forces a newer version. */
@Composable
fun AppVersionGate(storeUrl: String, content: @Composable () -> Unit) {
    val context = LocalContext.current
    var requiresUpdate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        requiresUpdate = try {
            VersionService(context).checkVersion()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // On any error (network/decoding/etc.) let the app continue, as iOS does.
            false
        }
    }

    Box(Modifier.fillMaxSize()) {
        content()

        if (requiresUpdate) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                Surface(shape = RoundedCornerShape(20.dp), shadowElevation = 10.dp, modifier = Modifier.padding(24.dp)) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(Icons.Filled.ArrowCircleDown, null, tint = Color(0xFF1E88E5), modifier = Modifier.size(48.dp))
                        Text("Update Required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "A new version of the app is available and is required to continue.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = { openUrl(context, storeUrl) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Update Now")
                        }
                    }
                }
            }
        }
    }
}
