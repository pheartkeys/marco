package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * ============================================================================================
 * TRIP BRIEF — the synthesised output of a Pow Wow
 * ============================================================================================
 *
 * The brief is what the group agreed, what they don't agree on, and how each disagreement was
 * resolved. It is the one Pow Wow artefact that is group-visible (the transcripts behind it are
 * not — see `PowWowModels.kt`), and it is what every other surface consumes.
 *
 * Structured content travels as JSON in the `*Json` columns, matching the project's existing
 * pattern (`suggestedActionJson`, `motivationWeightsJson`). Use [TripBriefPayloads] to read and
 * write them; hand-rolling the JSON in three tracks would produce three incompatible shapes.
 *
 * Empty column = that section was not produced. It must render as absent. A brief with no
 * tensions means the synthesis found none, and a brief with no agreements is a brief that failed —
 * neither may be papered over with sample text.
 */
@Entity(
    tableName = "trip_briefs",
    indices = [Index("tripId"), Index("ideaId")]
)
data class TripBriefEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 0 while the brief still belongs to an unpromoted idea. */
    val tripId: Long = 0,
    /** 0 when the brief was generated directly on a trip. */
    val ideaId: Long = 0,
    /** Monotonic per trip, starting at 1. A re-synthesis writes a new row, it does not overwrite. */
    val version: Int = 1,
    val generatedAtTimestamp: Long = System.currentTimeMillis(),
    /** Comma-separated [PowWowSessionEntity.id]s this brief was synthesised from. "" = unknown provenance. */
    val sourceSessionIdsCsv: String = "",
    /** JSON array of [BriefAgreement]. "" = none produced. */
    val agreementsJson: String = "",
    /** JSON array of [BriefTension]. "" = none produced. */
    val tensionsJson: String = "",
    /** JSON array of [BriefResolution]. "" = none produced. */
    val resolutionsJson: String = "",
    /** JSON array of [BriefReadinessItem]. "" = readiness not evaluated. */
    val readinessJson: String = "",
    /** Prose summary shown at the top of the brief card. "" = none produced. */
    val summaryText: String = "",
    /** 0 = the group has not accepted this brief yet. */
    val acceptedAtTimestamp: Long = 0
)

/** A point every member's input supported. */
@JsonClass(generateAdapter = true)
data class BriefAgreement(
    /** Short statement of the agreement, e.g. "Everyone wants at least two rest days". */
    val statement: String,
    /** [TravelerEntity.id]s whose input supported it. Empty = provenance not recorded. */
    val supportingTravelerIds: List<Long> = emptyList(),
    /** Where this came from, e.g. "TRANSCRIPT", "DNA", "BOTH". "" = unrecorded. */
    val evidenceSource: String = ""
)

/**
 * A named disagreement. Never averaged away — the product's position is that a conflict is stated,
 * attributed, and resolved by the group, not smoothed into a midpoint.
 */
@JsonClass(generateAdapter = true)
data class BriefTension(
    /** Stable id used to link a [BriefResolution] back to this tension. */
    val tensionId: String,
    /** What the disagreement is about, e.g. "Pace: packed days vs. slow mornings". */
    val topic: String,
    /** One entry per differing position. */
    val positions: List<BriefPosition> = emptyList(),
    /** Why it matters, in one line. "" = not stated. */
    val stakes: String = ""
)

/** One side of a [BriefTension]. */
@JsonClass(generateAdapter = true)
data class BriefPosition(
    /** Whose position. 0 = unattributed. */
    val travelerId: Long = 0,
    val stance: String,
    /**
     * Supporting DNA signal, e.g. "Pacing: Slow 7". "" = no weighted-preference evidence, which is
     * different from weak evidence and must not be rendered as one.
     */
    val dnaEvidence: String = ""
)

