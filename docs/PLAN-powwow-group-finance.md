# Marco: Pow Wow, Group Travel & Travel Finance Platform

> **Implementation handoff — read this first.**
>
> **Branch:** `feature/powwow-group-finance`. `main` stays stable; everything here merges only after review.
>
> **Assignment:** Opus implements **Phase 0** (foundation — schema, migration, repository interfaces, Firestore rules) and lands it as its own commit. **Gemini then implements Tracks A, B, and C** against that foundation. Opus reviews Gemini's work afterward and either fixes it or dispatches Sonnet to.
>
> **Do not start a track until Phase 0 is committed and verified.** The tracks share a schema; forking early guarantees merge conflicts in `TravelViewModel` and `TravelModels.kt`.
>
> **Non-negotiable project laws** (violating these is worse than missing scope):
> 1. **No fabricated data.** Absent values are stored empty and rendered as absent — never a plausible-looking default. Watch for `.ifBlank { "something" }` and `?: <realistic literal>`; that is the shape of the bug. An entire review pass exists to enforce this, and commit `8c9ee34` removed a previous round of it.
> 2. **No invented numbers in financial UI.** Never render a figure whose inputs the user did not supply or cannot inspect. Pooled contributions use *group-agreed* valuations recorded as human decisions, never computed equivalences.
> 3. **Theme tokens only**: `LuxuryDarkBase`, `LuxurySurface`, `LuxuryCard`, `LuxuryCardElevated`, `LuxuryBorder`, `ChampagneGold`, `TextPrimary`, `TextSecondary`, `TextMuted`. Never the legacy aliases (`ParchmentVellum`, `CartographyCard`, `NavigationalGold`, `TextAtlasPrimary`) — they are dead aliases pointing at the same values, left from a deleted aesthetic.
> 4. **`TravelViewModel.kt` is frozen** after Phase 0. It is ~2,000 lines because every past change accreted there. New state belongs in feature-scoped ViewModels built against the Phase 0 repository interfaces.
> 5. **No typing for high-variance input.** Typing is allowed only for low-variance normalizable values (a first name, an airport code). Anything with substantial variation in phrasing or format must be voice, chat, or a structured picker. Amounts get a numeric pad or stepper, never a raw text field.
> 6. **Respect file ownership** (see the table below) so the three tracks do not collide.
>
> **Verification before declaring any track done:**
> ```
> ./gradlew assembleDebug
> ./gradlew testDebugUnitTest
> ./gradlew lintDebug     # currently 0 errors — keep it there
> ```
> A physical device is available (Samsung SM-F936U). `adb` is not on PATH: use `/Users/po/Library/Android/sdk/platform-tools/adb`. `screencap` prints a multi-display warning to stdout that corrupts piped PNGs — capture to `/sdcard` then `adb pull`. When asserting against the Room DB, pull the `-wal` file alongside the `.db` or you will read false zeroes.
>
> Architecture reference for the foundation lives in the repo alongside this file once Phase 0 lands.


## Context

Marco today is a single-user Android app with no backend. Onboarding, the Traveler Passport, and weighted preferences shipped and are verified on device. What does **not** exist: any multi-user model, any voice capture, any real pricing data, and any way to share a trip — `firestore.rules` actively forbids it, permitting only `/users/{uid}/**`.

This plan builds Marco's actual differentiators: a **voice-first trip origination ritual (the Pow Wow)**, **group travel with pooled resources**, and **travel finance that spans cash, points, timeshares, and memberships**. The premise that makes it novel: *a group can take a trip none of its members could afford alone* — Dana's timeshare week plus Mike's points plus Pete's cash.

This is a platform build, not a feature. It adds two backends, third-party integrations, and privacy obligations.

### Settled decisions

| Decision | Answer |
|---|---|
| Account linking | Real backend — **Plaid** for money (transactions + balances, US, sandbox first), manual/OCR for loyalty |
| Multi-user | **Real backend multi-user**; accounts at **14+**, parental guidance on sensitive features |
| Trip origination | **Pow Wow**: recorded voice brain dump, 30s min / 3–5 min max, one per member, analyzed together |
| Research vs planning | `EXPLORING` (disposable Ideas) precedes `PLANNING`; the Pow Wow is the promotion ritual |
| Synthesis | Merge commonality, guide correlation, **mediate conflicts with DNA-backed proposals toward agreement** |
| Voice pipeline | **Record → batch transcribe** via Gemini. Timed prompt rail now; adaptive bubbles later |
| Under-14 | Guest capture on a parent's device, consent gate, **audio deleted after transcription** |
| Ledger | **All four models** (shared pot, split/settle, personal, corporate+policy), smart default by party shape, overridable |
| Pooling | **Group-agreed valuation** — humans set the rate, Marco records the agreement and computes from it |
| Financial privacy | Contribution-only by default. Program **titles/types are group-visible; monetary values never are** |
| Pricing | Model estimates in preliminary planning; live APIs where possible; many suppliers, tiered by confidence |
| Party structure | **Trip → Party Units → Travelers**, drawn as grouped unit cards with member chips |
| Input | Typing allowed only for low-variance normalizable values (first name, airport code). High-variance input → voice/chat or structured pickers |
| Backend | **Both** — Firebase (identity, shared data, AI) + a pricing service |

