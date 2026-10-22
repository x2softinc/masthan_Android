package com.x2softinc.masthan.data

import com.x2softinc.masthan.AUTH_TOKEN
import com.x2softinc.masthan.CLIENT_ID
import com.x2softinc.masthan.REDIRECT_URI
import com.x2softinc.masthan.TOKEN_EXPIRES_AT
import org.json.JSONObject
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Base64

/** OAuth PKCE device-code authentication (port of Services/AuthService.swift). */
class AuthService {

    private val tokenExpirySkewMs = 30 * 1000.0
    private val defaultTokenLifetimeMs = 60 * 60 * 1000.0
    private val baseUrl = getBaseUrl()

    private val random = SecureRandom()

    private class PkcePair(val verifier: String, val challenge: String)

    private fun createPkcePair(): PkcePair {
        val bytes = ByteArray(32).also(random::nextBytes)
        val verifier = toBase64Url(bytes)
        val hash = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.UTF_8))
        return PkcePair(verifier, toBase64Url(hash))
    }

    private fun toBase64Url(data: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(data)

    // region Error helpers

    fun isUnauthorizedError(error: Throwable?): Boolean {
        if (error == null) return false
        if (error is HttpStatusException && error.code == 401) return true
        val message = (error.message ?: "").lowercase()
        return message.contains("unauthorized") || message.contains("401")
    }

    fun isConflictError(error: Throwable?): Boolean {
        if (error == null) return false
        if (error is HttpStatusException && error.code == 409) return true
        val message = (error.message ?: "").lowercase()
        return message.contains("conflict") || message.contains("409")
    }

    // endregion

    // region Token expiry

    fun isAuthTokenExpired(): Boolean {
        val token = Prefs.getString(AUTH_TOKEN)
        if (token.isEmpty()) return true

        val rawExpiry = Prefs.getString(TOKEN_EXPIRES_AT)
        if (rawExpiry.isEmpty()) return true

        val expiry = rawExpiry.toDoubleOrNull() ?: return true
        if (expiry <= 0) return true

        val nowMs = System.currentTimeMillis().toDouble()
        return nowMs >= expiry - tokenExpirySkewMs
    }

    private fun decodeJwtExpiry(token: String): Double? {
        val parts = token.split(".")
        if (parts.size <= 1) return null
        return runCatching {
            val payload = String(Base64.getUrlDecoder().decode(padBase64(parts[1])), Charsets.UTF_8)
            val exp = JSONObject(payload).optDouble("exp", Double.NaN)
            if (exp.isNaN() || exp <= 0) null else exp * 1000
        }.getOrNull()
    }

    private fun padBase64(value: String): String {
        val remainder = value.length % 4
        return if (remainder > 0) value + "=".repeat(4 - remainder) else value
    }

    private fun resolveTokenExpiry(token: String, response: JSONObject?): Double {
        val raw = response?.opt("expiresIn") ?: response?.opt("expires_in")
        val expiresIn = when (raw) {
            is Number -> raw.toDouble()
            is String -> raw.toDoubleOrNull()
            else -> null
        }
        if (expiresIn != null && expiresIn > 0) {
            return System.currentTimeMillis() + expiresIn * 1000
        }

        val rawExpiresAt = response?.optString("expiresAt")?.takeIf { it.isNotEmpty() }
            ?: response?.optString("expires_at")?.takeIf { it.isNotEmpty() }
        if (rawExpiresAt != null) {
            val instant = runCatching { OffsetDateTime.parse(rawExpiresAt).toInstant() }.getOrNull()
                ?: runCatching { Instant.parse(rawExpiresAt) }.getOrNull()
            if (instant != null) return instant.toEpochMilli().toDouble()
        }

        decodeJwtExpiry(token)?.let { return it }

        return System.currentTimeMillis() + defaultTokenLifetimeMs
    }

    // endregion

    /**
     * Runs the PKCE device-code flow and returns a fresh access token.
     * As on iOS, the cached token is intentionally not reused.
     */
    suspend fun getDeviceToken(): String {
        val deviceCode = getDeviceCode()
        val pkce = createPkcePair()
        val state = toBase64Url(ByteArray(16).also(random::nextBytes))

        val authorization = authorizePkceDeviceCode(
            mapOf(
                "clientId" to CLIENT_ID,
                "deviceCode" to deviceCode,
                "redirectUri" to REDIRECT_URI,
                "responseType" to "code",
                "state" to state,
                "scope" to "openid profile",
                "codeChallenge" to pkce.challenge,
                "codeChallengeMethod" to "S256",
            )
        )

        val code = authorization.optString("code").takeIf { it.isNotEmpty() }
            ?: throw AuthException("The authorization server did not return an authorization code.")

        val tokenResponse = oauthTokenAuthorizationCode(
            mapOf(
                "grant_type" to "authorization_code",
                "client_id" to "masjid-mobile-app",
                "code" to code,
                "redirect_uri" to REDIRECT_URI,
                "code_verifier" to pkce.verifier,
            )
        )

        val token = tokenResponse.optString("accessToken").takeIf { it.isNotEmpty() }
            ?: tokenResponse.optString("token").takeIf { it.isNotEmpty() }
            ?: ""
        if (token.isEmpty()) throw AuthException("The authorization server did not return an access token.")

        Prefs.putString(AUTH_TOKEN, token)
        Prefs.putString(TOKEN_EXPIRES_AT, resolveTokenExpiry(token, tokenResponse).toString())
        return token
    }

    private suspend fun authorizePkceDeviceCode(payload: Map<String, String>): JSONObject {
        val response = Http.request(
            method = "POST",
            url = baseUrl + MasjidProApiPaths.OAuth.AUTHORIZE_PKCE_DEVICE_CODE,
            headers = mapOf("Accept" to "application/json", "Content-Type" to "application/json"),
            body = JSONObject(payload).toString(),
        )
        if (!response.isSuccess) throw HttpStatusException(response.code, "Authorization request failed.")
        return runCatching { JSONObject(response.body) }.getOrNull()
            ?: throw AuthException("Invalid JSON response.")
    }

    private suspend fun oauthTokenAuthorizationCode(payload: Map<String, String>): JSONObject {
        val form = payload.entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }
        val response = Http.request(
            method = "POST",
            url = baseUrl + MasjidProApiPaths.OAuth.TOKEN,
            headers = mapOf("Accept" to "application/json", "Content-Type" to "application/x-www-form-urlencoded"),
            body = form,
        )
        if (!response.isSuccess) throw HttpStatusException(response.code, "Token request failed.")
        return runCatching { JSONObject(response.body) }.getOrNull()
            ?: throw AuthException("Invalid JSON response from token endpoint.")
    }
}

class AuthException(message: String) : Exception(message)
