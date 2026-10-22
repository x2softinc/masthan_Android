package com.x2softinc.masthan.ui

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.x2softinc.masthan.data.Prefs

/** Compose-observable preference, the counterpart of SwiftUI's `@AppStorage`. */
class PrefState<T>(private val state: MutableState<T>, private val write: (T) -> Unit) {
    var value: T
        get() = state.value
        set(newValue) {
            state.value = newValue
            write(newValue)
        }

    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T = value
    operator fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, newValue: T) {
        value = newValue
    }
}

@Composable
private fun <T> rememberPref(key: String, read: () -> T, write: (T) -> Unit): PrefState<T> {
    val state = remember(key) { mutableStateOf(read()) }
    DisposableEffect(key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
            if (changed == key) state.value = read()
        }
        Prefs.shared.registerOnSharedPreferenceChangeListener(listener)
        onDispose { Prefs.shared.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return remember(key) { PrefState(state, write) }
}

@Composable
fun rememberStringPref(key: String, default: String = ""): PrefState<String> =
    rememberPref(key, { Prefs.getString(key, default) }, { Prefs.putString(key, it) })

@Composable
fun rememberBooleanPref(key: String, default: Boolean = false): PrefState<Boolean> =
    rememberPref(key, { Prefs.getBoolean(key, default) }, { Prefs.putBoolean(key, it) })

@Composable
fun rememberIntPref(key: String, default: Int = 0): PrefState<Int> =
    rememberPref(key, { Prefs.getInt(key, default) }, { Prefs.putInt(key, it) })

@Composable
fun rememberDoublePref(key: String, default: Double = 0.0): PrefState<Double> =
    rememberPref(key, { Prefs.getDouble(key, default) }, { Prefs.putDouble(key, it) })
