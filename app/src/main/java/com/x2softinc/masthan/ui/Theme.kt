package com.x2softinc.masthan.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colors from Assets.xcassets (themeColor, barColor, offWhite) and ColorExtension.swift.

/** Navigation / toolbar background. */
val BarColor = Color(0xFF25614D)

/** Light "themeColor" (dark mode uses the label color, i.e. white). */
private val ThemeGreen = Color(0xFF134E40)

/** Accent used by the Masjid and Prayer Settings cards. */
val AccentColor = Color(red = 0f, green = 0.6f, blue = 0.8f)

/** Soft gold used for Hijri accents. */
val HijriGold = Color(0xFFEAD0A2)

/** The iOS `Color("themeColor")` equivalent: dark green in light mode, white in dark mode. */
val androidx.compose.material3.ColorScheme.themeColor: Color get() = primary

private val LightColors = lightColorScheme(
    primary = ThemeGreen,
    onPrimary = Color.White,
    secondary = Color(0xFF25614D),
    background = Color(0xFFF7F7F5),
    surface = Color.White,
    surfaceVariant = Color(0xFFEFF1EF),
)

private val DarkColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color(0xFF134E40),
    secondary = Color(0xFF7FC8AF),
    background = Color(0xFF101312),
    surface = Color(0xFF1A1E1C),
    surfaceVariant = Color(0xFF262B29),
)

@Composable
fun MasthanTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
