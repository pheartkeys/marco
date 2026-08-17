package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ChatMessageEntity
import com.example.data.model.TripActivityEntity
import com.example.data.model.TripEntity
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.LuxuryBorder
import com.example.ui.theme.LuxuryCard
import com.example.ui.theme.LuxuryCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ChatVintageParchmentMapCard(
    message: ChatMessageEntity,
    trip: TripEntity?,
    activities: List<TripActivityEntity>,
    onPlayTts: () -> Unit = {},
    modifier: Modifier = Modifier,
    onOpenFullSafetyMap: () -> Unit = {}
) {
    val destination = trip?.destination ?: "Trip Region"

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = LuxuryCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, LuxuryBorder),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag("chat_vintage_parchment_map_card")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(LuxuryCardElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = ChampagneGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Route & Map Waypoints",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = destination,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary
                            )
                        )
                    }
                }

                IconButton(onClick = onPlayTts) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Listen",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Minimalist Dark Route Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0A0A0A))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Grid Lines
                    val gridColor = Color(0xFF1C1C1C)
                    for (x in 0..4) {
                        drawLine(
                            color = gridColor,
                            start = Offset(w * x / 4f, 0f),
                            end = Offset(w * x / 4f, h),
                            strokeWidth = 1f
                        )
                    }
                    for (y in 0..4) {
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, h * y / 4f),
                            end = Offset(w, h * y / 4f),
                            strokeWidth = 1f
                        )
                    }

                    // Route Path
                    val p1 = Offset(w * 0.2f, h * 0.7f)
                    val p2 = Offset(w * 0.5f, h * 0.35f)
                    val p3 = Offset(w * 0.8f, h * 0.5f)

                    val routePath = Path().apply {
                        moveTo(p1.x, p1.y)
                        cubicTo(w * 0.35f, h * 0.8f, w * 0.4f, h * 0.2f, p2.x, p2.y)
                        cubicTo(w * 0.6f, h * 0.5f, w * 0.7f, h * 0.3f, p3.x, p3.y)
                    }

                    drawPath(
                        path = routePath,
                        color = ChampagneGold.copy(alpha = 0.7f),
                        style = Stroke(
                            width = 3f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
                            cap = StrokeCap.Round
                        )
                    )

                    // Waypoints
                    drawCircle(Color(0xFF2A2A2A), radius = 10f, center = p1)
                    drawCircle(TextPrimary, radius = 5f, center = p1)

                    drawCircle(Color(0xFF2A2A2A), radius = 10f, center = p2)
                    drawCircle(TextPrimary, radius = 5f, center = p2)

                    drawCircle(ChampagneGold.copy(alpha = 0.3f), radius = 14f, center = p3)
                    drawCircle(ChampagneGold, radius = 6f, center = p3)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer action
            OutlinedButton(
                onClick = onOpenFullSafetyMap,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, LuxuryBorder)
            ) {
                Icon(Icons.Default.Navigation, null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Interactive Map", color = TextPrimary, fontWeight = FontWeight.Medium)
            }
        }
    }
}