---

## Phase 0 — Foundation (serialized, blocking, Opus)

Nothing forks until this lands and verifies. Three agent families writing into `TravelViewModel` (~2,000 lines) and `TravelModels.kt` simultaneously will collide — we observed file contention with a single agent.

**Schema** (`data/model/`, split out of the monolithic `TravelModels.kt`):
- `TravelerEntity` — identity, age band (adult/teen/child), account-linked or guest
- `PartyUnitEntity` — unit type (couple/family/solo/corporate-team), belongs to a trip, holds travelers
- `TripMembershipEntity` — traveler ↔ trip, role (organizer/traveler/viewer)
- `IdeaEntity` — disposable research record (destination, rough window, why); promotable to a trip
- `PowWowSessionEntity` + `PowWowTranscriptEntity` — per-member session, duration, consent flags, transcript; **no audio blob**
- `TripBriefEntity` — synthesized output: agreements, open tensions, resolutions
- `ContributionEntity` — who offers what (native unit + agreed value + agreement record)
- `LedgerEntryEntity` + `SplitRuleEntity` + `SettlementEntity` — payer, split rule, per-person balances, dual-currency
- Extend `TripStatus` with `EXPLORING` ahead of `PLANNING`

**Migration:** Room **v9 → v10**. Keep `fallbackToDestructiveMigration()` per project precedent; update the CLAUDE.md note.

**Boundaries:** extract repository interfaces per domain (`PowWowRepository`, `PartyRepository`, `LedgerRepository`, `PricingRepository`) so the three tracks depend on interfaces, not on `TravelViewModel`. **`TravelViewModel` must stop growing** — new state belongs in feature-scoped ViewModels.

**Firestore (hybrid model):**
- `/trips/{tripId}` — shared, members map drives rules; contains party structure, brief, itinerary, contributions (native unit + agreed value only)
- `/users/{uid}/**` — unchanged and never shared: Plaid data, account balances, monetary values
- Program **titles and types** may publish to the trip; **balances and valuations may not** — enforced in rules, not app code

---

## Track A — Pow Wow (Gemini)

The differentiator. Ships first-usable and produces the brief every other track consumes.

1. **Capture** — `MediaRecorder` (`RECORD_AUDIO` is already declared and currently unused). Enforce 30s minimum / 5min maximum with visible countdown. Consent gate before the first recording; guest-capture mode for under-14 with parental consent.
2. **Prompt rail** — deterministic timed prompts, no LLM. Structure the rail around: who's going, when, why this trip, non-negotiables, what worries you, budget feel.
3. **Transcription** — **use `firebase-ai`, which is already a declared but entirely unused dependency.** Firebase AI Logic proxies Gemini with audio input, so no API key ships in the APK and no transcription server is needed. Delete raw audio immediately after transcription succeeds.
4. **Synthesis** — merge N transcripts + N weighted DNA profiles (`PreferenceWeights`, `motivationWeightsJson` et al. already exist) into a `TripBrief`: agreed points, correlations, and **named tensions with DNA-backed resolution proposals**. Never average preferences into mush.
5. **Surfacing** — new `CARD_POW_WOW_BRIEF` and `CARD_TENSION` following the existing `sender = CARD_*` dispatcher pattern in `ConciergeChatScreen`. Readiness checklist derived from the brief: origin→destination transport, lodging per night, a spine per day, budget reconciles. No critical gap ⇒ eligible for `PLANNING → IN_PROGRESS` via the existing `checkAndAutoTransitionTripStatus()`.

## Track B — Group, Party & Ledger (Gemini)

Against Phase 0's fixed schema and interfaces.

1. **Party unit UI** — grouped unit cards, gold hairline per unit, member chips, unit type label. Must hold 12 travelers at 344dp. Reuse `LuxuryCard`/`ChampagneGold`/`TextPrimary` tokens; never the legacy aliases.
2. **Invitations** — link/QR join, roles, membership writes.
3. **Four ledger engines** behind one interface; smart default selected by party shape, confirmed once during brief review, overridable. Never a settings maze.
4. **Contributions & pooling** — offer an asset in native units; group-agreed valuation recorded as an **agreement** (who agreed, when), never a computed equivalence. Marco may *propose* a figure from user-supplied data, explicitly labelled a proposal.
5. **Settlement** — per-person balances, transfer-minimizing settlement, dual-currency throughout. Points-funded expenses tracked as **"funded with points"** and excluded from cash settlement.
6. **No-typing conversion** — audit every existing `OutlinedTextField`. Amounts → numeric pad/stepper. Categories, payers, currencies, split rules → pickers. The onboarding **uncanny question becomes voice**. Keep typing only for first name and airport code.

