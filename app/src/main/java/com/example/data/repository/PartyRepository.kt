package com.example.data.repository

import com.example.data.local.PartyDao
import com.example.data.model.IdeaEntity
import com.example.data.model.PartyUnitEntity
import com.example.data.model.TravelerEntity
import com.example.data.model.TripMembershipEntity
import com.example.data.model.TripRole
import com.example.data.model.TripMembershipState
import kotlinx.coroutines.flow.Flow

/**
 * Travellers, party units, memberships, and ideas.
 *
 * Depend on this interface, not on `TravelViewModel`. A feature ViewModel obtains one from
 * [com.example.data.di.MarcoRepositories] and needs to know nothing else about the data layer.
 *
 * Everything here is persistence and observation. It contains no party-shape heuristics, no
 * invitation transport, and no UI state — those belong to the feature layer.
 */
interface PartyRepository {

    // ---- Travelers ---------------------------------------------------------------------------

    fun observeAllTravelers(): Flow<List<TravelerEntity>>

    fun observeTraveler(travelerId: Long): Flow<TravelerEntity?>

    suspend fun getTraveler(travelerId: Long): TravelerEntity?

    /** The traveller row for a signed-in Firebase uid, or null if none has been created. */
    suspend fun getTravelerByAuthUid(authUid: String): TravelerEntity?

    /** Returns the new row id. */
    suspend fun upsertTraveler(traveler: TravelerEntity): Long

    suspend fun deleteTraveler(travelerId: Long)

    // ---- Party units -------------------------------------------------------------------------

    fun observePartyUnits(tripId: Long): Flow<List<PartyUnitEntity>>

    suspend fun getPartyUnits(tripId: Long): List<PartyUnitEntity>

    suspend fun upsertPartyUnit(unit: PartyUnitEntity): Long

    /**
     * Removes the unit and leaves its members on the trip as unassigned. Deleting a unit must never
     * remove people from the trip.
     */
    suspend fun deletePartyUnit(partyUnitId: Long)

    // ---- Memberships -------------------------------------------------------------------------

    fun observeMemberships(tripId: Long): Flow<List<TripMembershipEntity>>

    suspend fun getMemberships(tripId: Long): List<TripMembershipEntity>

    fun observeMembershipsForTraveler(travelerId: Long): Flow<List<TripMembershipEntity>>

    suspend fun getMembership(tripId: Long, travelerId: Long): TripMembershipEntity?

    /** Travellers on a trip, resolved through membership. */
    fun observeTravelersForTrip(tripId: Long): Flow<List<TravelerEntity>>

    suspend fun upsertMembership(membership: TripMembershipEntity): Long

    /** Move a member into a party unit. Pass 0 to unassign. */
    suspend fun assignToPartyUnit(membershipId: Long, partyUnitId: Long)

    suspend fun setMembershipRole(membershipId: Long, role: TripRole)

    suspend fun setMembershipState(membershipId: Long, state: TripMembershipState)

    // ---- Ideas (the EXPLORING stage) ----------------------------------------------------------

    /** Ideas that are neither promoted nor discarded. */
    fun observeActiveIdeas(): Flow<List<IdeaEntity>>

    fun observeAllIdeas(): Flow<List<IdeaEntity>>

    fun observeIdea(ideaId: Long): Flow<IdeaEntity?>

    suspend fun getIdea(ideaId: Long): IdeaEntity?

    suspend fun upsertIdea(idea: IdeaEntity): Long

    /**
     * Record that an idea became a trip. Writes the link on the idea only; creating the trip and
     * setting [com.example.data.model.TripEntity.originIdeaId] is the caller's job, because trip
     * creation is not this repository's concern.
     */
    suspend fun markIdeaPromoted(ideaId: Long, tripId: Long, timestamp: Long = System.currentTimeMillis())

    /** Hide an idea without deleting it, so provenance survives. */
    suspend fun discardIdea(ideaId: Long, timestamp: Long = System.currentTimeMillis())
}

