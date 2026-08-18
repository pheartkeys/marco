package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cabin
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import com.example.data.model.ProactiveSuggestionEntity
import com.example.ui.components.ExplorerVoyageLoadingCard
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CompassLilac
import com.example.ui.theme.LuxuryBorder
import com.example.ui.theme.LuxuryDarkBase
import com.example.ui.theme.MaritimeBlue
import com.example.ui.theme.SilkRoadTeal
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.WayfinderEmerald
import com.example.ui.theme.WaypointCyan
import com.example.viewmodel.TravelViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanTripDialog(
    viewModel: TravelViewModel,
    initialSuggestion: ProactiveSuggestionEntity? = null,
    onDismiss: () -> Unit,
    onTripCreated: (Long) -> Unit
) {
    val isGenerating by viewModel.isGeneratingTrip.collectAsState()
    val userPref by viewModel.userPreference.collectAsState()
    val proactiveSuggestions by viewModel.proactiveSuggestions.collectAsState()

    var isPreferenceStudioOpen by remember { mutableStateOf(false) }

    var destination by remember {
        mutableStateOf(initialSuggestion?.destination ?: "Kyoto & Tokyo, Japan")
    }
    var durationDays by remember {
        mutableIntStateOf(initialSuggestion?.durationDays ?: 7)
    }
    var budget by remember {
        mutableDoubleStateOf(initialSuggestion?.estimatedBudget ?: 4500.0)
    }
    var selectedCurrency by remember {
        mutableStateOf(initialSuggestion?.currency ?: "USD")
    }
    var adultsCount by remember { mutableIntStateOf(2) }
    var childrenCount by remember { mutableIntStateOf(1) }

    val accessibilityOptions = remember {
        mutableStateListOf(
            "Stroller Friendly",
            "Wheelchair & Step-Free Access",
            "Elevator Priority",
            "Sensory Quiet Areas"
        )
    }
    val selectedAccessibility = remember { mutableStateListOf<String>() }

    val integrationOptions = listOf(
        "Major Airlines Miles",
        "Hotel Points",
        "Timeshare Programs",
        "Credit Card Rewards",
        "Camping / Recreation",
        "Bullet Trains / Transit"
    )
    val selectedIntegrations = remember {
        mutableStateListOf<String>()
    }

    var travelStyle by remember {
        mutableStateOf(initialSuggestion?.title ?: "")
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, LuxuryBorder),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("plan_trip_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Plan New Expedition",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Personalized Itinerary & Logistics",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_plan_dialog")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Traveler DNA Active Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isPreferenceStudioOpen = true }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Psychology, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = userPref?.let { "Traveler Profile (${it.totalTripsAnalyzed} Trips Analyzed)" } ?: "Traveler Profile",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            val dnaSummary = listOfNotNull(
                                userPref?.preferredAirlines?.takeIf { it.isNotBlank() },
                                userPref?.activityLevel?.takeIf { it.isNotBlank() },
                                userPref?.dietaryPreferences?.takeIf { it.isNotBlank() }
                            ).joinToString(" • ").ifBlank { "Configure your preferences & travel style" }
                            Text(
                                text = dnaSummary,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    Text(
                        text = "Edit DNA",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = WaypointCyan,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Proactive Suggestions Quick Carousel
            if (proactiveSuggestions.isNotEmpty()) {
                Text(
                    text = "AI Proactive Proposals (based on your feedback)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(proactiveSuggestions) { sug ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                destination = sug.destination
                                durationDays = sug.durationDays
                                budget = sug.estimatedBudget
                                selectedCurrency = sug.currency
                                travelStyle = sug.title
                            }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${sug.matchScorePercent}% Match",
                                        style = MaterialTheme.typography.labelSmall.copy(color = WayfinderEmerald, fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Save $${sug.pointsSavingsUsd.toInt()}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = ChampagneGold, fontWeight = FontWeight.Bold)
                                    )
                                }
                                Text(
                                    text = sug.destination,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Destination
            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text("Destination & Region") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = MaritimeBlue)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("destination_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Duration & Budget Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Duration: $durationDays Days",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Slider(
                        value = durationDays.toFloat(),
                        onValueChange = { durationDays = it.toInt() },
                        valueRange = 2f..21f,
                        steps = 19,
                        modifier = Modifier.testTag("duration_slider")
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Budget: $${budget.toInt()} $selectedCurrency",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Slider(
                        value = budget.toFloat(),
                        onValueChange = { budget = it.toDouble() },
                        valueRange = 1000f..15000f,
                        steps = 27,
                        modifier = Modifier.testTag("budget_slider")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Travelers & Children
            Text(
                text = "Travelers & Family Members",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Adults: $adultsCount", fontWeight = FontWeight.Medium)
                        Row {
                            IconButton(onClick = { if (adultsCount > 1) adultsCount-- }) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaritimeBlue.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) { Text("-", fontWeight = FontWeight.Bold) }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { if (adultsCount < 10) adultsCount++ }) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaritimeBlue.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) { Text("+", fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ChildCare, null, modifier = Modifier.size(16.dp), tint = SunsetCoral)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kids: $childrenCount", fontWeight = FontWeight.Medium)
                        }
                        Row {
                            IconButton(onClick = { if (childrenCount > 0) childrenCount-- }) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaritimeBlue.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) { Text("-", fontWeight = FontWeight.Bold) }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { if (childrenCount < 8) childrenCount++ }) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaritimeBlue.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) { Text("+", fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Family & Accessibility Logistics
            Text(
                text = "Family Logistics & Accessibility Needs",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                accessibilityOptions.forEach { opt ->
                    val selected = selectedAccessibility.contains(opt)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (selected) selectedAccessibility.remove(opt) else selectedAccessibility.add(opt)
                        },
                        label = { Text(opt, style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (opt.contains("Stroller") || opt.contains("Sensory")) Icons.Default.ChildCare else Icons.Default.Accessible,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SilkRoadTeal.copy(alpha = 0.2f),
                            selectedLabelColor = SilkRoadTeal
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // APIs & Rewards Programs to Optimize
            Text(
                text = "Integrate Loyalty, Timeshares & Campground APIs",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                integrationOptions.forEach { opt ->
                    val selected = selectedIntegrations.contains(opt)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (selected) selectedIntegrations.remove(opt) else selectedIntegrations.add(opt)
                        },
                        label = { Text(opt, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ChampagneGold.copy(alpha = 0.2f),
                            selectedLabelColor = ChampagneGold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isGenerating) {
                ExplorerVoyageLoadingCard(
                    statusMessage = "Marco is charting $destination...",
                    subStatus = "Synthesizing $durationDays-day expedition coordinates, points & accessibility"
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Submit Button with AI Thinking State
            Button(
                onClick = {
                    val accessibilityStr = selectedAccessibility.joinToString(", ")
                    val rewardsStr = selectedIntegrations.joinToString(", ")
                    viewModel.createAITrip(
                        destination = destination,
                        durationDays = durationDays,
                        budget = budget,
                        currency = selectedCurrency,
                        adults = adultsCount,
                        children = childrenCount,
                        accessibilityNeeds = accessibilityStr,
                        travelStyle = travelStyle,
                        rewardsStrategy = rewardsStr,
                        onComplete = { tripId ->
                            onTripCreated(tripId)
                        }
                    )
                },
                enabled = !isGenerating && destination.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChampagneGold,
                    contentColor = LuxuryDarkBase
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("generate_itinerary_button")
            ) {
                if (isGenerating) {
                    Text(
                        text = "Marco Expedition Engine Active...",
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generate Tailored Trip Itinerary",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (isPreferenceStudioOpen) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { isPreferenceStudioOpen = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            TravelerPreferenceDialog(
                viewModel = viewModel,
                onDismiss = { isPreferenceStudioOpen = false },
                onSelectProactiveTrip = { sug ->
                    destination = sug.destination
                    durationDays = sug.durationDays
                    budget = sug.estimatedBudget
                    selectedCurrency = sug.currency
                    travelStyle = sug.title
                    isPreferenceStudioOpen = false
                }
            )
        }
    }
}

@Composable
fun PlanTripScreen(
    viewModel: TravelViewModel,
    modifier: Modifier = Modifier,
    initialSuggestion: ProactiveSuggestionEntity? = null,
    onNavigateBack: () -> Unit = {},
    onTripCreated: (Long) -> Unit = {}
) {
    BackHandler(onBack = onNavigateBack)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PlanTripDialog(
            viewModel = viewModel,
            initialSuggestion = initialSuggestion,
            onDismiss = onNavigateBack,
            onTripCreated = onTripCreated
        )
    }
}

