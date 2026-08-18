package com.example

import com.example.data.model.ContributionEntity
import com.example.data.model.ContributionState
import com.example.data.model.LedgerEntryEntity
import com.example.data.model.LedgerModel
import com.example.data.model.SettlementState
import com.example.data.model.SplitAllocations
import com.example.data.model.SplitRuleEntity
import com.example.data.model.SplitRuleType
import com.example.data.model.withRecordedAgreement
import com.example.feature.ledger.engine.CorporatePolicySettlementEngine
import com.example.feature.ledger.engine.PersonalSettlementEngine
import com.example.feature.ledger.engine.SettlementEngines
import com.example.feature.ledger.engine.SharedPotSettlementEngine
import com.example.feature.ledger.engine.SplitSettleSettlementEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettlementEngineTest {

    private val travelers = listOf(1L, 2L, 3L)

    @Test
    fun `engine factory returns expected engine per model`() {
        assertEquals(SharedPotSettlementEngine, SettlementEngines.forModel(LedgerModel.SHARED_POT))
        assertEquals(SplitSettleSettlementEngine, SettlementEngines.forModel(LedgerModel.SPLIT_SETTLE))
        assertEquals(PersonalSettlementEngine, SettlementEngines.forModel(LedgerModel.PERSONAL))
        assertEquals(CorporatePolicySettlementEngine, SettlementEngines.forModel(LedgerModel.CORPORATE_POLICY))
    }

    @Test
    fun `SplitSettle equal split divides expense evenly across all travelers`() {
        val entry = LedgerEntryEntity(
            id = 10,
            tripId = 1,
            payerTravelerId = 1L,
            amountOriginal = 300.0,
            originalCurrency = "USD"
        )
        val balances = SplitSettleSettlementEngine.balances(
            entries = listOf(entry),
            splitRules = emptyList(),
            contributions = emptyList(),
            travelerIds = travelers,
            normalizedCurrency = "USD"
        )

        val b1 = balances.first { it.travelerId == 1L }
        val b2 = balances.first { it.travelerId == 2L }
        val b3 = balances.first { it.travelerId == 3L }

        assertEquals(300.0, b1.paid, 0.001)
        assertEquals(100.0, b1.owed, 0.001)
        assertEquals(200.0, b1.net, 0.001)

        assertEquals(0.0, b2.paid, 0.001)
        assertEquals(100.0, b2.owed, 0.001)
        assertEquals(-100.0, b2.net, 0.001)

        assertEquals(0.0, b3.paid, 0.001)
        assertEquals(100.0, b3.owed, 0.001)
        assertEquals(-100.0, b3.net, 0.001)

        val transfers = SplitSettleSettlementEngine.proposeTransfers(1L, balances, "USD")
        assertEquals(2, transfers.size)
        assertTrue(transfers.any { it.fromTravelerId == 2L && it.toTravelerId == 1L && it.amount == 100.0 })
        assertTrue(transfers.any { it.fromTravelerId == 3L && it.toTravelerId == 1L && it.amount == 100.0 })
        assertEquals(SettlementState.PROPOSED.value, transfers.first().state)
    }

    @Test
    fun `SplitSettle SHARES allocation splits according to share weights`() {
        val rule = SplitRuleEntity(
            id = 5,
            tripId = 1,
            ledgerEntryId = 10,
            ruleType = SplitRuleType.SHARES.value,
            allocationsJson = SplitAllocations.encode(mapOf(1L to 1.0, 2L to 2.0, 3L to 1.0))
        )
        val entry = LedgerEntryEntity(
            id = 10,
            tripId = 1,
            payerTravelerId = 1L,
            amountOriginal = 400.0,
            originalCurrency = "USD",
            splitRuleId = 5
        )
        val balances = SplitSettleSettlementEngine.balances(
            entries = listOf(entry),
            splitRules = listOf(rule),
            contributions = emptyList(),
            travelerIds = travelers,
            normalizedCurrency = "USD"
        )

        val b2 = balances.first { it.travelerId == 2L }
        assertEquals(200.0, b2.owed, 0.001)
        assertEquals(-200.0, b2.net, 0.001)
    }

    @Test
    fun `points-funded entries are excluded from cash math but tracked in points quantity`() {
        val pointsEntry = LedgerEntryEntity(
            id = 20,
            tripId = 1,
            payerTravelerId = 2L,
            amountOriginal = 0.0,
            originalCurrency = "USD",
            fundedWithPoints = true,
            pointsProgramTitle = "Marriott Bonvoy",
            pointsQuantity = 120000.0
        )
        val balances = SplitSettleSettlementEngine.balances(
            entries = listOf(pointsEntry),
            splitRules = emptyList(),
            contributions = emptyList(),
            travelerIds = travelers,
            normalizedCurrency = "USD"
        )

        val b2 = balances.first { it.travelerId == 2L }
        assertEquals(0.0, b2.paid, 0.001)
        assertEquals(0.0, b2.owed, 0.001)
        assertEquals(0.0, b2.net, 0.001)
        assertEquals(120000.0, b2.pointsContributedQuantity, 0.001)
    }

    @Test
    fun `unconverted currency entries are flagged in unconvertedEntryIds without fake conversion`() {
        val unconvertedEntry = LedgerEntryEntity(
            id = 30,
            tripId = 1,
            payerTravelerId = 3L,
            amountOriginal = 15000.0,
            originalCurrency = "JPY", // Not normalized to USD, rate is 0.0
            exchangeRate = 0.0,
            amountNormalized = 0.0,
            normalizedCurrency = ""
        )
        val balances = SplitSettleSettlementEngine.balances(
            entries = listOf(unconvertedEntry),
            splitRules = emptyList(),
            contributions = emptyList(),
            travelerIds = travelers,
            normalizedCurrency = "USD"
        )

        val b3 = balances.first { it.travelerId == 3L }
        assertEquals(0.0, b3.paid, 0.001)
        assertEquals(listOf(30L), b3.unconvertedEntryIds)
    }

    @Test
    fun `recorded contribution agreement credits the contributor in normalized currency`() {
        val offer = ContributionEntity(
            id = 1,
            tripId = 1,
            contributorTravelerId = 2L,
            assetKind = "TIMESHARE_WEEK",
            state = ContributionState.OFFERED.value,
            nativeQuantity = 1.0,
            nativeUnitLabel = "week"
        ).withRecordedAgreement(
            amount = 1200.0,
            currency = "USD",
            agreedByTravelerIds = listOf(1L, 2L, 3L)
        )

        val balances = SplitSettleSettlementEngine.balances(
            entries = emptyList(),
            splitRules = emptyList(),
            contributions = listOf(offer),
            travelerIds = travelers,
            normalizedCurrency = "USD"
        )

        val b2 = balances.first { it.travelerId == 2L }
        assertEquals(1200.0, b2.paid, 0.001)
        assertEquals(1200.0, b2.net, 0.001)
    }

    @Test
    fun `Personal and Corporate models produce no settlement transfers`() {
        val entry = LedgerEntryEntity(
            id = 10,
            tripId = 1,
            payerTravelerId = 1L,
            amountOriginal = 100.0,
            originalCurrency = "USD"
        )
        val pBalances = PersonalSettlementEngine.balances(listOf(entry), emptyList(), emptyList(), travelers, "USD")
        assertTrue(PersonalSettlementEngine.proposeTransfers(1L, pBalances, "USD").isEmpty())

        val cBalances = CorporatePolicySettlementEngine.balances(listOf(entry), emptyList(), emptyList(), travelers, "USD")
        assertTrue(CorporatePolicySettlementEngine.proposeTransfers(1L, cBalances, "USD").isEmpty())
    }
}
