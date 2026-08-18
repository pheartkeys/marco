package com.example.data.di

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.example.data.local.AppDatabase
import com.example.data.repository.CachedOnlyPricingRepository
import com.example.data.repository.LedgerRepository
import com.example.data.repository.LivePricingRepository
import com.example.data.repository.PartyRepository
import com.example.data.repository.PowWowRepository
import com.example.data.repository.PricingRepository
import com.example.data.repository.RoomLedgerRepository
import com.example.data.repository.RoomPartyRepository
import com.example.data.repository.RoomPowWowRepository
import com.example.data.repository.TravelRepository

/**
 * The dependency seam. **There is no DI framework in this project and none is being added.**
 *
 * Historically every dependency was constructed inside `TravelViewModel.init`, which is why that
 * class became the only place features could live. This object exists so a feature-scoped ViewModel
 * can get exactly the repository it needs without touching `TravelViewModel` at all:
 *
 * ```kotlin
 * class PowWowViewModel(app: Application) : AndroidViewModel(app) {
 *     private val powWow = MarcoRepositories.powWow(app)
 *     private val party = MarcoRepositories.party(app)
 * }
 * ```
 *
 * Every accessor is idempotent and returns a process-wide singleton over the single
 * [AppDatabase] instance, so repositories obtained here share the same underlying `Flow`s as
 * anything else in the app.
 *
 * Tests replace implementations with [overrideForTests] and clean up with [resetForTests].
 */
object MarcoRepositories {

    @Volatile private var travelRepository: TravelRepository? = null
    @Volatile private var partyRepository: PartyRepository? = null
    @Volatile private var powWowRepository: PowWowRepository? = null
    @Volatile private var ledgerRepository: LedgerRepository? = null
    @Volatile private var pricingRepository: PricingRepository? = null

    /** The pre-existing catch-all repository: trips, activities, wallet, chat, preferences. */
    fun travel(context: Context): TravelRepository =
        travelRepository ?: synchronized(this) {
            travelRepository ?: TravelRepository(database(context).travelDao())
                .also { travelRepository = it }
        }

    /** Travellers, party units, memberships, ideas. */
    fun party(context: Context): PartyRepository =
        partyRepository ?: synchronized(this) {
            partyRepository ?: RoomPartyRepository(database(context).partyDao())
                .also { partyRepository = it }
        }

    /** Pow Wow sessions, transcripts, briefs. Needs the party DAO to resolve consent by age band. */
    fun powWow(context: Context): PowWowRepository =
        powWowRepository ?: synchronized(this) {
            powWowRepository ?: RoomPowWowRepository(
                powWowDao = database(context).powWowDao(),
                partyDao = database(context).partyDao()
            ).also { powWowRepository = it }
        }

    /** Contributions, expenses, split rules, settlements. Arithmetic lives in `SettlementEngine`. */
    fun ledger(context: Context): LedgerRepository =
        ledgerRepository ?: synchronized(this) {
            ledgerRepository ?: RoomLedgerRepository(database(context).ledgerDao())
                .also { ledgerRepository = it }
        }

    /**
     * Pricing. Uses [LivePricingRepository], which queries the pricing normalization service and
     * caches quotes locally via [com.example.data.local.PricingDao] for offline access and auditability.
     */
    fun pricing(context: Context): PricingRepository =
        pricingRepository ?: synchronized(this) {
            pricingRepository ?: LivePricingRepository(database(context).pricingDao())
                .also { pricingRepository = it }
        }

    private fun database(context: Context): AppDatabase =
        AppDatabase.getInstance(context.applicationContext)

    /**
     * Erase every local table, including the v10 party, Pow Wow, contribution, ledger, and pricing
     * tables.
     *
     * This is what "erase all local data" in Settings must call. It deliberately reaches past the
     * repository interfaces to the DAOs: a wipe has to cover tables no repository has been written
     * for yet, and a partial wipe that leaves travellers, transcripts, or a ledger behind is worse
     * than no wipe at all. **Any new table added later must be added here.**
     */
    suspend fun eraseAllLocalData(context: Context) {
        val db = database(context)
        travel(context).clearAllLocalData()

        db.partyDao().apply {
            clearMemberships()
            clearPartyUnits()
            clearTravelers()
            clearIdeas()
        }
        db.powWowDao().apply {
            clearTranscripts()
            clearSessions()
            clearBriefs()
        }
        db.ledgerDao().apply {
            clearSettlements()
            clearSplitRules()
            clearLedgerEntries()
            clearContributions()
            clearLedgerConfigs()
        }
        db.pricingDao().clearQuoteCache()
    }

    @VisibleForTesting
    fun overrideForTests(
        travel: TravelRepository? = null,
        party: PartyRepository? = null,
        powWow: PowWowRepository? = null,
        ledger: LedgerRepository? = null,
        pricing: PricingRepository? = null
    ) {
        synchronized(this) {
            travel?.let { travelRepository = it }
            party?.let { partyRepository = it }
            powWow?.let { powWowRepository = it }
            ledger?.let { ledgerRepository = it }
            pricing?.let { pricingRepository = it }
        }
    }

    @VisibleForTesting
    fun resetForTests() {
        synchronized(this) {
            travelRepository = null
            partyRepository = null
            powWowRepository = null
            ledgerRepository = null
            pricingRepository = null
        }
    }
}