## Track C — Pricing Service & Integrations (Gemini)

Self-contained behind a normalized schema; touches no app-layer files.

1. **Service** — Node/TypeScript on Railway. Every supplier normalizes to one quote schema: `{ amount, currency, source, fetchedAt, confidence }`. The app never calls a supplier directly.
2. **Confidence tiers** — `known` (curated versioned catalog, with "as of"), `estimated` (live supplier quote, timestamped), `modeled` (LLM estimate during preliminary planning, explicitly labelled), `unknown` (rendered as a gap to fill, never silently guessed).
3. **First suppliers** — **FX** (free tier, replacing the hardcoded seed rates in `TravelRepository.checkAndSeedInitialData()`, which are stale constants today), **Google Places** (street food → fine dining in one integration), **Amadeus Self-Service** (flights + hotels). Rail, rideshare, activities, and grocery follow behind the same schema.
4. **Plaid** — Cloud Function holds secrets and performs token exchange; `transactions` + `balances` only, US, sandbox until the money surfaces are real. Skip liabilities, investments, and payment initiation.
5. **Local taxonomy** — transport (bicycle → rail → economy → business → charter → private), lodging (hostel → timeshare → boutique → resort → villa), dining (street → casual → notable → fine). Extends `PreferenceConstants`, which already holds `LOYALTY_CATALOG`, `MAJOR_AIRPORTS`, and `ComfortOption`. This is what makes bicycles-to-private-jets real offline.

---

## File ownership (no overlap after Phase 0)

| Track | Owns |
|---|---|
| Phase 0 | `data/model/**`, `data/local/AppDatabase.kt`, repository interfaces, `firestore.rules`, `CLAUDE.md` |
| A — Pow Wow | `feature/powwow/**`, new `CARD_POW_WOW_*` branches in `ConciergeChatScreen.kt` |
| B — Group/Ledger | `feature/party/**`, `feature/ledger/**`, `ui/screens/WalletRewardsScreen.kt`, no-typing conversions |
| C — Integrations | `services/pricing/**` (separate repo/dir), Cloud Functions, `PreferenceConstants.kt` taxonomy additions |

`TravelViewModel.kt` is **frozen** to Phase 0 edits. Tracks add feature-scoped ViewModels.

## Reuse, don't rebuild

`TripStatus` + `checkAndAutoTransitionTripStatus()` (lifecycle) · `PreferenceWeights` (Moshi encode/decode/topKey) · `PreferenceConstants` (catalogs) · `ConnectedAccountEntity` + `addConnectedAccount`/`updateConnectedAccount` · `CurrencyRateEntity` + `convertCurrency` · `WalletSecurityManager` (Keystore AES-GCM) · `CloudSyncManager` (Auth) · `TravelerPassportCard`, `IntensityScaleRow` · CameraX + ML Kit (already wired) · the `CARD_*` dispatcher pattern.

## Verification

1. `./gradlew assembleDebug testDebugUnitTest lintDebug` — lint currently passes with **0 errors**; keep it there.
2. **Roborazzi goldens** for every new surface — Pow Wow capture, brief, tension card, party units, ledger, settlement. Only one golden exists today (`greeting.png`); this is the first work large enough that shipping without them is negligent.
3. **On device** (`installDebug`, Samsung SM-F936U). Verify at **344dp folded** — party units with 12 travelers, prompt rail, numeric pads.
4. **Database assertions** via `adb shell run-as com.go.marco` + `sqlite3`, pulling the `-wal` alongside the `.db` (WAL mode gives false zeroes otherwise).
5. **Privacy tests, non-negotiable**: a member cannot read another member's `/users/{uid}` financial data; program titles publish to a trip while values do not; under-14 audio is absent from storage after transcription. Test against the rules, not the UI.
6. **Offline**: ledger fully readable; cached values badged stale with timestamp; live-quote surfaces disabled rather than estimated.
7. **No fabricated numbers**: no dollar figure renders whose inputs the user didn't supply or can't inspect.

## Risks

- **Scope.** Three verticals in parallel is aggressive; Phase 0 is the only thing preventing merge chaos, so it cannot be rushed or partially landed.
- **Plaid + Amadeus onboarding** are external dependencies with lead time — start applications during Phase 0.
- **Minors' data.** 14+ accounts avoid COPPA, but guest capture of under-14 voice requires the consent gate and audio deletion to be correct on the first try, not retrofitted.
- **`TravelViewModel` gravity.** Every past change accreted here. If tracks add to it instead of feature ViewModels, the parallel plan collapses.
