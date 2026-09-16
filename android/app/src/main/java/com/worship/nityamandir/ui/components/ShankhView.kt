package com.worship.nityamandir.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.worship.nityamandir.ui.theme.SacredGold

@Composable
fun ShankhView(
    isBlowing: Boolean,
    onBlow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shankhWave")
    val waveRadius by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = 90f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveRadius"
    )
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveAlpha"
    )

    Box(
        modifier = modifier
            .size(90.dp)
            .clickable { onBlow() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f

            // Sonic Waves when blowing
            if (isBlowing) {
                drawCircle(
                    color = SacredGold.copy(alpha = waveAlpha),
                    radius = waveRadius,
                    center = Offset(centerX, centerY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = (waveAlpha * 0.7f)),
                    radius = (waveRadius * 0.6f),
                    center = Offset(centerX, centerY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
            }

            // Sacred White Shankh (दक्षिणावर्ती शंख)
            val shankhPath = Path().apply {
                moveTo(centerX - 24f, centerY + 12f)
                cubicTo(
                    centerX - 32f, centerY - 16f,
                    centerX - 10f, centerY - 32f,
                    centerX + 18f, centerY - 28f
                )
                cubicTo(
                    centerX + 34f, centerY - 14f,
                    centerX + 28f, centerY + 16f,
                    centerX + 10f, centerY + 30f
                )
                cubicTo(
                    centerX - 6f, centerY + 34f,
                    centerX - 16f, centerY + 24f,
                    centerX - 24f, centerY + 12f
                )
                close()
            }

            // Pearl White gradient with golden sheen
            drawPath(
                path = shankhPath,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFFFF9E6),
                        Color(0xFFF3E5AB),
                        Color(0xFFE8D8B8)
                    )
                )
            )

            // Shankh Lip & Spiral Trim
            val spiralPath = Path().apply {
                moveTo(centerX - 4f, centerY - 24f)
                cubicTo(
                    centerX + 14f, centerY - 18f,
                    centerX + 16f, centerY + 4f,
                    centerX + 2f, centerY + 18f
                )
            }
            drawPath(
                path = spiralPath,
                color = SacredGold,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
            )
        }
    }
}
