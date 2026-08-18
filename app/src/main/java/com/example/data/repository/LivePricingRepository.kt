package com.example.data.repository

import com.example.BuildConfig
import com.example.data.local.PricingDao
import com.example.data.model.PriceQuote
import com.example.data.model.PriceQuoteCacheEntity
import com.example.data.model.QuoteConfidence
import com.example.data.model.QuoteRequest
import com.example.data.model.QuoteResult
import com.example.data.model.toPriceQuoteOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Live [PricingRepository] connecting to the Marco Pricing Service on Railway / local endpoint,
 * backed by [PricingDao] for offline access and auditability.
 *
 * Honors the load-bearing rule: every quote carries its [QuoteConfidence] tier and timestamp.
 * If a quote cannot be obtained, it returns [QuoteResult.Unavailable] — never a fabricated figure.
 *
 * [serviceBaseUrl] is configuration, not a fabricated default: it comes from `BuildConfig`, which
 * the Secrets Gradle Plugin populates from `.env` (falling back to `.env.example`'s
 * `PRICING_SERVICE_BASE_URL`, empty by default). There is deliberately no hardcoded emulator or
 * production address here — an unset URL means quotes are unavailable rather than silently
 * pointed at an address that cannot possibly be reached from a physical device or production.
 * Prefer an `https://` target for anything other than a developer's own local/emulator override.
 */
class LivePricingRepository(
    private val pricingDao: PricingDao,
    private val serviceBaseUrl: String? = BuildConfig.PRICING_SERVICE_BASE_URL.ifBlank { null }
) : PricingRepository {

    override suspend fun quote(request: QuoteRequest): QuoteResult = withContext(Dispatchers.IO) {
        // 1. Check local cache first
        val cached = cachedQuote(request)

        // 2. No pricing service configured — never fabricate a target or an amount.
        val baseUrl = serviceBaseUrl
        if (baseUrl.isNullOrBlank()) {
            return@withContext cached?.let { QuoteResult.Available(it) }
                ?: QuoteResult.Unavailable("No pricing service is configured.")
        }

        // 3. Attempt remote live quote
        try {
            val url = URL("$baseUrl/quote")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 3000
                readTimeout = 3000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val requestJson = JSONObject().apply {
                put("category", request.category)
                put("locality", request.locality)
                put("tier", request.tier)
                put("dateIso", request.dateIso)
                put("partySize", request.partySize)
                put("preferredCurrency", request.preferredCurrency)
            }

            connection.outputStream.use { os ->
                os.write(requestJson.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val status = json.optString("status", "UNAVAILABLE")

                if (status == "AVAILABLE" && json.has("quote")) {
                    val liveQuote = parseTrustworthyQuote(json.getJSONObject("quote"))
                    if (liveQuote != null) {
                        cacheQuote(request, liveQuote)
                        return@withContext QuoteResult.Available(liveQuote)
                    }
                    // The service said AVAILABLE but the payload is missing mandatory provenance
                    // (confidence/currency/amount/fetch time) — falling back rather than inventing it.
                    return@withContext cached?.let { QuoteResult.Available(it) }
                        ?: QuoteResult.Unavailable("Pricing service returned an incomplete quote.")
                } else {
                    val reason = json.optString("reason", "Live quote unavailable for this request")
                    if (cached != null) {
                        return@withContext QuoteResult.Available(cached)
                    }
                    return@withContext QuoteResult.Unavailable(reason)
                }
            }
        } catch (_: Exception) {
            // Network failure or service offline — fall back to cached quote if available
        }

        if (cached != null) {
            QuoteResult.Available(cached)
        } else {
            QuoteResult.Unavailable("Live pricing service offline. No cached quote available.")
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

    /**
     * Parses a `quote` object from the pricing service into a [PriceQuote], treating every
     * provenance field as mandatory rather than papering over a gap with a plausible default:
     *
     * - an absent or unrecognized `confidence` resolves to [QuoteConfidence.UNKNOWN], which
     *   [QuoteConfidence.hasFigure] rejects — an unlabelled figure is not a figure;
     * - an absent or blank `currency` makes the quote unusable, never assumed to be USD;
     * - a missing `amount` is not the same as a `0.0` quote, so it is rejected rather than
     *   rendered as free;
     * - a missing `fetchedAtTimestamp` is not "now" — [PriceQuote.fetchedAtTimestamp] is the
     *   mandatory context that lets a surface show a quote's age, so a service that omits it
     *   gets no timestamp invented on its behalf.
     *
     * Returns null when the payload cannot support an honestly-labelled quote; the caller falls
     * back to the cache (still labelled with its own true age) or [QuoteResult.Unavailable].
     */
    private fun parseTrustworthyQuote(qJson: JSONObject): PriceQuote? {
        val confidence = QuoteConfidence.fromStringOrNull(qJson.optString("confidence", ""))
            ?: QuoteConfidence.UNKNOWN
        if (!confidence.hasFigure) return null

        val currency = qJson.optString("currency", "")
        if (currency.isBlank()) return null

        if (!qJson.has("amount")) return null
        val amount = qJson.optDouble("amount", Double.NaN)
        if (amount.isNaN()) return null

        if (!qJson.has("fetchedAtTimestamp")) return null
        val fetchedAtTimestamp = qJson.optLong("fetchedAtTimestamp", 0L)
        if (fetchedAtTimestamp <= 0L) return null

        return PriceQuote(
            amount = amount,
            currency = currency,
            source = qJson.optString("source", "PRICING_SERVICE"),
            fetchedAtTimestamp = fetchedAtTimestamp,
            confidence = confidence,
            asOfLabel = qJson.optString("asOfLabel", "")
        )
    }
}
