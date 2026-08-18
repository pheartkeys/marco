package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * ============================================================================================
 * LEDGER — what the trip cost, who paid, and who owes whom
 * ============================================================================================
 *
 * Four ledger models exist and a trip picks one ([TripLedgerConfigEntity]). All four read the same
 * [LedgerEntryEntity] rows; they differ in how [SplitRuleEntity] is applied and whether
 * settlement is produced at all.
 *
 * ## Dual currency, always
 *
 * Every entry stores the amount as it was actually incurred ([LedgerEntryEntity.amountOriginal] in
 * [LedgerEntryEntity.originalCurrency]) *and* the same amount in the trip's currency
 * ([LedgerEntryEntity.amountNormalized]). The rate used is stored on the row with its source and
 * age, because a converted figure whose rate the user cannot inspect is a fabricated figure. When
 * no rate was available, store the original only and leave [LedgerEntryEntity.exchangeRate] at 0.0
 * — that reads as "not converted", and surfaces must show the gap rather than convert at 1.0.
 *
 * ## Points-funded expenses
 *
 * When a booking was paid with points, no cash left the group, so it must not appear in a cash
 * settlement. Set [LedgerEntryEntity.fundedWithPoints] and record what was actually spent in
 * [LedgerEntryEntity.pointsQuantity] / [LedgerEntryEntity.pointsProgramTitle]. Do **not** invent a
 * dollar equivalent: if the group wants one, that is a [ContributionEntity] agreement with
 * signatories. [LedgerEntryEntity.countsTowardCashSettlement] is the single predicate every
 * settlement path must use so the four ledger models can't diverge on this.
 *
 * ## Per-person balances are derived, not stored
 *
 * There is deliberately no balances table. A stored balance goes stale the moment an entry is
 * edited and becomes a second, silently-wrong source of truth. `LedgerRepository` returns
 * [TravelerBalance] computed from entries and split rules on demand. [SettlementEntity] stores
 * only the concrete transfers the group actually agreed to make.
 */

/** How a trip handles money. Chosen once, overridable, never a settings maze. */
enum class LedgerModel(val value: String, val label: String) {
    /** Everyone pays into a pot up front; spending draws down the pot. */
    SHARED_POT("SHARED_POT", "Shared pot"),
    /** People pay as they go and square up at the end. */
    SPLIT_SETTLE("SPLIT_SETTLE", "Split and settle"),
    /** No group money at all; each person tracks their own. */
    PERSONAL("PERSONAL", "Personal"),
    /** Company-funded, with a policy ceiling and reimbursable/non-reimbursable categories. */
    CORPORATE_POLICY("CORPORATE_POLICY", "Corporate + policy");

    /** Whether this model produces inter-member settlement transfers at all. */
    val producesSettlement: Boolean
        get() = this == SHARED_POT || this == SPLIT_SETTLE

    companion object {
        fun fromStringOrNull(value: String): LedgerModel? =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
    }
}

/** How one expense divides across travellers. */
enum class SplitRuleType(val value: String, val label: String) {
    /** Divided evenly across the participants. [SplitRuleEntity.allocationsJson] may be empty. */
    EQUAL("EQUAL", "Split equally"),
    /** Weighted by shares, e.g. a family of four carries 4 shares. Allocations are share counts. */
    SHARES("SHARES", "By shares"),
    /** Explicit amounts per traveller, in the entry's original currency. */
    EXACT("EXACT", "Exact amounts"),
    /** Percentages per traveller; the group is responsible for them summing to 100. */
    PERCENTAGE("PERCENTAGE", "By percentage");

    companion object {
        fun fromStringOrNull(value: String): SplitRuleType? =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
    }
}

/**
 * The trip's money configuration.
 *
 * One row per trip. Track B selects a smart default from the party shape and asks the group to
 * confirm it once during brief review; [confirmedAtTimestamp] of 0 means the group has not
 * confirmed and the UI should still be asking.
 */
@Entity(tableName = "trip_ledger_configs", indices = [Index(value = ["tripId"], unique = true)])
data class TripLedgerConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    /** [LedgerModel.value]. Required. */
    val ledgerModel: String,
    /** Currency all entries normalise to. Defaults to the trip's primary currency at creation. */
    val normalizedCurrency: String,
    /** Trip-level default [SplitRuleEntity.id]. 0 = no default rule recorded yet. */
    val defaultSplitRuleId: Long = 0,
    /** Why this model was defaulted, e.g. "COUPLE_PARTY". "" = chosen manually. */
    val selectionRationale: String = "",
    /** 0 = the group has not confirmed the model yet. */
    val confirmedAtTimestamp: Long = 0,
    /** Who confirmed. 0 = nobody. */
    val confirmedByTravelerId: Long = 0,
    /** CORPORATE_POLICY only: per-person daily cap in [normalizedCurrency]. 0.0 = no cap set. */
    val policyDailyCapAmount: Double = 0.0,
    /** CORPORATE_POLICY only: free text of the policy. "" = none. */
    val policyNote: String = ""
)

