package com.example.data.repository

import com.example.data.local.TravelDao
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ConnectedAccountEntity
import com.example.data.model.CurrencyRateEntity
import com.example.data.model.EmergencyAlertEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.GroupMemoryEntity
import com.example.data.model.ProactiveSuggestionEntity
import com.example.data.model.TripActivityEntity
import com.example.data.model.TripEntity
import com.example.data.model.TripFeedbackEntity
import com.example.data.model.UserPreferenceEntity
import com.example.data.model.VendorCallLogEntity
import com.example.data.model.WalletBalanceEntity
import com.example.data.model.WalletTransactionEntity
import com.example.data.security.WalletSecurityManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class TravelRepository(private val travelDao: TravelDao) {

    val allTrips: Flow<List<TripEntity>> = travelDao.getAllTrips()
    val connectedAccounts: Flow<List<ConnectedAccountEntity>> = travelDao.getAllConnectedAccounts()
    val allAlerts: Flow<List<EmergencyAlertEntity>> = travelDao.getAllAlerts()
    val allVendorCalls: Flow<List<VendorCallLogEntity>> = travelDao.getAllVendorCalls()
    val chatMessages: Flow<List<ChatMessageEntity>> = travelDao.getChatMessages()
    val userPreferences: Flow<UserPreferenceEntity?> = travelDao.getUserPreferences()
    val allTripFeedbacks: Flow<List<TripFeedbackEntity>> = travelDao.getAllTripFeedbacks()
    val proactiveSuggestions: Flow<List<ProactiveSuggestionEntity>> = travelDao.getAllProactiveSuggestions()
    val allWalletBalances: Flow<List<WalletBalanceEntity>> = travelDao.getAllWalletBalances()
    val allWalletTransactions: Flow<List<WalletTransactionEntity>> = travelDao.getAllWalletTransactions()
    val allCurrencyRates: Flow<List<CurrencyRateEntity>> = travelDao.getAllCurrencyRates()

    fun getTripById(tripId: Long): Flow<TripEntity?> = travelDao.getTripById(tripId)
    fun getActivitiesForTrip(tripId: Long): Flow<List<TripActivityEntity>> = travelDao.getActivitiesForTrip(tripId)
    fun getExpensesForTrip(tripId: Long): Flow<List<ExpenseEntity>> = travelDao.getExpensesForTrip(tripId)
    fun getMemoriesForTrip(tripId: Long): Flow<List<GroupMemoryEntity>> = travelDao.getMemoriesForTrip(tripId)
    fun getFeedbacksForTrip(tripId: Long): Flow<List<TripFeedbackEntity>> = travelDao.getFeedbacksForTrip(tripId)
    fun getWalletBalancesForTrip(tripId: Long): Flow<List<WalletBalanceEntity>> = travelDao.getWalletBalancesForTrip(tripId)
    fun getWalletTransactionsForTrip(tripId: Long): Flow<List<WalletTransactionEntity>> = travelDao.getWalletTransactionsForTrip(tripId)

    suspend fun getUserPreferencesSync(): UserPreferenceEntity? = travelDao.getUserPreferencesSync()
    suspend fun saveUserPreferences(preferences: UserPreferenceEntity) = travelDao.insertOrUpdateUserPreferences(preferences)

    suspend fun insertTripFeedback(feedback: TripFeedbackEntity): Long = travelDao.insertTripFeedback(feedback)
    suspend fun deleteTripFeedback(feedbackId: Long) = travelDao.deleteTripFeedback(feedbackId)

    suspend fun updateProactiveSuggestions(suggestions: List<ProactiveSuggestionEntity>) {
        travelDao.clearProactiveSuggestions()
        travelDao.insertProactiveSuggestions(suggestions)
    }

    suspend fun insertTrip(trip: TripEntity): Long = travelDao.insertTrip(trip)
    suspend fun updateTrip(trip: TripEntity) = travelDao.updateTrip(trip)
    suspend fun updateTripStatus(tripId: Long, status: String) = travelDao.updateTripStatus(tripId, status)
    suspend fun deleteTrip(tripId: Long) = travelDao.deleteTrip(tripId)

    suspend fun insertActivity(activity: TripActivityEntity): Long = travelDao.insertActivity(activity)
    suspend fun insertActivities(activities: List<TripActivityEntity>) = travelDao.insertActivities(activities)
    suspend fun updateActivity(activity: TripActivityEntity) = travelDao.updateActivity(activity)
    suspend fun deleteActivity(activityId: Long) = travelDao.deleteActivity(activityId)
    suspend fun clearActivitiesForTrip(tripId: Long) = travelDao.clearActivitiesForTrip(tripId)

    suspend fun insertAccount(account: ConnectedAccountEntity): Long = travelDao.insertAccount(account)
    suspend fun updateAccount(account: ConnectedAccountEntity) = travelDao.updateAccount(account)
    suspend fun deleteAccount(accountId: Long) = travelDao.deleteAccount(accountId)

    suspend fun insertExpense(expense: ExpenseEntity): Long = travelDao.insertExpense(expense)
    suspend fun deleteExpense(expenseId: Long) = travelDao.deleteExpense(expenseId)

    suspend fun insertVendorCall(callLog: VendorCallLogEntity): Long = travelDao.insertVendorCall(callLog)

    suspend fun insertAlert(alert: EmergencyAlertEntity): Long = travelDao.insertAlert(alert)

    suspend fun insertMemory(memory: GroupMemoryEntity): Long = travelDao.insertMemory(memory)
    suspend fun incrementMemoryLikes(memoryId: Long) = travelDao.incrementMemoryLikes(memoryId)

    suspend fun insertChatMessage(message: ChatMessageEntity): Long = travelDao.insertChatMessage(message)
    suspend fun clearChatMessages() = travelDao.clearChatMessages()

    // Marco Wallet Balances & Encrypted Transactions
    suspend fun insertWalletBalance(balance: WalletBalanceEntity): Long = travelDao.insertWalletBalance(balance)
    suspend fun updateWalletBalance(balance: WalletBalanceEntity) = travelDao.updateWalletBalance(balance)
    suspend fun insertWalletTransaction(transaction: WalletTransactionEntity): Long = travelDao.insertWalletTransaction(transaction)
    suspend fun deleteWalletTransaction(transactionId: Long) = travelDao.deleteWalletTransaction(transactionId)
    suspend fun insertCurrencyRates(rates: List<CurrencyRateEntity>) = travelDao.insertCurrencyRates(rates)

    suspend fun clearAllLocalData() {
        travelDao.clearAllTrips()
        travelDao.clearAllActivities()
        travelDao.clearAccounts()
        travelDao.clearExpenses()
        travelDao.clearVendorCalls()
        travelDao.clearAlerts()
        travelDao.clearMemories()
        travelDao.clearChatMessages()
        travelDao.clearTripFeedbacks()
        travelDao.clearProactiveSuggestions()
        travelDao.clearUserPreferences()
        travelDao.clearAllWalletBalances()
        travelDao.clearAllWalletTransactions()
    }

    suspend fun checkAndSeedInitialData() {
        // Seed baseline live currency exchange rates if empty
        val currentRates = travelDao.getAllCurrencyRates().firstOrNull()
        if (currentRates.isNullOrEmpty()) {
            val currencyRates = listOf(
                CurrencyRateEntity(
                    currencyCode = "EUR",
                    baseCurrency = "USD",
                    rateAgainstBase = 0.921,
                    inverseRate = 1.085,
                    dayChangePercent = -0.18,
                    countryFlag = "🇪🇺",
                    feeAvoidanceTip = "Pay in local Euros (€) to bypass dynamic currency conversion markup (save 3-5%)."
                ),
                CurrencyRateEntity(
                    currencyCode = "JPY",
                    baseCurrency = "USD",
                    rateAgainstBase = 149.25,
                    inverseRate = 0.0067,
                    dayChangePercent = +0.42,
                    countryFlag = "🇯🇵",
                    feeAvoidanceTip = "Yen is at multi-year favorable value vs USD; ideal time to preload digital Suica passes."
                ),
                CurrencyRateEntity(
                    currencyCode = "GBP",
                    baseCurrency = "USD",
                    rateAgainstBase = 0.785,
                    inverseRate = 1.274,
                    dayChangePercent = +0.05,
                    countryFlag = "🇬🇧",
                    feeAvoidanceTip = "Contactless transit taps in London automatically cap fares at daily limits."
                ),
                CurrencyRateEntity(
                    currencyCode = "CHF",
                    baseCurrency = "USD",
                    rateAgainstBase = 0.875,
                    inverseRate = 1.142,
                    dayChangePercent = -0.12,
                    countryFlag = "🇨🇭",
                    feeAvoidanceTip = "Swiss Rail Passes & half-fare cards save 50% on all mountain funiculars."
                ),
                CurrencyRateEntity(
                    currencyCode = "CAD",
                    baseCurrency = "USD",
                    rateAgainstBase = 1.358,
                    inverseRate = 0.736,
                    dayChangePercent = +0.10,
                    countryFlag = "🇨🇦",
                    feeAvoidanceTip = "US Credit cards with 0% foreign transaction fee waive all Canadian exchange penalties."
                ),
                CurrencyRateEntity(
                    currencyCode = "AUD",
                    baseCurrency = "USD",
                    rateAgainstBase = 1.520,
                    inverseRate = 0.658,
                    dayChangePercent = -0.25,
                    countryFlag = "🇦🇺",
                    feeAvoidanceTip = "Tipping is not customary in Australia; prices display all taxes inclusive."
                )
            )
            travelDao.insertCurrencyRates(currencyRates)
        }
    }
}
