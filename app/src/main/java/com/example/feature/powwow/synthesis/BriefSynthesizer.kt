package com.example.feature.powwow.synthesis

import com.example.data.model.BriefReadinessItem
import com.example.data.model.PowWowSessionEntity
import com.example.data.model.PowWowTranscriptEntity
import com.example.data.model.TravelerEntity
import com.example.data.model.TripBriefEntity
import com.example.data.model.TripBriefPayloads

/**
 * Synthesizes member Pow Wow transcripts and weighted traveler profiles into an actionable,
 * unvarnished [TripBriefEntity].
 *
 * Core rule: Conflicts and differences are never averaged away into bland compromises; they are
 * stated cleanly as named [com.example.data.model.BriefTension] items with attributed member stances and DNA evidence.
 * If the transcripts contain no agreements or tensions, an honest empty brief is produced — never
 * fabricated filler.
 */
object BriefSynthesizer {

    suspend fun synthesizeBrief(
        tripId: Long,
        ideaId: Long = 0L,
        version: Int = 1,
        transcripts: List<PowWowTranscriptEntity>,
        travelers: List<TravelerEntity>,
        sessions: List<PowWowSessionEntity> = emptyList(),
        dnaClauses: Map<Long, String> = emptyMap(),
        engine: BriefSynthesisEngine = FirebaseGeminiBriefSynthesisEngine()
    ): TripBriefEntity {
        val sessionIdsCsv = transcripts.map { it.sessionId }.joinToString(",")
        val sessionToTraveler = sessions.associate { it.id to it.travelerId }

        val contributions = transcripts.mapNotNull { t ->
            if (t.transcriptText.isBlank()) return@mapNotNull null
            val travelerId = sessionToTraveler[t.sessionId]
                ?: travelers.firstOrNull()?.id
                ?: return@mapNotNull null
            val displayName = travelers.find { it.id == travelerId }?.displayName
                ?.ifBlank { "Traveler $travelerId" } ?: "Traveler $travelerId"
            TranscriptContribution(
                travelerId = travelerId,
                displayName = displayName,
                transcriptText = t.transcriptText
            )
        }

        val synthesisResult = if (contributions.isNotEmpty()) {
            engine.synthesize(contributions, dnaClauses)
        } else {
            BriefSynthesisResult.EMPTY
        }

        // Readiness Checklist - accurately derived from actual state
        val readiness = mutableListOf<BriefReadinessItem>()
        val nonBlankTranscripts = transcripts.count { it.transcriptText.isNotBlank() }
        readiness.add(
            BriefReadinessItem(
                key = "READINESS_POW_WOW",
                label = "Pow Wow Brain Dumps",
                isSatisfied = nonBlankTranscripts > 0,
                isCritical = true,
                detail = if (nonBlankTranscripts > 0) "$nonBlankTranscripts member sessions transcribed" else "Pending member capture"
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

        val summary = if (synthesisResult.summary.isNotBlank()) {
            synthesisResult.summary
        } else if (synthesisResult.agreements.isNotEmpty() || synthesisResult.tensions.isNotEmpty()) {
            "Synthesized ${synthesisResult.agreements.size} shared alignments and ${synthesisResult.tensions.size} key planning tensions from ${contributions.size} member transcripts."
        } else if (transcripts.isNotEmpty()) {
            "Transcripts analyzed; no consensus alignments or explicit tensions were derived from the current recordings."
        } else {
            "Preliminary brief initialized. Conduct Pow Wow sessions to elicit deeper alignments."
        }

        return TripBriefEntity(
            tripId = tripId,
            ideaId = ideaId,
            version = version,
            generatedAtTimestamp = System.currentTimeMillis(),
            sourceSessionIdsCsv = sessionIdsCsv,
            agreementsJson = TripBriefPayloads.encodeAgreements(synthesisResult.agreements),
            tensionsJson = TripBriefPayloads.encodeTensions(synthesisResult.tensions),
            resolutionsJson = TripBriefPayloads.encodeResolutions(synthesisResult.resolutions),
            readinessJson = TripBriefPayloads.encodeReadiness(readiness),
            summaryText = summary
        )
    }
}
