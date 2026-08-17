package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LuxuryEditorialColorScheme = darkColorScheme(
    primary = TextPrimary,
    onPrimary = LuxuryDarkBase,
    primaryContainer = LuxuryCardElevated,
    onPrimaryContainer = TextPrimary,
    secondary = TextSecondary,
    onSecondary = TextPrimary,
    secondaryContainer = LuxuryCard,
    onSecondaryContainer = TextPrimary,
    tertiary = ChampagneGold,
    onTertiary = LuxuryDarkBase,
    background = LuxuryDarkBase,
    onBackground = TextPrimary,
    surface = LuxurySurface,
    onSurface = TextPrimary,
    surfaceVariant = LuxuryCard,
    onSurfaceVariant = TextSecondary,
    outline = LuxuryBorder,
    outlineVariant = LuxuryBorderSubtle,
    error = StatusCrimson,
    onError = TextPrimary,
    errorContainer = Color(0xFF2B0A0A),
    onErrorContainer = Color(0xFFFCA5A5)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LuxuryEditorialColorScheme,
        typography = Typography,
        content = content
    )
}
