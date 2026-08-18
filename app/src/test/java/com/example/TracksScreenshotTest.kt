package com.example

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.data.model.BriefAgreement
import com.example.data.model.BriefPosition
import com.example.data.model.BriefReadinessItem
import com.example.data.model.BriefResolution
import com.example.data.model.BriefTension
import com.example.data.model.ContributionAssetKind
import com.example.data.model.ContributionEntity
import com.example.data.model.ContributionProposalSource
import com.example.data.model.ContributionState
import com.example.data.model.PartyUnitEntity
import com.example.data.model.PartyUnitType
import com.example.data.model.TravelerAgeBand
import com.example.data.model.TravelerEntity
import com.example.data.model.TripBriefEntity
import com.example.data.model.TripBriefPayloads
import com.example.data.model.TripMembershipEntity
import com.example.data.model.TripMembershipState
import com.example.data.model.TripRole
import com.example.feature.ledger.ui.ContributionCard
import com.example.feature.party.ui.PartyUnitsView
import com.example.ui.components.ChatPowWowBriefCard
import com.example.ui.components.ChatTensionCard
import com.example.ui.theme.LuxuryDarkBase
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w344dp-h700dp-xxhdpi", sdk = [36])
class TracksScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun screenshot_chat_pow_wow_brief_card() {
        val agreements = listOf(
            BriefAgreement(
                statement = "Dedicated morning exploration blocks combined with unhurried cultural dinners.",
                supportingTravelerIds = listOf(1, 2),
                evidenceSource = "BOTH"
            ),
            BriefAgreement(
                statement = "Shared digital expense pool with transparent individual logging.",
                supportingTravelerIds = listOf(1, 2),
                evidenceSource = "TRANSCRIPT"
            )
        )
        val readiness = listOf(
            BriefReadinessItem(
                key = "READINESS_POW_WOW",
                label = "Pow Wow Brain Dumps",
                isSatisfied = true,
                isCritical = true,
                detail = "2 sessions transcribed"
            ),
            BriefReadinessItem(
                key = "READINESS_PARTY_UNITS",
                label = "Party Units & Crew",
                isSatisfied = true,
                isCritical = true,
                detail = "4 crew members confirmed"
            )
        )
        val brief = TripBriefEntity(
            tripId = 1,
            version = 1,
            agreementsJson = TripBriefPayloads.encodeAgreements(agreements),
            readinessJson = TripBriefPayloads.encodeReadiness(readiness),
            summaryText = "Synthesized from 2 member Pow Wow sessions. Found 2 shared alignments and 1 key planning tension."
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Surface(color = LuxuryDarkBase, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    ChatPowWowBriefCard(brief = brief)
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/pow_wow_brief_card.png")
    }

    @Test
    fun screenshot_chat_tension_card() {
        val tension = BriefTension(
            tensionId = "tension_pacing_1",
            topic = "Daily Pace: High-Intensity Sightseeing vs. Leisurely Spa Recovery",
            positions = listOf(
                BriefPosition(
                    travelerId = 1,
                    stance = "Wants multi-site historical trails starting at sunrise.",
                    dnaEvidence = "Travel Style: High-Intensity Cultural Trail"
                ),
                BriefPosition(
                    travelerId = 2,
                    stance = "Prefers late mornings, 1-2 curated activities max per day.",
                    dnaEvidence = "Travel Style: Slow-Paced Relaxation"
                )
            ),
            stakes = "Risk of exhaustion or frustration if pacing is forced into a single rigid schedule."
        )
        val resolution = BriefResolution(
            tensionId = "tension_pacing_1",
            proposal = "Split mornings: early excursion group departs at 08:00; leisure group rejoins for lunch at 13:00.",
            state = "PROPOSED"
        )
        val travelers = mapOf(
            1L to TravelerEntity(id = 1, displayName = "Dana", ageBand = TravelerAgeBand.ADULT.value),
            2L to TravelerEntity(id = 2, displayName = "Pete", ageBand = TravelerAgeBand.ADULT.value)
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Surface(color = LuxuryDarkBase, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    ChatTensionCard(
                        tension = tension,
                        resolution = resolution,
                        travelersById = travelers
                    )
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/tension_card.png")
    }

    @Test
    fun screenshot_party_units_view_folded_width() {
        val units = listOf(
            PartyUnitEntity(id = 1, tripId = 1, unitType = PartyUnitType.FAMILY.value, label = "The Explorers"),
            PartyUnitEntity(id = 2, tripId = 1, unitType = PartyUnitType.COUPLE.value, label = "Suite 402")
        )
        val travelers = mapOf(
            1L to TravelerEntity(id = 1, displayName = "Dana", ageBand = TravelerAgeBand.ADULT.value),
            2L to TravelerEntity(id = 2, displayName = "Pete", ageBand = TravelerAgeBand.ADULT.value),
            3L to TravelerEntity(id = 3, displayName = "Leo", ageBand = TravelerAgeBand.TEEN.value),
            4L to TravelerEntity(id = 4, displayName = "Maya", ageBand = TravelerAgeBand.CHILD.value)
        )
        val memberships = listOf(
            TripMembershipEntity(id = 1, tripId = 1, travelerId = 1, partyUnitId = 1, role = TripRole.ORGANIZER.value, state = TripMembershipState.ACTIVE.value),
            TripMembershipEntity(id = 2, tripId = 1, travelerId = 2, partyUnitId = 1, role = TripRole.TRAVELER.value, state = TripMembershipState.ACTIVE.value),
            TripMembershipEntity(id = 3, tripId = 1, travelerId = 3, partyUnitId = 2, role = TripRole.TRAVELER.value, state = TripMembershipState.ACTIVE.value),
            TripMembershipEntity(id = 4, tripId = 1, travelerId = 4, partyUnitId = 0, role = TripRole.VIEWER.value, state = TripMembershipState.ACTIVE.value)
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Surface(color = LuxuryDarkBase, modifier = Modifier.fillMaxWidth()) {
                    PartyUnitsView(
                        partyUnits = units,
                        memberships = memberships,
                        travelers = travelers.values.toList()
                    )
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/party_units_folded.png")
    }

    @Test
    fun screenshot_contribution_card() {
        val contribution = ContributionEntity(
            id = 1,
            tripId = 1,
            contributorTravelerId = 1,
            assetKind = ContributionAssetKind.POINTS.value,
            state = ContributionState.AGREED.value,
            programTitle = "Amex Membership Rewards",
            nativeQuantity = 60000.0,
            nativeUnitLabel = "points",
            proposedValueAmount = 900.0,
            proposedValueCurrency = "USD",
            proposalSource = ContributionProposalSource.MODELED.value,
            agreedValueAmount = 900.0,
            agreedValueCurrency = "USD",
            agreedByTravelerIdsCsv = "1,2",
            agreedAtTimestamp = 1700000000000L
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Surface(color = LuxuryDarkBase, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    ContributionCard(
                        contribution = contribution,
                        contributorName = "Dana",
                        onRecordAgreementClick = {}
                    )
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/contribution_card.png")
    }
}
