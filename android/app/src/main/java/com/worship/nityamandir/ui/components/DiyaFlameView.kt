package com.worship.nityamandir.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.worship.nityamandir.ui.theme.*

@Composable
fun DiyaFlameView(
    isLit: Boolean,
    modifier: Modifier = Modifier
) {
    // Flicker animation for flame height and wobble
    val infiniteTransition = rememberInfiniteTransition(label = "flameTransition")
    val wobble by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 220, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wobble"
    )

    val heightScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heightScale"
    )

    Box(modifier = modifier.size(100.dp, 80.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val baseTopY = height * 0.65f

            // Draw Brass Diya Base (पीतल का दीपक)
            val diyaBasePath = Path().apply {
                moveTo(centerX - 35f, baseTopY)
                cubicTo(
                    centerX - 40f, height * 0.95f,
                    centerX + 40f, height * 0.95f,
                    centerX + 35f, baseTopY
                )
                close()
            }
            drawPath(
                path = diyaBasePath,
                brush = Brush.verticalGradient(
                    listOf(SacredGold, SacredBrass, Color(0xFF8B6508))
                )
            )

            // Diya Rim / Oil depression
            drawOval(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF5C3810), SacredBrass),
                    center = Offset(centerX, baseTopY)
                ),
                topLeft = Offset(centerX - 35f, baseTopY - 6f),
                size = androidx.compose.ui.geometry.Size(70f, 14f)
            )

            // Cotton Wick (बाती)
            drawLine(
                color = if (isLit) Color(0xFF2B1704) else Color(0xFFE0D8C8),
                start = Offset(centerX, baseTopY + 2f),
                end = Offset(centerX, baseTopY - 10f),
                strokeWidth = 4f
            )

            if (isLit) {
                // Outer Warm Radial Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x99FFA000),
                            Color(0x44FF6F00),
                            Color(0x00FF6F00)
                        ),
                        center = Offset(centerX, baseTopY - 24f),
                        radius = 65f * heightScale
                    ),
                    radius = 65f * heightScale,
                    center = Offset(centerX, baseTopY - 24f)
                )

                // Outer Orange Flame Path
                val outerFlamePath = Path().apply {
                    moveTo(centerX - 10f, baseTopY - 6f)
                    cubicTo(
                        centerX - 16f, baseTopY - 24f,
                        centerX - 8f + wobble, baseTopY - (48f * heightScale),
                        centerX + wobble, baseTopY - (56f * heightScale) // flame tip
                    )
                    cubicTo(
                        centerX + 8f + wobble, baseTopY - (48f * heightScale),
                        centerX + 16f, baseTopY - 24f,
                        centerX + 10f, baseTopY - 6f
                    )
                    close()
                }
                drawPath(
                    path = outerFlamePath,
                    brush = Brush.verticalGradient(
                        listOf(DiyaFlameOuter, Color(0xFFFF9800))
                    )
                )

                // Inner Yellow Flame Core
                val innerFlamePath = Path().apply {
                    moveTo(centerX - 6f, baseTopY - 8f)
                    cubicTo(
                        centerX - 8f, baseTopY - 20f,
                        centerX - 4f + (wobble * 0.5f), baseTopY - (36f * heightScale),
                        centerX + (wobble * 0.5f), baseTopY - (42f * heightScale)
                    )
                    cubicTo(
                        centerX + 4f + (wobble * 0.5f), baseTopY - (36f * heightScale),
                        centerX + 8f, baseTopY - 20f,
                        centerX + 6f, baseTopY - 8f
                    )
                    close()
                }
                drawPath(
                    path = innerFlamePath,
                    brush = Brush.verticalGradient(
                        listOf(DiyaFlameInner, DiyaFlameCore)
                    )
                )
            }
        }
    }
}
