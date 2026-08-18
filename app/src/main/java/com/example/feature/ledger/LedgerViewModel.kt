package com.example.feature.ledger

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.di.MarcoRepositories
import com.example.data.model.ContributionEntity
import com.example.data.model.ContributionProposalSource
import com.example.data.model.ContributionState
import com.example.data.model.LedgerEntryEntity
import com.example.data.model.LedgerModel
import com.example.data.model.SettlementEntity
import com.example.data.model.SettlementState
import com.example.data.model.SplitRuleEntity
import com.example.data.model.TravelerBalance
import com.example.data.model.TravelerEntity
import com.example.data.model.TripLedgerConfigEntity
import com.example.data.model.withProposedValue
import com.example.feature.ledger.engine.SettlementEngines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Explicit state for [LedgerViewModel.derivedBalances]. A ledger currency is a decision the group
 * makes, not a computation — so there is no "Ready" state with a guessed currency. See
 * [TripLedgerConfigEntity.confirmedAtTimestamp].
 */
sealed interface LedgerBalancesState {
    /** The group has not confirmed a ledger model/currency yet. The UI should prompt to configure it. */
    data object NotConfigured : LedgerBalancesState

    /** A confirmed currency exists; [balances] were computed against it. */
    data class Ready(val balances: List<TravelerBalance>) : LedgerBalancesState
}

