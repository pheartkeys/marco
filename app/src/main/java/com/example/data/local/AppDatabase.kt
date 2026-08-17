package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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

@Database(
    entities = [
        TripEntity::class,
        TripActivityEntity::class,
        ConnectedAccountEntity::class,
        ExpenseEntity::class,
        VendorCallLogEntity::class,
        EmergencyAlertEntity::class,
        GroupMemoryEntity::class,
        ChatMessageEntity::class,
        UserPreferenceEntity::class,
        TripFeedbackEntity::class,
        ProactiveSuggestionEntity::class,
        WalletBalanceEntity::class,
        WalletTransactionEntity::class,
        CurrencyRateEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun travelDao(): TravelDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "marco_travel.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
