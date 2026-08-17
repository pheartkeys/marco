package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessageEntity
import com.example.data.model.TripEntity
import com.example.data.model.WalletBalanceEntity
import com.example.data.model.WalletTransactionEntity
import com.example.ui.theme.*

/**
 * Automated Weekly Budget Summary Card delivered by AI in chat.
 * Features a visual breakdown comparison between actual out-of-pocket spend vs.
 * loyalty program savings, category arbitrage, and pacing projections.
 */
@Composable
fun ChatWeeklyBudgetSummaryCard(
    message: ChatMessageEntity,
    trip: TripEntity?,
    walletBalances: List<WalletBalanceEntity>,
    transactions: List<WalletTransactionEntity>,
    onPlayTts: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenFullWallet: () -> Unit = {}
) {
    var isBreakdownExpanded by remember { mutableStateOf(false) }

    val totalActualSpend = transactions.sumOf { it.amountUsd }
    val totalLoyaltySavings = transactions.sumOf { it.loyaltySavingsUsd }
    val totalGrossTripValue = totalActualSpend + totalLoyaltySavings

    val loyaltySavingsPercent = if (totalGrossTripValue > 0) {
        ((totalLoyaltySavings / totalGrossTripValue) * 100).toInt()
    } else 0

    val animatedSavingsPercent by animateFloatAsState(
        targetValue = loyaltySavingsPercent.toFloat(),
        label = "savingsPercent"
    )

    // Category Breakdowns
    val categories = listOf("Lodging", "Flights", "Transit", "Activities", "Dining", "Groceries")
    val spendByCategory = categories.map { cat ->
        val spent = transactions.filter { it.category.equals(cat, ignoreCase = true) }.sumOf { it.amountUsd }
        val saved = transactions.filter { it.category.equals(cat, ignoreCase = true) }.sumOf { it.loyaltySavingsUsd }
        CategorySpendSummary(cat, spent, saved)
    }.filter { it.spent > 0 || it.saved > 0 }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, ContourBorder),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag("chat_weekly_budget_summary_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Card Header
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
                            .background(LuxuryCardElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Weekly Budget Summary",
                            tint = ChampagneGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Weekly budget summary",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CompassLilac.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "AI Report",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = CompassLilac,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                        Text(
                            text = "Spend vs. Loyalty Program Savings",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onPlayTts,
                    modifier = Modifier.testTag("weekly_summary_tts_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Listen to weekly summary",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Visual Chart Box: Actual Spend vs Loyalty Savings Donut / Bar Breakdown
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Donut Visual representation
                        Box(
                            modifier = Modifier.size(90.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val savedRatio = if (totalGrossTripValue > 0) (totalLoyaltySavings / totalGrossTripValue).toFloat() else 0f
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 10.dp.toPx()
                                // Background circle (Spend)
                                drawCircle(
                                    color = MaritimeBlue,
                                    style = Stroke(strokeWidth)
                                )
                                // Arc for Savings
                                drawArc(
                                    color = WayfinderEmerald,
                                    startAngle = -90f,
                                    sweepAngle = 360f * savedRatio,
                                    useCenter = false,
                                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${animatedSavingsPercent.toInt()}%",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = WayfinderEmerald
                                    )
                                )
                                Text(
                                    text = "Saved",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Spend vs Savings Totals
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Actual Spend
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(MaritimeBlue)
                                    )
                                    Text(
                                        text = "Out of Pocket:",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                                Text(
                                    text = "$${String.format("%,.2f", totalActualSpend)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            // Loyalty Savings
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(WayfinderEmerald)
                                    )
                                    Text(
                                        text = "Loyalty Savings:",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = WayfinderEmerald,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                                Text(
                                    text = "+$${String.format("%,.2f", totalLoyaltySavings)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = WayfinderEmerald
                                    )
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Gross Trip Value
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Gross Experience Value:",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = "$${String.format("%,.2f", totalGrossTripValue)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Highlights & Arbitrage Insights
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = WayfinderEmerald.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = WayfinderEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "AI takeaway",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = WayfinderEmerald
                            )
                        )
                        val takeawayText = if (totalLoyaltySavings > 0) {
                            "Your loyalty points redemptions & membership benefits absorbed $loyaltySavingsPercent% of your primary expenses."
                        } else {
                            "Log expenses and apply loyalty points or timeshare weeks to track real-time savings arbitrage."
                        }
                        Text(
                            text = takeawayText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = WayfinderEmerald,
                                lineHeight = 15.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Arbitrage Breakdown Toggle
            OutlinedButton(
                onClick = { isBreakdownExpanded = !isBreakdownExpanded },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("toggle_weekly_category_breakdown_btn")
            ) {
                Icon(
                    imageVector = if (isBreakdownExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isBreakdownExpanded) "Hide Category Comparison" else "View Category Breakdown (${spendByCategory.size} Categories)",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            AnimatedVisibility(visible = isBreakdownExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    spendByCategory.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.category,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "Paid: $${String.format("%.0f", item.spent)}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                        if (item.saved > 0) {
                                            Text(
                                                text = "Saved: +$${String.format("%.0f", item.saved)}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = WayfinderEmerald,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Category comparative split bar
                                val catTotal = item.spent + item.saved
                                val catSavedRatio = if (catTotal > 0) (item.saved / catTotal).toFloat() else 0f
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(if (catSavedRatio > 0) (1f - catSavedRatio).coerceAtLeast(0.05f) else 1f)
                                            .fillMaxHeight()
                                            .background(MaritimeBlue)
                                    )
                                    if (catSavedRatio > 0) {
                                        Box(
                                            modifier = Modifier
                                                .weight(catSavedRatio.coerceAtLeast(0.05f))
                                                .fillMaxHeight()
                                                .background(WayfinderEmerald)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tap-through Navigation to Full Wallet & Timeshare Rewards
            Button(
                onClick = onOpenFullWallet,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WayfinderEmerald,
                    contentColor = CartographyDarkBase
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weekly_summary_open_wallet_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "View Points, Cards & Timeshare Swaps →",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

private data class CategorySpendSummary(
    val category: String,
    val spent: Double,
    val saved: Double
)
