package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =================================================================
// MARCO BRIGHT, FUN & LIGHT TRAVEL DESIGN SYSTEM
// A luminous, cheerful, and fresh travel companion palette
// with soft pastel tints and vibrant adventure accents.
// =================================================================

// Luminous Light Canvas & Surfaces
val LightCanvas = Color(0xFFF8FAFC)             // Fresh luminous off-white base canvas
val LightSurface = Color(0xFFFFFFFF)            // Pure crisp white container
val LightCard = Color(0xFFFFFFFF)               // Clean white card surface
val LightCardElevated = Color(0xFFF1F5F9)       // Elevated modal & input capsule surface
val LightBorder = Color(0xFFE2E8F0)             // Soft structural hairline border
val LightBorderSubtle = Color(0xFFF1F5F9)       // Faint divider

// Adventure Accent Primitives
val MarcoCoral = Color(0xFFFF6B4A)              // Vibrant warm sunset coral
val MarcoCoralLight = Color(0xFFFF8A65)         // Joyful coral highlight
val MarcoCoralDark = Color(0xFFE54B27)          // Rich sunset terra
val MarcoCoralMuted = Color(0x26FF6B4A)         // 15% coral tint
val MarcoCoralPastel = Color(0xFFFFF1EE)        // Soft pastel coral container

val VoyagerSky = Color(0xFF0284C7)              // Fresh azure sky & ocean
val VoyagerSkyLight = Color(0xFF38BDF8)         // Clear tropical sky highlight
val VoyagerSkyDark = Color(0xFF0369A1)          // Deep maritime azure
val VoyagerSkyMuted = Color(0x260284C7)         // 15% sky tint
val VoyagerSkyPastel = Color(0xFFE0F2FE)        // Soft pastel sky container

val GoldenSun = Color(0xFFF59E0B)               // Golden sunlight & discovery
val GoldenSunLight = Color(0xFFFBBF24)          // Bright sunshine sparkle
val GoldenSunDark = Color(0xFFD97706)           // Warm amber
val GoldenSunMuted = Color(0x26F59E0B)          // 15% amber tint
val GoldenSunPastel = Color(0xFFFEF3C7)         // Soft pastel gold container

val PalmEmerald = Color(0xFF10B981)             // Lush tropical palms & confirmed status
val PalmEmeraldLight = Color(0xFF34D399)        // Bright mint leaf
val PalmEmeraldDark = Color(0xFF059669)         // Deep emerald jungle
val PalmEmeraldMuted = Color(0x2610B981)        // 15% emerald tint
val PalmEmeraldPastel = Color(0xFFD1FAE5)       // Soft pastel mint container

val BerryOrchid = Color(0xFF8B5CF6)             // Cultural festivals & wanderlust
val BerryOrchidLight = Color(0xFFA78BFA)        // Bright orchid glow
val BerryOrchidMuted = Color(0x268B5CF6)
val BerryOrchidPastel = Color(0xFFEDE9FE)       // Soft pastel orchid container

val LagoonTeal = Color(0xFF0D9488)              // Tropical coral reef & crystal lagoon
val LagoonTealLight = Color(0xFF14B8A6)         // Turquoise sea spray
val LagoonTealMuted = Color(0x260D9488)
val LagoonTealPastel = Color(0xFFCCFBF1)        // Soft pastel teal container

// Functional Tokens
val ChampagneGold = GoldenSun
val ChampagneGoldLight = GoldenSunLight
val ChampagneGoldDark = GoldenSunDark
val ChampagneGoldMuted = GoldenSunPastel

val StatusEmerald = PalmEmerald
val StatusEmeraldMuted = PalmEmeraldPastel
val StatusCrimson = Color(0xFFEF4444)           // Emergency status
val StatusCrimsonMuted = Color(0xFFFEE2E2)      // Soft crimson pastel
val StatusAzure = VoyagerSky
val StatusAzureMuted = VoyagerSkyPastel

