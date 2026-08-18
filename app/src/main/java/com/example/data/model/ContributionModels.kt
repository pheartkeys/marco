package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ============================================================================================
 * CONTRIBUTIONS — who offers what, and what the group agreed it is worth
 * ============================================================================================
 *
 * The premise the whole product rests on: a group can take a trip none of its members could afford
 * alone. Dana's timeshare week + Mike's points + Pete's cash. To combine those, somebody has to say
 * what a week is worth relative to a dollar — and **that is a human decision, not a computation.**
 *
 * So a contribution has two distinct halves and they must never be conflated:
 *
 *  1. **The offer, in native units.** "One RCI week." "120,000 Bonvoy points." This is a fact.
 *     It lives in [ContributionEntity.nativeQuantity] / [ContributionEntity.nativeUnitLabel].
 *
 *  2. **The agreement, in money.** "We all agree Dana's week counts as $2,400 toward the pot."
 *     This is a recorded decision: it has signatories and a timestamp. It lives in the
 *     `agreed*` columns and is only valid when [ContributionEntity.hasRecordedAgreement] is true.
 *     Write it with [ContributionEntity.withRecordedAgreement]; never assign the amount alone.
 *
 * Marco may **propose** a figure from data the user supplied — that is the `proposed*` columns, and
 * a proposal is required to carry [ContributionEntity.proposalSource] so every surface can label it
 * as a proposal. A proposal is not a valuation and must never be read as the agreed value. There is
 * intentionally no code path that promotes a proposal to an agreement without signatories.
 *
 * ## Privacy
 *
 * A contribution is shared trip content. It may name a program's **title and type**
 * ("Marriott Bonvoy", "HOTEL") because that is what the group needs to plan. It may never carry
 * the contributor's balance, tier, masked account number, or the program's estimated cash value —
 * all of which live on [ConnectedAccountEntity] and stay under `/users/{uid}`. The Firestore rules
 * enforce this with a key allowlist, so a buggy client that copies a whole account object into a
 * contribution gets rejected server-side rather than leaking.
 */

/** What kind of thing is being offered. */
enum class ContributionAssetKind(val value: String, val label: String) {
    CASH("CASH", "Cash"),
    POINTS("POINTS", "Points / miles"),
    TIMESHARE_WEEK("TIMESHARE_WEEK", "Timeshare week"),
    CERTIFICATE("CERTIFICATE", "Certificate / voucher"),
    MEMBERSHIP_BENEFIT("MEMBERSHIP_BENEFIT", "Membership benefit"),
    /** Driving, hosting, cooking — value the group recognises that isn't a financial instrument. */
    IN_KIND("IN_KIND", "In kind");

    companion object {
        fun fromStringOrNull(value: String): ContributionAssetKind? =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
    }
}

/** Lifecycle of an offer. */
enum class ContributionState(val value: String) {
    /** Offered; the group has not agreed a value. */
    OFFERED("OFFERED"),
    /** A value has been agreed and signed by the recorded travellers. */
    AGREED("AGREED"),
    /** Actually used on the trip. */
    CONSUMED("CONSUMED"),
    /** Pulled back by the contributor before use. */
    WITHDRAWN("WITHDRAWN");

    companion object {
        fun fromStringOrNull(value: String): ContributionState? =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
    }
}

/**
 * A member's offer toward a trip.
 *
 * Every `agreed*` column defaults to "no agreement". Reading [agreedValueAmount] without checking
 * [hasRecordedAgreement] is a bug: 0.0 there means "the group never agreed anything", not "worth
 * nothing".
 */
