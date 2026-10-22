package com.x2softinc.masthan.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.x2softinc.masthan.CITY
import com.x2softinc.masthan.LATT
import com.x2softinc.masthan.LONG
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

enum class LocationAuthorization { NOT_DETERMINED, DENIED, AUTHORIZED }

/** Compass heading in degrees clockwise from north. */
data class Heading(val trueHeading: Double?, val magneticHeading: Double)

/**
 * App-wide location + compass source (port of Managers/LocationManager.swift).
 * Coordinates and city are persisted in [Prefs] so the rest of the app can read them.
 */
object LocationTracker {
    private const val REQUESTED_KEY = "locationPermissionRequested"
    private const val REGEOCODE_DISTANCE_M = 1000f

    var authorization by mutableStateOf(LocationAuthorization.NOT_DETERMINED)
        private set
    var location by mutableStateOf<Location?>(null)
        private set
    var heading by mutableStateOf<Heading?>(null)
        private set

    private lateinit var appContext: Context
    private var lastGeocoded: Location? = null
    private var updatesStarted = false
    private var headingStarted = false

    private var smoothedSin = 0.0
    private var smoothedCos = 0.0
    private var hasSmoothed = false

    fun init(context: Context) {
        appContext = context.applicationContext
        refreshAuthorization()
        if (authorization == LocationAuthorization.AUTHORIZED) start()
    }

    fun markPermissionRequested() = Prefs.putBoolean(REQUESTED_KEY, true)

    /** Re-evaluates the runtime permission and starts/stops updates accordingly. */
    fun refreshAuthorization() {
        val granted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        authorization = when {
            granted -> LocationAuthorization.AUTHORIZED
            Prefs.getBoolean(REQUESTED_KEY) -> LocationAuthorization.DENIED
            else -> LocationAuthorization.NOT_DETERMINED
        }
        if (granted) start() else stop()
    }

    // region Location

    private val locationListener = LocationListener { update -> onLocation(update) }

    @SuppressLint("MissingPermission")
    fun start() {
        if (updatesStarted || authorization != LocationAuthorization.AUTHORIZED) return
        val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        for (provider in providers) {
            runCatching {
                manager.requestLocationUpdates(provider, 10_000L, 25f, locationListener, Looper.getMainLooper())
                manager.getLastKnownLocation(provider)?.let { if (location == null) onLocation(it) }
            }
        }
        updatesStarted = providers.isNotEmpty()
    }

    fun stop() {
        if (!updatesStarted) return
        val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        runCatching { manager.removeUpdates(locationListener) }
        updatesStarted = false
    }

    private fun onLocation(update: Location) {
        location = update
        Prefs.putDouble(LATT, update.latitude)
        Prefs.putDouble(LONG, update.longitude)

        // Reverse geocode the city name, but avoid doing it on every update.
        val last = lastGeocoded
        if (Prefs.getString(CITY).isEmpty() || last == null || update.distanceTo(last) > REGEOCODE_DISTANCE_M) {
            reverseGeocode(update)
        }
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocode(target: Location) {
        if (!Geocoder.isPresent()) return
        val geocoder = Geocoder(appContext, Locale.getDefault())
        fun apply(address: android.location.Address?) {
            val name = address?.locality ?: address?.adminArea ?: return
            Prefs.putString(CITY, name)
            lastGeocoded = target
        }
        if (Build.VERSION.SDK_INT >= 33) {
            geocoder.getFromLocation(target.latitude, target.longitude, 1) { apply(it.firstOrNull()) }
        } else {
            Thread {
                runCatching { geocoder.getFromLocation(target.latitude, target.longitude, 1) }
                    .getOrNull()?.firstOrNull().let(::apply)
            }.start()
        }
    }

    // endregion

    // region Heading

    private val sensorListener = object : SensorEventListener {
        private val rotation = FloatArray(9)
        private val orientation = FloatArray(3)

        override fun onSensorChanged(event: SensorEvent) {
            SensorManager.getRotationMatrixFromVector(rotation, event.values)
            SensorManager.getOrientation(rotation, orientation)
            val azimuth = Math.toDegrees(orientation[0].toDouble())
            val magnetic = (azimuth + 360.0) % 360.0

            // Smooth on the unit circle so 359° -> 1° does not swing the long way round.
            val rad = Math.toRadians(magnetic)
            if (!hasSmoothed) {
                smoothedSin = sin(rad); smoothedCos = cos(rad); hasSmoothed = true
            } else {
                val alpha = 0.2
                smoothedSin += alpha * (sin(rad) - smoothedSin)
                smoothedCos += alpha * (cos(rad) - smoothedCos)
            }
            val smoothed = (Math.toDegrees(atan2(smoothedSin, smoothedCos)) + 360.0) % 360.0

            val loc = location
            val trueHeading = loc?.let {
                val declination = GeomagneticField(
                    it.latitude.toFloat(), it.longitude.toFloat(), it.altitude.toFloat(), System.currentTimeMillis()
                ).declination
                (smoothed + declination + 360.0) % 360.0
            }
            heading = Heading(trueHeading = trueHeading, magneticHeading = smoothed)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun startHeading() {
        if (headingStarted) return
        val sensors = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) ?: return
        hasSmoothed = false
        headingStarted = sensors.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stopHeading() {
        if (!headingStarted) return
        val sensors = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensors.unregisterListener(sensorListener)
        headingStarted = false
        heading = null
    }

    // endregion
}
