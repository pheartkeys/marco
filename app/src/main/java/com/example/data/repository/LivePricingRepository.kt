package com.example.data.repository

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
 */
class LivePricingRepository(
    private val pricingDao: PricingDao,
    private val serviceBaseUrl: String = "http://10.0.2.2:8085" // Default Android emulator host loopback
) : PricingRepository {

    override suspend fun quote(request: QuoteRequest): QuoteResult = withContext(Dispatchers.IO) {
        // 1. Check local cache first
        val cached = cachedQuote(request)

        // 2. Attempt remote live quote
        try {
            val url = URL("$serviceBaseUrl/quote")
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
                    val qJson = json.getJSONObject("quote")
                    val confidenceStr = qJson.optString("confidence", "ESTIMATED")
                    val confidence = QuoteConfidence.fromStringOrNull(confidenceStr) ?: QuoteConfidence.ESTIMATED

                    val liveQuote = PriceQuote(
                        amount = qJson.optDouble("amount", 0.0),
                        currency = qJson.optString("currency", request.preferredCurrency.ifBlank { "USD" }),
                        source = qJson.optString("source", "PRICING_SERVICE"),
                        fetchedAtTimestamp = qJson.optLong("fetchedAtTimestamp", System.currentTimeMillis()),
                        confidence = confidence,
                        asOfLabel = qJson.optString("asOfLabel", "")
                    )

                    // Cache live quote
                    cacheQuote(request, liveQuote)
                    return@withContext QuoteResult.Available(liveQuote)
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
}
