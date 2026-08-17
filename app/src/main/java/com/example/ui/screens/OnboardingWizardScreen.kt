package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import com.example.viewmodel.TravelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingWizardScreen(
    viewModel: TravelViewModel,
    onOnboardingComplete: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }

    // Step 1: Expedition Style & Pacing
    var selectedTravelStyle by remember { mutableStateOf("Balanced Scenic Explorer") }
    var selectedPacing by remember { mutableStateOf("Morning Active / Relaxed Afternoon") }
    var selectedActivityLevel by remember { mutableStateOf("Balanced") }

    // Step 2: Family & Accessibility Profiles
    val accessibilityOptions = remember {
        mutableStateListOf<String>()
    }
    var selectedDietary by remember { mutableStateOf("Gluten-Free & Tree Nut Alert") }
    var selectedFamilyDynamics by remember { mutableStateOf("Family & Multi-Gen (Ages 6-68)") }

    // Step 3: Loyalty & Timeshare Programs
    val selectedLoyaltyPrograms = remember {
        mutableStateListOf(
            "Delta SkyMiles (Platinum Elite)",
            "World of Hyatt (Globalist)",
            "RCI Timeshare Points (32 TPU)"
        )
    }

    Scaffold(
        containerColor = CartographyDarkBase,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(NavigationalGold.copy(alpha = 0.15f))
                                .border(1.dp, NavigationalGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = "Compass",
                                tint = NavigationalGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Marco Expeditions",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = NavigationalGold
                                )
                            )
                            Text(
                                text = "Traveler DNA Setup Wizard",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextAtlasPrimary
                                )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CartographySurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(CartographyDarkBase)
        ) {
            // Stepper Progress Header
            CartographicStepperHeader(
                currentStep = currentStep,
                totalSteps = 3
            )

            // Step Content Deck
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "WizardStepTransition"
                ) { step ->
                    when (step) {
                        1 -> StepOnePacingScreen(
                            selectedStyle = selectedTravelStyle,
                            onSelectStyle = { selectedTravelStyle = it },
                            selectedPacing = selectedPacing,
                            onSelectPacing = { selectedPacing = it },
                            selectedActivity = selectedActivityLevel,
                            onSelectActivity = { selectedActivityLevel = it }
                        )
                        2 -> StepTwoAccessibilityScreen(
                            accessibilityOptions = accessibilityOptions,
                            onToggleOption = { option ->
                                if (accessibilityOptions.contains(option)) {
                                    accessibilityOptions.remove(option)
                                } else {
                                    accessibilityOptions.add(option)
                                }
                            },
                            dietary = selectedDietary,
                            onDietaryChange = { selectedDietary = it },
                            familyDynamics = selectedFamilyDynamics,
                            onFamilyDynamicsChange = { selectedFamilyDynamics = it }
                        )
                        3 -> StepThreeLoyaltyScreen(
                            selectedPrograms = selectedLoyaltyPrograms,
                            onToggleProgram = { program ->
                                if (selectedLoyaltyPrograms.contains(program)) {
                                    selectedLoyaltyPrograms.remove(program)
                                } else {
                                    selectedLoyaltyPrograms.add(program)
                                }
                            }
                        )
                    }
                }
            }

            // Bottom Navigation Bar
            Surface(
                color = CartographySurface,
                border = BorderStroke(1.dp, ContourBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep -= 1 },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextAtlasPrimary
                            ),
                            border = BorderStroke(1.dp, ContourBorder)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        onClick = {
                            if (currentStep < 3) {
                                currentStep += 1
                            } else {
                                // Final step: Complete onboarding
                                viewModel.completeOnboarding(
                                    travelStyle = selectedTravelStyle,
                                    pacing = selectedPacing,
                                    activityLevel = selectedActivityLevel,
                                    wheelchair = accessibilityOptions.joinToString("; "),
                                    dietary = selectedDietary,
                                    family = selectedFamilyDynamics,
                                    sensory = "Quiet sensory pacing requested",
                                    preferredAirlines = selectedLoyaltyPrograms.filter { it.contains("SkyMiles") || it.contains("Alliance") || it.contains("Airline") }.joinToString(", "),
                                    preferredHotelTypes = selectedLoyaltyPrograms.filter { it.contains("Hyatt") || it.contains("Bonvoy") || it.contains("Hilton") || it.contains("Timeshare") }.joinToString(", "),
                                    onComplete = onOnboardingComplete
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NavigationalGold,
                            contentColor = CartographyDarkBase
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (currentStep == 3) "Initialize Traveler DNA" else "Continue",
                            fontWeight = FontWeight.Bold
                        )
                        if (currentStep < 3) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Forward",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartographicStepperHeader(
    currentStep: Int,
    totalSteps: Int
) {
    val stepTitles = listOf("Pacing & Style", "Accessibility & Family", "Loyalty & Timeshare")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CartographySurface)
            .border(BorderStroke(1.dp, ContourBorder))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..totalSteps) {
                val isActive = i == currentStep
                val isCompleted = i < currentStep

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isActive -> NavigationalGold
                                    isCompleted -> WayfinderEmerald
                                    else -> CartographyCard
                                }
                            )
                            .border(
                                1.dp,
                                if (isActive || isCompleted) NavigationalGold else ContourBorder,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Done",
                                tint = CartographyDarkBase,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Text(
                                text = "$i",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) CartographyDarkBase else TextAtlasSecondary
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Step $i",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) NavigationalGold else TextAtlasSecondary
                        )
                    )
                    if (i < totalSteps) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(if (isCompleted) WayfinderEmerald else ContourBorder)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stepTitles.getOrElse(currentStep - 1) { "" },
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextAtlasPrimary
            )
        )
    }
}