/** Room-backed [PartyRepository]. A thin pass-through; all policy lives above it. */
class RoomPartyRepository(private val partyDao: PartyDao) : PartyRepository {

    override fun observeAllTravelers(): Flow<List<TravelerEntity>> = partyDao.getAllTravelers()

    override fun observeTraveler(travelerId: Long): Flow<TravelerEntity?> =
        partyDao.getTravelerById(travelerId)

    override suspend fun getTraveler(travelerId: Long): TravelerEntity? =
        partyDao.getTravelerByIdSync(travelerId)

    override suspend fun getTravelerByAuthUid(authUid: String): TravelerEntity? =
        if (authUid.isBlank()) null else partyDao.getTravelerByAuthUid(authUid)

    override suspend fun upsertTraveler(traveler: TravelerEntity): Long =
        partyDao.insertTraveler(traveler)

    override suspend fun deleteTraveler(travelerId: Long) = partyDao.deleteTraveler(travelerId)

    override fun observePartyUnits(tripId: Long): Flow<List<PartyUnitEntity>> =
        partyDao.getPartyUnitsForTrip(tripId)

    override suspend fun getPartyUnits(tripId: Long): List<PartyUnitEntity> =
        partyDao.getPartyUnitsForTripSync(tripId)

    override suspend fun upsertPartyUnit(unit: PartyUnitEntity): Long =
        partyDao.insertPartyUnit(unit)

    override suspend fun deletePartyUnit(partyUnitId: Long) {
        partyDao.clearPartyUnitAssignments(partyUnitId)
        partyDao.deletePartyUnit(partyUnitId)
    }

    override fun observeMemberships(tripId: Long): Flow<List<TripMembershipEntity>> =
        partyDao.getMembershipsForTrip(tripId)

    override suspend fun getMemberships(tripId: Long): List<TripMembershipEntity> =
        partyDao.getMembershipsForTripSync(tripId)

    override fun observeMembershipsForTraveler(travelerId: Long): Flow<List<TripMembershipEntity>> =
        partyDao.getMembershipsForTraveler(travelerId)

    override suspend fun getMembership(tripId: Long, travelerId: Long): TripMembershipEntity? =
        partyDao.getMembership(tripId, travelerId)

    override fun observeTravelersForTrip(tripId: Long): Flow<List<TravelerEntity>> =
        partyDao.getTravelersForTrip(tripId)

    override suspend fun upsertMembership(membership: TripMembershipEntity): Long =
        partyDao.insertMembership(membership)

    override suspend fun assignToPartyUnit(membershipId: Long, partyUnitId: Long) =
        partyDao.assignMembershipToUnit(membershipId, partyUnitId)

    override suspend fun setMembershipRole(membershipId: Long, role: TripRole) =
        partyDao.updateMembershipRole(membershipId, role.value)

    override suspend fun setMembershipState(membershipId: Long, state: TripMembershipState) =
        partyDao.updateMembershipState(membershipId, state.value)

    override fun observeActiveIdeas(): Flow<List<IdeaEntity>> = partyDao.getActiveIdeas()

    override fun observeAllIdeas(): Flow<List<IdeaEntity>> = partyDao.getAllIdeas()

    override fun observeIdea(ideaId: Long): Flow<IdeaEntity?> = partyDao.getIdeaById(ideaId)

    override suspend fun getIdea(ideaId: Long): IdeaEntity? = partyDao.getIdeaByIdSync(ideaId)

    override suspend fun upsertIdea(idea: IdeaEntity): Long = partyDao.insertIdea(idea)

    override suspend fun markIdeaPromoted(ideaId: Long, tripId: Long, timestamp: Long) =
        partyDao.markIdeaPromoted(ideaId, tripId, timestamp)

    override suspend fun discardIdea(ideaId: Long, timestamp: Long) =
        partyDao.markIdeaDiscarded(ideaId, timestamp)
}
