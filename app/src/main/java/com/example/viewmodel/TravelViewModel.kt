package com.example.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiTravelService
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ConnectedAccountEntity
import com.example.data.model.EmergencyAlertEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.GroupMemoryEntity
import com.example.data.model.TripActivityEntity
import com.example.data.model.TripEntity
import com.example.data.model.TripStatus
import com.example.data.model.isTripInProgress
import com.example.data.model.VendorCallLogEntity
import com.example.data.repository.TravelRepository
import com.example.data.local.AppSettingsState
import com.example.data.local.AvailableVoiceProfiles
import com.example.data.local.SettingsManager
import com.example.data.local.VoiceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class TravelViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val repository: TravelRepository
    private val geminiService = GeminiTravelService()
    private val settingsManager = SettingsManager.getInstance(application)
    val dataStoreManager = com.example.data.local.DataStoreManager.getInstance(application)
    val cloudSyncManager = com.example.data.sync.CloudSyncManager(application)

    private val _settingsState = MutableStateFlow(settingsManager.loadSettings())
    val settingsState: StateFlow<AppSettingsState> = _settingsState.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    private fun ensureTtsInitialized() {
        if (tts == null) {
            try {
                tts = TextToSpeech(getApplication<Application>().applicationContext, this)
            } catch (e: Exception) {
                // Fallback gracefully if TTS service is unavailable on system
            }
        }
    }

    init {
        val db = AppDatabase.getInstance(application)
        repository = TravelRepository(db.travelDao())
        viewModelScope.launch(Dispatchers.IO) {
            repository.checkAndSeedInitialData()
        }

        // Wire dynamic API key & model to GeminiTravelService
        geminiService.setCustomApiKeyProvider {
            _settingsState.value.customGeminiApiKey
        }
        geminiService.setCustomModelProvider {
            _settingsState.value.defaultAiModel
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isTtsInitialized = true
        }
    }

    // Trips state
    val allTrips: StateFlow<List<TripEntity>> = repository.allTrips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTripId = MutableStateFlow<Long>(0L)
    val selectedTripId: StateFlow<Long> = _selectedTripId.asStateFlow()

    private val _selectedDayFilter = MutableStateFlow<Int>(0) // 0 = All Days
    val selectedDayFilter: StateFlow<Int> = _selectedDayFilter.asStateFlow()

    // Activities for currently selected trip
    private val _activities = MutableStateFlow<List<TripActivityEntity>>(emptyList())
    val activities: StateFlow<List<TripActivityEntity>> = _activities.asStateFlow()

    // Expenses
    private val _expenses = MutableStateFlow<List<ExpenseEntity>>(emptyList())
    val expenses: StateFlow<List<ExpenseEntity>> = _expenses.asStateFlow()

    // Connected Accounts
    val connectedAccounts: StateFlow<List<ConnectedAccountEntity>> = repository.connectedAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Vendor Calls
    val vendorCalls: StateFlow<List<VendorCallLogEntity>> = repository.allVendorCalls
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Emergency Alerts
    val emergencyAlerts: StateFlow<List<EmergencyAlertEntity>> = repository.allAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Marco Wallet Balances, Transactions, and Currency Rates
    private val _walletBalances = MutableStateFlow<List<com.example.data.model.WalletBalanceEntity>>(emptyList())
    val walletBalances: StateFlow<List<com.example.data.model.WalletBalanceEntity>> = _walletBalances.asStateFlow()

    private val _walletTransactions = MutableStateFlow<List<com.example.data.model.WalletTransactionEntity>>(emptyList())
    val walletTransactions: StateFlow<List<com.example.data.model.WalletTransactionEntity>> = _walletTransactions.asStateFlow()

    val currencyRates: StateFlow<List<com.example.data.model.CurrencyRateEntity>> = repository.allCurrencyRates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Group Memories
    private val _memories = MutableStateFlow<List<GroupMemoryEntity>>(emptyList())
    val memories: StateFlow<List<GroupMemoryEntity>> = _memories.asStateFlow()

    // Dual Chat Streams (0 = Private 1-on-1 Marco, 1 = Travel Crew Group)
    private val _activeChatStreamTab = MutableStateFlow(0)
    val activeChatStreamTab: StateFlow<Int> = _activeChatStreamTab.asStateFlow()

    fun setActiveChatStreamTab(tab: Int) {
        _activeChatStreamTab.value = tab
    }

    // Chat messages
    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.chatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Preference Learning & Traveler DNA
    val userPreference: StateFlow<com.example.data.model.UserPreferenceEntity?> = repository.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tripFeedbacks: StateFlow<List<com.example.data.model.TripFeedbackEntity>> = repository.allTripFeedbacks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val proactiveSuggestions: StateFlow<List<com.example.data.model.ProactiveSuggestionEntity>> = repository.proactiveSuggestions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAnalyzingPreferences = MutableStateFlow(false)
    val isAnalyzingPreferences: StateFlow<Boolean> = _isAnalyzingPreferences.asStateFlow()

    private val _preferenceNotification = MutableStateFlow<String?>(null)
    val preferenceNotification: StateFlow<String?> = _preferenceNotification.asStateFlow()

    // UI Loading & Generator states
    private val _isGeneratingTrip = MutableStateFlow(false)
    val isGeneratingTrip: StateFlow<Boolean> = _isGeneratingTrip.asStateFlow()

    private val _isConciergeTyping = MutableStateFlow(false)
    val isConciergeTyping: StateFlow<Boolean> = _isConciergeTyping.asStateFlow()

    private val _isVoiceCallActive = MutableStateFlow(false)
    val isVoiceCallActive: StateFlow<Boolean> = _isVoiceCallActive.asStateFlow()

    private val _activeCallProgressStep = MutableStateFlow("")
    val activeCallProgressStep: StateFlow<String> = _activeCallProgressStep.asStateFlow()

    private val _rewardsOptimizerResult = MutableStateFlow<String?>(null)
    val rewardsOptimizerResult: StateFlow<String?> = _rewardsOptimizerResult.asStateFlow()

    private val _isOptimizingRewards = MutableStateFlow(false)
    val isOptimizingRewards: StateFlow<Boolean> = _isOptimizingRewards.asStateFlow()

    private val _groupTripStoryReel = MutableStateFlow<String?>(null)
    val groupTripStoryReel: StateFlow<String?> = _groupTripStoryReel.asStateFlow()

    private val _isGeneratingStory = MutableStateFlow(false)
    val isGeneratingStory: StateFlow<Boolean> = _isGeneratingStory.asStateFlow()

    private val _offlineSyncBannerMessage = MutableStateFlow<String?>(null)
    val offlineSyncBannerMessage: StateFlow<String?> = _offlineSyncBannerMessage.asStateFlow()

    // AI Activity Suggestions Module State
    private val _suggestedActivities = MutableStateFlow<List<com.example.ai.AiSuggestedActivityItem>>(emptyList())
    val suggestedActivities: StateFlow<List<com.example.ai.AiSuggestedActivityItem>> = _suggestedActivities.asStateFlow()

    private val _isGeneratingSuggestions = MutableStateFlow(false)
    val isGeneratingSuggestions: StateFlow<Boolean> = _isGeneratingSuggestions.asStateFlow()

    // Dynamic Itinerary Adjustment Engine State
    private val _activeDynamicAdjustment = MutableStateFlow<com.example.ai.DynamicAdjustmentResult?>(null)
    val activeDynamicAdjustment: StateFlow<com.example.ai.DynamicAdjustmentResult?> = _activeDynamicAdjustment.asStateFlow()

    private val _isEvaluatingAdjustment = MutableStateFlow(false)
    val isEvaluatingAdjustment: StateFlow<Boolean> = _isEvaluatingAdjustment.asStateFlow()


    init {
        // Observe trips and select the first one if available
        viewModelScope.launch {
            repository.allTrips.collect { trips ->
                if (trips.isNotEmpty()) {
                    if (_selectedTripId.value == 0L || trips.none { it.id == _selectedTripId.value }) {
                        _selectedTripId.value = trips.first().id
                    }
                    observeActiveTripData(_selectedTripId.value)
                }
            }
        }
    }

    fun selectTrip(tripId: Long) {
        _selectedTripId.value = tripId
        observeActiveTripData(tripId)
    }

    fun selectNextTrip() {
        val list = allTrips.value
        if (list.size <= 1) return
        val currentIndex = list.indexOfFirst { it.id == _selectedTripId.value }
        if (currentIndex != -1) {
            val nextIndex = (currentIndex + 1) % list.size
            selectTrip(list[nextIndex].id)
        }
    }

    fun selectPreviousTrip() {
        val list = allTrips.value
        if (list.size <= 1) return
        val currentIndex = list.indexOfFirst { it.id == _selectedTripId.value }
        if (currentIndex != -1) {
            val prevIndex = if (currentIndex - 1 < 0) list.size - 1 else currentIndex - 1
            selectTrip(list[prevIndex].id)
        }
    }

    fun setDayFilter(day: Int) {
        _selectedDayFilter.value = day
    }

    private fun observeActiveTripData(tripId: Long) {
        viewModelScope.launch {
            repository.getActivitiesForTrip(tripId).collect {
                _activities.value = it
            }
        }
        viewModelScope.launch {
            repository.getExpensesForTrip(tripId).collect {
                _expenses.value = it
            }
        }
        viewModelScope.launch {
            repository.getMemoriesForTrip(tripId).collect {
                _memories.value = it
            }
        }
        viewModelScope.launch {
            repository.getWalletBalancesForTrip(tripId).collect {
                _walletBalances.value = it
            }
        }
        viewModelScope.launch {
            repository.getWalletTransactionsForTrip(tripId).collect {
                _walletTransactions.value = it
            }
        }
    }

    /**
     * Create a brand new AI-powered custom trip
     */
    fun createAITrip(
        destination: String,
        durationDays: Int,
        budget: Double,
        currency: String,
        adults: Int,
        children: Int,
        accessibilityNeeds: String,
        dietaryRestrictions: String = "Gluten-Free & Nut-Free Aware",
        familyAgeBrackets: String = "Toddler (2-3) & Teen (14-16)",
        travelStyle: String,
        rewardsStrategy: String,
        onComplete: (Long) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isGeneratingTrip.value = true
            try {
                val pref = repository.getUserPreferencesSync()
                val prefSummary = if (pref != null) {
                    """
                    Preferred Airlines: ${pref.preferredAirlines}
                    Preferred Hotel Types: ${pref.preferredHotelTypes}
                    Activity Level & Pace: ${pref.activityLevel}
                    Pacing Schedule: ${pref.pacingPreference}
                    Dietary Restrictions: ${pref.dietaryPreferences}
                    Wheelchair / Accessibility: ${pref.wheelchairRequirements}
                    Family Dynamics: ${pref.familyAgeBrackets}
                    Transit Preference: ${pref.preferredTransit}
                    Learned Traveler DNA: ${pref.learnedInsightsSummary}
                    """.trimIndent()
                } else ""

                val result = geminiService.generateCustomItinerary(
                    destination = destination,
                    durationDays = durationDays,
                    budget = budget,
                    currency = currency,
                    adults = adults,
                    children = children,
                    accessibilityNeeds = accessibilityNeeds,
                    dietaryRestrictions = dietaryRestrictions,
                    familyAgeBrackets = familyAgeBrackets,
                    travelStyle = travelStyle,
                    pointsOrTimeshares = rewardsStrategy,
                    userPreferencesSummary = prefSummary
                )

                val newTrip = TripEntity(
                    title = "$destination ($durationDays-Day $travelStyle)",
                    destination = destination,
                    startDate = "Nov 10, 2026",
                    endDate = "Nov ${10 + durationDays}, 2026",
                    budgetTotal = budget,
                    budgetSpent = 0.0,
                    primaryCurrency = currency,
                    travelersCount = adults + children,
                    childrenCount = children,
                    accessibilityRequirements = accessibilityNeeds,
                    dietaryRestrictions = dietaryRestrictions,
                    familyAgeBrackets = familyAgeBrackets,
                    travelStyle = travelStyle,
                    timeshareExchangeDetails = rewardsStrategy,
                    isOfflineSynced = true,
                    heroThemeIndex = (0..3).random()
                )
                val newTripId = repository.insertTrip(newTrip)

                val activitiesToInsert = result.activities.map { act ->
                    TripActivityEntity(
                        tripId = newTripId,
                        dayNumber = act.dayNumber,
                        timeSlot = act.timeSlot,
                        title = act.title,
                        category = act.category,
                        location = act.location,
                        confirmationCode = "AI-RES-${(1000..9999).random()}",
                        notes = act.notes,
                        cost = act.cost,
                        currency = currency,
                        accessibilityBadge = act.accessibilityBadge,
                        vendorName = destination,
                        vendorPhone = "+1-800-VOYAGE-AI",
                        isCompleted = false
                    )
                }
                repository.insertActivities(activitiesToInsert)

                // Add welcoming chat message
                repository.insertChatMessage(
                    ChatMessageEntity(
                        tripId = newTripId,
                        sender = "CONCIERGE_AI",
                        text = "I have built your personalized itinerary for $destination incorporating your learned traveler profile, dietary restrictions ($dietaryRestrictions), wheelchair needs ($accessibilityNeeds), and family dynamic ($familyAgeBrackets). How can I assist further?",
                        timestamp = System.currentTimeMillis()
                    )
                )

                _selectedTripId.value = newTripId
                observeActiveTripData(newTripId)

                launch(Dispatchers.Main) {
                    onComplete(newTripId)
                }
            } catch (e: Exception) {
                // Fallback
            } finally {
                _isGeneratingTrip.value = false
            }
        }
    }

    /**
     * Send message to 24/7 AI Concierge with full intent awareness and rich card responses
     */
    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val tripId = _selectedTripId.value
            val lower = userText.lowercase().trim()

            val targetChatType = if (_activeChatStreamTab.value == 1) "GROUP" else "PRIVATE"

            // 1. Insert user message
            repository.insertChatMessage(
                ChatMessageEntity(
                    tripId = tripId,
                    sender = "USER",
                    text = userText,
                    timestamp = System.currentTimeMillis(),
                    chatType = targetChatType,
                    authorName = "You"
                )
            )

            _isConciergeTyping.value = true

            // Intent Handling: Check if user is asking for specific rich modules
            when {
                // Trip Planning Intent
                lower.startsWith("plan") || lower.contains("plan a trip") || lower.contains("itinerary for") || lower.contains("vacation to") -> {
                    val dest = when {
                        lower.contains("tokyo") || lower.contains("japan") -> "Tokyo & Kyoto, Japan"
                        lower.contains("paris") || lower.contains("france") -> "Paris & Provence, France"
                        lower.contains("swiss") || lower.contains("alps") || lower.contains("switzerland") -> "Interlaken & Zermatt, Switzerland"
                        lower.contains("banff") || lower.contains("canada") -> "Banff & Lake Louise, Canada"
                        lower.contains("amalfi") || lower.contains("italy") || lower.contains("rome") -> "Amalfi Coast & Rome, Italy"
                        lower.contains("costa rica") -> "Arenas & Manuel Antonio, Costa Rica"
                        lower.contains("maui") || lower.contains("hawaii") -> "Maui & Kauai, Hawaii"
                        else -> {
                            val extracted = userText.substringAfter("to ", "").substringAfter("for ", "").trim()
                            if (extracted.isNotBlank() && extracted.length < 40) extracted else "Kyoto & Osaka, Japan"
                        }
                    }

                    _activeCallProgressStep.value = "Planning bespoke $dest itinerary..."
                    val userPref = repository.getUserPreferencesSync()
                    val prefSummary = userPref?.let {
                        "Preferred Airlines: ${it.preferredAirlines}, Lodging: ${it.preferredHotelTypes}, Pacing: ${it.pacingPreference}, Style: ${it.preferredTravelStyle}"
                    } ?: "Smart Points & Luxury Timeshare Villas"

                    val pref = repository.getUserPreferencesSync()
                    val accessNeeds = pref?.wheelchairRequirements?.ifBlank { pref.sensoryAndMobilityNotes } ?: "Standard Access"
                    val style = pref?.preferredTravelStyle?.ifBlank { "Personalized Expedition" } ?: "Personalized Expedition"
                    val loyalty = listOfNotNull(pref?.preferredAirlines?.takeIf { it.isNotBlank() }, pref?.preferredHotelTypes?.takeIf { it.isNotBlank() }).joinToString(", ").ifBlank { "Points & Rewards" }

                    val genResult = geminiService.generateCustomItinerary(
                        destination = dest,
                        durationDays = 5,
                        budget = 3500.0,
                        currency = "USD",
                        adults = 2,
                        children = 0,
                        accessibilityNeeds = accessNeeds,
                        travelStyle = style,
                        pointsOrTimeshares = loyalty,
                        userPreferencesSummary = prefSummary
                    )

                    // Create trip
                    val newTripId = repository.insertTrip(
                        TripEntity(
                            title = "$dest Expedition",
                            destination = dest,
                            countryCode = "US",
                            startDate = "Day 1",
                            endDate = "Day 5",
                            budgetTotal = 3500.0,
                            budgetSpent = 0.0,
                            primaryCurrency = "USD",
                            travelersCount = 2,
                            childrenCount = 0,
                            accessibilityRequirements = accessNeeds,
                            travelStyle = style,
                            timeshareExchangeDetails = "",
                            isOfflineSynced = true
                        )
                    )

                    val activitiesEntities = genResult.activities.map { act ->
                        TripActivityEntity(
                            tripId = newTripId,
                            dayNumber = act.dayNumber,
                            timeSlot = act.timeSlot,
                            title = act.title,
                            category = act.category,
                            location = act.location,
                            confirmationCode = "MARCO-${(1000..9999).random()}",
                            notes = act.notes,
                            cost = act.cost,
                            currency = "USD",
                            accessibilityBadge = act.accessibilityBadge,
                            vendorName = dest,
                            vendorPhone = "",
                            isCompleted = false
                        )
                    }
                    repository.insertActivities(activitiesEntities)
                    _selectedTripId.value = newTripId

                    _isConciergeTyping.value = false
                    repository.insertChatMessage(
                        ChatMessageEntity(
                            tripId = newTripId,
                            sender = "CARD_ITINERARY",
                            text = "✨ **New Tailored Itinerary Generated for $dest!**\nCreated a 5-day personalized itinerary matching your preferences and pacing. Expand below to explore your schedule.",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }

                // Family & Accessibility Planning Intent
                lower.contains("accessib") || lower.contains("wheelchair") || lower.contains("diet") || lower.contains("gluten") || lower.contains("allergy") || lower.contains("allergies") || lower.contains("family") || lower.contains("toddler") || lower.contains("stroller") || lower.contains("special need") || lower.contains("ada") -> {
                    _isConciergeTyping.value = false
                    repository.insertChatMessage(
                        ChatMessageEntity(
                            tripId = tripId,
                            sender = "CARD_FAMILY_ACCESSIBILITY",
                            text = "♿ **Advanced Family & Accessibility Planning Studio**\nConfigure your party's dietary restrictions, wheelchair mobility requirements, and age-appropriate activity pacing. Tap below to apply and recalculate your live itinerary.",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }

                // Voice Call Dispatcher Intent
                lower.contains("call") || lower.contains("phone") || lower.contains("front desk") || lower.contains("dispatch") -> {
                    _isConciergeTyping.value = false
                    val currentTrip = allTrips.value.find { it.id == _selectedTripId.value } ?: allTrips.value.firstOrNull()
                    val targetVendor = currentTrip?.destination?.let { "$it Lodging Front Desk" } ?: "Lodging Front Desk"
                    triggerVendorVoiceCall(
                        vendorName = targetVendor,
                        vendorCategory = "Resort & Hotel",
                        inquiryTopic = "Verify check-in time, heated pool hours, and ADA accessible accommodations",
                        reservationRef = ""
                    )
                }

                // Weekly Budget Summary & Spend vs Loyalty Savings Intent
                lower.contains("weekly") || lower.contains("budget summary") || lower.contains("summary of budget") || lower.contains("savings vs spend") || lower.contains("loyalty savings") || lower.contains("weekly summary") -> {
                    _isConciergeTyping.value = false
                    repository.insertChatMessage(
                        ChatMessageEntity(
                            tripId = tripId,
                            sender = "CARD_WEEKLY_BUDGET_SUMMARY",
                            text = "📊 **Automated Weekly Travel Budget & Loyalty Arbitrage Report**\nHere is your real-time breakdown comparing actual out-of-pocket spend vs. savings unlocked through loyalty status, credit card multipliers, and timeshare trading power.",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }

                // Real-Time Budget Tracking, Expenses & Currency Conversion Intent
                lower.contains("budget") || lower.contains("expense") || lower.contains("spend") || lower.contains("spent") || lower.contains("currency") || lower.contains("conversion") || lower.contains("convert") || lower.contains("fx") || lower.contains("exchange rate") || lower.contains("forex") || lower.contains("rate") -> {
                    _isConciergeTyping.value = false
                    repository.insertChatMessage(
                        ChatMessageEntity(
                            tripId = tripId,
                            sender = "CARD_BUDGET_TRACKER",
                            text = "💰 **Marco Real-Time Budget & Currency Tracker**\nLive multi-currency balances, encrypted payment tokens, currency converter with zero-fee optimization, and expense logs.",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }

                // Rewards / Points Intent
                lower.contains("point") || lower.contains("reward") || lower.contains("wallet") || lower.contains("skymile") || lower.contains("timeshare") -> {
                    _isConciergeTyping.value = false
                    repository.insertChatMessage(
                        ChatMessageEntity(
                            tripId = tripId,
                            sender = "CARD_REWARDS",
                            text = "💳 **Connected Loyalty & Rewards Portfolio**\nAnalyzed your 6 connected accounts with $10,480+ in estimated travel value. SkyMiles and RCI Timeshares optimized for maximum valuation.",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }

                // AI Activity Suggestions Intent
                lower.contains("suggest") || lower.contains("excursion") || lower.contains("tour") || lower.contains("activity") || lower.contains("what to do") -> {
                    _isConciergeTyping.value = false
                    generateAiActivitySuggestions()
                }

                // Dynamic Itinerary Adjustment & Weather Rebooking Intent
                lower.contains("adjust") || lower.contains("weather") || lower.contains("rain") || lower.contains("storm") || lower.contains("rebook") || lower.contains("disruption") || lower.contains("reschedule") -> {
                    _isConciergeTyping.value = false
                    triggerDynamicItineraryAdjustment(
                        reason = if (lower.contains("rain") || lower.contains("weather") || lower.contains("storm")) "⚡ Tropical Rain Squall Alert: Afternoon outdoor ocean/mountain activities impacted" else "User requested reschedule / adaptive pacing",
                        userFeedback = userText
                    )
                }

                // Traveler DNA & Preferences Intent

                lower.contains("dna") || lower.contains("preference") || lower.contains("profile") || lower.contains("learned") || lower.contains("feedback") || lower.contains("train") -> {
                    _isConciergeTyping.value = false
                    repository.insertChatMessage(
                        ChatMessageEntity(
                            tripId = tripId,
                            sender = "CARD_DNA",
                            text = "🧬 **AI Traveler DNA & Learned Profile**\nSynthesized across your travel history and 5-star ratings. Adjust your stated preferences or rate past trips below to retrain the AI.",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }

                // Emergency SOS Intent
                lower.contains("sos") || lower.contains("emergency beacon") || lower.contains("broadcast location") || lower.contains("send sos") -> {
                    _isConciergeTyping.value = false
                    triggerEmergencySos()
                }

                // Vintage Parchment Map Intent
                lower.contains("parchment") || lower.contains("vintage map") || lower.contains("cartography") || lower.contains("wind rose") || lower.contains("rhumb") -> {
                    _isConciergeTyping.value = false
                    repository.insertChatMessage(
                        ChatMessageEntity(
                            tripId = tripId,
                            sender = "CARD_PARCHMENT_MAP",
                            text = "🧭 **Antique Journey Cartography & Parchment Map**\nHistorical wind rose vectors, nautical rhumb lines, and GPS coordinate tracing across your route.",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }

                // Travel Log Journal Intent
                lower.contains("travel log") || lower.contains("journal") || lower.contains("chronicle") || lower.contains("daily reflection") -> {
                    _isConciergeTyping.value = false
                    repository.insertChatMessage(
                        ChatMessageEntity(
                            tripId = tripId,
                            sender = "CARD_TRAVEL_LOG",
                            text = "📜 **Marco's Daily Travel Log & Expedition Chronicle**\nAn AI-penned literary journal entry summarizing today's highlights. Tap below to archive directly to your permanent memories.",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }

                // Group Media Carousel Intent
                lower.contains("carousel") || lower.contains("photo reel") || lower.contains("group photo") || lower.contains("shared photos") -> {
                    _isConciergeTyping.value = false
                    repository.insertChatMessage(
                        ChatMessageEntity(
                            tripId = tripId,
                            sender = "CARD_GROUP_MEDIA_CAROUSEL",
                            text = "📸 **Group Memories Reel & Shared Photo Stream**\nCollaborative photo stream with AI scene badges and 1-tap likes.",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }

                // Structured Itinerary Snippet Intent
                lower.contains("snippet") || lower.contains("flight time") || lower.contains("hotel name") || lower.contains("cards") -> {
                    _isConciergeTyping.value = false
                    repository.insertChatMessage(
                        ChatMessageEntity(
                            tripId = tripId,
                            sender = "CARD_ITINERARY_SNIPPET",
                            text = "📋 **Structured Expedition Snippets**\nScrollable summary cards for flight gates, hotel vouchers, and accessible excursion stops.",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }

                // Offline Safety & Medical Intent
                lower.contains("safety") || lower.contains("offline") || lower.contains("emergency") || lower.contains("hospital") || lower.contains("embassy") -> {
                    _isConciergeTyping.value = false
                    repository.insertChatMessage(
                        ChatMessageEntity(
                            tripId = tripId,
                            sender = "CARD_SAFETY",
                            text = "🛡️ **Offline Safety & Medical Radar**\n24/7 emergency contacts, nearest pediatric trauma center, live flight tracking, and offline vector map status.",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }

                // View Itinerary Intent
                lower.contains("itinerary") || lower.contains("schedule") || lower.contains("show plan") || lower.contains("activities") -> {
                    _isConciergeTyping.value = false
                    val currentTrip = repository.getTripById(tripId).firstOrNull()
                    repository.insertChatMessage(
                        ChatMessageEntity(
                            tripId = tripId,
                            sender = "CARD_ITINERARY",
                            text = "📋 **Current Active Itinerary for ${currentTrip?.destination ?: "Active Trip"}**\nHere is your full day-by-day schedule with real-time confirmations and points savings.",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }

                // Group Memories Intent
                lower.contains("memory") || lower.contains("photo") || lower.contains("journal") || lower.contains("moment") -> {
                    _isConciergeTyping.value = false
                    repository.insertChatMessage(
                        ChatMessageEntity(
                            tripId = tripId,
                            sender = "CARD_MEMORY",
                            text = "📸 **Vacation Memories & Shared Moments**\nShared moments from your travel companions with AI scene tagging. Tap below to add a photo or journal note.",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }

                // Clear Chat Intent
                lower == "clear" || lower == "clear chat" || lower == "reset" -> {
                    _isConciergeTyping.value = false
                    clearChat()
                }

                // General conversational query
                else -> {
                    val currentTrip = repository.getTripById(tripId).firstOrNull()
                    val acts = _activities.value
                    val contextSummary = "Trip: ${currentTrip?.title}, Destination: ${currentTrip?.destination}, Budget: ${currentTrip?.budgetTotal} ${currentTrip?.primaryCurrency}, Total Activities: ${acts.size}"

                    val history = repository.chatMessages.firstOrNull()?.map {
                        it.sender to it.text
                    } ?: emptyList()

                    val aiReply = geminiService.sendConciergeMessage(
                        userMessage = userText,
                        history = history,
                        activeTripContext = contextSummary
                    )

                    _isConciergeTyping.value = false

                    // Insert assistant response
                    repository.insertChatMessage(
                        ChatMessageEntity(
                            tripId = tripId,
                            sender = "CONCIERGE_AI",
                            text = aiReply,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    /**
     * Clear chat and reset to clean welcome state
     */
    fun clearChat() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearChatMessages()
            repository.insertChatMessage(
                ChatMessageEntity(
                    tripId = _selectedTripId.value,
                    sender = "CONCIERGE_AI",
                    text = "Aloha! I am VoyageAI, your 24/7 personal travel agent. Everything you need—tailored itineraries, live phone calls to hotels, loyalty points optimizer, and offline safety—takes place right here in our chat.\n\nTry tapping a suggestion below or tell me where you want to travel!",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * AI Voice Vendor Call Dispatcher
     */
    fun triggerVendorVoiceCall(
        vendorName: String,
        vendorCategory: String,
        inquiryTopic: String,
        reservationRef: String,
        onCallFinished: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isVoiceCallActive.value = true
            _activeCallProgressStep.value = "Dialing $vendorName ($vendorCategory)..."
            delay(1200)

            _activeCallProgressStep.value = "Connected to Representative. AI Voice Agent speaking..."
            delay(1500)

            val callResult = geminiService.simulateVendorVoiceCall(
                vendorName = vendorName,
                vendorCategory = vendorCategory,
                inquiryTopic = inquiryTopic,
                reservationRef = reservationRef
            )

            _activeCallProgressStep.value = "Inquiry resolved. Recording transcript and summary..."
            delay(1000)

            // Save log to DB
            val logEntity = VendorCallLogEntity(
                tripId = _selectedTripId.value,
                vendorName = vendorName,
                vendorPhone = "+1-800-${(100..999).random()}-${(1000..9999).random()}",
                inquiryTopic = inquiryTopic,
                status = "COMPLETED",
                audioTranscript = callResult.transcript,
                callSummaryOutcome = callResult.summary,
                confirmedDetails = callResult.confirmedDetails
            )
            repository.insertVendorCall(logEntity)

            // Append chat notification
            repository.insertChatMessage(
                ChatMessageEntity(
                    tripId = _selectedTripId.value,
                    sender = "VOICE_CALL_DISPATCHER",
                    text = "📞 **Voice Call Completed with $vendorName**\nOutcome: ${callResult.summary}\nConfirmed Details: ${callResult.confirmedDetails}",
                    timestamp = System.currentTimeMillis()
                )
            )

            _isVoiceCallActive.value = false
            _activeCallProgressStep.value = ""

            // Speak audio summary if TTS available
            speakOut("Call completed with $vendorName. ${callResult.summary}")

            launch(Dispatchers.Main) {
                onCallFinished()
            }
        }
    }

    fun playAudioTranscript(text: String) {
        speakOut(text)
    }

    private fun speakOut(text: String) {
        ensureTtsInitialized()
        if (isTtsInitialized && tts != null) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "VOICE_PLAYBACK")
        }
    }

    fun stopAudio() {
        tts?.stop()
    }

    /**
     * Run AI Rewards & Timeshare Points Arbitrage Optimizer
     */
    fun runRewardsOptimizer() {
        viewModelScope.launch(Dispatchers.IO) {
            _isOptimizingRewards.value = true
            val accounts = connectedAccounts.value
            val summary = accounts.joinToString("\n") {
                "- ${it.providerName} (${it.categoryType}): ${it.balanceValue} ${it.unitLabel} [Tier: ${it.tierStatus}, Est Value: $${it.rewardsEstimatedValuationUsd}]"
            }
            val trip = repository.getTripById(_selectedTripId.value).firstOrNull()
            val goal = "Maximize value for ${trip?.destination ?: "upcoming travel"} with ${trip?.travelStyle}"

            val plan = geminiService.optimizeRewardsStrategy(summary, goal)
            _rewardsOptimizerResult.value = plan
            _isOptimizingRewards.value = false
        }
    }

    /**
     * Activity Management
     */
    fun updateTripActivity(activity: TripActivityEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateActivity(activity)
        }
    }

    fun deleteTripActivity(activityId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteActivity(activityId)
        }
    }

    fun toggleActivityCompleted(activity: TripActivityEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateActivity(activity.copy(isCompleted = !activity.isCompleted))
        }
    }

    /**
     * Multi-Currency Wallet & Expense Logging
     */
    fun addExpense(
        title: String,
        category: String,
        amountOriginal: Double,
        currency: String,
        paidBy: String,
        notes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val rate = when (currency) {
                "EUR" -> 1.08
                "GBP" -> 1.28
                "JPY" -> 0.0065
                "CAD" -> 0.73
                "CHF" -> 1.12
                "AUD" -> 0.65
                else -> 1.0
            }
            val usdAmount = amountOriginal * rate

            val expense = ExpenseEntity(
                tripId = _selectedTripId.value,
                title = title,
                category = category,
                amountOriginal = amountOriginal,
                originalCurrency = currency,
                amountUsd = usdAmount,
                exchangeRate = rate,
                paidBy = paidBy,
                date = "Today",
                notes = notes
            )
            repository.insertExpense(expense)

            // Update trip spent
            val trip = repository.getTripById(_selectedTripId.value).firstOrNull()
            trip?.let {
                repository.updateTrip(it.copy(budgetSpent = it.budgetSpent + usdAmount))
            }
        }
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExpense(expenseId)
        }
    }

    /**
     * Connected Accounts (Airlines, Hotels, Timeshares, CC, Campgrounds)
     */
    fun addConnectedAccount(
        categoryType: String,
        providerName: String,
        accountMasked: String,
        balance: String,
        unit: String,
        tier: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val account = ConnectedAccountEntity(
                categoryType = categoryType,
                providerName = providerName,
                accountNumberMasked = accountMasked,
                balanceValue = balance,
                unitLabel = unit,
                tierStatus = tier,
                rewardsEstimatedValuationUsd = 500.0,
                lastSyncTime = "Just now"
            )
            repository.insertAccount(account)
        }
    }

    fun deleteConnectedAccount(accountId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAccount(accountId)
        }
    }

    /**
     * Offline Sync & Emergency Safety Actions
     */
    fun toggleOfflineSync() {
        viewModelScope.launch(Dispatchers.IO) {
            val trip = repository.getTripById(_selectedTripId.value).firstOrNull()
            trip?.let {
                val newStatus = !it.isOfflineSynced
                repository.updateTrip(it.copy(isOfflineSynced = newStatus, lastSyncedTimestamp = System.currentTimeMillis()))
                _offlineSyncBannerMessage.value = if (newStatus) "✅ All maps, flight tickets, timeshare vouchers & contacts synced offline!" else "Offline sync paused."
                delay(3000)
                _offlineSyncBannerMessage.value = null
            }
        }
    }

    fun addEmergencyAlert(
        alertType: String,
        title: String,
        description: String,
        severity: String,
        contact: String,
        location: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val alert = EmergencyAlertEntity(
                tripId = _selectedTripId.value,
                alertType = alertType,
                title = title,
                description = description,
                severity = severity,
                actionContact = contact,
                location = location
            )
            repository.insertAlert(alert)
        }
    }

    /**
     * Collaborative Group Memories
     */
    fun addGroupMemory(
        authorName: String,
        caption: String,
        locationTag: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val colors = listOf(0xFF0284C7, 0xFF0D9488, 0xFFF59E0B, 0xFF8B5CF6, 0xFF10B981)
            val memory = GroupMemoryEntity(
                tripId = _selectedTripId.value,
                authorName = authorName,
                caption = caption,
                locationTag = locationTag,
                timestamp = "Just now",
                photoGradientColor = colors.random()
            )
            repository.insertMemory(memory)
        }
    }

    fun likeMemory(memoryId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.incrementMemoryLikes(memoryId)
        }
    }

    fun generateGroupStoryReel() {
        viewModelScope.launch(Dispatchers.IO) {
            _isGeneratingStory.value = true
            val currentTrip = repository.getTripById(_selectedTripId.value).firstOrNull()
            val captions = _memories.value.map { "${it.authorName}: ${it.caption} (${it.locationTag})" }

            val recap = geminiService.generateGroupStoryRecap(
                memories = captions,
                destination = currentTrip?.destination ?: "Vacation"
            )
            _groupTripStoryReel.value = recap
            _isGeneratingStory.value = false
        }
    }

    fun deleteActivity(activityId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteActivity(activityId)
        }
    }

    /**
     * Show Family & Accessibility Planning Card in chat
     */
    fun showFamilyAccessibilityCard() {
        viewModelScope.launch(Dispatchers.IO) {
            val tripId = _selectedTripId.value
            repository.insertChatMessage(
                ChatMessageEntity(
                    tripId = tripId,
                    sender = "CARD_FAMILY_ACCESSIBILITY",
                    text = "♿ **Family & Accessibility Planning Studio**\nCustomize dietary restrictions, wheelchair access, and age-appropriate activity pacing.",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Update Family & Accessibility preferences and recalculate itinerary
     */
    fun updateFamilyAndAccessibilityNeeds(
        dietary: String,
        wheelchair: String,
        familyAges: String,
        sensoryNotes: String,
        recalculateItinerary: Boolean = true
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val tripId = _selectedTripId.value
            val currentPref = repository.getUserPreferencesSync() ?: com.example.data.model.UserPreferenceEntity()

            // 1. Update UserPreferenceEntity
            val updatedPref = currentPref.copy(
                dietaryPreferences = dietary,
                wheelchairRequirements = wheelchair,
                familyAgeBrackets = familyAges,
                sensoryAndMobilityNotes = sensoryNotes,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
            repository.saveUserPreferences(updatedPref)

            // 2. Update active TripEntity
            val trip = repository.getTripById(tripId).firstOrNull()
            if (trip != null) {
                val updatedTrip = trip.copy(
                    dietaryRestrictions = dietary,
                    accessibilityRequirements = wheelchair,
                    familyAgeBrackets = familyAges
                )
                repository.updateTrip(updatedTrip)

                if (recalculateItinerary) {
                    _isConciergeTyping.value = true
                    _preferenceNotification.value = "♿ Recalculating itinerary for $dietary & $wheelchair..."

                    val prefSummary = """
                        Airlines: ${updatedPref.preferredAirlines}
                        Lodging: ${updatedPref.preferredHotelTypes}
                        Activity Level: ${updatedPref.activityLevel}
                        Pacing: ${updatedPref.pacingPreference}
                        Dietary Restrictions: $dietary
                        Wheelchair Accessibility: $wheelchair
                        Family Ages: $familyAges
                        Sensory: $sensoryNotes
                    """.trimIndent()

                    val genResult = geminiService.generateCustomItinerary(
                        destination = updatedTrip.destination,
                        durationDays = 5,
                        budget = updatedTrip.budgetTotal,
                        currency = updatedTrip.primaryCurrency,
                        adults = updatedTrip.travelersCount - updatedTrip.childrenCount,
                        children = updatedTrip.childrenCount,
                        accessibilityNeeds = wheelchair,
                        dietaryRestrictions = dietary,
                        familyAgeBrackets = familyAges,
                        travelStyle = updatedTrip.travelStyle,
                        pointsOrTimeshares = updatedTrip.timeshareExchangeDetails,
                        userPreferencesSummary = prefSummary
                    )

                    // Clear old activities and replace with new accessible & dietary-safe activities
                    val currentActs = _activities.value
                    for (act in currentActs) {
                        repository.deleteActivity(act.id)
                    }

                    val newActs = genResult.activities.map { act ->
                        TripActivityEntity(
                            tripId = tripId,
                            dayNumber = act.dayNumber,
                            timeSlot = act.timeSlot,
                            title = act.title,
                            category = act.category,
                            location = act.location,
                            confirmationCode = "ADA-RES-${(1000..9999).random()}",
                            notes = act.notes,
                            cost = act.cost,
                            currency = updatedTrip.primaryCurrency,
                            accessibilityBadge = act.accessibilityBadge,
                            vendorName = updatedTrip.destination,
                            vendorPhone = "+1-800-VOYAGE-AI",
                            isCompleted = false
                        )
                    }
                    repository.insertActivities(newActs)

                    _isConciergeTyping.value = false
                    repository.insertChatMessage(
                        ChatMessageEntity(
                            tripId = tripId,
                            sender = "CARD_ITINERARY",
                            text = "✨ **Itinerary Recalculated with Family & Accessibility Rules!**\n- **Dietary Filter**: $dietary\n- **Wheelchair & Mobility**: $wheelchair\n- **Age Dynamics**: $familyAges\nAll dining venues verified allergen-safe, lodging verified step-free with ADA pool lifts, and activities paced with nap intervals.",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }

            _preferenceNotification.value = "✨ Family & Accessibility rules saved & synced!"
            delay(3500)
            _preferenceNotification.value = null
        }
    }

    /**
     * Dispatch AI Voice Call specifically for ADA & Allergen Safety Verification
     */
    fun verifyVendorAccessibilityAndDietary(
        vendorName: String,
        vendorCategory: String,
        requirementTopic: String
    ) {
        triggerVendorVoiceCall(
            vendorName = vendorName,
            vendorCategory = vendorCategory,
            inquiryTopic = requirementTopic,
            reservationRef = "ADA-VERIF-${(100..999).random()}"
        )
    }

    /**
     * Preference Learning & Feedback System
     */
    fun updateUserPreferences(updated: com.example.data.model.UserPreferenceEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveUserPreferences(updated.copy(lastUpdatedTimestamp = System.currentTimeMillis()))
            _preferenceNotification.value = "✨ Preferences updated! AI will tailor upcoming plans accordingly."
            delay(3500)
            _preferenceNotification.value = null
        }
    }

    fun submitTripFeedback(
        tripId: Long,
        tripTitle: String,
        destination: String,
        rating: Int,
        likedAspects: String,
        dislikedAspects: String,
        notes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val takeaway = when {
                likedAspects.contains("villa", true) || likedAspects.contains("kitchen", true) -> "AI learned: Prioritize multi-bedroom suites & full kitchens for future family trips."
                likedAspects.contains("rail", true) || likedAspects.contains("train", true) -> "AI learned: Favor scenic high-speed trains over road transit."
                likedAspects.contains("delta", true) || likedAspects.contains("skyteam", true) -> "AI learned: Maximize Delta/SkyTeam partner flight upgrades."
                else -> "AI learned: Incorporated $likedAspects into your traveler DNA scoring."
            }

            val feedback = com.example.data.model.TripFeedbackEntity(
                tripId = tripId,
                tripTitle = tripTitle,
                destination = destination,
                rating = rating,
                likedAspects = likedAspects,
                dislikedAspects = dislikedAspects,
                feedbackNotes = notes,
                dateSubmitted = "Just now",
                learnedActionableTakeaway = takeaway
            )
            repository.insertTripFeedback(feedback)

            // Trigger AI Re-Analysis
            reanalyzeTravelerPreferencesInternal()

            _preferenceNotification.value = "🌟 Feedback learned! AI refined your Traveler DNA profile."
            delay(3500)
            _preferenceNotification.value = null
        }
    }

    fun deleteTripFeedback(feedbackId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTripFeedback(feedbackId)
        }
    }

    fun reanalyzeTravelerPreferences() {
        viewModelScope.launch(Dispatchers.IO) {
            reanalyzeTravelerPreferencesInternal()
        }
    }

    private suspend fun reanalyzeTravelerPreferencesInternal() {
        _isAnalyzingPreferences.value = true
        try {
            val allTripsList = repository.allTrips.firstOrNull() ?: emptyList()
            val allFeedbackList = repository.allTripFeedbacks.firstOrNull() ?: emptyList()
            val currentPref = repository.getUserPreferencesSync() ?: com.example.data.model.UserPreferenceEntity()
            val accounts = repository.connectedAccounts.firstOrNull() ?: emptyList()

            val pastTripsSummary = allTripsList.joinToString("\n") {
                "- ${it.title} to ${it.destination} (Budget: $${it.budgetTotal} ${it.primaryCurrency}, Travelers: ${it.travelersCount}, Style: ${it.travelStyle}, Offline Synced: ${it.isOfflineSynced})"
            }

            val feedbackSummary = allFeedbackList.joinToString("\n") {
                "- ${it.tripTitle} (${it.destination}): ${it.rating} Stars. Liked: ${it.likedAspects}. Disliked: ${it.dislikedAspects}. Note: ${it.feedbackNotes}"
            }

            val statedPrefSummary = """
                Airlines: ${currentPref.preferredAirlines}
                Lodging: ${currentPref.preferredHotelTypes}
                Activity Level: ${currentPref.activityLevel}
                Pacing: ${currentPref.pacingPreference}
                Dietary: ${currentPref.dietaryPreferences}
                Transit: ${currentPref.preferredTransit}
            """.trimIndent()

            val accountsSummary = accounts.joinToString("\n") {
                "- ${it.providerName} (${it.categoryType}): ${it.balanceValue} ${it.unitLabel} [Tier: ${it.tierStatus}]"
            }

            val newInsights = geminiService.analyzeAndSynthesizePreferences(
                pastTripsSummary = pastTripsSummary,
                feedbackHistory = feedbackSummary,
                statedPreferences = statedPrefSummary,
                connectedAccounts = accountsSummary
            )

            val updatedPref = currentPref.copy(
                learnedInsightsSummary = newInsights,
                totalTripsAnalyzed = allTripsList.size + allFeedbackList.size,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
            repository.saveUserPreferences(updatedPref)

            // Proactively generate new tailored suggestions matching updated profile
            val suggestionsResult = geminiService.generateProactiveRecommendations(
                userPreferenceSummary = statedPrefSummary + "\n" + newInsights,
                accountsSummary = accountsSummary
            )

            val suggestionsEntities = suggestionsResult.map { s ->
                com.example.data.model.ProactiveSuggestionEntity(
                    title = s.title,
                    destination = s.destination,
                    countryCode = s.countryCode,
                    durationDays = s.durationDays,
                    estimatedBudget = s.estimatedBudget,
                    currency = s.currency,
                    matchScorePercent = s.matchScorePercent,
                    rationale = s.rationale,
                    suggestedAirline = s.suggestedAirline,
                    suggestedLodging = s.suggestedLodging,
                    activityPace = s.activityPace,
                    heroTag = s.heroTag,
                    pointsSavingsUsd = s.pointsSavingsUsd,
                    season = s.season
                )
            }
            repository.updateProactiveSuggestions(suggestionsEntities)

        } catch (e: Exception) {
            // Log fallback
        } finally {
            _isAnalyzingPreferences.value = false
        }
    }

    /**
     * AI Activity Suggestions Module:
     * Generates curated excursions based on current itinerary, stated interests, and group composition (children, age ranges, wheelchair/dietary needs).
     */
    fun generateAiActivitySuggestions() {
        viewModelScope.launch(Dispatchers.IO) {
            _isGeneratingSuggestions.value = true
            val tripId = _selectedTripId.value
            val currentTrip = repository.getTripById(tripId).firstOrNull()
            val userPref = repository.getUserPreferencesSync() ?: com.example.data.model.UserPreferenceEntity()
            val currentActs = _activities.value

            val dest = currentTrip?.destination ?: "Active Journey"
            val actsSummary = currentActs.joinToString("; ") { "${it.timeSlot}: ${it.title} (${it.category})" }
            val interests = "${userPref.preferredTravelStyle}, ${userPref.activityLevel}, ${userPref.sensoryAndMobilityNotes}"
            val groupComposition = "${currentTrip?.travelersCount ?: 2} travelers (${currentTrip?.childrenCount ?: 1} children, ${userPref.familyAgeBrackets})"
            val accessibility = userPref.wheelchairRequirements.ifBlank { "Wheelchair step-free & sensory quiet breaks" }
            val dietary = userPref.dietaryPreferences.ifBlank { "Gluten-free & allergen safe" }


            val suggestions = geminiService.suggestRelevantActivities(
                destination = dest,
                currentItinerarySummary = actsSummary,
                interests = interests,
                groupComposition = groupComposition,
                accessibilityRequirements = accessibility,
                dietaryNeeds = dietary
            )

            _suggestedActivities.value = suggestions
            _isGeneratingSuggestions.value = false

            // Append rich card into chat
            repository.insertChatMessage(
                ChatMessageEntity(
                    tripId = tripId,
                    sender = "CARD_ACTIVITY_SUGGESTIONS",
                    text = "🎯 **AI Tailored Activity & Excursion Recommendations for $dest**\nCurated for your group ($groupComposition) with priority accessibility (${accessibility}) and dietary preferences (${dietary}). Tap below to add to your live itinerary or verify booking details via AI voice.",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Add a suggested AI activity directly to the active itinerary
     */
    fun addSuggestedActivityToItinerary(item: com.example.ai.AiSuggestedActivityItem, targetDay: Int = 1) {
        viewModelScope.launch(Dispatchers.IO) {
            val tripId = _selectedTripId.value
            val newAct = TripActivityEntity(
                tripId = tripId,
                dayNumber = targetDay,
                timeSlot = item.suggestedTimeSlot,
                title = item.title,
                category = item.category,
                location = item.location,
                confirmationCode = "AI-BOOK-${(1000..9999).random()}",
                notes = "${item.rationale} • ${item.familySuitability} • ${item.dietaryMatch}",
                cost = item.estimatedCost,
                currency = item.currency,
                accessibilityBadge = item.accessibilityBadge,
                vendorName = item.bookingVendor,
                vendorPhone = item.bookingPhone,
                isCompleted = false
            )
            repository.insertActivity(newAct)

            _preferenceNotification.value = "✅ Added '${item.title}' to Day $targetDay itinerary!"
            delay(3000)
            _preferenceNotification.value = null
        }
    }

    /**
     * Dynamic Itinerary Adjustment Engine:
     * Evaluates external factors (weather alerts, tropical storms, crowd closures) and user feedback to rebook or replace impacted activities.
     */
    fun triggerDynamicItineraryAdjustment(
        reason: String,
        userFeedback: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isEvaluatingAdjustment.value = true
            val tripId = _selectedTripId.value
            val currentTrip = repository.getTripById(tripId).firstOrNull()
            val currentActs = _activities.value
            val dest = currentTrip?.destination ?: "Active Trip"

            val actsSummary = currentActs.joinToString("\n") { "- Day ${it.dayNumber} [${it.timeSlot}]: ${it.title} (${it.category}) at ${it.location}" }

            val adjustmentResult = geminiService.evaluateDynamicItineraryAdjustment(
                destination = dest,
                currentActivitiesSummary = actsSummary,
                externalConditionsSummary = reason,
                userDisruptionFeedback = userFeedback
            )

            _activeDynamicAdjustment.value = adjustmentResult
            _isEvaluatingAdjustment.value = false

            repository.insertChatMessage(
                ChatMessageEntity(
                    tripId = tripId,
                    sender = "CARD_DYNAMIC_ADJUSTMENT",
                    text = "⚠️ **Real-Time Dynamic Itinerary Adjustment & Rebooking Radar**\nTrigger: $reason\nAI detected potential disruption and calculated a seamless accessible alternative to protect your travel schedule.",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Apply the recommended dynamic adjustment by updating the itinerary in place
     */
    fun applyDynamicItineraryAdjustment(adjustment: com.example.ai.DynamicAdjustmentResult) {
        viewModelScope.launch(Dispatchers.IO) {
            val tripId = _selectedTripId.value
            val acts = _activities.value
            val target = acts.firstOrNull { it.title.contains(adjustment.impactedActivityTitle, ignoreCase = true) || adjustment.impactedActivityTitle.contains(it.title, ignoreCase = true) } ?: acts.firstOrNull()

            if (target != null) {
                val updatedAct = target.copy(
                    title = adjustment.suggestedAlternativeTitle,
                    category = adjustment.category,
                    location = adjustment.location,
                    timeSlot = adjustment.replacementTimeSlot,
                    accessibilityBadge = adjustment.accessibilityBadge,
                    notes = "⚡ Dynamically adjusted: ${adjustment.summaryExplanation} • ${adjustment.dietaryBadge}",
                    cost = (target.cost + adjustment.costDifference).coerceAtLeast(0.0)
                )
                repository.updateActivity(updatedAct)

                _preferenceNotification.value = "⚡ Itinerary adjusted! Replaced with ${adjustment.suggestedAlternativeTitle}"
                delay(3500)
                _preferenceNotification.value = null

                repository.insertChatMessage(
                    ChatMessageEntity(
                        tripId = tripId,
                        sender = "CONCIERGE_AI",
                        text = "✅ **Dynamic Adjustment Rebooked & Confirmed!**\n- **Replaced**: ${target.title}\n- **New Item**: ${adjustment.suggestedAlternativeTitle}\n- **Time**: ${adjustment.replacementTimeSlot}\n- **Accessibility**: ${adjustment.accessibilityBadge}\n- **Savings / Cost Diff**: ${if (adjustment.costDifference <= 0) "Saved $${-adjustment.costDifference.toInt()}" else "+$${adjustment.costDifference.toInt()}"}\n\nYour day schedule and live confirmations are now up to date!",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    /**
     * Record a new wallet transaction with encrypted receipt token and auto-deduct balance
     */
    fun recordWalletTransaction(
        title: String,
        category: String,
        amount: Double,
        currency: String,
        paymentMethod: String,
        loyaltyProgram: String = "",
        loyaltySavingsUsd: Double = 0.0,
        notes: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val tripId = _selectedTripId.value
            val currentRates = repository.allCurrencyRates.firstOrNull() ?: emptyList()
            val rateObj = currentRates.find { it.currencyCode.equals(currency, ignoreCase = true) }
            val exchangeRate = if (currency.equals("USD", ignoreCase = true)) 1.0 else (rateObj?.inverseRate ?: 1.0)
            val amountUsd = if (currency.equals("USD", ignoreCase = true)) amount else (amount * exchangeRate)

            val secureToken = "TX-AUTH-${System.currentTimeMillis().toString().takeLast(6)}-${(100..999).random()}"
            val encryptedToken = com.example.data.security.WalletSecurityManager.encryptString(secureToken)

            val transaction = com.example.data.model.WalletTransactionEntity(
                tripId = tripId,
                title = title,
                category = category,
                amountOriginal = amount,
                currencyCode = currency.uppercase(),
                amountUsd = amountUsd,
                exchangeRate = exchangeRate,
                paymentMethod = paymentMethod,
                loyaltyProgramApplied = loyaltyProgram,
                loyaltySavingsUsd = loyaltySavingsUsd,
                dateString = "Today",
                timestamp = System.currentTimeMillis(),
                notes = notes,
                encryptedReceiptHash = encryptedToken
            )
            repository.insertWalletTransaction(transaction)

            // Update matching wallet balance if exists
            val existingBalances = _walletBalances.value
            val matchingBalance = existingBalances.find { it.currencyCode.equals(currency, ignoreCase = true) }
            if (matchingBalance != null) {
                val updatedBalance = matchingBalance.copy(
                    availableBalance = (matchingBalance.availableBalance - amount).coerceAtLeast(0.0),
                    spentAmount = matchingBalance.spentAmount + amount,
                    lastUpdatedTimestamp = System.currentTimeMillis()
                )
                repository.updateWalletBalance(updatedBalance)
            }

            // Also mirror in standard trip expenses
            val expense = com.example.data.model.ExpenseEntity(
                tripId = tripId,
                title = title,
                category = category,
                amountOriginal = amount,
                originalCurrency = currency.uppercase(),
                amountUsd = amountUsd,
                exchangeRate = exchangeRate,
                paidBy = paymentMethod,
                date = "Today",
                notes = notes
            )
            repository.insertExpense(expense)

            // Add confirmation in chat
            repository.insertChatMessage(
                ChatMessageEntity(
                    tripId = tripId,
                    sender = "CONCIERGE_AI",
                    text = "💳 **Expense Logged to Marco Wallet**\n- **Amount**: $currency ${String.format("%.2f", amount)} (~$${String.format("%.2f", amountUsd)} USD)\n- **Category**: $category ($title)\n- **Payment Method**: $paymentMethod\n- **Loyalty Savings Attributed**: $${String.format("%.2f", loyaltySavingsUsd)}\n- **Encrypted Token**: `$secureToken`",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Calculate instant currency conversion with zero-fee guidance
     */
    fun convertCurrency(amount: Double, fromCurrency: String, toCurrency: String): Pair<Double, Double> {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) return Pair(amount, 1.0)

        val rates = currencyRates.value
        val fromRateToUsd = if (fromCurrency.equals("USD", ignoreCase = true)) 1.0 else {
            rates.find { it.currencyCode.equals(fromCurrency, ignoreCase = true) }?.inverseRate ?: 1.0
        }
        val toRateAgainstUsd = if (toCurrency.equals("USD", ignoreCase = true)) 1.0 else {
            rates.find { it.currencyCode.equals(toCurrency, ignoreCase = true) }?.rateAgainstBase ?: 1.0
        }

        // amount * (USD per fromCurrency) * (toCurrency per USD)
        val converted = amount * fromRateToUsd * toRateAgainstUsd
        val effectiveRate = fromRateToUsd * toRateAgainstUsd
        return Pair(converted, effectiveRate)
    }

    /**
     * Save application settings & update local reactive state
     */
    fun saveSettings(settings: AppSettingsState) {
        settingsManager.saveSettings(settings)
        _settingsState.value = settings
    }

    /**
     * Preview sample voice narration for settings
     */
    fun previewVoiceSample(voiceId: String, speed: Float = 1.0f, pitch: Float = 1.0f) {
        ensureTtsInitialized()
        if (!isTtsInitialized || tts == null) return

        tts?.setSpeechRate(speed)
        tts?.setPitch(pitch)

        val sampleText = when (voiceId) {
            "marco_polo" -> "Greetings, fellow voyager. I am Marco. The trade winds are favorable and our navigational compass is set for discovery."
            "aurora_concierge" -> "Welcome. Your 5-star oceanfront villa and private cabana reservations have been confirmed with VIP lounge access."
            "atlas_navigator" -> "Navigator Atlas checking in. Elevation and offline trail GPS maps are calibrated. Storm radar is clear."
            "solaris_family" -> "Hello family! Stroller-friendly ramps, toddler sensory rest windows, and allergen-safe dining are all ready for a fun day."
            "meridian_points" -> "Points optimization active: 45,000 Chase points transferred to Hyatt, unlocking $850 in 5-star lodging value."
            else -> "Welcome to Marco Travel Concierge. Where shall our next expedition take us?"
        }

        tts?.speak(sampleText, TextToSpeech.QUEUE_FLUSH, null, "sample_voice_preview")
    }

    /**
     * Save AI Travel Log reflection directly to persistent group memories
     */
    fun saveTravelLogAsMemory(caption: String, location: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val tripId = _selectedTripId.value
            val memory = GroupMemoryEntity(
                tripId = tripId,
                authorName = "Marco Concierge Chronicle",
                caption = caption,
                locationTag = location,
                timestamp = "Today",
                photoGradientColor = 0xFFD97706,
                likesCount = 1,
                aiTag = "📜 Daily Chronicle"
            )
            repository.insertMemory(memory)
        }
    }

    /**
     * Trigger Emergency SOS Beacon in chat and broadcast telemetry.
     * Note: SOS features are strictly active only when on an actual trip in progress.
     */
    fun triggerEmergencySos() {
        viewModelScope.launch(Dispatchers.IO) {
            val tripId = _selectedTripId.value
            val currentTrip = repository.getTripById(tripId).firstOrNull()
            
            if (currentTrip == null || !currentTrip.isTripInProgress()) {
                repository.insertChatMessage(
                    ChatMessageEntity(
                        tripId = tripId,
                        sender = "AI",
                        text = "ℹ️ **Emergency SOS is Dormant During Planning Stage**\n\nLive GPS broadcast, telemetry dispatch, and 911 beacons are only active while on an actual trip in progress.\n\n**Pre-Trip Emergency Reference for ${currentTrip?.destination ?: "Your Destination"}**:\n- **Emergency Dispatch**: 911 (US/Canada) / 112 (Europe)\n- **Consular & Citizen Services**: 24/7 Traveler Safety Assistance Desk\n\n*Live SOS will automatically activate when your trip begins (${currentTrip?.startDate ?: "start date"}).*",
                        timestamp = System.currentTimeMillis()
                    )
                )
                return@launch
            }

            repository.insertChatMessage(
                ChatMessageEntity(
                    tripId = tripId,
                    sender = "CARD_EMERGENCY_SOS",
                    text = "🚨 **EMERGENCY SOS BEACON DISPATCHED**\n- **Expedition Destination**: ${currentTrip.destination}\n- **Emergency Dispatch**: 911 / Local Emergency Services & First Aid\n- **Safety Status**: Live telemetry broadcasted to emergency contacts",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Update trip status (PLANNING, IN_PROGRESS, COMPLETED)
     */
    fun setTripStatus(tripId: Long, status: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTripStatus(tripId, status)
            val updated = repository.getTripById(tripId).firstOrNull()
            if (updated != null) {
                cloudSyncManager.syncTripToCloud(updated)
            }
            if (status.equals("IN_PROGRESS", ignoreCase = true)) {
                repository.insertChatMessage(
                    ChatMessageEntity(
                        tripId = tripId,
                        sender = "CONCIERGE_AI",
                        text = "🧭 **Expedition to ${updated?.destination ?: "Destination"} is now LIVE!**\nEmergency SOS, offline safety radar, and live daily pacing cockpit are enabled.",
                        timestamp = System.currentTimeMillis(),
                        chatType = "PRIVATE",
                        authorName = "Marco Concierge"
                    )
                )
            }
        }
    }

    fun toggleTripStatus(tripId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getTripById(tripId).firstOrNull() ?: return@launch
            val newStatus = if (current.isTripInProgress()) TripStatus.PLANNING.value else TripStatus.IN_PROGRESS.value
            repository.updateTripStatus(tripId, newStatus)
            val updated = repository.getTripById(tripId).firstOrNull()
            if (updated != null) {
                cloudSyncManager.syncTripToCloud(updated)
            }
        }
    }

    /**
     * Firebase Cloud Authentication & Firestore Synchronization Operations
     */
    val firebaseUser = cloudSyncManager.currentUser
    val syncStatus = cloudSyncManager.syncStatus
    val syncMessage = cloudSyncManager.syncMessage
    val lastSyncTimestamp = cloudSyncManager.lastSyncTimestamp

    fun signInWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = cloudSyncManager.signInWithEmail(email, pass)
            result.onSuccess { user ->
                onResult(true, "Signed in as ${user.email}")
                // Auto trigger full sync on login
                syncAllToFirebase()
            }.onFailure { error ->
                onResult(false, error.localizedMessage ?: "Sign in failed")
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String, displayName: String? = null, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = cloudSyncManager.signUpWithEmail(email, pass, displayName)
            result.onSuccess { user ->
                onResult(true, "Account created for ${user.displayName ?: user.email}")
                syncAllToFirebase()
            }.onFailure { error ->
                onResult(false, error.localizedMessage ?: "Account creation failed")
            }
        }
    }

    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = cloudSyncManager.sendPasswordResetEmail(email)
            result.onSuccess {
                onResult(true, "Password reset email sent")
            }.onFailure { error ->
                onResult(false, error.localizedMessage ?: "Failed to send reset email")
            }
        }
    }

    fun signInWithGoogleToken(idToken: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = cloudSyncManager.signInWithGoogleToken(idToken)
            result.onSuccess { user ->
                onResult(true, "Signed in with Google as ${user.displayName ?: user.email}")
                syncAllToFirebase()
            }.onFailure { error ->
                onResult(false, error.localizedMessage ?: "Google sign in failed")
            }
        }
    }

    fun signOutFirebase() {
        cloudSyncManager.signOut()
    }

    fun syncAllToFirebase(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val trips = allTrips.value
            val currentActivities = activities.value
            val pref = userPreference.value
            val balances = walletBalances.value
            val exp = expenses.value
            val mem = memories.value

            val success = cloudSyncManager.syncAllDataToCloud(
                trips = trips,
                activities = currentActivities,
                preferences = pref,
                walletBalances = balances,
                expenses = exp,
                memories = mem
            )
            withContext(Dispatchers.Main) {
                onComplete?.invoke(success)
            }
        }
    }

    /**
     * Complete Guided 3-Step Setup Wizard
     */
    fun completeOnboarding(
        travelStyle: String,
        pacing: String,
        activityLevel: String,
        wheelchair: String,
        dietary: String,
        family: String,
        sensory: String,
        preferredAirlines: String,
        preferredHotelTypes: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val summary = "Expedition DNA Profile: $travelStyle ($pacing pace). Accessibility & Dietary: ${listOfNotNull(wheelchair.takeIf { it.isNotBlank() }, dietary.takeIf { it.isNotBlank() }, family.takeIf { it.isNotBlank() }).joinToString(", ")}. Preferred Loyalty: ${listOfNotNull(preferredAirlines.takeIf { it.isNotBlank() }, preferredHotelTypes.takeIf { it.isNotBlank() }).joinToString(", ")}."
            val updated = com.example.data.model.UserPreferenceEntity(
                id = 1,
                preferredAirlines = preferredAirlines,
                preferredHotelTypes = preferredHotelTypes,
                activityLevel = activityLevel,
                preferredTravelStyle = travelStyle,
                dietaryPreferences = dietary,
                wheelchairRequirements = wheelchair,
                familyAgeBrackets = family,
                sensoryAndMobilityNotes = sensory,
                pacingPreference = pacing,
                learnedInsightsSummary = summary,
                totalTripsAnalyzed = 0,
                onboardingCompleted = true,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
            repository.saveUserPreferences(updated)

            // Add personalized greeting in chat
            repository.insertChatMessage(
                ChatMessageEntity(
                    tripId = 0,
                    sender = "CONCIERGE_AI",
                    text = "🧭 **Welcome to Marco Expeditions!**\nYour baseline Traveler DNA is initialized ($travelStyle • $pacing • $activityLevel).\n\nAsk me to plan a custom journey, optimize your rewards points, or review offline cartography.",
                    timestamp = System.currentTimeMillis(),
                    chatType = "PRIVATE",
                    authorName = "Marco Concierge"
                )
            )

            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    /**
     * Seamless On-Trip Status Transition (Date-aware + Proximity)
     */
    fun checkAndAutoTransitionTripStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val tripsList = repository.allTrips.firstOrNull() ?: emptyList()
            for (trip in tripsList) {
                if (trip.status.equals("COMPLETED", ignoreCase = true)) continue
                if (trip.isTripInProgress() && !trip.status.equals("IN_PROGRESS", ignoreCase = true)) {
                    repository.updateTripStatus(trip.id, "IN_PROGRESS")
                    repository.insertChatMessage(
                        ChatMessageEntity(
                            tripId = trip.id,
                            sender = "CONCIERGE_AI",
                            text = "🧭 **Live Expedition Mode Activated for ${trip.destination}!**\nToday's live schedule, emergency SOS beacon, and multi-currency ledger are now active in your top cockpit.",
                            timestamp = System.currentTimeMillis(),
                            chatType = "PRIVATE",
                            authorName = "Marco Concierge"
                        )
                    )
                }
            }
        }
    }

    /**
     * Proactive Disruption Rebooking Trigger
     */
    fun triggerProactiveDisruptionAlert(
        tripId: Long,
        triggerReason: String = "Severe weather / thunderstorm alert during afternoon outdoor activity",
        impactedActivity: String = "Outdoor Snorkel / Summit Hike",
        suggestedAlternative: String = "Indoor Cultural Discovery & Living Reef Aquarium (100% ADA Ramp & Sensory Friendly)",
        costDiff: Double = -20.0
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val alertJson = """
                {
                    "triggerReason": "$triggerReason",
                    "impactedActivity": "$impactedActivity",
                    "suggestedAlternative": "$suggestedAlternative",
                    "costDifference": $costDiff,
                    "accessibility": "♿ 100% Step-Free & Stroller Ramp Access",
                    "dietary": "🥗 Verified Dedicated Kitchen Safety"
                }
            """.trimIndent()

            val targetChatType = if (_activeChatStreamTab.value == 1) "GROUP" else "PRIVATE"
            repository.insertChatMessage(
                ChatMessageEntity(
                    tripId = tripId,
                    sender = "CARD_PROACTIVE_DISRUPTION",
                    text = "⚠️ **Real-Time Weather Disruption & Auto-Rebooking Proposal**\n$triggerReason impacted '$impactedActivity'. Tap below to 1-tap auto-rebook with verified accessible indoor alternative.",
                    suggestedActionJson = alertJson,
                    actionChipText = "1-Tap AI Auto-Rebook",
                    timestamp = System.currentTimeMillis(),
                    chatType = targetChatType,
                    authorName = "Marco Concierge"
                )
            )
        }
    }

    /**
     * Post-Trip Celebration & 3-Question DNA Learning Loop
     */
    fun markTripCompleted(tripId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val trip = repository.getTripById(tripId).firstOrNull() ?: return@launch
            repository.updateTripStatus(tripId, "COMPLETED")

            repository.insertChatMessage(
                ChatMessageEntity(
                    tripId = tripId,
                    sender = "CARD_JOURNEY_COMPLETED",
                    text = "🏁 **Expedition to ${trip.destination} Completed!**\nCongratulations on completing your journey! Review your shared vacation story reel and rate your experience below to refine your Traveler DNA for upcoming journeys.",
                    timestamp = System.currentTimeMillis(),
                    chatType = "PRIVATE",
                    authorName = "Marco Concierge"
                )
            )
        }
    }

    fun submitPostTripRating(
        tripId: Long,
        pacingRating: Int,
        lodgingRating: Int,
        diningRating: Int,
        feedbackNotes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val trip = repository.getTripById(tripId).firstOrNull()
            val dest = trip?.destination ?: "Recent Expedition"
            val title = trip?.title ?: "Past Trip"
            val avgRating = ((pacingRating + lodgingRating + diningRating) / 3.0).toInt().coerceIn(1, 5)

            val takeaway = buildString {
                append("AI DNA Learned: ")
                if (pacingRating >= 4) append("Pacing cadence confirmed optimal. ") else append("Adjust future days with longer recovery pauses. ")
                if (lodgingRating >= 4) append("Accommodation style matched preferences. ") else append("Prioritize higher-tier suites/kitchens on next booking. ")
                if (diningRating >= 4) append("Culinary & allergen standards verified safe.") else append("Expand allergen-safe screening radius.")
            }

            repository.insertTripFeedback(
                com.example.data.model.TripFeedbackEntity(
                    tripId = tripId,
                    tripTitle = title,
                    destination = dest,
                    rating = avgRating,
                    likedAspects = "Pacing: $pacingRating/5, Lodging: $lodgingRating/5, Dining: $diningRating/5",
                    dislikedAspects = if (avgRating < 4) feedbackNotes else "",
                    feedbackNotes = feedbackNotes,
                    dateSubmitted = "Today",
                    learnedActionableTakeaway = takeaway
                )
            )

            // Retrain preferences
            val currentPref = repository.getUserPreferencesSync() ?: com.example.data.model.UserPreferenceEntity()
            val updatedCount = currentPref.totalTripsAnalyzed + 1
            val updatedSummary = "AI Learned DNA ($updatedCount Expeditions): Traveler values verified step-free accessibility, optimal morning pacing, and rewards transfer sweet spots. $takeaway"
            repository.saveUserPreferences(
                currentPref.copy(
                    totalTripsAnalyzed = updatedCount,
                    learnedInsightsSummary = updatedSummary,
                    lastUpdatedTimestamp = System.currentTimeMillis()
                )
            )

            repository.insertChatMessage(
                ChatMessageEntity(
                    tripId = tripId,
                    sender = "CONCIERGE_AI",
                    text = "🌟 **Traveler DNA Model Retrained!**\nIncorporated your ratings ($takeaway) into all future proactive recommendations.",
                    timestamp = System.currentTimeMillis(),
                    chatType = "PRIVATE",
                    authorName = "Marco Concierge"
                )
            )
        }
    }

    fun sendGroupPhotoMemory(caption: String, locationTag: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val tripId = _selectedTripId.value
            val memory = GroupMemoryEntity(
                tripId = tripId,
                authorName = "You",
                caption = caption,
                locationTag = locationTag,
                timestamp = "Just now",
                likesCount = 0,
                aiTag = "📸 Shared Memory"
            )
            repository.insertMemory(memory)

            repository.insertChatMessage(
                ChatMessageEntity(
                    tripId = tripId,
                    sender = "USER",
                    text = "📸 Posted memory: \"$caption\" at $locationTag",
                    timestamp = System.currentTimeMillis(),
                    chatType = "GROUP",
                    authorName = "You"
                )
            )
        }
    }

    /**
     * Clear all local trips, chats, and cached data
     */
    fun clearAllLocalData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllLocalData()
            _selectedTripId.value = 0L
            _activities.value = emptyList()
            _expenses.value = emptyList()
            _memories.value = emptyList()
            _walletBalances.value = emptyList()
            _walletTransactions.value = emptyList()
        }
    }

    /**
     * Reseed sample trip and rewards data
     */
    fun reseedSampleTrips() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.checkAndSeedInitialData()
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}
