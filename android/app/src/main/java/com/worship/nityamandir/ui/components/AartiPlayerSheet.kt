package com.worship.nityamandir.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worship.nityamandir.data.model.AartiLyric
import com.worship.nityamandir.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun AartiPlayerSheet(
    titleHi: String,
    titleEn: String,
    lyrics: List<AartiLyric>,
    isHindi: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(true) }
    var currentLineIndex by remember { mutableIntStateOf(0) }

    // Lyric line progress simulation
    LaunchedEffect(isPlaying) {
        while (isPlaying && currentLineIndex < lyrics.size) {
            delay(3500)
            if (currentLineIndex < lyrics.size - 1) {
                currentLineIndex++
            } else {
                break
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = AltarTeakDark),
        shape = RoundedCornerShape(20.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(SacredGold, SacredBrass))
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = SacredGold
                    )
                    Text(
                        text = if (isHindi) titleHi else titleEn,
                        color = SacredGold,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = { isPlaying = !isPlaying }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = SacredGold
                    )
                }
            }

            // Synced Scrolling Lyrics
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(lyrics) { index, lyric ->
                    val isCurrent = index == currentLineIndex
                    Surface(
                        color = if (isCurrent) Color(0x33FFD700) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        border = if (isCurrent) CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SacredGold, SacredBrass))) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isHindi) lyric.lineHi else lyric.lineEn,
                                color = if (isCurrent) SacredGold else SacredTextSecondary,
                                fontSize = if (isCurrent) 15.sp else 13.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                            if (!isHindi) {
                                Text(
                                    text = lyric.meaningEn,
                                    color = if (isCurrent) Color(0xFFFFECB3) else Color(0xFF9E9E9E),
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onComplete,
                colors = ButtonDefaults.buttonColors(containerColor = SacredDeepSaffron),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isHindi) "आरती पूर्ण व आशीर्वाद ग्रहण करें" else "Complete Aarti & Receive Blessings",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
