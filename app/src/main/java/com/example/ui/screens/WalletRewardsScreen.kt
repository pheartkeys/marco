package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ConnectedAccountEntity
import com.example.data.model.ExpenseEntity
import com.example.ui.components.CategoryIconBadge
import com.example.ui.model.PreferenceConstants
import com.example.ui.theme.*
import com.example.viewmodel.TravelViewModel

@Composable
fun WalletRewardsScreen(
    viewModel: TravelViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    BackHandler(onBack = onNavigateBack)
    val accounts by viewModel.connectedAccounts.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val isOptimizing by viewModel.isOptimizingRewards.collectAsState()
    val optimizerResult by viewModel.rewardsOptimizerResult.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Accounts & Rewards, 1 = Multi-Currency Expenses
    var isAddAccountDialogOpen by remember { mutableStateOf(false) }
    var isAddExpenseDialogOpen by remember { mutableStateOf(false) }
    var accountBeingEdited by remember { mutableStateOf<ConnectedAccountEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Top Navigation & Actions Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("wallet_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Chat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Marco Wallet & Rewards",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Points, Timeshares & FX Ledger",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Button(
                onClick = {
                    if (selectedTab == 0) isAddAccountDialogOpen = true else isAddExpenseDialogOpen = true
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LuxuryCardElevated,
                    contentColor = TextPrimary
                ),
                border = BorderStroke(1.dp, LuxuryBorder),
                modifier = Modifier.testTag("wallet_top_add_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (selectedTab == 0) "Add Account" else "Add Expense",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tab Selector
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = LuxurySurface,
            contentColor = TextPrimary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Loyalty & Timeshares (${accounts.size})", fontWeight = FontWeight.Medium) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Multi-Currency Ledger", fontWeight = FontWeight.Medium) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            // Loyalty & Timeshares tab
            AccountsAndRewardsView(
                accounts = accounts,
                isOptimizing = isOptimizing,
                optimizerResult = optimizerResult,
                onRunOptimizer = { viewModel.runRewardsOptimizer() },
                onAddAccount = { isAddAccountDialogOpen = true },
                onDeleteAccount = { viewModel.deleteConnectedAccount(it) },
                onEditAccount = { accountBeingEdited = it }
            )
        } else {
            // Multi-Currency Ledger tab
            ExpensesAndWalletView(
                expenses = expenses,
                onAddExpense = { isAddExpenseDialogOpen = true },
                onDeleteExpense = { viewModel.deleteExpense(it) }
            )
        }
    }

    if (isAddAccountDialogOpen) {
        AddAccountDialog(
            onDismiss = { isAddAccountDialogOpen = false },
            onConfirm = { cat, name, masked, bal, unit, tier ->
                viewModel.addConnectedAccount(cat, name, masked, bal, unit, tier)
                isAddAccountDialogOpen = false
            }
        )
    }

    if (isAddExpenseDialogOpen) {
        AddExpenseDialog(
            onDismiss = { isAddExpenseDialogOpen = false },
            onConfirm = { title, cat, amount, curr, paidBy, notes ->
                viewModel.addExpense(title, cat, amount, curr, paidBy, notes)
                isAddExpenseDialogOpen = false
            }
        )
    }

    accountBeingEdited?.let { account ->
        EditAccountDialog(
            account = account,
            onDismiss = { accountBeingEdited = null },
            onConfirm = { balance, tier ->
                viewModel.updateConnectedAccount(account.copy(balanceValue = balance, tierStatus = tier))
                accountBeingEdited = null
            }
        )
    }
}

