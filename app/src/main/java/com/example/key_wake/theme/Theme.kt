package com.example.key_wake.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MidnightColorScheme = darkColorScheme(
    primary = MidnightElectricPurple,
    onPrimary = Color.White,
    secondary = MidnightNeonPink,
    onSecondary = Color.White,
    background = MidnightBlack,
    onBackground = MidnightWhite,
    surface = MidnightDarkSurface,
    onSurface = MidnightWhite,
    surfaceVariant = MidnightDarkSurface,
    onSurfaceVariant = MidnightGray
)

private val CyberpunkColorScheme = darkColorScheme(
    primary = CyberpunkCyan,
    onPrimary = CyberpunkBlack,
    secondary = CyberpunkPink,
    onSecondary = Color.White,
    background = CyberpunkBlack,
    onBackground = CyberpunkWhite,
    surface = CyberpunkSurface,
    onSurface = CyberpunkWhite,
    surfaceVariant = CyberpunkSurface,
    onSurfaceVariant = CyberpunkYellow
)

private val SunsetColorScheme = darkColorScheme(
    primary = SunsetOrange,
    onPrimary = Color.White,
    secondary = SunsetGold,
    onSecondary = SunsetPurple,
    background = SunsetPurple,
    onBackground = SunsetWhite,
    surface = SunsetSurface,
    onSurface = SunsetWhite,
    surfaceVariant = SunsetSurface,
    onSurfaceVariant = SunsetPink
)

private val MinimalLightColorScheme = lightColorScheme(
    primary = MinimalBlue,
    onPrimary = Color.White,
    secondary = MinimalGreen,
    onSecondary = Color.White,
    background = MinimalBg,
    onBackground = MinimalDarkText,
    surface = MinimalSurface,
    onSurface = MinimalDarkText,
    surfaceVariant = MinimalSurface,
    onSurfaceVariant = MinimalMuted
)

@Composable
fun KeyWakeTheme(
    themeName: String = "DEFAULT",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        "CYBERPUNK" -> CyberpunkColorScheme
        "SUNSET" -> SunsetColorScheme
        "LIGHT" -> MinimalLightColorScheme
        else -> MidnightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
