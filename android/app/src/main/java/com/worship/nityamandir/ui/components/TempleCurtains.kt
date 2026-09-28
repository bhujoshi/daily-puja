package com.worship.nityamandir.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Procedural silk keeps folds crisp at every device size, without a bitmap seam. */
@Composable
fun TempleCurtains(openProgress: Float, saffron: Boolean) {
    val progress = openProgress.coerceIn(0f, 1f)
    val silk = if (saffron) Color(0xFFB98249) else Color(0xFFD6BE98)
    val shade = if (saffron) Color(0xFF81532F) else Color(0xFF9C8060)
    val light = if (saffron) Color(0xFFE8BC80) else Color(0xFFF5E4C7)
    val gold = Color(0xFFE9CC93)
    Box(Modifier.fillMaxSize().clipToBounds()) {
        listOf(-1f, 1f).forEach { direction ->
            Canvas(Modifier.fillMaxWidth(.5f).fillMaxHeight()
                .align(if (direction < 0) Alignment.CenterStart else Alignment.CenterEnd)
                .graphicsLayer {
                    translationX = direction * size.width * progress
                    scaleX = if (direction > 0) -1f else 1f
                }) {
                val w = size.width
                val h = size.height
                // The same cloth tone at both inside edges avoids a dark center seam.
                drawRect(Brush.verticalGradient(listOf(light, silk, silk, shade)))
                val folds = 6
                val pitch = w / folds
                repeat(folds) { i ->
                    val x = i * pitch
                    val fold = Path().apply {
                        moveTo(x, 0f)
                        cubicTo(x - pitch * .14f, h * .28f, x + pitch * .15f, h * .66f, x, h)
                        lineTo(x + pitch, h)
                        cubicTo(x + pitch * 1.12f, h * .66f, x + pitch * .88f, h * .28f, x + pitch, 0f)
                        close()
                    }
                    drawPath(fold, Brush.horizontalGradient(listOf(
                        silk, shade.copy(alpha = .52f), silk, light.copy(alpha = .72f), silk
                    ), x - pitch * .15f, x + pitch * 1.15f))
                }
                // Broad reflected light gives silk depth rather than hard vertical stripes.
                drawRect(Brush.radialGradient(listOf(light.copy(alpha = .35f), Color.Transparent),
                    Offset(w * .85f, h * .25f), h * .65f))
                // A scalloped drape crowns each moving panel.
                val depth = (h * .14f).coerceAtMost(120.dp.toPx())
                val drape = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w, 0f)
                    lineTo(w, depth * .48f)
                    cubicTo(w * .72f, depth * 1.15f, w * .28f, depth * 1.15f, 0f, depth * .48f)
                    close()
                }
                drawPath(drape, Brush.verticalGradient(listOf(shade, silk, light), 0f, depth))
                val edge = Path().apply {
                    moveTo(0f, depth * .48f)
                    cubicTo(w * .28f, depth * 1.15f, w * .72f, depth * 1.15f, w, depth * .48f)
                }
                drawPath(edge, gold.copy(alpha = .7f), style = Stroke(1.5.dp.toPx()))
                // Soft curved stitching echoes the drape without adding heavy ornament.
                clipPath(drape) {
                    repeat(3) { i ->
                        val y = depth * (.16f + i * .16f)
                        val stitch = Path().apply {
                            moveTo(0f, y)
                            quadraticBezierTo(w * .5f, y + depth * .45f, w, y)
                        }
                        drawPath(stitch, light.copy(alpha = .15f), style = Stroke(1.dp.toPx()))
                    }
                }
            }
        }
        // A quiet lotus seal belongs to the welcome state and dissolves as the doors part.
        val sealAlpha = (1f - progress * 5f).coerceIn(0f, 1f)
        if (sealAlpha > 0f) Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = sealAlpha }) {
            val center = Offset(size.width * .5f, size.height * .38f)
            val radius = 32.dp.toPx()
            drawCircle(Brush.radialGradient(listOf(light.copy(alpha = .4f), Color.Transparent), center, radius * 2.8f), radius * 2.8f, center)
            drawCircle(gold.copy(alpha = .45f), radius * 1.35f, center, style = Stroke(.75.dp.toPx()))
            val ink = shade.copy(alpha = .8f)
            listOf(-1f, -.5f, 0f, .5f, 1f).forEach { lean ->
                val petal = Path().apply {
                    val base = center + Offset(0f, radius * .45f)
                    val tip = center + Offset(lean * radius, -radius * (.8f - .35f * kotlin.math.abs(lean)))
                    moveTo(base.x, base.y)
                    quadraticBezierTo(tip.x - radius * .34f, center.y, tip.x, tip.y)
                    quadraticBezierTo(tip.x + radius * .34f, center.y + radius * .2f, base.x, base.y)
                }
                drawPath(petal, ink, style = Stroke(1.2.dp.toPx()))
            }
            drawLine(ink, center + Offset(-radius * .75f, radius * .65f), center + Offset(radius * .75f, radius * .65f), 1.dp.toPx())
        }
    }
}
