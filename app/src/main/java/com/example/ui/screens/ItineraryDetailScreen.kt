package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TripActivityEntity
import com.example.data.model.TripEntity
import com.example.data.model.isTripInProgress
import com.example.ui.components.AccessibilityTagChip
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.HeroGradientBanner
import com.example.ui.theme.AmberGold
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.Navy900
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.SkyBlueLight
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TealAccent
import com.example.viewmodel.TravelViewModel

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star

@Composable
fun ItineraryDetailScreen(
    viewModel: TravelViewModel,
    onOpenPlanDialog: () -> Unit,
    onNavigateToVendorCall: (vendorName: String, question: String) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    BackHandler(onBack = onNavigateBack)
    val trips by viewModel.allTrips.collectAsState()
    val selectedTripId by viewModel.selectedTripId.collectAsState()
    val selectedDayFilter by viewModel.selectedDayFilter.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val syncMessage by viewModel.offlineSyncBannerMessage.collectAsState()
    val userPref by viewModel.userPreference.collectAsState()
    val feedbacks by viewModel.tripFeedbacks.collectAsState()

    val currentTrip = trips.find { it.id == selectedTripId } ?: trips.firstOrNull()
    var isTripMenuExpanded by remember { mutableStateOf(false) }
    var isPreferenceDialogOpen by remember { mutableStateOf(false) }

    val filteredActivities = if (selectedDayFilter == 0) {
        activities
    } else {
        activities.filter { it.dayNumber == selectedDayFilter }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Top Navigation & Back Button Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("itinerary_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Chat",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Expedition Itinerary & Bookings",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                            )
                            Text(
                                text = currentTrip?.title ?: "Trip Details & Pacing",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenPlanDialog,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(OceanBlue.copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Plan New Trip", tint = OceanBlue)
                    }
                }
            }

            // Sync Banner Notification if active
            syncMessage?.let { msg ->
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldGreen.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudDone, null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(msg, color = EmeraldGreen, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                        }
                    }
                }
            }

            // Trip Header / Hero Card
            item {
                if (currentTrip != null) {
                    TripHeroCard(
                        trip = currentTrip,
                        tripsList = trips,
                        isMenuExpanded = isTripMenuExpanded,
                        onMenuToggle = { isTripMenuExpanded = !isTripMenuExpanded },
                        onSelectTrip = {
                            viewModel.selectTrip(it)
                            isTripMenuExpanded = false
                        },
                        onToggleTripStatus = { viewModel.toggleTripStatus(currentTrip.id) },
                        onToggleOfflineSync = { viewModel.toggleOfflineSync() },
                        onNewTripClick = onOpenPlanDialog
                    )
                } else {
                    EmptyTripCard(onOpenPlanDialog = onOpenPlanDialog)
                }
            }

            // AI Learned DNA & Feedback Bar
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPreferenceDialogOpen = true }
                        .testTag("open_preference_dna_card")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = Color(0xFF8B5CF6),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "AI Traveler DNA",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = EmeraldGreen.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "${userPref?.totalTripsAnalyzed ?: 2} Insights Learned",
                                            style = MaterialTheme.typography.labelSmall.copy(color = EmeraldGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = userPref?.learnedInsightsSummary?.take(65)?.let { "$it..." } ?: "Learns from your ratings and preferred airline & hotel types.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    maxLines = 1
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = AmberGold, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Train AI",
                                style = MaterialTheme.typography.labelSmall.copy(color = OceanBlue, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Day Selector Row
            item {
                DayFilterSection(
                    activities = activities,
                    selectedDay = selectedDayFilter,
                    onSelectDay = { viewModel.setDayFilter(it) }
                )
            }

            // Activities Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedDayFilter == 0) "All Scheduled Activities (${activities.size})" else "Day $selectedDayFilter Schedule",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    )
                    Text(
                        text = "Real-time sync active",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TealAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            // Activity List
            if (filteredActivities.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = OceanBlue,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No activities for this day",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ask your 24/7 AI Concierge to suggest activities or tap '+' to plan a custom trip.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            } else {
                items(filteredActivities, key = { it.id }) { activity ->
                    ActivityItemCard(
                        activity = activity,
                        onToggleCompleted = { viewModel.toggleActivityCompleted(activity) },
                        onDelete = { viewModel.deleteActivity(activity.id) },
                        onVoiceCall = {
                            onNavigateToVendorCall(
                                activity.vendorName.ifBlank { activity.title },
                                "Inquire about reservations, operating hours, and accessibility for ${activity.title}"
                            )
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onOpenPlanDialog,
            containerColor = OceanBlue,
            contentColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("plan_new_trip_fab")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Plan Trip", tint = AmberGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Trip Planner", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (isPreferenceDialogOpen) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { isPreferenceDialogOpen = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            TravelerPreferenceDialog(
                viewModel = viewModel,
                onDismiss = { isPreferenceDialogOpen = false },
                onSelectProactiveTrip = {
                    isPreferenceDialogOpen = false
                    onOpenPlanDialog()
                }
            )
        }
    }
}

@Composable
fun TripHeroCard(
    trip: TripEntity,
    tripsList: List<TripEntity>,
    isMenuExpanded: Boolean,
    onMenuToggle: () -> Unit,
    onSelectTrip: (Long) -> Unit,
    onToggleTripStatus: () -> Unit = {},
    onToggleOfflineSync: () -> Unit,
    onNewTripClick: () -> Unit
) {
    val progress = (trip.budgetSpent / trip.budgetTotal).coerceIn(0.0, 1.0).toFloat()
    val isTripActive = trip.isTripInProgress()

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Gradient Top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(Brush.horizontalGradient(listOf(Navy900, OceanBlue, TealAccent)))
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Switcher button
                        Box {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.clickable { onMenuToggle() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.SwapHoriz, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Switch Trip",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                                    )
                                    Icon(Icons.Default.ExpandMore, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }

                            DropdownMenu(
                                expanded = isMenuExpanded,
                                onDismissRequest = onMenuToggle
                            ) {
                                tripsList.forEach { t ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(t.title, fontSize = 13.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (t.isTripInProgress()) "🧭 Live" else "📋 Plan",
                                                    fontSize = 10.sp,
                                                    color = if (t.isTripInProgress()) EmeraldGreen else OceanBlue,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        },
                                        onClick = { onSelectTrip(t.id) }
                                    )
                                }
                                Divider()
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isTripActive) "📋 Set to Planning Mode (Hide SOS)" else "🧭 Start Trip / On-Trip Mode (Activate SOS)",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isTripActive) OceanBlue else EmeraldGreen,
                                            fontSize = 12.sp
                                        )
                                    },
                                    onClick = {
                                        onToggleTripStatus()
                                        onMenuToggle()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("✨ Plan New AI Trip...", fontWeight = FontWeight.Bold, color = OceanBlue, fontSize = 12.sp) },
                                    onClick = {
                                        onMenuToggle()
                                        onNewTripClick()
                                    }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Trip Status Badge Pill
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isTripActive) EmeraldGreen.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.clickable { onToggleTripStatus() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isTripActive) "🧭 ON-TRIP" else "📋 PLANNING",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }

                            // Offline sync status icon
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (trip.isOfflineSynced) EmeraldGreen.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.clickable { onToggleOfflineSync() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (trip.isOfflineSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = if (trip.isOfflineSynced) EmeraldGreen else Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (trip.isOfflineSynced) "Synced" else "Syncing...",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = trip.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${trip.startDate} - ${trip.endDate} • ${trip.destination}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp
                        )
                    )
                }
            }

            // Card Bottom Details
            Column(modifier = Modifier.padding(16.dp)) {
                // Family & Accessibility pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AccessibilityTagChip(text = trip.accessibilityRequirements)
                    if (trip.childrenCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEC4899).copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${trip.childrenCount} Child Travelers",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFEC4899)
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Budget Tracker Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Budget: $${trip.budgetSpent.toInt()} / $${trip.budgetTotal.toInt()} ${trip.primaryCurrency}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}% Used",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (progress > 0.85f) SunsetCoral else OceanBlue,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = if (progress > 0.85f) SunsetCoral else OceanBlue,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
fun DayFilterSection(
    activities: List<TripActivityEntity>,
    selectedDay: Int,
    onSelectDay: (Int) -> Unit
) {
    val totalDays = activities.maxOfOrNull { it.dayNumber } ?: 1

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedDay == 0,
                onClick = { onSelectDay(0) },
                label = { Text("All Days (${activities.size})", fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OceanBlue.copy(alpha = 0.2f),
                    selectedLabelColor = OceanBlue
                )
            )
        }

        items((1..totalDays).toList()) { d ->
            val dayCount = activities.count { it.dayNumber == d }
            FilterChip(
                selected = selectedDay == d,
                onClick = { onSelectDay(d) },
                label = { Text("Day $d ($dayCount)", fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OceanBlue.copy(alpha = 0.2f),
                    selectedLabelColor = OceanBlue
                )
            )
        }
    }
}

