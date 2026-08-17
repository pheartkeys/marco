package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ConnectedAccountEntity
import com.example.data.model.EmergencyAlertEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.GroupMemoryEntity
import com.example.data.model.ProactiveSuggestionEntity
import com.example.data.model.TripActivityEntity
import com.example.data.model.TripEntity
import com.example.data.model.CurrencyRateEntity
import com.example.data.model.TripFeedbackEntity
import com.example.data.model.UserPreferenceEntity
import com.example.data.model.VendorCallLogEntity
import com.example.data.model.WalletBalanceEntity
import com.example.data.model.WalletTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TravelDao {

    // Trips
    @Query("SELECT * FROM trips ORDER BY id DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :tripId LIMIT 1")
    fun getTripById(tripId: Long): Flow<TripEntity?>

    @Query("SELECT * FROM trips LIMIT 1")
    suspend fun getFirstTrip(): TripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity): Long

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Query("UPDATE trips SET status = :status WHERE id = :tripId")
    suspend fun updateTripStatus(tripId: Long, status: String)

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteTrip(tripId: Long)

    @Query("DELETE FROM trips")
    suspend fun clearAllTrips()

    @Query("DELETE FROM trip_activities")
    suspend fun clearAllActivities()

    // Activities
    @Query("SELECT * FROM trip_activities WHERE tripId = :tripId ORDER BY dayNumber ASC, id ASC")
    fun getActivitiesForTrip(tripId: Long): Flow<List<TripActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: TripActivityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<TripActivityEntity>)

    @Update
    suspend fun updateActivity(activity: TripActivityEntity)

    @Query("DELETE FROM trip_activities WHERE id = :activityId")
    suspend fun deleteActivity(activityId: Long)

    @Query("DELETE FROM trip_activities WHERE tripId = :tripId")
    suspend fun clearActivitiesForTrip(tripId: Long)

    // Connected Accounts (Airlines, Hotels, Timeshares, Cards, Camping)
    @Query("SELECT * FROM connected_accounts ORDER BY id ASC")
    fun getAllConnectedAccounts(): Flow<List<ConnectedAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: ConnectedAccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<ConnectedAccountEntity>)

    @Update
    suspend fun updateAccount(account: ConnectedAccountEntity)

    @Query("DELETE FROM connected_accounts WHERE id = :accountId")
    suspend fun deleteAccount(accountId: Long)

    @Query("DELETE FROM connected_accounts")
    suspend fun clearAccounts()

    // Expenses
    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY id DESC")
    fun getExpensesForTrip(tripId: Long): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpense(expenseId: Long)

    @Query("DELETE FROM expenses")
    suspend fun clearExpenses()

    // Vendor Call Logs
    @Query("SELECT * FROM vendor_call_logs ORDER BY dateTimestamp DESC")
    fun getAllVendorCalls(): Flow<List<VendorCallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVendorCall(callLog: VendorCallLogEntity): Long

    @Query("DELETE FROM vendor_call_logs")
    suspend fun clearVendorCalls()

    // Emergency Alerts & Safety
    @Query("SELECT * FROM emergency_alerts ORDER BY id DESC")
    fun getAllAlerts(): Flow<List<EmergencyAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: EmergencyAlertEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlerts(alerts: List<EmergencyAlertEntity>)

    @Query("DELETE FROM emergency_alerts")
    suspend fun clearAlerts()

    // Group Memories
    @Query("SELECT * FROM group_memories WHERE tripId = :tripId ORDER BY id DESC")
    fun getMemoriesForTrip(tripId: Long): Flow<List<GroupMemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: GroupMemoryEntity): Long

    @Query("UPDATE group_memories SET likesCount = likesCount + 1 WHERE id = :memoryId")
    suspend fun incrementMemoryLikes(memoryId: Long)

    @Query("DELETE FROM group_memories")
    suspend fun clearMemories()

    // Chat Messages
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getChatMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE chatType = :chatType ORDER BY timestamp ASC")
    fun getChatMessagesByType(chatType: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE tripId = :tripId AND chatType = :chatType ORDER BY timestamp ASC")
    fun getChatMessagesByTripAndType(tripId: Long, chatType: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatMessages()

    // User Preferences & Learning
    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    fun getUserPreferences(): Flow<UserPreferenceEntity?>

    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    suspend fun getUserPreferencesSync(): UserPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUserPreferences(preferences: UserPreferenceEntity)

    @Query("DELETE FROM user_preferences")
    suspend fun clearUserPreferences()

    // Trip Feedback
    @Query("SELECT * FROM trip_feedbacks ORDER BY id DESC")
    fun getAllTripFeedbacks(): Flow<List<TripFeedbackEntity>>

    @Query("SELECT * FROM trip_feedbacks WHERE tripId = :tripId ORDER BY id DESC")
    fun getFeedbacksForTrip(tripId: Long): Flow<List<TripFeedbackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTripFeedback(feedback: TripFeedbackEntity): Long

    @Query("DELETE FROM trip_feedbacks WHERE id = :feedbackId")
    suspend fun deleteTripFeedback(feedbackId: Long)

    @Query("DELETE FROM trip_feedbacks")
    suspend fun clearTripFeedbacks()

    // Proactive Suggestions
    @Query("SELECT * FROM proactive_suggestions ORDER BY matchScorePercent DESC, id ASC")
    fun getAllProactiveSuggestions(): Flow<List<ProactiveSuggestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProactiveSuggestions(suggestions: List<ProactiveSuggestionEntity>)

    @Query("DELETE FROM proactive_suggestions")
    suspend fun clearProactiveSuggestions()

    // Marco Secure Multi-Currency Wallet Balances
    @Query("SELECT * FROM wallet_balances WHERE tripId = :tripId ORDER BY id ASC")
    fun getWalletBalancesForTrip(tripId: Long): Flow<List<WalletBalanceEntity>>

    @Query("SELECT * FROM wallet_balances ORDER BY id ASC")
    fun getAllWalletBalances(): Flow<List<WalletBalanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWalletBalance(balance: WalletBalanceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWalletBalances(balances: List<WalletBalanceEntity>)

    @Update
    suspend fun updateWalletBalance(balance: WalletBalanceEntity)

    @Query("DELETE FROM wallet_balances WHERE tripId = :tripId")
    suspend fun clearWalletBalancesForTrip(tripId: Long)

    @Query("DELETE FROM wallet_balances")
    suspend fun clearAllWalletBalances()

    // Marco Wallet Transactions & Expense Attribution
    @Query("SELECT * FROM wallet_transactions WHERE tripId = :tripId ORDER BY timestamp DESC")
    fun getWalletTransactionsForTrip(tripId: Long): Flow<List<WalletTransactionEntity>>

    @Query("SELECT * FROM wallet_transactions ORDER BY timestamp DESC")
    fun getAllWalletTransactions(): Flow<List<WalletTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWalletTransaction(transaction: WalletTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWalletTransactions(transactions: List<WalletTransactionEntity>)

    @Query("DELETE FROM wallet_transactions WHERE id = :transactionId")
    suspend fun deleteWalletTransaction(transactionId: Long)

    @Query("DELETE FROM wallet_transactions")
    suspend fun clearAllWalletTransactions()

    // Currency Exchange Rates
    @Query("SELECT * FROM currency_rates")
    fun getAllCurrencyRates(): Flow<List<CurrencyRateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrencyRates(rates: List<CurrencyRateEntity>)
}
