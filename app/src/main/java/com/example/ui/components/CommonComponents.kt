package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Cabin
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.Restaurant
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun CategoryIconBadge(
    category: String,
    modifier: Modifier = Modifier,
    size: Int = 40
) {
    val (icon, bgColor, tintColor) = getCategoryStyling(category)

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, tintColor.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category,
            tint = tintColor,
            modifier = Modifier.size((size * 0.54).dp)
        )
    }
}

fun getCategoryStyling(category: String): Triple<ImageVector, Color, Color> {
    return when (category.uppercase()) {
        "FLIGHT", "AIRLINE" -> Triple(Icons.Default.Flight, VoyagerSkyPastel, VoyagerSky)
        "HOTEL" -> Triple(Icons.Default.Hotel, GoldenSunPastel, GoldenSunDark)
        "TIMESHARE", "EXCHANGE" -> Triple(Icons.Default.Apartment, BerryOrchidPastel, BerryOrchid)
        "CAMPGROUND", "CAMPING" -> Triple(Icons.Default.Cabin, PalmEmeraldPastel, PalmEmeraldDark)
        "TRANSIT", "TRANSPORT" -> Triple(Icons.Default.DirectionsBus, VoyagerSkyPastel, VoyagerSky)
        "DINING", "FOOD" -> Triple(Icons.Default.Restaurant, MarcoCoralPastel, MarcoCoral)
        "FAMILY_KIDS", "FAMILY" -> Triple(Icons.Default.ChildCare, GoldenSunPastel, GoldenSunDark)
        "CREDIT_CARD", "REWARDS" -> Triple(Icons.Default.CardMembership, BerryOrchidPastel, BerryOrchid)
        "WALLET" -> Triple(Icons.Default.AccountBalanceWallet, PalmEmeraldPastel, PalmEmeraldDark)
        else -> Triple(Icons.Default.LocalActivity, MarcoCoralPastel, MarcoCoral)
    }
}

@Composable
fun ExpeditionBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MarcoCoral,
    icon: ImageVector? = null,
    isFilled: Boolean = false
) {
    val containerBg = if (isFilled) {
        when (color) {
            MarcoCoral -> MarcoCoralPastel
            VoyagerSky -> VoyagerSkyPastel
            GoldenSun, GoldenSunLight, GoldenSunDark -> GoldenSunPastel
            PalmEmerald, PalmEmeraldLight, PalmEmeraldDark -> PalmEmeraldPastel
            BerryOrchid, BerryOrchidLight -> BerryOrchidPastel
            else -> color.copy(alpha = 0.12f)
        }
    } else {
        LightSurface
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerBg,
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp
                )
            )
        }
    }
}

@Composable
fun AccessibilityTagChip(
    text: String,
    modifier: Modifier = Modifier,
    tintColor: Color = PalmEmeraldDark
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = PalmEmeraldPastel,
        border = BorderStroke(1.dp, tintColor.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Accessible,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = tintColor
                )
            )
        }
    }
}

@Composable
fun TravelCrewAvatarRow(
    travelerNames: List<String>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 4
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val visible = travelerNames.take(maxVisible)
        val overflow = (travelerNames.size - maxVisible).coerceAtLeast(0)

        visible.forEachIndexed { index, name ->
            Box(
                modifier = Modifier
                    .padding(start = if (index > 0) (-7).dp else 0.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        when (index % 4) {
                            0 -> MarcoCoral
                            1 -> VoyagerSky
                            2 -> PalmEmerald
                            else -> GoldenSun
                        }
                    )
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.firstOrNull()?.uppercase() ?: "👤",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                )
            }
        }

        if (overflow > 0) {
            Box(
                modifier = Modifier
                    .padding(start = (-7).dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(LightCardElevated)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$overflow",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
            }
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
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveBar"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val heights = listOf(0.4f, 0.85f, 0.5f, 1.0f, 0.65f, 0.9f, 0.35f)
        for (i in 0 until barCount) {
            val scale = (heights[i % heights.size] * animProgress).coerceIn(0.25f, 1.0f)
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height((22 * scale).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                GoldenSun,
                                MarcoCoral
                            )
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(LightSurface)
            .border(1.dp, LightBorder, RoundedCornerShape(20.dp))
            .padding(20.dp)
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
                    ExpeditionBadge(
                        text = badgeText,
                        color = MarcoCoral,
                        isFilled = true,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                )
            }
        }
    }
}
