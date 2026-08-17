package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserPreferenceEntity
import com.example.ui.theme.*

@Composable
fun TravelerPassportCard(
    preference: UserPreferenceEntity?,
    modifier: Modifier = Modifier,
    isRevealed: Boolean = true,
    linkedProgramCount: Int = 0,
    onRefineDnaClick: (() -> Unit)? = null
) {
    if (preference == null) {
        EmptyTravelerPassportCard(
            modifier = modifier,
            onRefineDnaClick = onRefineDnaClick
        )
        return
    }
    val pref = preference

    val serialNumber = remember(pref.id, pref.displayName) {
        val hash = (pref.displayName.hashCode() + pref.homeAirport.hashCode()).toString().takeLast(6).padStart(6, '7')
        "DNA-EXP-${hash.take(3)}-${hash.takeLast(3)}"
    }

    // A passport is only "verified" once it actually identifies someone going somewhere —
    // name and home base are both genuinely optional at onboarding (only the airport is gated
    // by canContinue), so don't award the crest until both are truthfully filled in.
    val isVerified = pref.displayName.isNotBlank() && pref.homeAirport.isNotBlank()
    val hasPortfolio = linkedProgramCount > 0 || pref.accessibilityVerificationOptIn

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LuxuryCard),
        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("traveler_passport_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Top Header: Monospace Serial & Security Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(LuxuryCardElevated)
                            .border(1.dp, LuxuryBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = ChampagneGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "MARCO EXPEDITIONS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = ChampagneGold,
                                letterSpacing = 1.2.sp
                            )
                        )
                        Text(
                            text = "OFFICIAL TRAVELER PASSPORT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary,
                                fontSize = 9.sp,
                                letterSpacing = 0.8.sp
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = LuxuryCardElevated,
                    border = BorderStroke(1.dp, LuxuryBorder)
                ) {
                    Text(
                        text = serialNumber,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Traveler Identity Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pref.displayName.ifBlank { "Name not set" },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (pref.displayName.isNotBlank()) TextPrimary else TextMuted
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = ChampagneGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Home Base: ${pref.homeAirport.ifBlank { "Selected Port" }}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                // Verified Stamp Crest — only earned once name + home base are both truthfully set
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(LuxuryCardElevated)
                        .border(1.dp, if (isVerified) ChampagneGold.copy(alpha = 0.5f) else LuxuryBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isVerified) ChampagneGold else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isVerified) "VERIFIED" else "UNVERIFIED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = if (isVerified) 7.sp else 6.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isVerified) ChampagneGold else TextMuted
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = LuxuryBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // 4 Passport Stamp Dimensions
            Text(
                text = "ACCREDITED TRAVELER STAMPS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Stamp 1: Origin
                PassportStampBox(
                    label = "ORIGIN",
                    value = pref.homeAirport.take(3).ifBlank { "PORT" },
                    icon = Icons.Default.AirplanemodeActive,
                    isStamped = pref.homeAirport.isNotBlank(),
                    modifier = Modifier.weight(1f)
                )

                // Stamp 2: Motivation — shows the highest-weighted motivation (travelMotivation
                // is already that value); stays unstamped with no invented archetype if the
                // user never rated any motivation.
                PassportStampBox(
                    label = "MOTIVE",
                    value = pref.travelMotivation.take(7).uppercase().ifBlank { "—" },
                    icon = Icons.Default.Explore,
                    isStamped = pref.travelMotivation.isNotBlank(),
                    modifier = Modifier.weight(1f)
                )

                // Stamp 3: Pacing — same rule; no default "BALANCED" when nothing was rated.
                PassportStampBox(
                    label = "PACE",
                    value = when {
                        pref.pacingPreference.contains("Slow") -> "SLOW"
                        pref.pacingPreference.contains("High") -> "ACTIVE"
                        pref.pacingPreference.isNotBlank() -> "BALANCED"
                        else -> "—"
                    },
                    icon = Icons.Default.Speed,
                    isStamped = pref.pacingPreference.isNotBlank(),
                    modifier = Modifier.weight(1f)
                )

                // Stamp 4: Loyalty / Access — only stamped once something is actually linked
                PassportStampBox(
                    label = "PORTFOLIO",
                    value = when {
                        pref.accessibilityVerificationOptIn -> "ADA VIP"
                        linkedProgramCount > 0 -> "LINKED"
                        else -> "—"
                    },
                    icon = Icons.Default.CardMembership,
                    isStamped = hasPortfolio,
                    modifier = Modifier.weight(1f)
                )
            }

            // Signature Aspiration Quote
            if (pref.signatureAspiration.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LuxurySurface,
                    border = BorderStroke(1.dp, LuxuryBorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "SIGNATURE ASPIRATION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = ChampagneGold,
                                fontSize = 9.sp,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "“${pref.signatureAspiration}”",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                lineHeight = 17.sp
                            )
                        )
                    }
                }
            }

            // Refine DNA deep link button
            if (onRefineDnaClick != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onRefineDnaClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LuxuryCardElevated,
                        contentColor = TextPrimary
                    ),
                    border = BorderStroke(1.dp, LuxuryBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("refine_dna_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ChampagneGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Refine Your DNA Studio →",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTravelerPassportCard(
    modifier: Modifier = Modifier,
    onRefineDnaClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LuxuryCard),
        border = BorderStroke(1.dp, LuxuryBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("traveler_passport_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Top Header: Monospace Serial & Security Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(LuxuryCardElevated)
                            .border(1.dp, LuxuryBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "MARCO EXPEDITIONS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.2.sp
                            )
                        )
                        Text(
                            text = "TRAVELER PASSPORT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary,
                                fontSize = 9.sp,
                                letterSpacing = 0.8.sp
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = LuxuryCardElevated,
                    border = BorderStroke(1.dp, LuxuryBorder)
                ) {
                    Text(
                        text = "UNISSUED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Honest Empty Identity Row — no invented traveler
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Traveler Passport not yet issued",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Complete onboarding to set your Home Base",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                // Unverified Stamp Crest
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(LuxuryCardElevated)
                        .border(1.dp, LuxuryBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "UNVERIFIED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 6.sp,
                                fontWeight = FontWeight.Black,
                                color = TextMuted
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = LuxuryBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // 4 Passport Stamp Dimensions — all unstamped
            Text(
                text = "ACCREDITED TRAVELER STAMPS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PassportStampBox(
                    label = "ORIGIN",
                    value = "—",
                    icon = Icons.Default.AirplanemodeActive,
                    isStamped = false,
                    modifier = Modifier.weight(1f)
                )
                PassportStampBox(
                    label = "MOTIVE",
                    value = "—",
                    icon = Icons.Default.Explore,
                    isStamped = false,
                    modifier = Modifier.weight(1f)
                )
                PassportStampBox(
                    label = "PACE",
                    value = "—",
                    icon = Icons.Default.Speed,
                    isStamped = false,
                    modifier = Modifier.weight(1f)
                )
                PassportStampBox(
                    label = "PORTFOLIO",
                    value = "—",
                    icon = Icons.Default.CardMembership,
                    isStamped = false,
                    modifier = Modifier.weight(1f)
                )
            }

            // Setup CTA
            if (onRefineDnaClick != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onRefineDnaClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LuxuryCardElevated,
                        contentColor = TextPrimary
                    ),
                    border = BorderStroke(1.dp, LuxuryBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("refine_dna_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ChampagneGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Set Up Your Traveler DNA →",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
fun PassportStampBox(
    label: String,
    value: String,
    icon: ImageVector,
    isStamped: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isStamped) LuxuryCardElevated else LuxurySurface,
        border = BorderStroke(1.dp, if (isStamped) ChampagneGold.copy(alpha = 0.4f) else LuxuryBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isStamped) ChampagneGold else TextMuted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isStamped) TextPrimary else TextMuted,
                    fontSize = 9.sp
                ),
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 7.sp,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
            )
        }
    }
}
