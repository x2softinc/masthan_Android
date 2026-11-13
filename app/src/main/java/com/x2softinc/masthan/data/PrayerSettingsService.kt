package com.x2softinc.masthan.data

import kotlinx.serialization.encodeToString
import com.x2softinc.masthan.PRAYER_SETTINGS
import com.x2softinc.masthan.PRAYER_SETTINGS_LAST_FETCH

class PrayerSettingsService {
    suspend fun fetchPrayerSettings(): PrayerSetting {
        val cached = Prefs.getString(PRAYER_SETTINGS)
        if (cached.isNotEmpty() && Prefs.getString(PRAYER_SETTINGS_LAST_FETCH) == todayKey()) {
            runCatching { AppJson.decodeFromString<PrayerSetting>(cached) }.getOrNull()?.let { return it }
        }

        val body = ApiClient.authorizedGet(
            path = MasjidProApiPaths.Resources.byDeviceCode("prayerSettings", getDeviceCode()),
            feature = "Prayer Settings",
            retry = { fetchPrayerSettings() },
        )

        val settings = try {
            AppJson.decodeFromString<PrayerSetting>(body)
        } catch (e: Exception) {
            throw UserFriendlyError.dataLoading("Prayer Settings") { fetchPrayerSettings() }
        }
        Prefs.putString(PRAYER_SETTINGS, AppJson.encodeToString(settings))
        Prefs.putString(PRAYER_SETTINGS_LAST_FETCH, todayKey())
        return settings
    }
}