@Composable
fun AccountsAndRewardsView(
    accounts: List<ConnectedAccountEntity>,
    isOptimizing: Boolean,
    optimizerResult: String?,
    onRunOptimizer: () -> Unit,
    onAddAccount: () -> Unit,
    onDeleteAccount: (Long) -> Unit,
    onEditAccount: (ConnectedAccountEntity) -> Unit = {}
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // AI Optimizer Banner Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Navy900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                    .background(AmberGold.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, null, tint = AmberGold, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI Rewards & Timeshare Optimizer",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextAtlasPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Gemini analyzes all connected airline miles, hotel points, timeshare trading power (RCI/II), and credit card rewards to calculate optimal redemption sweet spots.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextAtlasPrimary.copy(alpha = 0.85f)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onRunOptimizer,
                        enabled = !isOptimizing,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LuxuryCardElevated,
                            contentColor = TextPrimary
                        ),
                        border = BorderStroke(1.dp, LuxuryBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isOptimizing) {
                            CircularProgressIndicator(
                                 modifier = Modifier.size(18.dp),
                                 color = ChampagneGold,
                                 strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Calculating Point Valuations...", color = TextPrimary, fontWeight = FontWeight.Medium)
                        } else {
                            Icon(Icons.Default.Sync, null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Calculate Optimal Redemption Plan", color = TextPrimary, fontWeight = FontWeight.Medium)
                        }
                    }

                    optimizerResult?.let { result ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LuxurySurface,
                            border = BorderStroke(1.dp, LuxuryBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = result,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextPrimary,
                                    lineHeight = 18.sp
                                ),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Header with Add Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Connected Portfolios",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = onAddAccount,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LuxuryCardElevated,
                        contentColor = TextPrimary
                    ),
                    border = BorderStroke(1.dp, LuxuryBorder)
                ) {
                    Icon(Icons.Default.Add, null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Link Account", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Account list
        if (accounts.isNotEmpty()) {
            items(accounts, key = { it.id }) { acc ->
                AccountItemCard(
                    account = acc,
                    onDelete = { onDeleteAccount(acc.id) },
                    onEdit = { onEditAccount(acc) }
                )
            }
        } else {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = OceanBlue,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Connected Loyalty Accounts",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Link your airline miles, hotel points, timeshare contracts, or credit cards to let Marco optimize your redemptions.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun AccountItemCard(
    account: ConnectedAccountEntity,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, ContourBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIconBadge(category = account.categoryType, size = 42)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = account.providerName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = run {
                            val tierLabel = account.tierStatus.ifBlank { "Tier not set" }
                            if (account.accountNumberMasked.isBlank()) {
                                tierLabel
                            } else {
                                "${account.accountNumberMasked} • $tierLabel"
                            }
                        },
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    if (account.balanceValue.isNotBlank()) {
                        Text(
                            text = "${account.balanceValue} ${account.unitLabel}",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    } else {
                        Text(
                            text = "Tap to add balance (${account.unitLabel})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = ChampagneGold
                            )
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (account.rewardsEstimatedValuationUsd > 0.0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LuxuryCardElevated,
                        border = BorderStroke(1.dp, LuxuryBorder)
                    ) {
                        Text(
                            text = "≈ $${account.rewardsEstimatedValuationUsd.toInt()}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LuxuryCardElevated,
                        border = BorderStroke(1.dp, LuxuryBorder)
                    ) {
                        Text(
                            text = "Linked",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(24.dp)
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ExpensesAndWalletView(
    expenses: List<ExpenseEntity>,
    onAddExpense: () -> Unit,
    onDeleteExpense: (Long) -> Unit
) {
    val totalSpentUsd = expenses.sumOf { it.amountUsd }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Digital Contactless Card Simulation
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CartographyCardElevated),
                border = BorderStroke(1.dp, ContourBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Marco Multi-Currency Pass",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Icon(Icons.Default.Nfc, null, tint = AmberGold, modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Total recorded spend",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$${String.format("%.2f", totalSpentUsd)} USD",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextAtlasPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Text(
                            text = "Zero FX Fee Auto-Lock",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextAtlasPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        // Action Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Expense Receipts (${expenses.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Button(
                    onClick = onAddExpense,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OceanBlue,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Receipt, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Expense", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Expense items
        if (expenses.isNotEmpty()) {
            items(expenses, key = { it.id }) { exp ->
                ExpenseItemCard(expense = exp, onDelete = { onDeleteExpense(exp.id) })
            }
        } else {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = null,
                            tint = OceanBlue,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Expenses Logged",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Log Expense' to track multi-currency expenses, exchange rates, and points savings on your trip.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun ExpenseItemCard(
    expense: ExpenseEntity,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, ContourBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIconBadge(category = expense.category, size = 36)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${expense.category} • Paid with ${expense.paidBy}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${expense.amountOriginal} ${expense.originalCurrency}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    if (expense.originalCurrency != "USD") {
                        Text(
                            text = "≈ $${String.format("%.2f", expense.amountUsd)} USD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = EmeraldGreen
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(24.dp)
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (category: String, name: String, masked: String, balance: String, unit: String, tier: String) -> Unit
) {
    var category by remember { mutableStateOf("AIRLINE") }
    var name by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("pts") }
    var tier by remember { mutableStateOf("Gold Status") }

    val categories = listOf("AIRLINE", "HOTEL", "TIMESHARE", "CREDIT_CARD", "CAMPING", "OTA", "TRANSIT")

    Dialog(onDismissRequest = onDismiss) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Link Travel / Loyalty Account", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category picker
            Text("Program Type:", style = MaterialTheme.typography.labelSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = {
                            category = cat
                            unit = when (cat) {
                                "AIRLINE" -> "miles"
                                "TIMESHARE" -> "TPU Trading Power"
                                "CAMPING" -> "Active Pass"
                                else -> "pts"
                            }
                        },
                        label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Provider Name (e.g. Hyatt, RCI, Delta)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = accountNumber, onValueChange = { accountNumber = it }, label = { Text("Account / Masked Number") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = balance, onValueChange = { balance = it }, label = { Text("Balance") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit (miles/pts/weeks)") }, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = tier, onValueChange = { tier = it }, label = { Text("Tier Status (Platinum/Diamond/Standard)") }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isNotBlank() && balance.isNotBlank()) {
                        onConfirm(category, name, accountNumber, balance, unit, tier)
                    }
                },
                enabled = name.isNotBlank() && balance.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OceanBlue,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sync account to Marco", fontWeight = FontWeight.Bold)
            }
        }
    }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, category: String, amount: Double, currency: String, paidBy: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
    var paidBy by remember { mutableStateOf("Chase Sapphire Reserve") }
    var notes by remember { mutableStateOf("") }

    val categories = listOf("Food", "Flights", "Lodging", "Transit", "Activities", "Timeshare Fees", "Shopping")
    val currencies = listOf("USD", "EUR", "JPY", "GBP", "CAD", "CHF", "AUD")

    Dialog(onDismissRequest = onDismiss) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Log Travel Expense", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Expense Title (e.g. Sushi Dinner, Rental Car)") }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1.5f)
                )

                // Currency selector
                Box(modifier = Modifier.weight(1f)) {
                    var isExpanded by remember { mutableStateOf(false) }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable { isExpanded = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(currency, fontWeight = FontWeight.Bold)
                        }
                    }
                    DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
                        currencies.forEach { c ->
                            DropdownMenuItem(text = { Text(c) }, onClick = {
                                currency = c
                                isExpanded = false
                            })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Category:", style = MaterialTheme.typography.labelSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = paidBy, onValueChange = { paidBy = it }, label = { Text("Paid By / Card Used") }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amount > 0) {
                        onConfirm(title, category, amount, currency, paidBy, notes)
                    }
                },
                enabled = title.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OceanBlue,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Expense to Ledger", fontWeight = FontWeight.Bold)
            }
        }
    }
    }
}

