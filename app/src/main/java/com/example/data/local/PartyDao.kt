package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.IdeaEntity
import com.example.data.model.PartyUnitEntity
import com.example.data.model.TravelerEntity
import com.example.data.model.TripMembershipEntity
import kotlinx.coroutines.flow.Flow

/**
 * Travellers, party units, memberships, and ideas.
 *
 * Split out of [TravelDao] on purpose: the party domain is owned by one track, and a DAO per
 * domain keeps three parallel tracks off the same file. Query naming follows the existing DAO —
 * `Flow` for anything the UI observes, `suspend` for one-shot reads and writes.
 */
@Dao
interface PartyDao {

    // ---- Travelers ---------------------------------------------------------------------------

    @Query("SELECT * FROM travelers ORDER BY id ASC")
    fun getAllTravelers(): Flow<List<TravelerEntity>>

    @Query("SELECT * FROM travelers WHERE id = :travelerId LIMIT 1")
    fun getTravelerById(travelerId: Long): Flow<TravelerEntity?>

    @Query("SELECT * FROM travelers WHERE id = :travelerId LIMIT 1")
    suspend fun getTravelerByIdSync(travelerId: Long): TravelerEntity?

    @Query("SELECT * FROM travelers WHERE authUid = :authUid LIMIT 1")
    suspend fun getTravelerByAuthUid(authUid: String): TravelerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTraveler(traveler: TravelerEntity): Long

    @Update
    suspend fun updateTraveler(traveler: TravelerEntity)

    @Query("DELETE FROM travelers WHERE id = :travelerId")
    suspend fun deleteTraveler(travelerId: Long)

    // ---- Party units -------------------------------------------------------------------------

    @Query("SELECT * FROM party_units WHERE tripId = :tripId ORDER BY displayOrder ASC, id ASC")
    fun getPartyUnitsForTrip(tripId: Long): Flow<List<PartyUnitEntity>>

    @Query("SELECT * FROM party_units WHERE tripId = :tripId ORDER BY displayOrder ASC, id ASC")
    suspend fun getPartyUnitsForTripSync(tripId: Long): List<PartyUnitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartyUnit(unit: PartyUnitEntity): Long

    @Update
    suspend fun updatePartyUnit(unit: PartyUnitEntity)

    @Query("DELETE FROM party_units WHERE id = :partyUnitId")
    suspend fun deletePartyUnit(partyUnitId: Long)

    /** Detach members from a unit being removed; they become unassigned, not deleted. */
    @Query("UPDATE trip_memberships SET partyUnitId = 0 WHERE partyUnitId = :partyUnitId")
    suspend fun clearPartyUnitAssignments(partyUnitId: Long)

    // ---- Memberships -------------------------------------------------------------------------

    @Query("SELECT * FROM trip_memberships WHERE tripId = :tripId ORDER BY id ASC")
    fun getMembershipsForTrip(tripId: Long): Flow<List<TripMembershipEntity>>

    @Query("SELECT * FROM trip_memberships WHERE tripId = :tripId ORDER BY id ASC")
    suspend fun getMembershipsForTripSync(tripId: Long): List<TripMembershipEntity>

    @Query("SELECT * FROM trip_memberships WHERE travelerId = :travelerId ORDER BY id ASC")
    fun getMembershipsForTraveler(travelerId: Long): Flow<List<TripMembershipEntity>>

    @Query("SELECT * FROM trip_memberships WHERE tripId = :tripId AND travelerId = :travelerId LIMIT 1")
    suspend fun getMembership(tripId: Long, travelerId: Long): TripMembershipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembership(membership: TripMembershipEntity): Long

    @Update
    suspend fun updateMembership(membership: TripMembershipEntity)

    @Delete
    suspend fun deleteMembership(membership: TripMembershipEntity)

    @Query("UPDATE trip_memberships SET partyUnitId = :partyUnitId WHERE id = :membershipId")
    suspend fun assignMembershipToUnit(membershipId: Long, partyUnitId: Long)

    @Query("UPDATE trip_memberships SET role = :role WHERE id = :membershipId")
    suspend fun updateMembershipRole(membershipId: Long, role: String)

    @Query("UPDATE trip_memberships SET state = :state WHERE id = :membershipId")
    suspend fun updateMembershipState(membershipId: Long, state: String)

    /** Travellers on a trip, joined through membership. */
    @Query(
        """
        SELECT t.* FROM travelers t
        INNER JOIN trip_memberships m ON m.travelerId = t.id
        WHERE m.tripId = :tripId
        ORDER BY t.id ASC
        """
    )
    fun getTravelersForTrip(tripId: Long): Flow<List<TravelerEntity>>

    // ---- Ideas -------------------------------------------------------------------------------

    @Query("SELECT * FROM ideas WHERE discardedAtTimestamp = 0 AND promotedTripId = 0 ORDER BY lastUpdatedTimestamp DESC")
    fun getActiveIdeas(): Flow<List<IdeaEntity>>

    @Query("SELECT * FROM ideas ORDER BY lastUpdatedTimestamp DESC")
    fun getAllIdeas(): Flow<List<IdeaEntity>>

    @Query("SELECT * FROM ideas WHERE id = :ideaId LIMIT 1")
    fun getIdeaById(ideaId: Long): Flow<IdeaEntity?>

    @Query("SELECT * FROM ideas WHERE id = :ideaId LIMIT 1")
    suspend fun getIdeaByIdSync(ideaId: Long): IdeaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdea(idea: IdeaEntity): Long

    @Update
    suspend fun updateIdea(idea: IdeaEntity)

    @Query("UPDATE ideas SET promotedTripId = :tripId, promotedAtTimestamp = :timestamp WHERE id = :ideaId")
    suspend fun markIdeaPromoted(ideaId: Long, tripId: Long, timestamp: Long)

    @Query("UPDATE ideas SET discardedAtTimestamp = :timestamp WHERE id = :ideaId")
    suspend fun markIdeaDiscarded(ideaId: Long, timestamp: Long)

    // ---- Wipe --------------------------------------------------------------------------------

    @Query("DELETE FROM travelers")
    suspend fun clearTravelers()

    @Query("DELETE FROM party_units")
    suspend fun clearPartyUnits()

    @Query("DELETE FROM trip_memberships")
    suspend fun clearMemberships()

    @Query("DELETE FROM ideas")
    suspend fun clearIdeas()
}
