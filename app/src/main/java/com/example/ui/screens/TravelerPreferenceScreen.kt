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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProactiveSuggestionEntity
import com.example.data.model.TripEntity
import com.example.data.model.TripFeedbackEntity
import com.example.data.model.UserPreferenceEntity
import com.example.ui.theme.AmberGold
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.Navy900
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TealAccent
import com.example.viewmodel.TravelViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TravelerPreferenceDialog(
    viewModel: TravelViewModel,
    onDismiss: () -> Unit,
    onSelectProactiveTrip: (ProactiveSuggestionEntity) -> Unit
) {
    val preference by viewModel.userPreference.collectAsState()
    val feedbacks by viewModel.tripFeedbacks.collectAsState()
    val proactiveSuggestions by viewModel.proactiveSuggestions.collectAsState()
    val trips by viewModel.allTrips.collectAsState()
    val isAnalyzing by viewModel.isAnalyzingPreferences.collectAsState()
    val notification by viewModel.preferenceNotification.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0 = DNA & Insights, 1 = Stated Preferences, 2 = Trip Feedback & Ratings, 3 = Proactive Suggestions

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .testTag("traveler_preference_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
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
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF8B5CF6), OceanBlue))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AI Traveler DNA & Preference Learning",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "Analyzes past trips, feedback & loyalty synergies",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_preference_dialog")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Notification Banner
            AnimatedVisibility(visible = notification != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldGreen.copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = notification ?: "",
                            style = MaterialTheme.typography.labelSmall.copy(color = EmeraldGreen, fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tabs = listOf("Learned DNA", "Preferences", "Trip Feedback (${feedbacks.size})", "AI Matches (${proactiveSuggestions.size})")
                tabs.forEachIndexed { index, tabName ->
                    FilterChip(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        label = { Text(tabName, fontSize = 11.sp, fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OceanBlue.copy(alpha = 0.2f),
                            selectedLabelColor = OceanBlue
                        ),
                        modifier = Modifier.testTag("pref_tab_$index")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable Content
            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    0 -> LearnedDnaSection(
                        preference = preference,
                        feedbacksCount = feedbacks.size,
                        isAnalyzing = isAnalyzing,
                        onReanalyze = { viewModel.reanalyzeTravelerPreferences() }
                    )
                    1 -> StatedPreferencesSection(
                        preference = preference ?: UserPreferenceEntity(),
                        onSavePreferences = { updated -> viewModel.updateUserPreferences(updated) }
                    )
                    2 -> FeedbackAndRatingsSection(
                        feedbacks = feedbacks,
                        trips = trips,
                        onSubmitFeedback = { tId, tTitle, dest, rating, liked, disliked, notes ->
                            viewModel.submitTripFeedback(tId, tTitle, dest, rating, liked, disliked, notes)
                        },
                        onDeleteFeedback = { id -> viewModel.deleteTripFeedback(id) }
                    )
                    3 -> ProactiveSuggestionsSection(
                        suggestions = proactiveSuggestions,
                        onSelectSuggestion = onSelectProactiveTrip
                    )
                }
            }
        }
    }
}

