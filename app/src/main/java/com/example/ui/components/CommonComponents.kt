package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Cabin
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberGold
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.Navy900
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.SkyBlueLight
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TealAccent

@Composable
fun CategoryIconBadge(
    category: String,
    modifier: Modifier = Modifier,
    size: Int = 36
) {
    val (icon, bgColor, tintColor) = getCategoryStyling(category)

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category,
            tint = tintColor,
            modifier = Modifier.size((size * 0.58).dp)
        )
    }
}

fun getCategoryStyling(category: String): Triple<ImageVector, Color, Color> {
    return when (category.uppercase()) {
        "FLIGHT", "AIRLINE" -> Triple(Icons.Default.Flight, Color(0xFF0284C7).copy(alpha = 0.15f), OceanBlue)
        "HOTEL" -> Triple(Icons.Default.Hotel, Color(0xFF8B5CF6).copy(alpha = 0.15f), PurpleAccent)
        "TIMESHARE", "EXCHANGE" -> Triple(Icons.Default.Apartment, Color(0xFFF59E0B).copy(alpha = 0.15f), AmberGold)
        "CAMPGROUND", "CAMPING" -> Triple(Icons.Default.Cabin, Color(0xFF10B981).copy(alpha = 0.15f), EmeraldGreen)
        "TRANSIT", "TRANSPORT" -> Triple(Icons.Default.DirectionsBus, Color(0xFF0D9488).copy(alpha = 0.15f), TealAccent)
        "DINING", "FOOD" -> Triple(Icons.Default.Restaurant, Color(0xFFF43F5E).copy(alpha = 0.15f), SunsetCoral)
        "FAMILY_KIDS", "FAMILY" -> Triple(Icons.Default.ChildCare, Color(0xFFEC4899).copy(alpha = 0.15f), Color(0xFFEC4899))
        "CREDIT_CARD", "REWARDS" -> Triple(Icons.Default.CardMembership, Color(0xFFF59E0B).copy(alpha = 0.15f), AmberGold)
        "WALLET" -> Triple(Icons.Default.AccountBalanceWallet, Color(0xFF0284C7).copy(alpha = 0.15f), OceanBlue)
        else -> Triple(Icons.Default.LocalActivity, Color(0xFF0284C7).copy(alpha = 0.15f), OceanBlue)
    }
}

@Composable
fun AccessibilityTagChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = TealAccent.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Accessible,
                contentDescription = null,
                tint = TealAccent,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = TealAccent
                )
            )
        }
    }
}

@Composable
fun LiveAudioWaveform(
    modifier: Modifier = Modifier,
    barCount: Int = 7
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveBar"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val heights = listOf(0.4f, 0.8f, 0.5f, 1.0f, 0.6f, 0.9f, 0.3f)
        for (i in 0 until barCount) {
            val scale = (heights[i % heights.size] * animProgress).coerceIn(0.2f, 1.0f)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((24 * scale).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(SkyBlueLight, TealAccent)
                        )
                    )
            )
        }
    }
}

@Composable
fun HeroGradientBanner(
    title: String,
    subtitle: String,
    badgeText: String = "",
    heroThemeIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    val gradientColors = when (heroThemeIndex % 4) {
        0 -> listOf(Navy900, OceanBlue, TealAccent)
        1 -> listOf(Navy900, Color(0xFF1E3A8A), PurpleAccent)
        2 -> listOf(Color(0xFF064E3B), EmeraldGreen, TealAccent)
        else -> listOf(Navy900, Color(0xFFB45309), AmberGold)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(gradientColors))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.weight(1f)
            ) {
                if (badgeText.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                )
            }
        }
    }
}
