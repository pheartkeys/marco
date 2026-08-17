package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val destination: String,
    val countryCode: String = "US",
    val startDate: String,
    val endDate: String,
    val budgetTotal: Double = 3500.0,
    val budgetSpent: Double = 1420.0,
    val primaryCurrency: String = "USD",
    val travelersCount: Int = 2,
    val childrenCount: Int = 0,
    val accessibilityRequirements: String = "Wheelchair Step-Free & ADA Pool Lift",
    val dietaryRestrictions: String = "Gluten-Free & Nut-Free Aware",
    val familyAgeBrackets: String = "Toddler (2-3) & Teen (14-16), 2 Adults",
    val travelStyle: String = "Smart Points & Luxury",
    val timeshareExchangeDetails: String = "",
    val isOfflineSynced: Boolean = true,
    val lastSyncedTimestamp: Long = System.currentTimeMillis(),
    val heroThemeIndex: Int = 0,
    val status: String = "PLANNING" // "PLANNING", "IN_PROGRESS", "COMPLETED"
)

enum class TripStatus(val value: String, val label: String, val badgeEmoji: String) {
    PLANNING("PLANNING", "Planning Stage", "📋"),
    IN_PROGRESS("IN_PROGRESS", "On Trip / Live", "🧭"),
    COMPLETED("COMPLETED", "Completed", "🏁");

    companion object {
        fun fromString(value: String): TripStatus {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: PLANNING
        }
    }
}

fun TripEntity.isTripInProgress(): Boolean {
    if (status.equals("IN_PROGRESS", ignoreCase = true) ||
        status.equals("ACTIVE", ignoreCase = true) ||
        status.equals("ON_TRIP", ignoreCase = true)) {
        return true
    }
    if (status.equals("COMPLETED", ignoreCase = true) ||
        status.equals("PAST", ignoreCase = true)) {
        return false
    }
    if (status.equals("PLANNING", ignoreCase = true)) {
        return false
    }
    return try {
        val now = java.time.LocalDate.now()
        val formats = listOf(
            java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy", java.util.Locale.US),
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd", java.util.Locale.US),
            java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy", java.util.Locale.US)
        )
        var start: java.time.LocalDate? = null
        var end: java.time.LocalDate? = null
        for (fmt in formats) {
            if (start == null) try { start = java.time.LocalDate.parse(startDate.trim(), fmt) } catch (_: Exception) {}
            if (end == null) try { end = java.time.LocalDate.parse(endDate.trim(), fmt) } catch (_: Exception) {}
        }
        if (start != null && end != null) {
            !now.isBefore(start) && !now.isAfter(end)
        } else false
    } catch (_: Exception) {
        false
    }
}

@Entity(tableName = "trip_activities")
data class TripActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val dayNumber: Int,
    val timeSlot: String,
    val title: String,
    val category: String, // FLIGHT, HOTEL, TIMESHARE, CAMPGROUND, TRANSIT, DINING, ATTRACTION, FAMILY_KIDS
    val location: String,
    val confirmationCode: String = "",
    val notes: String = "",
    val cost: Double = 0.0,
    val currency: String = "USD",
    val accessibilityBadge: String = "Wheelchair & Stroller Accessible",
    val vendorName: String = "",
    val vendorPhone: String = "",
    val isCompleted: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

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

@Entity(tableName = "expenses")
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

@Entity(tableName = "vendor_call_logs")
data class VendorCallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val vendorName: String,
    val vendorPhone: String,
    val inquiryTopic: String,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val status: String = "COMPLETED", // COMPLETED, IN_PROGRESS, SCHEDULED
    val audioTranscript: String,
    val callSummaryOutcome: String,
    val confirmedDetails: String
)

@Entity(tableName = "emergency_alerts")
data class EmergencyAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val alertType: String, // SAFETY_ADVISORY, WEATHER_ALERT, MEDICAL_ASSISTANCE, EMBASSY_INFO, FLIGHT_UPDATE
    val title: String,
    val description: String,
    val severity: String, // CRITICAL, WARNING, INFO
    val actionContact: String,
    val location: String,
    val timestamp: String = "Today"
)

@Entity(tableName = "group_memories")
data class GroupMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val authorName: String,
    val caption: String,
    val locationTag: String,
    val timestamp: String,
    val mediaType: String = "PHOTO",
    val photoGradientColor: Long = 0xFF0284C7,
    val likesCount: Int = 3,
    val aiTag: String = "Sunset Vista"
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long = 1,
    val sender: String, // USER, CONCIERGE_AI, VOICE_CALL_DISPATCHER
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionChipText: String = "",
    val suggestedActionJson: String = ""
)

