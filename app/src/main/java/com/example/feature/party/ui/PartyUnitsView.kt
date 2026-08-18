package com.example.feature.party.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PartyUnitEntity
import com.example.data.model.PartyUnitType
import com.example.data.model.TravelerAgeBand
import com.example.data.model.TravelerEntity
import com.example.data.model.TripMembershipEntity
import com.example.data.model.TripRole
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.LuxuryBorder
import com.example.ui.theme.LuxuryCard
import com.example.ui.theme.LuxuryCardElevated
import com.example.ui.theme.LuxurySurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Renders the party units and membership hierarchy for a trip.
 * Grouped unit cards with champagne gold hairline, member chips, and role badges.
 * Built to fit 12 travelers comfortably on 344dp folded screens.
 */
@Composable
fun PartyUnitsView(
    partyUnits: List<PartyUnitEntity>,
    memberships: List<TripMembershipEntity>,
    travelers: List<TravelerEntity>,
    onAddUnitClick: () -> Unit = {},
    onInviteClick: () -> Unit = {},
    onAssignMemberClick: (TripMembershipEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val travelersById = travelers.associateBy { it.id }
    val membershipsByUnit = memberships.groupBy { it.partyUnitId }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(LuxurySurface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TRAVEL CREW & UNITS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ChampagneGold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Text(
                    text = "${travelers.size} Expedition Members",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Row {
                IconButton(
                    onClick = onAddUnitClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(LuxuryCardElevated)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Party Unit",
                        tint = ChampagneGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onInviteClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(LuxuryCardElevated)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Invite Traveler",
                        tint = ChampagneGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Party Units
        if (partyUnits.isEmpty() && memberships.isEmpty()) {
            Surface(
                color = LuxuryCard,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, LuxuryBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No party units created yet",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap + above to group couples, families, or teams",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }
            }
        } else {
            partyUnits.forEach { unit ->
                val unitMemberships = membershipsByUnit[unit.id] ?: emptyList()
                PartyUnitCard(
                    unit = unit,
                    memberships = unitMemberships,
                    travelersById = travelersById,
                    onAssignMemberClick = onAssignMemberClick,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Unassigned Travelers (partyUnitId == 0)
            val unassigned = membershipsByUnit[0L] ?: emptyList()
            if (unassigned.isNotEmpty()) {
                Surface(
                    color = LuxuryCard,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LuxuryBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "UNASSIGNED TRAVELERS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        MemberChipsGrid(
                            memberships = unassigned,
                            travelersById = travelersById,
                            onAssignClick = onAssignMemberClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PartyUnitCard(
    unit: PartyUnitEntity,
    memberships: List<TripMembershipEntity>,
    travelersById: Map<Long, TravelerEntity>,
    onAssignMemberClick: (TripMembershipEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val unitType = PartyUnitType.fromStringOrNull(unit.unitType) ?: PartyUnitType.COUPLE
    val icon = when (unitType) {
        PartyUnitType.COUPLE -> Icons.Default.Person
        PartyUnitType.FAMILY -> Icons.Default.FamilyRestroom
        PartyUnitType.SOLO -> Icons.Default.Person
        PartyUnitType.CORPORATE_TEAM -> Icons.Default.CorporateFare
    }

    Surface(
        color = LuxuryCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = unitType.label,
                        tint = ChampagneGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = unit.label.ifBlank { unitType.label },
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Text(
                    text = "${memberships.size} members",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (memberships.isEmpty()) {
                Text(
                    text = "No members placed in this unit yet.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )
            } else {
                MemberChipsGrid(
                    memberships = memberships,
                    travelersById = travelersById,
                    onAssignClick = onAssignMemberClick
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemberChipsGrid(
    memberships: List<TripMembershipEntity>,
    travelersById: Map<Long, TravelerEntity>,
    onAssignClick: (TripMembershipEntity) -> Unit = {}
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        memberships.forEach { membership ->
            val traveler = travelersById[membership.travelerId]
            if (traveler == null) {
                // The membership row references a traveler id with no matching TravelerEntity.
                // That is a data-integrity gap, not a person with no name yet — render it as an
                // unresolved reference so the bug is visible, never as a normal-looking member.
                UnresolvedMemberChip(travelerId = membership.travelerId)
                return@forEach
            }
            val name = traveler.displayName.ifBlank { "Member #${membership.travelerId}" }
            val role = TripRole.fromStringOrNull(membership.role) ?: TripRole.TRAVELER
            val ageBand = TravelerAgeBand.fromStringOrNull(traveler.ageBand)

            Surface(
                color = LuxuryCardElevated,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, if (role == TripRole.ORGANIZER) ChampagneGold else LuxuryBorder),
                modifier = Modifier.clickable { onAssignClick(membership) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (role == TripRole.ORGANIZER) ChampagneGold.copy(alpha = 0.2f) else LuxuryBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name.take(1).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (role == TripRole.ORGANIZER) ChampagneGold else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    if (ageBand != null && ageBand != TravelerAgeBand.ADULT) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = ageBand.label.take(1),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }

                    if (role == TripRole.ORGANIZER) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Organizer",
                            tint = ChampagneGold,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders a membership row whose [TravelerEntity] is missing entirely — a data-integrity gap,
 * not an unnamed traveler. Deliberately styled unlike a normal member chip (muted, warning icon,
 * explicit "unresolved" text) so it never reads as a real, if nameless, member.
 */
@Composable
private fun UnresolvedMemberChip(travelerId: Long) {
    Surface(
        color = LuxuryCardElevated,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, TextMuted)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Unresolved traveler reference",
                tint = TextMuted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Unresolved traveler (id $travelerId)",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextMuted,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}
