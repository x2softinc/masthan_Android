package com.x2softinc.masthan.data

import android.content.Context
import android.content.SharedPreferences
import com.x2softinc.masthan.BASE_URL
import com.x2softinc.masthan.DEVICE_CODE
import kotlinx.serialization.json.Json
import java.time.LocalDate

/** Shared JSON configuration for API payloads and cached blobs. */
val AppJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
}

/**
 * Local key/value storage. Android counterpart of the iOS `@AppStorage` / `UserDefaults`
 * usage; the keys come from Constants.kt.
 */
object Prefs {
    private const val FILE = "masthan_prefs"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        }
    }

    val shared: SharedPreferences get() = prefs

    fun getString(key: String, default: String = ""): String = prefs.getString(key, default) ?: default
    fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()

    fun getBoolean(key: String, default: Boolean = false): Boolean = prefs.getBoolean(key, default)
    fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()

    fun getInt(key: String, default: Int = 0): Int = prefs.getInt(key, default)
    fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()

    fun getDouble(key: String, default: Double = 0.0): Double =
        if (prefs.contains(key)) java.lang.Double.longBitsToDouble(prefs.getLong(key, 0L)) else default

    fun putDouble(key: String, value: Double) =
        prefs.edit().putLong(key, java.lang.Double.doubleToRawLongBits(value)).apply()
}

/** "yyyy-MM-dd" of today, used as the once-a-day cache key. */
fun todayKey(): String = LocalDate.now().toString()

/** Port of helper/DeviceCodeHelper.swift (the iOS app forces this device code too). */
fun getDeviceCode(): String {
    var deviceCode = Prefs.getString(DEVICE_CODE)
    if (deviceCode.isEmpty()) deviceCode = "26277142"
    deviceCode = "26277142" // force
    Prefs.putString(DEVICE_CODE, deviceCode)
    return deviceCode
}

/** Port of helper/UrlHelper.swift. */
fun getBaseUrl(): String {
    var baseUrl = Prefs.getString(BASE_URL)
    if (baseUrl.isEmpty()) {
        baseUrl = "http://masjidpro.x2softinc.com"
        Prefs.putString(BASE_URL, baseUrl)
    }
    return baseUrl
}
