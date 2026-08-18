# Marco — Phase 0 Foundation

The data foundation and module boundaries the Pow Wow, Group/Ledger, and Pricing tracks build
against. Written for someone who has never seen this codebase.

Read `CLAUDE.md` first for the app-wide orientation (single Compose Activity, one god ViewModel,
no DI framework, dark-only theme). This document covers only what Phase 0 added and the contracts
it fixes in place.

---

## 1. The rules that override everything else

**No fabricated data.** An absent value is stored empty (`""`, `0`, `0.0`, `false`) and rendered as
absent. Never substitute a plausible-looking default. In this codebase the bug has a shape — watch
for `.ifBlank { "something" }` and `?: <realistic literal>`. Several commits and a full review
session exist purely to remove that pattern; do not reintroduce it.

Consequences you will meet constantly:

- `0` in an id column means *no relationship*, not row zero.
- `0` in a timestamp column means *never happened*, not the epoch.
- `""` in a label means *not supplied*, not "unknown" as a display string.
- `exchangeRate == 0.0` means *not converted*. Do not convert at 1.0 to make a number appear.

**Theme tokens only.** `LuxuryDarkBase`, `LuxurySurface`, `LuxuryCard`, `LuxuryCardElevated`,
`LuxuryBorder`, `ChampagneGold`, `TextPrimary`, `TextSecondary`, `TextMuted`. The legacy aliases
(`ParchmentVellum`, `CartographyCard`, `NavigationalGold`, `TextAtlasPrimary`, …) still compile but
are dead aliases from a deleted aesthetic pointing at the same colours. Never use them in new code.

**`TravelViewModel` is frozen.** It is ~2,000 lines because every past change landed there. New
feature state goes in a feature-scoped ViewModel that pulls its own repository from
`MarcoRepositories`. If a track adds to `TravelViewModel`, the parallel plan collapses.

---

## 2. File layout

`data/model/` was one 305-line file (`TravelModels.kt`) holding 14 entities. It is now split by
domain. Everything is in the same package `com.example.data.model`, so nothing else needed an
import change.

