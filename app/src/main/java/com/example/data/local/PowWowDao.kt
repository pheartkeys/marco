package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PowWowSessionEntity
import com.example.data.model.PowWowTranscriptEntity
import com.example.data.model.TripBriefEntity
import kotlinx.coroutines.flow.Flow

/**
 * Pow Wow sessions, transcripts, and the briefs synthesised from them.
 *
 * Note what is absent: there is no query that reads or writes audio, because no column holds it.
 * The capture layer holds the temp file path in memory and calls
 * [markAudioDeleted] once the file is gone.
 */
@Dao
interface PowWowDao {

    // ---- Sessions ----------------------------------------------------------------------------

    @Query("SELECT * FROM pow_wow_sessions WHERE tripId = :tripId ORDER BY id ASC")
    fun getSessionsForTrip(tripId: Long): Flow<List<PowWowSessionEntity>>

    @Query("SELECT * FROM pow_wow_sessions WHERE ideaId = :ideaId ORDER BY id ASC")
    fun getSessionsForIdea(ideaId: Long): Flow<List<PowWowSessionEntity>>

    @Query("SELECT * FROM pow_wow_sessions WHERE id = :sessionId LIMIT 1")
    fun getSessionById(sessionId: Long): Flow<PowWowSessionEntity?>

    @Query("SELECT * FROM pow_wow_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionByIdSync(sessionId: Long): PowWowSessionEntity?

    @Query("SELECT * FROM pow_wow_sessions WHERE tripId = :tripId AND travelerId = :travelerId ORDER BY id DESC LIMIT 1")
    suspend fun getLatestSessionFor(tripId: Long, travelerId: Long): PowWowSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: PowWowSessionEntity): Long

    @Update
    suspend fun updateSession(session: PowWowSessionEntity)

    @Query("UPDATE pow_wow_sessions SET state = :state WHERE id = :sessionId")
    suspend fun updateSessionState(sessionId: Long, state: String)

    /**
     * Stamp the moment the raw audio file was confirmed deleted. This is the only durable record
     * that deletion happened, since the file was never referenced from the database.
     */
    @Query("UPDATE pow_wow_sessions SET audioDeletedAtTimestamp = :timestamp WHERE id = :sessionId")
    suspend fun markAudioDeleted(sessionId: Long, timestamp: Long)

    /** Sessions that captured audio but have not confirmed its deletion — the retention audit query. */
    @Query("SELECT * FROM pow_wow_sessions WHERE durationSeconds > 0 AND audioDeletedAtTimestamp = 0")
    suspend fun getSessionsAwaitingAudioDeletion(): List<PowWowSessionEntity>

    @Query("DELETE FROM pow_wow_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    // ---- Transcripts -------------------------------------------------------------------------

    @Query("SELECT * FROM pow_wow_transcripts WHERE sessionId = :sessionId ORDER BY id DESC")
    fun getTranscriptsForSession(sessionId: Long): Flow<List<PowWowTranscriptEntity>>

    @Query("SELECT * FROM pow_wow_transcripts WHERE sessionId = :sessionId ORDER BY id DESC LIMIT 1")
    suspend fun getLatestTranscriptForSession(sessionId: Long): PowWowTranscriptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscript(transcript: PowWowTranscriptEntity): Long

    @Query("DELETE FROM pow_wow_transcripts WHERE sessionId = :sessionId")
    suspend fun deleteTranscriptsForSession(sessionId: Long)

    // ---- Briefs ------------------------------------------------------------------------------

    @Query("SELECT * FROM trip_briefs WHERE tripId = :tripId ORDER BY version DESC")
    fun getBriefsForTrip(tripId: Long): Flow<List<TripBriefEntity>>

    @Query("SELECT * FROM trip_briefs WHERE tripId = :tripId ORDER BY version DESC LIMIT 1")
    fun getLatestBriefForTrip(tripId: Long): Flow<TripBriefEntity?>

    @Query("SELECT * FROM trip_briefs WHERE tripId = :tripId ORDER BY version DESC LIMIT 1")
    suspend fun getLatestBriefForTripSync(tripId: Long): TripBriefEntity?

    @Query("SELECT * FROM trip_briefs WHERE ideaId = :ideaId ORDER BY version DESC LIMIT 1")
    suspend fun getLatestBriefForIdea(ideaId: Long): TripBriefEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrief(brief: TripBriefEntity): Long

    @Update
    suspend fun updateBrief(brief: TripBriefEntity)

    @Query("UPDATE trip_briefs SET acceptedAtTimestamp = :timestamp WHERE id = :briefId")
    suspend fun markBriefAccepted(briefId: Long, timestamp: Long)

    // ---- Wipe --------------------------------------------------------------------------------

    @Query("DELETE FROM pow_wow_sessions")
    suspend fun clearSessions()

    @Query("DELETE FROM pow_wow_transcripts")
    suspend fun clearTranscripts()

    @Query("DELETE FROM trip_briefs")
    suspend fun clearBriefs()
}
