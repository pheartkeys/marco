package com.example.feature.ledger.engine

import com.example.data.model.ContributionEntity
import com.example.data.model.LedgerEntryEntity
import com.example.data.model.LedgerModel
import com.example.data.model.SettlementEntity
import com.example.data.model.SettlementState
import com.example.data.model.SplitAllocations
import com.example.data.model.SplitRuleEntity
import com.example.data.model.SplitRuleType
import com.example.data.model.TravelerBalance
import com.example.data.model.countsTowardCashSettlement
import com.example.data.model.hasRecordedAgreement
import com.example.data.model.hasUsableConversion
import com.example.data.repository.SettlementEngine
import kotlin.math.abs
import kotlin.math.min

object SettlementEngines {
    fun forModel(model: LedgerModel): SettlementEngine = when (model) {
        LedgerModel.SHARED_POT -> SharedPotSettlementEngine
        LedgerModel.SPLIT_SETTLE -> SplitSettleSettlementEngine
        LedgerModel.PERSONAL -> PersonalSettlementEngine
        LedgerModel.CORPORATE_POLICY -> CorporatePolicySettlementEngine
    }
}

/**
 * 1. SHARED POT: Everyone contributes to a common pool up front, expenses draw down the pool.
 */
object SharedPotSettlementEngine : SettlementEngine {
    override val ledgerModel: LedgerModel = LedgerModel.SHARED_POT

    override fun balances(
        entries: List<LedgerEntryEntity>,
        splitRules: List<SplitRuleEntity>,
        contributions: List<ContributionEntity>,
        travelerIds: List<Long>,
        normalizedCurrency: String
    ): List<TravelerBalance> {
        val totalPaidByTraveler = mutableMapOf<Long, Double>()
        val totalPointsByTraveler = mutableMapOf<Long, Double>()
        val unconvertedEntriesByTraveler = mutableMapOf<Long, MutableList<Long>>()

        travelerIds.forEach { tid ->
            totalPaidByTraveler[tid] = 0.0
            totalPointsByTraveler[tid] = 0.0
            unconvertedEntriesByTraveler[tid] = mutableListOf()
        }

        // Contributions credited to pot
        contributions.forEach { contrib ->
            val tid = contrib.contributorTravelerId
            if (contrib.hasRecordedAgreement && contrib.agreedValueCurrency.equals(normalizedCurrency, ignoreCase = true)) {
                totalPaidByTraveler[tid] = (totalPaidByTraveler[tid] ?: 0.0) + contrib.agreedValueAmount
            }
            if (contrib.assetKind.equals("POINTS", ignoreCase = true)) {
                totalPointsByTraveler[tid] = (totalPointsByTraveler[tid] ?: 0.0) + contrib.nativeQuantity
            }
        }

        // Cash expenses paid by individuals into the pot
        var totalGroupExpense = 0.0
        entries.forEach { entry ->
            val tid = entry.payerTravelerId
            if (entry.fundedWithPoints) {
                totalPointsByTraveler[tid] = (totalPointsByTraveler[tid] ?: 0.0) + entry.pointsQuantity
            } else if (entry.countsTowardCashSettlement()) {
                if (entry.hasUsableConversion() || entry.originalCurrency.equals(normalizedCurrency, ignoreCase = true)) {
                    val amountInNorm = if (entry.originalCurrency.equals(normalizedCurrency, ignoreCase = true)) {
                        entry.amountOriginal
                    } else {
                        entry.amountNormalized
                    }
                    totalPaidByTraveler[tid] = (totalPaidByTraveler[tid] ?: 0.0) + amountInNorm
                    totalGroupExpense += amountInNorm
                } else {
                    unconvertedEntriesByTraveler.getOrPut(tid) { mutableListOf() }.add(entry.id)
                }
            }
        }

        // In shared pot, the total group spending is split equally across travelers as the expected share
        val perPersonOwed = if (travelerIds.isNotEmpty()) totalGroupExpense / travelerIds.size else 0.0

        return travelerIds.map { tid ->
            val paid = totalPaidByTraveler[tid] ?: 0.0
            val owed = perPersonOwed
            val net = paid - owed
            val points = totalPointsByTraveler[tid] ?: 0.0
            val unconverted = unconvertedEntriesByTraveler[tid] ?: emptyList()
            TravelerBalance(
                travelerId = tid,
                currency = normalizedCurrency,
                paid = paid,
                owed = owed,
                net = net,
                pointsContributedQuantity = points,
                unconvertedEntryIds = unconverted
            )
        }
    }

