package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessageEntity
import com.example.data.model.TripActivityEntity
import com.example.data.model.TripEntity
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom Canvas UI component rendering an antique parchment map with
 * nautical wind rose compass, rhumb navigation lines, dotted voyage path,
 * and geographic coordinates to trace the active expedition's journey.
 */
@Composable
fun ChatVintageParchmentMapCard(
    message: ChatMessageEntity,
    trip: TripEntity?,
    activities: List<TripActivityEntity>,
    onPlayTts: () -> Unit = {},
    modifier: Modifier = Modifier,
    onOpenFullSafetyMap: () -> Unit = {}
) {
    val destination = trip?.destination ?: "Expedition Region"

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFB45309), Color(0xFF78350F))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = Color(0xFFFDE68A),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Antique Journey Cartography",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = "Parchment Radar",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF92400E),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                )
                            }
                        }
                        Text(
                            text = "Tracing expedition path across $destination",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                IconButton(onClick = onPlayTts) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Listen to map guidance",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas Parchment Rendering
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF2C241B))
            ) {
                VintageParchmentMapCanvas(
                    modifier = Modifier.fillMaxSize(),
                    destination = destination
                )

                // Overlay Info Badges
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xDD1E1710)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🧭 20°48′N 156°20′W • Pacific Wind Vector 14kt NE",
                                color = Color(0xFFFDE68A),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xDD1E1710)
                        ) {
                            Text(
                                text = "⚓ Origin: SFO Port",
                                color = Color(0xFFE2E8F0),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xDD78350F)
                        ) {
                            Text(
                                text = "📍 Active Waypoint: Wailea Basin",
                                color = Color(0xFFFEF3C7),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tap-through Navigation to Full Safety Radar & Offline Map Screen
            Button(
                onClick = onOpenFullSafetyMap,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_full_safety_map_view_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open Offline Radar & Safety Telemetry →",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun VintageParchmentMapCanvas(
    destination: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Parchment base tint background
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF423526),
                    Color(0xFF2C241B),
                    Color(0xFF1D1711)
                ),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.7f
            )
        )

        // Nautical Rhumb Lines & Graticules
        val rhumbColor = Color(0xFFD97706).copy(alpha = 0.22f)
        val gridStep = 45.dp.toPx()

        for (x in 0..(w / gridStep).toInt()) {
            drawLine(
                color = Color(0xFF92400E).copy(alpha = 0.12f),
                start = Offset(x * gridStep, 0f),
                end = Offset(x * gridStep, h),
                strokeWidth = 1f
            )
        }
        for (y in 0..(h / gridStep).toInt()) {
            drawLine(
                color = Color(0xFF92400E).copy(alpha = 0.12f),
                start = Offset(0f, y * gridStep),
                end = Offset(w, y * gridStep),
                strokeWidth = 1f
            )
        }

        // Radiating Navigational Rhumb Rays from center
        val center = Offset(w * 0.45f, h * 0.52f)
        for (angle in 0 until 360 step 30) {
            val rad = Math.toRadians(angle.toDouble())
            val endX = center.x + (w * 0.9f) * cos(rad).toFloat()
            val endY = center.y + (w * 0.9f) * sin(rad).toFloat()
            drawLine(
                color = rhumbColor,
                start = center,
                end = Offset(endX, endY),
                strokeWidth = 1.2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 10f), 0f)
            )
        }

        // Nautical Compass Wind Rose (Antique 8-point star)
        val roseCenter = Offset(w * 0.82f, h * 0.28f)
        val starRadius = 26.dp.toPx()

        drawCircle(
            color = Color(0xFFD97706).copy(alpha = 0.25f),
            radius = starRadius * 1.2f,
            center = roseCenter,
            style = Stroke(width = 1.5f)
        )
        drawCircle(
            color = Color(0xFFD97706).copy(alpha = 0.4f),
            radius = starRadius * 0.6f,
            center = roseCenter,
            style = Stroke(width = 1f)
        )

        // 8 points
        for (i in 0 until 8) {
            val angle = i * 45f
            val rad = Math.toRadians(angle.toDouble())
            val rLen = if (i % 2 == 0) starRadius else starRadius * 0.65f
            val px = roseCenter.x + rLen * cos(rad).toFloat()
            val py = roseCenter.y + rLen * sin(rad).toFloat()

            val pLeftRad = Math.toRadians((angle + 20).toDouble())
            val pRightRad = Math.toRadians((angle - 20).toDouble())
            val leftBase = Offset(
                roseCenter.x + (starRadius * 0.25f) * cos(pLeftRad).toFloat(),
                roseCenter.y + (starRadius * 0.25f) * sin(pLeftRad).toFloat()
            )
            val rightBase = Offset(
                roseCenter.x + (starRadius * 0.25f) * cos(pRightRad).toFloat(),
                roseCenter.y + (starRadius * 0.25f) * sin(pRightRad).toFloat()
            )

            val pointPath = Path().apply {
                moveTo(roseCenter.x, roseCenter.y)
                lineTo(leftBase.x, leftBase.y)
                lineTo(px, py)
                close()
            }
            drawPath(pointPath, if (i % 2 == 0) Color(0xFFF59E0B) else Color(0xFFB45309))
        }

        // Island Coastline Contour Lines (Antique Cartography stylization)
        val island1 = Path().apply {
            moveTo(w * 0.15f, h * 0.65f)
            cubicTo(w * 0.25f, h * 0.45f, w * 0.4f, h * 0.5f, w * 0.55f, h * 0.68f)
            cubicTo(w * 0.48f, h * 0.85f, w * 0.25f, h * 0.88f, w * 0.15f, h * 0.65f)
            close()
        }
        drawPath(island1, Color(0xFF53412E))
        drawPath(island1, Color(0xFFB45309).copy(alpha = 0.5f), style = Stroke(width = 2f))

        val island2 = Path().apply {
            moveTo(w * 0.52f, h * 0.45f)
            cubicTo(w * 0.62f, h * 0.32f, w * 0.78f, h * 0.38f, w * 0.8f, h * 0.62f)
            cubicTo(w * 0.7f, h * 0.75f, w * 0.55f, h * 0.65f, w * 0.52f, h * 0.45f)
            close()
        }
        drawPath(island2, Color(0xFF53412E))
        drawPath(island2, Color(0xFFB45309).copy(alpha = 0.5f), style = Stroke(width = 2f))

        // Dotted Voyage Expedition Path
        val voyagePath = Path().apply {
            moveTo(w * 0.12f, h * 0.35f)
            cubicTo(w * 0.25f, h * 0.4f, w * 0.32f, h * 0.6f, w * 0.48f, h * 0.62f)
            cubicTo(w * 0.6f, h * 0.64f, w * 0.68f, h * 0.48f, w * 0.75f, h * 0.52f)
        }
        drawPath(
            path = voyagePath,
            color = Color(0xFFFDE68A),
            style = Stroke(
                width = 3.5f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        )

        // Waypoint markers
        val wp1 = Offset(w * 0.12f, h * 0.35f)
        val wp2 = Offset(w * 0.32f, h * 0.6f)
        val wp3 = Offset(w * 0.48f, h * 0.62f)
        val wp4 = Offset(w * 0.75f, h * 0.52f)

        drawCircle(Color(0xFF38BDF8), radius = 7f, center = wp1)
        drawCircle(Color(0xFF10B981), radius = 8f, center = wp2)
        drawCircle(Color(0xFFF59E0B), radius = 10f, center = wp3)
        drawCircle(Color(0xFFEF4444), radius = 8f, center = wp4)
    }
}
