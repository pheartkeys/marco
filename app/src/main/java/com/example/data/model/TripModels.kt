package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The trip and its lifecycle.
 *
 * A trip is the shared object: everything a group agrees on hangs off a `tripId`. See
 * [PartyUnitEntity] / [TripMembershipEntity] for who is on it, [TripBriefEntity] for what they
 * agreed, and [LedgerEntryEntity] for what it cost.
 */
@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val destination: String,
    val countryCode: String = "US",
    val startDate: String,
    val endDate: String,
    val budgetTotal: Double = 0.0,
    val budgetSpent: Double = 0.0,
    val primaryCurrency: String = "USD",
    val travelersCount: Int = 1,
    val childrenCount: Int = 0,
    val accessibilityRequirements: String = "",
    val dietaryRestrictions: String = "",
    val familyAgeBrackets: String = "",
    val travelStyle: String = "",
    val timeshareExchangeDetails: String = "",
    val departureAirport: String = "",
    val destinationAirport: String = "",
    val isOfflineSynced: Boolean = true,
    val lastSyncedTimestamp: Long = System.currentTimeMillis(),
    val heroThemeIndex: Int = 0,
    val status: String = "PLANNING", // TripStatus.value: EXPLORING, PLANNING, IN_PROGRESS, COMPLETED
    /**
     * The [IdeaEntity] this trip was promoted from, or 0 when the trip was created directly.
     * 0 means "no originating idea" — never treat it as a real row id.
     */
    val originIdeaId: Long = 0
)

/**
 * Trip lifecycle.
 *
 * `EXPLORING` sits ahead of `PLANNING`: it is the disposable research stage backed by
 * [IdeaEntity]. The Pow Wow is the promotion ritual that moves a trip EXPLORING -> PLANNING.
 * Nothing auto-advances out of EXPLORING on dates alone — see [isEligibleForAutoStart].
 */
enum class TripStatus(val value: String, val label: String, val badgeEmoji: String) {
    EXPLORING("EXPLORING", "Exploring", "🧭"),
    PLANNING("PLANNING", "Planning Stage", "📋"),
    IN_PROGRESS("IN_PROGRESS", "On Trip / Live", "🧳"),
    COMPLETED("COMPLETED", "Completed", "🏁");

    /** True before departure — EXPLORING and PLANNING. */
    val isPreDeparture: Boolean
        get() = this == EXPLORING || this == PLANNING

    companion object {
        /**
         * Tolerant parse. Unrecognised/legacy spellings fall back to [PLANNING], which is the
         * historical behaviour: rows written before EXPLORING existed were all planning-stage, and
         * re-reading them as EXPLORING would silently demote real trips.
         */
        fun fromString(value: String): TripStatus {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: PLANNING
        }
    }
}

/**
 * Is the traveller physically on this trip right now?
 *
 * Explicit statuses win; only genuinely ambiguous ones fall through to the date range. EXPLORING
 * returns false unconditionally — an idea sketched with a rough window that happens to cover today
 * is still an idea, and letting it fall through to the date check would light up live-trip surfaces
 * (SOS beacon, live cockpit) for a trip nobody has agreed to take.
 */
fun TripEntity.isTripInProgress(): Boolean {
    if (status.equals("IN_PROGRESS", ignoreCase = true) ||
        status.equals("ACTIVE", ignoreCase = true) ||
        status.equals("ON_TRIP", ignoreCase = true)) {
        return true
    }
    if (status.equals("COMPLETED", ignoreCase = true) ||
        status.equals("PAST", ignoreCase = true)) {
        return false
    }
    if (status.equals("PLANNING", ignoreCase = true)) {
        return false
    }
    if (status.equals(TripStatus.EXPLORING.value, ignoreCase = true)) {
        return false
    }
    return isTodayWithinTripDates()
}

/**
 * Is today inside this trip's date range?
 *
 * Tolerates the three date formats that exist in stored rows. Returns false when either bound is
 * unparseable or absent — an unknown date is never treated as a match.
 */
fun TripEntity.isTodayWithinTripDates(): Boolean {
    return try {
        val now = java.time.LocalDate.now()
        val formats = listOf(
            java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy", java.util.Locale.US),
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd", java.util.Locale.US),
            java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy", java.util.Locale.US)
        )
        var start: java.time.LocalDate? = null
        var end: java.time.LocalDate? = null
        for (fmt in formats) {
            if (start == null) try { start = java.time.LocalDate.parse(startDate.trim(), fmt) } catch (_: Exception) {}
            if (end == null) try { end = java.time.LocalDate.parse(endDate.trim(), fmt) } catch (_: Exception) {}
        }
        if (start != null && end != null) {
            !now.isBefore(start) && !now.isAfter(end)
        } else false
    } catch (_: Exception) {
        false
    }
}

/**
 * May a date-driven background transition move this trip into IN_PROGRESS?
 *
 * True for a committed trip — PLANNING, or a legacy/unknown spelling — whose dates cover today.
 * EXPLORING and COMPLETED never auto-advance: promoting an idea is a human decision made through
 * the Pow Wow, not a calendar side effect.
 *
 * Note this deliberately does **not** delegate to [isTripInProgress], which returns false for
 * PLANNING by design. Before v10 the auto-transition did delegate, which meant a PLANNING trip
 * whose dates had arrived never actually transitioned — the check only ever fired for rows
 * carrying a legacy status string.
 */
fun TripEntity.isEligibleForAutoStart(): Boolean {
    if (status.equals(TripStatus.EXPLORING.value, ignoreCase = true)) return false
    if (status.equals(TripStatus.COMPLETED.value, ignoreCase = true)) return false
    if (status.equals("PAST", ignoreCase = true)) return false
    if (status.equals(TripStatus.IN_PROGRESS.value, ignoreCase = true)) return false
    if (status.equals("ACTIVE", ignoreCase = true)) return false
    if (status.equals("ON_TRIP", ignoreCase = true)) return false
    return isTodayWithinTripDates()
}

@Entity(tableName = "trip_activities", indices = [Index("tripId")])
data class TripActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val dayNumber: Int,
    val timeSlot: String,
    val title: String,
    val category: String, // FLIGHT, HOTEL, TIMESHARE, CAMPGROUND, TRANSIT, DINING, ATTRACTION, FAMILY_KIDS
    val location: String,
    val confirmationCode: String = "",
    val notes: String = "",
    val cost: Double = 0.0,
    val currency: String = "USD",
    val accessibilityBadge: String = "",
    val vendorName: String = "",
    val vendorPhone: String = "",
    val isCompleted: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

@Entity(tableName = "trip_feedbacks", indices = [Index("tripId")])
data class TripFeedbackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val tripTitle: String,
    val destination: String,
    val rating: Int = 5, // 1 to 5 stars
    val likedAspects: String = "",
    val dislikedAspects: String = "",
    val feedbackNotes: String = "",
    val dateSubmitted: String = "",
    val learnedActionableTakeaway: String = ""
)

@Entity(tableName = "proactive_suggestions")
data class ProactiveSuggestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val destination: String,
    val countryCode: String,
    val durationDays: Int,
    val estimatedBudget: Double,
    val currency: String = "USD",
    val matchScorePercent: Int = 98,
    val rationale: String,
    val suggestedAirline: String,
    val suggestedLodging: String,
    val activityPace: String,
    val heroTag: String = "AI Tailored Match",
    val pointsSavingsUsd: Double = 1850.0,
    val season: String = "Spring / Autumn Peak"
)
