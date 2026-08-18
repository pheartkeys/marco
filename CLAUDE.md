# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Marco — an Android AI travel concierge app (Kotlin + Jetpack Compose, single `:app` module). Per `metadata.json`: multi-modal itinerary planning, 24/7 AI concierge chat, simulated vendor voice calling, timeshare/loyalty optimizer, family & accessibility logistics, multi-currency budget wallet, offline safety, and group travel memories.

Namespace is `com.example` (all packages), but `applicationId` is `com.go.marco`. Firebase project is `go-marco`.

## Commands

```bash
./gradlew assembleDebug                 # build debug APK
./gradlew installDebug                  # build + install on connected device/emulator
./gradlew testDebugUnitTest             # all JVM/Robolectric unit tests
./gradlew lintDebug                     # Android Lint (lintFix applies safe fixes)
./gradlew connectedDebugAndroidTest     # instrumented tests (needs device)
```

Run a single test class or method:

```bash
./gradlew testDebugUnitTest --tests "com.example.ExampleRobolectricTest"
./gradlew testDebugUnitTest --tests "com.example.ExampleRobolectricTest.read string from context"
```

Roborazzi screenshot tests (Robolectric-based, no device needed; goldens live in `app/src/test/screenshots/`):

```bash
./gradlew recordRoborazziDebug           # regenerate goldens
./gradlew verifyRoborazziDebug           # fail on visual diff
./gradlew compareRoborazziDebug          # write diff images
```

Toolchain: JDK 21 installed, Gradle 9.7, AGP 9.1.1, Kotlin 2.0.21, compileSdk/targetSdk 36, minSdk 24. Configuration cache and build cache are on, so avoid adding non-cacheable build logic.

## Secrets and Firebase

- The **Secrets Gradle Plugin** is configured with `propertiesFileName = ".env"` and defaults from `.env.example` — non-standard for Android, so put keys in `.env` (gitignored), not `local.properties`. `GEMINI_API_KEY` becomes `BuildConfig.GEMINI_API_KEY`.
- `google-services.json` is committed; the plugin uses `MissingGoogleServicesStrategy.WARN` plus `googleServices.missing.passthrough=true`, so the build still succeeds without it.
- Firestore rules (`firestore.rules`) implement a **hybrid model**: `/users/{uid}/**` stays private to the matching authed user (Plaid data, balances, monetary valuations, wallet, Pow Wow transcripts), while `/trips/{tripId}` is shared with the members listed in the trip document's `members` map (party structure, brief, itinerary, contributions, ledger). **The privacy line is enforced in the rules, not in app code**: a loyalty/timeshare program's title and type may be published to a trip; its balance, valuation, tier, and account number may not. Money-bearing shared collections use strict `keys().hasOnly([...])` allowlists plus scalar type checks, so a client that copies a whole account object into a shared doc is rejected rather than sanitised. Use `ConnectedAccountEntity.toShareableProgramRef()` when crossing that boundary in app code. Details in `ARCHITECTURE.md`.

## Architecture

Single-Activity Compose app. `MainActivity` → `MyApplicationTheme` → `MainAppScreen`.

**One ViewModel for the existing app, and it is now frozen.** `TravelViewModel` (~2k lines, `AndroidViewModel`) is created once in `MainAppScreen` and passed to every screen. It owns the repository, the Gemini service, settings, cloud sync, and `TextToSpeech`. There is still no DI framework, but dependencies now come from `data/di/MarcoRepositories` rather than being constructed inline. **Do not add new feature state here** — it absorbed every past change, which is why it is 2k lines. New features get a feature-scoped ViewModel that pulls its own repository interface from `MarcoRepositories`. Existing state stays as `MutableStateFlow` + exposed `StateFlow`; screens are stateless consumers.

**Navigation.** `MainAppScreen` holds the single `NavHost`; routes are string constants in `AppRoutes`. Start destination is `CHAT`. Navigation args are avoided — cross-screen values (e.g. `vendorCallTarget`) are hoisted into `remember` state in `MainAppScreen` and passed as lambdas. Shared slide/fade transitions are defined once on the `NavHost`.