/** A proposal for resolving a [BriefTension], and whether the group took it. */
@JsonClass(generateAdapter = true)
data class BriefResolution(
    /** Matches [BriefTension.tensionId]. */
    val tensionId: String,
    /** The proposed resolution. */
    val proposal: String,
    /** The DNA-backed reasoning behind the proposal. "" = none recorded. */
    val rationale: String = "",
    /** PROPOSED | ACCEPTED | REJECTED | SUPERSEDED. */
    val state: String = "PROPOSED",
    /** [TravelerEntity.id]s who accepted. Empty = nobody has yet. */
    val acceptedByTravelerIds: List<Long> = emptyList(),
    /** 0 = not accepted. */
    val acceptedAtTimestamp: Long = 0
)

/**
 * One readiness requirement derived from the brief (transport, lodging per night, a spine per day,
 * budget reconciles). [isSatisfied] false with [detail] blank means "still a gap" — render it as a
 * gap to fill, never as a plausible placeholder.
 */
@JsonClass(generateAdapter = true)
data class BriefReadinessItem(
    /** Stable key, e.g. "TRANSPORT_ORIGIN_TO_DESTINATION", "LODGING_PER_NIGHT". */
    val key: String,
    val label: String,
    val isSatisfied: Boolean = false,
    /** Whether an unmet item blocks PLANNING -> IN_PROGRESS. */
    val isCritical: Boolean = true,
    /** What satisfies it, when satisfied. "" = nothing recorded. */
    val detail: String = ""
)

/**
 * Encode/decode helpers for the JSON columns on [TripBriefEntity].
 *
 * Mirrors [PreferenceWeights]: an empty column decodes to an empty list, and an empty list encodes
 * back to "" rather than "[]", so "produced nothing" and "not produced" stay indistinguishable in
 * storage and both render as absent. Decoding never throws — malformed JSON yields an empty list,
 * because a half-parsed brief must not be presented as a whole one.
 */
object TripBriefPayloads {
    private val moshi = Moshi.Builder().build()

    private val agreementsAdapter: JsonAdapter<List<BriefAgreement>> =
        moshi.adapter(Types.newParameterizedType(List::class.java, BriefAgreement::class.java))
    private val tensionsAdapter: JsonAdapter<List<BriefTension>> =
        moshi.adapter(Types.newParameterizedType(List::class.java, BriefTension::class.java))
    private val resolutionsAdapter: JsonAdapter<List<BriefResolution>> =
        moshi.adapter(Types.newParameterizedType(List::class.java, BriefResolution::class.java))
    private val readinessAdapter: JsonAdapter<List<BriefReadinessItem>> =
        moshi.adapter(Types.newParameterizedType(List::class.java, BriefReadinessItem::class.java))

    fun encodeAgreements(items: List<BriefAgreement>): String =
        if (items.isEmpty()) "" else agreementsAdapter.toJson(items)

    fun decodeAgreements(json: String): List<BriefAgreement> =
        if (json.isBlank()) emptyList()
        else runCatching { agreementsAdapter.fromJson(json) }.getOrNull() ?: emptyList()

    fun encodeTensions(items: List<BriefTension>): String =
        if (items.isEmpty()) "" else tensionsAdapter.toJson(items)

    fun decodeTensions(json: String): List<BriefTension> =
        if (json.isBlank()) emptyList()
        else runCatching { tensionsAdapter.fromJson(json) }.getOrNull() ?: emptyList()

    fun encodeResolutions(items: List<BriefResolution>): String =
        if (items.isEmpty()) "" else resolutionsAdapter.toJson(items)

    fun decodeResolutions(json: String): List<BriefResolution> =
        if (json.isBlank()) emptyList()
        else runCatching { resolutionsAdapter.fromJson(json) }.getOrNull() ?: emptyList()

    fun encodeReadiness(items: List<BriefReadinessItem>): String =
        if (items.isEmpty()) "" else readinessAdapter.toJson(items)

    fun decodeReadiness(json: String): List<BriefReadinessItem> =
        if (json.isBlank()) emptyList()
        else runCatching { readinessAdapter.fromJson(json) }.getOrNull() ?: emptyList()

    /** Session ids a brief was built from. Empty list when provenance was not recorded. */
    fun decodeSourceSessionIds(csv: String): List<Long> =
        csv.split(',').mapNotNull { it.trim().toLongOrNull() }

    fun encodeSourceSessionIds(ids: List<Long>): String = ids.joinToString(",")
}
