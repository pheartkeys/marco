package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

data class HistoricalTravelerQuote(
    val explorerName: String,
    val epithet: String,
    val workOrJourney: String,
    val quote: String,
    val yearOrEra: String,
    val routeSummary: String,
    val iconEmoji: String
)

val FamousTravelersQuotes = listOf(
    HistoricalTravelerQuote(
        explorerName = "Marco Polo",
        epithet = "Venetian Silk Road Trailblazer",
        workOrJourney = "The Travels of Marco Polo (Il Milione)",
        quote = "I did not tell half of what I saw, for I knew I would not be believed.",
        yearOrEra = "1271-1295 CE",
        routeSummary = "Venice → Persia → Pamir Mountains → Xanadu & China",
        iconEmoji = "📜"
    ),
    HistoricalTravelerQuote(
        explorerName = "Ibn Battuta",
        epithet = "The Great Medieval Wayfarer",
        workOrJourney = "A Gift to Those Who Contemplate the Wonders of Cities (The Rihla)",
        quote = "Traveling. It leaves you speechless, then turns you into a storyteller.",
        yearOrEra = "1325-1354 CE",
        routeSummary = "Tangier → Mecca → Samarkand → Maldives → Beijing",
        iconEmoji = "🌙"
    ),
    HistoricalTravelerQuote(
        explorerName = "Nellie Bly",
        epithet = "Record-Breaking Circumnavigator",
        workOrJourney = "Around the World in Seventy-Two Days",
        quote = "Energy rightly applied and directed will accomplish anything.",
        yearOrEra = "1889-1890 CE",
        routeSummary = "New York → London → Brindisi → Singapore → San Francisco",
        iconEmoji = "🧭"
    ),
    HistoricalTravelerQuote(
        explorerName = "Phileas Fogg & Passepartout",
        epithet = "Jules Verne's Legendary Globetrotters",
        workOrJourney = "Around the World in Eighty Days",
        quote = "A true gentleman doesn't joke when he is talking about so serious a thing as a journey.",
        yearOrEra = "1872 CE (Literary Classic)",
        routeSummary = "London → Suez → Bombay → Calcutta → Hong Kong → Yokohama",
        iconEmoji = "🎈"
    ),
    HistoricalTravelerQuote(
        explorerName = "Odysseus",
        epithet = "Homer's Cunning Wanderer",
        workOrJourney = "The Odyssey by Homer",
        quote = "There is a time for many words, and there is also a time for sleep. But first, let the journey unfold.",
        yearOrEra = "c. 8th Century BCE",
        routeSummary = "Troy → Lotus-Eaters → Aeaea → Phaeacia → Ithaca",
        iconEmoji = "⛵"
    ),
    HistoricalTravelerQuote(
        explorerName = "Xuanzang (Tripitaka)",
        epithet = "Pilgrim of the Great Tang",
        workOrJourney = "Great Tang Records on the Western Regions / Journey to the West",
        quote = "The mind is the origin of all things; from fearless steps is the path across the desert forged.",
        yearOrEra = "627-645 CE",
        routeSummary = "Chang'an → Taklamakan Desert → Tian Shan → Nalanda",
        iconEmoji = "🏯"
    ),
    HistoricalTravelerQuote(
        explorerName = "Isabella Bird",
        epithet = "Victorian Wilderness Pioneer & Geographer",
        workOrJourney = "A Lady's Life in the Rocky Mountains & Unbeaten Tracks in Japan",
        quote = "I have only one ambition: to see as much of the wondrous world as I can before I leave it.",
        yearOrEra = "1873-1897 CE",
        routeSummary = "Rocky Mountains → Hawaii → Hokkaido → Yangtze River",
        iconEmoji = "🏔️"
    ),
    HistoricalTravelerQuote(
        explorerName = "Captain Nemo",
        epithet = "Commander of the Nautilus",
        workOrJourney = "Twenty Thousand Leagues Under the Seas by Jules Verne",
        quote = "The sea is everything. It covers seven tenths of the terrestrial globe... It is an immense reservoir of nature.",
        yearOrEra = "1870 CE (Literary Classic)",
        routeSummary = "Pacific depths → Red Sea tunnel → South Pole → Atlantis",
        iconEmoji = "🌊"
    ),
    HistoricalTravelerQuote(
        explorerName = "Ernest Shackleton",
        epithet = "Legend of the Endurance",
        workOrJourney = "Imperial Trans-Antarctic Expedition (South)",
        quote = "Difficulties are just things to overcome, after all. By endurance we conquer.",
        yearOrEra = "1914-1917 CE",
        routeSummary = "Weddell Sea → Elephant Island → South Georgia Crossing",
        iconEmoji = "❄️"
    )
)

