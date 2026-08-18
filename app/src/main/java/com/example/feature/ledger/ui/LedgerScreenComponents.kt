package com.example.feature.ledger.ui

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ContributionEntity
import com.example.data.model.ContributionProposalSource
import com.example.data.model.ContributionState
import com.example.data.model.SettlementEntity
import com.example.data.model.SettlementState
import com.example.data.model.TravelerBalance
import com.example.data.model.TravelerEntity
import com.example.data.model.hasLabelledProposal
import com.example.data.model.hasRecordedAgreement
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.LuxuryBorder
import com.example.ui.theme.LuxuryCard
import com.example.ui.theme.LuxuryCardElevated
import com.example.ui.theme.LuxuryDarkBase
import com.example.ui.theme.LuxurySurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

/**
 * Renders a contribution offer / agreement card.
 */
@Composable
fun ContributionCard(
    contribution: ContributionEntity,
    contributorName: String,
    onRecordAgreementClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAgreed = contribution.hasRecordedAgreement
    val hasProposal = contribution.hasLabelledProposal

    Surface(
        color = LuxuryCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isAgreed) ChampagneGold.copy(alpha = 0.5f) else LuxuryBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = contribution.assetKind.replace('_', ' '),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ChampagneGold,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Text(
                        text = "${contribution.nativeQuantity} ${contribution.nativeUnitLabel}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Surface(
                    color = if (isAgreed) ChampagneGold.copy(alpha = 0.15f) else LuxuryCardElevated,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isAgreed) ChampagneGold else LuxuryBorder)
                ) {
                    Text(
                        text = if (isAgreed) "AGREED" else "OFFERED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isAgreed) ChampagneGold else TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Offered by $contributorName",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )

            if (isAgreed) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Agreed",
                        tint = ChampagneGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Valued at ${String.format(Locale.US, "%.2f", contribution.agreedValueAmount)} ${contribution.agreedValueCurrency}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = ChampagneGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            } else if (hasProposal) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Proposed: ${String.format(Locale.US, "%.2f", contribution.proposedValueAmount)} ${contribution.proposedValueCurrency} (${contribution.proposalSource})",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRecordAgreementClick,
                    border = BorderStroke(1.dp, ChampagneGold),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ChampagneGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Record Group Agreement", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRecordAgreementClick,
                    border = BorderStroke(1.dp, LuxuryBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Value Contribution", fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Agreement recording modal with signatories multi-select and numeric keypad.
 *
 * [ledgerCurrency] is the trip's confirmed ledger currency (empty when the group hasn't confirmed
 * one yet). An agreement can only be recorded in that currency — there is no assumed fallback, so
 * recording is disabled until it is set.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordAgreementDialog(
    contribution: ContributionEntity,
    travelers: List<TravelerEntity>,
    ledgerCurrency: String,
    onDismiss: () -> Unit,
    onConfirmAgreement: (amount: Double, currency: String, agreedByTravelerIds: List<Long>) -> Unit
) {
    var amountText by remember {
        mutableStateOf(
            if (contribution.proposedValueAmount > 0.0) contribution.proposedValueAmount.toString() else ""
        )
    }
    var selectedSignatories by remember {
        mutableStateOf(travelers.map { it.id }.toSet())
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = LuxurySurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, LuxuryBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RECORD CONTRIBUTION VALUE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ChampagneGold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Displayed amount — never assumes a currency; if the group hasn't confirmed one
                // yet, say so instead of labelling the figure USD.
                Text(
                    text = if (ledgerCurrency.isBlank()) {
                        "Ledger currency not set"
                    } else {
                        "${amountText.ifBlank { "0.00" }} $ledgerCurrency"
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = ChampagneGold,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Numeric Keypad
                NumericKeypad(
                    value = amountText,
                    onValueChange = { amountText = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Signatories selector
                Text(
                    text = "SIGNATORIES (REQUIRED)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    travelers.forEach { traveler ->
                        val isSelected = traveler.id in selectedSignatories
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedSignatories = if (isSelected) {
                                    selectedSignatories - traveler.id
                                } else {
                                    selectedSignatories + traveler.id
                                }
                            },
                            label = { Text(traveler.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ChampagneGold.copy(alpha = 0.2f),
                                selectedLabelColor = ChampagneGold,
                                containerColor = LuxuryCard,
                                labelColor = TextMuted
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) ChampagneGold else LuxuryBorder,
                                enabled = true,
                                selected = isSelected
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, LuxuryBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            if (amount > 0.0 && selectedSignatories.isNotEmpty() && ledgerCurrency.isNotBlank()) {
                                onConfirmAgreement(amount, ledgerCurrency, selectedSignatories.toList())
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChampagneGold,
                            contentColor = LuxuryDarkBase
                        ),
                        enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0.0 &&
                            selectedSignatories.isNotEmpty() &&
                            ledgerCurrency.isNotBlank()
                    ) {
                        Text("Sign & Record", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Summary view for settlements and net balances.
 */
@Composable
fun SettlementSummaryCard(
    balances: List<TravelerBalance>,
    settlements: List<SettlementEntity>,
    travelersById: Map<Long, TravelerEntity>,
    onMarkPaid: (SettlementEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        color = LuxuryCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, LuxuryBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "EXPEDITION SETTLEMENT & BALANCES",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = ChampagneGold,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    fontFamily = FontFamily.Monospace
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Per-person balance breakdown
            balances.forEach { b ->
                val traveler = travelersById[b.travelerId]
                // A blank name on a real traveler row is "unnamed yet" — id-based label is fine.
                // A traveler id with no row at all is a data-integrity gap and must not be
                // rendered as though it were a normal member.
                val isUnresolved = traveler == null
                val name = traveler?.displayName?.ifBlank { "Member #${b.travelerId}" }
                    ?: "Unresolved traveler (id ${b.travelerId})"
                val isCreditor = b.net > 0.01
                val isDebtor = b.net < -0.01

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isUnresolved) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Unresolved traveler reference",
                                    tint = TextMuted,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isUnresolved) TextMuted else TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                        if (b.pointsContributedQuantity > 0.0) {
                            Text(
                                text = "${String.format(Locale.US, "%.0f", b.pointsContributedQuantity)} pts contributed",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = ChampagneGold,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                        if (b.unconvertedEntryIds.isNotEmpty()) {
                            Text(
                                text = "${b.unconvertedEntryIds.size} unconverted fx entries",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    Text(
                        text = if (isCreditor) {
                            "+${String.format(Locale.US, "%.2f", b.net)} ${b.currency}"
                        } else if (isDebtor) {
                            "-${String.format(Locale.US, "%.2f", -b.net)} ${b.currency}"
                        } else {
                            "Settled"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isCreditor) ChampagneGold else if (isDebtor) TextSecondary else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }

            if (settlements.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "PROPOSED TRANSFERS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                settlements.forEach { s ->
                    val isPaid = s.state.equals(SettlementState.PAID.value, ignoreCase = true)

                    Surface(
                        color = LuxuryCardElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isPaid) ChampagneGold.copy(alpha = 0.3f) else LuxuryBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TravelerNameLabel(
                                    traveler = travelersById[s.fromTravelerId],
                                    travelerId = s.fromTravelerId
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "pays",
                                    tint = ChampagneGold,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                TravelerNameLabel(
                                    traveler = travelersById[s.toTravelerId],
                                    travelerId = s.toTravelerId
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", s.amount)} ${s.currency}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = ChampagneGold,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (!isPaid) {
                                    Surface(
                                        color = ChampagneGold,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.clickable { onMarkPaid(s) }
                                    ) {
                                        Text(
                                            text = "Mark Paid",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = LuxuryDarkBase,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Paid",
                                        tint = ChampagneGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders a traveler's name inline. A real traveler with a blank name is genuinely unnamed yet
 * and gets an id-based label ("Member #12"). A traveler id with no matching row at all is a
 * data-integrity gap, not an unnamed person, so it renders as an explicit unresolved reference
 * rather than a plausible-looking name.
 */
@Composable
private fun TravelerNameLabel(traveler: TravelerEntity?, travelerId: Long) {
    if (traveler == null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Unresolved traveler reference",
                tint = TextMuted,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Unresolved (id $travelerId)",
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
            )
        }
    } else {
        Text(
            text = traveler.displayName.ifBlank { "Member #$travelerId" },
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
