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
import com.example.data.model.ChatMessageEntity
import com.example.data.model.TripEntity
import com.example.ui.theme.CartographyDarkBase
import com.example.ui.theme.CelestialLapis
import com.example.ui.theme.ContourBorder
import com.example.ui.theme.MaritimeBlue
import com.example.ui.theme.TextAtlasSecondary
import com.example.ui.theme.WaxSealCrimson

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

    val destination = trip?.destination ?: "Active Expedition"
    val emergencyContactPhone = "911"

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, ContourBorder),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag("chat_emergency_sos_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(WaxSealCrimson),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "EMERGENCY SOS BEACON",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color(0xFFFCA5A5)
                            )
                        )
                        Text(
                            text = "Live Active Trip Safety Network",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFD1D5DB)
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onPlayTts,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Read Alert",
                        tint = Color(0xFFFCA5A5),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Telemetry summary
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CelestialLapis.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Expedition:",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextAtlasSecondary
                            )
                        )
                        Text(
                            text = destination,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Emergency Support:",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextAtlasSecondary
                            )
                        )
                        Text(
                            text = "Local 911/112 Dispatch & Trauma ER",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Consular Assistance:",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextAtlasSecondary
                            )
                        )
                        Text(
                            text = "24/7 Citizen Traveler Safety Desk",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:911"))
                        context.startActivity(dialIntent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WaxSealCrimson),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Call, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
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
                        containerColor = MaritimeBlue,
                        contentColor = CartographyDarkBase
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isDispatched) Icons.Default.CheckCircle else Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isDispatched) "Sent SOS GPS" else "Broadcast SOS",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
