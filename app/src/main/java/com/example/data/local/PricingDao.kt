package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PriceQuoteCacheEntity
import kotlinx.coroutines.flow.Flow

/** Local cache of normalised price quotes, so priced surfaces stay readable offline. */
@Dao
interface PricingDao {

    @Query("SELECT * FROM price_quote_cache WHERE requestKey = :requestKey LIMIT 1")
    suspend fun getCachedQuote(requestKey: String): PriceQuoteCacheEntity?

    @Query("SELECT * FROM price_quote_cache ORDER BY cachedAtTimestamp DESC")
    fun getAllCachedQuotes(): Flow<List<PriceQuoteCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCachedQuote(quote: PriceQuoteCacheEntity): Long

    @Query("DELETE FROM price_quote_cache WHERE requestKey = :requestKey")
    suspend fun deleteCachedQuote(requestKey: String)

    @Query("DELETE FROM price_quote_cache")
    suspend fun clearQuoteCache()
}
