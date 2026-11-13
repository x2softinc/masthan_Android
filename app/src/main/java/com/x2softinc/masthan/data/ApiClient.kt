package com.x2softinc.masthan.data

import kotlinx.coroutines.CancellationException

/**
 * Authenticated GET with a single refresh-and-retry on 401. This is the request flow that
 * MasjidService, TimeDelayService, PrayerSettingsService and IqamaTimesService each
 * repeat in the Swift app, shared in one place.
 */
object ApiClient {
    /**
     * @param feature name shown in the "Unable to Load ..." error.
     * @param retry   action offered by the error's "Try Again" button.
     * @return the response body of a successful (2xx) response.
     */
    suspend fun authorizedGet(
        path: String,
        feature: String,
        timeoutMs: Int = 60_000,
        retry: (suspend () -> Unit)? = null,
    ): String {
        val auth = AuthService()

        val token = try {
            auth.getDeviceToken()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw UserFriendlyError.authentication(retry)
        }
        if (token.isEmpty()) throw UserFriendlyError.authentication()

        val url = getBaseUrl() + path
        fun headers(bearer: String) = mapOf("Authorization" to "Bearer $bearer", "Accept" to "application/json")

        try {
            val response = Http.request("GET", url, headers(token), timeoutMs = timeoutMs)

            val final = if (response.code == 401) {
                // Token was rejected. Refresh it and retry once.
                val newToken = auth.getDeviceToken()
                if (newToken.isEmpty()) throw UserFriendlyError.authentication()
                Http.request("GET", url, headers(newToken), timeoutMs = timeoutMs)
            } else {
                response
            }

            if (!final.isSuccess) {
                if (final.code >= 500) throw UserFriendlyError.serverError()
                throw UserFriendlyError.dataLoading(feature, retry)
            }
            return final.body
        } catch (e: CancellationException) {
            throw e
        } catch (e: UserFriendlyError) {
            throw e
        } catch (e: Exception) {
            throw UserFriendlyError.generic(e, retry)
        }
    }
}