@Composable
fun LearnedDnaSection(
    preference: UserPreferenceEntity?,
    feedbacksCount: Int,
    isAnalyzing: Boolean,
    onReanalyze: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // AI Synthesis Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = AmberGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Traveler DNA Synthesis",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = OceanBlue.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${preference?.totalTripsAnalyzed ?: 0} Data Points Learned",
                            style = MaterialTheme.typography.labelSmall.copy(color = OceanBlue, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = preference?.learnedInsightsSummary ?: "Set up your preferences or plan your first journey to build your Traveler DNA profile.",
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp, fontSize = 13.sp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onReanalyze,
                    enabled = !isAnalyzing,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reanalyze_preferences_button")
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Re-analyzing Trips with Gemini...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    } else {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Re-Synthesize Behavioral DNA", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Behavioral Affinity Matrix
        Text(
            text = "Learned Affinity Matrix",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        val affinities = if (preference != null && preference.totalTripsAnalyzed > 0) {
            val list = mutableListOf<AffinityItem>()
            if (preference.pacingPreference.isNotBlank()) {
                list.add(AffinityItem(preference.pacingPreference, 95, EmeraldGreen, "Learned pacing profile"))
            }
            if (preference.preferredHotelTypes.isNotBlank()) {
                list.add(AffinityItem(preference.preferredHotelTypes, 92, OceanBlue, "Preferred accommodation style"))
            }
            if (preference.preferredAirlines.isNotBlank()) {
                list.add(AffinityItem(preference.preferredAirlines, 90, AmberGold, "Target airline loyalty programs"))
            }
            if (preference.dietaryPreferences.isNotBlank()) {
                list.add(AffinityItem(preference.dietaryPreferences, 88, Color(0xFFEC4899), "Dietary & allergen safety parameters"))
            }
            list
        } else {
            emptyList()
        }

        if (affinities.isNotEmpty()) {
            affinities.forEach { item ->
                AffinityScoreRow(item)
                Spacer(modifier = Modifier.height(8.dp))
            }
        } else {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No behavioral affinity data yet. Complete trips or rate past journeys to build your Traveler DNA.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

data class AffinityItem(
    val label: String,
    val scorePercent: Int,
    val color: Color,
    val reason: String
)

@Composable
fun AffinityScoreRow(item: AffinityItem) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = item.label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    text = "${item.scorePercent}% Match",
                    style = MaterialTheme.typography.labelSmall.copy(color = item.color, fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            androidx.compose.material3.LinearProgressIndicator(
                progress = { item.scorePercent / 100f },
                color = item.color,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.reason,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatedPreferencesSection(
    preference: UserPreferenceEntity,
    onSavePreferences: (UserPreferenceEntity) -> Unit
) {
    val scrollState = rememberScrollState()

    var airlines by remember(preference) { mutableStateOf(preference.preferredAirlines) }
    var lodgingTypes by remember(preference) { mutableStateOf(preference.preferredHotelTypes) }
    var activityLevel by remember(preference) { mutableStateOf(preference.activityLevel) }
    var travelStyle by remember(preference) { mutableStateOf(preference.preferredTravelStyle) }
    var dietary by remember(preference) { mutableStateOf(preference.dietaryPreferences) }
    var pacing by remember(preference) { mutableStateOf(preference.pacingPreference) }
    var transit by remember(preference) { mutableStateOf(preference.preferredTransit) }

    val activityLevels = listOf(
        "Relaxed & Slow-Paced (Leisure & Spa)",
        "Balanced & Moderate (Sensory & Rest Intervals)",
        "High-Intensity & Active Explorer"
    )

    val pacingOptions = listOf(
        "Morning Activities (8am-12pm) with Leisure Afternoons",
        "Midday Discovery & Twilight Dining",
        "All-Day Immersive Sightseeing"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Preferred Airlines
        Text("Preferred Airlines & Alliances", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = airlines,
            onValueChange = { airlines = it },
            leadingIcon = { Icon(Icons.Default.AirplanemodeActive, null, tint = OceanBlue) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Preferred Lodging
        Text("Preferred Hotel & Lodging Types", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = lodgingTypes,
            onValueChange = { lodgingTypes = it },
            leadingIcon = { Icon(Icons.Default.Hotel, null, tint = AmberGold) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Activity Level
        Text("Activity Level & Energy", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        activityLevels.forEach { lvl ->
            val isSelected = activityLevel.contains(lvl.substringBefore(" ("), ignoreCase = true)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) OceanBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clickable { activityLevel = lvl }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.TrendingUp else Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (isSelected) OceanBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = lvl,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) OceanBlue else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pacing Schedule
        Text("Daily Pacing Schedule", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        pacingOptions.forEach { opt ->
            val isSelected = pacing.contains(opt.substringBefore(" ("), ignoreCase = true)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) TealAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clickable { pacing = opt }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (isSelected) TealAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = opt,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) TealAccent else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dietary & Transit
        Text("Dietary Preferences & Allergies", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = dietary,
            onValueChange = { dietary = it },
            leadingIcon = { Icon(Icons.Default.Fastfood, null, tint = Color(0xFFEC4899)) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Wheelchair Accessibility & Mobility
        var wheelchair by remember(preference) { mutableStateOf(preference.wheelchairRequirements) }
        Text("Wheelchair & Mobility Requirements", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = wheelchair,
            onValueChange = { wheelchair = it },
            leadingIcon = { Icon(Icons.Default.Schedule, null, tint = OceanBlue) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Family Age Brackets & Composition
        var familyAges by remember(preference) { mutableStateOf(preference.familyAgeBrackets) }
        Text("Family Age Brackets & Party Composition", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = familyAges,
            onValueChange = { familyAges = it },
            leadingIcon = { Icon(Icons.Default.Star, null, tint = AmberGold) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Sensory Needs & Rest Intervals
        var sensoryNotes by remember(preference) { mutableStateOf(preference.sensoryAndMobilityNotes) }
        Text("Sensory & Rest Interval Notes", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = sensoryNotes,
            onValueChange = { sensoryNotes = it },
            leadingIcon = { Icon(Icons.Default.Psychology, null, tint = Color(0xFF8B5CF6)) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Transit & Mobility Preference", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = transit,
            onValueChange = { transit = it },
            leadingIcon = { Icon(Icons.Default.DirectionsTransit, null, tint = EmeraldGreen) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                onSavePreferences(
                    preference.copy(
                        preferredAirlines = airlines,
                        preferredHotelTypes = lodgingTypes,
                        activityLevel = activityLevel,
                        preferredTravelStyle = travelStyle,
                        dietaryPreferences = dietary,
                        wheelchairRequirements = wheelchair,
                        familyAgeBrackets = familyAges,
                        sensoryAndMobilityNotes = sensoryNotes,
                        pacingPreference = pacing,
                        preferredTransit = transit
                    )
                )
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_preferences_button")
        ) {
            Icon(Icons.Default.AutoAwesome, null, tint = AmberGold)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save & Update AI Planner", fontWeight = FontWeight.Bold)
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackAndRatingsSection(
    feedbacks: List<TripFeedbackEntity>,
    trips: List<TripEntity>,
    onSubmitFeedback: (Long, String, String, Int, String, String, String) -> Unit,
    onDeleteFeedback: (Long) -> Unit
) {
    val scrollState = rememberScrollState()
    var isAddingNewFeedback by remember { mutableStateOf(false) }

    var selectedTripId by remember { mutableStateOf(trips.firstOrNull()?.id ?: 0L) }
    var rating by remember { mutableIntStateOf(5) }
    var likedTags = remember { mutableStateListOf<String>() }
    var dislikedTags = remember { mutableStateListOf<String>() }
    var notes by remember { mutableStateOf("") }

    val presetLikedOptions = listOf(
        "Scenic views", "Spacious kitchen", "Scenic rail", "Flight upgrades",
        "Step-free ramps", "Morning quiet walks", "Heated pool", "Organic dining"
    )
    val presetDislikedOptions = listOf(
        "Midday rush", "Too many stairs", "Long car drive", "Crowded venue", "High altitude wind"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Past Trip Feedback & AI Learnings",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )

            OutlinedButton(
                onClick = { isAddingNewFeedback = !isAddingNewFeedback },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(if (isAddingNewFeedback) Icons.Default.Close else Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isAddingNewFeedback) "Cancel" else "Rate a Trip", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Add feedback card
        AnimatedVisibility(visible = isAddingNewFeedback) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Select Completed Trip to Rate", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        trips.forEach { t ->
                            FilterChip(
                                selected = selectedTripId == t.id,
                                onClick = { selectedTripId = t.id },
                                label = { Text(t.destination, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Star Rating
                    Text("Overall Experience Rating", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (1..5).forEach { star ->
                            IconButton(
                                onClick = { rating = star },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (star <= rating) AmberGold else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Liked Aspects
                    Text("What did you love? (Teaches AI your affinities)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        presetLikedOptions.forEach { tag ->
                            val selected = likedTags.contains(tag)
                            FilterChip(
                                selected = selected,
                                onClick = { if (selected) likedTags.remove(tag) else likedTags.add(tag) },
                                label = { Text(tag, fontSize = 10.sp) },
                                leadingIcon = { Icon(Icons.Default.ThumbUp, null, modifier = Modifier.size(12.dp), tint = EmeraldGreen) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = EmeraldGreen
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Disliked Aspects
                    Text("What could be improved? (AI will avoid in future)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        presetDislikedOptions.forEach { tag ->
                            val selected = dislikedTags.contains(tag)
                            FilterChip(
                                selected = selected,
                                onClick = { if (selected) dislikedTags.remove(tag) else dislikedTags.add(tag) },
                                label = { Text(tag, fontSize = 10.sp) },
                                leadingIcon = { Icon(Icons.Default.ThumbDown, null, modifier = Modifier.size(12.dp), tint = SunsetCoral) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SunsetCoral.copy(alpha = 0.2f),
                                    selectedLabelColor = SunsetCoral
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Specific Trip Feedback / Highlights") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val selectedTrip = trips.firstOrNull { it.id == selectedTripId }
                            val tripTitle = selectedTrip?.title ?: "Vacation"
                            val destination = selectedTrip?.destination ?: "Destination"
                            onSubmitFeedback(
                                selectedTripId,
                                tripTitle,
                                destination,
                                rating,
                                likedTags.joinToString(", "),
                                dislikedTags.joinToString(", "),
                                notes
                            )
                            isAddingNewFeedback = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = AmberGold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Submit & Train AI Travel Model", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // List of feedbacks
        if (feedbacks.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No past feedbacks yet. Complete a trip and rate it to teach the AI your personalized preferences!",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            feedbacks.forEach { fb ->
                FeedbackCardItem(feedback = fb, onDelete = { onDeleteFeedback(fb.id) })
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun FeedbackCardItem(
    feedback: TripFeedbackEntity,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = feedback.destination,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = OceanBlue)
                    )
                    Text(
                        text = feedback.tripTitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(feedback.rating) {
                        Icon(Icons.Default.Star, null, tint = AmberGold, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (feedback.likedAspects.isNotBlank()) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.ThumbUp, null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Liked: ${feedback.likedAspects}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (feedback.dislikedAspects.isNotBlank()) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.ThumbDown, null, tint = SunsetCoral, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Avoided: ${feedback.dislikedAspects}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (feedback.feedbackNotes.isNotBlank()) {
                Text(
                    text = "\"${feedback.feedbackNotes}\"",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 12.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Learned AI Takeaway Pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = OceanBlue.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = OceanBlue, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = feedback.learnedActionableTakeaway,
                        style = MaterialTheme.typography.labelSmall.copy(color = OceanBlue, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProactiveSuggestionsSection(
    suggestions: List<ProactiveSuggestionEntity>,
    onSelectSuggestion: (ProactiveSuggestionEntity) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Proactive Tailored Trip Proposals",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Generated automatically by matching your learned Traveler DNA with loyalty points & timeshares",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (suggestions.isNotEmpty()) {
            suggestions.forEach { sug ->
                ProactiveSuggestionCard(suggestion = sug, onPlanNow = { onSelectSuggestion(sug) })
                Spacer(modifier = Modifier.height(12.dp))
            }
        } else {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("No Proactive Proposals Yet", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "As you plan trips and save preferences, Gemini will automatically synthesize tailored vacation proposals matching your travel style.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }
    }
}

@Composable
fun ProactiveSuggestionCard(
    suggestion: ProactiveSuggestionEntity,
    onPlanNow: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Hero tag + Match score badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF8B5CF6).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = suggestion.heroTag,
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldGreen.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = EmeraldGreen, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${suggestion.matchScorePercent}% Match",
                            style = MaterialTheme.typography.labelSmall.copy(color = EmeraldGreen, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = suggestion.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${suggestion.destination} • ${suggestion.durationDays} Days • ${suggestion.season}",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Rationale
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "💡 ${suggestion.rationale}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Details Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("✈️ Airline Loyalty", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Text(suggestion.suggestedAirline, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("🏡 Lodging", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Text(suggestion.suggestedLodging, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Est. Budget: $${suggestion.estimatedBudget.toInt()} ${suggestion.currency}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "💰 Points Savings: ~$${suggestion.pointsSavingsUsd.toInt()}",
                        style = MaterialTheme.typography.labelSmall.copy(color = EmeraldGreen, fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = onPlanNow,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                    modifier = Modifier.testTag("plan_proactive_trip_btn_${suggestion.id}")
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = AmberGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Plan This Trip", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun TravelerPreferenceScreen(
    viewModel: TravelViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onSelectProactiveTrip: (ProactiveSuggestionEntity) -> Unit = {}
) {
    BackHandler(onBack = onNavigateBack)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TravelerPreferenceDialog(
            viewModel = viewModel,
            onDismiss = onNavigateBack,
            onSelectProactiveTrip = onSelectProactiveTrip
        )
    }
}

