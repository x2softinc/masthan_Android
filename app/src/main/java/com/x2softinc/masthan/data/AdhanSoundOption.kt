package com.x2softinc.masthan.data

import android.content.Context
import android.net.Uri

object AdhanSoundPreference {
    const val STORAGE_KEY = "adhanSound"
}

/**
 * Adhan sounds bundled with the app (port of Models/AdhanSoundOption.swift).
 * [rawValue] is the persisted identifier shared with iOS; the audio lives in res/raw
 * under the lower-cased rawValue.
 */
enum class AdhanSoundOption(val rawValue: String, val displayName: String) {
    SYSTEM_DEFAULT("Default", "Default"),
    ADHAN1("Adhan1", "Adhan 1"),
    ADHAN2("Adhan2", "Adhan 2"),
    ADHAN3("Adhan3", "Adhan 3"),
    ADHAN4("Adhan4", "Adhan 4"),
    AHMAD_AL_NAFEES("Ahmad_Al_Nafees", "Ahmad Al Nafees"),
    AL_AQSA("AlAqsa", "Al Aqsa"),
    AL_HARAM("Al_Haram", "Al Haram"),
    HAFIZ_MUSTAFA_OOZCAN("Hafiz_Mustafa_Oozcan", "Hafiz Mustafa Oozcan"),
    KARL_JENKINS("Karl_Jenkins", "Karl Jenkins"),
    MADHINA("Madhina", "Madinah"),
    MAKKAH("Makkah", "Makkah"),
    MISHARY_RASHID_ALAFASY("Mishary_Rashid_Alafasy", "Mishary Rashid Alafasy"),
    MISHARY_RASHID_ALAFASY2("Mishary_Rashid_Alafasy2", "Mishary Rashid Alafasy 2"),
    MISHARY_RASHID_ALAFASY3("Mishary_Rashid_Alafasy3", "Mishary Rashid Alafasy 3"),
    QARI_ABDUL_KAREEM("Qari_Abdul_Kareem", "Qari Abdul Kareem"),
    SALAH_MANSOOR_AZ_ZAHRANI("Salah_Mansoor_AzZahrani", "Salah Mansoor Az Zahrani"),
    SHEIKH_JAMAC_HAREED("Sheikh_Jamac_Hareed", "Sheikh Jamac Hareed");

    /** The audio resource name without extension, or null for the system default sound. */
    val resourceName: String? get() = if (this == SYSTEM_DEFAULT) null else rawValue.lowercase()

    /** `android.resource://` URI for notification channels, or null for the default sound. */
    fun soundUri(context: Context): Uri? = resourceName?.let { name ->
        val id = context.resources.getIdentifier(name, "raw", context.packageName)
        if (id == 0) null else Uri.parse("android.resource://${context.packageName}/$id")
    }

    companion object {
        fun fromRawValue(raw: String?): AdhanSoundOption? = entries.firstOrNull { it.rawValue == raw }
    }
}
