package com.x2softinc.masthan.data

import kotlinx.serialization.encodeToString
import com.x2softinc.masthan.TIME_DELAY
import com.x2softinc.masthan.TIME_DELAY_LAST_FETCH

class TimeDelayService {
    private val deviceCode = getDeviceCode()

    suspend fun fetchTimeDelay(): TimeDelay {
        // Return cached value if present and it's still the same day
        val cached = Prefs.getString(TIME_DELAY)
        if (cached.isNotEmpty() && Prefs.getString(TIME_DELAY_LAST_FETCH) == todayKey()) {
            runCatching { AppJson.decodeFromString<TimeDelay>(cached) }.getOrNull()?.let { return it }
        }

        val body = ApiClient.authorizedGet(
            path = MasjidProApiPaths.Resources.byDeviceCode("timeDelays", deviceCode),
            feature = "Time Delay",
            timeoutMs = 30_000, // Prevent hanging on slow networks
            retry = { fetchTimeDelay() },
        )

        val decoded = try {
            AppJson.decodeFromString<TimeDelay>(body)
        } catch (e: Exception) {
            throw UserFriendlyError.dataLoading("Time Delay") { fetchTimeDelay() }
        }
        Prefs.putString(TIME_DELAY, AppJson.encodeToString(decoded))
        Prefs.putString(TIME_DELAY_LAST_FETCH, todayKey())
        return decoded
    }
}
