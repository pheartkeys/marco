package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessageEntity
import com.example.data.model.TripActivityEntity
import com.example.data.model.TripEntity
import com.example.ui.theme.*

/**
 * In-Chat Itinerary Highlights & Voyage Timeline Snippets
 * A fresh, inspiring, colorful carousel of journey stops, lodging, and excursions.
 */
@Composable
fun ChatItinerarySnippetCard(
    message: ChatMessageEntity,
    trip: TripEntity?,
    activities: List<TripActivityEntity>,
    onActivityClick: (TripActivityEntity) -> Unit = {},
    onPlayTts: () -> Unit = {},
    modifier: Modifier = Modifier,
    onOpenFullItinerary: () -> Unit = {}
) {
    val displayActivities = activities.take(6)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = LuxuryCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, LuxuryBorder),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag("chat_itinerary_snippet_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(VoyagerSkyMuted)
                            .border(1.dp, VoyagerSkyLight.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.EventNote,
                            contentDescription = null,
                            tint = VoyagerSkyLight,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Itinerary Highlights",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            ExpeditionBadge(
                                text = "${displayActivities.size} Highlights",
                                color = GoldenSunLight,
                                isFilled = true
                            )
                        }
                        Text(
                            text = trip?.title ?: "Trip Itinerary",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary
                            )
                        )
                    }
                }

                IconButton(onClick = onPlayTts) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Listen to itinerary snippet",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Horizontal Scrollable Stops
            if (displayActivities.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    displayActivities.forEach { activity ->
                        ItinerarySnippetItem(
                            activity = activity,
                            onClick = {
                                onActivityClick(activity)
                                onOpenFullItinerary()
                            }
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = LuxurySurface,
                    border = BorderStroke(1.dp, LuxuryBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No activities added yet. Tap below to build or customize this itinerary with your travel crew!",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tap-through Navigation to Full Itinerary Screen
            Button(
                onClick = onOpenFullItinerary,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MarcoCoral,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_full_itinerary_view_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open Full Itinerary & Booking Details",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun ItinerarySnippetItem(
    activity: TripActivityEntity,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = LuxurySurface
        ),
        border = BorderStroke(1.dp, LuxuryBorder),
        modifier = Modifier
            .width(230.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Category Badge & Time Slot Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryIconBadge(
                        category = activity.category,
                        size = 34
                    )
                    Text(
                        text = "Day ${activity.dayNumber}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = GoldenSunLight
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LuxuryCardElevated,
                    border = BorderStroke(1.dp, LuxuryBorder)
                ) {
                    Text(
                        text = activity.timeSlot,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = activity.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 18.sp
                ),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Location
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = activity.location,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary
                    ),
                    maxLines = 1
                )
            }

            if (activity.accessibilityBadge.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                AccessibilityTagChip(
                    text = activity.accessibilityBadge,
                    tintColor = PalmEmeraldLight
                )
            }
        }
    }
}
