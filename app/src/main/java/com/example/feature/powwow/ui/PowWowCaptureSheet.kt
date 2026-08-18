package com.example.feature.powwow.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.PowWowLimits
import com.example.data.model.PowWowSessionEntity
import com.example.data.model.TravelerAgeBand
import com.example.data.repository.ConsentStatus
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.LuxuryBorder
import com.example.ui.theme.LuxuryCard
import com.example.ui.theme.LuxuryCardElevated
import com.example.ui.theme.LuxuryDarkBase
import com.example.ui.theme.LuxurySurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

/**
 * Deterministic Pow Wow timed prompts.
 */
val POW_WOW_PROMPT_RAIL = listOf(
    "1. Who is going on this trip?",
    "2. When are you thinking of traveling?",
    "3. Why this trip right now?",
    "4. What are your absolute non-negotiables & must-haves?",
    "5. What worries you or might become a point of friction?",
    "6. What is your target budget and comfort feel?"
)

/**
 * Full Pow Wow audio capture interface with consent gate, 6-prompt rail, and waveform visualizer.
 */
@Composable
fun PowWowCaptureSheet(
    session: PowWowSessionEntity,
    travelerAgeBand: TravelerAgeBand,
    consentStatus: ConsentStatus,
    isRecording: Boolean,
    durationTenths: Int,
    amplitude: Int,
    currentPromptIndex: Int,
    onGrantConsent: (guardianId: Long) -> Unit,
    onStartRecording: () -> Unit,
    onAdvancePrompt: () -> Unit,
    onFinishRecording: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val durationSec = durationTenths / 10
    val canFinish = durationSec >= PowWowLimits.MIN_DURATION_SECONDS
    val isChild = travelerAgeBand == TravelerAgeBand.CHILD

    Dialog(onDismissRequest = onDiscard) {
        Surface(
            color = LuxurySurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, LuxuryBorder),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "POW WOW AUDIO RITUAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ChampagneGold,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Text(
                            text = if (isRecording) "Recording Brain Dump" else "Session Setup",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    IconButton(
                        onClick = onDiscard,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(LuxuryCardElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Phase 1: Consent Gate
                if (consentStatus !is ConsentStatus.Granted) {
                    ConsentGateView(
                        isChild = isChild,
                        onGrantConsent = { onGrantConsent(if (isChild) 1L else 0L) }
                    )
                } else if (!isRecording) {
                    // Ready to Record
                    ReadyToRecordView(onStartRecording = onStartRecording)
                } else {
                    // Phase 2: Active Recording & Prompt Rail
                    ActiveRecordingView(
                        durationSec = durationSec,
                        amplitude = amplitude,
                        promptIndex = currentPromptIndex,
                        canFinish = canFinish,
                        onAdvancePrompt = onAdvancePrompt,
                        onFinishRecording = onFinishRecording
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsentGateView(
    isChild: Boolean,
    onGrantConsent: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LuxuryCard),
        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Privacy",
                tint = ChampagneGold,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isChild) "Guardian Consent Required" else "Audio Privacy Guarantee",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isChild) {
                    "Under-14 traveler capture requires guardian consent. Raw audio is transcribed into text and immediately purged from the device. Audio is never uploaded or shared."
                } else {
                    "Your brain dump is private. Audio is held only in temporary memory during recording, transcribed on-device, and permanently deleted immediately after transcription."
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onGrantConsent,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChampagneGold,
                    contentColor = LuxuryDarkBase
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isChild) "Guardian Grants Consent" else "I Consent & Agree",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ReadyToRecordView(
    onStartRecording: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Speak freely for 30–300 seconds. Marco will present 6 guiding prompts to elicit your genuine desires, non-negotiables, and worries.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(ChampagneGold)
                .clickable { onStartRecording() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Start Recording",
                tint = LuxuryDarkBase,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Tap to Begin Recording",
            style = MaterialTheme.typography.labelMedium.copy(
                color = ChampagneGold,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun ActiveRecordingView(
    durationSec: Int,
    amplitude: Int,
    promptIndex: Int,
    canFinish: Boolean,
    onAdvancePrompt: () -> Unit,
    onFinishRecording: () -> Unit
) {
    val minLeft = maxOf(0, PowWowLimits.MIN_DURATION_SECONDS - durationSec)
    val maxLeft = maxOf(0, PowWowLimits.MAX_DURATION_SECONDS - durationSec)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Timer
        val minutes = durationSec / 60
        val seconds = durationSec % 60
        Text(
            text = String.format(Locale.US, "%02d:%02d", minutes, seconds),
            style = MaterialTheme.typography.headlineLarge.copy(
                color = ChampagneGold,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        )

        Text(
            text = if (minLeft > 0) "Minimum remaining: ${minLeft}s" else "Time remaining: ${maxLeft}s",
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (minLeft > 0) TextMuted else TextSecondary,
                fontFamily = FontFamily.Monospace
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Prompt Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = LuxuryCard),
            border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "PROMPT ${promptIndex + 1} OF 6",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ChampagneGold,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = POW_WOW_PROMPT_RAIL.getOrElse(promptIndex) { POW_WOW_PROMPT_RAIL.last() },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Waveform / Mic Indicator
        Box(
            modifier = Modifier
                .size(64.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(ChampagneGold.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Recording",
                tint = ChampagneGold,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (promptIndex < 5) {
                OutlinedButton(
                    onClick = onAdvancePrompt,
                    border = BorderStroke(1.dp, LuxuryBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Text("Next Prompt", fontSize = 12.sp)
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Button(
                onClick = onFinishRecording,
                enabled = canFinish,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChampagneGold,
                    contentColor = LuxuryDarkBase
                )
            ) {
                Icon(Icons.Default.Stop, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (canFinish) "Finish & Transcribe" else "Min 30s Required",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
