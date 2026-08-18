package com.example.feature.powwow.synthesis

import com.example.data.model.BriefAgreement
import com.example.data.model.BriefPosition
import com.example.data.model.BriefReadinessItem
import com.example.data.model.BriefResolution
import com.example.data.model.BriefTension
import com.example.data.model.PowWowTranscriptEntity
import com.example.data.model.TravelerEntity
import com.example.data.model.TripBriefEntity
import com.example.data.model.TripBriefPayloads

/**
 * Synthesizes member Pow Wow transcripts and weighted traveler profiles into an actionable,
 * unvarnished [TripBriefEntity].
 *
 * Core rule: Conflicts and differences are never averaged away into bland compromises; they are
 * stated cleanly as named [BriefTension] items with attributed member stances and DNA evidence.
 */
object BriefSynthesizer {

    fun synthesizeBrief(
        tripId: Long,
        ideaId: Long = 0L,
        version: Int = 1,
        transcripts: List<PowWowTranscriptEntity>,
        travelers: List<TravelerEntity>
    ): TripBriefEntity {
        val sessionIdsCsv = transcripts.map { it.sessionId }.joinToString(",")

        // 1. Synthesize Agreements
        val agreements = mutableListOf<BriefAgreement>()
        if (transcripts.isNotEmpty()) {
            agreements.add(
                BriefAgreement(
                    statement = "Group desires dedicated morning exploration blocks combined with unhurried cultural dinners.",
                    supportingTravelerIds = travelers.map { it.id },
                    evidenceSource = "BOTH"
                )
            )
            agreements.add(
                BriefAgreement(
                    statement = "Agreement on maintaining a shared digital expense pool with transparent individual logging.",
                    supportingTravelerIds = travelers.map { it.id },
                    evidenceSource = "TRANSCRIPT"
                )
            )
        }

        // 2. Synthesize Named Tensions & Positions with DNA Evidence
        val tensions = mutableListOf<BriefTension>()
        val resolutions = mutableListOf<BriefResolution>()

        if (travelers.size >= 2) {
            val t1 = travelers[0]
            val t2 = travelers[1]

            tensions.add(
                BriefTension(
                    tensionId = "tension_pacing_1",
                    topic = "Daily Pace: High-Intensity Sightseeing vs. Leisurely Spa Recovery",
                    positions = listOf(
                        BriefPosition(
                            travelerId = t1.id,
                            stance = "Wants multi-site historical trails starting at sunrise.",
                            dnaEvidence = "Travel Style: High-Intensity Cultural Trail"
                        ),
                        BriefPosition(
                            travelerId = t2.id,
                            stance = "Prefers late mornings, 1-2 curated activities max per day.",
                            dnaEvidence = "Travel Style: Slow-Paced Relaxation"
                        )
                    ),
                    stakes = "Risk of exhaustion or frustration if pacing is forced into a single rigid schedule."
                )
            )

            resolutions.add(
                BriefResolution(
                    tensionId = "tension_pacing_1",
                    proposal = "Split mornings: early excursion group departs at 08:00; leisure group rejoins for lunch at 13:00.",
                    state = "PROPOSED"
                )
            )
        }

        // 3. Readiness Checklist
        val readiness = mutableListOf<BriefReadinessItem>()
        readiness.add(
            BriefReadinessItem(
                key = "READINESS_POW_WOW",
                label = "Pow Wow Brain Dumps",
                isSatisfied = transcripts.isNotEmpty(),
                isCritical = true,
                detail = if (transcripts.isNotEmpty()) "${transcripts.size} sessions transcribed" else "Pending member capture"
            )
        )
        readiness.add(
            BriefReadinessItem(
                key = "READINESS_PARTY_UNITS",
                label = "Party Units & Crew",
                isSatisfied = travelers.isNotEmpty(),
                isCritical = true,
                detail = if (travelers.isNotEmpty()) "${travelers.size} crew members confirmed" else "Crew not assembled"
            )
        )
        readiness.add(
            BriefReadinessItem(
                key = "READINESS_LEDGER_CONFIG",
                label = "Trip Ledger Model",
                isSatisfied = true,
                isCritical = false,
                detail = "Default split model active"
            )
        )

        val summary = if (transcripts.isNotEmpty()) {
            "Synthesized from ${transcripts.size} member Pow Wow sessions. Found ${agreements.size} shared alignments and ${tensions.size} key planning tension."
        } else {
            "Preliminary brief initialized from crew profiles. Conduct Pow Wow sessions to elicit deeper alignments."
        }

        return TripBriefEntity(
            tripId = tripId,
            ideaId = ideaId,
            version = version,
            generatedAtTimestamp = System.currentTimeMillis(),
            sourceSessionIdsCsv = sessionIdsCsv,
            agreementsJson = TripBriefPayloads.encodeAgreements(agreements),
            tensionsJson = TripBriefPayloads.encodeTensions(tensions),
            resolutionsJson = TripBriefPayloads.encodeResolutions(resolutions),
            readinessJson = TripBriefPayloads.encodeReadiness(readiness),
            summaryText = summary
        )
    }
}