@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
    @PrimaryKey val id: Long = 1,
    val preferredAirlines: String = "Delta Air Lines, ANA, United",
    val preferredHotelTypes: String = "Oceanfront Timeshare Villa, Boutique Luxury, Historic Ryokan",
    val activityLevel: String = "Balanced & Moderate (Sensory & Rest Intervals)", // Relaxed / Slow-Paced, Balanced & Moderate, High Intensity & Adventurous
    val preferredTravelStyle: String = "Smart Points Maximizer & Scenic Culture",
    val dietaryPreferences: String = "Gluten-Free, Nut-Free, Child-Friendly Seating",
    val wheelchairRequirements: String = "Step-Free Access, Roll-in Shower, ADA Pool Chair Lift",
    val familyAgeBrackets: String = "Toddler (2-3) & Teen (14-16), 2 Adults",
    val sensoryAndMobilityNotes: String = "Stroller accessible, quiet sensory rest zones, elevator guaranteed",
    val pacingPreference: String = "Morning Activities (8am-12pm) with Leisure Afternoons",
    val preferredTransit: String = "Accessible SUV & Scenic Rail",
    val preferredClimate: String = "Tropical Coast & Alpine Scenic",
    val learnedInsightsSummary: String = "AI Analysis: Traveler values quiet oceanfront views, priority family boarding on Delta flights, step-free access, and mid-afternoon pool breaks. Highly receptive to RCI timeshare upgrades and Chase UR transfers to Hyatt.",
    val totalTripsAnalyzed: Int = 3,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "trip_feedbacks")
data class TripFeedbackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val tripTitle: String,
    val destination: String,
    val rating: Int = 5, // 1 to 5 stars
    val likedAspects: String = "Quiet oceanfront villa, Delta first class upgrade, stroller accessibility",
    val dislikedAspects: String = "High altitude wind at summit",
    val feedbackNotes: String = "The Grand Champions heated pool and early morning Haleakala sunrise were incredible highlights. Keep suggesting villas with full kitchens.",
    val dateSubmitted: String = "Oct 20, 2026",
    val learnedActionableTakeaway: String = "AI learned: Prioritize lodging with full kitchens and quiet heated pools."
)

@Entity(tableName = "proactive_suggestions")
data class ProactiveSuggestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val destination: String,
    val countryCode: String,
    val durationDays: Int,
    val estimatedBudget: Double,
    val currency: String = "USD",
    val matchScorePercent: Int = 98,
    val rationale: String,
    val suggestedAirline: String,
    val suggestedLodging: String,
    val activityPace: String,
    val heroTag: String = "AI Tailored Match",
    val pointsSavingsUsd: Double = 1850.0,
    val season: String = "Spring / Autumn Peak"
)

/**
 * Secure encrypted multi-currency travel budget balance for the Marco wallet
 */
@Entity(tableName = "wallet_balances")
data class WalletBalanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long = 1,
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
 * Secure transaction history with category tagging, currency conversion, and loyalty points savings attribution
 */
@Entity(tableName = "wallet_transactions")
data class WalletTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long = 1,
    val title: String,
    val category: String, // Lodging, Dining, Flights, Activities, Transit, Timeshare, Shopping, Groceries
    val amountOriginal: Double,
    val currencyCode: String,
    val amountUsd: Double,
    val exchangeRate: Double = 1.0,
    val paymentMethod: String, // Chase Sapphire Reserve, Amex Platinum, Apple Pay, Cash, Loyalty Points
    val loyaltyProgramApplied: String = "", // Delta SkyMiles, Marriott Bonvoy, RCI TPU, Chase UR
    val loyaltySavingsUsd: Double = 0.0, // Dollar value saved via points or certificates
    val dateString: String = "Today",
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val encryptedReceiptHash: String = "", // Secure encrypted receipt/verification token
    val isVerified: Boolean = true
)

/**
 * Live / Cached Currency Conversion Rates with volatility indicators
 */
@Entity(tableName = "currency_rates")
data class CurrencyRateEntity(
    @PrimaryKey val currencyCode: String, // EUR, JPY, GBP, CHF, CAD, AUD
    val baseCurrency: String = "USD",
    val rateAgainstBase: Double,
    val inverseRate: Double,
    val dayChangePercent: Double = 0.0,
    val countryFlag: String,
    val feeAvoidanceTip: String = "0% foreign transaction fees active via Chase Sapphire"
)
