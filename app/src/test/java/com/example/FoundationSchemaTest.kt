package com.example

import com.example.data.model.BriefAgreement
import com.example.data.model.BriefTension
import com.example.data.model.ContributionEntity
import com.example.data.model.ContributionState
import com.example.data.model.LedgerEntryEntity
import com.example.data.model.PowWowSessionEntity
import com.example.data.model.PowWowSessionState
import com.example.data.model.QuoteConfidence
import com.example.data.model.SplitAllocations
import com.example.data.model.TravelerAgeBand
import com.example.data.model.TripBriefPayloads
import com.example.data.model.TripEntity
import com.example.data.model.TripStatus
import com.example.data.model.agreedByTravelerIds
import com.example.data.model.countsTowardCashSettlement
import com.example.data.model.hasLabelledProposal
import com.example.data.model.hasRecordedAgreement
import com.example.data.model.hasRequiredConsent
import com.example.data.model.hasUsableConversion
import com.example.data.model.isEligibleForAutoStart
import com.example.data.model.isTripInProgress
import com.example.data.model.withRecordedAgreement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Invariants of the Phase 0 foundation.
 *
 * These are the rules the feature tracks build on top of. Each test here corresponds to a way the
 * schema could be misread that would produce a fabricated figure or a wrongly-live trip.
 */
class FoundationSchemaTest {

    private fun tripCoveringToday(status: String): TripEntity {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
        return TripEntity(
            title = "",
            destination = "",
            startDate = LocalDate.now().minusDays(1).format(fmt),
            endDate = LocalDate.now().plusDays(1).format(fmt),
            status = status
        )
    }

    // ---- TripStatus / EXPLORING ------------------------------------------------------------

    @Test
    fun `EXPLORING sits ahead of PLANNING and is pre-departure`() {
        assertEquals(TripStatus.EXPLORING, TripStatus.entries.first())
        assertTrue(TripStatus.EXPLORING.isPreDeparture)
        assertTrue(TripStatus.PLANNING.isPreDeparture)
        assertFalse(TripStatus.IN_PROGRESS.isPreDeparture)
    }

    @Test
    fun `an EXPLORING trip whose rough window covers today is not in progress`() {
        // The failure mode this guards: EXPLORING falling through to the date-range check and
        // lighting up live-trip surfaces (SOS, live cockpit) for a trip nobody agreed to take.
        assertFalse(tripCoveringToday(TripStatus.EXPLORING.value).isTripInProgress())
    }

    @Test
    fun `an unknown status still falls through to the date range`() {
        assertTrue(tripCoveringToday("SOME_LEGACY_SPELLING").isTripInProgress())
    }

    @Test
    fun `EXPLORING and COMPLETED never auto-start, PLANNING does`() {
        assertFalse(tripCoveringToday(TripStatus.EXPLORING.value).isEligibleForAutoStart())
        assertFalse(tripCoveringToday(TripStatus.COMPLETED.value).isEligibleForAutoStart())
        assertFalse(tripCoveringToday(TripStatus.IN_PROGRESS.value).isEligibleForAutoStart())
        assertTrue(tripCoveringToday(TripStatus.PLANNING.value).isEligibleForAutoStart())
    }

    @Test
    fun `unrecognised statuses still parse to PLANNING, not EXPLORING`() {
        // Rows written before EXPLORING existed were planning-stage; re-reading them as EXPLORING
        // would silently demote real trips.
        assertEquals(TripStatus.PLANNING, TripStatus.fromString("WHATEVER"))
        assertEquals(TripStatus.EXPLORING, TripStatus.fromString("exploring"))
    }

    // ---- Contribution agreements -------------------------------------------------------------

    private fun offer() = ContributionEntity(
        tripId = 1,
        contributorTravelerId = 7,
        assetKind = "TIMESHARE_WEEK",
        state = ContributionState.OFFERED.value,
        nativeQuantity = 1.0,
        nativeUnitLabel = "week"
    )

    @Test
    fun `an amount without signatories is not an agreement`() {
        val sneaky = offer().copy(agreedValueAmount = 2400.0, agreedValueCurrency = "USD")
        assertFalse(sneaky.hasRecordedAgreement)
    }

    @Test
    fun `a recorded agreement needs signatories, a currency, and a time`() {
        val agreed = offer().withRecordedAgreement(
            amount = 2400.0,
            currency = "USD",
            agreedByTravelerIds = listOf(7L, 8L),
            agreedAtTimestamp = 1_700_000_000_000L
        )
        assertTrue(agreed.hasRecordedAgreement)
        assertEquals(listOf(7L, 8L), agreed.agreedByTravelerIds())
        assertEquals(ContributionState.AGREED.value, agreed.state)
    }

