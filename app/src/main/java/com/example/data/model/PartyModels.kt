package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ============================================================================================
 * THE PARTY MODEL:  Trip  ->  PartyUnit  ->  Traveler
 * ============================================================================================
 *
 * A trip has **party units** (a couple, a family, a solo traveller, a corporate team). Each unit
 * holds **travellers**. This is the shape the UI draws: grouped unit cards with member chips.
 *
 * The traveller-to-trip link is [TripMembershipEntity], and it is that row — not the traveller and
 * not the unit — that carries `partyUnitId`. Rationale: a person exists once
 * ([TravelerEntity]) and can appear on many trips in a different unit each time (with their
 * partner in June, with their whole family in December). Putting the unit on the membership keeps
 * one row per (traveller, trip) and makes "which unit is Dana in on THIS trip" a single lookup.
 *
 *     TripEntity 1---* PartyUnitEntity
 *     TripEntity 1---* TripMembershipEntity *---1 TravelerEntity
 *     TripMembershipEntity *---1 PartyUnitEntity   (partyUnitId, 0 = unassigned)
 *
 * A membership with `partyUnitId == 0` is valid and expected: someone joins the trip before the
 * organiser has sorted the party into units. Render them as unassigned, never auto-file them into
 * a guessed unit.
 */

/** How a party unit is composed. Stored as [PartyUnitType.value] in [PartyUnitEntity.unitType]. */
enum class PartyUnitType(val value: String, val label: String) {
    COUPLE("COUPLE", "Couple"),
    FAMILY("FAMILY", "Family"),
    SOLO("SOLO", "Solo"),
    CORPORATE_TEAM("CORPORATE_TEAM", "Corporate Team");

    companion object {
        /** Returns null for unknown/absent input — the caller renders "no unit type" rather than a guess. */
        fun fromStringOrNull(value: String): PartyUnitType? =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
    }
}

/**
 * Age band. Drives consent and account rules, not pricing.
 *
 * Accounts exist at 14+, so a CHILD never holds an account: their Pow Wow is captured as a guest
 * on a guardian's device with recorded parental consent. TEEN holds an account but sensitive
 * surfaces stay parentally guided.
 */
enum class TravelerAgeBand(val value: String, val label: String) {
    ADULT("ADULT", "Adult"),
    TEEN("TEEN", "Teen"),
    CHILD("CHILD", "Child");

    /** True when any capture involving this traveller requires a recorded guardian consent. */
    val requiresGuardianConsent: Boolean
        get() = this == CHILD

    /** True when this band may hold their own account (14+). */
    val canHoldAccount: Boolean
        get() = this != CHILD

    companion object {
        /** Returns null for unknown/absent input. Absent age band must render as absent. */
        fun fromStringOrNull(value: String): TravelerAgeBand? =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
    }
}

/** Whether a traveller is a real signed-in account or a guest profile someone else created. */
enum class TravelerAccountLinkage(val value: String) {
    ACCOUNT_LINKED("ACCOUNT_LINKED"),
    GUEST("GUEST");

    companion object {
        fun fromStringOrNull(value: String): TravelerAccountLinkage? =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
    }
}

/**
 * A person who can be on a trip.
 *
 * Exists independently of any trip and independently of whether they ever open the app. Empty
 * string means "not supplied" everywhere in this entity — do not substitute a placeholder name,
 * a guessed age band, or a default home airport.
 */
@Entity(
    tableName = "travelers",
    indices = [Index("authUid"), Index("guardianTravelerId")]
)
data class TravelerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Given name only — the one high-variance-free typed field the product allows. "" = unknown. */
    val displayName: String = "",
    /** [TravelerAgeBand.value]. "" = not yet declared; render as absent, never assume ADULT. */
    val ageBand: String = "",
    /** [TravelerAccountLinkage.value]. Guest is the honest default: no account has been linked yet. */
    val accountLinkage: String = "GUEST",
    /** Firebase Auth uid when ACCOUNT_LINKED. "" for guests. This is the key the Firestore rules match on. */
    val authUid: String = "",
    /**
     * The adult responsible for a CHILD guest profile. 0 = none.
     * Required before any [PowWowSessionEntity] for a CHILD may record consent.
     */
    val guardianTravelerId: Long = 0,
    /** IATA code, "" = not supplied. */
    val homeAirport: String = "",
    /** "" = not supplied. */
    val avatarEmoji: String = "",
    val createdAtTimestamp: Long = System.currentTimeMillis()
)

/** True when this traveller row is the signed-in device owner's own participant row. */
fun TravelerEntity.isAccountLinked(): Boolean =
    accountLinkage.equals(TravelerAccountLinkage.ACCOUNT_LINKED.value, ignoreCase = true) &&
        authUid.isNotBlank()

/**
 * A group within a trip's party.
 *
 * [label] is user-supplied ("The Riveras", "Ops team"). When blank, render the unit type alone —
 * do not synthesise a name from member names.
 */
@Entity(tableName = "party_units", indices = [Index("tripId")])
data class PartyUnitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    /** [PartyUnitType.value]. Required — a unit is created by picking its type. */
    val unitType: String,
    /** "" = unnamed unit. */
    val label: String = "",
    /** Display order within the trip; ties broken by id. */
    val displayOrder: Int = 0,
    /** Traveller who created the unit. 0 = unknown (e.g. imported). */
    val createdByTravelerId: Long = 0,
    val createdAtTimestamp: Long = System.currentTimeMillis()
)

/** A traveller's role on a trip. */
enum class TripRole(val value: String, val label: String) {
    /** Can manage membership, party structure, and the ledger configuration. */
    ORGANIZER("ORGANIZER", "Organizer"),
    /** Full participant: contributes, records expenses, takes part in the Pow Wow. */
    TRAVELER("TRAVELER", "Traveler"),
    /** Read-only observer. Never writes ledger or party data. */
    VIEWER("VIEWER", "Viewer");

    /** Roles allowed to write shared trip content. Mirrors the `canWrite()` check in firestore.rules. */
    val canWriteTripContent: Boolean
        get() = this == ORGANIZER || this == TRAVELER

    companion object {
        fun fromStringOrNull(value: String): TripRole? =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
    }
}

/** Lifecycle of a membership. */
enum class TripMembershipState(val value: String) {
    INVITED("INVITED"),
    ACTIVE("ACTIVE"),
    DECLINED("DECLINED"),
    REMOVED("REMOVED");

    companion object {
        fun fromStringOrNull(value: String): TripMembershipState? =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
    }
}

/**
 * traveller <-> trip, with role and party-unit placement.
 *
 * One row per (tripId, travelerId) — enforced by a unique index. This entity is the local mirror
 * of the `members` map on the shared `/trips/{tripId}` Firestore document that the security rules
 * read to decide access; keep the two in step when writing membership changes.
 */
@Entity(
    tableName = "trip_memberships",
    indices = [
        Index(value = ["tripId", "travelerId"], unique = true),
        Index("travelerId"),
        Index("partyUnitId")
    ]
)
data class TripMembershipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val travelerId: Long,
    /** [PartyUnitEntity.id], or 0 when the traveller has not been placed in a unit yet. */
    val partyUnitId: Long = 0,
    /** [TripRole.value]. Required. */
    val role: String,
    /** [TripMembershipState.value]. Required. */
    val state: String,
    /** Who invited them. 0 = not applicable (e.g. the founding organiser). */
    val invitedByTravelerId: Long = 0,
    val invitedAtTimestamp: Long = 0,
    /** 0 = has not joined yet. Never backfill this with the invite time. */
    val joinedAtTimestamp: Long = 0
)
