package com.example.feature.ledger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.LuxuryCardElevated
import com.example.ui.theme.LuxurySurface
import com.example.ui.theme.TextPrimary

/**
 * Luxury tactile keypad eliminating standard soft keyboard typing for monetary amounts and quantities.
 */
@Composable
fun NumericKeypad(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    allowDecimal: Boolean = true
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(if (allowDecimal) "." else "", "0", "DEL")
    )

    fun handleKeyPress(key: String) {
        when (key) {
            "DEL" -> {
                if (value.isNotEmpty()) {
                    onValueChange(value.dropLast(1))
                }
            }
            "." -> {
                if (!value.contains(".")) {
                    onValueChange(if (value.isEmpty()) "0." else "$value.")
                }
            }
            "" -> {}
            else -> {
                // Limit to 2 decimal places if decimal present
                val dotIndex = value.indexOf('.')
                if (dotIndex != -1 && value.length - dotIndex > 2) {
                    return
                }
                if (value == "0" && key != ".") {
                    onValueChange(key)
                } else {
                    onValueChange(value + key)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(LuxurySurface, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(if (key.isNotEmpty()) LuxuryCardElevated else LuxurySurface)
                            .clickable(enabled = key.isNotEmpty()) { handleKeyPress(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (key == "DEL") {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Delete",
                                tint = ChampagneGold,
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (key.isNotEmpty()) {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 20.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
