package com.example.feature.party

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.di.MarcoRepositories
import com.example.data.model.PartyUnitEntity
import com.example.data.model.PartyUnitType
import com.example.data.model.TravelerAccountLinkage
import com.example.data.model.TravelerAgeBand
import com.example.data.model.TravelerEntity
import com.example.data.model.TripMembershipEntity
import com.example.data.model.TripMembershipState
import com.example.data.model.TripRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Feature ViewModel for Group Party Structure and Member Management.
 * Obtains its repositories cleanly from [MarcoRepositories] without touching [TravelViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PartyViewModel(application: Application) : AndroidViewModel(application) {

    private val partyRepository = MarcoRepositories.party(application)
    private val travelRepository = MarcoRepositories.travel(application)

    private val _currentTripId = MutableStateFlow(0L)
    val currentTripId: StateFlow<Long> = _currentTripId.asStateFlow()

    fun selectTrip(tripId: Long) {
        _currentTripId.value = tripId
    }

    val partyUnits: StateFlow<List<PartyUnitEntity>> = _currentTripId.flatMapLatest { tripId ->
        if (tripId == 0L) flowOf(emptyList()) else partyRepository.observePartyUnits(tripId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memberships: StateFlow<List<TripMembershipEntity>> = _currentTripId.flatMapLatest { tripId ->
        if (tripId == 0L) flowOf(emptyList()) else partyRepository.observeMemberships(tripId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val travelersForTrip: StateFlow<List<TravelerEntity>> = _currentTripId.flatMapLatest { tripId ->
        if (tripId == 0L) flowOf(emptyList()) else partyRepository.observeTravelersForTrip(tripId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTravelers: StateFlow<List<TravelerEntity>> = partyRepository.observeAllTravelers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createPartyUnit(unitType: PartyUnitType, label: String = "") {
        val tripId = _currentTripId.value
        if (tripId == 0L) return
        viewModelScope.launch(Dispatchers.IO) {
            partyRepository.upsertPartyUnit(
                PartyUnitEntity(
                    tripId = tripId,
                    unitType = unitType.value,
                    label = label,
                    displayOrder = (partyUnits.value.maxOfOrNull { it.displayOrder } ?: 0) + 1
                )
            )
        }
    }

    fun deletePartyUnit(partyUnitId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            partyRepository.deletePartyUnit(partyUnitId)
        }
    }

    fun assignMemberToUnit(membershipId: Long, partyUnitId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            partyRepository.assignToPartyUnit(membershipId, partyUnitId)
        }
    }

    fun addTravelerToTrip(
        displayName: String,
        ageBand: TravelerAgeBand,
        role: TripRole,
        partyUnitId: Long = 0L,
        homeAirport: String = "",
        guardianTravelerId: Long = 0L
    ) {
        val tripId = _currentTripId.value
        if (tripId == 0L) return
        viewModelScope.launch(Dispatchers.IO) {
            val travelerId = partyRepository.upsertTraveler(
                TravelerEntity(
                    displayName = displayName,
                    ageBand = ageBand.value,
                    accountLinkage = TravelerAccountLinkage.GUEST.value,
                    homeAirport = homeAirport,
                    guardianTravelerId = guardianTravelerId
                )
            )
            partyRepository.upsertMembership(
                TripMembershipEntity(
                    tripId = tripId,
                    travelerId = travelerId,
                    partyUnitId = partyUnitId,
                    role = role.value,
                    state = TripMembershipState.ACTIVE.value,
                    joinedAtTimestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateMembershipRole(membershipId: Long, role: TripRole) {
        viewModelScope.launch(Dispatchers.IO) {
            partyRepository.setMembershipRole(membershipId, role)
        }
    }

    fun updateMembershipState(membershipId: Long, state: TripMembershipState) {
        viewModelScope.launch(Dispatchers.IO) {
            partyRepository.setMembershipState(membershipId, state)
        }
    }
}
