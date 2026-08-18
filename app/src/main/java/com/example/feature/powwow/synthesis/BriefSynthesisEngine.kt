package com.example.feature.powwow.synthesis

import com.example.data.model.BriefAgreement
import com.example.data.model.BriefResolution
import com.example.data.model.BriefTension
import com.example.data.model.TripBriefPayloads
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * One member's real, verbatim transcript text, used to ground [BriefSynthesisEngine] output.
 * Never fabricated upstream — [com.example.feature.powwow.synthesis.BriefSynthesizer] only builds
 * these from actual [com.example.data.model.PowWowTranscriptEntity] rows.
 */
data class TranscriptContribution(
    val travelerId: Long,
    val displayName: String,
    val transcriptText: String
)

/** What a synthesis attempt produced. All fields empty is a legitimate, honest result. */
data class BriefSynthesisResult(
    val agreements: List<BriefAgreement> = emptyList(),
    val tensions: List<BriefTension> = emptyList(),
    val resolutions: List<BriefResolution> = emptyList(),
    val summary: String = ""
) {
    companion object {
        val EMPTY = BriefSynthesisResult()
    }
}

/**
 * Turns real member transcripts (plus any real weighted-DNA evidence available for them) into
 * agreements, named tensions, and proposed resolutions.
 */
interface BriefSynthesisEngine {
    /**
     * @param contributions real member transcripts with non-blank text. Callers must filter out
     *        blank transcripts before calling — this engine is never asked to synthesize from
     *        nothing.
     * @param dnaClauses travelerId -> a real, pre-rendered [com.example.data.model.PreferenceWeights]
     *        clause. Only travelers with an entry here may be cited with DNA evidence in the
     *        result; anyone else must come back with `dnaEvidence == ""`.
     */
    suspend fun synthesize(
        contributions: List<TranscriptContribution>,
        dnaClauses: Map<Long, String>
    ): BriefSynthesisResult
}

/**
 * Real LLM-backed synthesis via Firebase AI Logic (Gemini) — no API key ships in the APK. The
 * model is given the actual transcripts and actual DNA clauses and instructed to ground every
 * agreement, tension, and citation in them; the response is then re-validated against those same
 * inputs before being trusted, so the model cannot invent a traveler id or a DNA clause even if it
 * tried. Any failure, empty response, or unparsable output degrades to
 * [BriefSynthesisResult.EMPTY] — never a canned brief.
 */
class FirebaseGeminiBriefSynthesisEngine(
    private val modelName: String = "gemini-3.5-flash"
) : BriefSynthesisEngine {

    override suspend fun synthesize(
        contributions: List<TranscriptContribution>,
        dnaClauses: Map<Long, String>
    ): BriefSynthesisResult = withContext(Dispatchers.IO) {
        if (contributions.isEmpty()) return@withContext BriefSynthesisResult.EMPTY

        try {
            val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(modelName)
            val promptText = buildPrompt(contributions, dnaClauses)
            val response = model.generateContent(promptText)

            val validTravelerIds = contributions.map { it.travelerId }.toSet()
            parseResponse(response.text.orEmpty(), validTravelerIds, dnaClauses)
        } catch (e: Exception) {
            BriefSynthesisResult.EMPTY
        }
    }

    private fun buildPrompt(contributions: List<TranscriptContribution>, dnaClauses: Map<Long, String>): String {
        val transcriptsBlock = contributions.joinToString("\n\n") { c ->
            val dnaLine = dnaClauses[c.travelerId]
                ?.let { "\nKnown weighted preference DNA for ${c.displayName} (travelerId ${c.travelerId}): $it" }
                .orEmpty()
            "Traveler \"${c.displayName}\" (travelerId ${c.travelerId}) said, verbatim:\n" +
                "\"\"\"${c.transcriptText}\"\"\"$dnaLine"
        }

        return """
            You are reconciling a small travel group's individually recorded trip-planning brain
            dumps into a shared brief. Below are the real, verbatim transcripts of what each member
            actually said, plus any real weighted travel-preference data ("DNA") known for members
            who have it.

            $transcriptsBlock

            Respond with ONLY a single JSON object — no prose, no markdown code fences — in exactly
            this shape:
            {
              "agreements": [ { "statement": string, "supportingTravelerIds": [number], "evidenceSource": "TRANSCRIPT" } ],
              "tensions": [ { "tensionId": string, "topic": string, "stakes": string,
                  "positions": [ { "travelerId": number, "stance": string, "dnaEvidence": string } ] } ],
              "resolutions": [ { "tensionId": string, "proposal": string, "rationale": string, "state": "PROPOSED" } ],
              "summary": string
            }

            Hard rules — follow these exactly:
            - Only state an agreement when at least two members' transcripts actually support it.
              List their real travelerIds in supportingTravelerIds.
            - Only state a tension when the transcripts show a real, stated difference of
              preference. Attribute each position to the real travelerId who said it, and make
              "stance" a close paraphrase of what that person actually said — never a guess at what
              they might think.
            - "dnaEvidence" for a position must be copied EXACTLY from the DNA clause given above
              for that travelerId, or be "" if none was given for them. Never invent a DNA clause
              and never attach DNA evidence to a traveler it was not given for above.
            - Never invent a travelerId that is not listed above.
            - If nothing genuinely worth reporting exists, return empty arrays for "agreements",
              "tensions", and "resolutions", and an empty "summary". An empty result is the correct
              and expected output when the transcripts don't support more — do not invent filler
              content to avoid returning an empty result.
        """.trimIndent()
    }

    private fun parseResponse(
        raw: String,
        validTravelerIds: Set<Long>,
        dnaClauses: Map<Long, String>
    ): BriefSynthesisResult {
        val jsonText = extractJsonObject(raw) ?: return BriefSynthesisResult.EMPTY
        val json = runCatching { JSONObject(jsonText) }.getOrNull() ?: return BriefSynthesisResult.EMPTY

        val agreements = TripBriefPayloads.decodeAgreements(json.optJSONArray("agreements")?.toString().orEmpty())
            .map { agreement ->
                agreement.copy(
                    supportingTravelerIds = agreement.supportingTravelerIds.filter { it in validTravelerIds }
                )
            }
            .filter { it.statement.isNotBlank() && it.supportingTravelerIds.size >= 2 }

        val tensions = TripBriefPayloads.decodeTensions(json.optJSONArray("tensions")?.toString().orEmpty())
            .map { tension ->
                val positions = tension.positions
                    .filter { it.travelerId in validTravelerIds && it.stance.isNotBlank() }
                    .map { position ->
                        val allowedDna = dnaClauses[position.travelerId].orEmpty()
                        if (position.dnaEvidence.isNotBlank() && position.dnaEvidence != allowedDna) {
                            position.copy(dnaEvidence = "")
                        } else {
                            position
                        }
                    }
                tension.copy(positions = positions)
            }
            .filter { it.topic.isNotBlank() && it.positions.size >= 2 }

        val validTensionIds = tensions.map { it.tensionId }.toSet()
        val resolutions = TripBriefPayloads.decodeResolutions(json.optJSONArray("resolutions")?.toString().orEmpty())
            .filter { it.tensionId in validTensionIds && it.proposal.isNotBlank() }

        val summary = json.optString("summary", "")

        return BriefSynthesisResult(
            agreements = agreements,
            tensions = tensions,
            resolutions = resolutions,
            summary = summary
        )
    }

    /** The model is asked for raw JSON but may still wrap it in prose or code fences; extract defensively. */
    private fun extractJsonObject(raw: String): String? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start == -1 || end == -1 || end < start) return null
        return raw.substring(start, end + 1)
    }
}
