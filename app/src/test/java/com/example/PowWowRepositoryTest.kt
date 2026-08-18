package com.example

import com.example.data.local.PartyDao
import com.example.data.local.PowWowDao
import com.example.data.model.IdeaEntity
import com.example.data.model.PartyUnitEntity
import com.example.data.model.PowWowSessionEntity
import com.example.data.model.PowWowSessionState
import com.example.data.model.PowWowTranscriptEntity
import com.example.data.model.TravelerAgeBand
import com.example.data.model.TravelerEntity
import com.example.data.model.TripBriefEntity
import com.example.data.model.TripMembershipEntity
import com.example.data.model.hasRequiredConsent
import com.example.data.repository.ConsentStatus
import com.example.data.repository.RoomPowWowRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PowWowRepositoryTest {

    private class FakePartyDao : PartyDao {
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

    private class FakePowWowDao : PowWowDao {
        val sessions = mutableMapOf<Long, PowWowSessionEntity>()
        val transcripts = mutableMapOf<Long, PowWowTranscriptEntity>()
        val briefs = mutableMapOf<Long, TripBriefEntity>()
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

    private lateinit var partyDao: FakePartyDao
    private lateinit var dao: FakePowWowDao
    private lateinit var repo: RoomPowWowRepository

    @Before
    fun setUp() = runBlocking {
        partyDao = FakePartyDao()
        partyDao.insertTraveler(TravelerEntity(id = 1, displayName = "Alice", ageBand = TravelerAgeBand.ADULT.value))
        partyDao.insertTraveler(TravelerEntity(id = 2, displayName = "Billy", ageBand = TravelerAgeBand.CHILD.value, guardianTravelerId = 1))

        dao = FakePowWowDao()
        repo = RoomPowWowRepository(dao, partyDao)
    }

    @Test
    fun `session starts in PENDING_CONSENT and requires explicit consent`() = runBlocking {
        val sessionId = repo.createSession(
            PowWowSessionEntity(
                tripId = 10,
                travelerId = 1,
                state = PowWowSessionState.PENDING_CONSENT.value
            )
        )

        val session = repo.getSession(sessionId)
        assertNotNull(session)
        assertEquals(PowWowSessionState.PENDING_CONSENT.value, session?.state)
        assertTrue(repo.consentStatus(sessionId) is ConsentStatus.Missing)
        assertFalse(session?.hasRequiredConsent(TravelerAgeBand.ADULT) ?: true)

        // Grant consent
        repo.recordConsent(sessionId, grantedByTravelerId = 1L)
        val updated = repo.getSession(sessionId)
        assertEquals(PowWowSessionState.READY.value, updated?.state)
        assertTrue(repo.consentStatus(sessionId) is ConsentStatus.Granted)
        assertTrue(updated?.hasRequiredConsent(TravelerAgeBand.ADULT) ?: false)
    }

    @Test
    fun `CHILD session requires both member consent and guardian consent`() = runBlocking {
        val sessionId = repo.createSession(
            PowWowSessionEntity(
                tripId = 10,
                travelerId = 2,
                state = PowWowSessionState.PENDING_CONSENT.value
            )
        )

        assertTrue(repo.consentStatus(sessionId) is ConsentStatus.Missing)

        // Grant member consent only -> still missing guardian
        repo.recordConsent(sessionId, grantedByTravelerId = 2L)
        assertTrue(repo.consentStatus(sessionId) is ConsentStatus.Missing)

        // Grant guardian consent -> fully Granted
        repo.recordGuardianConsent(sessionId, guardianTravelerId = 1L)
        assertTrue(repo.consentStatus(sessionId) is ConsentStatus.Granted)
    }

    @Test
    fun `recording without consent throws IllegalStateException`() = runBlocking {
        val sessionId = repo.createSession(
            PowWowSessionEntity(
                tripId = 10,
                travelerId = 1,
                state = PowWowSessionState.PENDING_CONSENT.value
            )
        )

        var threw = false
        try {
            repo.markRecordingStarted(sessionId)
        } catch (_: IllegalStateException) {
            threw = true
        }
        assertTrue("Expected markRecordingStarted without consent to throw", threw)
    }

    @Test
    fun `saveTranscriptAndConfirmAudioDeleted stores transcript and records audit timestamp`() = runBlocking {
        val sessionId = repo.createSession(
            PowWowSessionEntity(
                tripId = 10,
                travelerId = 1,
                state = PowWowSessionState.PENDING_CONSENT.value
            )
        )
        repo.recordConsent(sessionId, grantedByTravelerId = 1L)
        repo.markRecordingStarted(sessionId)
        repo.markRecordingComplete(sessionId, 45)

        repo.saveTranscriptAndConfirmAudioDeleted(
            transcript = PowWowTranscriptEntity(
                sessionId = sessionId,
                transcriptText = "Looking for unhurried dinners and morning hikes.",
                wordCount = 7
            ),
            audioDeletedAtTimestamp = 1700000000000L
        )

        val session = repo.getSession(sessionId)
        assertEquals(PowWowSessionState.TRANSCRIBED.value, session?.state)
        assertEquals(1700000000000L, session?.audioDeletedAtTimestamp)

        val transcript = repo.getLatestTranscript(sessionId)
        assertNotNull(transcript)
        assertEquals(7, transcript?.wordCount)
    }
}
