package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ConnectedAccountEntity
import com.example.data.model.GroupMemoryEntity
import com.example.data.model.TripActivityEntity
import com.example.data.model.TripBriefEntity
import com.example.data.model.BriefTension
import com.example.data.model.TripEntity
import com.example.data.model.isTripInProgress
import com.example.data.model.TripStatus
import com.example.data.model.UserPreferenceEntity
import com.example.data.model.VendorCallLogEntity
import com.example.ui.components.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.theme.*
import com.example.viewmodel.TravelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConciergeChatScreen(
    viewModel: TravelViewModel,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onOpenItinerary: () -> Unit = {},
    onOpenSafetyMap: () -> Unit = {},
    onOpenMemories: () -> Unit = {},
    onOpenVendorCall: (vendorName: String, question: String) -> Unit = { _, _ -> },
    onOpenPreferences: () -> Unit = {},
    onOpenPlanTrip: () -> Unit = {},
    onOpenAuth: () -> Unit = {}
) {
    val messages by viewModel.chatMessages.collectAsState()
    val activeChatStreamTab by viewModel.activeChatStreamTab.collectAsState()
    val isTyping by viewModel.isConciergeTyping.collectAsState()
    val tripsList by viewModel.allTrips.collectAsState()
    val selectedTripId by viewModel.selectedTripId.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val connectedAccounts by viewModel.connectedAccounts.collectAsState()
    val userPref by viewModel.userPreference.collectAsState()
    val vendorCalls by viewModel.vendorCalls.collectAsState()
    val memoriesList by viewModel.memories.collectAsState()
    val offlineMsg by viewModel.offlineSyncBannerMessage.collectAsState()
    val firebaseUser by viewModel.firebaseUser.collectAsState()
    val suggestedActivities by viewModel.suggestedActivities.collectAsState()
    val isGeneratingSuggestions by viewModel.isGeneratingSuggestions.collectAsState()
    val activeAdjustment by viewModel.activeDynamicAdjustment.collectAsState()
    val isEvaluatingAdjustment by viewModel.isEvaluatingAdjustment.collectAsState()
    val walletBalances by viewModel.walletBalances.collectAsState()
    val walletTransactions by viewModel.walletTransactions.collectAsState()
    val currencyRates by viewModel.currencyRates.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var isQuickActionSheetOpen by remember { mutableStateOf(false) }
    var isTripMenuExpanded by remember { mutableStateOf(false) }
    var selectedActivityForSnippetDetail by remember { mutableStateOf<com.example.data.model.TripActivityEntity?>(null) }
    val listState = rememberLazyListState()

    val currentTrip = tripsList.find { it.id == selectedTripId } ?: tripsList.firstOrNull()

    val tripPagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tripsList.size.coerceAtLeast(1) }
    )

    LaunchedEffect(selectedTripId, tripsList) {
        val index = tripsList.indexOfFirst { it.id == selectedTripId }
        if (index != -1 && index != tripPagerState.currentPage && index < tripsList.size) {
            tripPagerState.animateScrollToPage(index)
        }
    }

    LaunchedEffect(tripPagerState.currentPage) {
        if (tripPagerState.currentPage in tripsList.indices) {
            val targetTrip = tripsList[tripPagerState.currentPage]
            if (targetTrip.id != selectedTripId) {
                viewModel.selectTrip(targetTrip.id)
            }
        }
    }

    val isTripActive = currentTrip?.isTripInProgress() == true
    val suggestionChips = remember(isTripActive) {
        buildList {
            add("Show Current Itinerary")
            add("Real-Time Budget & FX Tracker")
            add("AI Activity & Excursion Radar")
            add("Pre-Trip Safety & Offline Pack")
            if (isTripActive) {
                add("Emergency SOS Beacon")
            }
            add("Plan New Trip")
            add("My Traveler DNA & Profile")
        }
    }

    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 80) {
                        viewModel.selectPreviousTrip()
                    } else if (dragAmount < -80) {
                        viewModel.selectNextTrip()
                    }
                }
            }
    ) {
        // TOP APP BAR: AI Status + Active Trip Selector + Clear Chat
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, LuxuryBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AI Agent Brand & Status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(LuxuryCardElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = "Marco Expedition Concierge",
                            tint = ChampagneGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Marco",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                }

                // Actions: Trip Context Pill + SOS + Settings + Clear Chat
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Active Trip Dropdown Pill
                    Box {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = OceanBlue.copy(alpha = 0.12f),
                            modifier = Modifier
                                .clickable { isTripMenuExpanded = true }
                                .testTag("trip_context_selector")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val destLabel = currentTrip?.destination?.split(",")?.firstOrNull()?.trim() ?: "Plan Trip"
                                Text(
                                    text = destLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = WaypointCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = "Switch Trip",
                                    tint = WaypointCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isTripMenuExpanded,
                            onDismissRequest = { isTripMenuExpanded = false }
                        ) {
                            if (tripsList.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text("Plan New AI Trip...", fontWeight = FontWeight.Bold, color = WaypointCyan)
                                    },
                                    onClick = {
                                        isTripMenuExpanded = false
                                        onOpenPlanTrip()
                                    }
                                )
                            } else {
                                tripsList.forEach { trip ->
                                    val active = trip.isTripInProgress()
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(trip.title, fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = if (active) EmeraldGreen.copy(alpha = 0.15f) else OceanBlue.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = if (active) "On-trip" else "Planning",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                color = if (active) EmeraldGreen else WaypointCyan,
                                                                fontWeight = FontWeight.Bold
                                                            ),
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    "${trip.destination} • ${trip.startDate}",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = TextAtlasSecondary)
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectTrip(trip.id)
                                            isTripMenuExpanded = false
                                        }
                                    )
                                }
                                currentTrip?.let { trip ->
                                    HorizontalDivider(color = LuxuryBorder)
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (trip.isTripInProgress()) "Switch to Planning Mode (Hide SOS)" else "Start Vacation / On-Trip Mode (Activate SOS)",
                                                fontWeight = FontWeight.Bold,
                                                color = if (trip.isTripInProgress()) WaypointCyan else EmeraldGreen
                                            )
                                        },
                                        onClick = {
                                            viewModel.toggleTripStatus(trip.id)
                                            isTripMenuExpanded = false
                                        }
                                    )
                                }
                                HorizontalDivider(color = LuxuryBorder)
                                DropdownMenuItem(
                                    text = {
                                        Text("Plan Another Trip...", fontWeight = FontWeight.Medium, color = WaypointCyan)
                                    },
                                    onClick = {
                                        isTripMenuExpanded = false
                                        onOpenPlanTrip()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Emergency SOS Beacon Button (Strictly only visible while actively on an actual trip)
                    if (isTripActive) {
                        IconButton(
                            onClick = { viewModel.triggerEmergencySos() },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(WaxSealCrimson.copy(alpha = 0.15f))
                                .testTag("sos_beacon_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = "Emergency SOS",
                                tint = WaxSealCrimson,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                    }

                    // Account / Profile Button
                    IconButton(
                        onClick = {
                            if (firebaseUser != null) {
                                onOpenSettings()
                            } else {
                                onOpenAuth()
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(34.dp)
                            .testTag("top_bar_account_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = if (firebaseUser != null) "Account: ${firebaseUser?.displayName ?: firebaseUser?.email}" else "Sign In",
                            tint = if (firebaseUser != null) VenetianGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    // Settings Button
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(34.dp)
                            .testTag("open_settings_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings & Voices",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    // Reset / Clear Chat Button
                    IconButton(
                        onClick = { viewModel.clearChat() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(34.dp)
                            .testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Chat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // ACTIVE ITINERARY HORIZONTAL PAGER (Swipeable between multiple expeditions)
        if (tripsList.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    HorizontalPager(
                        state = tripPagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        pageSpacing = 8.dp
                    ) { page ->
                        val trip = tripsList.getOrNull(page)
                        if (trip != null) {
                            val isSelected = trip.id == selectedTripId
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = LuxuryCard
                                ),
                                border = BorderStroke(1.dp, LuxuryBorder),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        viewModel.selectTrip(trip.id)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(LuxuryCardElevated),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FlightTakeoff,
                                                contentDescription = null,
                                                tint = ChampagneGold,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = trip.title,
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = TextPrimary
                                                    ),
                                                    maxLines = 1
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                val inProgress = trip.isTripInProgress()
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = LuxuryCardElevated,
                                                    border = BorderStroke(1.dp, LuxuryBorder)
                                                ) {
                                                    Text(
                                                        text = if (inProgress) "Active" else "Planning",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = TextSecondary,
                                                            fontWeight = FontWeight.Medium
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${trip.destination} • ${trip.startDate} to ${trip.endDate}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextSecondary
                                                ),
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = onOpenItinerary,
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = "Open Full Itinerary",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (tripsList.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tripsList.indices.forEach { index ->
                                val isCurrent = tripPagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(if (isCurrent) 14.dp else 5.dp, 5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (isCurrent) ChampagneGold else TextMuted.copy(alpha = 0.35f))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Offline Banner if active
        if (!offlineMsg.isNullOrBlank()) {
            Surface(
                color = EmeraldGreen.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = offlineMsg ?: "",
                    style = MaterialTheme.typography.labelSmall.copy(color = EmeraldGreen, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // DUAL CHAT STREAM TABS (Marco vs Travel Crew)
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            SegmentedButton(
                selected = activeChatStreamTab == 0,
                onClick = { viewModel.setActiveChatStreamTab(0) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Marco",
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text("Marco") }
            )
            SegmentedButton(
                selected = activeChatStreamTab == 1,
                onClick = { viewModel.setActiveChatStreamTab(1) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = {
                    Icon(
                        imageVector = Icons.Default.NaturePeople,
                        contentDescription = "Travel Crew",
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text("Travel Crew") }
            )
        }

        val displayedMessages = remember(messages, activeChatStreamTab) {
            if (activeChatStreamTab == 1) {
                messages.filter { it.chatType.equals("GROUP", ignoreCase = true) }
            } else {
                messages.filter { it.chatType.equals("PRIVATE", ignoreCase = true) || it.chatType.isBlank() }
            }
        }

        // CHAT MESSAGE STREAM
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (displayedMessages.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = LuxuryCard),
                        border = BorderStroke(1.dp, LuxuryBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(LuxuryCardElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (activeChatStreamTab == 1) Icons.Default.NaturePeople else Icons.Default.Explore,
                                        contentDescription = null,
                                        tint = ChampagneGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (activeChatStreamTab == 1) "Travel Crew" else "Marco Concierge",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (activeChatStreamTab == 1)
                                    "Coordinate activities, share trip memories, and plan with your travel party.\n\nTap '+' below to share a group photo."
                                else
                                    "Research destinations, check loyalty rewards, arrange accessible itineraries, and coordinate travel safety.\n\nType a message or tap '+' below to begin.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                        }
                    }
                }
            }

            items(displayedMessages, key = { it.id }) { msg ->
                when (msg.sender) {
                    "USER" -> {
                        UserMessageBubble(text = msg.text)
                    }
                    "CARD_ITINERARY" -> {
                        ChatItineraryCard(
                            message = msg,
                            trip = currentTrip,
                            activities = activities,
                            onTriggerCall = {
                                viewModel.sendChatMessage("Call resort front desk for pool hours & check-in")
                            },
                            onToggleOffline = { viewModel.toggleOfflineSync() },
                            onPlayTts = { viewModel.playAudioTranscript(msg.text) }
                        )
                    }
                    "VOICE_CALL_DISPATCHER" -> {
                        ChatVoiceCallCard(
                            message = msg,
                            callLog = vendorCalls.firstOrNull(),
                            onPlayAudio = {
                                val log = vendorCalls.firstOrNull()
                                viewModel.playAudioTranscript("Outcome: ${log?.callSummaryOutcome ?: msg.text}. Details: ${log?.confirmedDetails ?: ""}")
                            }
                        )
                    }
                    "CARD_REWARDS" -> {
                        ChatRewardsCard(
                            message = msg,
                            accounts = connectedAccounts,
                            onRunOptimizer = {
                                viewModel.sendChatMessage("Analyze best points transfer partners and timeshare upgrades")
                            },
                            onPlayTts = { viewModel.playAudioTranscript(msg.text) }
                        )
                    }
                    "CARD_DNA" -> {
                        ChatTravelerDnaCard(
                            message = msg,
                            preference = userPref,
                            linkedProgramCount = connectedAccounts.size,
                            onRefineDna = onOpenPreferences,
                            onPlayTts = { viewModel.playAudioTranscript(msg.text) }
                        )
                    }
                    "CARD_SAFETY" -> {
                        ChatOfflineSafetyCard(
                            message = msg,
                            trip = currentTrip,
                            onToggleOffline = { viewModel.toggleOfflineSync() },
                            onPlayTts = { viewModel.playAudioTranscript(msg.text) }
                        )
                    }
                    "CARD_FAMILY_ACCESSIBILITY" -> {
                        ChatFamilyAccessibilityCard(
                            message = msg,
                            trip = currentTrip,
                            userPref = userPref,
                            onSavePreferences = { dietary, wheelchair, familyAges, sensoryNotes, recalculate ->
                                viewModel.updateFamilyAndAccessibilityNeeds(
                                    dietary = dietary,
                                    wheelchair = wheelchair,
                                    familyAges = familyAges,
                                    sensoryNotes = sensoryNotes,
                                    recalculateItinerary = recalculate
                                )
                            },
                            onVerifyVendorAda = { vendorName, category, topic ->
                                viewModel.verifyVendorAccessibilityAndDietary(
                                    vendorName = vendorName,
                                    vendorCategory = category,
                                    requirementTopic = topic
                                )
                            },
                            onPlayTts = { viewModel.playAudioTranscript(msg.text) }
                        )
                    }
                    "CARD_ACTIVITY_SUGGESTIONS" -> {
                        ChatActivitySuggestionsCard(
                            message = msg,
                            trip = currentTrip,
                            userPref = userPref,
                            suggestions = suggestedActivities,
                            isGenerating = isGeneratingSuggestions,
                            onRefresh = { viewModel.generateAiActivitySuggestions() },
                            onAddActivity = { item, targetDay ->
                                viewModel.addSuggestedActivityToItinerary(item, targetDay)
                            },
                            onVerifyVendor = { vendorName, topic ->
                                viewModel.verifyVendorAccessibilityAndDietary(
                                    vendorName = vendorName,
                                    vendorCategory = "Activity & Excursion",
                                    requirementTopic = topic
                                )
                            },
                            onPlayTts = { viewModel.playAudioTranscript(msg.text) }
                        )
                    }
                    "CARD_DYNAMIC_ADJUSTMENT" -> {
                        ChatDynamicAdjustmentCard(
                            message = msg,
                            adjustment = activeAdjustment,
                            isEvaluating = isEvaluatingAdjustment,
                            onApplyAdjustment = { adj ->
                                viewModel.applyDynamicItineraryAdjustment(adj)
                            },
                            onSimulateVoiceRebook = { vendorName, topic ->
                                viewModel.triggerVendorVoiceCall(
                                    vendorName = vendorName,
                                    vendorCategory = "Activity Rebooking",
                                    inquiryTopic = topic,
                                    reservationRef = "REBOOK-ADJ-${(100..999).random()}"
                                )
                            },
                            onPlayTts = { viewModel.playAudioTranscript(msg.text) }
                        )
                    }
                    "CARD_MEMORY" -> {

                        ChatGroupMemoryCard(
                            message = msg,
                            memories = memoriesList,
                            onAddMemory = { caption, tag ->
                                viewModel.addGroupMemory(
                                    authorName = "You",
                                    caption = caption,
                                    locationTag = tag
                                )
                            }
                        )
                    }
                    "CARD_BUDGET_TRACKER" -> {
                        ChatBudgetTrackerWidget(
                            message = msg,
                            trip = currentTrip,
                            walletBalances = walletBalances,
                            transactions = walletTransactions,
                            currencyRates = currencyRates,
                            onAddExpense = { title, cat, amt, curr, payMethod, loyProg, loySav, notes ->
                                viewModel.recordWalletTransaction(
                                    title = title,
                                    category = cat,
                                    amount = amt,
                                    currency = curr,
                                    paymentMethod = payMethod,
                                    loyaltyProgram = loyProg,
                                    loyaltySavingsUsd = loySav,
                                    notes = notes
                                )
                            },
                            onPlayTts = { viewModel.playAudioTranscript(msg.text) },
                            onOpenFullWallet = onOpenWallet
                        )
                    }
                    "CARD_WEEKLY_BUDGET_SUMMARY" -> {
                        ChatWeeklyBudgetSummaryCard(
                            message = msg,
                            trip = currentTrip,
                            walletBalances = walletBalances,
                            transactions = walletTransactions,
                            onPlayTts = { viewModel.playAudioTranscript(msg.text) },
                            onOpenFullWallet = onOpenWallet
                        )
                    }
                    "CARD_ITINERARY_SNIPPET" -> {
                        ChatItinerarySnippetCard(
                            message = msg,
                            trip = currentTrip,
                            activities = activities,
                            onActivityClick = { activity ->
                                selectedActivityForSnippetDetail = activity
                            },
                            onPlayTts = { viewModel.playAudioTranscript(msg.text) },
                            onOpenFullItinerary = onOpenItinerary
                        )
                    }
                    "CARD_GROUP_MEDIA_CAROUSEL" -> {
                        ChatGroupMediaCarouselCard(
                            message = msg,
                            memories = memoriesList,
                            onAddPhotoClick = {
                                viewModel.addGroupMemory("You", "Stunning coastal vista with family", currentTrip?.destination ?: "Wailea Point")
                            },
                            onLikeClick = { memory ->
                                // Trigger positive like interaction
                            },
                            onPlayTts = { viewModel.playAudioTranscript(msg.text) },
                            onOpenFullMemories = onOpenMemories
                        )
                    }
                    "CARD_EMERGENCY_SOS" -> {
                        ChatEmergencySosCard(
                            message = msg,
                            trip = currentTrip,
                            onPlayTts = { viewModel.playAudioTranscript(msg.text) }
                        )
                    }
                    "CARD_PROACTIVE_DISRUPTION" -> {
                        ChatProactiveDisruptionCard(
                            message = msg,
                            trip = currentTrip,
                            onAutoRebook = {
                                viewModel.sendChatMessage("Auto-rebooking confirmed for verified accessible indoor alternative.")
                            },
                            onPlayTts = { viewModel.playAudioTranscript(msg.text) }
                        )
                    }
                    "CARD_JOURNEY_COMPLETED" -> {
                        ChatJourneyCompletedCard(
                            message = msg,
                            trip = currentTrip,
                            onSubmitRating = { pacing, lodging, dining, notes ->
                                currentTrip?.let {
                                    viewModel.submitPostTripRating(
                                        tripId = it.id,
                                        pacingRating = pacing,
                                        lodgingRating = lodging,
                                        diningRating = dining,
                                        feedbackNotes = notes
                                    )
                                }
                            },
                            onOpenStoryReel = onOpenMemories,
                            onPlayTts = { viewModel.playAudioTranscript(msg.text) }
                        )
                    }
                    "CARD_POW_WOW_BRIEF" -> {
                        val brief = TripBriefEntity(
                            tripId = currentTrip?.id ?: 0L,
                            summaryText = msg.text
                        )
                        ChatPowWowBriefCard(
                            brief = brief,
                            onAcceptBrief = {
                                viewModel.sendChatMessage("Brief accepted. Alignment locked for this expedition.")
                            }
                        )
                    }
                    "CARD_TENSION" -> {
                        val tension = BriefTension(
                            tensionId = "tension_${msg.id}",
                            topic = msg.text.substringBefore("\n").ifBlank { "Planning Tension" },
                            stakes = msg.text.substringAfter("\n", "")
                        )
                        ChatTensionCard(
                            tension = tension,
                            onAcceptResolution = {
                                viewModel.sendChatMessage("Adopted proposed resolution for: ${tension.topic}")
                            }
                        )
                    }
                    else -> {
                        // Standard AI Assistant text response
                        AiAssistantMessageBubble(
                            message = msg,
                            onPlayTts = { viewModel.playAudioTranscript(msg.text) }
                        )
                    }
                }
            }

            // Typing Indicator
            if (isTyping) {
                item {
                    TypingIndicatorBubble()
                }
            }
        }

        // QUICK-ACTION PROMPT CHIPS
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(suggestionChips) { chip ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clickable {
                            if (chip.contains("Settings", ignoreCase = true)) {
                                onOpenSettings()
                            } else {
                                viewModel.sendChatMessage(chip)
                            }
                        }
                        .testTag("suggestion_chip_${chip.take(8)}")
                ) {
                    Text(
                        text = chip,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // CHAT INPUT BAR (Unified Full-Width Papercraft Composer)
        Surface(
            color = LuxurySurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Surface(
                    color = LuxuryCard,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, LuxuryBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Multi-line Text Input Area (spans full width)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 36.dp, max = 130.dp)
                        ) {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = if (activeChatStreamTab == 1)
                                        "Message travel crew..."
                                    else
                                        "Message Marco or plan a trip...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextMuted
                                    ),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            androidx.compose.foundation.text.BasicTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextPrimary
                                ),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(ChampagneGold),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("chat_input_field")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Bottom Actions Row (Contained inside the text input box)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Bottom-Left Corner: '+' Quick Action & Mic Button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // '+' Quick Action Button
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(LuxuryCardElevated)
                                        .border(1.dp, LuxuryBorder, CircleShape)
                                        .clickable { isQuickActionSheetOpen = true }
                                        .testTag("open_quick_actions_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Quick Actions",
                                        tint = ChampagneGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Voice STT Mic Button
                                var isListeningVoice by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isListeningVoice) ChampagneGold.copy(alpha = 0.25f)
                                             else LuxuryCardElevated
                                        )
                                        .border(
                                            1.dp,
                                            if (isListeningVoice) ChampagneGold else LuxuryBorder,
                                            CircleShape
                                        )
                                        .clickable {
                                            isListeningVoice = !isListeningVoice
                                            if (isListeningVoice) {
                                                inputText = "Show pre-trip safety pack and emergency hospital coordinates"
                                            }
                                        }
                                        .testTag("voice_input_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isListeningVoice) Icons.Default.Mic else Icons.Default.MicOff,
                                        contentDescription = "Voice Input",
                                        tint = if (isListeningVoice) ChampagneGold else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Bottom-Right Corner: Send Button
                            val canSend = inputText.isNotBlank() && !isTyping
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (canSend) LuxuryCardElevated else LuxuryCard
                                    )
                                    .border(
                                        1.dp,
                                        if (canSend) ChampagneGold else LuxuryBorder,
                                        CircleShape
                                    )
                                    .clickable(enabled = canSend) {
                                        if (inputText.isNotBlank()) {
                                            val msg = inputText
                                            inputText = ""
                                            viewModel.sendChatMessage(msg)
                                        }
                                    }
                                    .testTag("send_message_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Message",
                                    tint = if (canSend) TextPrimary else TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Actions Modal Bottom Sheet
    if (isQuickActionSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { isQuickActionSheetOpen = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(16.dp))

                val actions = buildList {
                    add(Triple("Group Photo Reel", "Collaborative photo reel with AI scene tags & 1-tap likes", "Show group photo carousel"))
                    add(Triple("Structured Itinerary Snippets", "Quick-scan cards for flights, hotels, and vouchers", "Show itinerary snippets and cards"))
                    if (isTripActive) {
                        add(Triple("Emergency SOS Beacon", "Transmit live GPS telemetry to contacts and 911 dispatch", "Broadcast emergency SOS beacon"))
                    }
                    add(Triple("Budget & Currency Tracker", "Live balances, encrypted tokens, and zero-fee FX converter", "Check my budget, expenses, and currency conversion"))
                    add(Triple("Weekly Budget & Arbitrage Summary", "Spend vs loyalty savings comparison & category breakdown", "Show weekly budget summary and savings vs spend"))
                    add(Triple("Family & Accessibility Planner", "Dietary restrictions, wheelchair needs, and family age pacing", "Open family and accessibility planner studio"))
                    add(Triple("Plan New Itinerary", "Generate tailored 5-day trip with points & pacing", "Plan trip to Tokyo & Kyoto, Japan"))
                    add(Triple("Call Hotel Front Desk", "Simulate live phone inquiry for pool hours & check-in", "Call hotel front desk for heated pool hours & ADA access"))
                    add(Triple("Rewards & Timeshare Wallet", "View SkyMiles, Bonvoy, and RCI Trading Power", "Check my points and timeshare wallet"))
                    add(Triple("My Traveler DNA", "View learned preferences and rate past trips", "Show my traveler DNA and learned profile"))
                    if (isTripActive) {
                        add(Triple("Offline Safety & Live SOS", "Live GPS telemetry, hospital ER & emergency dispatch", "Show offline safety, hospital and emergency info"))
                    } else {
                        add(Triple("Pre-Trip Safety & Preparation", "Emergency directory, hospital & consular guide", "Show offline safety, hospital and emergency info"))
                    }
                    add(Triple("Cloud Account & Sign In", "Sign in to sync itineraries, wallet, and traveler DNA", "__OPEN_AUTH__"))
                    add(Triple("Vacation Memories", "View shared photos and vacation notes", "Show vacation memories and moments"))
                    add(Triple("Settings & Voice Concierge", "Switch narrator voice, BYOK API keys, and model parameters", "__OPEN_SETTINGS__"))
                }

                actions.forEach { (title, subtitle, prompt) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                isQuickActionSheetOpen = false
                                if (prompt == "__OPEN_SETTINGS__") {
                                    onOpenSettings()
                                } else if (prompt == "__OPEN_AUTH__") {
                                    onOpenAuth()
                                } else {
                                    viewModel.sendChatMessage(prompt)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            }
                            Icon(Icons.Default.Send, null, tint = WaypointCyan, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Expanded detail and editing sheet for tapped itinerary snippets
    selectedActivityForSnippetDetail?.let { activity ->
        ItinerarySnippetDetailSheet(
            activity = activity,
            onDismiss = { selectedActivityForSnippetDetail = null },
            onSave = { updated ->
                viewModel.updateTripActivity(updated)
                selectedActivityForSnippetDetail = null
            },
            onDelete = { id ->
                viewModel.deleteTripActivity(id)
                selectedActivityForSnippetDetail = null
            },
            onCallVendor = { vendor, question ->
                onOpenVendorCall(vendor, question)
            }
        )
    }
}

// -------------------------------------------------------------
// INLINE RICH CARD: ITINERARY
// -------------------------------------------------------------
@Composable
fun ChatItineraryCard(
    message: ChatMessageEntity,
    trip: TripEntity?,
    activities: List<TripActivityEntity>,
    onTriggerCall: () -> Unit,
    onToggleOffline: () -> Unit,
    onPlayTts: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    var selectedDay by remember { mutableIntStateOf(1) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, LuxuryBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(OceanBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FlightTakeoff, null, tint = OceanBlue, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = trip?.title ?: "Tailored Journey",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${trip?.destination ?: "Active Trip"} • ${trip?.travelStyle ?: "Points & Luxury"}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle",
                        tint = WaypointCyan
                    )
                }
            }

            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    // Day Selector Chips
                    val daysCount = if (activities.isNotEmpty()) activities.maxOf { it.dayNumber } else 3
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (d in 1..daysCount) {
                            val isDaySelected = selectedDay == d
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDaySelected) OceanBlue else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { selectedDay = d }
                            ) {
                                Text(
                                    text = "Day $d",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isDaySelected) CartographyDarkBase else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Activities for Selected Day
                    val dayActs = activities.filter { it.dayNumber == selectedDay }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        dayActs.forEach { act ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (act.category) {
                                            "FLIGHT" -> Icons.Default.AirplanemodeActive
                                            "HOTEL", "TIMESHARE" -> Icons.Default.Hotel
                                            "DINING" -> Icons.Default.Restaurant
                                            "TRANSIT" -> Icons.Default.DirectionsBus
                                            else -> Icons.Default.NaturePeople
                                        },
                                        contentDescription = null,
                                        tint = OceanBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${act.timeSlot} • ${act.title}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${act.location} • ${act.accessibilityBadge}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AmberGold.copy(alpha = 0.15f),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clickable { onTriggerCall() }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PhoneInTalk, null, tint = AmberGold, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call Hotel", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = AmberGold))
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldGreen.copy(alpha = 0.15f),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clickable { onToggleOffline() }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CloudDownload, null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync Offline", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldGreen))
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// INLINE RICH CARD: VOICE TELEPHONY CALL
// -------------------------------------------------------------
@Composable
fun ChatVoiceCallCard(
    message: ChatMessageEntity,
    callLog: VendorCallLogEntity?,
    onPlayAudio: () -> Unit
) {
    var isTranscriptOpen by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AmberGold.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, LuxuryBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AmberGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PhoneInTalk, null, tint = AmberGold, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "AI Voice Telephony Dispatch",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = AmberGold)
                        )
                        Text(
                            text = callLog?.vendorName ?: "Lodging Front Desk",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldGreen.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "Completed",
                        style = MaterialTheme.typography.labelSmall.copy(color = EmeraldGreen, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Confirmed Details Highlight Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = callLog?.callSummaryOutcome ?: "Pool open daily 7am-10pm with ADA chair lift verified.",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                    if (!callLog?.confirmedDetails.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Note: ${callLog?.confirmedDetails}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Row: Listen TTS + View Transcript
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isTranscriptOpen) "Hide Transcript" else "View Call Transcript",
                    style = MaterialTheme.typography.labelSmall.copy(color = WaypointCyan, fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable { isTranscriptOpen = !isTranscriptOpen }
                        .padding(vertical = 4.dp)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AmberGold,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable { onPlayAudio() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VolumeUp, null, tint = CartographyDarkBase, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play Audio", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = CartographyDarkBase))
                    }
                }
            }

            AnimatedVisibility(visible = isTranscriptOpen) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = callLog?.audioTranscript ?: "AI Agent: 'Inquiring regarding pool operating hours.'\nVendor: 'Pool open 7am to 10pm daily.'",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// INLINE RICH CARD: REWARDS & POINTS WALLET
// -------------------------------------------------------------
@Composable
fun ChatRewardsCard(
    message: ChatMessageEntity,
    accounts: List<ConnectedAccountEntity>,
    onRunOptimizer: () -> Unit,
    onPlayTts: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, LuxuryBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TealAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, null, tint = TealAccent, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        val totalValuation = accounts.sumOf { it.rewardsEstimatedValuationUsd }
                        Text(
                            text = "Rewards & Timeshare Portfolio",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (accounts.isNotEmpty()) "${accounts.size} Connected Accounts • $${totalValuation.toInt()} Est. Value" else "0 Connected Accounts • Link points & timeshares",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                IconButton(onClick = onPlayTts, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.VolumeUp, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Account Badges Grid
            if (accounts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    accounts.take(4).forEach { acc ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(acc.providerName, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(acc.accountNumberMasked, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${acc.balanceValue} ${acc.unitLabel}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = WaypointCyan
                                        )
                                    )
                                    Text(
                                        text = "≈ $${acc.rewardsEstimatedValuationUsd.toInt()}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = EmeraldGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No loyalty or timeshare accounts linked yet. Link your Delta, Marriott, Hilton, or RCI accounts to optimize points.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Point Optimization Button
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = OceanBlue,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { onRunOptimizer() }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = CartographyDarkBase, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Optimize Points & Timeshare Swaps", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = CartographyDarkBase))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// INLINE RICH CARD: TRAVELER DNA & PASSPORT
// -------------------------------------------------------------
@Composable
fun ChatTravelerDnaCard(
    message: ChatMessageEntity,
    preference: UserPreferenceEntity?,
    linkedProgramCount: Int = 0,
    onRefineDna: () -> Unit,
    onPlayTts: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TravelerPassportCard(
            preference = preference,
            linkedProgramCount = linkedProgramCount,
            onRefineDnaClick = onRefineDna
        )
    }
}

// -------------------------------------------------------------
// INLINE RICH CARD: OFFLINE SAFETY & EMERGENCY
// -------------------------------------------------------------
@Composable
fun ChatOfflineSafetyCard(
    message: ChatMessageEntity,
    trip: TripEntity?,
    onToggleOffline: () -> Unit,
    onPlayTts: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, LuxuryBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(WaxSealCrimson.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Shield, null, tint = WaxSealCrimson, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Emergency Safety & Medical Radar",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = WaxSealCrimson)
                        )
                        Text(
                            text = "${trip?.destination ?: "Active Destination"} • Monitored",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                IconButton(onClick = onPlayTts, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.VolumeUp, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Safety details
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Local Emergency SOS: Dial 911 (US) / 112 (EU) / 110 (JP)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Emergency Medical Care: 24/7 Local Trauma ER & Regional Citizen Consular Services",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Offline Sync Button
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = EmeraldGreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { onToggleOffline() }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CloudDownload, null, tint = CartographyDarkBase, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Toggle Offline Emergency Vector Pack", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = CartographyDarkBase))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// INLINE RICH CARD: VACATION MEMORIES
// -------------------------------------------------------------
@Composable
fun ChatGroupMemoryCard(
    message: ChatMessageEntity,
    memories: List<GroupMemoryEntity>,
    onAddMemory: (caption: String, tag: String) -> Unit
) {
    var captionText by remember { mutableStateOf("") }
    var isAdding by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, LuxuryBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(OceanBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Favorite, null, tint = OceanBlue, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Vacation Moments & Shared Log",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${memories.size} Moments Recorded",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Moments list
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                memories.take(3).forEach { mem ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(mem.photoGradientColor)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mem.caption, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text("${mem.authorName} • ${mem.locationTag}", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!isAdding) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = OceanBlue.copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { isAdding = true }
                ) {
                    Text(
                        text = "+ Add New Travel Memory",
                        style = MaterialTheme.typography.labelSmall.copy(color = WaypointCyan, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = captionText,
                        onValueChange = { captionText = it },
                        placeholder = { Text("Write a moment caption...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            if (captionText.isNotBlank()) {
                                onAddMemory(captionText, "Makena Beach")
                                captionText = ""
                                isAdding = false
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, null, tint = OceanBlue)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STANDARD MESSAGE BUBBLES
// -------------------------------------------------------------
@Composable
fun UserMessageBubble(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp),
            color = LuxuryCardElevated,
            border = BorderStroke(1.dp, LuxuryBorder),
            modifier = Modifier.padding(start = 48.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextPrimary
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
fun AiAssistantMessageBubble(
    message: ChatMessageEntity,
    onPlayTts: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Explore,
                contentDescription = null,
                tint = ChampagneGold,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Marco Concierge",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = ChampagneGold
                )
            )
        }

        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
            color = LuxuryCard,
            border = BorderStroke(1.dp, LuxuryBorder),
            modifier = Modifier.padding(end = 36.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onPlayTts,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Listen",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TypingIndicatorBubble() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        MarcoConciergeTypingIndicator()
    }
}

// -------------------------------------------------------------
// INLINE RICH CARD: FAMILY & ACCESSIBILITY PLANNING STUDIO
// -------------------------------------------------------------
@Composable
fun ChatFamilyAccessibilityCard(
    message: ChatMessageEntity,
    trip: TripEntity?,
    userPref: UserPreferenceEntity?,
    onSavePreferences: (dietary: String, wheelchair: String, familyAges: String, sensoryNotes: String, recalculate: Boolean) -> Unit,
    onVerifyVendorAda: (vendorName: String, category: String, topic: String) -> Unit,
    onPlayTts: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    var selectedDietary by remember {
        mutableStateOf(trip?.dietaryRestrictions?.ifBlank { null } ?: userPref?.dietaryPreferences?.ifBlank { null } ?: "")
    }
    var selectedWheelchair by remember {
        mutableStateOf(trip?.accessibilityRequirements?.ifBlank { null } ?: userPref?.wheelchairRequirements?.ifBlank { null } ?: "")
    }
    var selectedFamilyAges by remember {
        mutableStateOf(trip?.familyAgeBrackets?.ifBlank { null } ?: userPref?.familyAgeBrackets?.ifBlank { null } ?: "")
    }
    var customNotes by remember {
        mutableStateOf(userPref?.sensoryAndMobilityNotes ?: "")
    }
    var hasAppliedChanges by remember { mutableStateOf(false) }

    val dietaryOptions = listOf(
        "Gluten-Free (Celiac Safe)",
        "Peanut & Tree Nut-Free",
        "Dairy-Free / Lactose",
        "Halal Certified",
        "Kosher Certified",
        "Vegan & Vegetarian",
        "Kid Picky-Eater Menu"
    )

    val wheelchairOptions = listOf(
        "Step-Free Ramp Access",
        "ADA Hydraulic Pool Lift",
        "Roll-In Shower & 36\" Doors",
        "Low-Floor Transit Ramps",
        "Beach All-Terrain Chair"
    )

    val ageBracketOptions = listOf(
        "Toddler (0-3 yrs)",
        "Kids (4-11 yrs)",
        "Teens (12-17 yrs)",
        "Adults (18-59 yrs)",
        "Seniors (60+ yrs)"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("family_accessibility_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(OceanBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Accessible,
                            contentDescription = "Accessibility",
                            tint = OceanBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Family & Accessibility Studio",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        )
                        Text(
                            text = "Dietary, Wheelchair & Age Pacing for ${trip?.destination ?: "Active Trip"}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPlayTts, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Listen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle",
                            tint = OceanBlue
                        )
                    }
                }
            }

            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    // SECTION 1: DIETARY RESTRICTIONS & ALLERGENS
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Restaurant, null, tint = OceanBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Dietary Restrictions & Allergies",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = OceanBlue, fontSize = 12.sp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(dietaryOptions) { opt ->
                            val isSelected = selectedDietary.contains(opt, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) OceanBlue else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable {
                                        selectedDietary = if (isSelected) {
                                            selectedDietary.replace(opt, "").trim(' ', ',', '&')
                                        } else {
                                            if (selectedDietary.isBlank()) opt else "$selectedDietary, $opt"
                                        }
                                        hasAppliedChanges = false
                                    }
                                    .testTag("dietary_chip_${opt.take(6)}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = opt,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // SECTION 2: WHEELCHAIR & MOBILITY REQUIREMENTS
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Accessible, null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Wheelchair & Physical Accessibility",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldGreen, fontSize = 12.sp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(wheelchairOptions) { opt ->
                            val isSelected = selectedWheelchair.contains(opt, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) EmeraldGreen else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable {
                                        selectedWheelchair = if (isSelected) {
                                            selectedWheelchair.replace(opt, "").trim(' ', ',', '&')
                                        } else {
                                            if (selectedWheelchair.isBlank()) opt else "$selectedWheelchair & $opt"
                                        }
                                        hasAppliedChanges = false
                                    }
                                    .testTag("wheelchair_chip_${opt.take(6)}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = opt,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // SECTION 3: FAMILY AGE DYNAMICS & PACING
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ChildCare, null, tint = AmberGold, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Family Age Groups & Activity Pacing",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = AmberGold, fontSize = 12.sp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(ageBracketOptions) { opt ->
                            val isSelected = selectedFamilyAges.contains(opt, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) AmberGold else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable {
                                        selectedFamilyAges = if (isSelected) {
                                            selectedFamilyAges.replace(opt, "").trim(' ', ',', '&')
                                        } else {
                                            if (selectedFamilyAges.isBlank()) opt else "$selectedFamilyAges & $opt"
                                        }
                                        hasAppliedChanges = false
                                    }
                                    .testTag("age_chip_${opt.take(6)}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = opt,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // SECTION 4: SENSORY & SPECIAL MOBILITY NOTES
                    OutlinedTextField(
                        value = customNotes,
                        onValueChange = {
                            customNotes = it
                            hasAppliedChanges = false
                        },
                        label = { Text("Sensory & Custom Equipment Notes", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Needs low-sensory quiet hours, crib, stroller gate-check", fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sensory_notes_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OceanBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // ACTION BUTTON 1: RECALCULATE ITINERARY
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (hasAppliedChanges) EmeraldGreen else OceanBlue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSavePreferences(
                                    selectedDietary.ifBlank { "Gluten-Free Safe" },
                                    selectedWheelchair.ifBlank { "Step-Free Ramp & ADA Access" },
                                    selectedFamilyAges.ifBlank { "Family & Kids" },
                                    customNotes,
                                    true
                                )
                                hasAppliedChanges = true
                            }
                            .testTag("apply_accessibility_recalculate_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (hasAppliedChanges) Icons.Default.Check else Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hasAppliedChanges) "Applied! Itinerary Recalculated" else "Apply & Recalculate Live Itinerary",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ACTION BUTTON 2: DISPATCH AI VOICE CALL FOR HOTEL ADA & ALLERGEN CHECK
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AmberGold.copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onVerifyVendorAda(
                                    trip?.title ?: "Lodging Front Desk",
                                    "Resort & Culinary",
                                    "Verify step-free wheelchair ramp, hydraulic ADA pool chair lift, and celiac-safe dedicated gluten-free kitchen"
                                )
                            }
                            .testTag("ai_voice_ada_verify_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhoneInTalk, null, tint = AmberGold, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI Voice Call: Verify Hotel ADA & Kitchen Safety",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberGold
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// INLINE RICH CARD: AI ACTIVITY & EXCURSION SUGGESTIONS
// -------------------------------------------------------------
@Composable
fun ChatActivitySuggestionsCard(
    message: ChatMessageEntity,
    trip: TripEntity?,
    userPref: UserPreferenceEntity?,
    suggestions: List<com.example.ai.AiSuggestedActivityItem>,
    isGenerating: Boolean,
    onRefresh: () -> Unit,
    onAddActivity: (com.example.ai.AiSuggestedActivityItem, targetDay: Int) -> Unit,
    onVerifyVendor: (vendorName: String, topic: String) -> Unit,
    onPlayTts: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_activity_suggestions_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(LuxuryCardElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ChampagneGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Activity Suggestions",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Filtered for ${trip?.travelersCount ?: 3} travelers" +
                                (userPref?.wheelchairRequirements?.ifBlank { null }?.let { " • $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Row {
                    IconButton(onClick = onPlayTts, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.VolumeUp, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Tune, null, tint = OceanBlue, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = message.text,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isGenerating) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CaravelSailingLoadingIndicator(widthDp = 140, heightDp = 44)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Marco is navigating accessible family excursions...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = VenetianGold
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    suggestions.forEach { item ->
                        var isAdded by remember { mutableStateOf(false) }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        )
                                        Text(
                                            text = "${item.suggestedTimeSlot} • ${item.location} • ${item.durationHours} hrs",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = EmeraldGreen.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = "${item.matchScore}% Match",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = EmeraldGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Badges
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = OceanBlue.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = item.accessibilityBadge,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = OceanBlue,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = AmberGold.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = item.familySuitability,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = AmberGold,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "🥗 ${item.dietaryMatch}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = EmeraldGreen,
                                        fontWeight = FontWeight.Medium
                                    )
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = item.rationale,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Action Row: Add to Day 1/2 or Call Vendor
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isAdded) EmeraldGreen else OceanBlue,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                if (!isAdded) {
                                                    onAddActivity(item, 1)
                                                    isAdded = true
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 7.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isAdded) Icons.Default.Check else Icons.Default.Add,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isAdded) "Added to Day 1" else "+ Add to Itinerary ($${item.estimatedCost.toInt()})",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AmberGold.copy(alpha = 0.15f),
                                        modifier = Modifier.clickable {
                                            onVerifyVendor(
                                                item.bookingVendor,
                                                "Verify ramp access, stroller parking, and reservations for ${item.title}"
                                            )
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.PhoneInTalk, null, tint = AmberGold, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Call Vendor", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// INLINE RICH CARD: DYNAMIC ITINERARY ADJUSTMENT ENGINE
// -------------------------------------------------------------
@Composable
fun ChatDynamicAdjustmentCard(
    message: ChatMessageEntity,
    adjustment: com.example.ai.DynamicAdjustmentResult?,
    isEvaluating: Boolean,
    onApplyAdjustment: (com.example.ai.DynamicAdjustmentResult) -> Unit,
    onSimulateVoiceRebook: (vendorName: String, topic: String) -> Unit,
    onPlayTts: () -> Unit
) {
    var hasApplied by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_dynamic_adjustment_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(LuxuryCardElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = ChampagneGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Itinerary Adjustment Notice",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Weather alerts & disruption contingency",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                IconButton(onClick = onPlayTts, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.VolumeUp, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isEvaluating) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    MarcoAstrolabeLoadingAnimation(sizeDp = 64, accentColor = SunsetCoral)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Marco astrolabe evaluating weather radar & contingency routes...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SunsetCoral
                    )
                }
            } else if (adjustment != null) {
                // Trigger Reason Alert Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SunsetCoral.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Shield, null, tint = SunsetCoral, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = adjustment.triggerReason,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = SunsetCoral,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Adjustment Replacement Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "⚠️ Compromised:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), fontSize = 11.sp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = adjustment.impactedActivityTitle,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "✨ AI Alternative:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldGreen, fontSize = 11.sp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = adjustment.suggestedAlternativeTitle,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OceanBlue)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${adjustment.replacementTimeSlot} • ${adjustment.location}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = OceanBlue.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = adjustment.accessibilityBadge,
                                    style = MaterialTheme.typography.labelSmall.copy(color = OceanBlue, fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = adjustment.dietaryBadge,
                                    style = MaterialTheme.typography.labelSmall.copy(color = EmeraldGreen, fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = adjustment.summaryExplanation,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions: 1-Tap Rebook & AI Voice Telephony Dispatch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (hasApplied) EmeraldGreen else SunsetCoral,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (!hasApplied) {
                                    onApplyAdjustment(adjustment)
                                    hasApplied = true
                                }
                            }
                            .testTag("apply_dynamic_adjustment_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (hasApplied) Icons.Default.Check else Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (hasApplied) "Rebooked & Updated!" else "1-Tap Auto-Rebook (${if (adjustment.costDifference <= 0) "Save $${-adjustment.costDifference.toInt()}" else "+$${adjustment.costDifference.toInt()}"})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AmberGold.copy(alpha = 0.15f),
                        modifier = Modifier.clickable {
                            onSimulateVoiceRebook(
                                adjustment.suggestedAlternativeTitle,
                                "Rebook reservation due to weather alert and ensure wheelchair ramp confirmation"
                            )
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhoneInTalk, null, tint = AmberGold, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Voice Dispatch", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatProactiveDisruptionCard(
    message: ChatMessageEntity,
    trip: TripEntity?,
    onAutoRebook: () -> Unit,
    onPlayTts: () -> Unit
) {
    var isRebooked by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LuxuryCard),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, WaxSealCrimson),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(WaxSealCrimson.copy(alpha = 0.15f))
                            .border(1.dp, WaxSealCrimson, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = WaxSealCrimson, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "DISRUPTION ALERT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = WaxSealCrimson,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "Weather Advisory & Alternative",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                }
                IconButton(onClick = onPlayTts) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = ChampagneGold, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = LuxuryDarkBase,
                border = androidx.compose.foundation.BorderStroke(1.dp, LuxuryBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "⚡ Thunderstorm & Wave Advisory Impacting Outdoor Activities",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TerracottaStamp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "✨ Verified Alternative: Indoor Living Reef Discovery Center & Cultural Gallery\n♿ 100% Step-Free & Stroller Ramp Access • 🥗 Allergen-Safe Kitchen Verified",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!isRebooked) {
                Button(
                    onClick = {
                        isRebooked = true
                        onAutoRebook()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChampagneGold,
                        contentColor = LuxuryDarkBase
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rebook Alternative", fontWeight = FontWeight.Bold)
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = WayfinderEmerald.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WayfinderEmerald),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = WayfinderEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Itinerary Successfully Rebooked & Synced!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = WayfinderEmerald
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatJourneyCompletedCard(
    message: ChatMessageEntity,
    trip: TripEntity?,
    onSubmitRating: (pacing: Int, lodging: Int, dining: Int, notes: String) -> Unit,
    onOpenStoryReel: () -> Unit,
    onPlayTts: () -> Unit
) {
    var pacingScore by remember { mutableIntStateOf(5) }
    var lodgingScore by remember { mutableIntStateOf(5) }
    var diningScore by remember { mutableIntStateOf(5) }
    var feedbackNotes by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LuxuryCard),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, ChampagneGold),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ChampagneGold.copy(alpha = 0.2f))
                            .border(1.dp, ChampagneGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🏁", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "JOURNEY COMPLETED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = ChampagneGold,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "${trip?.destination ?: "Trip"} Summary",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                }
                IconButton(onClick = onPlayTts) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = ChampagneGold, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action: View Story Reel
            OutlinedButton(
                onClick = onOpenStoryReel,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = WaypointCyan),
                border = androidx.compose.foundation.BorderStroke(1.dp, WaypointCyan),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("🎬 View Story Reel & Photos", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = LuxuryBorder)
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Rate Your Experience",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = ChampagneGold
                )
            )
            Text(
                text = "Your feedback helps personalize future trip recommendations.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Question 1: Daily Pacing
            RatingRowSelector(
                label = "1. Daily Pacing & Cadence",
                score = pacingScore,
                onScoreChange = { pacingScore = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Question 2: Lodging & Villa Quality
            RatingRowSelector(
                label = "2. Lodging & Accessibility",
                score = lodgingScore,
                onScoreChange = { lodgingScore = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Question 3: Dining & Culinary Safety
            RatingRowSelector(
                label = "3. Dining & Allergen Safety",
                score = diningScore,
                onScoreChange = { diningScore = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = feedbackNotes,
                onValueChange = { feedbackNotes = it },
                placeholder = { Text("What did you love or what should be adjusted next time?", fontSize = 12.sp, color = TextMuted) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ChampagneGold,
                    unfocusedBorderColor = LuxuryBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!isSubmitted) {
                Button(
                    onClick = {
                        isSubmitted = true
                        onSubmitRating(pacingScore, lodgingScore, diningScore, feedbackNotes)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChampagneGold,
                        contentColor = LuxuryDarkBase
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Feedback", fontWeight = FontWeight.Bold)
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = WayfinderEmerald.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WayfinderEmerald),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = WayfinderEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Feedback Saved!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = WayfinderEmerald
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingRowSelector(
    label: String,
    score: Int,
    onScoreChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary))
        Row {
            for (i in 1..5) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "$i Stars",
                    tint = if (i <= score) ChampagneGold else LuxuryBorder,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onScoreChange(i) }
                        .padding(2.dp)
                )
            }
        }
    }
}

