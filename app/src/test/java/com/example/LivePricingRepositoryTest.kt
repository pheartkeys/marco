package com.example

import com.example.data.local.PricingDao
import com.example.data.model.PriceQuote
import com.example.data.model.PriceQuoteCacheEntity
import com.example.data.model.QuoteConfidence
import com.example.data.model.QuoteRequest
import com.example.data.model.QuoteResult
import com.example.data.repository.LivePricingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LivePricingRepositoryTest {

    private class FakePricingDao : PricingDao {
        private val cache = mutableMapOf<String, PriceQuoteCacheEntity>()

        override suspend fun getCachedQuote(requestKey: String): PriceQuoteCacheEntity? =
            cache[requestKey]

        override fun getAllCachedQuotes(): Flow<List<PriceQuoteCacheEntity>> =
            flowOf(cache.values.toList())

        override suspend fun upsertCachedQuote(quote: PriceQuoteCacheEntity): Long {
            cache[quote.requestKey] = quote
            return 1L
        }

        override suspend fun deleteCachedQuote(requestKey: String) {
            cache.remove(requestKey)
        }

        override suspend fun clearQuoteCache() {
            cache.clear()
        }
    }

    private lateinit var dao: FakePricingDao
    private lateinit var repo: LivePricingRepository

    @Before
    fun setUp() {
        dao = FakePricingDao()
        // Point to an invalid host so it exercises the offline fallback path
        repo = LivePricingRepository(dao, serviceBaseUrl = "http://127.0.0.1:9999")
    }

    @Test
    fun `quote returns Unavailable when service is offline and cache is empty`() = runBlocking {
        val request = QuoteRequest(
            category = "LODGING",
            locality = "Tokyo",
            tier = "boutique",
            partySize = 2,
            preferredCurrency = "USD"
        )
        val result = repo.quote(request)
        assertTrue(result is QuoteResult.Unavailable)
    }

    @Test
    fun `quote returns cached Available quote when offline`() = runBlocking {
        val request = QuoteRequest(
            category = "LODGING",
            locality = "Tokyo",
            tier = "boutique",
            partySize = 2,
            preferredCurrency = "USD"
        )
        val quote = PriceQuote(
            amount = 280.0,
            currency = "USD",
            source = "AMADEUS_SELF_SERVICE",
            fetchedAtTimestamp = 1700000000000L,
            confidence = QuoteConfidence.ESTIMATED,
            asOfLabel = "Amadeus benchmark"
        )

        repo.cacheQuote(request, quote)
        val result = repo.quote(request)

        assertTrue(result is QuoteResult.Available)
        val available = (result as QuoteResult.Available).quote
        assertEquals(280.0, available.amount, 0.001)
        assertEquals("USD", available.currency)
        assertEquals(QuoteConfidence.ESTIMATED, available.confidence)
    }
}