| File | Holds |
|---|---|
| `TripModels.kt` | `TripEntity`, `TripStatus`, `TripActivityEntity`, `TripFeedbackEntity`, `ProactiveSuggestionEntity`, and the status predicates |
| `ChatModels.kt` | `ChatMessageEntity` (the `CARD_*` dispatcher's payload) |
| `UserPreferenceModels.kt` | `UserPreferenceEntity` — the device owner's Traveler DNA |
| `MoneyModels.kt` | **PRIVATE** financial entities: `ConnectedAccountEntity`, `ExpenseEntity`, `WalletBalanceEntity`, `WalletTransactionEntity`, `CurrencyRateEntity` |
| `TripSupportModels.kt` | `VendorCallLogEntity`, `EmergencyAlertEntity`, `GroupMemoryEntity` |
| `PreferenceWeights.kt` | (pre-existing) Moshi codec for the 1–7 intensity weights |
| **`PartyModels.kt`** | `TravelerEntity`, `PartyUnitEntity`, `TripMembershipEntity` + the `PartyUnitType` / `TravelerAgeBand` / `TripRole` / `TripMembershipState` enums |
| **`IdeaModels.kt`** | `IdeaEntity` — the disposable `EXPLORING` record |
| **`PowWowModels.kt`** | `PowWowSessionEntity`, `PowWowTranscriptEntity`, `PowWowSessionState`, `PowWowLimits` |
| **`TripBriefModels.kt`** | `TripBriefEntity` + typed JSON payloads and the `TripBriefPayloads` codec |
| **`ContributionModels.kt`** | `ContributionEntity`, the agreement helpers, `ShareableProgramRef` |
| **`LedgerModels.kt`** | `LedgerEntryEntity`, `SplitRuleEntity`, `SettlementEntity`, `TripLedgerConfigEntity`, `TravelerBalance`, `LedgerModel`, `SplitRuleType` |
| **`PricingModels.kt`** | `QuoteConfidence`, `QuoteRequest`, `PriceQuote`, `QuoteResult`, `PriceQuoteCacheEntity` |

DAOs are split the same way: `TravelDao` (pre-existing entities), **`PartyDao`**, **`PowWowDao`**,
**`LedgerDao`**, **`PricingDao`**. Repository interfaces live in `data/repository/`, and the
dependency seam is `data/di/MarcoRepositories.kt`.

---

## 3. The party model: Trip → PartyUnit → Traveler

A trip has **party units**; each unit holds **travellers**. This is the shape the UI draws: grouped
unit cards with member chips and a unit-type label.

```
TripEntity ──1:*── PartyUnitEntity
     │                    ▲
     │                    │ partyUnitId (0 = unassigned)
     └──1:*── TripMembershipEntity ──*:1── TravelerEntity
```

**The traveller-to-trip link is `TripMembershipEntity`, and it — not the traveller, not the unit —
carries `partyUnitId`.** A person exists once and can appear on many trips in a different unit each
time (with their partner in June, with the whole family in December). Putting the unit on the
membership keeps one row per `(traveller, trip)` (enforced by a unique index) and makes "which unit
is Dana in on *this* trip" a single lookup.

`partyUnitId == 0` is valid and expected: someone joins before the organiser has sorted the party.
Render them as unassigned; never auto-file them into a guessed unit.

**Unit types** (`PartyUnitType`): `COUPLE`, `FAMILY`, `SOLO`, `CORPORATE_TEAM`.

**Age bands** (`TravelerAgeBand`): `ADULT`, `TEEN`, `CHILD`.
Accounts exist at 14+, so `CHILD.canHoldAccount` is false — a child never holds an account and their
Pow Wow is captured as a guest on a guardian's device. `CHILD.requiresGuardianConsent` is true.
An **undeclared** age band (`""` → `fromStringOrNull` returns null) is never assumed to be an adult;
consent checks fail closed on it.

**Roles** (`TripRole`): `ORGANIZER` (manages membership, party structure, ledger config),
`TRAVELER` (full participant), `VIEWER` (read-only). `TripRole.canWriteTripContent` mirrors the
`canWrite()` function in `firestore.rules` — keep them in step.

**Membership states** (`TripMembershipState`): `INVITED`, `ACTIVE`, `DECLINED`, `REMOVED`.

---

## 4. Trip lifecycle, with EXPLORING

```
EXPLORING ──(Pow Wow promotes)──▶ PLANNING ──(dates arrive)──▶ IN_PROGRESS ──▶ COMPLETED
```

`EXPLORING` is new in v10 and sits **ahead of** `PLANNING`. It is backed by `IdeaEntity`: cheap,
disposable research records that are meant to be thrown away. Promotion sets `IdeaEntity.promotedTripId`
and `TripEntity.originIdeaId`; the idea row is kept as provenance rather than deleted.

Three predicates, and they are not interchangeable:

| Predicate | Answers | EXPLORING |
|---|---|---|
| `TripEntity.isTripInProgress()` | Is the traveller physically on this trip now? Drives SOS, live cockpit. | always `false` |
| `TripEntity.isTodayWithinTripDates()` | Pure date-range check. | n/a |
| `TripEntity.isEligibleForAutoStart()` | May a background job flip this to IN_PROGRESS? | always `false` |

`isTripInProgress()` matches explicit statuses first (including legacy `ACTIVE` / `ON_TRIP` /
`PAST`) and only falls through to the date range for genuinely unknown spellings. **EXPLORING
returns false unconditionally** — an idea with a rough window that happens to cover today is still
an idea, and letting it fall through would light up live-trip surfaces for a trip nobody agreed to
take.

`TripStatus.fromString` still falls back to `PLANNING`, not `EXPLORING`, for unrecognised input:
rows written before v10 were all planning-stage, and re-reading them as EXPLORING would silently
demote real trips.

> **Bug fixed in Phase 0.** `checkAndAutoTransitionTripStatus()` used to gate on
> `isTripInProgress()`, which returns false for `PLANNING` — so a planning trip whose dates had
> arrived never actually transitioned; the check only ever fired for rows carrying a legacy status
> string. `isEligibleForAutoStart()` does its own date check and fixes this.

---

## 5. The Pow Wow

One recorded brain dump per member, analysed together. `PowWowSessionEntity` anchors to a **trip or
an idea** — the ritual usually runs while the group is still EXPLORING, so `ideaId` is set and
`tripId` is 0 until promotion.

### There is deliberately nowhere to store audio

`PowWowSessionEntity` has **no blob column, no file path, no URI**. This is not an oversight and
must not be "fixed". Raw audio is deleted as soon as transcription succeeds, and under-14 guest
capture makes that a legal obligation. A path column would survive process death, get synced, get
backed up, and outlive the file it names — so the column does not exist. The recorder keeps its temp
path in memory in the capture layer.

`audioDeletedAtTimestamp` is the only durable record that deletion happened.
`PowWowDao.getSessionsAwaitingAudioDeletion()` is the retention audit query; it should be empty
outside an in-flight capture.

### Consent is a precondition

- `consentGrantedAtTimestamp == 0` → no consent → recording must not start.
- A `CHILD` speaker additionally needs `guardianConsentGrantedAtTimestamp != 0`.
- A guest capture does **not** inherit the capturing adult's age band. The speaker's own band
  governs, which is the entire point of the guardian consent.

`PowWowRepository.markRecordingStarted()` **throws** unless `consentStatus()` is
`ConsentStatus.Granted`. Check `consentStatus()` first and show the gate; the throw is the backstop.

`PowWowLimits`: 30s minimum, 300s maximum.

### Transcripts are private; only the brief is shared

A member's unedited brain dump is not group content. Transcripts sync under `/users/{uid}/...`.
Only the synthesised `TripBriefEntity` is published to the shared trip document, and
`firestore.rules` denies the transcript path under `/trips`. Synthesis that needs every transcript
runs server-side with admin credentials.

### The brief

`TripBriefEntity` holds `agreementsJson` / `tensionsJson` / `resolutionsJson` / `readinessJson`,
matching the project's existing JSON-in-a-column convention. **Use `TripBriefPayloads` to read and
write them** — hand-rolling the JSON in three tracks would produce three incompatible shapes.

Typed payloads: `BriefAgreement`, `BriefTension` (with `BriefPosition`), `BriefResolution`,
`BriefReadinessItem`.

An empty column encodes back to `""` rather than `"[]"`, so "produced nothing" and "not produced"
stay indistinguishable in storage and both render as absent. Malformed JSON decodes to an empty
list — a half-parsed brief must never be presented as a whole one.

A re-synthesis writes a **new version row**; it does not overwrite. History is the audit trail.

Tensions are named and attributed, never averaged into a midpoint. That is a product position, not
an implementation detail.

---

## 6. Contributions: the offer is a fact, the value is a decision

The premise the product rests on: a group can take a trip none of its members could afford alone.
Dana's timeshare week + Mike's points + Pete's cash. Combining those requires somebody to say what a
week is worth against a dollar — **and that is a human decision, not a computation.**

So `ContributionEntity` has two halves that must never be conflated:

1. **The offer, in native units** — `nativeQuantity` + `nativeUnitLabel` ("1 week", "120,000
   points"). A fact.
2. **The agreement, in money** — the `agreed*` columns. A recorded decision with **signatories** and
   a **timestamp**.

```kotlin
// The only sanctioned way to attach a value. Throws on no signatories or no currency.
contribution.withRecordedAgreement(
    amount = 2400.0, currency = "USD", agreedByTravelerIds = listOf(7L, 8L)
)

// The only correct way to read one. 0.0 in agreedValueAmount means "never agreed", not "worthless".
if (contribution.hasRecordedAgreement) { /* … */ }
```

`LedgerRepository.recordContributionAgreement()` routes through the same helper, so the repository
API cannot create an unsigned agreement either.

**Proposals are separate and must be labelled.** Marco may *propose* a figure from user-supplied
data — that is the `proposed*` columns, and `proposalSource` (a `ContributionProposalSource`) is
required. `hasLabelledProposal` is false without it, and an unlabelled proposal is not displayable.
There is deliberately no code path that promotes a proposal to an agreement without signatories.

---

## 7. Ledger

`TripLedgerConfigEntity` holds the trip's `LedgerModel`: `SHARED_POT`, `SPLIT_SETTLE`, `PERSONAL`,
`CORPORATE_POLICY`. All four read the same `LedgerEntryEntity` rows and differ only in how
`SplitRuleEntity` is applied and whether settlement is produced (`LedgerModel.producesSettlement`).
`confirmedAtTimestamp == 0` means the group has not accepted the model yet and the UI should still
be asking.

**Dual currency, always.** Every entry stores the amount as incurred *and* in the trip's currency,
with the rate, its source, and its age on the row. A converted figure whose rate the user cannot
inspect is a fabricated figure. `exchangeRate == 0.0` means *not converted* — show the original and
flag the gap. `hasUsableConversion()` is the check.

**Points-funded expenses.** Set `fundedWithPoints` and record `pointsQuantity` /
`pointsProgramTitle`. Do **not** invent a dollar equivalent; if the group wants one, that is a
contribution agreement with signatories. `countsTowardCashSettlement()` is the single predicate every
settlement path must use, so the four ledger models cannot diverge on this.

**Per-person balances are derived, not stored.** There is no balances table on purpose: a stored
balance goes stale the moment an entry is edited and becomes a silently-wrong second source of
truth. `TravelerBalance` is a plain data class returned on demand. `SettlementEntity` stores only
the concrete transfers.

`TravelerBalance.unconvertedEntryIds` carries the entries that had no usable rate and are therefore
missing from `paid`/`owed` — surface them rather than quietly producing a smaller total.
`pointsContributedQuantity` is deliberately *not* converted into the balance currency.

### `SettlementEngine` — declared here, implemented by the ledger track

Phase 0 declares the arithmetic contract and does not implement it:

```kotlin
interface SettlementEngine {
    val ledgerModel: LedgerModel
    fun balances(entries, splitRules, contributions, travelerIds, normalizedCurrency): List<TravelerBalance>
    fun proposeTransfers(tripId, balances, normalizedCurrency): List<SettlementEntity>
}
```

Both functions are pure — everything comes in as a parameter, nothing is written back. That is what
makes four ledger models testable against fixed inputs. Implementations live in `feature/ledger/`.

---

## 8. Repository interfaces and the dependency seam

**There is no DI framework and none is being added.** Historically everything was constructed in
`TravelViewModel.init`, which is why that class became the only place features could live.
`data/di/MarcoRepositories.kt` replaces that:

```kotlin
class PowWowViewModel(app: Application) : AndroidViewModel(app) {
    private val powWow = MarcoRepositories.powWow(app)
    private val party  = MarcoRepositories.party(app)
}
```

Every accessor returns a process-wide singleton over the one `AppDatabase`, so repositories obtained
here share the same underlying `Flow`s as the rest of the app. Tests use `overrideForTests()` /
`resetForTests()`.

| Interface | Owns | Implementation |
|---|---|---|
| `PartyRepository` | travellers, party units, memberships, ideas | `RoomPartyRepository` |
| `PowWowRepository` | sessions, consent, transcripts, briefs | `RoomPowWowRepository` |
| `LedgerRepository` | ledger config, contributions, entries, split rules, settlements | `RoomLedgerRepository` |
| `PricingRepository` | normalised quotes + offline cache | `CachedOnlyPricingRepository` |
| `TravelRepository` | (pre-existing) trips, activities, wallet, chat, preferences | concrete class, unchanged |

The interfaces are persistence and observation. Policy that belongs to a feature — party-shape
heuristics, invitation transport, ledger arithmetic, supplier calls — stays in the feature layer.
Two exceptions, both deliberate, because a UI-only guarantee is one refactor from being no
guarantee: `PowWowRepository` enforces consent-before-recording, and `LedgerRepository` enforces
signed contribution agreements.

`MarcoRepositories.eraseAllLocalData(context)` is what "erase all local data" in Settings calls. It
reaches past the interfaces to the DAOs because a wipe must cover tables no repository exists for
yet. **Any new table must be added there.**

---

## 9. Pricing

The app never calls a supplier directly. A pricing service normalises every supplier to one quote
shape and `PricingRepository` exposes it.

`QuoteConfidence` is the load-bearing idea. A figure with no tier is not renderable:

| Tier | Means | Rendering obligation |
|---|---|---|
| `KNOWN` | curated versioned catalog | always with its "as of" date |
| `ESTIMATED` | live supplier quote | with its fetch timestamp |
| `MODELED` | LLM estimate during preliminary planning | explicitly labelled an estimate |
| `UNKNOWN` | no figure | **render a gap to fill — never a number** |

`QuoteResult` is a sealed type rather than a nullable quote, so `Unavailable` can carry a reason a
surface can show ("No live quote available offline") instead of a blank that looks like zero.

`CachedOnlyPricingRepository` is the honest pre-service state: it answers from
`PriceQuoteCacheEntity` when it has a row and reports unavailable otherwise. It never estimates and
never falls back to a hardcoded figure. The pricing track swaps the implementation in
`MarcoRepositories.pricing()` and nothing else in the app changes.

`PriceQuoteCacheEntity` ships in v10 rather than later so the pricing track does not have to bump
the database version underneath the other two tracks.

> The FX rates seeded by `TravelRepository.checkAndSeedInitialData()` are **stale hardcoded
> constants**. Replacing them with a live FX supplier is the pricing track's first job.

---

## 10. Firestore: the hybrid model

Room is the local source of truth; Firestore is what makes a trip shared. `firestore.rules` was
rewritten in Phase 0 — it previously permitted only `/users/{uid}/**` and actively forbade all
sharing.

```
/users/{uid}/**      PRIVATE, never shared. Plaid, balances, valuations, wallet,
                     personal expenses, Pow Wow transcripts.

/trips/{tripId}      SHARED with members, gated by a `members` map on the trip document.
  ├── partyUnits/
  ├── briefs/
  ├── itinerary/
  ├── memberPrograms/    ← program TITLES and TYPES only
  ├── contributions/
  ├── ledgerEntries/
  ├── splitRules/
  └── settlements/
```

The trip document carries:

```jsonc
{
  "members": {
    "<authUid>": { "role": "ORGANIZER", "travelerId": 12, "state": "ACTIVE" }
  }
}
```

Membership in that map grants access. It mirrors `TripMembershipEntity` — keep the two in step.
`ORGANIZER` and `TRAVELER` may write shared content; `VIEWER` is read-only. Only an `ORGANIZER` may
change the `members` map, so a member cannot promote themselves.

### The privacy line, and how the rules enforce it

> A loyalty or timeshare program's **title and type** may be published into a shared trip document.
> Its **balance, valuation, tier, and account number may not** — not ever, not by any client.

Enforced in rules and not in app code, because app code is one refactor from leaking and a
compromised client has no app code at all. Three mechanisms:

1. **Key allowlists.** Every money-bearing shared collection declares its exact accepted field set
   via `keys().hasOnly([...])`. A client that copies a whole `ConnectedAccountEntity` into a
   contribution is **rejected, not sanitised** — the extra keys are not on the list. New fields fail
   closed.
2. **Scalar type checks.** `hasOnly()` only inspects top-level keys, so every allowed field is
   asserted `is string` / `is number` / `is bool` / `is list`. That stops a private object being
   smuggled inside an allowed key as a nested map.
3. **A private-key denylist** (`hasNoPrivateFinancialKeys`) applied to every `/trips` write as
   defence in depth: `balanceValue`, `rewardsEstimatedValuationUsd`, `exchangePowerScore`,
   `tierStatus`, `accountNumberMasked`, `encryptedAccountDetails`, `plaidAccessToken`, and the rest.

`memberPrograms` is the line in its purest form — the allowlist is exactly
`ownerUid`, `programTitle`, `programType`, `addedAtMillis`, and not one of them is a number
describing money. App-side, `ConnectedAccountEntity.toShareableProgramRef()` is the sanctioned way
to build one; it returns only the title and type by construction, so the safe path is also the
shortest.

Two further invariants the rules enforce server-side, because they are product law:

- **A contribution's agreed value must be signed** — a currency, a timestamp `> 0`, and a non-empty
  `agreedByUids` list. A bare amount is rejected. This is "the agreement is a recorded human
  decision, never a computed equivalence" expressed in rules.
- **A proposed value must be labelled** with a recognised `proposalSource`, so no client can render
  Marco's suggestion as though the group had agreed it.

What *is* shareable: a group expense amount and who paid it, and a group-agreed contribution value.
Those are group decisions, not facts about one person's account. What is never shareable: what your
account holds and what a program is worth to you.

The `allow read, write: if false` blocks at the bottom of the rules file (Pow Wow transcripts,
connected accounts, wallet under a trip) are **documentation**. Firestore grants on any matching
allow, so a deny block cannot revoke one; the real enforcement is that no rule grants those paths
and the default is deny. They exist so a future broad `allow` under `/trips` has to argue with a
comment first.

---

## 11. Database version

**Room v10**, `fallbackToDestructiveMigration()` retained per project precedent — there is no
migration path, so bumping the version wipes local data, and that is accepted at this stage.

v10 added: `travelers`, `party_units`, `trip_memberships`, `ideas`, `pow_wow_sessions`,
`pow_wow_transcripts`, `trip_briefs`, `contributions`, `ledger_entries`, `split_rules`,
`settlements`, `trip_ledger_configs`, `price_quote_cache`, plus `TripEntity.originIdeaId`,
`UserPreferenceEntity.ownerTravelerId`, and `@Index` annotations on existing `tripId` columns.

---

## 12. File ownership after Phase 0

| Track | Owns |
|---|---|
| Phase 0 (done) | `data/model/**`, `data/local/**`, `data/repository/**` interfaces, `data/di/**`, `firestore.rules`, `CLAUDE.md`, this file |
| A — Pow Wow | `feature/powwow/**`, new `CARD_POW_WOW_*` branches in `ConciergeChatScreen.kt` |
| B — Group/Ledger | `feature/party/**`, `feature/ledger/**` (incl. `SettlementEngine` implementations), `ui/screens/WalletRewardsScreen.kt`, no-typing conversions |
| C — Integrations | `services/pricing/**`, Cloud Functions, `PreferenceConstants.kt` taxonomy additions, the live `PricingRepository` |

`TravelViewModel.kt` is frozen. Tracks add feature-scoped ViewModels.

---

## 13. Verification

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug      # must stay at 0 errors
```

`app/src/test/java/com/example/FoundationSchemaTest.kt` pins the invariants above: EXPLORING never
reads as in-progress or auto-starts, an unsigned contribution agreement is rejected, points-funded
entries are excluded from cash settlement, a child's session needs guardian consent, an undeclared
age band is never assumed adult, and every JSON codec round-trips with empty staying empty. If a
change to the foundation breaks one of those, the change is wrong.
