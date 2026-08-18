package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TripBriefEntity
import com.example.data.model.TripBriefPayloads
import com.example.ui.theme.*

/**
 * Chat stream card for synthesized Pow Wow Trip Brief (CARD_POW_WOW_BRIEF).
 * Highlights shared alignment between travel crew members and readiness checklist.
 */
@Composable
fun ChatPowWowBriefCard(
    brief: TripBriefEntity,
    onAcceptBrief: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val agreements = TripBriefPayloads.decodeAgreements(brief.agreementsJson)
    val readiness = TripBriefPayloads.decodeReadiness(brief.readinessJson)
    val isAccepted = brief.acceptedAtTimestamp > 0L

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = LuxuryCard),
        border = BorderStroke(1.dp, MarcoCoral.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    ExpeditionBadge(
                        text = "Vacation Alignment Brief",
                        color = MarcoCoral,
                        isFilled = true
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Crew Consensus v${brief.version}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    )
                }

                Surface(
                    color = if (isAccepted) PalmEmerald.copy(alpha = 0.2f) else LuxuryCardElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isAccepted) PalmEmerald else LuxuryBorder)
                ) {
                    Text(
                        text = if (isAccepted) "✓ ALIGNED" else "IN REVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isAccepted) PalmEmeraldLight else GoldenSunLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }

            if (brief.summaryText.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = brief.summaryText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                )
            }

            // Shared Alignments
            if (agreements.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "SHARED VIBES & ALIGNMENTS (${agreements.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = GoldenSunLight,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                agreements.forEach { agreement ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = LuxurySurface,
                        border = BorderStroke(1.dp, LuxuryBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Agreed",
                                tint = PalmEmeraldLight,
                                modifier = Modifier
                                    .size(17.dp)
                                    .padding(top = 1.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = agreement.statement,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 17.sp
                                )
                            )
                        }
                    }
                }
            }

            // Expedition Readiness Checklist
            if (readiness.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "EXPEDITION READINESS CHECKLIST",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                readiness.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LuxurySurface,
                        border = BorderStroke(1.dp, LuxuryBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (item.isSatisfied) Icons.Default.Check else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (item.isSatisfied) PalmEmeraldLight else GoldenSunLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (item.isSatisfied) TextPrimary else TextSecondary,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                )
                                if (item.detail.isNotBlank()) {
                                    Text(
                                        text = item.detail,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextMuted,
                                            fontSize = 10.5.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Accept CTA
            if (!isAccepted) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAcceptBrief,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MarcoCoral,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🚀 Lock In Vacation Alignment", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
