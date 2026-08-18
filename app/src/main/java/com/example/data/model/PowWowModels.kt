package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ============================================================================================
 * POW WOW — one recorded brain dump per member, analysed together
 * ============================================================================================
 *
 * ## There is deliberately nowhere to store audio.
 *
 * [PowWowSessionEntity] has no blob column, no file path, and no URI. This is not an oversight and
 * must not be "fixed" by a later track. Policy: raw audio is deleted as soon as transcription
 * succeeds, and under-14 guest capture makes that a legal obligation rather than a nicety. The
 * recorder keeps its temp file path in memory in the capture layer, writes the transcript to
 * [PowWowTranscriptEntity], deletes the file, and stamps [PowWowSessionEntity.audioDeletedAtTimestamp].
 * A path column would survive process death, get synced, get backed up, and outlive the file it
 * names — so the column does not exist.
 *
 * ## Consent is a precondition, not a flag to tidy up afterwards.
 *
 * A session may not start recording until consent is recorded. `consentGrantedAtTimestamp == 0`
 * means no consent exists. For a CHILD ([TravelerAgeBand.requiresGuardianConsent]) a second,
 * separate guardian timestamp is required as well — see [PowWowSessionEntity.hasRequiredConsent].
 *
 * ## Transcripts are private by default.
 *
 * A member's unedited brain dump is not group content. Transcripts sync under
 * `/users/{uid}/powWowSessions/...`; only the synthesised [TripBriefEntity] is published to the
 * shared trip document. `firestore.rules` denies the `powWowTranscripts` subcollection under
 * `/trips/{tripId}` outright.
 */

/** Capture/transcription lifecycle of one session. */
enum class PowWowSessionState(val value: String) {
    /** Created, consent not yet recorded. Recording is blocked in this state. */
    PENDING_CONSENT("PENDING_CONSENT"),
    /** Consent recorded, ready to record. */
    READY("READY"),
    RECORDING("RECORDING"),
    /** Audio captured, transcription not yet attempted. Audio still exists on disk in this state only. */
    RECORDED("RECORDED"),
    TRANSCRIBING("TRANSCRIBING"),
    /** Transcript stored and raw audio deleted. Terminal success. */
    TRANSCRIBED("TRANSCRIBED"),
    /** Transcription failed. Audio must still be deleted; the member re-records. */
    FAILED("FAILED"),
    /** Member discarded the session. Terminal. */
    DISCARDED("DISCARDED");

    companion object {
        fun fromStringOrNull(value: String): PowWowSessionState? =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
    }
}

/** Product limits on a Pow Wow recording. Enforced by the capture layer; stated here so all tracks agree. */
object PowWowLimits {
    const val MIN_DURATION_SECONDS: Int = 30
    const val MAX_DURATION_SECONDS: Int = 300
}

/**
 * One member's Pow Wow session.
 *
 * Anchored to a trip **or** an idea: the ritual usually runs while the group is still EXPLORING,
 * so [ideaId] is set and [tripId] is 0 until promotion. Exactly one of the two is normally
 * non-zero; both may be set after promotion backfills [tripId].
 */
@Entity(
    tableName = "pow_wow_sessions",
    indices = [Index("tripId"), Index("ideaId"), Index("travelerId")]
)
data class PowWowSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 0 = the session belongs to an idea that has not been promoted yet. */
    val tripId: Long = 0,
    /** 0 = the session was started directly on a trip. */
    val ideaId: Long = 0,
    /** Whose brain dump this is. Required. */
    val travelerId: Long,
    /**
     * For guest capture (typically a CHILD speaking into a parent's phone): the adult who held the
     * device. 0 = the traveller captured it themselves.
     */
    val capturedOnBehalfByTravelerId: Long = 0,
    /** [PowWowSessionState.value]. Required. */
    val state: String,
    /** Actual recorded length. 0 = nothing recorded yet. Never estimate this. */
    val durationSeconds: Int = 0,
    val startedAtTimestamp: Long = 0,
    /** 0 = not finished. */
    val completedAtTimestamp: Long = 0,
    /**
     * When the speaking traveller (or, for a guest capture, the adult on their behalf) granted
     * consent. **0 = no consent recorded; recording must not start.**
     */
    val consentGrantedAtTimestamp: Long = 0,
    /** Who granted it. 0 = nobody has. */
    val consentGrantedByTravelerId: Long = 0,
    /**
     * Separate guardian consent, required when the speaker's age band is CHILD.
     * 0 = not granted.
     */
    val guardianConsentGrantedAtTimestamp: Long = 0,
    /** The guardian who granted it. 0 = none. */
    val guardianConsentGrantedByTravelerId: Long = 0,
    /**
     * When the raw audio file was confirmed deleted. 0 = deletion not yet confirmed.
     * Anything reporting on retention reads this field — it is the only record that the file is
     * gone, because the file itself is never referenced from the database.
     */
    val audioDeletedAtTimestamp: Long = 0,
    /** Identifier of the prompt rail the member heard, so a brief can be read in context. "" = unknown. */
    val promptRailVersion: String = "",
    /** Populated only on FAILED. "" = no failure. */
    val failureReason: String = ""
)

/**
 * Are all consents this session needs on file?
 *
 * @param speakerAgeBand the speaking traveller's [TravelerAgeBand], or null if it was never
 *        declared. A null band returns false: an undeclared age cannot be assumed to be an adult.
 */
fun PowWowSessionEntity.hasRequiredConsent(speakerAgeBand: TravelerAgeBand?): Boolean {
    if (consentGrantedAtTimestamp == 0L) return false
    if (speakerAgeBand == null) return false
    if (speakerAgeBand.requiresGuardianConsent && guardianConsentGrantedAtTimestamp == 0L) return false
    return true
}

/** True once raw audio deletion has been confirmed for this session. */
fun PowWowSessionEntity.isAudioConfirmedDeleted(): Boolean = audioDeletedAtTimestamp != 0L

/**
 * The text of one session.
 *
 * Separate from the session so a session row can exist (and prove consent and deletion) with no
 * transcript at all, and so a re-transcription can be added without touching capture metadata.
 */
@Entity(
    tableName = "pow_wow_transcripts",
    indices = [Index("sessionId")]
)
data class PowWowTranscriptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** [PowWowSessionEntity.id]. Required. */
    val sessionId: Long,
    /** The transcript. "" is valid — a session that produced no intelligible speech. */
    val transcriptText: String = "",
    /** BCP-47 tag if the transcriber reported one. "" = unreported; do not default to "en". */
    val languageTag: String = "",
    /** What produced it, e.g. "FIREBASE_AI_GEMINI". "" = unrecorded. */
    val transcriptionSource: String = "",
    /** Model/version string when reported. "" = unrecorded. */
    val transcriptionModel: String = "",
    val transcribedAtTimestamp: Long = System.currentTimeMillis(),
    /**
     * Word count as reported by the transcriber, 0 when not reported. Present so surfaces can show
     * how much material a member actually gave without re-deriving it inconsistently.
     */
    val wordCount: Int = 0
)
