package com.x2softinc.masthan.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.x2softinc.masthan.data.MasjidInfo
import com.x2softinc.masthan.data.MasjidService
import kotlinx.coroutines.CancellationException

/**
 * Masjid details (port of Views/MasjidView.swift).
 *
 * iOS embeds an Apple Maps snapshot; embedding Google Maps would require an API key, so the
 * Android version shows the coordinates and hands off to the user's maps app for directions.
 */
@Composable
fun MasjidScreen() {
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    var masjidInfo by remember { mutableStateOf<MasjidInfo?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            masjidInfo = MasjidService().fetchMasjidInfo()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorMessage = e.message
        }
        isLoading = false
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(if (isTablet) 24.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val info = masjidInfo
        when {
            isLoading -> CenteredMessage(top = 100.dp) {
                CircularProgressIndicator()
                Text("Loading Masjid info…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            errorMessage != null -> CenteredMessage(top = 100.dp) {
                Icon(Icons.Filled.Warning, null, tint = Color(0xFFFF9800), modifier = Modifier.size(40.dp))
                Text(errorMessage.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            info != null -> MasjidCard(info, isTablet)
            else -> CenteredMessage(top = 100.dp) {
                Text("No masjid information available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CenteredMessage(top: androidx.compose.ui.unit.Dp, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = top),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun MasjidCard(info: MasjidInfo, isTablet: Boolean) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(24.dp)

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, AccentColor.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(if (isTablet) 28.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (isTablet) 24.dp else 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp)) {
                Box(
                    Modifier.size(if (isTablet) 60.dp else 44.dp).clip(CircleShape).background(AccentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Apartment, null, tint = AccentColor, modifier = Modifier.size(if (isTablet) 32.dp else 24.dp))
                }
                Text(
                    info.masjidName ?: "Unknown Masjid",
                    style = if (isTablet) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Column(Modifier.padding(start = if (isTablet) 24.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(if (isTablet) 12.dp else 8.dp)) {
                info.address.nonEmpty()?.let { InfoRow(Icons.Filled.PinDrop, "Address", it, isTablet) }
                info.address1.nonEmpty()?.let { InfoRow(Icons.Filled.Place, "Address Line 2", it, isTablet) }

                val cityState = listOfNotNull(info.city, info.state).filter { it.isNotEmpty() }.joinToString(", ")
                if (cityState.isNotEmpty()) InfoRow(Icons.Filled.LocationCity, "City/State", cityState, isTablet)

                info.postalCode.nonEmpty()?.let { InfoRow(Icons.Filled.Numbers, "Postal Code", it, isTablet) }
                info.country.nonEmpty()?.let { InfoRow(Icons.Filled.Language, "Country", it, isTablet) }
                info.phone.nonEmpty()?.let { InfoRow(Icons.Filled.Phone, "Phone", it, isTablet) }
                info.email.nonEmpty()?.let { InfoRow(Icons.Filled.Email, "Email", it, isTablet) }
                info.url.nonEmpty()?.let { InfoRow(Icons.Filled.Link, "Website", it, isTablet) }
            }

            if (info.latitude != 0.0 || info.longitude != 0.0) {
                HorizontalDivider()
                InfoRow(Icons.Filled.Directions, "Coordinates", "%.5f, %.5f".format(info.latitude, info.longitude), isTablet)

                Button(
                    onClick = { openDirections(context, info.latitude, info.longitude, info.masjidName ?: "Masjid") },
                    colors = ButtonDefaults.buttonColors(containerColor = BarColor, contentColor = BarContentColor),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.DirectionsCar, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Driving Directions",
                        style = if (isTablet) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

private fun String?.nonEmpty(): String? = this?.takeIf { it.isNotEmpty() }

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String, isTablet: Boolean) {
    Row(Modifier.padding(vertical = if (isTablet) 4.dp else 2.dp), horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp)) {
        Icon(icon, null, tint = AccentColor, modifier = Modifier.size(if (isTablet) 24.dp else 20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = if (isTablet) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = if (isTablet) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Opens turn-by-turn driving directions in the user's maps app. */
private fun openDirections(context: Context, latitude: Double, longitude: Double, name: String) {
    val navigation = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$latitude,$longitude&mode=d"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(name)})"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(navigation) }
        .recoverCatching { context.startActivity(geo) }
}
