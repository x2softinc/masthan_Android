package com.x2softinc.masthan.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val preferredCompassSize = if (isTablet) 440.dp else 340.dp
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

    BoxWithConstraints(Modifier.fillMaxSize()) {
    // Leave room for the status text, stats, alignment bar and toggle below the dial
    // so short (e.g. landscape tablet) windows don't push them off-screen.
    val compassSize = min(preferredCompassSize.value, (maxHeight - 250.dp).value.coerceAtLeast(200f)).dp
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (isTablet) 32.dp else 24.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(compassSize), contentAlignment = Alignment.Center) {
            // The dial rotates opposite to the device heading so North stays true; the Qibla
            // pointer is drawn at the absolute Qibla direction on top of it.
            QiblaCompass(
                faceRotation = animateFloatAsState(rotationOfFace, tween(200), label = "face").value,
                needleRotation = animateFloatAsState(rotationOfNeedle, tween(200), label = "needle").value,
                isLocked = isLocked,
                modifier = Modifier.size(compassSize),
            )

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
}

private val DialGold = Color(0xFFC9A24B)
private val DialGreen = Color(0xFF2E7D32)
private val DialRed = Color(0xFFB3261E)

/** Vector compass: a rotating dial with a fixed-direction Qibla pointer and Kaaba marker. */
@Composable
private fun QiblaCompass(faceRotation: Float, needleRotation: Float, isLocked: Boolean, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = if (isLocked) DialGreen else DialGold

    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f
        val dp = r / 170f // design unit: 340dp compass

        // Dial plate with soft radial shading and a gold rim.
        drawCircle(Brush.radialGradient(listOf(surface, surface.copy(alpha = 0.85f).compositeOver(ink.copy(alpha = 0.12f))), c, r), r, c)
        drawCircle(accent, r - 1.5f * dp, c, style = Stroke(3f * dp))
        drawCircle(accent.copy(alpha = 0.45f), r - 9f * dp, c, style = Stroke(1f * dp))

        rotate(faceRotation, c) {
            // Tick marks every 5°, longer every 15°, longest every 45°.
            for (deg in 0 until 360 step 5) {
                val len = when { deg % 45 == 0 -> 14f; deg % 15 == 0 -> 9f; else -> 5f } * dp
                rotate(deg.toFloat(), c) {
                    drawLine(
                        ink.copy(alpha = if (deg % 15 == 0) 0.75f else 0.35f),
                        Offset(c.x, c.y - r + 10f * dp), Offset(c.x, c.y - r + 10f * dp + len),
                        strokeWidth = (if (deg % 45 == 0) 2f else 1f) * dp, cap = StrokeCap.Round,
                    )
                }
            }
            // Cardinal and intercardinal labels, upright along the radius.
            val labels = listOf("N" to 0, "NE" to 45, "E" to 90, "SE" to 135, "S" to 180, "SW" to 225, "W" to 270, "NW" to 315)
            for ((text, deg) in labels) {
                val cardinal = text.length == 1
                val style = TextStyle(
                    color = if (text == "N") DialRed else if (cardinal) ink else ink.copy(alpha = 0.6f),
                    fontSize = (if (cardinal) 22f else 12f).dp.toSp(),
                    fontWeight = FontWeight.Bold,
                )
                val m = measurer.measure(text, style)
                rotate(deg.toFloat(), c) {
                    drawText(m, topLeft = Offset(c.x - m.size.width / 2f, c.y - r + 34f * dp))
                }
            }
            // North marker.
            val north = Path().apply {
                moveTo(c.x, c.y - r + 8f * dp)
                lineTo(c.x - 7f * dp, c.y - r + 22f * dp)
                lineTo(c.x + 7f * dp, c.y - r + 22f * dp)
                close()
            }
            drawPath(north, DialRed)
        }

        // Qibla pointer: tapered arrow from the hub to the Kaaba marker near the rim.
        rotate(needleRotation, c) {
            val tip = c.y - r * 0.66f
            val arrow = Path().apply {
                moveTo(c.x, tip)
                lineTo(c.x + 11f * dp, tip + 30f * dp)
                lineTo(c.x + 4f * dp, tip + 26f * dp)
                lineTo(c.x + 4f * dp, c.y)
                lineTo(c.x - 4f * dp, c.y)
                lineTo(c.x - 4f * dp, tip + 26f * dp)
                lineTo(c.x - 11f * dp, tip + 30f * dp)
                close()
            }
            drawPath(arrow, Brush.verticalGradient(listOf(DialGreen, DialGreen.copy(alpha = 0.55f)), tip, c.y))

            // Kaaba: dark cube with a gold band, sitting inside the tick ring.
            val k = 30f * dp
            val topLeft = Offset(c.x - k / 2f, c.y - r * 0.66f - k - 4f * dp)
            drawRoundRect(Color(0xFF1B1B1B), topLeft, Size(k, k), CornerRadius(4f * dp))
            drawRect(DialGold, Offset(topLeft.x, topLeft.y + k * 0.22f), Size(k, k * 0.14f))
            drawRoundRect(DialGold, topLeft, Size(k, k), CornerRadius(4f * dp), style = Stroke(1.5f * dp))
        }

        // Hub.
        drawCircle(surface, 16f * dp, c)
        drawCircle(accent, 16f * dp, c, style = Stroke(3f * dp))
        drawCircle(accent, 5f * dp, c)
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
                .background(Brush.horizontalGradient(listOf(Color(0xFF2E7D32), Color(0xFF2E7D32).copy(alpha = 0.7f)))),
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