    @Test
    fun `an unsigned agreement is rejected rather than stored`() {
        assertThrows(IllegalArgumentException::class.java) {
            offer().withRecordedAgreement(2400.0, "USD", emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            offer().withRecordedAgreement(2400.0, "", listOf(7L))
        }
    }

    @Test
    fun `an unlabelled proposal is not displayable`() {
        val unlabelled = offer().copy(
            proposedValueAmount = 2400.0,
            proposedValueCurrency = "USD",
            proposedAtTimestamp = 1L
        )
        assertFalse(unlabelled.hasLabelledProposal)

        val labelled = unlabelled.copy(proposalSource = "MODELED")
        assertTrue(labelled.hasLabelledProposal)
        // A proposal is still not an agreement.
        assertFalse(labelled.hasRecordedAgreement)
    }

    // ---- Ledger --------------------------------------------------------------------------------

    private fun entry() = LedgerEntryEntity(
        tripId = 1,
        payerTravelerId = 7,
        amountOriginal = 120.0,
        originalCurrency = "USD"
    )

    @Test
    fun `points-funded entries are excluded from cash settlement`() {
        val points = entry().copy(
            fundedWithPoints = true,
            amountOriginal = 0.0,
            pointsProgramTitle = "Marriott Bonvoy",
            pointsQuantity = 120_000.0
        )
        assertFalse(points.countsTowardCashSettlement())
        assertTrue(entry().countsTowardCashSettlement())
    }

    @Test
    fun `a zero-rate entry reports no usable conversion`() {
        assertFalse(entry().hasUsableConversion())
        val converted = entry().copy(
            exchangeRate = 0.92,
            amountNormalized = 110.4,
            normalizedCurrency = "EUR"
        )
        assertTrue(converted.hasUsableConversion())
    }

    @Test
    fun `split allocations round-trip and empty stays empty`() {
        val allocations = mapOf(7L to 2.0, 8L to 1.0)
        val json = SplitAllocations.encode(allocations)
        assertEquals(allocations, SplitAllocations.decode(json))
        assertEquals("", SplitAllocations.encode(emptyMap()))
        assertEquals(emptyMap<Long, Double>(), SplitAllocations.decode(""))
        assertEquals(emptyMap<Long, Double>(), SplitAllocations.decode("{not json"))
    }

    // ---- Pow Wow consent -----------------------------------------------------------------------

    private fun session() = PowWowSessionEntity(
        travelerId = 7,
        state = PowWowSessionState.PENDING_CONSENT.value
    )

    @Test
    fun `no consent means no recording`() {
        assertFalse(session().hasRequiredConsent(TravelerAgeBand.ADULT))
    }

    @Test
    fun `an adult needs only their own consent`() {
        val consented = session().copy(consentGrantedAtTimestamp = 1L)
        assertTrue(consented.hasRequiredConsent(TravelerAgeBand.ADULT))
        assertTrue(consented.hasRequiredConsent(TravelerAgeBand.TEEN))
    }

    @Test
    fun `a child additionally needs guardian consent`() {
        val consented = session().copy(consentGrantedAtTimestamp = 1L)
        assertFalse(consented.hasRequiredConsent(TravelerAgeBand.CHILD))
        assertTrue(
            consented.copy(guardianConsentGrantedAtTimestamp = 2L)
                .hasRequiredConsent(TravelerAgeBand.CHILD)
        )
    }

    @Test
    fun `an undeclared age band is never assumed to be an adult`() {
        val consented = session().copy(consentGrantedAtTimestamp = 1L)
        assertFalse(consented.hasRequiredConsent(null))
    }

    @Test
    fun `age bands govern accounts and consent`() {
        assertTrue(TravelerAgeBand.CHILD.requiresGuardianConsent)
        assertFalse(TravelerAgeBand.CHILD.canHoldAccount)
        assertTrue(TravelerAgeBand.TEEN.canHoldAccount)
        assertNull(TravelerAgeBand.fromStringOrNull("toddler"))
    }

    // ---- Brief payloads ------------------------------------------------------------------------

    @Test
    fun `brief payloads round-trip and empty encodes to empty string`() {
        val agreements = listOf(BriefAgreement("Two rest days", listOf(7L, 8L), "BOTH"))
        val json = TripBriefPayloads.encodeAgreements(agreements)
        assertEquals(agreements, TripBriefPayloads.decodeAgreements(json))

        assertEquals("", TripBriefPayloads.encodeAgreements(emptyList()))
        assertEquals(emptyList<BriefAgreement>(), TripBriefPayloads.decodeAgreements(""))
        // Malformed JSON must read as "nothing", never as a partial brief.
        assertEquals(emptyList<BriefTension>(), TripBriefPayloads.decodeTensions("{{{"))
    }

    // ---- Pricing -------------------------------------------------------------------------------

    @Test
    fun `UNKNOWN confidence carries no figure`() {
        assertFalse(QuoteConfidence.UNKNOWN.hasFigure)
        assertTrue(QuoteConfidence.MODELED.hasFigure)
        assertTrue(QuoteConfidence.KNOWN.hasFigure)
    }
}
