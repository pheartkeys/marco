package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BrightExplorerColorScheme = lightColorScheme(
    primary = MarcoCoral,
    onPrimary = Color.White,
    primaryContainer = MarcoCoralPastel,
    onPrimaryContainer = MarcoCoralDark,
    secondary = VoyagerSky,
    onSecondary = Color.White,
    secondaryContainer = VoyagerSkyPastel,
    onSecondaryContainer = VoyagerSkyDark,
    tertiary = GoldenSun,
    onTertiary = Color.White,
    tertiaryContainer = GoldenSunPastel,
    onTertiaryContainer = GoldenSunDark,
    background = LightCanvas,
    onBackground = TextPrimary,
    surface = LightSurface,
    onSurface = TextPrimary,
    surfaceVariant = LightCardElevated,
    onSurfaceVariant = TextSecondary,
    outline = LightBorder,
    outlineVariant = LightBorderSubtle,
    error = StatusCrimson,
    onError = Color.White,
    errorContainer = StatusCrimsonMuted,
    onErrorContainer = StatusCrimson
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = BrightExplorerColorScheme,
        typography = Typography,
        content = content
    )
}
