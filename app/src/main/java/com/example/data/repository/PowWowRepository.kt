package com.example.data.repository

import com.example.data.local.PartyDao
import com.example.data.local.PowWowDao
import com.example.data.model.PowWowSessionEntity
import com.example.data.model.PowWowSessionState
import com.example.data.model.PowWowTranscriptEntity
import com.example.data.model.TravelerAgeBand
import com.example.data.model.TripBriefEntity
import com.example.data.model.hasRequiredConsent
import kotlinx.coroutines.flow.Flow

/**
 * Pow Wow sessions, transcripts, and briefs.
 *
 * Two rules are enforced here rather than in the UI, because a UI-only guarantee is one refactor
 * away from being no guarantee:
 *
 *  1. **Consent precedes recording.** [markRecordingStarted] throws unless [consentStatus] is
 *     [ConsentStatus.Granted]. A CHILD needs a guardian consent on top of their own.
 *  2. **Audio deletion is recorded.** [saveTranscriptAndConfirmAudioDeleted] is the normal way to
 *     finish a session: it stores the transcript, marks the session TRANSCRIBED, and stamps the
 *     deletion timestamp in one step, so a transcript cannot land without the deletion being
 *     recorded alongside it. The caller is still responsible for actually deleting the file — no
 *     column in this database ever points at it.
 */
interface PowWowRepository {

    // ---- Sessions ----------------------------------------------------------------------------

    fun observeSessionsForTrip(tripId: Long): Flow<List<PowWowSessionEntity>>

    fun observeSessionsForIdea(ideaId: Long): Flow<List<PowWowSessionEntity>>

    fun observeSession(sessionId: Long): Flow<PowWowSessionEntity?>

    suspend fun getSession(sessionId: Long): PowWowSessionEntity?

    suspend fun getLatestSessionFor(tripId: Long, travelerId: Long): PowWowSessionEntity?

    /** Creates a session in PENDING_CONSENT. Returns the new session id. */
    suspend fun createSession(session: PowWowSessionEntity): Long

    suspend fun updateSession(session: PowWowSessionEntity)

    // ---- Consent -----------------------------------------------------------------------------

    /**
     * Record the speaker's own consent (or, for a guest capture, the adult acting on their behalf).
     * Moves a PENDING_CONSENT session to READY once every required consent is present.
     */
    suspend fun recordConsent(
        sessionId: Long,
        grantedByTravelerId: Long,
        timestamp: Long = System.currentTimeMillis()
    )

    /** Record the guardian consent that a CHILD speaker additionally requires. */
    suspend fun recordGuardianConsent(
        sessionId: Long,
        guardianTravelerId: Long,
        timestamp: Long = System.currentTimeMillis()
    )

    /** Whether this session may record yet, and if not, why not. */
    suspend fun consentStatus(sessionId: Long): ConsentStatus

    // ---- Capture lifecycle -------------------------------------------------------------------

    /**
     * @throws IllegalStateException when consent is not fully recorded. Callers must check
     *         [consentStatus] first and show the consent gate; the throw is the backstop.
     */
    suspend fun markRecordingStarted(sessionId: Long, timestamp: Long = System.currentTimeMillis())

    /** Store the true recorded length. Never pass an estimate. */
    suspend fun markRecordingComplete(
        sessionId: Long,
        durationSeconds: Int,
        timestamp: Long = System.currentTimeMillis()
    )

    suspend fun markTranscribing(sessionId: Long)

    /**
     * The normal end of a session: persist the transcript, mark the session TRANSCRIBED, and stamp
     * the audio-deletion time. Call it *after* deleting the audio file.
     *
     * @return the new transcript row id.
     */
    suspend fun saveTranscriptAndConfirmAudioDeleted(
        transcript: PowWowTranscriptEntity,
        audioDeletedAtTimestamp: Long = System.currentTimeMillis()
    ): Long

    /**
     * Transcription failed. The audio must still be deleted, so this also stamps the deletion
     * timestamp; call it after deleting the file.
     */
    suspend fun markTranscriptionFailed(
        sessionId: Long,
        reason: String,
        audioDeletedAtTimestamp: Long = System.currentTimeMillis()
    )

    suspend fun discardSession(sessionId: Long, audioDeletedAtTimestamp: Long = System.currentTimeMillis())

    /**
     * Sessions that recorded audio but have no confirmed deletion. Should always be empty outside
     * an in-flight capture; anything lingering here is a retention bug worth surfacing.
     */
    suspend fun sessionsAwaitingAudioDeletion(): List<PowWowSessionEntity>

