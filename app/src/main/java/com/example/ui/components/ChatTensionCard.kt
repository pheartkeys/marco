package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BriefResolution
import com.example.data.model.BriefTension
import com.example.data.model.TravelerEntity
import com.example.ui.theme.*

/**
 * Chat stream card for a named planning tension with conflicting stances and DNA evidence (CARD_TENSION).
 * Presents member viewpoints clearly and offers an AI-synthesized win-win resolution.
 */
@Composable
fun ChatTensionCard(
    tension: BriefTension,
    resolution: BriefResolution? = null,
    travelersById: Map<Long, TravelerEntity> = emptyMap(),
    onAcceptResolution: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = LuxuryCard),
        border = BorderStroke(1.dp, GoldenSun.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpeditionBadge(
                    text = "Finding The Sweet Spot",
                    color = GoldenSunLight,
                    icon = Icons.Default.TipsAndUpdates,
                    isFilled = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = tension.topic,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            )

            if (tension.stakes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Goal: ${tension.stakes}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Member Positions
            tension.positions.forEach { position ->
                val travelerName = travelersById[position.travelerId]?.displayName ?: "Member #${position.travelerId}"
                Surface(
                    color = LuxurySurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, LuxuryBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = travelerName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = GoldenSunLight,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            if (position.dnaEvidence.isNotBlank()) {
                                Text(
                                    text = position.dnaEvidence,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = position.stance,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            }

            // Proposed Win-Win Resolution
            if (resolution != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = PalmEmerald.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, PalmEmerald.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PalmEmeraldLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "✨ AI WIN-WIN COMPROMISE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = PalmEmeraldLight,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = resolution.proposal,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 18.sp
                            )
                        )
                        if (resolution.state != "ACCEPTED") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onAcceptResolution,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PalmEmerald,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🤝 Adopt Compromise & Plan", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
