package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ConnectedAccountEntity
import com.example.data.model.ContributionEntity
import com.example.data.model.EmergencyAlertEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.GroupMemoryEntity
import com.example.data.model.IdeaEntity
import com.example.data.model.LedgerEntryEntity
import com.example.data.model.PartyUnitEntity
import com.example.data.model.PowWowSessionEntity
import com.example.data.model.PowWowTranscriptEntity
import com.example.data.model.PriceQuoteCacheEntity
import com.example.data.model.ProactiveSuggestionEntity
import com.example.data.model.SettlementEntity
import com.example.data.model.SplitRuleEntity
import com.example.data.model.TravelerEntity
import com.example.data.model.TripActivityEntity
import com.example.data.model.TripBriefEntity
import com.example.data.model.TripEntity
import com.example.data.model.CurrencyRateEntity
import com.example.data.model.TripFeedbackEntity
import com.example.data.model.TripLedgerConfigEntity
import com.example.data.model.TripMembershipEntity
import com.example.data.model.UserPreferenceEntity
import com.example.data.model.VendorCallLogEntity
import com.example.data.model.WalletBalanceEntity
import com.example.data.model.WalletTransactionEntity

/**
 * Room database `marco_travel.db`.
 *
 * **Version 10.** v10 added the multi-user foundation: travellers, party units, trip memberships,
 * ideas (the EXPLORING stage), Pow Wow sessions and transcripts, trip briefs, contributions, and
 * the ledger (entries, split rules, settlements, per-trip ledger config). It also added
 * `TripEntity.originIdeaId` and `UserPreferenceEntity.ownerTravelerId`.
 *
 * `fallbackToDestructiveMigration()` is retained per project precedent: there is no migration path,
 * so bumping the version wipes local data. Bump `version` whenever an entity changes.
 *
 * DAOs are split by domain ([TravelDao] for the pre-existing single-user entities, plus
 * [PartyDao], [PowWowDao], [LedgerDao]) so parallel feature work does not converge on one file.
 */
@Database(
    entities = [
        // Pre-existing single-user entities
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
        CurrencyRateEntity::class,
        // v10 — party & multi-user
        TravelerEntity::class,
        PartyUnitEntity::class,
        TripMembershipEntity::class,
        // v10 — exploring stage
        IdeaEntity::class,
        // v10 — pow wow
        PowWowSessionEntity::class,
        PowWowTranscriptEntity::class,
        TripBriefEntity::class,
        // v10 — contributions & ledger
        ContributionEntity::class,
        LedgerEntryEntity::class,
        SplitRuleEntity::class,
        SettlementEntity::class,
        TripLedgerConfigEntity::class,
        // v10 — pricing
        PriceQuoteCacheEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun travelDao(): TravelDao
    abstract fun partyDao(): PartyDao
    abstract fun powWowDao(): PowWowDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun pricingDao(): PricingDao

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
