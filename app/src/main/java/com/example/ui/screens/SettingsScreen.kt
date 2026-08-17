package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppSettingsState
import com.example.data.local.AvailableVoiceProfiles
import com.example.data.local.VoiceProfile
import com.example.data.sync.CloudSyncManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CelestialLapis
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.ParchmentSand
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TealAccent
import com.example.ui.theme.VenetianGold
import com.example.ui.theme.VenetianGoldDeep
import com.example.ui.theme.VenetianGoldLight
import com.example.viewmodel.TravelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TravelViewModel,
    onNavigateToAuth: () -> Unit = {},
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)
    val context = LocalContext.current
    val settingsState by viewModel.settingsState.collectAsState()

    // Local form state for draft changes
    var geminiKey by remember(settingsState.customGeminiApiKey) { mutableStateOf(settingsState.customGeminiApiKey) }
    var openAiKey by remember(settingsState.customOpenAiApiKey) { mutableStateOf(settingsState.customOpenAiApiKey) }
    var anthropicKey by remember(settingsState.customAnthropicApiKey) { mutableStateOf(settingsState.customAnthropicApiKey) }
    var showGeminiKey by remember { mutableStateOf(false) }
    var showOpenAiKey by remember { mutableStateOf(false) }
    var showAnthropicKey by remember { mutableStateOf(false) }

    var selectedVoiceId by remember(settingsState.selectedVoiceId) { mutableStateOf(settingsState.selectedVoiceId) }
    var voiceSpeed by remember(settingsState.voiceSpeed) { mutableStateOf(settingsState.voiceSpeed) }
    var voicePitch by remember(settingsState.voicePitch) { mutableStateOf(settingsState.voicePitch) }
    var autoPlayVoice by remember(settingsState.autoPlayVoiceReplies) { mutableStateOf(settingsState.autoPlayVoiceReplies) }

    var selectedModel by remember(settingsState.defaultAiModel) { mutableStateOf(settingsState.defaultAiModel) }
    var temperature by remember(settingsState.temperature) { mutableStateOf(settingsState.temperature) }

    var unitsSystem by remember(settingsState.unitsSystem) { mutableStateOf(settingsState.unitsSystem) }
    var currency by remember(settingsState.defaultCurrency) { mutableStateOf(settingsState.defaultCurrency) }
    var offlineCache by remember(settingsState.offlineCacheEnabled) { mutableStateOf(settingsState.offlineCacheEnabled) }
    var autoEmergencySync by remember(settingsState.autoSyncEmergencyAlerts) { mutableStateOf(settingsState.autoSyncEmergencyAlerts) }
    var reducedMotion by remember(settingsState.sensoryReducedMotion) { mutableStateOf(settingsState.sensoryReducedMotion) }
    var hapticFeedback by remember(settingsState.hapticFeedbackEnabled) { mutableStateOf(settingsState.hapticFeedbackEnabled) }
    var dynamicRebook by remember(settingsState.dynamicWeatherRebooking) { mutableStateOf(settingsState.dynamicWeatherRebooking) }

    var isModelDropdownExpanded by remember { mutableStateOf(false) }
    var isCurrencyDropdownExpanded by remember { mutableStateOf(false) }
    var isUnitsDropdownExpanded by remember { mutableStateOf(false) }

    var isSavedNotificationVisible by remember { mutableStateOf(false) }

    // Firebase Auth & Cloud Sync state
    val firebaseUser by viewModel.firebaseUser.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsState()

    var isSyncingNow by remember { mutableStateOf(false) }

    val availableModels = listOf(
        "gemini-3.5-flash" to "Gemini 3.5 Flash (Fastest, Recommended)",
        "gemini-3.1-pro" to "Gemini 3.1 Pro (Deepest Itinerary Reasoning)",
        "gemini-2.5-flash" to "Gemini 2.5 Flash (High Throughput)",
        "openai-gpt-4o" to "OpenAI GPT-4o (BYOK Required)",
        "claude-3-5-sonnet" to "Claude 3.5 Sonnet (BYOK Required)"
    )

    val availableCurrencies = listOf("USD ($)", "EUR (€)", "GBP (£)", "JPY (¥)", "CAD ($)", "AUD ($)", "CHF (Fr)")
    val availableUnits = listOf("Imperial (mi, °F, lbs)", "Metric (km, °C, kg)")

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(CelestialLapis, VenetianGoldDeep))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = VenetianGoldLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Settings & Voice Concierge",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                letterSpacing = 0.3.sp
                            )
                        )
                        Text(
                            text = "API Keys • Explorer Voices • System Preferences",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Button(
                        onClick = {
                            val updated = AppSettingsState(
                                customGeminiApiKey = geminiKey.trim(),
                                customOpenAiApiKey = openAiKey.trim(),
                                customAnthropicApiKey = anthropicKey.trim(),
                                selectedVoiceId = selectedVoiceId,
                                voiceSpeed = voiceSpeed,
                                voicePitch = voicePitch,
                                autoPlayVoiceReplies = autoPlayVoice,
                                defaultAiModel = selectedModel,
                                temperature = temperature,
                                unitsSystem = unitsSystem,
                                defaultCurrency = currency.substringBefore(" ").trim(),
                                offlineCacheEnabled = offlineCache,
                                autoSyncEmergencyAlerts = autoEmergencySync,
                                sensoryReducedMotion = reducedMotion,
                                hapticFeedbackEnabled = hapticFeedback,
                                dynamicWeatherRebooking = dynamicRebook
                            )
                            viewModel.saveSettings(updated)
                            Toast.makeText(context, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
                            isSavedNotificationVisible = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VenetianGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_settings_top_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }

            // ==========================================
            // SECTION 0: FIREBASE CLOUD SYNC & ACCOUNT
            // ==========================================
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CelestialLapis.copy(alpha = 0.08f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CelestialLapis.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("firebase_sync_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(CelestialLapis.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = CelestialLapis,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Cloud Sync & Account",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "App ID: go-marco • com.go.marco",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = CelestialLapis,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }

                            // Connection Badge
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (firebaseUser != null) EmeraldGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (firebaseUser != null) EmeraldGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (firebaseUser != null) EmeraldGreen else Color.Gray)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (firebaseUser != null) "Connected" else "Offline Local",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (firebaseUser != null) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (firebaseUser != null) {
                            // Logged In State UI
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountCircle,
                                                contentDescription = null,
                                                tint = VenetianGold,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = firebaseUser?.displayName ?: firebaseUser?.email ?: "Authenticated Traveler",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp
                                                )
                                                if (firebaseUser?.displayName != null && firebaseUser?.email != null) {
                                                    Text(
                                                        text = firebaseUser?.email ?: "",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text(
                                                    text = "UID: ${firebaseUser?.uid?.take(12)}...",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        TextButton(
                                            onClick = {
                                                viewModel.signOutFirebase()
                                                Toast.makeText(context, "Signed out of Firebase", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(14.dp), tint = SunsetCoral)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Sign Out", color = SunsetCoral, fontSize = 11.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Firestore Sync: Ready",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            val lastTimeFormatted = remember(lastSyncTimestamp) {
                                                val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                                                sdf.format(Date(lastSyncTimestamp))
                                            }
                                            Text(
                                                text = "Last synced: $lastTimeFormatted",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                isSyncingNow = true
                                                viewModel.syncAllToFirebase { success ->
                                                    isSyncingNow = false
                                                    Toast.makeText(
                                                        context,
                                                        if (success) "Synced all itineraries & wallet to Firestore!" else "Sync completed with offline cache",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            enabled = !isSyncingNow,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = CelestialLapis),
                                            modifier = Modifier.testTag("sync_to_firestore_button")
                                        ) {
                                            if (isSyncingNow) {
                                                CircularProgressIndicator(
                                                    color = Color.White,
                                                    modifier = Modifier.size(14.dp),
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Syncing...", fontSize = 11.sp, color = Color.White)
                                            } else {
                                                Icon(Icons.Default.Sync, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Sync All Data", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Signed Out Clean View with Link to Dedicated Auth Screens
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Cloud Account Not Connected",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Sign in to back up your custom itineraries, sync wallet points across devices, and share memories.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = onNavigateToAuth,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CelestialLapis,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("settings_open_auth_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            tint = VenetianGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Sign In or Register",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = VenetianGoldLight
                                        )
                                    }
                                }
                            }
                        }

                        syncMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = msg,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ==========================================
            // SECTION 1: AI API KEYS & CUSTOM PROVIDERS
            // ==========================================
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(VenetianGold.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = VenetianGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AI Engine & Custom API Keys",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Bring Your Own Key (BYOK) for unlimited itineraries & custom quotas",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Gemini API Key Input
                        Text(
                            text = "Google Gemini API Key (Recommended)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = VenetianGold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = geminiKey,
                            onValueChange = { geminiKey = it },
                            placeholder = { Text("AIzaSy... (leave blank to use default built-in key)") },
                            visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                                    Icon(
                                        imageVector = if (showGeminiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Visibility"
                                    )
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gemini_api_key_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VenetianGold,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // OpenAI API Key Input
                        Text(
                            text = "OpenAI API Key (Optional / BYOK)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = openAiKey,
                            onValueChange = { openAiKey = it },
                            placeholder = { Text("sk-proj-... (optional for GPT-4o models)") },
                            visualTransformation = if (showOpenAiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showOpenAiKey = !showOpenAiKey }) {
                                    Icon(
                                        imageVector = if (showOpenAiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Visibility"
                                    )
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("openai_api_key_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OceanBlue,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Anthropic API Key Input
                        Text(
                            text = "Anthropic Claude API Key (Optional / BYOK)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = anthropicKey,
                            onValueChange = { anthropicKey = it },
                            placeholder = { Text("sk-ant-... (optional for Claude models)") },
                            visualTransformation = if (showAnthropicKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showAnthropicKey = !showAnthropicKey }) {
                                    Icon(
                                        imageVector = if (showAnthropicKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Visibility"
                                    )
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("anthropic_api_key_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SunsetCoral,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Keys are securely encrypted and stored locally in on-device keystore.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ==========================================
            // SECTION 2: CONCIERGE VOICE PERSONAS & TTS
            // ==========================================
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(OceanBlue.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = OceanBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Voice Concierge & Speech Synthesis",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Choose your AI travel narrator, pacing, and speech characteristics",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Voice Selection Cards
                        Text(
                            text = "Select Narrator Voice Profile:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        AvailableVoiceProfiles.forEach { profile ->
                            val isSelected = selectedVoiceId == profile.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) VenetianGold.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, VenetianGold) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        selectedVoiceId = profile.id
                                        viewModel.previewVoiceSample(profile.id, voiceSpeed, voicePitch)
                                    }
                                    .testTag("voice_profile_${profile.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = profile.iconEmoji,
                                        fontSize = 24.sp,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = profile.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (isSelected) VenetianGold else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = VenetianGold,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = profile.epithet,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = profile.description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            lineHeight = 14.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            selectedVoiceId = profile.id
                                            viewModel.previewVoiceSample(profile.id, voiceSpeed, voicePitch)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Sample Voice",
                                            tint = if (isSelected) VenetianGold else OceanBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Voice Speed Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Speech Speed: ${String.format("%.1fx", voiceSpeed)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            TextButton(onClick = { voiceSpeed = 1.0f }) {
                                Text("Reset (1.0x)", fontSize = 11.sp)
                            }
                        }
                        Slider(
                            value = voiceSpeed,
                            onValueChange = { voiceSpeed = it },
                            valueRange = 0.5f..2.0f,
                            steps = 14,
                            colors = SliderDefaults.colors(
                                thumbColor = VenetianGold,
                                activeTrackColor = VenetianGold
                            ),
                            modifier = Modifier.testTag("voice_speed_slider")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Voice Pitch Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Voice Pitch Tone: ${String.format("%.1fx", voicePitch)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            TextButton(onClick = { voicePitch = 1.0f }) {
                                Text("Natural (1.0x)", fontSize = 11.sp)
                            }
                        }
                        Slider(
                            value = voicePitch,
                            onValueChange = { voicePitch = it },
                            valueRange = 0.6f..1.6f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = OceanBlue,
                                activeTrackColor = OceanBlue
                            ),
                            modifier = Modifier.testTag("voice_pitch_slider")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Autoplay Voice Responses Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Read Concierge Responses",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Automatically speak incoming chat suggestions and vendor updates",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoPlayVoice,
                                onCheckedChange = { autoPlayVoice = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = VenetianGold, checkedTrackColor = VenetianGold.copy(alpha = 0.4f)),
                                modifier = Modifier.testTag("auto_voice_toggle")
                            )
                        }
                    }
                }
            }

            // ==========================================
            // SECTION 3: AI REASONING & MODEL CONFIG
            // ==========================================
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(SunsetCoral.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = SunsetCoral,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AI Model & Reasoning Engine",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Select model temperature, creative exploration, and reasoning mode",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Default Model Selector Dropdown
                        Text(
                            text = "Active Generation Model:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isModelDropdownExpanded = true }
                                    .testTag("model_selector_dropdown")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = availableModels.firstOrNull { it.first == selectedModel }?.second ?: selectedModel,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Text("▼", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            DropdownMenu(
                                expanded = isModelDropdownExpanded,
                                onDismissRequest = { isModelDropdownExpanded = false }
                            ) {
                                availableModels.forEach { (modelId, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(modelId, fontSize = 11.sp, color = Color.Gray)
                                            }
                                        },
                                        onClick = {
                                            selectedModel = modelId
                                            isModelDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Temperature Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Creativity / Temperature: ${String.format("%.2f", temperature)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = if (temperature < 0.4f) "Strict & Deterministic" else if (temperature > 0.8f) "High Discovery" else "Balanced",
                                fontSize = 11.sp,
                                color = VenetianGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = temperature,
                            onValueChange = { temperature = it },
                            valueRange = 0.1f..1.2f,
                            steps = 10,
                            colors = SliderDefaults.colors(
                                thumbColor = SunsetCoral,
                                activeTrackColor = SunsetCoral
                            ),
                            modifier = Modifier.testTag("temperature_slider")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dynamic Weather & Crowd Rebooking Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dynamic Weather & Storm Rebooking",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Automatically monitor live precipitation, surf, and road closures to propose instant indoor alternatives",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = dynamicRebook,
                                onCheckedChange = { dynamicRebook = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreen, checkedTrackColor = EmeraldGreen.copy(alpha = 0.4f)),
                                modifier = Modifier.testTag("dynamic_rebooking_toggle")
                            )
                        }
                    }
                }
            }

            // ==========================================
            // SECTION 4: LOCALIZATION, UNITS & ACCESSIBILITY
            // ==========================================
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(TealAccent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = null,
                                    tint = TealAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Regional, Currency & Accessibility",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Measurement systems, currency display, and sensory comfort",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Default Currency
                        Text(
                            text = "Base Currency:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isCurrencyDropdownExpanded = true }
                                    .testTag("currency_selector_dropdown")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(currency, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("▼", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            DropdownMenu(
                                expanded = isCurrencyDropdownExpanded,
                                onDismissRequest = { isCurrencyDropdownExpanded = false }
                            ) {
                                availableCurrencies.forEach { curr ->
                                    DropdownMenuItem(
                                        text = { Text(curr, fontSize = 13.sp) },
                                        onClick = {
                                            currency = curr
                                            isCurrencyDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Units of Measurement
                        Text(
                            text = "Units of Measurement:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isUnitsDropdownExpanded = true }
                                    .testTag("units_selector_dropdown")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(unitsSystem, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("▼", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            DropdownMenu(
                                expanded = isUnitsDropdownExpanded,
                                onDismissRequest = { isUnitsDropdownExpanded = false }
                            ) {
                                availableUnits.forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text(unit, fontSize = 13.sp) },
                                        onClick = {
                                            unitsSystem = unit
                                            isUnitsDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Offline Room Cache Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Offline SQLite & Map Caching",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Store all itineraries, confirmation vouchers, and offline emergency hospital maps on device",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = offlineCache,
                                onCheckedChange = { offlineCache = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreen, checkedTrackColor = EmeraldGreen.copy(alpha = 0.4f)),
                                modifier = Modifier.testTag("offline_cache_toggle")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sensory Reduced Motion Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sensory Friendly: Reduced Animations",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Softens astrolabe and caravel rotational motion for sensory comfort",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = reducedMotion,
                                onCheckedChange = { reducedMotion = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = VenetianGold, checkedTrackColor = VenetianGold.copy(alpha = 0.4f)),
                                modifier = Modifier.testTag("reduced_motion_toggle")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Haptic Feedback Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Haptic Vibration Feedback",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Tactile confirmations for concierge dispatch and rebooking",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = hapticFeedback,
                                onCheckedChange = { hapticFeedback = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = OceanBlue, checkedTrackColor = OceanBlue.copy(alpha = 0.4f)),
                                modifier = Modifier.testTag("haptic_toggle")
                            )
                        }
                    }
                }
            }

            // ==========================================
            // SECTION 5: DATA MANAGEMENT & RESET
            // ==========================================
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Data Management & Keystore",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.clearAllLocalData()
                                    Toast.makeText(context, "Local trips, chats, and cache cleared!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Local Data", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val updated = AppSettingsState(
                                        customGeminiApiKey = geminiKey.trim(),
                                        customOpenAiApiKey = openAiKey.trim(),
                                        customAnthropicApiKey = anthropicKey.trim(),
                                        selectedVoiceId = selectedVoiceId,
                                        voiceSpeed = voiceSpeed,
                                        voicePitch = voicePitch,
                                        autoPlayVoiceReplies = autoPlayVoice,
                                        defaultAiModel = selectedModel,
                                        temperature = temperature,
                                        unitsSystem = unitsSystem,
                                        defaultCurrency = currency.substringBefore(" ").trim(),
                                        offlineCacheEnabled = offlineCache,
                                        autoSyncEmergencyAlerts = autoEmergencySync,
                                        sensoryReducedMotion = reducedMotion,
                                        hapticFeedbackEnabled = hapticFeedback,
                                        dynamicWeatherRebooking = dynamicRebook
                                    )
                                    viewModel.saveSettings(updated)
                                    Toast.makeText(context, "All configuration saved!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VenetianGold),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Save, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Marco Travel Concierge v2.4.0 • Android M3 Edition • Offline-Ready Room Database",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