@Entity(
    tableName = "contributions",
    indices = [Index("tripId"), Index("contributorTravelerId")]
)
data class ContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    /** Who is offering. Required. */
    val contributorTravelerId: Long,
    /** [ContributionAssetKind.value]. Required. */
    val assetKind: String,
    /** [ContributionState.value]. Required. */
    val state: String,

    // ---- The offer, in native units (fact) ----------------------------------------------------
    /** How much, in the offer's own unit: 120000 points, 1 week, 3 nights. 0.0 = not quantified. */
    val nativeQuantity: Double = 0.0,
    /** The unit those numbers are in: "points", "week", "nights". "" = not stated. */
    val nativeUnitLabel: String = "",
    /**
     * SHAREABLE program title, e.g. "Marriott Bonvoy". Copy it from
     * [ConnectedAccountEntity.providerName] via [toShareableProgramRef] — never copy the whole
     * account. "" = not tied to a program (e.g. plain cash).
     */
    val programTitle: String = "",
    /** SHAREABLE program type, e.g. "HOTEL", "AIRLINE", "TIMESHARE". "" = not stated. */
    val programType: String = "",
    /** Free text from the contributor. "" = none. */
    val description: String = "",
    val offeredAtTimestamp: Long = System.currentTimeMillis(),

    // ---- The agreement, in money (recorded human decision) -------------------------------------
    /** Only meaningful when [hasRecordedAgreement]. 0.0 otherwise, meaning "no agreement". */
    val agreedValueAmount: Double = 0.0,
    /** ISO currency of [agreedValueAmount]. "" = no agreement. */
    val agreedValueCurrency: String = "",
    /** Comma-separated [TravelerEntity.id]s who agreed. "" = nobody. */
    val agreedByTravelerIdsCsv: String = "",
    /** When the group agreed. **0 = no agreement exists.** */
    val agreedAtTimestamp: Long = 0,
    /** How they justified it, in their words. "" = not recorded. */
    val agreementNote: String = "",

    // ---- A proposal (explicitly not an agreement) ----------------------------------------------
    /** A figure Marco or a member suggested. 0.0 = no proposal. */
    val proposedValueAmount: Double = 0.0,
    /** ISO currency of [proposedValueAmount]. "" = no proposal. */
    val proposedValueCurrency: String = "",
    /**
     * Where the proposal came from — required whenever a proposal exists, so the UI can label it.
     * Use a [ContributionProposalSource] value. "" = no proposal.
     */
    val proposalSource: String = "",
    /** 0 = no proposal. */
    val proposedAtTimestamp: Long = 0
)

/** Where a proposed figure came from. A proposal without one of these must not be displayed. */
enum class ContributionProposalSource(val value: String, val label: String) {
    /** Derived from a figure the contributor themselves entered. */
    USER_SUPPLIED("USER_SUPPLIED", "From your own figure"),
    /** Derived from a live supplier quote via the pricing service. */
    SUPPLIER_QUOTE("SUPPLIER_QUOTE", "From a live quote"),
    /** An LLM estimate. Must always render as an estimate, never as a value. */
    MODELED("MODELED", "Marco's estimate"),
    /** Suggested by another member. */
    MEMBER_SUGGESTION("MEMBER_SUGGESTION", "Suggested by a member");

    companion object {
        fun fromStringOrNull(value: String): ContributionProposalSource? =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
    }
}

/**
 * True only when a real agreement was recorded: a timestamp, at least one signatory, and a
 * currency. Anything less is an offer with no agreed value, however tempting the amount column is.
 */
val ContributionEntity.hasRecordedAgreement: Boolean
    get() = agreedAtTimestamp != 0L &&
        agreedValueCurrency.isNotBlank() &&
        agreedByTravelerIdsCsv.split(',').any { it.trim().toLongOrNull() != null }

/** True when a proposal exists *and* is properly labelled. An unlabelled proposal is not displayable. */
val ContributionEntity.hasLabelledProposal: Boolean
    get() = proposedAtTimestamp != 0L &&
        proposedValueCurrency.isNotBlank() &&
        ContributionProposalSource.fromStringOrNull(proposalSource) != null

/** The signatories to the agreement, empty when there is none. */
fun ContributionEntity.agreedByTravelerIds(): List<Long> =
    agreedByTravelerIdsCsv.split(',').mapNotNull { it.trim().toLongOrNull() }

/**
 * The only sanctioned way to attach a monetary value to a contribution.
 *
 * Requires signatories and stamps the time, so an agreement cannot be created by assigning an
 * amount. Throws when handed no signatories — an unsigned agreement is not an agreement, and
 * silently storing one would be exactly the fabrication this model exists to prevent.
 */
fun ContributionEntity.withRecordedAgreement(
    amount: Double,
    currency: String,
    agreedByTravelerIds: List<Long>,
    agreedAtTimestamp: Long = System.currentTimeMillis(),
    note: String = ""
): ContributionEntity {
    require(agreedByTravelerIds.isNotEmpty()) {
        "A contribution agreement needs at least one traveler who agreed to it."
    }
    require(currency.isNotBlank()) { "A contribution agreement needs a currency." }
    return copy(
        state = ContributionState.AGREED.value,
        agreedValueAmount = amount,
        agreedValueCurrency = currency,
        agreedByTravelerIdsCsv = agreedByTravelerIds.joinToString(","),
        agreedAtTimestamp = agreedAtTimestamp,
        agreementNote = note
    )
}

/**
 * The **only** fields of a linked account that may cross into shared trip content: the program's
 * title and its type. Nothing here is a number.
 */
data class ShareableProgramRef(
    val programTitle: String,
    val programType: String
)

/**
 * Narrow a private [ConnectedAccountEntity] down to what may be published to a trip.
 *
 * Use this at every boundary where account data becomes group data. It drops the balance, the
 * valuation, the tier, the exchange power score, and the masked account number by construction, so
 * the safe path is also the shortest one.
 */
fun ConnectedAccountEntity.toShareableProgramRef(): ShareableProgramRef =
    ShareableProgramRef(programTitle = providerName, programType = categoryType)
