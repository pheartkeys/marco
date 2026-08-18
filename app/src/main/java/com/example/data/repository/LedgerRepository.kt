package com.example.data.repository

import com.example.data.local.LedgerDao
import com.example.data.model.ContributionEntity
import com.example.data.model.LedgerEntryEntity
import com.example.data.model.SettlementEntity
import com.example.data.model.SplitRuleEntity
import com.example.data.model.TripLedgerConfigEntity
import com.example.data.model.withRecordedAgreement
import kotlinx.coroutines.flow.Flow

/**
 * Contributions, expenses, split rules, settlements, and the per-trip ledger configuration.
 *
 * **This interface stores money; it does not compute it.** Balances and transfer minimisation live
 * behind [SettlementEngine], which the ledger track implements. Keeping the two apart means the
 * four ledger models can differ in arithmetic without four different persistence layers, and it
 * keeps a stale stored balance from ever becoming a second source of truth.
 *
 * The one invariant enforced here is the contribution agreement: [recordContributionAgreement] is
 * the only way through this interface to attach a monetary value to a contribution, and it demands
 * signatories. Writing a bare [ContributionEntity] with `agreedValueAmount` set and no signatories
 * produces a row that [com.example.data.model.hasRecordedAgreement] reports as having no
 * agreement, which is what every reader must check.
 */
interface LedgerRepository {

    // ---- Ledger configuration ------------------------------------------------------------------

    fun observeLedgerConfig(tripId: Long): Flow<TripLedgerConfigEntity?>

    suspend fun getLedgerConfig(tripId: Long): TripLedgerConfigEntity?

    /**
     * Store the trip's ledger model. A config with `confirmedAtTimestamp == 0` is a proposal the
     * group has not accepted yet, and surfaces should still be asking.
     */
    suspend fun upsertLedgerConfig(config: TripLedgerConfigEntity): Long

    suspend fun confirmLedgerConfig(
        tripId: Long,
        confirmedByTravelerId: Long,
        timestamp: Long = System.currentTimeMillis()
    )

    // ---- Contributions ---------------------------------------------------------------------------

    fun observeContributions(tripId: Long): Flow<List<ContributionEntity>>

    fun observeContributionsByTraveler(tripId: Long, travelerId: Long): Flow<List<ContributionEntity>>

    suspend fun getContributions(tripId: Long): List<ContributionEntity>

    suspend fun getContribution(contributionId: Long): ContributionEntity?

    /**
     * Record an offer in its native units. Do not populate the `agreed*` fields here — use
     * [recordContributionAgreement].
     */
    suspend fun upsertContribution(contribution: ContributionEntity): Long

    /**
     * Record the group's decision about what a contribution is worth.
     *
     * @param agreedByTravelerIds who agreed. Must not be empty — an agreement with no signatories
     *        is not an agreement, and this call throws rather than storing one.
     * @throws IllegalArgumentException when there are no signatories or no currency.
     */
    suspend fun recordContributionAgreement(
        contributionId: Long,
        amount: Double,
        currency: String,
        agreedByTravelerIds: List<Long>,
        note: String = "",
        timestamp: Long = System.currentTimeMillis()
    )

    suspend fun deleteContribution(contributionId: Long)

    // ---- Ledger entries --------------------------------------------------------------------------

    fun observeLedgerEntries(tripId: Long): Flow<List<LedgerEntryEntity>>

    suspend fun getLedgerEntries(tripId: Long): List<LedgerEntryEntity>

    suspend fun getLedgerEntry(entryId: Long): LedgerEntryEntity?

    suspend fun upsertLedgerEntry(entry: LedgerEntryEntity): Long

    suspend fun deleteLedgerEntry(entryId: Long)

    // ---- Split rules -----------------------------------------------------------------------------

    fun observeSplitRules(tripId: Long): Flow<List<SplitRuleEntity>>

    suspend fun getSplitRules(tripId: Long): List<SplitRuleEntity>

    suspend fun getSplitRule(splitRuleId: Long): SplitRuleEntity?

    /** The trip-level default rule (the one with `ledgerEntryId == 0`), or null if none recorded. */
    suspend fun getDefaultSplitRule(tripId: Long): SplitRuleEntity?

    suspend fun upsertSplitRule(rule: SplitRuleEntity): Long

    suspend fun deleteSplitRule(splitRuleId: Long)

    // ---- Settlements -----------------------------------------------------------------------------

    fun observeSettlements(tripId: Long): Flow<List<SettlementEntity>>

    suspend fun getSettlements(tripId: Long): List<SettlementEntity>

    /**
     * Replace the outstanding PROPOSED transfers with a freshly computed set. AGREED and PAID rows
     * are historical fact and are left untouched.
     */
    suspend fun replaceProposedSettlements(tripId: Long, settlements: List<SettlementEntity>)

    suspend fun upsertSettlement(settlement: SettlementEntity): Long

    suspend fun updateSettlement(settlement: SettlementEntity)
}

