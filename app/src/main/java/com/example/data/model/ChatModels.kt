package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A message in either chat stream.
 *
 * `sender` doubles as the rich-card discriminator: besides USER / CONCIERGE_AI /
 * VOICE_CALL_DISPATCHER it carries `CARD_*` values that `ConciergeChatScreen` `when`-dispatches to
 * a composable, with the structured payload as JSON in [suggestedActionJson].
 */
@Entity(tableName = "chat_messages", indices = [Index("tripId")])
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long = 0,
    val sender: String, // USER, CONCIERGE_AI, VOICE_CALL_DISPATCHER, CARD_*
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionChipText: String = "",
    val suggestedActionJson: String = "",
    val chatType: String = "PRIVATE", // "PRIVATE" (1-on-1 with Marco) or "GROUP" (Travel Crew stream)
    val authorName: String = "Marco Concierge",
    val mediaUrl: String = ""
)