**Data layer** (`data/`) — see **`ARCHITECTURE.md`** for the full Phase 0 contract (party model, Pow Wow, contributions, ledger, pricing, Firestore shapes). Summary:
- `model/` — Room entities split by domain: `TripModels`, `ChatModels`, `UserPreferenceModels`, `MoneyModels` (private financial entities), `TripSupportModels`, plus the v10 additions `PartyModels`, `IdeaModels`, `PowWowModels`, `TripBriefModels`, `ContributionModels`, `LedgerModels`, `PricingModels`. All in package `com.example.data.model`. (Before v10 these were one file, `TravelModels.kt`.)
- `local/AppDatabase.kt` — singleton Room DB `marco_travel.db`, **version 10 with `fallbackToDestructiveMigration()`**. Any entity change wipes local data; bump `version` when you touch schema. v8 added onboarding fields (displayName, homeAirport, travelMotivation, signatureAspiration, accessibilityVerificationOptIn) to UserPreferenceEntity; v9 added the weighted-preference JSON fields (motivationWeightsJson, travelStyleWeightsJson, pacingWeightsJson, comfortWeightsJson, loyaltyRankJson — see `data/model/PreferenceWeights.kt`) that back the onboarding wizard's 1-7 intensity scales and loyalty ranking, while the pre-existing flat fields stay populated with each group's highest-weighted value; **v10** added the multi-user foundation — travelers, party units, trip memberships, ideas (the new `EXPLORING` trip status), Pow Wow sessions/transcripts, trip briefs, contributions, ledger entries/split rules/settlements/config, and the price-quote cache, plus `TripEntity.originIdeaId` and `UserPreferenceEntity.ownerTravelerId`.
- `local/TravelDao.kt` — DAO for the pre-existing entities, returning `Flow`s. v10 added domain DAOs alongside it: `PartyDao`, `PowWowDao`, `LedgerDao`, `PricingDao`.
- `di/MarcoRepositories.kt` — the dependency seam. **No DI framework**; feature-scoped ViewModels get their repository from here (`MarcoRepositories.powWow(app)`) instead of going through `TravelViewModel`. Also holds `eraseAllLocalData()`, which the Settings wipe calls — add any new table there.
- `repository/` — `PartyRepository`, `PowWowRepository`, `LedgerRepository`, `PricingRepository` interfaces (with Room-backed implementations) so feature tracks depend on interfaces rather than the god ViewModel. `SettlementEngine` declares the ledger arithmetic contract and is deliberately unimplemented — the ledger track writes it.
- `repository/TravelRepository.kt` — thin pass-through over the DAO, plus `checkAndSeedInitialData()` (seeds baseline currency rates only) and `clearAllLocalData()`. Deliberately holds no other seed/mock data — several commits exist specifically to remove placeholder data, so don't reintroduce it.
- `security/WalletSecurityManager.kt` — Android Keystore AES-GCM object used to encrypt wallet fields (`encryptedAccountDetails`, `encryptedReceiptHash`) before they hit Room.
- `sync/CloudSyncManager.kt` — Firebase Auth (email/password, Google ID token via Credential Manager, password reset) plus one-way Firestore push under `users/{uid}/{trips,memories,preferences,...}`. Exposes `currentUser` / `syncStatus` / `syncMessage` / `lastSyncTimestamp` StateFlows that the ViewModel re-exports.

**Two parallel settings stores exist** — `SettingsManager` (SharedPreferences `marco_app_settings`, holds the `AppSettingsState` data class incl. user-supplied API keys, voice, model, currency) and `DataStoreManager` (Preferences DataStore `marco_preferences`, theme/voice/offline/API-key keys). `SettingsManager` is the one wired to behavior (it feeds the Gemini key/model providers); check both before adding a setting.

**AI layer.** `ai/GeminiTravelService.kt` calls `generativelanguage.googleapis.com/v1beta` directly over OkHttp with hand-rolled `org.json` request/response building — no SDK, no Retrofit for this path. Key resolution order: user key from `SettingsManager` → `BuildConfig.GEMINI_API_KEY`. **Every entry point degrades gracefully**: `callGeminiApi` returns `""` on missing/placeholder key, HTTP error, or exception, and each public method then falls back to a curated local result (`generateCuratedFallbackItinerary`, `getDefaultSuggestedActivities`, `getSmartLocalConciergeResponse`, …). Preserve this pattern — the app is expected to work with no API key. Prompt text and result DTOs (`ItineraryGenerationResult`, `AiSuggestedActivityItem`, `DynamicAdjustmentResult`, …) also live in this file.