// High-Readability Light Typography Tones
val TextPrimary = Color(0xFF0F172A)             // Deep slate navy (super crisp on light)
val TextSecondary = Color(0xFF475569)           // Medium slate (readable body & descriptions)
val TextMuted = Color(0xFF94A3B8)               // Soft metadata slate

// =================================================================
// HARMONIZED TOKEN MAPPINGS (ALL MAPPED TO CLEAN LIGHT THEME)
// =================================================================

val LuxuryDarkBase = LightCanvas
val LuxurySurface = LightSurface
val LuxuryCard = LightCard
val LuxuryCardElevated = LightCardElevated
val LuxuryBorder = LightBorder
val LuxuryBorderSubtle = LightBorderSubtle

val CartographyDarkBase = LightCanvas
val CartographySurface = LightSurface
val CartographyCard = LightCard
val CartographyCardElevated = LightCardElevated
val ContourBorder = LightBorder
val ContourBorderSubtle = LightBorderSubtle

val ParchmentVellum = LightCard
val ParchmentSand = LightCardElevated
val ParchmentMuted = LightSurface
val ParchmentDark = LightCanvas

val NavigationalGold = GoldenSun
val VenetianGold = GoldenSun
val VenetianGoldLight = GoldenSunLight
val VenetianGoldDeep = GoldenSunDark
val AntiqueBrass = GoldenSun
val GoldenSparkle = GoldenSunLight

val MaritimeBlue = VoyagerSky
val MediterraneanAzure = VoyagerSky
val WaypointCyan = VoyagerSky
val SilkRoadTeal = LagoonTeal
val SilkRoadJade = PalmEmerald
val CelestialLapis = VoyagerSky

val WaxSealCrimson = StatusCrimson
val TerracottaStamp = MarcoCoral
val TerracottaMap = MarcoCoralLight
val WayfinderEmerald = PalmEmerald
val CompassLilac = BerryOrchid

val TextAtlasPrimary = TextPrimary
val TextAtlasSecondary = TextSecondary
val TextAtlasSubtle = TextMuted

// Theme Compatibility Aliases
val Navy900 = LightCanvas
val Navy800 = LightSurface
val Navy700 = LightCard
val OceanBlue = VoyagerSky
val SkyBlueLight = VoyagerSky
val TealAccent = LagoonTeal
val EmeraldGreen = PalmEmerald
val AmberGold = GoldenSun
val SunsetCoral = MarcoCoral
val PurpleAccent = BerryOrchid

val DarkSurface = LightCanvas
val DarkSurfaceElevated = LightSurface
val DarkSurfaceCard = LightCard
val DarkBorder = LightBorder

val LightBackground = LightCanvas
val LightSurfaceCard = LightCard

val TextPrimaryDark = TextPrimary
val TextSecondaryDark = TextSecondary
val TextPrimaryLight = TextPrimary
val TextSecondaryLight = TextSecondary

val ElegantDarkBackground = LightCanvas
val ElegantDarkSurface = LightSurface
val ElegantDarkSurfaceVariant = LightCard
val ElegantDarkBorder = LightBorder
val ElegantDarkOutlineVariant = LightBorderSubtle

val ElegantLilacPrimary = MarcoCoral
val ElegantOnPrimary = Color.White
val ElegantPrimaryContainer = MarcoCoralPastel
val ElegantOnPrimaryContainer = TextPrimary

val ElegantSecondary = VoyagerSky
val ElegantOnSecondary = Color.White
val ElegantSecondaryContainer = VoyagerSkyPastel
val ElegantOnSecondaryContainer = TextPrimary

val ElegantTextPrimary = TextPrimary
val ElegantTextSecondary = TextSecondary
val ElegantTextSubtle = TextMuted

val ElegantError = StatusCrimson
val ElegantOnError = Color.White
val ElegantErrorContainer = StatusCrimsonMuted
val ElegantOnErrorContainer = StatusCrimson

val ElegantSuccess = PalmEmerald
val ElegantAmber = GoldenSun
val ElegantTeal = LagoonTeal
val ExplorerParchmentCard = LightCard
