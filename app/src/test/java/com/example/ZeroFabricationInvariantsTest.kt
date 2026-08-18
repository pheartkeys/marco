package com.example

import com.example.data.local.PartyDao
import com.example.data.local.PowWowDao
import com.example.data.local.PricingDao
import com.example.data.model.BriefAgreement
import com.example.data.model.BriefPosition
import com.example.data.model.BriefResolution
import com.example.data.model.BriefTension
import com.example.data.model.IdeaEntity
import com.example.data.model.PartyUnitEntity
import com.example.data.model.PowWowSessionEntity
import com.example.data.model.PowWowSessionState
import com.example.data.model.PowWowTranscriptEntity
import com.example.data.model.PriceQuoteCacheEntity
import com.example.data.model.QuoteRequest
import com.example.data.model.QuoteResult
import com.example.data.model.TravelerAgeBand
import com.example.data.model.TravelerEntity
import com.example.data.model.TripBriefEntity
import com.example.data.model.TripBriefPayloads
import com.example.data.model.TripMembershipEntity
import com.example.data.repository.LivePricingRepository
import com.example.data.repository.RoomPowWowRepository
import com.example.feature.powwow.synthesis.BriefSynthesisEngine
import com.example.feature.powwow.synthesis.BriefSynthesisResult
import com.example.feature.powwow.synthesis.BriefSynthesizer
import com.example.feature.powwow.synthesis.TranscriptContribution
import com.example.feature.powwow.transcription.PowWowTranscriber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ZeroFabricationInvariantsTest {

    private class TestPartyDao : PartyDao {
        val travelers = mutableMapOf<Long, TravelerEntity>()
        override fun getAllTravelers(): Flow<List<TravelerEntity>> = flowOf(travelers.values.toList())
        override fun getTravelerById(travelerId: Long): Flow<TravelerEntity?> = flowOf(travelers[travelerId])
        override suspend fun getTravelerByIdSync(travelerId: Long): TravelerEntity? = travelers[travelerId]
        override suspend fun getTravelerByAuthUid(authUid: String): TravelerEntity? = travelers.values.firstOrNull { it.authUid == authUid }
        override suspend fun insertTraveler(traveler: TravelerEntity): Long {
            val id = if (traveler.id == 0L) (travelers.size + 1).toLong() else traveler.id
            travelers[id] = traveler.copy(id = id)
            return id
        }
        override suspend fun updateTraveler(traveler: TravelerEntity) { travelers[traveler.id] = traveler }
        override suspend fun deleteTraveler(travelerId: Long) { travelers.remove(travelerId) }
        override fun getPartyUnitsForTrip(tripId: Long): Flow<List<PartyUnitEntity>> = flowOf(emptyList())
        override suspend fun getPartyUnitsForTripSync(tripId: Long): List<PartyUnitEntity> = emptyList()
        override suspend fun insertPartyUnit(unit: PartyUnitEntity): Long = 1L
        override suspend fun updatePartyUnit(unit: PartyUnitEntity) {}
        override suspend fun deletePartyUnit(partyUnitId: Long) {}
        override suspend fun clearPartyUnitAssignments(partyUnitId: Long) {}
        override fun getMembershipsForTrip(tripId: Long): Flow<List<TripMembershipEntity>> = flowOf(emptyList())
        override suspend fun getMembershipsForTripSync(tripId: Long): List<TripMembershipEntity> = emptyList()
        override fun getMembershipsForTraveler(travelerId: Long): Flow<List<TripMembershipEntity>> = flowOf(emptyList())
        override suspend fun getMembership(tripId: Long, travelerId: Long): TripMembershipEntity? = null
        override suspend fun insertMembership(membership: TripMembershipEntity): Long = 1L
        override suspend fun updateMembership(membership: TripMembershipEntity) {}
        override suspend fun deleteMembership(membership: TripMembershipEntity) {}
        override suspend fun assignMembershipToUnit(membershipId: Long, partyUnitId: Long) {}
        override suspend fun updateMembershipRole(membershipId: Long, role: String) {}
        override suspend fun updateMembershipState(membershipId: Long, state: String) {}
        override fun getTravelersForTrip(tripId: Long): Flow<List<TravelerEntity>> = flowOf(travelers.values.toList())
        override fun getActiveIdeas(): Flow<List<IdeaEntity>> = flowOf(emptyList())
        override fun getAllIdeas(): Flow<List<IdeaEntity>> = flowOf(emptyList())
        override fun getIdeaById(ideaId: Long): Flow<IdeaEntity?> = flowOf(null)
        override suspend fun getIdeaByIdSync(ideaId: Long): IdeaEntity? = null
        override suspend fun insertIdea(idea: IdeaEntity): Long = 1L
        override suspend fun updateIdea(idea: IdeaEntity) {}
        override suspend fun markIdeaPromoted(ideaId: Long, tripId: Long, timestamp: Long) {}
        override suspend fun markIdeaDiscarded(ideaId: Long, timestamp: Long) {}
        override suspend fun clearTravelers() { travelers.clear() }
        override suspend fun clearPartyUnits() {}
        override suspend fun clearMemberships() {}
        override suspend fun clearIdeas() {}
    }

    private class TestPowWowDao : PowWowDao {
        val sessions = mutableMapOf<Long, PowWowSessionEntity>()
        val transcripts = mutableMapOf<Long, PowWowTranscriptEntity>()
        val briefs = mutableMapOf<Long, TripBriefEntity>()
        var recordedFailure: String? = null
        private var idCounter = 1L

        override fun getSessionsForTrip(tripId: Long): Flow<List<PowWowSessionEntity>> =
            flowOf(sessions.values.filter { it.tripId == tripId })

        override fun getSessionsForIdea(ideaId: Long): Flow<List<PowWowSessionEntity>> =
            flowOf(sessions.values.filter { it.ideaId == ideaId })

        override fun getSessionById(sessionId: Long): Flow<PowWowSessionEntity?> =
            flowOf(sessions[sessionId])

        override suspend fun getSessionByIdSync(sessionId: Long): PowWowSessionEntity? =
            sessions[sessionId]

        override suspend fun getLatestSessionFor(tripId: Long, travelerId: Long): PowWowSessionEntity? =
            sessions.values.filter { it.tripId == tripId && it.travelerId == travelerId }.maxByOrNull { it.id }

        override suspend fun insertSession(session: PowWowSessionEntity): Long {
            val id = if (session.id == 0L) idCounter++ else session.id
            sessions[id] = session.copy(id = id)
            return id
        }

        override suspend fun updateSession(session: PowWowSessionEntity) {
            sessions[session.id] = session
            if (session.failureReason.isNotBlank()) {
                recordedFailure = session.failureReason
            }
        }

        override suspend fun updateSessionState(sessionId: Long, state: String) {
            sessions[sessionId]?.let { sessions[sessionId] = it.copy(state = state) }
        }

        override suspend fun markAudioDeleted(sessionId: Long, timestamp: Long) {
            sessions[sessionId]?.let { sessions[sessionId] = it.copy(audioDeletedAtTimestamp = timestamp) }
        }

        override suspend fun deleteSession(sessionId: Long) {
            sessions.remove(sessionId)
        }

        override suspend fun getSessionsAwaitingAudioDeletion(): List<PowWowSessionEntity> =
            sessions.values.filter { it.durationSeconds > 0 && it.audioDeletedAtTimestamp == 0L }

        override fun getTranscriptsForSession(sessionId: Long): Flow<List<PowWowTranscriptEntity>> =
            flowOf(transcripts.values.filter { it.sessionId == sessionId })

        override suspend fun getLatestTranscriptForSession(sessionId: Long): PowWowTranscriptEntity? =
            transcripts[sessionId]

        override suspend fun insertTranscript(transcript: PowWowTranscriptEntity): Long {
            transcripts[transcript.sessionId] = transcript
            return transcript.sessionId
        }

        override suspend fun deleteTranscriptsForSession(sessionId: Long) {
            transcripts.remove(sessionId)
        }

        override fun getLatestBriefForTrip(tripId: Long): Flow<TripBriefEntity?> =
            flowOf(briefs.values.filter { it.tripId == tripId }.maxByOrNull { it.version })

        override suspend fun getLatestBriefForTripSync(tripId: Long): TripBriefEntity? =
            briefs.values.filter { it.tripId == tripId }.maxByOrNull { it.version }

        override fun getBriefsForTrip(tripId: Long): Flow<List<TripBriefEntity>> =
            flowOf(briefs.values.filter { it.tripId == tripId })

        override suspend fun getLatestBriefForIdea(ideaId: Long): TripBriefEntity? =
            briefs.values.filter { it.ideaId == ideaId }.maxByOrNull { it.version }

        override suspend fun insertBrief(brief: TripBriefEntity): Long {
            val id = if (brief.id == 0L) idCounter++ else brief.id
            briefs[id] = brief.copy(id = id)
            return id
        }

        override suspend fun updateBrief(brief: TripBriefEntity) {
            briefs[brief.id] = brief
        }

        override suspend fun markBriefAccepted(briefId: Long, timestamp: Long) {
            briefs[briefId]?.let { briefs[briefId] = it.copy(acceptedAtTimestamp = timestamp) }
        }

        override suspend fun clearSessions() { sessions.clear() }
        override suspend fun clearTranscripts() { transcripts.clear() }
        override suspend fun clearBriefs() { briefs.clear() }
    }

    // -------------------------------------------------------------
    // TRACK A: Pow Wow Transcription & Synthesis Zero-Fabrication
    // -------------------------------------------------------------

    @Test
    fun `PowWowTranscriber deletes audio file and fails honestly on empty file`() = runBlocking {
        val tempAudioFile = File.createTempFile("test_zero_audio", ".m4a")
        tempAudioFile.writeBytes(ByteArray(0)) // 0-byte file

        val fakeDao = TestPowWowDao()
        val fakePartyDao = TestPartyDao()
        val repository = RoomPowWowRepository(fakeDao, fakePartyDao)
        val sessionId = repository.createSession(
            PowWowSessionEntity(
                tripId = 1L,
                travelerId = 1L,
                state = PowWowSessionState.RECORDING.value
            )
        )
        val transcriber = PowWowTranscriber(repository)

        val result = transcriber.transcribeAndPurgeAudio(sessionId, tempAudioFile)

        // Must fail honestly
        assertTrue(result.isFailure)
        // Must delete audio immediately
        assertFalse(tempAudioFile.exists())
        // Must record honest failure reason on session
        val session = repository.getSession(sessionId)
        assertEquals(PowWowSessionState.FAILED.value, session?.state)
        assertTrue(session?.failureReason?.isNotBlank() ?: false)
    }

    @Test
    fun `BriefSynthesizer produces empty brief without canned content when transcripts are empty`() = runBlocking {
        val brief = BriefSynthesizer.synthesizeBrief(
            tripId = 1L,
            ideaId = 0L,
            version = 1,
            transcripts = emptyList(),
            travelers = listOf(
                TravelerEntity(id = 1L, displayName = "Alice", ageBand = TravelerAgeBand.ADULT.value),
                TravelerEntity(id = 2L, displayName = "Bob", ageBand = TravelerAgeBand.ADULT.value)
            )
        )

        val agreements = TripBriefPayloads.decodeAgreements(brief.agreementsJson)
        val tensions = TripBriefPayloads.decodeTensions(brief.tensionsJson)
        val resolutions = TripBriefPayloads.decodeResolutions(brief.resolutionsJson)
        val readiness = TripBriefPayloads.decodeReadiness(brief.readinessJson)

        assertEquals(0, agreements.size)
        assertEquals(0, tensions.size)
        assertEquals(0, resolutions.size)

        val powWowReadiness = readiness.firstOrNull { it.key == "READINESS_POW_WOW" }
        assertNotNull(powWowReadiness)
        assertFalse(powWowReadiness!!.isSatisfied)
        assertEquals("Pending member capture", powWowReadiness.detail)
    }

    @Test
    fun `BriefSynthesizer delegates to BriefSynthesisEngine and preserves genuine alignments`() = runBlocking {
        val fakeEngine = object : BriefSynthesisEngine {
            override suspend fun synthesize(
                contributions: List<TranscriptContribution>,
                dnaClauses: Map<Long, String>
            ): BriefSynthesisResult {
                return BriefSynthesisResult(
                    agreements = listOf(
                        BriefAgreement(
                            statement = "Both want seafood on day 1",
                            supportingTravelerIds = listOf(1L, 2L),
                            evidenceSource = "TRANSCRIPT"
                        )
                    ),
                    tensions = listOf(
                        BriefTension(
                            tensionId = "t1",
                            topic = "Wake-up Time",
                            positions = listOf(
                                BriefPosition(1L, "Wants 6am sunrise hike", dnaEvidence = dnaClauses[1L].orEmpty()),
                                BriefPosition(2L, "Wants sleep until 9am", dnaEvidence = "")
                            ),
                            stakes = "Morning schedule coordination"
                        )
                    ),
                    resolutions = listOf(
                        BriefResolution("t1", "Optional early hike, meet at brunch", state = "PROPOSED")
                    ),
                    summary = "Derived 1 agreement and 1 tension."
                )
            }
        }

        val travelers = listOf(
            TravelerEntity(id = 1L, displayName = "Alice", ageBand = TravelerAgeBand.ADULT.value),
            TravelerEntity(id = 2L, displayName = "Bob", ageBand = TravelerAgeBand.ADULT.value)
        )
        val sessions = listOf(
            PowWowSessionEntity(id = 101L, tripId = 1L, travelerId = 1L, state = PowWowSessionState.TRANSCRIBED.value),
            PowWowSessionEntity(id = 102L, tripId = 1L, travelerId = 2L, state = PowWowSessionState.TRANSCRIBED.value)
        )
        val transcripts = listOf(
            PowWowTranscriptEntity(id = 1L, sessionId = 101L, transcriptText = "I want seafood and early morning hikes!"),
            PowWowTranscriptEntity(id = 2L, sessionId = 102L, transcriptText = "I love seafood dinners and sleeping in.")
        )
        val dnaClauses = mapOf(1L to "Motivation: Adventure 7")

        val brief = BriefSynthesizer.synthesizeBrief(
            tripId = 1L,
            ideaId = 0L,
            version = 1,
            transcripts = transcripts,
            travelers = travelers,
            sessions = sessions,
            dnaClauses = dnaClauses,
            engine = fakeEngine
        )

        val agreements = TripBriefPayloads.decodeAgreements(brief.agreementsJson)
        val tensions = TripBriefPayloads.decodeTensions(brief.tensionsJson)

        assertEquals(1, agreements.size)
        assertEquals("Both want seafood on day 1", agreements[0].statement)
        assertEquals(listOf(1L, 2L), agreements[0].supportingTravelerIds)

        assertEquals(1, tensions.size)
        assertEquals("Wake-up Time", tensions[0].topic)
        assertEquals("Motivation: Adventure 7", tensions[0].positions[0].dnaEvidence)
        assertEquals("", tensions[0].positions[1].dnaEvidence)
    }

    // -------------------------------------------------------------
    // TRACK C: Pricing Provenance & Confidence Zero-Fabrication
    // -------------------------------------------------------------

    @Test
    fun `LivePricingRepository returns Unavailable when no service configured and cache empty`() = runBlocking {
        val fakeDao = object : PricingDao {
            override suspend fun getCachedQuote(requestKey: String): PriceQuoteCacheEntity? = null
            override fun getAllCachedQuotes(): Flow<List<PriceQuoteCacheEntity>> = flowOf(emptyList())
            override suspend fun upsertCachedQuote(quote: PriceQuoteCacheEntity): Long = 1L
            override suspend fun deleteCachedQuote(requestKey: String) {}
            override suspend fun clearQuoteCache() {}
        }

        val repo = LivePricingRepository(fakeDao, serviceBaseUrl = null)
        val request = QuoteRequest(
            category = "LODGING",
            locality = "Kyoto",
            tier = "luxury",
            partySize = 2,
            preferredCurrency = "USD"
        )
        val result = repo.quote(request)

        assertTrue("Empty base URL without cache must return Unavailable", result is QuoteResult.Unavailable)
        assertEquals("No pricing service is configured.", (result as QuoteResult.Unavailable).reason)
    }
}