@Composable
private fun StepOnePacingScreen(
    selectedStyle: String,
    onSelectStyle: (String) -> Unit,
    selectedPacing: String,
    onSelectPacing: (String) -> Unit,
    selectedActivity: String,
    onSelectActivity: (String) -> Unit
) {
    val styles = listOf(
        "Balanced Scenic Explorer" to "Cultural discovery with scenic vistas and balanced walking pace.",
        "Slow-Paced Relaxation" to "Leisurely mornings, long culinary lunches, and spa retreats.",
        "High-Intensity Cultural Trail" to "Full-day historical itineraries, sunrise photography, and multi-site tours.",
        "Luxury Multi-Gen Villa Escape" to "Private transport, multi-bedroom timeshares, and kid-friendly adventures."
    )

    val pacings = listOf(
        "Morning Active / Relaxed Afternoon",
        "Evenly Distributed Daylight Pacing",
        "Late Start / Sunset & Evening Focus"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        PapercraftSectionHeader(
            title = "1. Expedition Travel Style",
            subtitle = "How do you prefer to experience destinations?"
        )
        Spacer(modifier = Modifier.height(8.dp))

        styles.forEach { (style, desc) ->
            val selected = selectedStyle == style
            PapercraftSelectCard(
                title = style,
                description = desc,
                isSelected = selected,
                icon = Icons.Default.Terrain,
                onClick = { onSelectStyle(style) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        PapercraftSectionHeader(
            title = "2. Daily Pacing & Rhythm",
            subtitle = "Marco tailors morning departure times and rest buffers."
        )
        Spacer(modifier = Modifier.height(8.dp))

        pacings.forEach { pace ->
            val selected = selectedPacing == pace
            PapercraftSelectCard(
                title = pace,
                description = "",
                isSelected = selected,
                icon = Icons.Default.Schedule,
                onClick = { onSelectPacing(pace) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StepTwoAccessibilityScreen(
    accessibilityOptions: List<String>,
    onToggleOption: (String) -> Unit,
    dietary: String,
    onDietaryChange: (String) -> Unit,
    familyDynamics: String,
    onFamilyDynamicsChange: (String) -> Unit
) {
    val accessibilityChecklist = listOf(
        "Step-Free Wheelchair & Ramp Access",
        "Stroller-Friendly Paths & Elevators",
        "Low-Sensory & Quiet Recovery Zones",
        "Visual & Audio Guide Support",
        "Accessible Ground Transit Pre-Booking"
    )

    val dietaryOptions = listOf(
        "Gluten-Free & Tree Nut Alert",
        "Vegetarian / Plant-Based Only",
        "Kosher Certified Dining",
        "Halal Certified Dining",
        "No Dietary Restrictions"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        PapercraftSectionHeader(
            title = "Accessibility & Mobility Standards",
            subtitle = "Marco strictly verifies venues for step-free access and assistance."
        )
        Spacer(modifier = Modifier.height(8.dp))

        accessibilityChecklist.forEach { option ->
            val isChecked = accessibilityOptions.contains(option)
            PapercraftCheckboxCard(
                title = option,
                isChecked = isChecked,
                onClick = { onToggleOption(option) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        PapercraftSectionHeader(
            title = "Dietary & Allergen Verification",
            subtitle = "Marco prompts kitchens directly for cross-contamination safety."
        )
        Spacer(modifier = Modifier.height(8.dp))

        dietaryOptions.forEach { diet ->
            val isSelected = dietary == diet
            PapercraftSelectCard(
                title = diet,
                description = "",
                isSelected = isSelected,
                icon = Icons.Default.Restaurant,
                onClick = { onDietaryChange(diet) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StepThreeLoyaltyScreen(
    selectedPrograms: List<String>,
    onToggleProgram: (String) -> Unit
) {
    val loyaltyOptions = listOf(
        "Delta SkyMiles (Platinum Elite)" to "Airline",
        "United MileagePlus (Premier 1K)" to "Airline",
        "American AAdvantage (Executive Platinum)" to "Airline",
        "World of Hyatt (Globalist / Free Suite Upgrades)" to "Hotel",
        "Marriott Bonvoy (Titanium Elite)" to "Hotel",
        "Hilton Honors (Diamond)" to "Hotel",
        "RCI Timeshare Exchange Points (32 TPU)" to "Timeshare",
        "Interval International (Club Interval Gold)" to "Timeshare",
        "Chase Sapphire Ultimate Rewards" to "Credit Card",
        "Amex Membership Rewards (1:1 Airline Transfers)" to "Credit Card"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        PapercraftSectionHeader(
            title = "Loyalty & Timeshare Optimization",
            subtitle = "Select your active programs. Marco calculates cash vs points arbitrage sweet spots automatically."
        )
        Spacer(modifier = Modifier.height(12.dp))

        loyaltyOptions.forEach { (program, category) ->
            val isSelected = selectedPrograms.contains(program)
            PapercraftLoyaltyBadgeCard(
                name = program,
                category = category,
                isSelected = isSelected,
                onClick = { onToggleProgram(program) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PapercraftSectionHeader(
    title: String,
    subtitle: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = NavigationalGold
            )
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextAtlasSecondary
            )
        )
    }
}

@Composable
private fun PapercraftSelectCard(
    title: String,
    description: String,
    isSelected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) CartographyCardElevated else CartographyCard,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) NavigationalGold else ContourBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) NavigationalGold.copy(alpha = 0.2f) else CartographyDarkBase
                    )
                    .border(
                        1.dp,
                        if (isSelected) NavigationalGold else ContourBorder,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) NavigationalGold else TextAtlasSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) TextAtlasPrimary else TextAtlasSecondary
                    )
                )
                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextAtlasSecondary
                        )
                    )
                }
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = NavigationalGold,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PapercraftCheckboxCard(
    title: String,
    isChecked: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isChecked) CartographyCardElevated else CartographyCard,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (isChecked) SilkRoadTeal else ContourBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = SilkRoadTeal,
                    uncheckedColor = ContourBorder,
                    checkmarkColor = CartographyDarkBase
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                    color = if (isChecked) TextAtlasPrimary else TextAtlasSecondary
                )
            )
        }
    }
}

@Composable
private fun PapercraftLoyaltyBadgeCard(
    name: String,
    category: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val categoryColor = when (category) {
        "Airline" -> WaypointCyan
        "Hotel" -> NavigationalGold
        "Timeshare" -> SilkRoadTeal
        else -> CompassLilac
    }

    Surface(
        color = if (isSelected) CartographyCardElevated else CartographyCard,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) categoryColor else ContourBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(categoryColor.copy(alpha = 0.15f))
                    .border(1.dp, categoryColor, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = categoryColor,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) TextAtlasPrimary else TextAtlasSecondary
                ),
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = categoryColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
