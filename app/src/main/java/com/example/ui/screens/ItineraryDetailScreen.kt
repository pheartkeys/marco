package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.model.TripActivityEntity
import com.example.data.model.TripEntity
import com.example.data.model.isTripInProgress
import com.example.ui.components.AccessibilityTagChip
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.HeroGradientBanner
import com.example.ui.theme.*
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
                                .minimumInteractiveComponentSize()
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
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = currentTrip?.title ?: "Trip Details & Pacing",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenPlanDialog,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaritimeBlue.copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Plan New Trip", tint = MaritimeBlue)
                    }
                }
            }

            // Sync Banner Notification if active
            syncMessage?.let { msg ->
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = WayfinderEmerald.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudDone, null, tint = WayfinderEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(msg, color = WayfinderEmerald, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
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
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, ContourBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPreferenceDialogOpen = true }
                        .testTag("open_preference_dna_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CartographyCardElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = NavigationalGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Traveler Profile & AI Preferences",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    )
                                    userPref?.let { pref ->
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "• ${pref.totalTripsAnalyzed} Insights",
                                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }
                                Text(
                                    text = userPref?.learnedInsightsSummary?.take(65)?.let { "$it..." } ?: "Learns from your ratings and preferred airline & hotel types.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    maxLines = 1
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = NavigationalGold, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Preferences",
                                style = MaterialTheme.typography.labelSmall.copy(color = NavigationalGold, fontWeight = FontWeight.Bold)
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
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Live Sync",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = WayfinderEmerald,
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
                                tint = NavigationalGold,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
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
        ExtendedFloatingActionButton(
            onClick = onOpenPlanDialog,
            containerColor = LuxuryCardElevated,
            contentColor = TextPrimary,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .border(1.dp, LuxuryBorder, RoundedCornerShape(12.dp))
                .testTag("plan_new_trip_fab"),
            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Plan Trip", tint = ChampagneGold) },
            text = { Text("AI Trip Planner", fontWeight = FontWeight.Medium) }
        )
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, ContourBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Flat Top Band
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(CartographyCardElevated)
                    .padding(16.dp)
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
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable { onMenuToggle() }
                                    .minimumInteractiveComponentSize()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.SwapHoriz, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Switch Trip",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                    )
                                    Icon(Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(14.dp))
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
                                                Text(t.title, style = MaterialTheme.typography.bodyMedium)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (t.isTripInProgress()) "Live" else "Plan",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = if (t.isTripInProgress()) WayfinderEmerald else WaypointCyan,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        },
                                        onClick = { onSelectTrip(t.id) }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isTripActive) "Set to Planning Mode (Hide SOS)" else "Start Trip / On-Trip Mode (Activate SOS)",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isTripActive) WaypointCyan else WayfinderEmerald
                                            )
                                        )
                                    },
                                    onClick = {
                                        onToggleTripStatus()
                                        onMenuToggle()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Plan New AI Trip...", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = WaypointCyan)) },
                                    onClick = {
                                        onMenuToggle()
                                        onNewTripClick()
                                    }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Trip Status Badge Pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isTripActive) WayfinderEmerald.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable { onToggleTripStatus() }
                                    .minimumInteractiveComponentSize()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isTripActive) "On-trip" else "Planning",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            // Offline sync status icon
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (trip.isOfflineSynced) WayfinderEmerald.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable { onToggleOfflineSync() }
                                    .minimumInteractiveComponentSize()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (trip.isOfflineSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = if (trip.isOfflineSynced) WayfinderEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (trip.isOfflineSynced) "Synced" else "Syncing...",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = trip.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${trip.startDate} - ${trip.endDate} • ${trip.destination}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            color = TerracottaStamp.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${trip.childrenCount} Child Travelers",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = TerracottaStamp
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
                            color = if (progress > 0.85f) SunsetCoral else WaypointCyan,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = if (progress > 0.85f) SunsetCoral else MaritimeBlue,
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
                label = { Text("All Days (${activities.size})", fontWeight = FontWeight.Medium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedDay == 0,
                    borderColor = MaterialTheme.colorScheme.outline,
                    selectedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        items((1..totalDays).toList()) { d ->
            val dayCount = activities.count { it.dayNumber == d }
            FilterChip(
                selected = selectedDay == d,
                onClick = { onSelectDay(d) },
                label = { Text("Day $d ($dayCount)", fontWeight = FontWeight.Medium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedDay == d,
                    borderColor = MaterialTheme.colorScheme.outline,
                    selectedBorderColor = MaterialTheme.colorScheme.primary
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Category Badge, Time, Day, Checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIconBadge(category = activity.category, size = 32)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Day ${activity.dayNumber} • ${activity.timeSlot}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            text = activity.category,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleCompleted,
                        modifier = Modifier.minimumInteractiveComponentSize().size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (activity.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Toggle Complete",
                            tint = if (activity.isCompleted) WayfinderEmerald else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = activity.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
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
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = activity.location,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // Notes
            if (activity.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = activity.notes,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(8.dp)
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // AI Voice Call trigger
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .clickable { onVoiceCall() }
                            .minimumInteractiveComponentSize()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Call, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Call Vendor",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    // Delete
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.minimumInteractiveComponentSize().size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(15.dp)
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.AutoAwesome, null, tint = MaritimeBlue, modifier = Modifier.size(44.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Welcome to Marco",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Plan itineraries, track budgets, and call vendors in one place.",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onOpenPlanDialog,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaritimeBlue,
                    contentColor = CartographyDarkBase
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Plan First Trip", fontWeight = FontWeight.Bold)
            }
        }
    }
}
