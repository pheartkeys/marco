package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.LuxuryBorder
import com.example.ui.theme.LuxuryCardElevated
import com.example.ui.theme.LuxuryDarkBase
import com.example.ui.theme.LuxurySurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * A single option in a weighted preference group: label + a 1-7 intensity scale, plus an
 * explicit "Not rated" state that is visually and semantically distinct from any numeric value —
 * including 1. Nothing is pre-selected; [weight] starts null and only user interaction (tapping a
 * pip) assigns a value. Tapping the currently-selected pip again clears it back to unrated, so a
 * user can always retreat to "I have no opinion on this."
 */
@Composable
fun IntensityScaleRow(
    label: String,
    weight: Int?,
    onWeightChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    val isRated = weight != null

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isRated) LuxuryCardElevated else LuxurySurface,
        border = BorderStroke(1.dp, if (isRated) ChampagneGold.copy(alpha = 0.4f) else LuxuryBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (isRated) TextPrimary else TextSecondary
                        )
                    )
                    if (description != null) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                        )
                    }
                }
                Text(
                    text = if (isRated) "$weight/7" else "Not rated",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isRated) ChampagneGold else TextMuted,
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (1..7).forEach { pip ->
                    val selected = weight == pip
                    Surface(
                        shape = CircleShape,
                        color = if (selected) ChampagneGold else LuxurySurface,
                        border = BorderStroke(1.dp, if (selected) ChampagneGold else LuxuryBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .clickable {
                                // Tapping the already-selected pip clears the rating back to unset
                                // rather than forcing the user to always land on some number.
                                onWeightChange(if (selected) null else pip)
                            }
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(30.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = pip.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) LuxuryDarkBase else TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
