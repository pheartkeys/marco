package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BriefTension
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ConnectedAccountEntity
import com.example.data.model.GroupMemoryEntity
import com.example.data.model.TripActivityEntity
import com.example.data.model.TripBriefEntity
import com.example.data.model.TripEntity
import com.example.data.model.UserPreferenceEntity
import com.example.data.model.VendorCallLogEntity
import com.example.data.model.isTripInProgress
import com.example.ui.components.*
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
    var selectedActivityForSnippetDetail by remember { mutableStateOf<TripActivityEntity?>(null) }
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

    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LuxuryDarkBase)
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
            color = LuxurySurface,
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
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        LuxuryCardElevated,
                                        LuxuryCard
                                    )
                                )
                            )
                            .border(1.dp, LuxuryBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = "Marco Expedition Concierge",
                            tint = ChampagneGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Marco",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            ExpeditionBadge(
                                text = "Expeditions",
                                color = ChampagneGold
                            )
                        }
                    }
                }

                // Actions: Trip Context Pill + SOS + Settings + Clear Chat
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Active Trip Dropdown Pill
                    Box {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LuxuryCard,
                            border = BorderStroke(1.dp, LuxuryBorder),
                            modifier = Modifier
                                .clickable { isTripMenuExpanded = true }
                                .testTag("trip_context_selector")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val destLabel = currentTrip?.destination?.split(",")?.firstOrNull()?.trim() ?: "Plan Trip"
                                Text(
                                    text = destLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = ChampagneGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = "Switch Trip",
                                    tint = ChampagneGold,
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
                                        Text("Plan New Voyage...", fontWeight = FontWeight.Bold, color = ChampagneGold)
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
                                                    Text(trip.title, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    ExpeditionBadge(
                                                        text = if (active) "On-trip" else "Planning",
                                                        color = if (active) StatusEmerald else ChampagneGold
                                                    )
                                                }
                                                Text(
                                                    "${trip.destination} • ${trip.startDate}",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
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
                                                text = if (trip.isTripInProgress()) "Switch to Planning Mode" else "Start Vacation (Activate SOS)",
                                                fontWeight = FontWeight.Bold,
                                                color = if (trip.isTripInProgress()) ChampagneGold else StatusEmerald
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
                                        Text("Plan Another Trip...", fontWeight = FontWeight.Medium, color = ChampagneGold)
                                    },
                                    onClick = {
                                        isTripMenuExpanded = false
                                        onOpenPlanTrip()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Emergency SOS Beacon Button
                    if (isTripActive) {
                        IconButton(
                            onClick = { viewModel.triggerEmergencySos() },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(StatusCrimson.copy(alpha = 0.2f))
                                .border(1.dp, StatusCrimson, CircleShape)
                                .testTag("sos_beacon_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = "Emergency SOS",
                                tint = StatusCrimson,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
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
                            .size(34.dp)
                            .testTag("top_bar_account_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = if (firebaseUser != null) "Account: ${firebaseUser?.displayName ?: firebaseUser?.email}" else "Sign In",
                            tint = if (firebaseUser != null) ChampagneGold else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Settings Button
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("open_settings_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings & Voices",
                            tint = TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Reset / Clear Chat Button
                    IconButton(
                        onClick = { viewModel.clearChat() },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Chat",
                            tint = TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }

        // ACTIVE ITINERARY HORIZONTAL PAGER (Swipeable between multiple expeditions)
        if (tripsList.isNotEmpty()) {
            Surface(
                color = LuxurySurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    HorizontalPager(
                        state = tripPagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        pageSpacing = 8.dp
                    ) { page ->
                        val trip = tripsList.getOrNull(page)
                        if (trip != null) {
                            val isSelected = trip.id == selectedTripId
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) LuxuryCardElevated else LuxuryCard
                                ),
                                border = BorderStroke(1.dp, if (isSelected) ChampagneGold.copy(alpha = 0.5f) else LuxuryBorder),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        viewModel.selectTrip(trip.id)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(LuxurySurface)
                                                .border(1.dp, LuxuryBorder, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FlightTakeoff,
                                                contentDescription = null,
                                                tint = ChampagneGold,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = trip.title,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary
                                                    ),
                                                    maxLines = 1
                                                )
                                                if (trip.isTripInProgress()) {
                                                    ExpeditionBadge(
                                                        text = "Active",
                                                        color = StatusEmerald,
                                                        isFilled = true
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${trip.destination} • ${trip.startDate} - ${trip.endDate}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = TextSecondary,
                                                    fontSize = 10.5.sp
                                                ),
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
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
                                        .padding(horizontal = 3.dp)
                                        .size(if (isCurrent) 14.dp else 5.dp, 4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (isCurrent) ChampagneGold else LuxuryBorder)
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
                color = StatusEmerald.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, StatusEmerald.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = offlineMsg ?: "",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = StatusEmerald,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // DUAL CHAT STREAM TABS (Marco vs Travel Crew)
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
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
                label = { Text("Marco Concierge") }
            )
            SegmentedButton(
                selected = activeChatStreamTab == 1,
                onClick = { viewModel.setActiveChatStreamTab(1) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Groups,
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
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = LuxuryCard),
                        border = BorderStroke(1.dp, LuxuryBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(LuxuryCardElevated)
                                        .border(1.dp, LuxuryBorder, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (activeChatStreamTab == 1) Icons.Default.Groups else Icons.Default.Explore,
                                        contentDescription = null,
                                        tint = ChampagneGold,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (activeChatStreamTab == 1) "Travel Crew Expedition Hub" else "Marco Expedition Concierge",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = if (activeChatStreamTab == 1) "Collaborate with friends & family" else "Named after history's most famous globetrotter",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (activeChatStreamTab == 1)
                                    "Plan adventures together, align on itineraries, vote on excursions, and share live vacation moments with your loved ones.\n\nTap '+' below to share a trip photo or review party alignments."
                                else
                                    "Dream up unforgettable vacations, optimize loyalty points, find hidden cultural gems, and coordinate seamless travel with your loved ones.\n\nType your dream destination or tap a prompt chip below to begin charting your course.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary,
                                    lineHeight = 20.sp
                                )
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
                                // Positive like reaction
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

        // CHAT INPUT BAR (Unified Floating Capsule Composer)
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
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, LuxuryBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        // Multi-line Text Input Area
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 38.dp, max = 130.dp)
                        ) {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = if (activeChatStreamTab == 1)
                                        "Message travel crew..."
                                    else
                                        "Ask Marco anything or plan a trip... 🌴",
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
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(MarcoCoral),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("chat_input_field")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Bottom Actions Row
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
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(VoyagerSkyPastel)
                                        .border(1.dp, VoyagerSky.copy(alpha = 0.35f), CircleShape)
                                        .clickable { isQuickActionSheetOpen = true }
                                        .testTag("open_quick_actions_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Quick Actions",
                                        tint = VoyagerSky,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Voice STT Mic Button
                                var isListeningVoice by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isListeningVoice) GoldenSunPastel
                                            else LightCardElevated
                                        )
                                        .border(
                                            1.dp,
                                            if (isListeningVoice) GoldenSun else LightBorder,
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
                                        tint = if (isListeningVoice) GoldenSunDark else TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Bottom-Right Corner: Send Button
                            val canSend = inputText.isNotBlank() && !isTyping
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (canSend) MarcoCoral else LightCardElevated
                                    )
                                    .border(
                                        1.dp,
                                        if (canSend) MarcoCoralLight else LightBorder,
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
                                    tint = if (canSend) Color.White else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Actions Modal Bottom Sheet (2-Column Large Icon Tiles Grid)
    if (isQuickActionSheetOpen) {
        data class QuickActionTileItem(
            val title: String,
            val subtitle: String,
            val prompt: String,
            val icon: androidx.compose.ui.graphics.vector.ImageVector,
            val iconColor: Color,
            val containerColor: Color
        )

        val quickActionTiles = remember(isTripActive) {
            buildList {
                add(
                    QuickActionTileItem(
                        title = "Itinerary",
                        subtitle = "Flights, stays & stops",
                        prompt = "Show itinerary snippets and cards",
                        icon = Icons.AutoMirrored.Filled.EventNote,
                        iconColor = VoyagerSky,
                        containerColor = VoyagerSkyPastel
                    )
                )
                add(
                    QuickActionTileItem(
                        title = "Plan New Trip",
                        subtitle = "Bespoke 5-day route",
                        prompt = "Plan trip to Tokyo & Kyoto, Japan",
                        icon = Icons.Default.FlightTakeoff,
                        iconColor = MarcoCoral,
                        containerColor = MarcoCoralPastel
                    )
                )
                add(
                    QuickActionTileItem(
                        title = "Group Memories",
                        subtitle = "Photo reel & moments",
                        prompt = "Show group photo carousel",
                        icon = Icons.Default.Collections,
                        iconColor = BerryOrchid,
                        containerColor = BerryOrchidPastel
                    )
                )
                add(
                    QuickActionTileItem(
                        title = "Budget & FX",
                        subtitle = "Spend & live exchange",
                        prompt = "Check my budget, expenses, and currency conversion",
                        icon = Icons.Default.AccountBalanceWallet,
                        iconColor = PalmEmerald,
                        containerColor = PalmEmeraldPastel
                    )
                )
                add(
                    QuickActionTileItem(
                        title = "Savings Summary",
                        subtitle = "Points vs cash savings",
                        prompt = "Show weekly budget summary and savings vs spend",
                        icon = Icons.Default.Savings,
                        iconColor = GoldenSunDark,
                        containerColor = GoldenSunPastel
                    )
                )
                add(
                    QuickActionTileItem(
                        title = "Accessibility",
                        subtitle = "Dietary, wheelchair, ages",
                        prompt = "Open family and accessibility planner studio",
                        icon = Icons.AutoMirrored.Filled.Accessible,
                        iconColor = LagoonTeal,
                        containerColor = LagoonTealPastel
                    )
                )
                add(
                    QuickActionTileItem(
                        title = "Call Hotel Desk",
                        subtitle = "AI voice inquiry",
                        prompt = "Call hotel front desk for heated pool hours & ADA access",
                        icon = Icons.Default.PhoneInTalk,
                        iconColor = VoyagerSky,
                        containerColor = VoyagerSkyPastel
                    )
                )
                add(
                    QuickActionTileItem(
                        title = "Rewards Wallet",
                        subtitle = "SkyMiles & Bonvoy",
                        prompt = "Check my points and timeshare wallet",
                        icon = Icons.Default.CardMembership,
                        iconColor = GoldenSunDark,
                        containerColor = GoldenSunPastel
                    )
                )
                add(
                    QuickActionTileItem(
                        title = "Traveler DNA",
                        subtitle = "Your explorer profile",
                        prompt = "Show my traveler DNA and learned profile",
                        icon = Icons.Default.Fingerprint,
                        iconColor = BerryOrchid,
                        containerColor = BerryOrchidPastel
                    )
                )
                add(
                    QuickActionTileItem(
                        title = if (isTripActive) "Emergency SOS" else "Safety & Offline",
                        subtitle = if (isTripActive) "Broadcast live GPS" else "Offline medical guide",
                        prompt = if (isTripActive) "Broadcast emergency SOS beacon" else "Show offline safety, hospital and emergency info",
                        icon = Icons.Default.HealthAndSafety,
                        iconColor = StatusCrimson,
                        containerColor = StatusCrimsonMuted
                    )
                )
                add(
                    QuickActionTileItem(
                        title = "Cloud Account",
                        subtitle = "Sync trips & wallet",
                        prompt = "__OPEN_AUTH__",
                        icon = Icons.Default.CloudSync,
                        iconColor = VoyagerSky,
                        containerColor = VoyagerSkyPastel
                    )
                )
                add(
                    QuickActionTileItem(
                        title = "Settings",
                        subtitle = "Voice & AI models",
                        prompt = "__OPEN_SETTINGS__",
                        icon = Icons.Default.Settings,
                        iconColor = TextSecondary,
                        containerColor = LightCardElevated
                    )
                )
            }
        }

        ModalBottomSheet(
            onDismissRequest = { isQuickActionSheetOpen = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = LightSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Travel Tools & Actions",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 20.sp
                            )
                        )
                        Text(
                            text = "Select an action to explore or plan",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MarcoCoralPastel)
                            .clickable { isQuickActionSheetOpen = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MarcoCoral,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2-Column Grid of Large Icon Tiles
                val pairs = quickActionTiles.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    pairs.forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pair.forEach { tile ->
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = LightCard,
                                    border = BorderStroke(1.dp, LightBorder),
                                    shadowElevation = 1.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(130.dp)
                                        .clickable {
                                            isQuickActionSheetOpen = false
                                            if (tile.prompt == "__OPEN_SETTINGS__") {
                                                onOpenSettings()
                                            } else if (tile.prompt == "__OPEN_AUTH__") {
                                                onOpenAuth()
                                            } else {
                                                viewModel.sendChatMessage(tile.prompt)
                                            }
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(tile.containerColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = tile.icon,
                                                contentDescription = tile.title,
                                                tint = tile.iconColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = tile.title,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    fontSize = 14.sp
                                                ),
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = tile.subtitle,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = TextSecondary,
                                                    fontSize = 11.sp,
                                                    lineHeight = 14.sp
                                                ),
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LuxuryCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, LuxuryBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                            .background(LuxuryCardElevated)
                            .border(1.dp, LuxuryBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FlightTakeoff, null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = trip?.title ?: "Tailored Journey",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "${trip?.destination ?: "Active Trip"} • ${trip?.travelStyle ?: "Points & Luxury"}",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPlayTts, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Listen",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle",
                            tint = ChampagneGold
                        )
                    }
                }
            }

            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
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
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDaySelected) ChampagneGold else LuxurySurface,
                                border = BorderStroke(1.dp, if (isDaySelected) ChampagneGold else LuxuryBorder),
                                modifier = Modifier.clickable { selectedDay = d }
                            ) {
                                Text(
                                    text = "Day $d",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isDaySelected) LuxuryDarkBase else TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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
                                shape = RoundedCornerShape(14.dp),
                                color = LuxurySurface,
                                border = BorderStroke(1.dp, LuxuryBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CategoryIconBadge(category = act.category, size = 34)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${act.timeSlot} • ${act.title}",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        )
                                        Text(
                                            text = "${act.location} • ${act.accessibilityBadge}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = LuxurySurface,
                            border = BorderStroke(1.dp, LuxuryBorder),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .clickable { onTriggerCall() }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PhoneInTalk, null, tint = ChampagneGold, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Call Hotel",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ChampagneGold
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = LuxurySurface,
                            border = BorderStroke(1.dp, LuxuryBorder),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .clickable { onToggleOffline() }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CloudDownload, null, tint = StatusEmerald, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Sync Offline",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = StatusEmerald
                                    )
                                )
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LuxuryCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, LuxuryBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                            .background(LuxuryCardElevated)
                            .border(1.dp, LuxuryBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PhoneInTalk, null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        ExpeditionBadge(text = "AI Voice Dispatch", color = ChampagneGold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = callLog?.vendorName ?: "Lodging Front Desk",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StatusEmerald.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, StatusEmerald.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Completed",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = StatusEmerald,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Confirmed Details Highlight Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = LuxurySurface,
                border = BorderStroke(1.dp, LuxuryBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, null, tint = StatusEmerald, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = callLog?.callSummaryOutcome ?: "Pool open daily 7am-10pm with ADA chair lift verified.",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        )
                    }
                    if (!callLog?.confirmedDetails.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Note: ${callLog?.confirmedDetails}",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row: Listen TTS + View Transcript
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isTranscriptOpen) "Hide Transcript" else "View Call Transcript",
                    style = MaterialTheme.typography.labelSmall.copy(color = ChampagneGold, fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .clickable { isTranscriptOpen = !isTranscriptOpen }
                        .padding(vertical = 4.dp)
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ChampagneGold,
                    modifier = Modifier
                        .clickable { onPlayAudio() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = LuxuryDarkBase,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Play Audio",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = LuxuryDarkBase
                            )
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isTranscriptOpen) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LuxurySurface,
                        border = BorderStroke(1.dp, LuxuryBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = callLog?.audioTranscript ?: "AI Agent: 'Inquiring regarding pool operating hours.'\nVendor: 'Pool open 7am to 10pm daily.'",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                            modifier = Modifier.padding(10.dp)
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LuxuryCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, LuxuryBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                            .background(LuxuryCardElevated)
                            .border(1.dp, LuxuryBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        val totalValuation = accounts.sumOf { it.rewardsEstimatedValuationUsd }
                        Text(
                            text = "Rewards & Loyalty Portfolio",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = if (accounts.isNotEmpty()) "${accounts.size} Connected Accounts • $${totalValuation.toInt()} Est. Value" else "0 Connected Accounts • Link points & timeshares",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }

                IconButton(onClick = onPlayTts, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Listen",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Account Badges Grid
            if (accounts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    accounts.take(4).forEach { acc ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = LuxurySurface,
                            border = BorderStroke(1.dp, LuxuryBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = acc.providerName,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = acc.accountNumberMasked,
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${acc.balanceValue} ${acc.unitLabel}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = ChampagneGold
                                        )
                                    )
                                    Text(
                                        text = "≈ $${acc.rewardsEstimatedValuationUsd.toInt()}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = StatusEmerald,
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
                    color = LuxurySurface,
                    border = BorderStroke(1.dp, LuxuryBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No loyalty or timeshare accounts linked yet. Link your Delta, Marriott, Hilton, or RCI accounts to optimize points.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Point Optimization Button
            Button(
                onClick = onRunOptimizer,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChampagneGold,
                    contentColor = LuxuryDarkBase
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Optimize Points & Timeshare Swaps", fontWeight = FontWeight.Bold)
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LuxuryCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, LuxuryBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                            .background(LuxuryCardElevated)
                            .border(1.dp, LuxuryBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Shield, null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Safety & Emergency Radar",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "${trip?.destination ?: "Active Destination"} • Offline Packet",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }

                IconButton(onClick = onPlayTts, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Listen",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Safety details
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = LuxurySurface,
                border = BorderStroke(1.dp, LuxuryBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Local Emergency SOS: Dial 911 (US) / 112 (EU) / 110 (JP)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Emergency Medical Care: 24/7 Local Trauma ER & Regional Citizen Consular Services",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Offline Sync Button
            Button(
                onClick = onToggleOffline,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusEmerald,
                    contentColor = LuxuryDarkBase
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Toggle Offline Emergency Vector Pack", fontWeight = FontWeight.Bold)
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LuxuryCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, LuxuryBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                            .background(LuxuryCardElevated)
                            .border(1.dp, LuxuryBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Collections, null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Vacation Moments & Memories",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "${memories.size} Moments Recorded",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Moments list
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                memories.take(3).forEach { mem ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = LuxurySurface,
                        border = BorderStroke(1.dp, LuxuryBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(LuxuryCardElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PhotoCamera, null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mem.caption,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "${mem.authorName} • ${mem.locationTag}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!isAdding) {
                Button(
                    onClick = { isAdding = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LuxuryCardElevated,
                        contentColor = TextPrimary
                    ),
                    border = BorderStroke(1.dp, LuxuryBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Add New Travel Memory", fontWeight = FontWeight.Bold)
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
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (captionText.isNotBlank()) {
                                onAddMemory(captionText, "Wailea Beach")
                                captionText = ""
                                isAdding = false
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = ChampagneGold)
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
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp),
            color = MarcoCoral,
            modifier = Modifier.padding(start = 48.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.5.sp,
                    lineHeight = 21.sp
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
                tint = MarcoCoral,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Marco Concierge",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MarcoCoral
                )
            )
        }

        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp),
            color = LuxuryCard,
            border = BorderStroke(1.dp, LuxuryBorder),
            modifier = Modifier.padding(end = 36.dp)
        ) {
            Column(modifier = Modifier.padding(15.dp)) {
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
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LuxuryCard),
        border = BorderStroke(1.dp, LuxuryBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("family_accessibility_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                            .background(LuxuryCardElevated)
                            .border(1.dp, LuxuryBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Accessible,
                            contentDescription = "Accessibility",
                            tint = ChampagneGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Family & Accessibility Studio",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Dietary, Wheelchair & Age Pacing for ${trip?.destination ?: "Active Trip"}",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPlayTts, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Listen",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle",
                            tint = ChampagneGold
                        )
                    }
                }
            }

            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    // SECTION 1: DIETARY RESTRICTIONS & ALLERGENS
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Restaurant, null, tint = ChampagneGold, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Dietary Restrictions & Allergies",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ChampagneGold
                            )
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
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) ChampagneGold else LuxurySurface,
                                border = BorderStroke(1.dp, if (isSelected) ChampagneGold else LuxuryBorder),
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
                                        Icon(Icons.Default.Check, null, tint = LuxuryDarkBase, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = opt,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelected) LuxuryDarkBase else TextSecondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // SECTION 2: WHEELCHAIR & MOBILITY REQUIREMENTS
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Accessible, null, tint = StatusEmerald, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Wheelchair & Physical Accessibility",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = StatusEmerald
                            )
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
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) StatusEmerald else LuxurySurface,
                                border = BorderStroke(1.dp, if (isSelected) StatusEmerald else LuxuryBorder),
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
                                        Icon(Icons.Default.Check, null, tint = LuxuryDarkBase, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = opt,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelected) LuxuryDarkBase else TextSecondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // SECTION 3: FAMILY AGE DYNAMICS & PACING
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ChildCare, null, tint = ChampagneGold, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Family Age Groups & Activity Pacing",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ChampagneGold
                            )
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
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) ChampagneGold else LuxurySurface,
                                border = BorderStroke(1.dp, if (isSelected) ChampagneGold else LuxuryBorder),
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
                                        Icon(Icons.Default.Check, null, tint = LuxuryDarkBase, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = opt,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelected) LuxuryDarkBase else TextSecondary,
                                            fontWeight = FontWeight.SemiBold
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
                            focusedBorderColor = ChampagneGold,
                            unfocusedBorderColor = LuxuryBorder
                        ),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // ACTION BUTTON 1: RECALCULATE ITINERARY
                    Button(
                        onClick = {
                            onSavePreferences(
                                selectedDietary.ifBlank { "Gluten-Free Safe" },
                                selectedWheelchair.ifBlank { "Step-Free Ramp & ADA Access" },
                                selectedFamilyAges.ifBlank { "Family & Kids" },
                                customNotes,
                                true
                            )
                            hasAppliedChanges = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasAppliedChanges) StatusEmerald else ChampagneGold,
                            contentColor = LuxuryDarkBase
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apply_accessibility_recalculate_button")
                    ) {
                        Icon(
                            imageVector = if (hasAppliedChanges) Icons.Default.Check else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasAppliedChanges) "Applied! Itinerary Recalculated" else "Apply & Recalculate Live Itinerary",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ACTION BUTTON 2: DISPATCH AI VOICE CALL FOR HOTEL ADA & ALLERGEN CHECK
                    Button(
                        onClick = {
                            onVerifyVendorAda(
                                trip?.title ?: "Lodging Front Desk",
                                "Resort & Culinary",
                                "Verify step-free wheelchair ramp, hydraulic ADA pool chair lift, and celiac-safe dedicated gluten-free kitchen"
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LuxuryCardElevated,
                            contentColor = TextPrimary
                        ),
                        border = BorderStroke(1.dp, LuxuryBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_voice_ada_verify_button")
                    ) {
                        Icon(Icons.Default.PhoneInTalk, null, tint = ChampagneGold, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI Voice Call: Verify Hotel ADA & Kitchen Safety",
                            fontWeight = FontWeight.Bold
                        )
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
        colors = CardDefaults.cardColors(containerColor = LuxuryCard),
        border = BorderStroke(1.dp, LuxuryBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_activity_suggestions_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                            .background(LuxuryCardElevated)
                            .border(1.dp, LuxuryBorder, CircleShape),
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
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Filtered for ${trip?.travelersCount ?: 3} travelers" +
                                (userPref?.wheelchairRequirements?.ifBlank { null }?.let { " • $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPlayTts, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Listen",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Tune, null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = message.text,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                color = TextPrimary
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
                        color = ChampagneGold
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    suggestions.forEach { item ->
                        var isAdded by remember { mutableStateOf(false) }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = LuxurySurface,
                            border = BorderStroke(1.dp, LuxuryBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
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
                                                color = TextPrimary
                                            )
                                        )
                                        Text(
                                            text = "${item.suggestedTimeSlot} • ${item.location} • ${item.durationHours} hrs",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                        )
                                    }

                                    ExpeditionBadge(
                                        text = "${item.matchScore}% Match",
                                        color = StatusEmerald,
                                        isFilled = true
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Badges
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ExpeditionBadge(
                                        text = item.accessibilityBadge,
                                        color = ChampagneGold
                                    )
                                    ExpeditionBadge(
                                        text = item.familySuitability,
                                        color = ChampagneGold
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "🥗 ${item.dietaryMatch}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = StatusEmerald,
                                        fontWeight = FontWeight.Medium
                                    )
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = item.rationale,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        lineHeight = 16.sp,
                                        color = TextSecondary
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Action Row: Add to Day 1/2 or Call Vendor
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            if (!isAdded) {
                                                onAddActivity(item, 1)
                                                isAdded = true
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isAdded) StatusEmerald else ChampagneGold,
                                            contentColor = LuxuryDarkBase
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (isAdded) Icons.Default.Check else Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isAdded) "Added to Day 1" else "+ Add to Itinerary ($${item.estimatedCost.toInt()})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            onVerifyVendor(
                                                item.bookingVendor,
                                                "Verify ramp access, stroller parking, and reservations for ${item.title}"
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = LuxuryCardElevated,
                                            contentColor = TextPrimary
                                        ),
                                        border = BorderStroke(1.dp, LuxuryBorder)
                                    ) {
                                        Icon(Icons.Default.PhoneInTalk, null, tint = ChampagneGold, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Call Vendor", fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
        colors = CardDefaults.cardColors(containerColor = LuxuryCard),
        border = BorderStroke(1.dp, LuxuryBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_dynamic_adjustment_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                            .background(LuxuryCardElevated)
                            .border(1.dp, LuxuryBorder, CircleShape),
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
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Weather alerts & disruption contingency",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }

                IconButton(onClick = onPlayTts, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Listen",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
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
                    MarcoAstrolabeLoadingAnimation(sizeDp = 64, accentColor = ChampagneGold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Marco astrolabe evaluating weather radar & contingency routes...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = ChampagneGold
                    )
                }
            } else if (adjustment != null) {
                // Trigger Reason Alert Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StatusCrimson.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, StatusCrimson.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Shield, null, tint = StatusCrimson, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = adjustment.triggerReason,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = StatusCrimson
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Adjustment Replacement Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = LuxurySurface,
                    border = BorderStroke(1.dp, LuxuryBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "⚠️ Compromised:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StatusCrimson
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = adjustment.impactedActivityTitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "✨ AI Alternative:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StatusEmerald
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = adjustment.suggestedAlternativeTitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ChampagneGold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${adjustment.replacementTimeSlot} • ${adjustment.location}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ExpeditionBadge(
                                text = adjustment.accessibilityBadge,
                                color = ChampagneGold
                            )
                            ExpeditionBadge(
                                text = adjustment.dietaryBadge,
                                color = StatusEmerald
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = adjustment.summaryExplanation,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions: 1-Tap Rebook & AI Voice Telephony Dispatch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (!hasApplied) {
                                onApplyAdjustment(adjustment)
                                hasApplied = true
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasApplied) StatusEmerald else ChampagneGold,
                            contentColor = LuxuryDarkBase
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("apply_dynamic_adjustment_button")
                    ) {
                        Icon(
                            imageVector = if (hasApplied) Icons.Default.Check else Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (hasApplied) "Rebooked & Updated!" else "1-Tap Auto-Rebook (${if (adjustment.costDifference <= 0) "Save $${-adjustment.costDifference.toInt()}" else "+$${adjustment.costDifference.toInt()}"})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = {
                            onSimulateVoiceRebook(
                                adjustment.suggestedAlternativeTitle,
                                "Rebook reservation due to weather alert and ensure wheelchair ramp confirmation"
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LuxuryCardElevated,
                            contentColor = TextPrimary
                        ),
                        border = BorderStroke(1.dp, LuxuryBorder)
                    ) {
                        Icon(Icons.Default.PhoneInTalk, null, tint = ChampagneGold, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Voice Dispatch", fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LuxuryCard),
        border = BorderStroke(1.dp, StatusCrimson.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                            .background(StatusCrimson.copy(alpha = 0.15f))
                            .border(1.dp, StatusCrimson, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = StatusCrimson, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        ExpeditionBadge(text = "Disruption Alert", color = StatusCrimson)
                        Spacer(modifier = Modifier.height(2.dp))
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
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Listen",
                        tint = ChampagneGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = LuxurySurface,
                border = BorderStroke(1.dp, LuxuryBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "⚡ Thunderstorm & Wave Advisory Impacting Outdoor Activities",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFB923C)
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "✨ Verified Alternative: Indoor Living Reef Discovery Center & Cultural Gallery\n♿ 100% Step-Free & Stroller Ramp Access • 🥗 Allergen-Safe Kitchen Verified",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, lineHeight = 17.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rebook Alternative", fontWeight = FontWeight.Bold)
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StatusEmerald.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, StatusEmerald),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = StatusEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Itinerary Successfully Rebooked & Synced!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = StatusEmerald
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LuxuryCard),
        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                            .background(ChampagneGold.copy(alpha = 0.2f))
                            .border(1.dp, ChampagneGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Flag, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        ExpeditionBadge(text = "Journey Completed", color = ChampagneGold)
                        Spacer(modifier = Modifier.height(2.dp))
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
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Listen",
                        tint = ChampagneGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action: View Story Reel
            OutlinedButton(
                onClick = onOpenStoryReel,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ChampagneGold),
                border = BorderStroke(1.dp, ChampagneGold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Collections, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("🎬 View Story Reel & Moments", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = LuxuryBorder)
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Rate Your Expedition",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = ChampagneGold
                )
            )
            Text(
                text = "Your feedback fine-tunes future traveler DNA & pacing preferences.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )

            Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.height(12.dp))

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
                shape = RoundedCornerShape(10.dp),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(14.dp))

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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Feedback", fontWeight = FontWeight.Bold)
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StatusEmerald.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, StatusEmerald),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = StatusEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Feedback Saved to Traveler DNA!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = StatusEmerald
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
