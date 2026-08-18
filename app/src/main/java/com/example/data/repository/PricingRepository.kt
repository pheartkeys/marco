package com.example.data.repository

import com.example.data.local.PricingDao
import com.example.data.model.PriceQuote
import com.example.data.model.PriceQuoteCacheEntity
import com.example.data.model.QuoteRequest
import com.example.data.model.QuoteResult
import java.util.Locale
import com.example.data.model.toPriceQuoteOrNull

/**
 * The app's only door to pricing. Suppliers are never called from app code — the pricing service
 * normalises them and this interface exposes the result.
 *
 * Phase 0 ships the contract and the cache. The live implementation belongs to the pricing track;
 * until it lands, [CachedOnlyPricingRepository] answers from the local cache and returns
 * [QuoteResult.Unavailable] for anything it has not seen. That is the correct behaviour, not a
 * stub: a missing quote must render as a gap the user can fill, never as a plausible number.
 */
interface PricingRepository {

    /**
     * A quote for one request. Returns [QuoteResult.Unavailable] with a user-safe reason when no
     * figure can be produced — callers must handle it rather than substituting a default.
     */
    suspend fun quote(request: QuoteRequest): QuoteResult

    /** Batch form. The result list is index-aligned with [requests]. */
    suspend fun quotes(requests: List<QuoteRequest>): List<QuoteResult>

    /** The most recent cached answer for a request, ignoring the network. Null when never cached. */
    suspend fun cachedQuote(request: QuoteRequest): PriceQuote?

    /** Store a fetched quote so the surface stays readable offline. */
    suspend fun cacheQuote(request: QuoteRequest, quote: PriceQuote)

    /**
     * Stable cache key for a request. Implementations must agree on this, so it is defined once.
     * Case folding is pinned to [Locale.US]: a cache key that changes with the device locale would
     * silently miss (and in Turkish locales, mangle) every lookup.
     */
    fun cacheKey(request: QuoteRequest): String = buildString {
        append(request.category.trim().uppercase(Locale.US))
        append('|').append(request.locality.trim().lowercase(Locale.US))
        append('|').append(request.tier.trim().lowercase(Locale.US))
        append('|').append(request.dateIso.trim())
        append('|').append(request.partySize)
        append('|').append(request.preferredCurrency.trim().uppercase(Locale.US))
    }
}

/**
 * Cache-only [PricingRepository] — the honest state of the world before a pricing service exists.
 *
 * Answers from the local cache when it has a row, and otherwise says so. It never estimates, and
 * it never falls back to a hardcoded figure.
 */
class CachedOnlyPricingRepository(private val pricingDao: PricingDao) : PricingRepository {

    override suspend fun quote(request: QuoteRequest): QuoteResult {
        val cached = cachedQuote(request)
        return if (cached != null) {
            QuoteResult.Available(cached)
        } else {
            QuoteResult.Unavailable("No pricing available yet for this.")
        }
    }

    override suspend fun quotes(requests: List<QuoteRequest>): List<QuoteResult> =
        requests.map { quote(it) }

    override suspend fun cachedQuote(request: QuoteRequest): PriceQuote? =
        pricingDao.getCachedQuote(cacheKey(request))?.toPriceQuoteOrNull()

    override suspend fun cacheQuote(request: QuoteRequest, quote: PriceQuote) {
        pricingDao.upsertCachedQuote(
            PriceQuoteCacheEntity(
                requestKey = cacheKey(request),
                category = request.category,
                locality = request.locality,
                amount = quote.amount,
                currency = quote.currency,
                source = quote.source,
                confidence = quote.confidence.value,
                fetchedAtTimestamp = quote.fetchedAtTimestamp,
                asOfLabel = quote.asOfLabel
            )
        )
    }
}
