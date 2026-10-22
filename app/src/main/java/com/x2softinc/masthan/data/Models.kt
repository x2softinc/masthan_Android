package com.x2softinc.masthan.data

import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

// Ports of the Swift Codable structs in Masthan/Models. The Swift property names are the
// JSON keys, so they are kept as-is.

/** Daily calculated prayer times (port of AdhanTime.swift). Held in memory only. */
data class AdhanTime(
    val fajr: ZonedDateTime? = null,
    val imsak: ZonedDateTime? = null,
    val sunrise: ZonedDateTime? = null,
    val dhuhar: ZonedDateTime? = null,
    val asar: ZonedDateTime? = null,
    val asarHanafi: ZonedDateTime? = null,
    val maghrib: ZonedDateTime? = null,
    val sunset: ZonedDateTime? = null,
    val isha: ZonedDateTime? = null,
    val midnight: ZonedDateTime? = null,
    val zawal: ZonedDateTime? = null,
)

/** Adhan or iqama time per prayer (port of IqamaTime.swift). */
data class IqamaTime(
    val fajr: ZonedDateTime? = null,
    val dhuhar: ZonedDateTime? = null,
    val asar: ZonedDateTime? = null,
    val maghrib: ZonedDateTime? = null,
    val isha: ZonedDateTime? = null,
)

@Serializable
data class Announcement(
    val id: Int = 0,
    val masjidCode: String? = null,
    val fromDate: String? = null,
    val toDate: String? = null,
    val infoText: String? = null,
)

@Serializable
data class AnnualTime(
    val id: Int? = null,
    val recordType: String? = null,
    val masjidCode: String? = null,
    val month: Int? = null,
    val fromDay: Int? = null,
    val toDay: Int? = null,
    val fajrTime: String? = null,
    val dhuharTime: String? = null,
    val asrTime: String? = null,
    val maghribTime: String? = null,
    val ishaTime: String? = null,
)

@Serializable
data class FixedTime(
    val id: Int? = null,
    val masjidCode: String? = null,
    val fajr: String? = null,
    val dhuhar: String? = null,
    val asr: String? = null,
    val isha: String? = null,
)

@Serializable
data class MasjidInfo(
    val id: Int? = null,
    val masjidCode: String? = null,
    val masjidName: String? = null,
    val address: String? = null,
    val address1: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val email: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val phone: String? = null,
    val url: String? = null,
)

@Serializable
data class PrayerSetting(
    val id: Int? = null,
    val deviceCode: String? = null,
    val calcMethod: String = "MWL",
    val maintMethod: Int = 0,
    val jumma: String? = null,
    val jumma2: String? = null,
    val hijriAdjustment: Int = 0,
    val asrMethod: Int = 0,
    val maghribDelay: Int = 3,
    val roundMinutes: Int = 0,
)

@Serializable
data class TimeDelay(
    val id: Int? = null,
    val masjidCode: String? = null,
    val fajr: Int = 0,
    val dhuhar: Int = 0,
    val asr: Int = 0,
    val isha: Int = 0,
)

@Serializable
data class VersionInfo(
    val id: Int? = null,
    val appCode: String? = null,
    val description: String? = null,
    val version: String? = null,
    val forceVersion: Boolean? = null,
    val platform: String? = null,
    val status: String? = null,
    val owner: String? = null,
    val isActive: Boolean? = null,
    val baseURL: String? = null,
    val overrideURL: Boolean? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

// region Payload transfer models (Payloads.swift)

data class LoginPayload(val username: String, val password: String, val clientId: String)
data class ForgotPasswordPayload(val email: String)
data class ResetPasswordPayload(val email: String, val token: String, val newPassword: String)
data class RegisterDevicePayload(val userEmail: String, val deviceName: String)
data class AuthPayload(
    val clientId: String,
    val deviceCode: String,
    val redirectUri: String,
    val responseType: String,
    val state: String,
    val scope: String,
    val codeChallenge: String,
    val codeChallengeMethod: String,
)

// endregion