    override fun proposeTransfers(
        tripId: Long,
        balances: List<TravelerBalance>,
        normalizedCurrency: String
    ): List<SettlementEntity> = minimizeTransfers(tripId, balances, normalizedCurrency)
}

/**
 * 2. SPLIT & SETTLE: Members pay as they go; expenses are split according to split rules.
 */
object SplitSettleSettlementEngine : SettlementEngine {
    override val ledgerModel: LedgerModel = LedgerModel.SPLIT_SETTLE

    override fun balances(
        entries: List<LedgerEntryEntity>,
        splitRules: List<SplitRuleEntity>,
        contributions: List<ContributionEntity>,
        travelerIds: List<Long>,
        normalizedCurrency: String
    ): List<TravelerBalance> {
        val paidMap = mutableMapOf<Long, Double>()
        val owedMap = mutableMapOf<Long, Double>()
        val pointsMap = mutableMapOf<Long, Double>()
        val unconvertedMap = mutableMapOf<Long, MutableList<Long>>()

        travelerIds.forEach { tid ->
            paidMap[tid] = 0.0
            owedMap[tid] = 0.0
            pointsMap[tid] = 0.0
            unconvertedMap[tid] = mutableListOf()
        }

        val rulesById = splitRules.associateBy { it.id }
        val defaultRule = splitRules.firstOrNull { it.ledgerEntryId == 0L }

        entries.forEach { entry ->
            val payerId = entry.payerTravelerId

            // Points-funded entries are excluded from cash math
            if (entry.fundedWithPoints) {
                pointsMap[payerId] = (pointsMap[payerId] ?: 0.0) + entry.pointsQuantity
                return@forEach
            }

            if (!entry.countsTowardCashSettlement()) return@forEach

            // Check currency conversion
            val isDirectMatch = entry.originalCurrency.equals(normalizedCurrency, ignoreCase = true)
            if (!isDirectMatch && !entry.hasUsableConversion()) {
                unconvertedMap.getOrPut(payerId) { mutableListOf() }.add(entry.id)
                return@forEach
            }

            val amount = if (isDirectMatch) entry.amountOriginal else entry.amountNormalized
            paidMap[payerId] = (paidMap[payerId] ?: 0.0) + amount

            // Determine split rule
            val rule = (if (entry.splitRuleId != 0L) rulesById[entry.splitRuleId] else null) ?: defaultRule
            val ruleType = rule?.let { SplitRuleType.fromStringOrNull(it.ruleType) } ?: SplitRuleType.EQUAL
            val allocations = rule?.let { SplitAllocations.decode(it.allocationsJson) } ?: emptyMap()

            when (ruleType) {
                SplitRuleType.EQUAL -> {
                    val participants = if (rule?.participantTravelerIdsCsv?.isNotBlank() == true) {
                        rule.participantTravelerIdsCsv.split(',').mapNotNull { it.trim().toLongOrNull() }.filter { it in travelerIds }
                    } else {
                        travelerIds
                    }
                    val share = if (participants.isNotEmpty()) amount / participants.size else 0.0
                    participants.forEach { pid ->
                        owedMap[pid] = (owedMap[pid] ?: 0.0) + share
                    }
                }
                SplitRuleType.SHARES -> {
                    val totalShares = allocations.values.sum()
                    if (totalShares > 0.0) {
                        allocations.forEach { (tid, shares) ->
                            if (tid in travelerIds) {
                                val shareAmount = amount * (shares / totalShares)
                                owedMap[tid] = (owedMap[tid] ?: 0.0) + shareAmount
                            }
                        }
                    } else {
                        val share = if (travelerIds.isNotEmpty()) amount / travelerIds.size else 0.0
                        travelerIds.forEach { tid -> owedMap[tid] = (owedMap[tid] ?: 0.0) + share }
                    }
                }
                SplitRuleType.EXACT -> {
                    allocations.forEach { (tid, exactAmount) ->
                        if (tid in travelerIds) {
                            owedMap[tid] = (owedMap[tid] ?: 0.0) + exactAmount
                        }
                    }
                }
                SplitRuleType.PERCENTAGE -> {
                    allocations.forEach { (tid, pct) ->
                        if (tid in travelerIds) {
                            val pctAmount = amount * (pct / 100.0)
                            owedMap[tid] = (owedMap[tid] ?: 0.0) + pctAmount
                        }
                    }
                }
            }
        }

        // Incorporate agreed contribution amounts
        contributions.forEach { contrib ->
            val tid = contrib.contributorTravelerId
            if (contrib.hasRecordedAgreement && contrib.agreedValueCurrency.equals(normalizedCurrency, ignoreCase = true)) {
                paidMap[tid] = (paidMap[tid] ?: 0.0) + contrib.agreedValueAmount
            }
            if (contrib.assetKind.equals("POINTS", ignoreCase = true)) {
                pointsMap[tid] = (pointsMap[tid] ?: 0.0) + contrib.nativeQuantity
            }
        }

        return travelerIds.map { tid ->
            val paid = paidMap[tid] ?: 0.0
            val owed = owedMap[tid] ?: 0.0
            TravelerBalance(
                travelerId = tid,
                currency = normalizedCurrency,
                paid = paid,
                owed = owed,
                net = paid - owed,
                pointsContributedQuantity = pointsMap[tid] ?: 0.0,
                unconvertedEntryIds = unconvertedMap[tid] ?: emptyList()
            )
        }
    }

