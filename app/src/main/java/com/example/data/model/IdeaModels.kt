package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A disposable research record — the `EXPLORING` stage, ahead of `PLANNING`.
 *
 * Ideas are cheap and meant to be thrown away. Half of them are a destination and nothing else.
 * Every field except [createdAtTimestamp] may legitimately be empty, and an empty field renders as
 * absent: no placeholder destination, no invented window, no filled-in "why".
 *
 * An idea becomes a trip through the Pow Wow. Promotion sets [promotedTripId] to the new
 * [TripEntity.id] and stamps [promotedAtTimestamp]; the idea row is kept as provenance, and the
 * trip points back via [TripEntity.originIdeaId].
 */
@Entity(
    tableName = "ideas",
    indices = [Index("promotedTripId"), Index("createdByTravelerId")]
)
data class IdeaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** "" = the idea has no destination yet ("somewhere warm in February" is a window, not a place). */
    val destination: String = "",
    val countryCode: String = "",
    /**
     * The window exactly as the human expressed it — "late spring", "school holidays", "any time
     * after the merger closes". This is the authoritative field for display. "" = no window given.
     */
    val roughWindowLabel: String = "",
    /**
     * ISO-8601 (yyyy-MM-dd) bounds, populated only when the window was genuinely normalisable.
     * "" = not normalisable, which is the common case. Never derive these from a vague label.
     */
    val roughWindowStartIso: String = "",
    val roughWindowEndIso: String = "",
    /** Why this idea exists, in the traveller's words. "" = not given. */
    val why: String = "",
    /** Free-form notes / links gathered while researching. "" = none. */
    val notes: String = "",
    /** Who had the idea. 0 = unknown. */
    val createdByTravelerId: Long = 0,
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val lastUpdatedTimestamp: Long = System.currentTimeMillis(),
    /** The [TripEntity.id] this idea became. 0 = still just an idea. */
    val promotedTripId: Long = 0,
    /** 0 = not promoted. */
    val promotedAtTimestamp: Long = 0,
    /** 0 = not discarded. A discarded idea is hidden, not deleted, so provenance survives. */
    val discardedAtTimestamp: Long = 0
)

/** Still live: neither promoted into a trip nor discarded. */
fun IdeaEntity.isActive(): Boolean = promotedTripId == 0L && discardedAtTimestamp == 0L

/** Promoted into a real trip. */
fun IdeaEntity.isPromoted(): Boolean = promotedTripId != 0L
