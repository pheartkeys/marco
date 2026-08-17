package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneGoldMuted
import com.example.ui.theme.LuxuryBorder
import com.example.ui.theme.LuxuryCard
import com.example.ui.theme.LuxuryCardElevated
import com.example.ui.theme.LuxurySurface
import com.example.ui.theme.StatusAzure
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

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
            .background(bgColor)
            .border(1.dp, LuxuryBorder, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category,
            tint = tintColor,
            modifier = Modifier.size((size * 0.52).dp)
        )
    }
}

fun getCategoryStyling(category: String): Triple<ImageVector, Color, Color> {
    return when (category.uppercase()) {
        "FLIGHT", "AIRLINE" -> Triple(Icons.Default.Flight, LuxuryCardElevated, TextSecondary)
        "HOTEL" -> Triple(Icons.Default.Hotel, LuxuryCardElevated, ChampagneGold)
        "TIMESHARE", "EXCHANGE" -> Triple(Icons.Default.Apartment, LuxuryCardElevated, ChampagneGold)
        "CAMPGROUND", "CAMPING" -> Triple(Icons.Default.Cabin, LuxuryCardElevated, TextSecondary)
        "TRANSIT", "TRANSPORT" -> Triple(Icons.Default.DirectionsBus, LuxuryCardElevated, TextSecondary)
        "DINING", "FOOD" -> Triple(Icons.Default.Restaurant, LuxuryCardElevated, ChampagneGold)
        "FAMILY_KIDS", "FAMILY" -> Triple(Icons.Default.ChildCare, LuxuryCardElevated, TextSecondary)
        "CREDIT_CARD", "REWARDS" -> Triple(Icons.Default.CardMembership, LuxuryCardElevated, ChampagneGold)
        "WALLET" -> Triple(Icons.Default.AccountBalanceWallet, LuxuryCardElevated, TextSecondary)
        else -> Triple(Icons.Default.LocalActivity, LuxuryCardElevated, ChampagneGold)
    }
}

@Composable
fun AccessibilityTagChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = LuxurySurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, LuxuryBorder),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Accessible,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
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
                    .width(3.dp)
                    .height((22 * scale).dp)
                    .clip(CircleShape)
                    .background(ChampagneGold)
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LuxuryCard)
            .border(1.dp, LuxuryBorder, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (badgeText.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ChampagneGoldMuted,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.3f)),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(ChampagneGold)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = badgeText.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ChampagneGold,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary
                    )
                )
            }
        }
    }
}
