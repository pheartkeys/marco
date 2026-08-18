package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The device owner's Traveler DNA. Single row, id = 1.
 *
 * This is the *local device owner's* profile and is distinct from [TravelerEntity], which models
 * any person on a trip (including guests who never had this app open). When the owner joins a
 * trip they get a [TravelerEntity] row too; link the two by writing the owner's traveler id into
 * [ownerTravelerId].
 */
@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
    @PrimaryKey val id: Long = 1,
    val displayName: String = "",
    val homeAirport: String = "",
    val travelMotivation: String = "",
    val signatureAspiration: String = "",
    val accessibilityVerificationOptIn: Boolean = false,
    val preferredAirlines: String = "",
    val preferredHotelTypes: String = "",
    val activityLevel: String = "Balanced",
    val preferredTravelStyle: String = "",
    val dietaryPreferences: String = "",
    val wheelchairRequirements: String = "",
    val familyAgeBrackets: String = "",
    val sensoryAndMobilityNotes: String = "",
    val pacingPreference: String = "",
    val preferredTransit: String = "",
    val learnedInsightsSummary: String = "Set up your preferences or plan your first journey to build your AI Traveler DNA profile.",
    val totalTripsAnalyzed: Int = 0,
    val onboardingCompleted: Boolean = false,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis(),
    // Full-fidelity 1-7 intensity weights (JSON-encoded via PreferenceWeights; option -> weight,
    // absent = never rated). The flat fields above stay populated with each group's
    // highest-weighted value so existing UI/planner code keeps working unchanged; these JSON
    // fields carry the complete picture for anything that wants it (e.g. the Gemini prompt).
    val motivationWeightsJson: String = "",
    val travelStyleWeightsJson: String = "",
    val pacingWeightsJson: String = "",
    val comfortWeightsJson: String = "",
    // categoryType -> ordered list of loyalty program ids, ranked by booking preference within
    // that category (index 0 = book first).
    val loyaltyRankJson: String = "",
    /**
     * The [TravelerEntity] row representing this device owner as a participant on trips.
     * 0 = not yet created. Never invent a traveler id here.
     */
    val ownerTravelerId: Long = 0
)
