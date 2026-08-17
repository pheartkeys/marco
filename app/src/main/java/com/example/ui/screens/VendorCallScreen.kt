package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.VendorCallLogEntity
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.LiveAudioWaveform
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CartographyCardElevated
import com.example.ui.theme.ContourBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.Navy900
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.SkyBlueLight
import com.example.ui.theme.SunsetCoral
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.ui.theme.TealAccent
import com.example.viewmodel.TravelViewModel

@Composable
fun VendorCallScreen(
    viewModel: TravelViewModel,
    initialVendor: String = "Lodging Front Desk",
    initialQuestion: String = "Verify check-in time, pool hours, and ADA accessible accommodations",
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    BackHandler(onBack = onNavigateBack)
    val isCallActive by viewModel.isVoiceCallActive.collectAsState()
    val callProgressStep by viewModel.activeCallProgressStep.collectAsState()
    val callLogs by viewModel.vendorCalls.collectAsState()

    var vendorName by remember { mutableStateOf(initialVendor) }
    var vendorCategory by remember { mutableStateOf("HOTEL") }
    var inquiryTopic by remember { mutableStateOf(initialQuestion) }
    var reservationRef by remember { mutableStateOf("") }

    val presetVendors = listOf(
        Pair("Hotel / Villa Front Desk", "HOTEL"),
        Pair("Airline Priority Reservations", "FLIGHT"),
        Pair("Car Rental / Mobility Transit", "TRANSIT"),
        Pair("National Park / Campgrounds Office", "CAMPGROUND"),
        Pair("Restaurant Concierge Desk", "DINING")
    )

    val presetQuestions = listOf(
        "Is the heated swimming pool open and is there ADA ramp/lift access?",
        "Can we reserve toddler booster seats and stroller storage for our arrival?",
        "Are 50-amp RV hookups and potable water stations open at the campground?",
        "Can we guarantee adjoining quiet rooms with early check-in?",
        "What are the baggage drop cutoff times and priority lane access for families?"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Top Navigation Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("vendor_back_button")
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
                        text = "Vendor Concierge & Telephony",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Automated AI Calls, Bookings & ADA Inquiries",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        // Hero Studio Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CartographyCardElevated)
                    .border(1.dp, ContourBorder, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AmberGold.copy(alpha = 0.25f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PhoneInTalk, null, tint = AmberGold, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI Voice Telephony Agent",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = AmberGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Vendor Voice Calling Studio",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your AI travel agent directly phones hotels, airlines, and campsites to verify amenities, pool status, and family accessibility.",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        // Active Call Live Simulation Box (if active)
        if (isCallActive) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy900),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(SunsetCoral)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Live outbound call",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = SunsetCoral,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = vendorName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Inquiry: $inquiryTopic",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = SkyBlueLight
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        LiveAudioWaveform(modifier = Modifier.padding(vertical = 8.dp), barCount = 11)

                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = AmberGold,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = callProgressStep,
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                                )
                            }
                        }
                    }
                }
            }
        }

        // New Dispatch Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, ContourBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Configure Outbound Voice Inquiry",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset Vendors chips
                    Text("Quick Select Vendor:", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        presetVendors.forEach { (name, cat) ->
                            val isSelected = vendorName == name
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    vendorName = name
                                    vendorCategory = cat
                                },
                                label = { Text(name) },
                                leadingIcon = {
                                    CategoryIconBadge(category = cat, size = 20)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OceanBlue.copy(alpha = 0.18f),
                                    selectedLabelColor = SkyBlueLight
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = vendorName,
                        onValueChange = { vendorName = it },
                        label = { Text("Vendor Name & Department") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Inquiry Topic
                    Text("Select or customize inquiry:", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Spacer(modifier = Modifier.height(8.dp))
                    presetQuestions.take(3).forEach { q ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { inquiryTopic = q }
                        ) {
                            Text(
                                text = "• $q",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inquiryTopic,
                        onValueChange = { inquiryTopic = it },
                        label = { Text("Custom Question for AI Agent to Ask") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.triggerVendorVoiceCall(
                                vendorName = vendorName,
                                vendorCategory = vendorCategory,
                                inquiryTopic = inquiryTopic,
                                reservationRef = reservationRef
                            )
                        },
                        enabled = !isCallActive && vendorName.isNotBlank() && inquiryTopic.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("dispatch_call_button")
                    ) {
                        Icon(imageVector = Icons.Default.PhoneInTalk, contentDescription = null, tint = Navy900)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dispatch AI Agent to Call Vendor",
                            color = Navy900,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Call Logs Header
        item {
            Text(
                text = "Verified Call Logs & Transcripts (${callLogs.size})",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }

        // Call Log items
        items(callLogs, key = { it.id }) { log ->
            CallLogCard(
                log = log,
                onListen = { viewModel.playAudioTranscript(log.audioTranscript) }
            )
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun CallLogCard(
    log: VendorCallLogEntity,
    onListen: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, ContourBorder),
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
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmeraldGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = log.vendorName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = log.vendorPhone,
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Confirmed",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Inquiry: ${log.inquiryTopic}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = SkyBlueLight
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Outcome: ${log.callSummaryOutcome}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                    if (log.confirmedDetails.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Details: ${log.confirmedDetails}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TealAccent
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Expand Transcript & Listen Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable { isExpanded = !isExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "Hide Full Transcript" else "View Audio Transcript",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SkyBlueLight,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = SkyBlueLight,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onListen,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Listen to Recording",
                        tint = AmberGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Navy900,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = log.audioTranscript,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}
