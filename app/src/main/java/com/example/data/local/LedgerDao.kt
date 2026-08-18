package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ContributionEntity
import com.example.data.model.LedgerEntryEntity
import com.example.data.model.SettlementEntity
import com.example.data.model.SplitRuleEntity
import com.example.data.model.TripLedgerConfigEntity
import kotlinx.coroutines.flow.Flow

/**
 * Contributions, ledger entries, split rules, settlements, and the per-trip ledger config.
 *
 * There is no balances table and no balances query: per-traveller balances are derived by
 * `LedgerRepository` from entries and split rules. See `LedgerModels.kt` for why.
 */
@Dao
interface LedgerDao {

    // ---- Ledger config -----------------------------------------------------------------------

    @Query("SELECT * FROM trip_ledger_configs WHERE tripId = :tripId LIMIT 1")
    fun getLedgerConfig(tripId: Long): Flow<TripLedgerConfigEntity?>

    @Query("SELECT * FROM trip_ledger_configs WHERE tripId = :tripId LIMIT 1")
    suspend fun getLedgerConfigSync(tripId: Long): TripLedgerConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerConfig(config: TripLedgerConfigEntity): Long

    @Update
    suspend fun updateLedgerConfig(config: TripLedgerConfigEntity)

    // ---- Contributions -----------------------------------------------------------------------

    @Query("SELECT * FROM contributions WHERE tripId = :tripId ORDER BY id ASC")
    fun getContributionsForTrip(tripId: Long): Flow<List<ContributionEntity>>

    @Query("SELECT * FROM contributions WHERE tripId = :tripId ORDER BY id ASC")
    suspend fun getContributionsForTripSync(tripId: Long): List<ContributionEntity>

    @Query("SELECT * FROM contributions WHERE id = :contributionId LIMIT 1")
    suspend fun getContributionByIdSync(contributionId: Long): ContributionEntity?

    @Query("SELECT * FROM contributions WHERE tripId = :tripId AND contributorTravelerId = :travelerId ORDER BY id ASC")
    fun getContributionsByTraveler(tripId: Long, travelerId: Long): Flow<List<ContributionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContribution(contribution: ContributionEntity): Long

    @Update
    suspend fun updateContribution(contribution: ContributionEntity)

    @Query("DELETE FROM contributions WHERE id = :contributionId")
    suspend fun deleteContribution(contributionId: Long)

    // ---- Ledger entries ----------------------------------------------------------------------

    @Query("SELECT * FROM ledger_entries WHERE tripId = :tripId ORDER BY incurredOnIso DESC, id DESC")
    fun getLedgerEntriesForTrip(tripId: Long): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM ledger_entries WHERE tripId = :tripId ORDER BY incurredOnIso DESC, id DESC")
    suspend fun getLedgerEntriesForTripSync(tripId: Long): List<LedgerEntryEntity>

    @Query("SELECT * FROM ledger_entries WHERE id = :entryId LIMIT 1")
    suspend fun getLedgerEntryByIdSync(entryId: Long): LedgerEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntry(entry: LedgerEntryEntity): Long

    @Update
    suspend fun updateLedgerEntry(entry: LedgerEntryEntity)

    @Query("DELETE FROM ledger_entries WHERE id = :entryId")
    suspend fun deleteLedgerEntry(entryId: Long)

    // ---- Split rules -------------------------------------------------------------------------

    @Query("SELECT * FROM split_rules WHERE tripId = :tripId ORDER BY id ASC")
    fun getSplitRulesForTrip(tripId: Long): Flow<List<SplitRuleEntity>>

    @Query("SELECT * FROM split_rules WHERE tripId = :tripId ORDER BY id ASC")
    suspend fun getSplitRulesForTripSync(tripId: Long): List<SplitRuleEntity>

    @Query("SELECT * FROM split_rules WHERE id = :splitRuleId LIMIT 1")
    suspend fun getSplitRuleByIdSync(splitRuleId: Long): SplitRuleEntity?

    /** The trip-level default rule, if one has been recorded. */
    @Query("SELECT * FROM split_rules WHERE tripId = :tripId AND ledgerEntryId = 0 ORDER BY id DESC LIMIT 1")
    suspend fun getDefaultSplitRule(tripId: Long): SplitRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplitRule(rule: SplitRuleEntity): Long

    @Update
    suspend fun updateSplitRule(rule: SplitRuleEntity)

    @Query("DELETE FROM split_rules WHERE id = :splitRuleId")
    suspend fun deleteSplitRule(splitRuleId: Long)

    // ---- Settlements -------------------------------------------------------------------------

    @Query("SELECT * FROM settlements WHERE tripId = :tripId ORDER BY id ASC")
    fun getSettlementsForTrip(tripId: Long): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE tripId = :tripId ORDER BY id ASC")
    suspend fun getSettlementsForTripSync(tripId: Long): List<SettlementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlements(settlements: List<SettlementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: SettlementEntity): Long

    @Update
    suspend fun updateSettlement(settlement: SettlementEntity)

    /**
     * Drop superseded proposals before writing a fresh set. PAID and AGREED rows are historical
     * fact and are never cleared by a recompute.
     */
    @Query("DELETE FROM settlements WHERE tripId = :tripId AND state = 'PROPOSED'")
    suspend fun clearProposedSettlements(tripId: Long)

    // ---- Wipe --------------------------------------------------------------------------------

    @Query("DELETE FROM contributions")
    suspend fun clearContributions()

    @Query("DELETE FROM ledger_entries")
    suspend fun clearLedgerEntries()

    @Query("DELETE FROM split_rules")
    suspend fun clearSplitRules()

    @Query("DELETE FROM settlements")
    suspend fun clearSettlements()

    @Query("DELETE FROM trip_ledger_configs")
    suspend fun clearLedgerConfigs()
}
