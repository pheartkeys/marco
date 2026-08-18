package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ============================================================================================
 * PRIVATE FINANCIAL MODELS — NEVER SHARED WITH A TRIP
 * ============================================================================================
 *
 * Everything in this file describes *one person's own money*: what their accounts hold, what a
 * program is worth to them, what their card was charged. None of it may be written to a shared
 * `/trips/{tripId}` document. `firestore.rules` enforces this independently of app code, and the
 * shared-collection allowlists there do not contain any field name declared in this file.
 *
 * The one thing that may cross the line is a program's **title and type** — "Marriott Bonvoy",
 * "HOTEL" — never its balance or valuation. Use
 * [com.example.data.model.toShareableProgramRef] to cross that line; it is the only sanctioned
 * path, and it exists so the safe move is also the short one.
 *
 * Group-visible money (what an expense cost, who paid, what the group agreed a contribution is
 * worth) lives in `LedgerModels.kt` and `ContributionModels.kt`, not here.
 */

/**
 * A linked loyalty / timeshare / card account. PRIVATE.
 *
 * [providerName] and [categoryType] are the only shareable fields, and only via
 * [toShareableProgramRef]. [balanceValue], [rewardsEstimatedValuationUsd], [exchangePowerScore],
 * [tierStatus] and [accountNumberMasked] are private without exception.
 */
@Entity(tableName = "connected_accounts")
data class ConnectedAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryType: String, // AIRLINE, HOTEL, TIMESHARE, CREDIT_CARD, CAMPING, OTA, TRANSIT
    val providerName: String,
    val accountNumberMasked: String,
    val balanceValue: String,
    val unitLabel: String, // miles, pts, weeks, nights, credits
    val tierStatus: String, // Diamond, Platinum Elite, President's Club, Standard
    val rewardsEstimatedValuationUsd: Double,
    val exchangePowerScore: Int = 0, // for timeshares like RCI Trading Power 32 TPU
    val lastSyncTime: String = "Just now"
)

/** A personal expense record (pre-dates the group ledger). PRIVATE. */
@Entity(tableName = "expenses", indices = [Index("tripId")])
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val title: String,
    val category: String, // Flights, Lodging, Food, Transit, Activities, Shopping, Timeshare Fees
    val amountOriginal: Double,
    val originalCurrency: String,
    val amountUsd: Double,
    val exchangeRate: Double = 1.0,
    val paidBy: String = "You",
    val date: String,
    val notes: String = "",
    val isSynced: Boolean = true
)

/** Secure encrypted multi-currency travel budget balance for the Marco wallet. PRIVATE. */
@Entity(tableName = "wallet_balances", indices = [Index("tripId")])
data class WalletBalanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long = 0,
    val currencyCode: String, // USD, EUR, JPY, GBP, CHF, CAD, AUD
    val currencySymbol: String = "$",
    val currencyName: String,
    val allocatedBudget: Double,
    val availableBalance: Double,
    val spentAmount: Double,
    val exchangeRateToUsd: Double = 1.0,
    val encryptedAccountDetails: String = "", // Encrypted via WalletSecurityManager
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

/**
 * Secure transaction history with category tagging, currency conversion, and loyalty points
 * savings attribution. PRIVATE.
 */
@Entity(tableName = "wallet_transactions", indices = [Index("tripId")])
data class WalletTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long = 0,
    val title: String,
    val category: String, // Lodging, Dining, Flights, Activities, Transit, Timeshare, Shopping, Groceries
    val amountOriginal: Double,
    val currencyCode: String,
    val amountUsd: Double,
    val exchangeRate: Double = 1.0,
    val paymentMethod: String, // Payment method or card
    val loyaltyProgramApplied: String = "", // Loyalty program used
    val loyaltySavingsUsd: Double = 0.0, // Dollar value saved via points or certificates
    val dateString: String = "Today",
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val encryptedReceiptHash: String = "", // Secure encrypted receipt/verification token
    val isVerified: Boolean = true
)

/**
 * Live / cached currency conversion rates with volatility indicators.
 *
 * Not personal data, but note that the rows seeded by
 * `TravelRepository.checkAndSeedInitialData()` are stale hardcoded constants; Track C replaces
 * them with a live FX supplier. Anything rendering a converted figure should surface the rate's
 * age rather than implying it is current.
 */
@Entity(tableName = "currency_rates")
data class CurrencyRateEntity(
    @PrimaryKey val currencyCode: String, // EUR, JPY, GBP, CHF, CAD, AUD
    val baseCurrency: String = "USD",
    val rateAgainstBase: Double,
    val inverseRate: Double,
    val dayChangePercent: Double = 0.0,
    val countryFlag: String,
    val feeAvoidanceTip: String = "Pay in local currency to avoid dynamic conversion fees"
)
