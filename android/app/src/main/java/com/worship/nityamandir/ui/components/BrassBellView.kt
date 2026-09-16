package com.worship.nityamandir.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.worship.nityamandir.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun BrassBellView(
    onRing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val swingAngle = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .size(70.dp, 100.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                coroutineScope.launch {
                    onRing()
                    // Realistic decaying swing: +15 deg -> -12 deg -> +8 deg -> 0 deg
                    swingAngle.animateTo(15f, tween(80, easing = LinearEasing))
                    swingAngle.animateTo(-12f, tween(120, easing = LinearEasing))
                    swingAngle.animateTo(8f, tween(100, easing = LinearEasing))
                    swingAngle.animateTo(-4f, tween(80, easing = LinearEasing))
                    swingAngle.animateTo(0f, tween(60, easing = LinearEasing))
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val centerX = width / 2f

            rotate(degrees = swingAngle.value, pivot = Offset(centerX, 0f)) {
                // Brass Chain / Hanging Link
                drawLine(
                    brush = Brush.verticalGradient(listOf(SacredGold, SacredBrass)),
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, 28f),
                    strokeWidth = 4f
                )

                // Bell Dome & Flare (पीतल की घंटी)
                val bellPath = Path().apply {
                    moveTo(centerX - 8f, 28f)
                    cubicTo(
                        centerX - 10f, 40f,
                        centerX - 24f, 60f,
                        centerX - 28f, 72f
                    )
                    // Lip of the bell
                    lineTo(centerX + 28f, 72f)
                    cubicTo(
                        centerX + 24f, 60f,
                        centerX + 10f, 40f,
                        centerX + 8f, 28f
                    )
                    close()
                }

                drawPath(
                    path = bellPath,
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(0xFF8B6508),
                            SacredGold,
                            SacredBrass,
                            Color(0xFF8B6508)
                        )
                    )
                )

                // Bell Clapper (लोलक / जिह्वा)
                drawCircle(
                    color = SacredGold,
                    radius = 6f,
                    center = Offset(centerX - (swingAngle.value * 0.3f), 78f)
                )
            }
        }
    }
}
