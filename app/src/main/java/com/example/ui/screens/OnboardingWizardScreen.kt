package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.model.ProactiveSuggestionEntity
import com.example.data.model.UserPreferenceEntity
import com.example.ui.components.IntensityScaleRow
import com.example.ui.components.TravelerPassportCard
import com.example.ui.model.AirportItem
import com.example.ui.model.LoyaltyProgramCatalogItem
import com.example.ui.model.PreferenceConstants
import com.example.ui.model.SelectedLoyaltyProgram
import com.example.ui.theme.*
import com.example.viewmodel.TravelViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun OnboardingWizardScreen(
    viewModel: TravelViewModel,
    onOnboardingComplete: (ProactiveSuggestionEntity) -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(1) }

    // Step 1: Origin & Identity
    var displayName by remember { mutableStateOf("") }
    var selectedAirport by remember { mutableStateOf<AirportItem?>(null) }
    var airportQuery by remember { mutableStateOf("") }
    var isLocatingGps by remember { mutableStateOf(false) }

    // Step 2: Motivation (weighted 1-7, key = MotivationItem.id) & Aspiration
    // Absent from the map = never rated. No default weight is ever assigned automatically.
    val motivationWeights = remember { mutableStateMapOf<String, Int>() }
    var signatureAspiration by remember { mutableStateOf("") }

    // Step 3: Travel Style (weighted 1-7, key = PreferenceConstants.TRAVEL_STYLES title)
    val travelStyleWeights = remember { mutableStateMapOf<String, Int>() }

    // Step 4: Loyalty Ledger
    val selectedLoyaltyPrograms = remember { mutableStateListOf<LoyaltyProgramCatalogItem>() }
    // Program.id -> tier the user actually claimed. Empty/absent means "not set" — never guessed.
    val selectedLoyaltyTiers = remember { mutableStateMapOf<String, String>() }
    // Program.id -> balance entered at link time. Optional; absent/blank stays blank.
    val selectedLoyaltyBalances = remember { mutableStateMapOf<String, String>() }
    var isCameraScannerOpen by remember { mutableStateOf(false) }
    var scannedCardText by remember { mutableStateOf("") }

    // Step 5: Pace, Comfort & Reveal — both weighted 1-7, same "absent = unrated" rule
    var accessibilityOptIn by remember { mutableStateOf(false) }
    val pacingWeights = remember { mutableStateMapOf<String, Int>() } // key = PACING_OPTIONS title
    val comfortWeights = remember { mutableStateMapOf<String, Int>() } // key = ComfortOption.label

    // Location Permission for GPS airport resolution
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_COARSE_LOCATION)

    fun resolveGpsAirport() {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
            return
        }
        isLocatingGps = true
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                isLocatingGps = false
                if (location != null) {
                    val nearest = PreferenceConstants.findNearestAirport(location.latitude, location.longitude)
                    selectedAirport = nearest
                    airportQuery = "${nearest.code} - ${nearest.city} (${nearest.name})"
                }
            }.addOnFailureListener {
                isLocatingGps = false
            }
        } catch (_: SecurityException) {
            isLocatingGps = false
        }
    }

    // Swaps `program` with its up/down neighbor *within its own category* — rank only ever
    // means something relative to other programs of the same type (which airline to book
    // before which other airline), so ranking is scoped per category rather than one global
    // list. List position within a category is the rank; there is no separate rank field.
    fun moveLoyaltyRank(program: LoyaltyProgramCatalogItem, direction: Int) {
        val sameCategoryIndices = selectedLoyaltyPrograms.indices.filter {
            selectedLoyaltyPrograms[it].categoryType == program.categoryType
        }
        val posInCategory = sameCategoryIndices.indexOfFirst { selectedLoyaltyPrograms[it].id == program.id }
        val swapWith = posInCategory + direction
        if (posInCategory !in sameCategoryIndices.indices || swapWith !in sameCategoryIndices.indices) return
        val idxA = sameCategoryIndices[posInCategory]
        val idxB = sameCategoryIndices[swapWith]
        val tmp = selectedLoyaltyPrograms[idxA]
        selectedLoyaltyPrograms[idxA] = selectedLoyaltyPrograms[idxB]
        selectedLoyaltyPrograms[idxB] = tmp
    }

    Scaffold(
        containerColor = LuxuryDarkBase,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(LuxuryCardElevated)
                                .border(1.dp, LuxuryBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = "Compass",
                                tint = ChampagneGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MARCO EXPEDITIONS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ChampagneGold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = when (currentStep) {
                                    1 -> "Step 1 of 5: Origin & Base Port"
                                    2 -> "Step 2 of 5: Motivation"
                                    3 -> "Step 3 of 5: Travel Style"
                                    4 -> "Step 4 of 5: Loyalty & Timeshare Ledger"
                                    5 -> "Step 5 of 5: Pace, Comfort & Passport Reveal"
                                    else -> "Traveler Setup"
                                },
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LuxuryDarkBase)
            )
        },
        bottomBar = {
            Surface(
                color = LuxurySurface,
                border = BorderStroke(1.dp, LuxuryBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep -= 1 },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, LuxuryBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            modifier = Modifier.testTag("onboarding_back_button")
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

                    // Forward / Complete Button
                    val canContinue = when (currentStep) {
                        1 -> selectedAirport != null || airportQuery.isNotBlank()
                        else -> true
                    }

                    Button(
                        onClick = {
                            if (currentStep < 5) {
                                currentStep += 1
                            } else {
                                // Final Step: Save preferences and build initial suggestion.
                                // Both fields are gated by canContinue on Step 1 (airport is
                                // required, name is not), so store exactly what the user gave —
                                // no invented "JFK (New York)" or "Marco Voyager" stand-ins.
                                val airportStr = selectedAirport?.let { "${it.code} (${it.city})" }
                                    ?: airportQuery
                                val nameStr = displayName

                                // Loyalty rank = current per-category list order (moveLoyaltyRank
                                // reorders in place), tier/balance are whatever the user actually
                                // set (blank if skipped).
                                val rankedLoyaltyPrograms = selectedLoyaltyPrograms.toList().map { program ->
                                    SelectedLoyaltyProgram(
                                        program = program,
                                        tier = selectedLoyaltyTiers[program.id] ?: "",
                                        balance = selectedLoyaltyBalances[program.id] ?: ""
                                    )
                                }

                                viewModel.completeOnboarding(
                                    displayName = nameStr,
                                    homeAirport = airportStr,
                                    motivationWeights = motivationWeights.toMap(),
                                    travelStyleWeights = travelStyleWeights.toMap(),
                                    signatureAspiration = signatureAspiration,
                                    accessibilityOptIn = accessibilityOptIn,
                                    pacingWeights = pacingWeights.toMap(),
                                    comfortWeights = comfortWeights.toMap(),
                                    selectedLoyaltyPrograms = rankedLoyaltyPrograms,
                                    onComplete = {
                                        // Build proactive suggestion handoff, keyed off the
                                        // highest-weighted motivation. If nothing was rated,
                                        // fall back to a generic starting point rather than
                                        // claiming it "matches" a motive the user never gave.
                                        val topMotivationId = motivationWeights.maxByOrNull { it.value }?.key
                                        val match = PreferenceConstants.MOTIVATIONS.find { it.id == topMotivationId }
                                            ?: PreferenceConstants.MOTIVATIONS.last()
                                        val rationale = buildString {
                                            append(
                                                if (nameStr.isNotBlank()) "Custom journey generated for $nameStr ($airportStr)"
                                                else "Custom journey generated from $airportStr"
                                            )
                                            if (topMotivationId != null) append(" matching your ${match.label} motive.") else append(".")
                                        }
                                        val suggestion = ProactiveSuggestionEntity(
                                            title = "${match.label} in ${match.sampleDestination}",
                                            destination = match.sampleDestination,
                                            countryCode = match.sampleCountryCode,
                                            durationDays = match.durationDays,
                                            estimatedBudget = match.estimatedBudget,
                                            currency = "USD",
                                            matchScorePercent = 99,
                                            rationale = rationale,
                                            suggestedAirline = selectedLoyaltyPrograms.firstOrNull { it.categoryType == "AIRLINE" }?.name ?: "Partner Airline",
                                            suggestedLodging = selectedLoyaltyPrograms.firstOrNull { it.categoryType == "HOTEL" }?.name ?: "Curated Boutique Lodge",
                                            activityPace = pacingWeights.maxByOrNull { it.value }?.key ?: "",
                                            heroTag = "Tailored Passport Match",
                                            pointsSavingsUsd = 0.0,
                                            season = "Ideal Season"
                                        )
                                        onOnboardingComplete(suggestion)
                                    }
                                )
                            }
                        },
                        enabled = canContinue,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LuxuryCardElevated,
                            contentColor = TextPrimary,
                            disabledContainerColor = LuxurySurface,
                            disabledContentColor = TextMuted
                        ),
                        border = BorderStroke(1.dp, if (canContinue) ChampagneGold else LuxuryBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("onboarding_continue_button")
                    ) {
                        Text(
                            text = if (currentStep == 5) "Plan Your Journey →" else "Continue",
                            fontWeight = FontWeight.Bold
                        )
                        if (currentStep < 5) {
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Step Progress Indicator Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 1..5) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (i <= currentStep) ChampagneGold else LuxuryBorder)
                    )
                }
            }

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                label = "onboarding_steps"
            ) { step ->
                when (step) {
                    1 -> Step1OriginSection(
                        displayName = displayName,
                        onDisplayNameChange = { displayName = it },
                        selectedAirport = selectedAirport,
                        airportQuery = airportQuery,
                        onAirportQueryChange = { query ->
                            airportQuery = query
                            selectedAirport = PreferenceConstants.MAJOR_AIRPORTS.find {
                                it.code.equals(query.trim(), ignoreCase = true) ||
                                        it.city.contains(query.trim(), ignoreCase = true)
                            }
                        },
                        onSelectAirport = { airport ->
                            selectedAirport = airport
                            airportQuery = "${airport.code} - ${airport.city} (${airport.name})"
                        },
                        isLocating = isLocatingGps,
                        onGpsLocate = { resolveGpsAirport() }
                    )

                    2 -> Step2MotivationSection(
                        motivationWeights = motivationWeights,
                        onWeightChange = { id, weight ->
                            if (weight == null) motivationWeights.remove(id) else motivationWeights[id] = weight
                        },
                        signatureAspiration = signatureAspiration,
                        onSignatureAspirationChange = { signatureAspiration = it }
                    )

                    3 -> Step3TravelStyleSection(
                        travelStyleWeights = travelStyleWeights,
                        onWeightChange = { title, weight ->
                            if (weight == null) travelStyleWeights.remove(title) else travelStyleWeights[title] = weight
                        }
                    )

                    4 -> Step4LoyaltySection(
                        selectedPrograms = selectedLoyaltyPrograms,
                        selectedTiers = selectedLoyaltyTiers,
                        selectedBalances = selectedLoyaltyBalances,
                        onToggleProgram = { program ->
                            if (selectedLoyaltyPrograms.any { it.id == program.id }) {
                                selectedLoyaltyPrograms.removeAll { it.id == program.id }
                                selectedLoyaltyTiers.remove(program.id)
                                selectedLoyaltyBalances.remove(program.id)
                            } else {
                                selectedLoyaltyPrograms.add(program)
                            }
                        },
                        onSelectTier = { program, tier ->
                            // Tap the already-selected tier again to clear it back to "not set"
                            if (selectedLoyaltyTiers[program.id] == tier) {
                                selectedLoyaltyTiers.remove(program.id)
                            } else {
                                selectedLoyaltyTiers[program.id] = tier
                            }
                        },
                        onBalanceChange = { program, balance -> selectedLoyaltyBalances[program.id] = balance },
                        onMoveRank = { program, direction -> moveLoyaltyRank(program, direction) },
                        onOpenScanner = { isCameraScannerOpen = true }
                    )

                    5 -> {
                        val topMotivationLabel = motivationWeights.maxByOrNull { it.value }?.key
                            ?.let { id -> PreferenceConstants.MOTIVATIONS.find { m -> m.id == id }?.label } ?: ""
                        val topTravelStyleLabel = travelStyleWeights.maxByOrNull { it.value }?.key ?: ""

                        Step5ComfortAndRevealSection(
                            displayName = displayName,
                            homeAirport = selectedAirport?.let { "${it.code} (${it.city})" } ?: airportQuery,
                            motivationLabel = topMotivationLabel,
                            travelStyleLabel = topTravelStyleLabel,
                            signatureAspiration = signatureAspiration,
                            accessibilityOptIn = accessibilityOptIn,
                            onAccessibilityOptInChange = { accessibilityOptIn = it },
                            pacingWeights = pacingWeights,
                            onPacingWeightChange = { title, weight ->
                                if (weight == null) pacingWeights.remove(title) else pacingWeights[title] = weight
                            },
                            comfortWeights = comfortWeights,
                            onComfortWeightChange = { label, weight ->
                                if (weight == null) comfortWeights.remove(label) else comfortWeights[label] = weight
                            },
                            linkedProgramCount = selectedLoyaltyPrograms.size
                        )
                    }
                }
            }
        }
    }

    // CameraX + ML Kit OCR Loyalty Scanner Bottom Sheet
    if (isCameraScannerOpen) {
        ModalBottomSheet(
            onDismissRequest = { isCameraScannerOpen = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            CameraCardScannerSheet(
                onCardDetected = { text ->
                    scannedCardText = text
                    // Auto match program from catalog
                    val matched = PreferenceConstants.LOYALTY_CATALOG.find {
                        text.contains(it.name, ignoreCase = true) ||
                                text.contains(it.id, ignoreCase = true)
                    }
                    if (matched != null && !selectedLoyaltyPrograms.any { it.id == matched.id }) {
                        selectedLoyaltyPrograms.add(matched)
                    }
                    isCameraScannerOpen = false
                },
                onDismiss = { isCameraScannerOpen = false }
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 1: ORIGIN & BASE PORT
// -------------------------------------------------------------
@Composable
private fun Step1OriginSection(
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    selectedAirport: AirportItem?,
    airportQuery: String,
    onAirportQueryChange: (String) -> Unit,
    onSelectAirport: (AirportItem) -> Unit,
    isLocating: Boolean,
    onGpsLocate: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Welcome to Marco",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Set your origin once. Marco calculates direct routes, airline alliances, and flight timings from your home port.",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // First Name Input
        OutlinedTextField(
            value = displayName,
            onValueChange = onDisplayNameChange,
            label = { Text("Your First Name or Call-Sign") },
            placeholder = { Text("e.g. Alexandra") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ChampagneGold,
                unfocusedBorderColor = LuxuryBorder,
                focusedContainerColor = LuxurySurface,
                unfocusedContainerColor = LuxurySurface,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_name_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Home Airport Input with GPS Autodetect Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Home Airport (Required)",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            TextButton(
                onClick = onGpsLocate,
                enabled = !isLocating
            ) {
                if (isLocating) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = ChampagneGold)
                } else {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "GPS",
                        tint = ChampagneGold,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isLocating) "Locating..." else "Use Current Location",
                    style = MaterialTheme.typography.labelSmall.copy(color = ChampagneGold, fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = airportQuery,
            onValueChange = onAirportQueryChange,
            placeholder = { Text("Search by airport code or city (e.g. SFO, London, Chicago)") },
            leadingIcon = {
                Icon(Icons.Default.AirplanemodeActive, null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (airportQuery.isNotBlank()) {
                    IconButton(onClick = { onAirportQueryChange("") }) {
                        Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ChampagneGold,
                unfocusedBorderColor = LuxuryBorder,
                focusedContainerColor = LuxurySurface,
                unfocusedContainerColor = LuxurySurface,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_airport_input")
        )

        val filteredAirports = remember(airportQuery) {
            if (airportQuery.isBlank()) {
                PreferenceConstants.MAJOR_AIRPORTS.take(8)
            } else {
                PreferenceConstants.MAJOR_AIRPORTS.filter {
                    it.code.contains(airportQuery.trim(), ignoreCase = true) ||
                            it.city.contains(airportQuery.trim(), ignoreCase = true) ||
                            it.name.contains(airportQuery.trim(), ignoreCase = true)
                }.take(6)
            }
        }

        // Airport Quick Suggestions — the header only renders when there is a list beneath it.
        // Once an airport is picked the list filters to empty, and an orphaned "Major Hubs:"
        // label sitting above a screen of blank space reads as a rendering failure.
        if (filteredAirports.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Major Hubs:",
                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(6.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            filteredAirports.forEach { airport ->
                val isSelected = selectedAirport?.code == airport.code
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) LuxuryCardElevated else LuxurySurface,
                    border = BorderStroke(1.dp, if (isSelected) ChampagneGold else LuxuryBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectAirport(airport) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = LuxuryCard,
                                border = BorderStroke(1.dp, LuxuryBorder)
                            ) {
                                Text(
                                    text = airport.code,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ChampagneGold
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = airport.city,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = airport.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        if (isSelected) {
                            Icon(Icons.Default.Check, null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// -------------------------------------------------------------
// STEP 2: MOTIVATION (WEIGHTED) & ASPIRATION
// -------------------------------------------------------------
@Composable
private fun Step2MotivationSection(
    motivationWeights: Map<String, Int>,
    onWeightChange: (id: String, weight: Int?) -> Unit,
    signatureAspiration: String,
    onSignatureAspirationChange: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "What Drives Your Travels?",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Rate every motivation that applies to you — 7 means it defines your trips, 1 means it barely registers. Leave anything you're unsure about unrated.",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PreferenceConstants.MOTIVATIONS.forEach { item ->
                IntensityScaleRow(
                    label = item.label,
                    description = item.description,
                    weight = motivationWeights[item.id],
                    onWeightChange = { onWeightChange(item.id, it) },
                    modifier = Modifier.testTag("motivation_weight_${item.id}")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Signature Aspiration Question
        Text(
            text = "The Uncanny Question:",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = ChampagneGold,
                letterSpacing = 0.8.sp
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "What is the one trip you have always dreamed of, but haven't taken yet?",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = signatureAspiration,
            onValueChange = onSignatureAspirationChange,
            placeholder = { Text("e.g. Stargazing at Atacama desert observatory & tasting Patagonia malbec...") },
            minLines = 3,
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ChampagneGold,
                unfocusedBorderColor = LuxuryBorder,
                focusedContainerColor = LuxurySurface,
                unfocusedContainerColor = LuxurySurface,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signature_aspiration_input")
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// -------------------------------------------------------------
// STEP 3: TRAVEL STYLE (WEIGHTED) — restored as its own question, distinct from motivation
// -------------------------------------------------------------
@Composable
private fun Step3TravelStyleSection(
    travelStyleWeights: Map<String, Int>,
    onWeightChange: (title: String, weight: Int?) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Your Travel Style",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Separate from why you travel — how do you like to move through a trip once you're there? Rate each; most travelers hold more than one.",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PreferenceConstants.TRAVEL_STYLES.forEach { (title, description) ->
                IntensityScaleRow(
                    label = title,
                    description = description,
                    weight = travelStyleWeights[title],
                    onWeightChange = { onWeightChange(title, it) },
                    modifier = Modifier.testTag("travel_style_weight_${title.hashCode()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// -------------------------------------------------------------
// STEP 4: LOYALTY & REWARDS LEDGER
// -------------------------------------------------------------
@Composable
private fun Step4LoyaltySection(
    selectedPrograms: List<LoyaltyProgramCatalogItem>,
    selectedTiers: Map<String, String>,
    selectedBalances: Map<String, String>,
    onToggleProgram: (LoyaltyProgramCatalogItem) -> Unit,
    onSelectTier: (LoyaltyProgramCatalogItem, String) -> Unit,
    onBalanceChange: (LoyaltyProgramCatalogItem, String) -> Unit,
    onMoveRank: (LoyaltyProgramCatalogItem, Int) -> Unit,
    onOpenScanner: () -> Unit
) {
    val scrollState = rememberScrollState()

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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Loyalty & Portfolios",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select your programs or scan a card to let Marco auto-calculate transfer multipliers.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1-Tap Camera Card Scanner Button
        Button(
            onClick = onOpenScanner,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LuxuryCardElevated,
                contentColor = TextPrimary
            ),
            border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("scan_loyalty_card_button")
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Scan Card",
                tint = ChampagneGold,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scan Card with Camera (ML Kit OCR)", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Programs Count Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Catalog (${selectedPrograms.size} Selected):",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
            )
            if (selectedPrograms.isNotEmpty()) {
                Text(
                    text = "Balances start at $0 (Genuine Ledger)",
                    style = MaterialTheme.typography.labelSmall.copy(color = ChampagneGold)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grouped Catalog
        val categories = listOf(
            "AIRLINE" to "Airline Alliances & Frequent Flyer",
            "HOTEL" to "Hotel Loyalty Programs",
            "TIMESHARE" to "Timeshares & Vacation Clubs",
            "CREDIT_CARD" to "Flexible Points & Credit Cards"
        )

        categories.forEach { (catKey, catTitle) ->
            Text(
                text = catTitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.padding(vertical = 6.dp)
            )

            val items = PreferenceConstants.LOYALTY_CATALOG.filter { it.categoryType == catKey }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEach { program ->
                    val isSelected = selectedPrograms.any { it.id == program.id }
                    val chosenTier = selectedTiers[program.id]
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) LuxuryCardElevated else LuxurySurface,
                        border = BorderStroke(1.dp, if (isSelected) ChampagneGold else LuxuryBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleProgram(program) }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = program.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) ChampagneGold else TextPrimary
                                        )
                                    )
                                    Text(
                                        text = if (isSelected) {
                                            "Unit: ${program.defaultUnit} • Status: ${chosenTier ?: "Not set"}"
                                        } else {
                                            "Unit: ${program.defaultUnit}"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    )
                                }

                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                                }
                            }

                            // Real tier selector — only appears once the program is selected.
                            // Nothing is preselected; if the user never taps one, tier stays "".
                            if (isSelected) {
                                Text(
                                    text = "Your status (optional):",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp),
                                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 4.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    program.tierOptions.forEach { tier ->
                                        val tierSelected = chosenTier == tier
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (tierSelected) ChampagneGold.copy(alpha = 0.18f) else LuxurySurface,
                                            border = BorderStroke(1.dp, if (tierSelected) ChampagneGold else LuxuryBorder),
                                            modifier = Modifier.clickable { onSelectTier(program, tier) }
                                        ) {
                                            Text(
                                                text = tier,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (tierSelected) ChampagneGold else TextSecondary,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (tierSelected) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                maxLines = 1,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                // Optional balance at link time — still optional; blank stays
                                // blank and the wallet will show "Tap to add balance" later.
                                OutlinedTextField(
                                    value = selectedBalances[program.id] ?: "",
                                    onValueChange = { onBalanceChange(program, it) },
                                    label = { Text("Balance (optional, ${program.defaultUnit})") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ChampagneGold,
                                        unfocusedBorderColor = LuxuryBorder,
                                        focusedContainerColor = LuxurySurface,
                                        unfocusedContainerColor = LuxurySurface,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                                        .testTag("loyalty_balance_input_${program.id}")
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Booking Priority — ordinal rank, scoped per category (rank only means something
        // relative to same-type programs). Move-up/move-down instead of drag-to-reorder: no new
        // dependency, no gesture complexity, and materially more accessible than drag.
        if (selectedPrograms.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = LuxuryBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "YOUR BOOKING PRIORITY",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = ChampagneGold,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "When more than one linked program could book the same trip, Marco follows this order — ranked separately within each category.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
            Spacer(modifier = Modifier.height(10.dp))

            val rankCategories = listOf(
                "AIRLINE" to "Airlines",
                "HOTEL" to "Hotels",
                "TIMESHARE" to "Timeshares & Vacation Clubs",
                "CREDIT_CARD" to "Credit Cards"
            )

            rankCategories.forEach { (catKey, catTitle) ->
                val programsInCategory = selectedPrograms.filter { it.categoryType == catKey }
                if (programsInCategory.isNotEmpty()) {
                    Text(
                        text = catTitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        programsInCategory.forEachIndexed { index, program ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = LuxuryCardElevated,
                                border = BorderStroke(1.dp, LuxuryBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Surface(
                                            shape = CircleShape,
                                            color = LuxurySurface,
                                            border = BorderStroke(1.dp, ChampagneGold)
                                        ) {
                                            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${index + 1}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = ChampagneGold,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = program.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextPrimary
                                                )
                                            )
                                            Text(
                                                text = (selectedTiers[program.id]?.ifBlank { null } ?: "Tier not set"),
                                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                                            )
                                        }
                                    }

                                    Row {
                                        IconButton(
                                            onClick = { onMoveRank(program, -1) },
                                            enabled = index > 0,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("rank_up_${program.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Move ${program.name} up in booking priority",
                                                tint = if (index > 0) ChampagneGold else TextMuted
                                            )
                                        }
                                        IconButton(
                                            onClick = { onMoveRank(program, 1) },
                                            enabled = index < programsInCategory.lastIndex,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("rank_down_${program.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Move ${program.name} down in booking priority",
                                                tint = if (index < programsInCategory.lastIndex) ChampagneGold else TextMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

// -------------------------------------------------------------
// STEP 5: PACE (WEIGHTED), COMFORT (WEIGHTED) & PASSPORT REVEAL
// -------------------------------------------------------------
@Composable
private fun Step5ComfortAndRevealSection(
    displayName: String,
    homeAirport: String,
    motivationLabel: String,
    travelStyleLabel: String,
    signatureAspiration: String,
    accessibilityOptIn: Boolean,
    onAccessibilityOptInChange: (Boolean) -> Unit,
    pacingWeights: Map<String, Int>,
    onPacingWeightChange: (title: String, weight: Int?) -> Unit,
    comfortWeights: Map<String, Int>,
    onComfortWeightChange: (label: String, weight: Int?) -> Unit,
    linkedProgramCount: Int
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Pace & Comfort",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        // Pacing — weighted
        Text(
            text = "Journey Pacing — rate each:",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PreferenceConstants.PACING_OPTIONS.forEach { (paceTitle, paceDesc) ->
                IntensityScaleRow(
                    label = paceTitle,
                    description = paceDesc,
                    weight = pacingWeights[paceTitle],
                    onWeightChange = { onPacingWeightChange(paceTitle, it) },
                    modifier = Modifier.testTag("pacing_weight_${paceTitle.hashCode()}")
                )
            }
        }

        // Accessibility Opt-In Gate
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = LuxuryCard,
            border = BorderStroke(1.dp, LuxuryBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Verify Accessibility Accommodations?",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Marco audits step-free elevators, sensory zones, and ADA transit on every venue.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                Switch(
                    checked = accessibilityOptIn,
                    onCheckedChange = onAccessibilityOptInChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ChampagneGold,
                        checkedTrackColor = LuxuryCardElevated,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = LuxurySurface
                    ),
                    modifier = Modifier.testTag("accessibility_opt_in_switch")
                )
            }
        }

        // Detail options if opted in — weighted, and the weight is meaningful downstream: a 7
        // reaches Gemini as a hard filter, a low score as a soft nice-to-have (not the same
        // "prefers" sentence for every score).
        AnimatedVisibility(visible = accessibilityOptIn) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Rate how essential each accommodation is — 7 means Marco must filter for it, 1 means it's nice if available.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                )

                PreferenceConstants.ACCESSIBILITY_COMFORT_OPTIONS.forEach { opt ->
                    IntensityScaleRow(
                        label = opt.label,
                        weight = comfortWeights[opt.label],
                        onWeightChange = { onComfortWeightChange(opt.label, it) },
                        modifier = Modifier.testTag("comfort_weight_${opt.label.hashCode()}")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interactive Traveler Passport Card Reveal
        Text(
            text = "YOUR TRAVELER PASSPORT PREVIEW:",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = ChampagneGold,
                letterSpacing = 1.sp
            )
        )

        // Preview must reflect exactly what will be saved — no invented name/home port/motive,
        // and each weighted field shows its current top-rated value (empty if nothing rated yet).
        val previewPref = UserPreferenceEntity(
            displayName = displayName,
            homeAirport = homeAirport,
            travelMotivation = motivationLabel,
            preferredTravelStyle = travelStyleLabel,
            pacingPreference = pacingWeights.maxByOrNull { it.value }?.key ?: "",
            signatureAspiration = signatureAspiration,
            accessibilityVerificationOptIn = accessibilityOptIn,
            totalTripsAnalyzed = 0
        )

        TravelerPassportCard(
            preference = previewPref,
            linkedProgramCount = linkedProgramCount,
            onRefineDnaClick = null
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// -------------------------------------------------------------
// CAMERAX + ML KIT OCR CARD SCANNER SHEET
// -------------------------------------------------------------
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun CameraCardScannerSheet(
    onCardDetected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var manualEntryText by remember { mutableStateOf("") }
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Scan Loyalty Card",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )
        Text(
            text = "Hold your loyalty card in frame to auto-detect program and tier status.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (cameraPermission.status.isGranted) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(LuxurySurface)
                    .border(1.dp, ChampagneGold, RoundedCornerShape(16.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        val executor = Executors.newSingleThreadExecutor()
                        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(executor) { imageProxy: ImageProxy ->
                                @SuppressLint("UnsafeOptInUsageError")
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    textRecognizer.process(image)
                                        .addOnSuccessListener { visionText ->
                                            if (visionText.text.isNotBlank()) {
                                                onCardDetected(visionText.text)
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    ctx as androidx.lifecycle.LifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (_: Exception) {}
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // Permission request or manual entry fallback
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = LuxurySurface,
                border = BorderStroke(1.dp, LuxuryBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Camera access required for live card scanning",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { cameraPermission.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LuxuryCardElevated,
                            contentColor = TextPrimary
                        ),
                        border = BorderStroke(1.dp, LuxuryBorder)
                    ) {
                        Text("Grant Camera Permission")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Manual Entry Fallback
        Text(
            text = "Or enter card name manually:",
            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = manualEntryText,
                onValueChange = { manualEntryText = it },
                placeholder = { Text("e.g. Delta SkyMiles Platinum") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (manualEntryText.isNotBlank()) {
                        onCardDetected(manualEntryText)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = LuxuryCardElevated,
                    contentColor = TextPrimary
                ),
                border = BorderStroke(1.dp, LuxuryBorder)
            ) {
                Text("Add")
            }
        }
    }
}
