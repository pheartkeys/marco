package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =================================================================
// MARCO PAPERCRAFT-FLAT-CARTOGRAPHY DESIGN SYSTEM
// =================================================================

// Deep Atlas Canvas (Midnight Ink & Contour Slate)
val CartographyDarkBase = Color(0xFF11141D)        // Deepest atlas background
val CartographySurface = Color(0xFF181E2B)         // Main component surface
val CartographyCard = Color(0xFF22293A)            // Flat papercraft card
val CartographyCardElevated = Color(0xFF2B3448)    // Floating modal & topbar layer
val ContourBorder = Color(0xFF38445E)              // Crisp 1dp cartographic contour border
val ContourBorderSubtle = Color(0xFF263044)        // Subtle secondary border

// Tactile Paper & Vellum Tones
val ParchmentVellum = Color(0xFFFBF8F2)            // Warm ivory parchment
val ParchmentSand = Color(0xFFF4EDE2)              // Tactile papercraft fill
val ParchmentMuted = Color(0xFFE8E0D2)             // Muted vellum tone
val ParchmentDark = Color(0xFF24201E)              // Weathered antique paper

// Navigational Brass & Compass Gold
val NavigationalGold = Color(0xFFF59E0B)           // Primary compass gold accent
val VenetianGold = Color(0xFFF59E0B)               // Alias for historical explorer
val VenetianGoldLight = Color(0xFFFDE68A)          // Gilded glow
val VenetianGoldDeep = Color(0xFFB45309)           // Deep burnished gold
val AntiqueBrass = Color(0xFFD97706)               // Warm navigational brass
val GoldenSparkle = Color(0xFFFFE082)              // AI sparkle gold

// Maritime Wayfinder Blues & Route Teals
val MaritimeBlue = Color(0xFF0284C7)               // Primary route indigo / blue
val MediterraneanAzure = Color(0xFF0284C7)         // Azure sea
val WaypointCyan = Color(0xFF38BDF8)               // Active waypoint indicator
val SilkRoadTeal = Color(0xFF0D9488)               // Silk road jade & water
val SilkRoadJade = SilkRoadTeal
val CelestialLapis = Color(0xFF1E293B)             // Night sky lapis

// Wax Seals & Status Stamps
val WaxSealCrimson = Color(0xFFE11D48)             // Emergency SOS & critical seal
val TerracottaStamp = Color(0xFFEA580C)            // Expedition coral / transit stamp
val TerracottaMap = Color(0xFFEA580C)              // Map terrain highlight
val WayfinderEmerald = Color(0xFF10B981)           // Safe route & sync green
val CompassLilac = Color(0xFF8B5CF6)               // AI Traveler DNA synthesis

// High-Contrast Typography Tones
val TextAtlasPrimary = Color(0xFFF8FAFC)           // Crisp ivory-white header text
val TextAtlasSecondary = Color(0xFF94A3B8)         // Slate subtitle & metadata text
val TextAtlasSubtle = Color(0xFF64748B)            // Muted coordinate/timestamp text

// =================================================================
// COMPATIBILITY ALIASES (Harmonized with Cartography Palette)
// =================================================================
val Navy900 = CartographyDarkBase
val Navy800 = CartographySurface
val Navy700 = CartographyCard
val OceanBlue = MaritimeBlue                       // Action blue
val SkyBlueLight = WaypointCyan                    // Highlight cyan
val TealAccent = SilkRoadTeal                      // Accessibility & transit
val EmeraldGreen = WayfinderEmerald                // Live sync & budget savings
val AmberGold = NavigationalGold                   // Voice calls & rewards sweet spots
val SunsetCoral = TerracottaStamp                  // Alerts & highlights
val PurpleAccent = CompassLilac                    // AI learning & group reels

val DarkSurface = CartographyDarkBase
val DarkSurfaceElevated = CartographySurface
val DarkSurfaceCard = CartographyCard
val DarkBorder = ContourBorder

val LightBackground = CartographyDarkBase
val LightSurface = CartographySurface
val LightSurfaceElevated = CartographyCardElevated
val LightSurfaceCard = CartographyCard
val LightBorder = ContourBorder

val TextPrimaryDark = TextAtlasPrimary
val TextSecondaryDark = TextAtlasSecondary
val TextPrimaryLight = TextAtlasPrimary
val TextSecondaryLight = TextAtlasSecondary

// Elegant Dark Palette mappings
val ElegantDarkBackground = CartographyDarkBase
val ElegantDarkSurface = CartographySurface
val ElegantDarkSurfaceVariant = CartographyCard
val ElegantDarkBorder = ContourBorder
val ElegantDarkOutlineVariant = ContourBorderSubtle

val ElegantLilacPrimary = MaritimeBlue
val ElegantOnPrimary = Color.White
val ElegantPrimaryContainer = CartographyCardElevated
val ElegantOnPrimaryContainer = TextAtlasPrimary

val ElegantSecondary = NavigationalGold
val ElegantOnSecondary = CartographyDarkBase
val ElegantSecondaryContainer = CartographyCard
val ElegantOnSecondaryContainer = TextAtlasPrimary

val ElegantTextPrimary = TextAtlasPrimary
val ElegantTextSecondary = TextAtlasSecondary
val ElegantTextSubtle = TextAtlasSubtle

val ElegantError = WaxSealCrimson
val ElegantOnError = Color.White
val ElegantErrorContainer = Color(0xFF4C0519)
val ElegantOnErrorContainer = Color(0xFFFFE4E6)

val ElegantSuccess = WayfinderEmerald
val ElegantAmber = NavigationalGold
val ElegantTeal = SilkRoadTeal
val ExplorerParchmentCard = CartographyCard
