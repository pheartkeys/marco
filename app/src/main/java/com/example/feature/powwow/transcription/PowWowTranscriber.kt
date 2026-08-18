package com.example.feature.powwow.transcription

import com.example.data.model.PowWowTranscriptEntity
import com.example.data.repository.PowWowRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Transcribes audio for a Pow Wow session and enforces the mandatory zero-audio storage sequence:
 *
 * transcribe -> delete audio file -> [PowWowRepository.saveTranscriptAndConfirmAudioDeleted]
 *
 * The raw audio file is deleted on every path out of this function — success or failure — because
 * it must never outlive this call. On failure, [PowWowRepository.markTranscriptionFailed] records
 * the honest reason; no text is ever substituted for a failed or unavailable transcription. An
 * empty transcript on success (no intelligible speech) is a legitimate outcome, not a failure.
 */
class PowWowTranscriber(
    private val powWowRepository: PowWowRepository,
    private val transcriptionEngine: AudioTranscriptionEngine = FirebaseGeminiTranscriptionEngine()
) {

    suspend fun transcribeAndPurgeAudio(
        sessionId: Long,
        audioFile: File
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Mark state as transcribing
            powWowRepository.markTranscribing(sessionId)

            // Real transcription: Firebase AI Logic proxying Gemini over the actual recorded audio.
            val outcome = transcriptionEngine.transcribe(audioFile)

            // MUST delete raw audio file before completing the audit record, regardless of outcome.
            if (audioFile.exists()) {
                audioFile.delete()
            }

            when (outcome) {
                is TranscriptionOutcome.Success -> {
                    val wordCount = outcome.transcriptText.split("\\s+".toRegex()).count { it.isNotBlank() }

                    val transcriptEntity = PowWowTranscriptEntity(
                        sessionId = sessionId,
                        transcriptText = outcome.transcriptText,
                        transcriptionSource = outcome.transcriptionSource,
                        transcriptionModel = outcome.transcriptionModel,
                        languageTag = outcome.languageTag,
                        wordCount = wordCount
                    )

                    // Record transcript and stamp audioDeletedAtTimestamp
                    powWowRepository.saveTranscriptAndConfirmAudioDeleted(
                        transcript = transcriptEntity,
                        audioDeletedAtTimestamp = System.currentTimeMillis()
                    )

                    Result.success(outcome.transcriptText)
                }

                is TranscriptionOutcome.Failure -> {
                    powWowRepository.markTranscriptionFailed(sessionId, outcome.reason)
                    Result.failure(TranscriptionFailedException(outcome.reason))
                }
            }
        } catch (e: Exception) {
            // Guarantee audio deletion even on an unexpected exception.
            if (audioFile.exists()) {
                audioFile.delete()
            }
            powWowRepository.markTranscriptionFailed(sessionId, e.message ?: "Transcription failed")
            Result.failure(e)
        }
    }
}

/** Carries an honest transcription failure reason through [Result.failure]. */
class TranscriptionFailedException(reason: String) : Exception(reason)
