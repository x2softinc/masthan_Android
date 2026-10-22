package com.x2softinc.masthan.data

import kotlinx.serialization.encodeToString
import com.x2softinc.masthan.MASJID_INFO
import com.x2softinc.masthan.MASJID_INFO_LAST_FETCH

class MasjidService {
    private val deviceCode = getDeviceCode()

    suspend fun fetchMasjidInfo(): MasjidInfo {
        // Return cached value if present and it's still the same day
        val cached = Prefs.getString(MASJID_INFO)
        if (cached.isNotEmpty() && Prefs.getString(MASJID_INFO_LAST_FETCH) == todayKey()) {
            runCatching { AppJson.decodeFromString<MasjidInfo>(cached) }.getOrNull()?.let { return it }
        }

        val body = ApiClient.authorizedGet(
            path = MasjidProApiPaths.Resources.byDeviceCode("masjidInfo", deviceCode),
            feature = "Masjid Information",
            retry = { fetchMasjidInfo() },
        )

        val decoded = try {
            AppJson.decodeFromString<MasjidInfo>(body)
        } catch (e: Exception) {
            throw UserFriendlyError.dataLoading("Masjid Information") { fetchMasjidInfo() }
        }
        Prefs.putString(MASJID_INFO, AppJson.encodeToString(decoded))
        Prefs.putString(MASJID_INFO_LAST_FETCH, todayKey())
        return decoded
    }
}
