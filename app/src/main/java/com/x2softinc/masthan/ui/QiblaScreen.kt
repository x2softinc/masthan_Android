package com.x2softinc.masthan.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x2softinc.masthan.R
import com.x2softinc.masthan.data.LocationAuthorization
import com.x2softinc.masthan.data.LocationTracker
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// Kaaba coordinates
private const val KAABA_LATITUDE = 21.4225
private const val KAABA_LONGITUDE = 39.8262

/** Compass pointing to the Qibla (port of Views/QiblaView.swift). */
@Composable
fun QiblaScreen() {
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    val compassSize = if (isTablet) 440.dp else 340.dp
    val haptics = LocalHapticFeedback.current

    var useTrueNorth by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var lastLockHapticAt by remember { mutableStateOf(0L) }

    // The compass sensor only runs while this screen is visible.
    DisposableEffect(Unit) {
        LocationTracker.startHeading()
        onDispose { LocationTracker.stopHeading() }
    }

    val location = LocationTracker.location
    val heading = LocationTracker.heading

    val headingDegrees: Double = if (useTrueNorth) {
        heading?.trueHeading ?: heading?.magneticHeading ?: 0.0
    } else {
        heading?.magneticHeading ?: heading?.trueHeading ?: 0.0
    }

    // Bearing from user to Kaaba in degrees (0° = true North, clockwise)
    val qiblaBearing: Double? = location?.let { bearingToKaaba(it.latitude, it.longitude) }
    val distanceKm: Double? = location?.let { haversineMeters(it.latitude, it.longitude, KAABA_LATITUDE, KAABA_LONGITUDE) / 1000.0 }

    // Needle should point to the Qibla bearing, compensating for the compass rotation.
    val qiblaPointerRotation = if (qiblaBearing != null) qiblaBearing - headingDegrees else 0.0

    val alignmentError: Double = qiblaBearing?.let {
        val diff = (it - headingDegrees) % 360
        val normalized = if (diff < -180) diff + 360 else if (diff > 180) diff - 360 else diff
        abs(normalized)
    } ?: 180.0
    val alignmentProgress = min(max(alignmentError / 45.0, 0.0), 1.0).toFloat() // 0..45° -> 0..1

    LaunchedEffect(alignmentError) {
        val wasLocked = isLocked
        isLocked = alignmentError <= 3.0 // tolerance in degrees
        // Light haptic when entering the lock state, throttled to 1 per 0.8s
        if (isLocked && !wasLocked) {
            val now = System.currentTimeMillis()
            if (now - lastLockHapticAt >= 800) {
                lastLockHapticAt = now
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    val rotationOfFace = rememberUnwrappedAngle(-headingDegrees.toFloat())
    val rotationOfNeedle = rememberUnwrappedAngle(qiblaPointerRotation.toFloat())
    val lockScale by animateFloatAsState(if (isLocked) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium), label = "lock")

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (isTablet) 32.dp else 24.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(compassSize), contentAlignment = Alignment.Center) {
            // Background compass and outer ring rotate opposite to the device heading
            // to keep North pointing up on screen.
            Box(Modifier.size(compassSize).rotate(animateFloatAsState(rotationOfFace, tween(200), label = "face").value)) {
                Image(painterResource(R.drawable.compass_face), null, Modifier.size(compassSize), contentScale = ContentScale.Fit)
                Image(
                    painterResource(R.drawable.outer_ring), null,
                    Modifier
                        .size(compassSize)
                        .then(
                            if (isLocked) Modifier.shadow(if (isTablet) 20.dp else 15.dp, CircleShape, ambientColor = Color.Green, spotColor = Color.Green)
                            else Modifier
                        ),
                    contentScale = ContentScale.Fit,
                )
            }

            // Needle and Kaaba point to the absolute Qibla direction.
            val radius = compassSize / 2
            val kaabaSize = if (isTablet) 46.dp else 32.dp
            Box(
                Modifier.size(compassSize).rotate(animateFloatAsState(rotationOfNeedle, tween(200), label = "needle").value),
                contentAlignment = Alignment.Center,
            ) {
                // The needle artwork is a square; it spans from the centre up to the rim.
                Image(
                    painterResource(R.drawable.needle), null,
                    Modifier.size(radius).offset(y = -radius / 2),
                    contentScale = ContentScale.Fit,
                )
                // Kaaba icon at the tip of the needle
                Image(
                    painterResource(R.drawable.kaaba), null,
                    Modifier.size(kaabaSize).offset(y = -radius),
                    contentScale = ContentScale.Fit,
                )
            }

            // Centre cap on top of everything
            Image(painterResource(R.drawable.center_cap), null, Modifier.size(if (isTablet) 62.dp else 38.dp), contentScale = ContentScale.Fit)

            // Lock icon when aligned
            if (lockScale > 0.01f) {
                Box(
                    Modifier
                        .size(if (isTablet) 70.dp else 46.dp)
                        .scale(lockScale)
                        .alpha(lockScale.coerceIn(0f, 1f))
                        .clip(CircleShape)
                        .background(Color.Green.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Lock, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(if (isTablet) 24.dp else 18.dp))
                }
            }
        }

        Column(Modifier.padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(statusText(), style = if (isTablet) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)

            // Bearing and distance
            Row(horizontalArrangement = Arrangement.spacedBy(if (isTablet) 20.dp else 16.dp)) {
                InfoStat(qiblaBearing?.let { "%.0f°".format(it) } ?: "--°", "Bearing", isTablet, available = qiblaBearing != null)
                InfoStat(distanceKm?.let { "%.1f km".format(it) } ?: "-- km", "Distance", isTablet, available = distanceKm != null)
            }
        }

        AlignmentBar(
            progress = alignmentProgress,
            isTablet = isTablet,
            modifier = Modifier
                .padding(horizontal = if (isTablet) 32.dp else 16.dp)
                .padding(top = 4.dp)
                .widthIn(max = if (isTablet) 500.dp else Dp.Unspecified)
                .fillMaxWidth()
                .height(if (isTablet) 12.dp else 8.dp),
        )

        Row(
            Modifier
                .padding(horizontal = if (isTablet) 32.dp else 16.dp)
                .widthIn(max = if (isTablet) 500.dp else Dp.Unspecified)
                .fillMaxWidth()
                .semantics { contentDescription = "Use True North for heading" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Use True North", style = if (isTablet) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium)
            Switch(
                checked = useTrueNorth,
                onCheckedChange = { useTrueNorth = it },
                colors = SwitchDefaults.colors(checkedTrackColor = Color.Red, checkedThumbColor = Color.White),
            )
        }
    }
}

@Composable
private fun statusText(): String = when (LocationTracker.authorization) {
    LocationAuthorization.NOT_DETERMINED -> "Requesting location permission…"
    LocationAuthorization.DENIED -> "Location permission needed to show Qibla."
    LocationAuthorization.AUTHORIZED -> when {
        LocationTracker.location == null -> "Getting your location…"
        LocationTracker.heading == null -> "Getting your heading…"
        else -> "Point the arrow toward the Qibla."
    }
}

@Composable
private fun InfoStat(value: String, label: String, isTablet: Boolean, available: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            value,
            fontSize = if (isTablet) 22.sp else 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (available) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(label, fontSize = if (isTablet) 12.sp else 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** progress: 0..1 where 0 is perfect alignment. */
@Composable
private fun AlignmentBar(progress: Float, isTablet: Boolean, modifier: Modifier = Modifier) {
    val fraction by animateFloatAsState(1f - progress, tween(200), label = "alignment")
    val minFraction = if (isTablet) 0.03f else 0.05f // never fully zero, to keep it visible
    Box(modifier.clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(max(minFraction, fraction))
                .clip(CircleShape)
                .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFF2E7D32), Color(0xFF2E7D32).copy(alpha = 0.7f)))),
        )
    }
}

/** Keeps a continuous angle so animating from 359° to 1° turns 2°, not 358°. */
@Composable
private fun rememberUnwrappedAngle(target: Float): Float {
    var unwrapped by remember { mutableStateOf(target) }
    var last by remember { mutableStateOf(target) }
    if (target != last) {
        var delta = (target - last) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        unwrapped += delta
        last = target
    }
    return unwrapped
}

private fun bearingToKaaba(lat: Double, lon: Double): Double {
    val lat1 = Math.toRadians(lat)
    val lon1 = Math.toRadians(lon)
    val lat2 = Math.toRadians(KAABA_LATITUDE)
    val lon2 = Math.toRadians(KAABA_LONGITUDE)
    val dLon = lon2 - lon1

    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    var bearing = Math.toDegrees(atan2(y, x))
    if (bearing < 0) bearing += 360
    return bearing
}

private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}
