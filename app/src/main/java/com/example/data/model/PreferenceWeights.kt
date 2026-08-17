package com.example.data.model

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * Encode/decode helpers for the weighted-preference JSON fields on [UserPreferenceEntity]
 * (motivationWeightsJson, travelStyleWeightsJson, pacingWeightsJson, comfortWeightsJson,
 * loyaltyRankJson).
 *
 * An option the user never rated is simply absent from the map — never a 0, never a manufactured
 * midpoint. That distinction (unrated vs. a deliberate low score) is the entire point of this
 * type; collapsing it back into a default value anywhere in this file would reintroduce the same
 * fabrication defect these weights exist to fix.
 */
object PreferenceWeights {
    private val moshi = Moshi.Builder().build()

    private val weightsType = Types.newParameterizedType(Map::class.java, String::class.java, Int::class.javaObjectType)
    private val weightsAdapter: JsonAdapter<Map<String, Int>> = moshi.adapter(weightsType)

    private val rankType = Types.newParameterizedType(
        Map::class.java,
        String::class.java,
        Types.newParameterizedType(List::class.java, String::class.java)
    )
    private val rankAdapter: JsonAdapter<Map<String, List<String>>> = moshi.adapter(rankType)

    fun encodeWeights(weights: Map<String, Int>): String =
        if (weights.isEmpty()) "" else weightsAdapter.toJson(weights)

    fun decodeWeights(json: String): Map<String, Int> =
        if (json.isBlank()) emptyMap() else (runCatching { weightsAdapter.fromJson(json) }.getOrNull() ?: emptyMap())

    fun encodeRanks(ranks: Map<String, List<String>>): String =
        if (ranks.isEmpty()) "" else rankAdapter.toJson(ranks)

    fun decodeRanks(json: String): Map<String, List<String>> =
        if (json.isBlank()) emptyMap() else (runCatching { rankAdapter.fromJson(json) }.getOrNull() ?: emptyMap())

    /** Key of the highest-weighted entry, or null if nothing in the group was rated. */
    fun <K> topKey(weights: Map<K, Int>): K? = weights.maxByOrNull { it.value }?.key

    /**
     * Renders a weighted group as a legible clause for Gemini prompt context, strongest first —
     * e.g. "Motivation: Explore 7, Escape 6, Work 1". Returns null (never a placeholder sentence)
     * when nothing in the group was rated, so the caller omits the clause entirely instead of
     * describing an unrated group as if it meant something.
     */
    fun formatWeightsClause(groupLabel: String, labeled: Map<String, Int>): String? {
        if (labeled.isEmpty()) return null
        val ranked = labeled.entries
            .sortedByDescending { it.value }
            .joinToString(", ") { "${it.key} ${it.value}" }
        return "$groupLabel: $ranked"
    }

    /**
     * Phrases a single accessibility comfort rating for the model: a high weight (6-7) reads as a
     * hard filter/requirement, mid (4-5) as a strong preference, low (1-3) as a soft nice-to-have.
     * Using the same "prefers" sentence at every score would make the 1-7 scale pointless once it
     * reaches the prompt.
     */
    fun accessibilityIntensityPhrase(label: String, weight: Int): String {
        val framing = when {
            weight >= 6 -> "HARD REQUIREMENT — filter out venues/itineraries that don't meet this"
            weight >= 4 -> "strong preference"
            else -> "soft, nice-to-have preference"
        }
        return "$label ($weight/7, $framing)"
    }
}
