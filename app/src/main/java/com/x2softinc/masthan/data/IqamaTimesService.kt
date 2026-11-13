package com.x2softinc.masthan.data

import com.x2softinc.masthan.ANNUAL_TIMES
import com.x2softinc.masthan.ANNUAL_TIMES_LAST_FETCH
import kotlinx.serialization.builtins.ListSerializer

class IqamaTimesService {
    // NOTE: Despite the name, this service actually fetches ADHAN times from the masjid's API.
    // The iqama times are calculated separately in TimesViewModel as: Adhan + delay.

    /** Fetches annual adhan times from the API (not iqama times). */
    suspend fun fetchIqamaTimes(): List<AnnualTime> {
        val serializer = ListSerializer(AnnualTime.serializer())

        val cached = Prefs.getString(ANNUAL_TIMES)
        if (cached.isNotEmpty() && Prefs.getString(ANNUAL_TIMES_LAST_FETCH) == todayKey()) {
            runCatching { AppJson.decodeFromString(serializer, cached) }.getOrNull()?.let { return it }
        }

        val body = ApiClient.authorizedGet(
            path = MasjidProApiPaths.Resources.byDeviceCode("annualTimes", getDeviceCode()) + "?recordType=ADHAN",
            feature = "Prayer Times",
            retry = { fetchIqamaTimes() },
        )

        val decoded = try {
            AppJson.decodeFromString(serializer, body)
        } catch (e: Exception) {
            throw UserFriendlyError.dataLoading("Prayer Times") { fetchIqamaTimes() }
        }
        Prefs.putString(ANNUAL_TIMES, AppJson.encodeToString(serializer, decoded))
        Prefs.putString(ANNUAL_TIMES_LAST_FETCH, todayKey())
        return decoded
    }
}