// -------------------------------------------------------------
// EDIT CONNECTED ACCOUNT: BALANCE + REAL TIER (never a guessed default)
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditAccountDialog(
    account: ConnectedAccountEntity,
    onDismiss: () -> Unit,
    onConfirm: (balance: String, tier: String) -> Unit
) {
    var balance by remember { mutableStateOf(account.balanceValue) }
    var tier by remember { mutableStateOf(account.tierStatus) }

    // If this provider matches a known loyalty catalog entry, offer its real tier list so we're
    // never inventing a status name either — otherwise fall back to free text (manually-added
    // accounts aren't in the catalog).
    val catalogProgram = remember(account.providerName) {
        PreferenceConstants.LOYALTY_CATALOG.find { it.name == account.providerName }
    }

    Dialog(onDismissRequest = onDismiss) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(account.providerName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = balance,
                onValueChange = { balance = it },
                label = { Text("Balance (${account.unitLabel})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_account_balance_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Your status:", style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(4.dp))

            if (catalogProgram != null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    catalogProgram.tierOptions.forEach { option ->
                        FilterChip(
                            selected = tier == option,
                            onClick = { tier = if (tier == option) "" else option },
                            label = { Text(option, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                if (tier.isBlank()) {
                    Text(
                        text = "Tier not set",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                OutlinedTextField(
                    value = tier,
                    onValueChange = { tier = it },
                    label = { Text("Tier / Status (leave blank if not set)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onConfirm(balance, tier) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LuxuryCardElevated,
                    contentColor = TextPrimary
                ),
                border = BorderStroke(1.dp, LuxuryBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_account_save_button")
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }
    }
    }
}