    // ---- Transcripts -------------------------------------------------------------------------

    fun observeTranscripts(sessionId: Long): Flow<List<PowWowTranscriptEntity>>

    suspend fun getLatestTranscript(sessionId: Long): PowWowTranscriptEntity?

    // ---- Briefs ------------------------------------------------------------------------------

    fun observeLatestBrief(tripId: Long): Flow<TripBriefEntity?>

    fun observeBriefs(tripId: Long): Flow<List<TripBriefEntity>>

    suspend fun getLatestBrief(tripId: Long): TripBriefEntity?

    suspend fun getLatestBriefForIdea(ideaId: Long): TripBriefEntity?

    /** Writes a new brief version. Never overwrite an existing one — history is the audit trail. */
    suspend fun insertBrief(brief: TripBriefEntity): Long

    suspend fun updateBrief(brief: TripBriefEntity)

    suspend fun markBriefAccepted(briefId: Long, timestamp: Long = System.currentTimeMillis())
}

/** Whether a session's consents are complete. */
sealed interface ConsentStatus {
    /** Every required consent is on file. */
    data object Granted : ConsentStatus

    /** Not cleared to record. [reason] is safe to show the user. */
    data class Missing(val reason: String) : ConsentStatus
}

/** Room-backed [PowWowRepository]. */
class RoomPowWowRepository(
    private val powWowDao: PowWowDao,
    private val partyDao: PartyDao
) : PowWowRepository {

    override fun observeSessionsForTrip(tripId: Long): Flow<List<PowWowSessionEntity>> =
        powWowDao.getSessionsForTrip(tripId)

    override fun observeSessionsForIdea(ideaId: Long): Flow<List<PowWowSessionEntity>> =
        powWowDao.getSessionsForIdea(ideaId)

    override fun observeSession(sessionId: Long): Flow<PowWowSessionEntity?> =
        powWowDao.getSessionById(sessionId)

    override suspend fun getSession(sessionId: Long): PowWowSessionEntity? =
        powWowDao.getSessionByIdSync(sessionId)

    override suspend fun getLatestSessionFor(tripId: Long, travelerId: Long): PowWowSessionEntity? =
        powWowDao.getLatestSessionFor(tripId, travelerId)

    override suspend fun createSession(session: PowWowSessionEntity): Long =
        powWowDao.insertSession(session)

    override suspend fun updateSession(session: PowWowSessionEntity) =
        powWowDao.updateSession(session)

    override suspend fun recordConsent(sessionId: Long, grantedByTravelerId: Long, timestamp: Long) {
        val session = powWowDao.getSessionByIdSync(sessionId) ?: return
        val updated = session.copy(
            consentGrantedAtTimestamp = timestamp,
            consentGrantedByTravelerId = grantedByTravelerId
        )
        powWowDao.updateSession(promoteToReadyIfConsentComplete(updated))
    }

    override suspend fun recordGuardianConsent(
        sessionId: Long,
        guardianTravelerId: Long,
        timestamp: Long
    ) {
        val session = powWowDao.getSessionByIdSync(sessionId) ?: return
        val updated = session.copy(
            guardianConsentGrantedAtTimestamp = timestamp,
            guardianConsentGrantedByTravelerId = guardianTravelerId
        )
        powWowDao.updateSession(promoteToReadyIfConsentComplete(updated))
    }

    override suspend fun consentStatus(sessionId: Long): ConsentStatus {
        val session = powWowDao.getSessionByIdSync(sessionId)
            ?: return ConsentStatus.Missing("This recording session no longer exists.")
        val band = speakerAgeBand(session)
            ?: return ConsentStatus.Missing("This traveler's age band hasn't been set yet.")
        if (session.consentGrantedAtTimestamp == 0L) {
            return ConsentStatus.Missing("Recording consent hasn't been given yet.")
        }
        if (band.requiresGuardianConsent && session.guardianConsentGrantedAtTimestamp == 0L) {
            return ConsentStatus.Missing("A parent or guardian needs to give consent first.")
        }
        return ConsentStatus.Granted
    }

    override suspend fun markRecordingStarted(sessionId: Long, timestamp: Long) {
        when (val status = consentStatus(sessionId)) {
            is ConsentStatus.Missing ->
                throw IllegalStateException("Cannot start a Pow Wow recording: ${status.reason}")
            ConsentStatus.Granted -> Unit
        }
        val session = powWowDao.getSessionByIdSync(sessionId) ?: return
        powWowDao.updateSession(
            session.copy(
                state = PowWowSessionState.RECORDING.value,
                startedAtTimestamp = timestamp
            )
        )
    }

    override suspend fun markRecordingComplete(
        sessionId: Long,
        durationSeconds: Int,
        timestamp: Long
    ) {
        val session = powWowDao.getSessionByIdSync(sessionId) ?: return
        powWowDao.updateSession(
            session.copy(
                state = PowWowSessionState.RECORDED.value,
                durationSeconds = durationSeconds,
                completedAtTimestamp = timestamp
            )
        )
    }

    override suspend fun markTranscribing(sessionId: Long) =
        powWowDao.updateSessionState(sessionId, PowWowSessionState.TRANSCRIBING.value)

    override suspend fun saveTranscriptAndConfirmAudioDeleted(
        transcript: PowWowTranscriptEntity,
        audioDeletedAtTimestamp: Long
    ): Long {
        val transcriptId = powWowDao.insertTranscript(transcript)
        powWowDao.updateSessionState(transcript.sessionId, PowWowSessionState.TRANSCRIBED.value)
        powWowDao.markAudioDeleted(transcript.sessionId, audioDeletedAtTimestamp)
        return transcriptId
    }

    override suspend fun markTranscriptionFailed(
        sessionId: Long,
        reason: String,
        audioDeletedAtTimestamp: Long
    ) {
        val session = powWowDao.getSessionByIdSync(sessionId) ?: return
        powWowDao.updateSession(
            session.copy(
                state = PowWowSessionState.FAILED.value,
                failureReason = reason,
                audioDeletedAtTimestamp = audioDeletedAtTimestamp
            )
        )
    }

    override suspend fun discardSession(sessionId: Long, audioDeletedAtTimestamp: Long) {
        val session = powWowDao.getSessionByIdSync(sessionId) ?: return
        powWowDao.updateSession(
            session.copy(
                state = PowWowSessionState.DISCARDED.value,
                audioDeletedAtTimestamp = audioDeletedAtTimestamp
            )
        )
        powWowDao.deleteTranscriptsForSession(sessionId)
    }

    override suspend fun sessionsAwaitingAudioDeletion(): List<PowWowSessionEntity> =
        powWowDao.getSessionsAwaitingAudioDeletion()

    override fun observeTranscripts(sessionId: Long): Flow<List<PowWowTranscriptEntity>> =
        powWowDao.getTranscriptsForSession(sessionId)

    override suspend fun getLatestTranscript(sessionId: Long): PowWowTranscriptEntity? =
        powWowDao.getLatestTranscriptForSession(sessionId)

    override fun observeLatestBrief(tripId: Long): Flow<TripBriefEntity?> =
        powWowDao.getLatestBriefForTrip(tripId)

    override fun observeBriefs(tripId: Long): Flow<List<TripBriefEntity>> =
        powWowDao.getBriefsForTrip(tripId)

    override suspend fun getLatestBrief(tripId: Long): TripBriefEntity? =
        powWowDao.getLatestBriefForTripSync(tripId)

    override suspend fun getLatestBriefForIdea(ideaId: Long): TripBriefEntity? =
        powWowDao.getLatestBriefForIdea(ideaId)

    override suspend fun insertBrief(brief: TripBriefEntity): Long = powWowDao.insertBrief(brief)

    override suspend fun updateBrief(brief: TripBriefEntity) = powWowDao.updateBrief(brief)

    override suspend fun markBriefAccepted(briefId: Long, timestamp: Long) =
        powWowDao.markBriefAccepted(briefId, timestamp)

    /**
     * The age band that governs consent: the speaker's own. A guest capture does not inherit the
     * capturing adult's band — that is the whole point of the guardian consent.
     */
    private suspend fun speakerAgeBand(session: PowWowSessionEntity): TravelerAgeBand? {
        val speaker = partyDao.getTravelerByIdSync(session.travelerId) ?: return null
        return TravelerAgeBand.fromStringOrNull(speaker.ageBand)
    }

    private suspend fun promoteToReadyIfConsentComplete(
        session: PowWowSessionEntity
    ): PowWowSessionEntity {
        if (session.state != PowWowSessionState.PENDING_CONSENT.value) return session
        val band = speakerAgeBand(session) ?: return session
        return if (session.hasRequiredConsent(band)) {
            session.copy(state = PowWowSessionState.READY.value)
        } else {
            session
        }
    }
}
