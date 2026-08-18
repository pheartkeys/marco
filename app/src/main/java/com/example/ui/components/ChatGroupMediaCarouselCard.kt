package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessageEntity
import com.example.data.model.GroupMemoryEntity
import com.example.ui.theme.*

/**
 * In-stream collaborative media carousel displaying photos and moments shared
 * by travel group members directly in the conversation feed.
 */
@Composable
fun ChatGroupMediaCarouselCard(
    message: ChatMessageEntity,
    memories: List<GroupMemoryEntity>,
    onAddPhotoClick: () -> Unit = {},
    onLikeClick: (GroupMemoryEntity) -> Unit = {},
    onPlayTts: () -> Unit = {},
    modifier: Modifier = Modifier,
    onOpenFullMemories: () -> Unit = {}
) {
    val displayMemories = memories

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = LuxuryCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, LuxuryBorder),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag("chat_group_media_carousel_card")
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
                            .background(BerryOrchidMuted)
                            .border(1.dp, BerryOrchidLight.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = null,
                            tint = BerryOrchidLight,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Group Memories Reel",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            ExpeditionBadge(
                                text = "${displayMemories.size} Memories",
                                color = BerryOrchidLight,
                                isFilled = true
                            )
                        }
                        Text(
                            text = "Snapshots & stories from your crew",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPlayTts) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Listen to memory descriptions",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onAddPhotoClick) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Upload trip photo",
                            tint = GoldenSunLight,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Carousel List
            if (displayMemories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    displayMemories.forEach { memory ->
                        MediaCardItem(
                            memory = memory,
                            onLike = { onLikeClick(memory) }
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
                        text = "No group photos added yet. Tap '+' above to capture and share moments with your travel crew!",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tap-through Navigation to Full Group Memories Screen
            Button(
                onClick = onOpenFullMemories,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BerryOrchid,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_full_memories_view_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Collections,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open Vacation Moments & AI Story Reels",
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
private fun MediaCardItem(
    memory: GroupMemoryEntity,
    onLike: () -> Unit
) {
    var isLiked by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = LuxurySurface),
        border = BorderStroke(1.dp, LuxuryBorder),
        modifier = Modifier.width(210.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Postcard frame container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                LuxuryCardElevated,
                                LuxurySurface
                            )
                        )
                    )
                    .padding(10.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LuxuryDarkBase.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, LuxuryBorder)
                ) {
                    Text(
                        text = memory.aiTag,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GoldenSunLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Info & Caption
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = memory.caption,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        lineHeight = 16.sp
                    ),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = memory.authorName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = GoldenSunLight
                            )
                        )
                        Text(
                            text = memory.locationTag,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        )
                    }

                    IconButton(
                        onClick = {
                            isLiked = !isLiked
                            onLike()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like memory",
                            tint = if (isLiked) StatusCrimson else TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}