/**
 * Feature ViewModel for Trip Ledger, Pooling, and Settlements.
 * Obtains its repositories cleanly from [MarcoRepositories] without touching [TravelViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LedgerViewModel(application: Application) : AndroidViewModel(application) {

    private val ledgerRepository = MarcoRepositories.ledger(application)
    private val partyRepository = MarcoRepositories.party(application)
    private val travelRepository = MarcoRepositories.travel(application)

    private val _currentTripId = MutableStateFlow(0L)
    val currentTripId: StateFlow<Long> = _currentTripId.asStateFlow()

    fun selectTrip(tripId: Long) {
        _currentTripId.value = tripId
    }

    val ledgerConfig: StateFlow<TripLedgerConfigEntity?> = _currentTripId.flatMapLatest { tripId ->
        if (tripId == 0L) flowOf(null) else ledgerRepository.observeLedgerConfig(tripId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val contributions: StateFlow<List<ContributionEntity>> = _currentTripId.flatMapLatest { tripId ->
        if (tripId == 0L) flowOf(emptyList()) else ledgerRepository.observeContributions(tripId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ledgerEntries: StateFlow<List<LedgerEntryEntity>> = _currentTripId.flatMapLatest { tripId ->
        if (tripId == 0L) flowOf(emptyList()) else ledgerRepository.observeLedgerEntries(tripId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val splitRules: StateFlow<List<SplitRuleEntity>> = _currentTripId.flatMapLatest { tripId ->
        if (tripId == 0L) flowOf(emptyList()) else ledgerRepository.observeSplitRules(tripId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settlements: StateFlow<List<SettlementEntity>> = _currentTripId.flatMapLatest { tripId ->
        if (tripId == 0L) flowOf(emptyList()) else ledgerRepository.observeSettlements(tripId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val travelers: StateFlow<List<TravelerEntity>> = _currentTripId.flatMapLatest { tripId ->
        if (tripId == 0L) flowOf(emptyList()) else partyRepository.observeTravelersForTrip(tripId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Derived per-traveler balances computed on the fly using the active [SettlementEngine].
     *
     * [TripLedgerConfigEntity.confirmedAtTimestamp] == 0 means the group has not confirmed a
     * ledger currency yet — there is no honest currency to compute in, so this surfaces
     * [LedgerBalancesState.NotConfigured] instead of guessing one. The UI renders that as a
     * prompt to configure the ledger, not as an empty or zeroed balance sheet.
     */
    val derivedBalances: StateFlow<LedgerBalancesState> = combine(
        ledgerConfig,
        ledgerEntries,
        splitRules,
        contributions,
        travelers
    ) { config, entries, rules, contribs, travs ->
        val currency = config?.normalizedCurrency?.takeIf { it.isNotBlank() }
        if (config == null || config.confirmedAtTimestamp == 0L || currency == null) {
            LedgerBalancesState.NotConfigured
        } else {
            val model = LedgerModel.fromStringOrNull(config.ledgerModel) ?: LedgerModel.SPLIT_SETTLE
            val engine = SettlementEngines.forModel(model)
            val travelerIds = travs.map { it.id }
            LedgerBalancesState.Ready(engine.balances(entries, rules, contribs, travelerIds, currency))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LedgerBalancesState.NotConfigured)

    /**
     * Confirms the trip's ledger model and currency. This is a human decision — there is no
     * fallback currency; the caller must supply the one the group actually chose.
     */
    fun updateLedgerModel(model: LedgerModel, normalizedCurrency: String) {
        val tripId = _currentTripId.value
        if (tripId == 0L) return
        require(normalizedCurrency.isNotBlank()) {
            "The group must choose a currency before confirming a ledger model."
        }
        viewModelScope.launch(Dispatchers.IO) {
            val existing = ledgerRepository.getLedgerConfig(tripId)
            val updated = existing?.copy(
                ledgerModel = model.value,
                normalizedCurrency = normalizedCurrency
            ) ?: TripLedgerConfigEntity(
                tripId = tripId,
                ledgerModel = model.value,
                normalizedCurrency = normalizedCurrency
            )
            ledgerRepository.upsertLedgerConfig(updated)
        }
    }

    fun recordContributionOffer(
        contributorTravelerId: Long,
        assetKind: String,
        nativeQuantity: Double,
        nativeUnitLabel: String,
        description: String = "",
        proposedAmount: Double = 0.0,
        proposedCurrency: String = "",
        proposalSource: ContributionProposalSource = ContributionProposalSource.USER_SUPPLIED
    ) {
        val tripId = _currentTripId.value
        if (tripId == 0L) return
        viewModelScope.launch(Dispatchers.IO) {
            var entity = ContributionEntity(
                tripId = tripId,
                contributorTravelerId = contributorTravelerId,
                assetKind = assetKind,
                description = description,
                state = ContributionState.OFFERED.value,
                nativeQuantity = nativeQuantity,
                nativeUnitLabel = nativeUnitLabel,
                offeredAtTimestamp = System.currentTimeMillis()
            )
            // A proposal without a currency is not a proposal — leave the offer unvalued rather
            // than guessing one (see ContributionEntity.withProposedValue, which requires it).
            if (proposedAmount > 0.0 && proposedCurrency.isNotBlank()) {
                entity = entity.withProposedValue(proposedAmount, proposedCurrency, proposalSource)
            }
            ledgerRepository.upsertContribution(entity)
        }
    }

    fun recordContributionAgreement(
        contributionId: Long,
        amount: Double,
        currency: String,
        agreedByTravelerIds: List<Long>,
        note: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            ledgerRepository.recordContributionAgreement(
                contributionId = contributionId,
                amount = amount,
                currency = currency,
                agreedByTravelerIds = agreedByTravelerIds,
                note = note
            )
            recalculateSettlements()
        }
    }

    fun addExpense(
        payerTravelerId: Long,
        amountOriginal: Double,
        currency: String,
        category: String,
        description: String = "",
        splitRuleId: Long = 0L,
        fundedWithPoints: Boolean = false,
        pointsQuantity: Double = 0.0,
        pointsProgramTitle: String = ""
    ) {
        val tripId = _currentTripId.value
        if (tripId == 0L) return
        viewModelScope.launch(Dispatchers.IO) {
            val config = ledgerRepository.getLedgerConfig(tripId)
            // No configured currency (or the group hasn't confirmed one) means this entry cannot
            // be normalized yet. Store it as-incurred and leave the normalized columns at their
            // "not converted" defaults rather than assuming USD.
            val configuredCurrency = config?.normalizedCurrency?.takeIf { it.isNotBlank() }
            val isDirect = configuredCurrency != null && currency.equals(configuredCurrency, ignoreCase = true)

            val entry = LedgerEntryEntity(
                tripId = tripId,
                payerTravelerId = payerTravelerId,
                amountOriginal = amountOriginal,
                originalCurrency = currency,
                amountNormalized = if (isDirect) amountOriginal else 0.0,
                // isDirect is only true when configuredCurrency is non-null (see above).
                normalizedCurrency = if (isDirect) configuredCurrency!! else "",
                exchangeRate = if (isDirect) 1.0 else 0.0,
                category = category,
                description = description,
                splitRuleId = splitRuleId,
                fundedWithPoints = fundedWithPoints,
                pointsProgramTitle = pointsProgramTitle,
                pointsQuantity = pointsQuantity,
                createdAtTimestamp = System.currentTimeMillis()
            )
            ledgerRepository.upsertLedgerEntry(entry)
            recalculateSettlements()
        }
    }

    fun recalculateSettlements() {
        val tripId = _currentTripId.value
        if (tripId == 0L) return
        viewModelScope.launch(Dispatchers.IO) {
            val config = ledgerRepository.getLedgerConfig(tripId)
            val currency = config?.normalizedCurrency?.takeIf { it.isNotBlank() }
            if (config == null || config.confirmedAtTimestamp == 0L || currency == null) {
                // The group hasn't confirmed a ledger model/currency yet — there is nothing
                // honest to settle in, so clear any stale proposed transfers instead of
                // computing one in an assumed currency.
                ledgerRepository.replaceProposedSettlements(tripId, emptyList())
                return@launch
            }
            val model = LedgerModel.fromStringOrNull(config.ledgerModel) ?: LedgerModel.SPLIT_SETTLE
            val engine = SettlementEngines.forModel(model)

            val entries = ledgerRepository.getLedgerEntries(tripId)
            val rules = ledgerRepository.getSplitRules(tripId)
            val contribs = ledgerRepository.getContributions(tripId)
            val memberships = partyRepository.getMemberships(tripId)
            val travelerIds = memberships.map { it.travelerId }

            val balances = engine.balances(entries, rules, contribs, travelerIds, currency)
            val proposed = engine.proposeTransfers(tripId, balances, currency)

            ledgerRepository.replaceProposedSettlements(tripId, proposed)
        }
    }

    fun markSettlementPaid(settlement: SettlementEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            ledgerRepository.updateSettlement(
                settlement.copy(
                    state = SettlementState.PAID.value,
                    settledAtTimestamp = System.currentTimeMillis()
                )
            )
        }
    }
}
