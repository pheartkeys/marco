package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PapercraftCartographyColorScheme = darkColorScheme(
    primary = NavigationalGold,
    onPrimary = CartographyDarkBase,
    primaryContainer = CartographyCardElevated,
    onPrimaryContainer = TextAtlasPrimary,
    secondary = MaritimeBlue,
    onSecondary = Color.White,
    secondaryContainer = CartographyCard,
    onSecondaryContainer = TextAtlasPrimary,
    tertiary = SilkRoadTeal,
    onTertiary = Color.White,
    background = CartographyDarkBase,
    onBackground = TextAtlasPrimary,
    surface = CartographySurface,
    onSurface = TextAtlasPrimary,
    surfaceVariant = CartographyCard,
    onSurfaceVariant = TextAtlasSecondary,
    outline = ContourBorder,
    outlineVariant = ContourBorderSubtle,
    error = WaxSealCrimson,
    onError = Color.White,
    errorContainer = Color(0xFF4C0519),
    onErrorContainer = Color(0xFFFFE4E6)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = PapercraftCartographyColorScheme,
        typography = Typography,
        content = content
    )
}