/**
 * A recorded expense.
 *
 * [payerTravelerId] is who actually paid — the fact settlement is built from. [createdByTravelerId]
 * is who typed it in, which is often someone else.
 */
@Entity(
    tableName = "ledger_entries",
    indices = [Index("tripId"), Index("payerTravelerId"), Index("splitRuleId"), Index("contributionId")]
)
data class LedgerEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    /** "" = not described. Never generate a description from the category. */
    val description: String = "",
    /** Lodging, Dining, Flights, Activities, Transit, Groceries, Fees. "" = uncategorised. */
    val category: String = "",
    /** Who paid. Required — an expense with no payer cannot be settled. */
    val payerTravelerId: Long,

    // ---- Dual currency -------------------------------------------------------------------------
    /** The amount as incurred. 0.0 is legitimate for a points-funded entry. */
    val amountOriginal: Double,
    /** ISO currency it was incurred in. Required. */
    val originalCurrency: String,
    /** The same amount in [normalizedCurrency]. 0.0 when no rate was available — see [exchangeRate]. */
    val amountNormalized: Double = 0.0,
    /** The trip's ledger currency. "" = not normalised. */
    val normalizedCurrency: String = "",
    /**
     * Rate applied to reach [amountNormalized]. 1.0 when the currencies match.
     * **0.0 means no conversion was performed** — render the original amount and flag the gap.
     */
    val exchangeRate: Double = 0.0,
    /** Where the rate came from, e.g. "SEEDED_CONSTANT", "PRICING_SERVICE_FX". "" = unrecorded. */
    val exchangeRateSource: String = "",
    /** When the rate was fetched. 0 = unknown age; surfaces must not imply it is current. */
    val exchangeRateAsOfTimestamp: Long = 0,

    // ---- Splitting -----------------------------------------------------------------------------
    /** [SplitRuleEntity.id] for this entry. 0 = use the trip default from [TripLedgerConfigEntity]. */
    val splitRuleId: Long = 0,

    // ---- Points funding ------------------------------------------------------------------------
    /** True when this was paid with points/certificate rather than cash. Excludes it from cash settlement. */
    val fundedWithPoints: Boolean = false,
    /** SHAREABLE program title, e.g. "Marriott Bonvoy". "" = not stated. Never a balance or valuation. */
    val pointsProgramTitle: String = "",
    /** How many points/certificates were spent. 0.0 = not stated. */
    val pointsQuantity: Double = 0.0,
    /** The [ContributionEntity] this draws on, when it does. 0 = none. */
    val contributionId: Long = 0,

    // ---- Provenance ----------------------------------------------------------------------------
    /** Date incurred, ISO yyyy-MM-dd. "" = not supplied. */
    val incurredOnIso: String = "",
    /** Who recorded it. 0 = unknown. */
    val createdByTravelerId: Long = 0,
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

/**
 * Should this entry take part in cash settlement?
 *
 * The one predicate every settlement path must call. Points-funded entries are excluded because no
 * cash moved; a zero-cash entry is excluded because there is nothing to settle.
 */
fun LedgerEntryEntity.countsTowardCashSettlement(): Boolean =
    !fundedWithPoints && amountOriginal != 0.0

/** True when the row carries a usable, inspectable conversion. */
fun LedgerEntryEntity.hasUsableConversion(): Boolean =
    exchangeRate != 0.0 && normalizedCurrency.isNotBlank()

/**
 * How an expense divides.
 *
 * A rule with [ledgerEntryId] == 0 is the trip-level default; one with a non-zero id overrides for
 * that single entry.
 */
