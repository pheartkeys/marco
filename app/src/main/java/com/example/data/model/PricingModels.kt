package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ============================================================================================
 * PRICING — every number the app shows carries its provenance
 * ============================================================================================
 *
 * The app never calls a supplier directly. A pricing service normalises every supplier to one
 * quote shape, and the app consumes it through `PricingRepository`.
 *
 * The load-bearing idea is [QuoteConfidence]. A figure with no confidence tier is not renderable:
 * the difference between "the catalog says $340 as of March" and "a model guessed $340" is the
 * difference between information and fabrication, and the UI must show which one it has.
 * [QuoteConfidence.UNKNOWN] is not a fallback value — it means *render a gap to fill*.
 */

/** How much a quoted figure can be trusted, and how it must be labelled. */
enum class QuoteConfidence(val value: String, val label: String) {
    /** From a curated, versioned catalog. Always shown with its "as of" date. */
    KNOWN("KNOWN", "Known"),
    /** A live supplier quote. Shown with its fetch timestamp. */
    ESTIMATED("ESTIMATED", "Live estimate"),
    /** An LLM estimate used during preliminary planning. Must be explicitly labelled as an estimate. */
    MODELED("MODELED", "Marco's estimate"),
    /** No figure available. Render as a gap the user can fill — never silently substitute a number. */
    UNKNOWN("UNKNOWN", "Not known");

    /** True when a figure may be shown at all. UNKNOWN carries no figure. */
    val hasFigure: Boolean
        get() = this != UNKNOWN

    companion object {
        fun fromStringOrNull(value: String): QuoteConfidence? =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
    }
}

/** What is being priced. Keep supplier-agnostic; the pricing service maps it to a supplier. */
data class QuoteRequest(
    /** e.g. "LODGING", "FLIGHT", "DINING", "TRANSPORT", "ACTIVITY". */
    val category: String,
    /** Free-form locality, "" if not scoped to one. */
    val locality: String = "",
    /** Taxonomy tier within the category, e.g. "boutique", "economy". "" = unspecified. */
    val tier: String = "",
    /** ISO yyyy-MM-dd, "" when the date is not yet known. */
    val dateIso: String = "",
    /** Number of people the quote should cover. 0 = unspecified. */
    val partySize: Int = 0,
    /** Currency the caller wants back. "" = the supplier's own currency. */
    val preferredCurrency: String = ""
)

/**
 * One normalised quote.
 *
 * [amount] is only meaningful when [confidence] `.hasFigure` is true. [fetchedAtTimestamp] is
 * mandatory context, not decoration: an old quote rendered without its age is a lie about currency.
 */
data class PriceQuote(
    val amount: Double,
    val currency: String,
    /** Which supplier or catalog produced it, e.g. "AMADEUS", "GOOGLE_PLACES", "CURATED_CATALOG". */
    val source: String,
    val fetchedAtTimestamp: Long,
    val confidence: QuoteConfidence,
    /** Human-readable "as of" for KNOWN catalog entries, e.g. "March 2026". "" = none. */
    val asOfLabel: String = ""
)

/**
 * The result of asking for a price.
 *
 * Deliberately not `PriceQuote?`: [Unavailable] carries *why*, so a surface can say "we couldn't
 * reach the supplier" rather than showing a blank that looks like zero.
 */
sealed interface QuoteResult {
    data class Available(val quote: PriceQuote) : QuoteResult
    /**
     * No figure. [reason] is for the user, e.g. "No live quote available offline".
     * The caller renders a gap, never a placeholder number.
     */
    data class Unavailable(val reason: String) : QuoteResult
}

/**
 * A cached quote, so the ledger and planning surfaces stay readable offline.
 *
 * Cached rows must always be rendered with their age. This table exists in v10 rather than being
 * added later so the pricing track does not have to bump the database version underneath the other
 * tracks.
 */
@Entity(
    tableName = "price_quote_cache",
    indices = [Index(value = ["requestKey"], unique = true)]
)
data class PriceQuoteCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Stable key derived from the [QuoteRequest]; the uniqueness constraint for the cache. */
    val requestKey: String,
    val category: String = "",
    val locality: String = "",
    val amount: Double,
    val currency: String,
    val source: String,
    /** [QuoteConfidence.value]. */
    val confidence: String,
    val fetchedAtTimestamp: Long,
    val asOfLabel: String = "",
    /** When this row was written locally. */
    val cachedAtTimestamp: Long = System.currentTimeMillis()
)

/** Rehydrate a cached row. Returns null when the stored confidence tier is unrecognised. */
fun PriceQuoteCacheEntity.toPriceQuoteOrNull(): PriceQuote? {
    val tier = QuoteConfidence.fromStringOrNull(confidence) ?: return null
    return PriceQuote(
        amount = amount,
        currency = currency,
        source = source,
        fetchedAtTimestamp = fetchedAtTimestamp,
        confidence = tier,
        asOfLabel = asOfLabel
    )
}
