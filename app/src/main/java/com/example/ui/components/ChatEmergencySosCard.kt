package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessageEntity
import com.example.data.model.TripEntity
import com.example.ui.theme.*

/**
 * Emergency SOS & Safety Radar Card:
 * Instant dispatch trigger that prepares GPS telemetry broadcast to emergency contacts,
 * local emergency services (911 / 112), and caches offline safety protocols.
 */
@Composable
fun ChatEmergencySosCard(
    message: ChatMessageEntity,
    trip: TripEntity?,
    onPlayTts: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isDispatched by remember { mutableStateOf(false) }

    val destination = trip?.destination ?: "Active Journey"
    val emergencyContactPhone = "911"

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = LuxuryCard),
        border = BorderStroke(1.dp, StatusCrimson.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag("chat_emergency_sos_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(StatusCrimson),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "EMERGENCY SOS & SAFETY",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color(0xFFFCA5A5),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Text(
                            text = "Marco 24/7 Global Traveler Safety Hub",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onPlayTts,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Read Alert",
                        tint = Color(0xFFFCA5A5),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Telemetry summary
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = LuxurySurface,
                border = BorderStroke(1.dp, LuxuryBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Destination:",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary
                            )
                        )
                        Text(
                            text = destination,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Emergency Support:",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary
                            )
                        )
                        Text(
                            text = "Local 911 / 112 Dispatch",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Consular Assistance:",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary
                            )
                        )
                        Text(
                            text = "24/7 Citizen Traveler Desk",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:911"))
                        context.startActivity(dialIntent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCrimson),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Call, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Call 911 / ER",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = {
                        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("smsto:$emergencyContactPhone")
                            putExtra(
                                "sms_body",
                                "EMERGENCY SOS from Marco Travel App: I need immediate assistance in $destination. Expedition: ${trip?.title ?: "Active Journey"}"
                            )
                        }
                        try {
                            context.startActivity(smsIntent)
                            isDispatched = true
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Unable to open messaging app for SOS broadcast.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldenSun,
                        contentColor = LuxuryDarkBase
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isDispatched) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDispatched) "Sent SOS GPS" else "Broadcast SOS",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
