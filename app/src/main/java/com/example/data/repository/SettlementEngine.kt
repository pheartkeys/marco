package com.example.data.repository

import com.example.data.model.ContributionEntity
import com.example.data.model.LedgerEntryEntity
import com.example.data.model.LedgerModel
import com.example.data.model.SettlementEntity
import com.example.data.model.SplitRuleEntity
import com.example.data.model.TravelerBalance

/**
 * The arithmetic seam. **Phase 0 declares this contract and deliberately does not implement it** —
 * the ledger track writes the implementations, one per [LedgerModel], behind this one interface.
 *
 * Both functions are pure: everything they need is passed in and nothing is written back. That is
 * what makes the four ledger models testable against fixed inputs and keeps balances derived rather
 * than stored.
 *
 * Two rules any implementation must honour, because they are product law rather than arithmetic
 * preference:
 *
 *  - Exclude entries where
 *    [com.example.data.model.countsTowardCashSettlement] is false. Points-funded spending never
 *    enters cash settlement, and it must not be converted into a cash figure to make the maths
 *    tidy.
 *  - Never silently convert an entry that has no usable exchange rate
 *    ([com.example.data.model.hasUsableConversion]). Report it in
 *    [TravelerBalance.unconvertedEntryIds] so the surface can show the gap instead of a number
 *    built on an assumed 1.0 rate.
 */
interface SettlementEngine {

    /** Which ledger model this implementation covers. */
    val ledgerModel: LedgerModel

    /**
     * Per-traveller position for a trip.
     *
     * @param travelerIds everyone on the trip, so a member who paid nothing still appears with a
     *        zero position rather than vanishing from the list.
     * @param normalizedCurrency the currency all returned figures are in.
     */
    fun balances(
        entries: List<LedgerEntryEntity>,
        splitRules: List<SplitRuleEntity>,
        contributions: List<ContributionEntity>,
        travelerIds: List<Long>,
        normalizedCurrency: String
    ): List<TravelerBalance>

    /**
     * A transfer-minimising set of payments that clears [balances].
     *
     * Returned rows are PROPOSED and disposable: the group may replace them, and only rows the
     * group marks PAID are historical fact. Returns an empty list when
     * [LedgerModel.producesSettlement] is false for this implementation.
     */
    fun proposeTransfers(
        tripId: Long,
        balances: List<TravelerBalance>,
        normalizedCurrency: String
    ): List<SettlementEntity>
}
