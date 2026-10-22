package com.x2softinc.masthan.data

import android.content.Context
import com.x2softinc.masthan.AppMetadata
import com.x2softinc.masthan.BASE_URL
import com.x2softinc.masthan.BuildConfig

/** Secrets and endpoints for the version check (port of SecureConfiguration). */
object SecureConfiguration {
    /** Injected at build time from local.properties / the VERSION_API_KEY env variable. */
    val versionApiKey: String get() = BuildConfig.VERSION_API_KEY

    /** Version check URL (HTTPS only). */
    const val VERSION_CHECK_URL = "https://nexus.x2softinc.com/api/v1/apps/by-code/ALBIOS1"
}

open class VersionService(private val context: Context) {

    /** Returns true when the app must be updated before it can be used. */
    open suspend fun checkVersion(): Boolean {
        val response = try {
            Http.request(
                method = "GET",
                url = SecureConfiguration.VERSION_CHECK_URL,
                headers = mapOf(
                    "x-api-key" to SecureConfiguration.versionApiKey,
                    "Content-Type" to "application/json",
                ),
                timeoutMs = 30_000,
            )
        } catch (e: Exception) {
            throw VersionException(e.message ?: "Network error", e)
        }

        if (response.code != 200) throw VersionException("Unexpected status code: ${response.code}")

        val versionInfo = try {
            AppJson.decodeFromString<VersionInfo>(response.body)
        } catch (e: Exception) {
            throw VersionException(e.message ?: "Decoding failed", e)
        }

        val appVersion = AppMetadata.appVersion(context)

        val url = versionInfo.baseURL
        if (!url.isNullOrEmpty() && versionInfo.overrideURL == true) {
            Prefs.putString(BASE_URL, url)
        }

        val currentVersion = (versionInfo.version ?: "").trim()
        val force = versionInfo.forceVersion ?: false
        if (currentVersion.isEmpty()) return false

        return force && isNewerVersion(currentVersion, appVersion)
    }
}

class VersionException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Returns true when [candidate] is a newer release than [current].
 * Compares numeric segments only, ignoring pre-release and build metadata.
 */
internal fun isNewerVersion(candidate: String, current: String): Boolean {
    fun segments(v: String): List<Int> =
        v.substringBefore("-").substringBefore("+").split(".").map { it.toIntOrNull() ?: 0 }

    val c = segments(candidate)
    val cur = segments(current)
    for (i in 0 until maxOf(c.size, cur.size)) {
        val a = c.getOrElse(i) { 0 }
        val b = cur.getOrElse(i) { 0 }
        if (a != b) return a > b
    }
    return false
}
