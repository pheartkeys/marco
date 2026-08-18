package com.example.feature.powwow.transcription

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.TextPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Outcome of one attempt to turn a recorded Pow Wow audio file into text.
 *
 * There is no third option and no middle ground. A transcription attempt either produces the
 * model's real output — [Success], where an empty [Success.transcriptText] is a legitimate result
 * ("no intelligible speech"), not an error — or it [Failure]s outright. Nothing here may ever
 * substitute plausible-sounding text for either case.
 */
sealed interface TranscriptionOutcome {
    /**
     * @param transcriptText the model's actual output, verbatim. "" is a legitimate, honest result.
     * @param transcriptionSource what produced it, e.g. "FIREBASE_AI_GEMINI". Only ever the real
     *        engine identifier — never stamped on text that engine did not produce.
     * @param transcriptionModel the model/version string used, so a surface can show provenance.
     * @param languageTag BCP-47 tag if known. "" = unreported; never guess "en".
     */
    data class Success(
        val transcriptText: String,
        val transcriptionSource: String,
        val transcriptionModel: String = "",
        val languageTag: String = ""
    ) : TranscriptionOutcome

    /**
     * Transcription could not be completed — no network, no Firebase config, a blocked or errored
     * model response, a missing/empty file, or anything else. [reason] is safe to log/show. The
     * caller must never substitute text for this; it must call
     * [com.example.data.repository.PowWowRepository.markTranscriptionFailed].
     */
    data class Failure(val reason: String) : TranscriptionOutcome
}

/** Turns one recorded Pow Wow audio file into a [TranscriptionOutcome]. */
interface AudioTranscriptionEngine {
    suspend fun transcribe(audioFile: File): TranscriptionOutcome
}

/**
 * Real transcription via Firebase AI Logic, which proxies Gemini so no API key ships in the APK.
 * Sends the recorded audio inline to the model and returns its actual transcript.
 *
 * Any failure along the way — no network, no Firebase configuration, a blocked/errored response,
 * a missing or empty file — is surfaced as [TranscriptionOutcome.Failure]. This class never
 * fabricates a transcript: an honest failure is always preferred to a plausible-looking guess, and
 * [TranscriptionOutcome.Success.transcriptionSource] is only ever set here, on text this engine
 * actually produced.
 */
class FirebaseGeminiTranscriptionEngine(
    private val modelName: String = "gemini-3.5-flash"
) : AudioTranscriptionEngine {

    override suspend fun transcribe(audioFile: File): TranscriptionOutcome = withContext(Dispatchers.IO) {
        if (!audioFile.exists() || audioFile.length() == 0L) {
            return@withContext TranscriptionOutcome.Failure("No audio was recorded.")
        }

        try {
            val audioBytes = audioFile.readBytes()
            val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(modelName)

            val prompt = Content(
                role = "user",
                parts = listOf(
                    InlineDataPart(audioBytes, AUDIO_MIME_TYPE),
                    TextPart(TRANSCRIPTION_INSTRUCTION)
                )
            )

            val response = model.generateContent(prompt)
            val transcriptText = response.text?.trim().orEmpty()

            TranscriptionOutcome.Success(
                transcriptText = transcriptText,
                transcriptionSource = "FIREBASE_AI_GEMINI",
                transcriptionModel = modelName
            )
        } catch (e: Exception) {
            val reason = e.message?.takeIf { it.isNotBlank() }
                ?: (e::class.simpleName ?: "Unknown error")
            TranscriptionOutcome.Failure("Transcription failed: $reason")
        }
    }

    private companion object {
        /** The capture layer records AAC audio in an MPEG-4 (.m4a) container. */
        const val AUDIO_MIME_TYPE = "audio/mp4"

        const val TRANSCRIPTION_INSTRUCTION =
            "Transcribe the spoken audio verbatim, in the language it was spoken in. Output only " +
                "the transcript text itself, with no commentary, labels, timestamps, or formatting. " +
                "If the audio contains no intelligible speech, output nothing."
    }
}
