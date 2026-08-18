package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Simulated vendor phone call log. */
@Entity(tableName = "vendor_call_logs", indices = [Index("tripId")])
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

@Entity(tableName = "emergency_alerts", indices = [Index("tripId")])
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

@Entity(tableName = "group_memories", indices = [Index("tripId")])
data class GroupMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val authorName: String,
    val caption: String,
    val locationTag: String,
    val timestamp: String,
    val mediaType: String = "PHOTO",
    val photoGradientColor: Long = 0xFF0284C7,
    val likesCount: Int = 0,
    val aiTag: String = ""
)