@Entity(
    tableName = "split_rules",
    indices = [Index("tripId"), Index("ledgerEntryId")]
)
data class SplitRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    /** 0 = this is the trip-level default rule. */
    val ledgerEntryId: Long = 0,
    /** [SplitRuleType.value]. Required. */
    val ruleType: String,
    /**
     * JSON object of travellerId (as a string key) -> number, read with [SplitAllocations].
     * Meaning depends on [ruleType]: share counts, exact amounts, or percentages.
     * "" is valid only for EQUAL, where participation is the membership list.
     */
    val allocationsJson: String = "",
    /**
     * Comma-separated [TravelerEntity.id]s the split applies to, for EQUAL where allocations are
     * empty. "" = every active member of the trip.
     */
    val participantTravelerIdsCsv: String = "",
    val createdByTravelerId: Long = 0,
    val createdAtTimestamp: Long = System.currentTimeMillis()
)

/**
 * Read/write helper for [SplitRuleEntity.allocationsJson].
 *
 * Keys are traveller ids rendered as strings, because JSON object keys are strings. An empty map
 * encodes to "" rather than "{}" so "no allocations" stays one value, matching the convention in
 * [PreferenceWeights] and [TripBriefPayloads]. Malformed JSON decodes to an empty map — a split
 * that cannot be read must surface as unsplit, never as a half-applied division of money.
 */
object SplitAllocations {
    private val moshi = Moshi.Builder().build()
    private val adapter: JsonAdapter<Map<String, Double>> = moshi.adapter(
        Types.newParameterizedType(Map::class.java, String::class.java, Double::class.javaObjectType)
    )

    fun encode(allocations: Map<Long, Double>): String =
        if (allocations.isEmpty()) "" else adapter.toJson(allocations.mapKeys { it.key.toString() })

    fun decode(json: String): Map<Long, Double> {
        if (json.isBlank()) return emptyMap()
        val raw = runCatching { adapter.fromJson(json) }.getOrNull() ?: return emptyMap()
        return raw.mapNotNull { (key, value) ->
            val travelerId = key.trim().toLongOrNull() ?: return@mapNotNull null
            travelerId to value
        }.toMap()
    }
}

/** Lifecycle of a settlement transfer. */
enum class SettlementState(val value: String) {
    /** Computed and shown, not yet accepted by both sides. */
    PROPOSED("PROPOSED"),
    AGREED("AGREED"),
    /** The money actually moved. */
    PAID("PAID"),
    CANCELLED("CANCELLED");

    companion object {
        fun fromStringOrNull(value: String): SettlementState? =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
    }
}

/**
 * One concrete transfer: [fromTravelerId] pays [toTravelerId].
 *
 * Only transfers are stored. The balances they resolve are derived — see [TravelerBalance].
 * Marco proposes a transfer-minimising set; the group may replace it, so PROPOSED rows are
 * disposable and only PAID rows are historical fact.
 */
@Entity(
    tableName = "settlements",
    indices = [Index("tripId"), Index("fromTravelerId"), Index("toTravelerId")]
)
data class SettlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    /** Who pays. Required. */
    val fromTravelerId: Long,
    /** Who receives. Required. */
    val toTravelerId: Long,
    /** Amount in [currency]. */
    val amount: Double,
    /** ISO currency of [amount] — the trip's normalised currency in practice. Required. */
    val currency: String,
    /**
     * The same transfer in the payer's own currency, when they differ. 0.0 = not converted;
     * do not fill this in at a 1.0 rate.
     */
    val amountInPayerCurrency: Double = 0.0,
    /** "" = not converted. */
    val payerCurrency: String = "",
    /** [SettlementState.value]. Required. */
    val state: String,
    /** When the proposal was computed. */
    val computedAtTimestamp: Long = System.currentTimeMillis(),
    /** 0 = not paid. */
    val settledAtTimestamp: Long = 0,
    /** How it was paid, if recorded. "" = unrecorded. */
    val method: String = "",
    val note: String = ""
)

/**
 * A traveller's position on a trip, **derived on demand** by `LedgerRepository` — never stored.
 *
 * [paid] is what they actually laid out, [owed] is their share under the split rules, and
 * [net] is `paid - owed`: positive means the group owes them. All three are in [currency].
 *
 * [pointsContributedQuantity] is carried separately and is deliberately not converted into
 * [currency]: points-funded spending never enters cash settlement, and expressing it in dollars
 * here would manufacture exactly the equivalence the contribution-agreement model exists to keep
 * human.
 */
data class TravelerBalance(
    val travelerId: Long,
    val currency: String,
    val paid: Double,
    val owed: Double,
    val net: Double,
    val pointsContributedQuantity: Double = 0.0,
    /** Entries that had no usable conversion and so are missing from [paid]/[owed]. */
    val unconvertedEntryIds: List<Long> = emptyList()
)
