package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =================================================================
// MARCO BLACK-ON-BLACK MINIMALIST DESIGN SYSTEM
// Pure deep black architecture with a single subtle warm gold accent.
// =================================================================

// Pure Pitch Black & Deep Graphite Surfaces
val LuxuryDarkBase = Color(0xFF000000)          // Pure pitch black canvas
val LuxurySurface = Color(0xFF0A0A0A)           // Subtle structural black
val LuxuryCard = Color(0xFF121212)              // Dark graphite card surface
val LuxuryCardElevated = Color(0xFF1A1A1A)      // Floating sheets & modals
val LuxuryBorder = Color(0xFF242424)            // Hairline 1dp structural border
val LuxuryBorderSubtle = Color(0xFF181818)      // Faint hairline divider

// The Single Subtle Accent (Muted Warm Gold)
val ChampagneGold = Color(0xFFD4AF37)           // Subtle warm champagne gold
val ChampagneGoldLight = Color(0xFFE8D390)      // Subtle gold highlight
val ChampagneGoldDark = Color(0xFFA68525)       // Deep gold
val ChampagneGoldMuted = Color(0x1AD4AF37)      // 10% faint gold tint

// Functional Status Tokens (Subtle & Restrained)
val StatusEmerald = Color(0xFF10B981)           // Subtle green (active status)
val StatusEmeraldMuted = Color(0x1A10B981)
val StatusCrimson = Color(0xFFDC2626)           // Muted red (emergency only)
val StatusCrimsonMuted = Color(0x1ADC2626)
val StatusAzure = ChampagneGold                 // Unified with main accent
val StatusAzureMuted = ChampagneGoldMuted

// Monochrome Typography Tones
val TextPrimary = Color(0xFFF2F2F2)             // Crisp clean off-white
val TextSecondary = Color(0xFF8E8E93)           // Neutral muted slate-gray
val TextMuted = Color(0xFF555555)               // Subtle dark gray metadata

// =================================================================
// UNIFIED TOKEN MAPPINGS (ALL RAINBOW COLORS REMOVED)
// =================================================================

// Cartography Design Tokens (Strict Monochrome Black Architecture)
val CartographyDarkBase = LuxuryDarkBase
val CartographySurface = LuxurySurface
val CartographyCard = LuxuryCard
val CartographyCardElevated = LuxuryCardElevated
val ContourBorder = LuxuryBorder
val ContourBorderSubtle = LuxuryBorderSubtle

// Legacy Palette Aliases Harmonized to Strict Monochrome + Single Accent
val ParchmentVellum = LuxuryCard
val ParchmentSand = LuxuryCard
val ParchmentMuted = LuxurySurface
val ParchmentDark = LuxuryDarkBase

val NavigationalGold = ChampagneGold
val VenetianGold = ChampagneGold
val VenetianGoldLight = ChampagneGoldLight
val VenetianGoldDeep = ChampagneGoldDark
val AntiqueBrass = ChampagneGold
val GoldenSparkle = ChampagneGoldLight

// All legacy theme tokens mapped to monochrome black/graphite structure
val MaritimeBlue = LuxuryCardElevated
val MediterraneanAzure = LuxuryCardElevated
val WaypointCyan = TextSecondary
val SilkRoadTeal = TextSecondary
val SilkRoadJade = TextSecondary
val CelestialLapis = LuxuryCardElevated

val WaxSealCrimson = StatusCrimson
val TerracottaStamp = TextSecondary
val TerracottaMap = TextSecondary
val WayfinderEmerald = TextSecondary
val CompassLilac = TextSecondary

val TextAtlasPrimary = TextPrimary
val TextAtlasSecondary = TextSecondary
val TextAtlasSubtle = TextMuted

// Theme Compatibility Aliases (Strict Black & Monochrome)
val Navy900 = LuxuryDarkBase
val Navy800 = LuxurySurface
val Navy700 = LuxuryCard
val OceanBlue = LuxuryCardElevated
val SkyBlueLight = TextSecondary
val TealAccent = TextSecondary
val EmeraldGreen = TextSecondary
val AmberGold = LuxuryCardElevated
val SunsetCoral = TextSecondary
val PurpleAccent = TextSecondary

val DarkSurface = LuxuryDarkBase
val DarkSurfaceElevated = LuxurySurface
val DarkSurfaceCard = LuxuryCard
val DarkBorder = LuxuryBorder

val LightBackground = LuxuryDarkBase
val LightSurface = LuxurySurface
val LightSurfaceElevated = LuxuryCardElevated
val LightSurfaceCard = LuxuryCard
val LightBorder = LuxuryBorder

val TextPrimaryDark = TextPrimary
val TextSecondaryDark = TextSecondary
val TextPrimaryLight = TextPrimary
val TextSecondaryLight = TextSecondary

val ElegantDarkBackground = LuxuryDarkBase
val ElegantDarkSurface = LuxurySurface
val ElegantDarkSurfaceVariant = LuxuryCard
val ElegantDarkBorder = LuxuryBorder
val ElegantDarkOutlineVariant = LuxuryBorderSubtle

val ElegantLilacPrimary = ChampagneGold
val ElegantOnPrimary = LuxuryDarkBase
val ElegantPrimaryContainer = LuxuryCardElevated
val ElegantOnPrimaryContainer = TextPrimary

val ElegantSecondary = TextSecondary
val ElegantOnSecondary = LuxuryDarkBase
val ElegantSecondaryContainer = LuxuryCard
val ElegantOnSecondaryContainer = TextPrimary

val ElegantTextPrimary = TextPrimary
val ElegantTextSecondary = TextSecondary
val ElegantTextSubtle = TextMuted

val ElegantError = StatusCrimson
val ElegantOnError = Color.White
val ElegantErrorContainer = Color(0xFF2B0A0A)
val ElegantOnErrorContainer = Color(0xFFFCA5A5)

val ElegantSuccess = StatusEmerald
val ElegantAmber = ChampagneGold
val ElegantTeal = TextSecondary
val ExplorerParchmentCard = LuxuryCard