    override fun proposeTransfers(
        tripId: Long,
        balances: List<TravelerBalance>,
        normalizedCurrency: String
    ): List<SettlementEntity> = minimizeTransfers(tripId, balances, normalizedCurrency)
}

/**
 * 3. PERSONAL: Each person tracks their own expenses. No settlement is produced.
 */
object PersonalSettlementEngine : SettlementEngine {
    override val ledgerModel: LedgerModel = LedgerModel.PERSONAL

    override fun balances(
        entries: List<LedgerEntryEntity>,
        splitRules: List<SplitRuleEntity>,
        contributions: List<ContributionEntity>,
        travelerIds: List<Long>,
        normalizedCurrency: String
    ): List<TravelerBalance> {
        val paidMap = mutableMapOf<Long, Double>()
        val unconvertedMap = mutableMapOf<Long, MutableList<Long>>()

        travelerIds.forEach { tid ->
            paidMap[tid] = 0.0
            unconvertedMap[tid] = mutableListOf()
        }

        entries.forEach { entry ->
            val tid = entry.payerTravelerId
            if (entry.countsTowardCashSettlement()) {
                if (entry.hasUsableConversion() || entry.originalCurrency.equals(normalizedCurrency, ignoreCase = true)) {
                    val amount = if (entry.originalCurrency.equals(normalizedCurrency, ignoreCase = true)) {
                        entry.amountOriginal
                    } else {
                        entry.amountNormalized
                    }
                    paidMap[tid] = (paidMap[tid] ?: 0.0) + amount
                } else {
                    unconvertedMap.getOrPut(tid) { mutableListOf() }.add(entry.id)
                }
            }
        }

        return travelerIds.map { tid ->
            val paid = paidMap[tid] ?: 0.0
            TravelerBalance(
                travelerId = tid,
                currency = normalizedCurrency,
                paid = paid,
                owed = paid, // Self-contained
                net = 0.0,
                unconvertedEntryIds = unconvertedMap[tid] ?: emptyList()
            )
        }
    }

