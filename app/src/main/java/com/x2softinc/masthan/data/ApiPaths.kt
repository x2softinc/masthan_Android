package com.x2softinc.masthan.data

import java.net.URLEncoder

/** Port of MasjidProApiPaths (Models/ApiPaths.swift). */
object MasjidProApiPaths {
    /** Percent-encodes a path segment (spaces as %20, not '+'). */
    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    object Auth {
        const val LOGIN = "/api/auth/login"
        const val ME = "/api/auth/me"
        const val FORGOT_PASSWORD = "/api/auth/forgot-password"
        const val RESET_PASSWORD = "/api/auth/reset-password"
    }

    object OAuth {
        const val AUTHORIZE_PKCE = "/oauth/authorize-pkce"
        const val AUTHORIZE_PKCE_DEVICE_CODE = "/oauth/authorize-pkce-deviceCode"
        const val TOKEN = "/oauth/token"
        const val CLIENTS = "/oauth/clients"
    }

    object Device {
        const val REGISTER = "/api/device/register"
        const val ALL = "/api/device/all"

        fun byUser(userEmail: String) = "/api/device/by-user/${encode(userEmail)}"
        fun byDeviceCode(deviceCode: String) = "/api/device/${encode(deviceCode)}"
        fun validate(deviceCode: String) = "/api/device/${encode(deviceCode)}/validate"
        fun deactivate(deviceCode: String) = "/api/device/${encode(deviceCode)}/deactivate"
    }

    object Resources {
        fun list(resource: String) = "/api/$resource"
        fun byDeviceCode(resource: String, deviceCode: Any) = "/api/$resource/masjid/${encode(deviceCode.toString())}"
        fun byId(resource: String, id: Any) = "/api/$resource/${encode(id.toString())}"
    }
}