/** Room-backed [LedgerRepository]. Persistence only — no arithmetic beyond what a column requires. */
class RoomLedgerRepository(private val ledgerDao: LedgerDao) : LedgerRepository {

    override fun observeLedgerConfig(tripId: Long): Flow<TripLedgerConfigEntity?> =
        ledgerDao.getLedgerConfig(tripId)

    override suspend fun getLedgerConfig(tripId: Long): TripLedgerConfigEntity? =
        ledgerDao.getLedgerConfigSync(tripId)

    override suspend fun upsertLedgerConfig(config: TripLedgerConfigEntity): Long =
        ledgerDao.insertLedgerConfig(config)

    override suspend fun confirmLedgerConfig(
        tripId: Long,
        confirmedByTravelerId: Long,
        timestamp: Long
    ) {
        val config = ledgerDao.getLedgerConfigSync(tripId) ?: return
        ledgerDao.updateLedgerConfig(
            config.copy(
                confirmedAtTimestamp = timestamp,
                confirmedByTravelerId = confirmedByTravelerId
            )
        )
    }

    override fun observeContributions(tripId: Long): Flow<List<ContributionEntity>> =
        ledgerDao.getContributionsForTrip(tripId)

    override fun observeContributionsByTraveler(
        tripId: Long,
        travelerId: Long
    ): Flow<List<ContributionEntity>> = ledgerDao.getContributionsByTraveler(tripId, travelerId)

    override suspend fun getContributions(tripId: Long): List<ContributionEntity> =
        ledgerDao.getContributionsForTripSync(tripId)

    override suspend fun getContribution(contributionId: Long): ContributionEntity? =
        ledgerDao.getContributionByIdSync(contributionId)

    override suspend fun upsertContribution(contribution: ContributionEntity): Long =
        ledgerDao.insertContribution(contribution)

    override suspend fun recordContributionAgreement(
        contributionId: Long,
        amount: Double,
        currency: String,
        agreedByTravelerIds: List<Long>,
        note: String,
        timestamp: Long
    ) {
        val contribution = ledgerDao.getContributionByIdSync(contributionId) ?: return
        // withRecordedAgreement() rejects an unsigned or currency-less agreement outright.
        ledgerDao.updateContribution(
            contribution.withRecordedAgreement(
                amount = amount,
                currency = currency,
                agreedByTravelerIds = agreedByTravelerIds,
                agreedAtTimestamp = timestamp,
                note = note
            )
        )
    }

    override suspend fun deleteContribution(contributionId: Long) =
        ledgerDao.deleteContribution(contributionId)

    override fun observeLedgerEntries(tripId: Long): Flow<List<LedgerEntryEntity>> =
        ledgerDao.getLedgerEntriesForTrip(tripId)

    override suspend fun getLedgerEntries(tripId: Long): List<LedgerEntryEntity> =
        ledgerDao.getLedgerEntriesForTripSync(tripId)

    override suspend fun getLedgerEntry(entryId: Long): LedgerEntryEntity? =
        ledgerDao.getLedgerEntryByIdSync(entryId)

    override suspend fun upsertLedgerEntry(entry: LedgerEntryEntity): Long =
        ledgerDao.insertLedgerEntry(entry)

    override suspend fun deleteLedgerEntry(entryId: Long) = ledgerDao.deleteLedgerEntry(entryId)

    override fun observeSplitRules(tripId: Long): Flow<List<SplitRuleEntity>> =
        ledgerDao.getSplitRulesForTrip(tripId)

    override suspend fun getSplitRules(tripId: Long): List<SplitRuleEntity> =
        ledgerDao.getSplitRulesForTripSync(tripId)

    override suspend fun getSplitRule(splitRuleId: Long): SplitRuleEntity? =
        ledgerDao.getSplitRuleByIdSync(splitRuleId)

    override suspend fun getDefaultSplitRule(tripId: Long): SplitRuleEntity? =
        ledgerDao.getDefaultSplitRule(tripId)

    override suspend fun upsertSplitRule(rule: SplitRuleEntity): Long =
        ledgerDao.insertSplitRule(rule)

    override suspend fun deleteSplitRule(splitRuleId: Long) = ledgerDao.deleteSplitRule(splitRuleId)

    override fun observeSettlements(tripId: Long): Flow<List<SettlementEntity>> =
        ledgerDao.getSettlementsForTrip(tripId)

    override suspend fun getSettlements(tripId: Long): List<SettlementEntity> =
        ledgerDao.getSettlementsForTripSync(tripId)

    override suspend fun replaceProposedSettlements(
        tripId: Long,
        settlements: List<SettlementEntity>
    ) {
        ledgerDao.clearProposedSettlements(tripId)
        if (settlements.isNotEmpty()) ledgerDao.insertSettlements(settlements)
    }

    override suspend fun upsertSettlement(settlement: SettlementEntity): Long =
        ledgerDao.insertSettlement(settlement)

    override suspend fun updateSettlement(settlement: SettlementEntity) =
        ledgerDao.updateSettlement(settlement)
}