### The chat stream is the app's primary surface

`ConciergeChatScreen.kt` (~3.4k lines) is the home screen and the main UI extension point. Two mechanisms drive it:

- **Dual stream:** `ChatMessageEntity.chatType` is `"PRIVATE"` (1-on-1 with Marco) or `"GROUP"` (travel crew). `activeChatStreamTab` in the ViewModel (0/1) selects which the composer targets and which messages render.
- **Rich cards via the `sender` discriminator:** besides `USER`, `CONCIERGE_AI`, and `VOICE_CALL_DISPATCHER`, the ViewModel persists messages whose `sender` is a card type — `CARD_ITINERARY`, `CARD_ITINERARY_SNIPPET`, `CARD_BUDGET_TRACKER`, `CARD_WEEKLY_BUDGET_SUMMARY`, `CARD_REWARDS`, `CARD_DNA`, `CARD_SAFETY`, `CARD_EMERGENCY_SOS`, `CARD_FAMILY_ACCESSIBILITY`, `CARD_ACTIVITY_SUGGESTIONS`, `CARD_DYNAMIC_ADJUSTMENT`, `CARD_MEMORY`, `CARD_GROUP_MEDIA_CAROUSEL`, `CARD_PROACTIVE_DISRUPTION`, `CARD_JOURNEY_COMPLETED`. The screen `when`-dispatches on `sender` to a composable in `ui/components/`; structured payloads travel as JSON in `suggestedActionJson`.

Adding a card means: emit the message with the new `sender` in `TravelViewModel`, add the `when` branch in `ConciergeChatScreen`, add the composable in `ui/components/`.

**Intent routing is keyword-based, not model-based.** `TravelViewModel.sendChatMessage` lowercases the input and runs a large `when { lower.contains(...) }` chain to decide whether to plan a trip, show budget, trigger SOS, etc., including hardcoded destination matching. Free-form text falls through to `sendConciergeMessage`. Extend the chain rather than assuming an LLM classifier exists.

### Theming

Dark-only by design: `MyApplicationTheme` ignores its `darkTheme`/`dynamicColor` params and always applies `LuxuryEditorialColorScheme` — pure-black surfaces with a champagne-gold accent (`Color.kt`). Note that `Color.kt` keeps a large block of **legacy aliases** from earlier parchment/cartography and blue-navy palettes (`ParchmentVellum`, `MaritimeBlue`, `Navy900`, `ElegantLilacPrimary`, …), all re-pointed at the current black/gold tokens. Old names still compile but are not distinct colors; use `LuxuryDarkBase` / `LuxurySurface` / `LuxuryCard` / `LuxuryCardElevated` / `LuxuryBorder` / `ChampagneGold` / `TextPrimary` / `TextSecondary` / `TextMuted` in new code.

Shared UI helpers: `CommonComponents.kt` (`CategoryIconBadge`, `getCategoryStyling`, `AccessibilityTagChip`, `LiveAudioWaveform`, `HeroGradientBanner`) and `ExplorerLoadingAnimations.kt` (`MarcoAstrolabeLoadingAnimation`, `MarcoConciergeTypingIndicator`, …).

## Conventions worth matching

- Entity string fields carry enum-like values as `String` with the allowed set documented in a trailing comment (e.g. `category`, `status`, `severity`); `TripStatus` (`EXPLORING` → `PLANNING` → `IN_PROGRESS` → `COMPLETED`) is the main lifecycle enum, with `TripStatus.fromString` and `TripEntity.isTripInProgress()` tolerating several legacy status spellings and three date formats. `EXPLORING` (v10, backed by `IdeaEntity`) never reads as in-progress and never auto-advances on dates — use `isEligibleForAutoStart()` for date-driven transitions and `isTripInProgress()` for "are they physically on the trip". v10 added more enums alongside it (`PartyUnitType`, `TravelerAgeBand`, `TripRole`, `LedgerModel`, `SplitRuleType`, `QuoteConfidence`, …), each stored as its `value` string with a `fromStringOrNull` that returns null rather than guessing.
- `kotlin.code.style=official`, 4-space indent in `app/src/**` Kotlin (the Gradle build scripts use 2).
- Accessibility, dietary, and family-age requirements are threaded through models, prompts, and card UI as first-class fields — carry them along when adding itinerary features rather than dropping them.
