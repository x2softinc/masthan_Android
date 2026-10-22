package com.x2softinc.masthan

import android.content.Context

/**
 * Centralized app metadata constants (port of AppMetadata.swift).
 * Version info comes from the package manager instead of the iOS Info.plist.
 */
object AppMetadata {
    // region App identity

    fun appName(context: Context): String =
        context.applicationInfo.loadLabel(context.packageManager).toString()
            .ifBlank { "Masthan Gani Masjid" }

    fun appVersion(context: Context): String = packageInfo(context)?.versionName ?: "1.0"

    @Suppress("DEPRECATION")
    fun buildNumber(context: Context): String {
        val info = packageInfo(context) ?: return "1"
        val code = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        return code.toString()
    }

    fun versionWithBuild(context: Context): String = "${appVersion(context)} / ${buildNumber(context)}"

    private fun packageInfo(context: Context) = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }.getOrNull()

    // endregion

    // region Legal documents

    const val TERMS_OF_USE_TITLE = "Terms of Use (EULA)"
    const val PRIVACY_POLICY_TITLE = "Privacy Policy"
    const val PRIVACY_POLICY_URL = "https://x2softinc.com/privacy-policy-masthan-gani-masjid/"

    // endregion
}