/**
 * Marco Astrolabe Celestial Loading Animation
 * A detailed Canvas drawing inspired by medieval brass astrolabes, armillary spheres, and navigational compass roses.
 */
@Composable
fun MarcoAstrolabeLoadingAnimation(
    modifier: Modifier = Modifier,
    sizeDp: Int = 110,
    accentColor: Color = VenetianGold
) {
    val infiniteTransition = rememberInfiniteTransition(label = "marcoAstrolabe")

    // Continuous rotation for outer astrolabe rete ring
    val reteAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reteRotation"
    )

    // Reverse rotation for inner zodiac/celestial coordinates
    val innerAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerRotation"
    )

    // Gentle oscillation for compass needle
    val needleRocking by infiniteTransition.animateFloat(
        initialValue = -25f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "needleRocking"
    )

    // Star sparkle pulse
    val starPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starPulse"
    )

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .testTag("marco_astrolabe_loader"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val outerRadius = size.minDimension / 2 * 0.92f
            val midRadius = outerRadius * 0.72f
            val innerRadius = outerRadius * 0.45f

            // Outer Brass Mater Ring
            drawCircle(
                color = accentColor.copy(alpha = 0.25f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )

            // Outer Degree Ticks
            val tickCount = 24
            for (i in 0 until tickCount) {
                val angleRad = Math.toRadians((i * 360f / tickCount).toDouble())
                val isMajor = i % 6 == 0
                val tickLength = if (isMajor) 7.dp.toPx() else 3.5.dp.toPx()
                val startX = (center.x + (outerRadius - tickLength) * cos(angleRad)).toFloat()
                val startY = (center.y + (outerRadius - tickLength) * sin(angleRad)).toFloat()
                val endX = (center.x + outerRadius * cos(angleRad)).toFloat()
                val endY = (center.y + outerRadius * sin(angleRad)).toFloat()

                drawLine(
                    color = if (isMajor) accentColor else accentColor.copy(alpha = 0.4f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Rotating Rete (Star-pointer circle with eccentric offset)
            rotate(reteAngle, center) {
                drawCircle(
                    color = VenetianGoldLight.copy(alpha = 0.35f),
                    radius = midRadius,
                    center = center,
                    style = Stroke(width = 1.8.dp.toPx())
                )

                // Eccentric Tropic Circle
                drawCircle(
                    color = VenetianGoldLight.copy(alpha = 0.22f),
                    radius = midRadius * 0.75f,
                    center = Offset(center.x + midRadius * 0.25f, center.y),
                    style = Stroke(width = 1.2.dp.toPx())
                )

                // Star Point Pointers on Rete
                for (j in 0 until 4) {
                    val starRad = Math.toRadians((j * 90.0))
                    val starCenter = Offset(
                        (center.x + midRadius * cos(starRad)).toFloat(),
                        (center.y + midRadius * sin(starRad)).toFloat()
                    )
                    drawCircle(
                        color = VenetianGoldLight,
                        radius = (2.5f * starPulse).dp.toPx(),
                        center = starCenter
                    )
                }
            }

            // Inner Zodiac & Cardinal Ring (Reverse Spin)
            rotate(innerAngle, center) {
                drawCircle(
                    color = AntiqueBrass.copy(alpha = 0.4f),
                    radius = innerRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                // Rhumb line cross
                drawLine(
                    color = AntiqueBrass.copy(alpha = 0.3f),
                    start = Offset(center.x - innerRadius, center.y),
                    end = Offset(center.x + innerRadius, center.y),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = AntiqueBrass.copy(alpha = 0.3f),
                    start = Offset(center.x, center.y - innerRadius),
                    end = Offset(center.x, center.y + innerRadius),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Compass Alidade / Needle with magnetic oscillation
            rotate(needleRocking, center) {
                val needleLength = outerRadius * 0.85f
                val needleWidth = 5.dp.toPx()

                // North Pointer (Golden Venetian Brass)
                val northPath = Path().apply {
                    moveTo(center.x, center.y - needleLength)
                    lineTo(center.x + needleWidth, center.y - 3.dp.toPx())
                    lineTo(center.x, center.y)
                    lineTo(center.x - needleWidth, center.y - 3.dp.toPx())
                    close()
                }
                drawPath(path = northPath, color = VenetianGold)

                // South Pointer (Antique Terracotta / Parchment)
                val southPath = Path().apply {
                    moveTo(center.x, center.y + needleLength)
                    lineTo(center.x + needleWidth, center.y + 3.dp.toPx())
                    lineTo(center.x, center.y)
                    lineTo(center.x - needleWidth, center.y + 3.dp.toPx())
                    close()
                }
                drawPath(path = southPath, color = TerracottaMap)
            }

            // Central Pivot Brass Pin
            drawCircle(
                color = VenetianGoldLight,
                radius = 4.5.dp.toPx(),
                center = center
            )
            drawCircle(
                color = CelestialLapis,
                radius = 2.dp.toPx(),
                center = center
            )
        }
    }
}

/**
 * ExplorerVoyageLoadingCard
 * Full interactive loading card that cycles through famous historical and literary travelers,
 * displaying their route, era, book title, and quote while loading itineraries, AI replies, or concierge actions.
 */
@Composable
fun ExplorerVoyageLoadingCard(
    statusMessage: String = "Marco AI Concierge Navigating...",
    subStatus: String = "Plotting multi-modal voyage routes & coordinates",
    modifier: Modifier = Modifier
) {
    var currentQuoteIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            currentQuoteIndex = (currentQuoteIndex + 1) % FamousTravelersQuotes.size
        }
    }

    val quoteItem = FamousTravelersQuotes[currentQuoteIndex]

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = LuxuryBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("explorer_voyage_loading_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar: "MARCO • EXPEDITION CONCIERGE"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Marco",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = VenetianGold
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = VenetianGold.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Expeditions",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = VenetianGold
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Shimmering Compass Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = VenetianGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Navigating",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = VenetianGoldLight
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animated Astrolabe Canvas & Sailing Wave Loader
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(LuxuryCardElevated),
                contentAlignment = Alignment.Center
            ) {
                MarcoAstrolabeLoadingAnimation(sizeDp = 90)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Action Status
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = subStatus,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Great Travelers Literary Quote Carousel
            AnimatedContent(
                targetState = quoteItem,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(300)) },
                label = "travelerQuoteTransition"
            ) { traveler ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = null,
                                tint = VenetianGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = traveler.explorerName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = VenetianGold
                                )
                            )
                            Text(
                                text = " • ${traveler.yearOrEra}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "“${traveler.quote}”",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = FontStyle.Italic,
                                lineHeight = 16.sp,
                                textAlign = TextAlign.Center
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Route / Work Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                tint = TealAccent,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = traveler.routeSummary,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TealAccent
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * CaravelSailingLoadingIndicator
 * A lightweight sailing caravel & waves animation for quick in-line progress indicators.
 */
@Composable
fun CaravelSailingLoadingIndicator(
    modifier: Modifier = Modifier,
    widthDp: Int = 120,
    heightDp: Int = 40
) {
    val infiniteTransition = rememberInfiniteTransition(label = "caravelSailing")

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val shipRocking by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shipRocking"
    )

    Box(
        modifier = modifier
            .size(width = widthDp.dp, height = heightDp.dp)
            .testTag("caravel_sailing_indicator"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(heightDp.dp)) {
            val w = size.width
            val h = size.height
            val midY = h * 0.65f

            // Oscillating Ocean Waves
            val wavePath1 = Path()
            val wavePath2 = Path()

            wavePath1.moveTo(0f, midY)
            wavePath2.moveTo(0f, midY + 4.dp.toPx())

            for (x in 0..w.toInt() step 5) {
                val xNorm = x.toFloat() / w
                val y1 = midY + sin(xNorm * 4 * Math.PI + wavePhase).toFloat() * 4.dp.toPx()
                val y2 = midY + 4.dp.toPx() + sin(xNorm * 4 * Math.PI + wavePhase + Math.PI / 2).toFloat() * 3.dp.toPx()

                wavePath1.lineTo(x.toFloat(), y1)
                wavePath2.lineTo(x.toFloat(), y2)
            }

            drawPath(
                path = wavePath1,
                color = MediterraneanAzure.copy(alpha = 0.7f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
            drawPath(
                path = wavePath2,
                color = TealAccent.copy(alpha = 0.4f),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Ship at center oscillating with rocking angle
            val shipCenterX = w * 0.5f
            val shipCenterY = midY - 3.dp.toPx() + sin(wavePhase.toDouble() + Math.PI).toFloat() * 3.dp.toPx()

            rotate(shipRocking, Offset(shipCenterX, shipCenterY)) {
                // Caravel Hull
                val hullPath = Path().apply {
                    moveTo(shipCenterX - 14.dp.toPx(), shipCenterY + 2.dp.toPx())
                    lineTo(shipCenterX + 14.dp.toPx(), shipCenterY + 2.dp.toPx())
                    lineTo(shipCenterX + 10.dp.toPx(), shipCenterY + 8.dp.toPx())
                    lineTo(shipCenterX - 10.dp.toPx(), shipCenterY + 8.dp.toPx())
                    close()
                }
                drawPath(path = hullPath, color = AntiqueBrass)

                // Mast
                drawLine(
                    color = VenetianGoldDeep,
                    start = Offset(shipCenterX, shipCenterY + 2.dp.toPx()),
                    end = Offset(shipCenterX, shipCenterY - 14.dp.toPx()),
                    strokeWidth = 1.8.dp.toPx()
                )

                // Sails (Parchment White)
                val mainSailPath = Path().apply {
                    moveTo(shipCenterX, shipCenterY - 12.dp.toPx())
                    lineTo(shipCenterX + 9.dp.toPx(), shipCenterY - 3.dp.toPx())
                    lineTo(shipCenterX, shipCenterY - 3.dp.toPx())
                    close()
                }
                drawPath(path = mainSailPath, color = ParchmentSand)

                val foreSailPath = Path().apply {
                    moveTo(shipCenterX, shipCenterY - 10.dp.toPx())
                    lineTo(shipCenterX - 7.dp.toPx(), shipCenterY - 3.dp.toPx())
                    lineTo(shipCenterX, shipCenterY - 3.dp.toPx())
                    close()
                }
                drawPath(path = foreSailPath, color = VenetianGoldLight)
            }
        }
    }
}

/**
 * MarcoConciergeTypingIndicator
 * A refined typing bubble with rotating celestial compass rose and golden wave pulses.
 */
@Composable
fun MarcoConciergeTypingIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typingIndicator")
    val dotScale by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotScale"
    )

    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = modifier.testTag("marco_typing_indicator")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Explore,
                contentDescription = null,
                tint = VenetianGold,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(spinAngle)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Marco is charting response...",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = VenetianGoldLight
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (i in 0..2) {
                    val pulse = ((dotScale + i * 0.25f) % 1.0f).coerceIn(0.3f, 1.0f)
                    Box(
                        modifier = Modifier
                            .size((4 * pulse).dp)
                            .clip(CircleShape)
                            .background(VenetianGold)
                    )
                }
            }
        }
    }
}