@Composable
fun ActivityItemCard(
    activity: TripActivityEntity,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit,
    onVoiceCall: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Category Badge, Time, Day, Checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIconBadge(category = activity.category, size = 36)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Day ${activity.dayNumber} • ${activity.timeSlot}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = OceanBlue,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = activity.category,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleCompleted,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (activity.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Toggle Complete",
                            tint = if (activity.isCompleted) EmeraldGreen else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = activity.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (activity.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
            )

            // Location
            if (activity.location.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = activity.location,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            // Notes
            if (activity.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = activity.notes,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Badges: Accessibility & AI Voice Call
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AccessibilityTagChip(text = activity.accessibilityBadge)

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // AI Voice Call trigger
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AmberGold.copy(alpha = 0.15f),
                        modifier = Modifier.clickable { onVoiceCall() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Call, null, tint = AmberGold, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "AI Call Vendor",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AmberGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Delete
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyTripCard(onOpenPlanDialog: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.AutoAwesome, null, tint = OceanBlue, modifier = Modifier.size(44.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Welcome to VoyageAI Travel Agent",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Replace traditional agency hassles with smart AI multi-modal itinerary planning, rewards optimization, and live vendor voice calls.",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onOpenPlanDialog,
                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Plan First Trip", fontWeight = FontWeight.Bold)
            }
        }
    }
}
