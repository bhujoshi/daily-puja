package com.worship.nityamandir.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worship.nityamandir.ui.theme.*

@Composable
fun TimeTravelDebugDrawer(
    currentOffsetHours: Float,
    onSetOffset: (Float) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xEE1E0E08)),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(SacredBrass, SacredGold))
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚙️ काल-चक्र परीक्षण (Time Travel Harness)",
                    color = SacredGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "+${currentOffsetHours.toInt()}h",
                    color = SacredTextSecondary,
                    fontSize = 13.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { onSetOffset(0f) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    Text("Now", fontSize = 11.sp, color = SacredGold)
                }

                OutlinedButton(
                    onClick = { onSetOffset(12f) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    Text("+12h", fontSize = 11.sp, color = SacredGold)
                }

                OutlinedButton(
                    onClick = { onSetOffset(24f) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    Text("+24h", fontSize = 11.sp, color = SacredGold)
                }

                OutlinedButton(
                    onClick = { onSetOffset(72f) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    Text("+72h", fontSize = 11.sp, color = SacredGold)
                }

                OutlinedButton(
                    onClick = { onSetOffset(168f) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    Text("+7d", fontSize = 11.sp, color = SacredGold)
                }
            }

            TextButton(
                onClick = onReset,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Reset Mandir State", color = SacredDeepSaffron, fontSize = 12.sp)
            }
        }
    }
}
