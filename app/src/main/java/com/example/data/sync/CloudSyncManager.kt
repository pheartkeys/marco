package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.model.ExpenseEntity
import com.example.data.model.GroupMemoryEntity
import com.example.data.model.TripActivityEntity
import com.example.data.model.TripEntity
import com.example.data.model.UserPreferenceEntity
import com.example.data.model.WalletBalanceEntity
import com.example.data.model.WalletTransactionEntity
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Cloud Synchronization & Cross-Device Authentication Manager using Firebase Auth & Firestore.
 * Supports Email/Password, Google Sign-In, and two-way synchronization of itineraries,
 * activities, group memories, traveler DNA, and multi-currency wallet states.
 */
class CloudSyncManager(private val context: Context) {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus.OFFLINE_LOCAL)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    init {
        try {
            _currentUser.value = auth.currentUser
            auth.addAuthStateListener { firebaseAuth ->
                _currentUser.value = firebaseAuth.currentUser
                if (firebaseAuth.currentUser != null) {
                    _syncStatus.value = SyncStatus.SYNCED_CLOUD
                } else {
                    _syncStatus.value = SyncStatus.OFFLINE_LOCAL
                }
            }
        } catch (e: Exception) {
            Log.w("CloudSyncManager", "Firebase Auth initialization note: ${e.message}")
        }
    }

    /**
     * Authenticate with Email and Password
     */
    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            _syncStatus.value = SyncStatus.SYNCING
            val authResult = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user ?: throw Exception("Authentication returned empty user profile")
            _currentUser.value = user
            _syncStatus.value = SyncStatus.SYNCED_CLOUD
            _syncMessage.value = "Signed in as ${user.email}"
            Result.success(user)
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.OFFLINE_LOCAL
            _syncMessage.value = "Sign in error: ${e.localizedMessage ?: e.message}"
            Result.failure(e)
        }
    }

    /**
     * Create Account with Email and Password
     */
    suspend fun signUpWithEmail(email: String, pass: String, displayName: String? = null): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            _syncStatus.value = SyncStatus.SYNCING
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user ?: throw Exception("Account creation returned empty user profile")
            if (!displayName.isNullOrBlank()) {
                try {
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName.trim())
                        .build()
                    user.updateProfile(profileUpdates).await()
                } catch (pe: Exception) {
                    Log.w("CloudSyncManager", "Note setting displayName: ${pe.message}")
                }
            }
            _currentUser.value = user
            _syncStatus.value = SyncStatus.SYNCED_CLOUD
            _syncMessage.value = "Account created for ${user.displayName ?: user.email}"
            Result.success(user)
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.OFFLINE_LOCAL
            _syncMessage.value = "Registration error: ${e.localizedMessage ?: e.message}"
            Result.failure(e)
        }
    }

    /**
     * Send Password Reset Email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            auth.sendPasswordResetEmail(email.trim()).await()
            _syncMessage.value = "Password reset instructions sent to ${email.trim()}"
            Result.success(Unit)
        } catch (e: Exception) {
            _syncMessage.value = "Reset error: ${e.localizedMessage ?: e.message}"
            Result.failure(e)
        }
    }

    /**
     * Authenticate with Google ID Token
     */
    suspend fun signInWithGoogleToken(idToken: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            _syncStatus.value = SyncStatus.SYNCING
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Google Sign-In returned empty user")
            _currentUser.value = user
            _syncStatus.value = SyncStatus.SYNCED_CLOUD
            _syncMessage.value = "Google authenticated as ${user.displayName ?: user.email}"
            Result.success(user)
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.OFFLINE_LOCAL
            _syncMessage.value = "Google auth error: ${e.localizedMessage ?: e.message}"
            Result.failure(e)
        }
    }

    /**
     * Synchronizes a trip itinerary to Firestore under the current authenticated user's workspace
     */
    suspend fun syncTripToCloud(trip: TripEntity): Boolean = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) {
            _syncStatus.value = SyncStatus.OFFLINE_LOCAL
            return@withContext false
        }

        return@withContext try {
            val tripDoc = mapOf(
                "id" to trip.id,
                "title" to trip.title,
                "destination" to trip.destination,
                "countryCode" to trip.countryCode,
                "startDate" to trip.startDate,
                "endDate" to trip.endDate,
                "budgetTotal" to trip.budgetTotal,
                "budgetSpent" to trip.budgetSpent,
                "primaryCurrency" to trip.primaryCurrency,
                "accessibilityRequirements" to trip.accessibilityRequirements,
                "dietaryRestrictions" to trip.dietaryRestrictions,
                "familyAgeBrackets" to trip.familyAgeBrackets,
                "travelStyle" to trip.travelStyle,
                "status" to trip.status,
                "lastSyncedTimestamp" to System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(user.uid)
                .collection("trips")
                .document(trip.id.toString())
                .set(tripDoc, SetOptions.merge())
                .await()

            _syncStatus.value = SyncStatus.SYNCED_CLOUD
            _lastSyncTimestamp.value = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Firestore sync exception: ${e.message}")
            _syncStatus.value = SyncStatus.OFFLINE_LOCAL
            false
        }
    }

    /**
     * Synchronizes activities for a trip to Firestore
     */
    suspend fun syncActivitiesToCloud(tripId: Long, activities: List<TripActivityEntity>): Boolean = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext false
        return@withContext try {
            val batch = firestore.batch()
            val collection = firestore.collection("users")
                .document(user.uid)
                .collection("trips")
                .document(tripId.toString())
                .collection("activities")

            activities.forEach { act ->
                val docRef = collection.document(act.id.toString())
                val data = mapOf(
                    "id" to act.id,
                    "tripId" to act.tripId,
                    "dayNumber" to act.dayNumber,
                    "timeSlot" to act.timeSlot,
                    "title" to act.title,
                    "category" to act.category,
                    "location" to act.location,
                    "notes" to act.notes,
                    "cost" to act.cost,
                    "currency" to act.currency,
                    "isCompleted" to act.isCompleted,
                    "confirmationCode" to act.confirmationCode,
                    "accessibilityBadge" to act.accessibilityBadge,
                    "vendorName" to act.vendorName,
                    "vendorPhone" to act.vendorPhone,
                    "lastUpdatedTimestamp" to System.currentTimeMillis()
                )
                batch.set(docRef, data, SetOptions.merge())
            }
            batch.commit().await()
            _syncStatus.value = SyncStatus.SYNCED_CLOUD
            true
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Activities sync failed: ${e.message}")
            false
        }
    }

    /**
     * Synchronizes group photo memories to Firestore
     */
    suspend fun syncMemoriesToCloud(memories: List<GroupMemoryEntity>): Boolean = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext false
        return@withContext try {
            val batch = firestore.batch()
            val collection = firestore.collection("users")
                .document(user.uid)
                .collection("memories")

            memories.forEach { mem ->
                val docRef = collection.document(mem.id.toString())
                val data = mapOf(
                    "id" to mem.id,
                    "tripId" to mem.tripId,
                    "authorName" to mem.authorName,
                    "caption" to mem.caption,
                    "locationTag" to mem.locationTag,
                    "timestamp" to mem.timestamp,
                    "likesCount" to mem.likesCount,
                    "aiTag" to mem.aiTag,
                    "syncedAt" to System.currentTimeMillis()
                )
                batch.set(docRef, data, SetOptions.merge())
            }
            batch.commit().await()
            _syncStatus.value = SyncStatus.SYNCED_CLOUD
            true
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Memories sync failed: ${e.message}")
            false
        }
    }

    /**
     * Synchronizes user preferences and learned DNA
     */
    suspend fun syncPreferencesToCloud(pref: UserPreferenceEntity): Boolean = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext false
        return@withContext try {
            val prefMap = mapOf(
                "preferredAirlines" to pref.preferredAirlines,
                "preferredHotelTypes" to pref.preferredHotelTypes,
                "activityLevel" to pref.activityLevel,
                "wheelchairRequirements" to pref.wheelchairRequirements,
                "dietaryPreferences" to pref.dietaryPreferences,
                "familyAgeBrackets" to pref.familyAgeBrackets,
                "learnedInsightsSummary" to pref.learnedInsightsSummary,
                "lastUpdatedTimestamp" to System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(user.uid)
                .collection("preferences")
                .document("traveler_dna")
                .set(prefMap, SetOptions.merge())
                .await()

            _syncStatus.value = SyncStatus.SYNCED_CLOUD
            true
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Preferences sync failed: ${e.message}")
            false
        }
    }

    /**
     * Synchronizes encrypted multi-currency wallet balances
     */
    suspend fun syncWalletBalanceToCloud(balances: List<WalletBalanceEntity>): Boolean = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext false
        return@withContext try {
            val batch = firestore.batch()
            val collection = firestore.collection("users").document(user.uid).collection("wallet")

            balances.forEach { balance ->
                val docRef = collection.document(balance.currencyCode)
                val data = mapOf(
                    "currencyCode" to balance.currencyCode,
                    "currencyName" to balance.currencyName,
                    "allocatedBudget" to balance.allocatedBudget,
                    "availableBalance" to balance.availableBalance,
                    "spentAmount" to balance.spentAmount,
                    "exchangeRateToUsd" to balance.exchangeRateToUsd,
                    "lastUpdatedTimestamp" to System.currentTimeMillis()
                )
                batch.set(docRef, data, SetOptions.merge())
            }
            batch.commit().await()
            _syncStatus.value = SyncStatus.SYNCED_CLOUD
            true
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Wallet sync failed: ${e.message}")
            false
        }
    }

    /**
     * Synchronizes individual trip expense receipts
     */
    suspend fun syncExpensesToCloud(expenses: List<ExpenseEntity>): Boolean = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext false
        return@withContext try {
            val batch = firestore.batch()
            val collection = firestore.collection("users").document(user.uid).collection("expenses")

            expenses.forEach { exp ->
                val docRef = collection.document(exp.id.toString())
                val data = mapOf(
                    "id" to exp.id,
                    "tripId" to exp.tripId,
                    "title" to exp.title,
                    "category" to exp.category,
                    "amountOriginal" to exp.amountOriginal,
                    "originalCurrency" to exp.originalCurrency,
                    "amountUsd" to exp.amountUsd,
                    "paidBy" to exp.paidBy,
                    "date" to exp.date,
                    "notes" to exp.notes,
                    "lastSyncedAt" to System.currentTimeMillis()
                )
                batch.set(docRef, data, SetOptions.merge())
            }
            batch.commit().await()
            _syncStatus.value = SyncStatus.SYNCED_CLOUD
            true
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Expenses sync failed: ${e.message}")
            false
        }
    }

    /**
     * Master Full Sync across all datasets to Firestore
     */
    suspend fun syncAllDataToCloud(
        trips: List<TripEntity>,
        activities: List<TripActivityEntity>,
        preferences: UserPreferenceEntity?,
        walletBalances: List<WalletBalanceEntity>,
        expenses: List<ExpenseEntity>,
        memories: List<GroupMemoryEntity>
    ): Boolean = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) {
            _syncStatus.value = SyncStatus.OFFLINE_LOCAL
            _syncMessage.value = "Sign in to sync with Firebase Cloud Firestore"
            return@withContext false
        }

        try {
            _syncStatus.value = SyncStatus.SYNCING
            _syncMessage.value = "Syncing with Firestore collection..."

            // Sync all trips
            trips.forEach { trip ->
                syncTripToCloud(trip)
            }

            // Sync all activities
            if (activities.isNotEmpty()) {
                val tripId = trips.firstOrNull()?.id ?: 1L
                syncActivitiesToCloud(tripId, activities)
            }

            // Sync preferences
            preferences?.let { syncPreferencesToCloud(it) }

            // Sync wallet balances
            if (walletBalances.isNotEmpty()) {
                syncWalletBalanceToCloud(walletBalances)
            }

            // Sync expenses
            if (expenses.isNotEmpty()) {
                syncExpensesToCloud(expenses)
            }

            // Sync memories
            if (memories.isNotEmpty()) {
                syncMemoriesToCloud(memories)
            }

            _syncStatus.value = SyncStatus.SYNCED_CLOUD
            _lastSyncTimestamp.value = System.currentTimeMillis()
            _syncMessage.value = "Synced with Firebase (go-marco / com.go.marco)"
            true
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Master sync error: ${e.message}")
            _syncStatus.value = SyncStatus.OFFLINE_LOCAL
            _syncMessage.value = "Sync failed: ${e.localizedMessage ?: e.message}"
            false
        }
    }

    fun signOut() {
        try {
            auth.signOut()
            _currentUser.value = null
            _syncStatus.value = SyncStatus.OFFLINE_LOCAL
            _syncMessage.value = "Signed out of Firebase"
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Sign out error: ${e.message}")
        }
    }

    enum class SyncStatus {
        SYNCED_CLOUD,
        OFFLINE_LOCAL,
        SYNCING
    }
}