    override fun proposeTransfers(
        tripId: Long,
        balances: List<TravelerBalance>,
        normalizedCurrency: String
    ): List<SettlementEntity> = emptyList() // Produces no settlement transfers
}

/**
 * 4. CORPORATE POLICY: Company-funded with daily policy ceiling.
 */
object CorporatePolicySettlementEngine : SettlementEngine {
    override val ledgerModel: LedgerModel = LedgerModel.CORPORATE_POLICY

    override fun balances(
        entries: List<LedgerEntryEntity>,
        splitRules: List<SplitRuleEntity>,
        contributions: List<ContributionEntity>,
        travelerIds: List<Long>,
        normalizedCurrency: String
    ): List<TravelerBalance> {
        val paidMap = mutableMapOf<Long, Double>()
        val unconvertedMap = mutableMapOf<Long, MutableList<Long>>()

        travelerIds.forEach { tid ->
            paidMap[tid] = 0.0
            unconvertedMap[tid] = mutableListOf()
        }

        entries.forEach { entry ->
            val tid = entry.payerTravelerId
            if (entry.countsTowardCashSettlement()) {
                if (entry.hasUsableConversion() || entry.originalCurrency.equals(normalizedCurrency, ignoreCase = true)) {
                    val amount = if (entry.originalCurrency.equals(normalizedCurrency, ignoreCase = true)) {
                        entry.amountOriginal
                    } else {
                        entry.amountNormalized
                    }
                    paidMap[tid] = (paidMap[tid] ?: 0.0) + amount
                } else {
                    unconvertedMap.getOrPut(tid) { mutableListOf() }.add(entry.id)
                }
            }
        }

        return travelerIds.map { tid ->
            val paid = paidMap[tid] ?: 0.0
            TravelerBalance(
                travelerId = tid,
                currency = normalizedCurrency,
                paid = paid,
                owed = 0.0, // Reimbursable by company
                net = paid,
                unconvertedEntryIds = unconvertedMap[tid] ?: emptyList()
            )
        }
    }

    override fun proposeTransfers(
        tripId: Long,
        balances: List<TravelerBalance>,
        normalizedCurrency: String
    ): List<SettlementEntity> = emptyList() // Company handles reimbursement, no inter-member transfers
}

/**
 * Transfer minimization helper using greedy net settlement matching.
 */
private fun minimizeTransfers(
    tripId: Long,
    balances: List<TravelerBalance>,
    normalizedCurrency: String
): List<SettlementEntity> {
    data class Debtor(val travelerId: Long, var amount: Double)
    data class Creditor(val travelerId: Long, var amount: Double)

    val debtors = mutableListOf<Debtor>()
    val creditors = mutableListOf<Creditor>()

    balances.forEach { b ->
        val rounded = Math.round(b.net * 100.0) / 100.0
        if (rounded < -0.01) {
            debtors.add(Debtor(b.travelerId, abs(rounded)))
        } else if (rounded > 0.01) {
            creditors.add(Creditor(b.travelerId, rounded))
        }
    }

    debtors.sortByDescending { it.amount }
    creditors.sortByDescending { it.amount }

    val result = mutableListOf<SettlementEntity>()
    var dIdx = 0
    var cIdx = 0

    while (dIdx < debtors.size && cIdx < creditors.size) {
        val debtor = debtors[dIdx]
        val creditor = creditors[cIdx]

        val transferAmount = min(debtor.amount, creditor.amount)
        if (transferAmount > 0.009) {
            val roundedTransfer = Math.round(transferAmount * 100.0) / 100.0
            result.add(
                SettlementEntity(
                    tripId = tripId,
                    fromTravelerId = debtor.travelerId,
                    toTravelerId = creditor.travelerId,
                    amount = roundedTransfer,
                    currency = normalizedCurrency,
                    state = SettlementState.PROPOSED.value,
                    computedAtTimestamp = System.currentTimeMillis()
                )
            )
        }

        debtor.amount -= transferAmount
        creditor.amount -= transferAmount

        if (debtor.amount <= 0.01) dIdx++
        if (creditor.amount <= 0.01) cIdx++
    }

    return result
}
