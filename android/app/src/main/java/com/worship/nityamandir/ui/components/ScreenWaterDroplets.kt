package com.worship.nityamandir.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.worship.nityamandir.engine.AbhishekWaterPhysics
import com.worship.nityamandir.engine.IdolPlacement
import com.worship.nityamandir.engine.IdolWaterSurface
import com.worship.nityamandir.engine.TemplePoint
import com.worship.nityamandir.engine.TempleViewport
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Delicate glistening Abhishek water beads strictly confined to each bathed idol's
 * actual solid surface (never scattered across the screen, walls, or Puja Thali).
 * Beads appear progressively as water washes down, gently trickle along the idol's
 * contours, and naturally evaporate after bathing completes.
 */
@Composable
fun ScreenWaterDroplets(
    bathTarget: Int?,
    progress: Float,
    bathed: Set<Int>,
    deityCount: Int,
    placement: IdolPlacement,
    surface: IdolWaterSurface,
    modifier: Modifier = Modifier
) {
    val safeCount = deityCount.coerceAtLeast(1)
    val deityBeads = remember(placement, surface, safeCount) {
        List(safeCount) { deity ->
            AbhishekWaterPhysics.buildPlan(placement, deity, surface).residualBeads
        }
    }
    val opacities = remember(safeCount) { List(safeCount) { Animatable(0f) } }
    val trickles = remember(safeCount) { List(safeCount) { Animatable(0f) } }

    for (deity in 0 until safeCount) {
        val activePour = bathTarget == deity
        val isBathed = deity in bathed
        LaunchedEffect(deity, activePour, isBathed) {
            if (activePour) {
                opacities[deity].snapTo(1f)
                trickles[deity].snapTo(0f)
            } else if (isBathed) {
                opacities[deity].snapTo(1f)
                val slideJob = launch {
                    trickles[deity].animateTo(1f, tween(2400, easing = FastOutSlowInEasing))
                }
                delay(1200)
                opacities[deity].animateTo(0f, tween(4200, easing = LinearEasing))
                slideJob.cancel()
            } else {
                opacities[deity].snapTo(0f)
                trickles[deity].snapTo(0f)
            }
        }
    }

    // No pointer handlers: all altar & ritual controls remain responsive beneath.
    Canvas(modifier.fillMaxSize()) {
        if (size.width <= 1f || size.height <= 1f) return@Canvas
        val viewport = TempleViewport(size.width, size.height)
        val u = viewport.imageWidth

        deityBeads.forEachIndexed { deity, beads ->
            val fade = opacities[deity].value
            if (fade <= 0.005f) return@forEachIndexed
            val activePour = bathTarget == deity
            val completed = deity in bathed
            if (!completed && !activePour) return@forEachIndexed
            val slide = trickles[deity].value

            beads.forEach { bead ->
                val appear = if (activePour) {
                    ((progress - bead.arrivalProgress) / 0.06f).coerceIn(0f, 1f)
                } else {
                    1f
                }
                if (appear <= 0.01f) return@forEach

                val alpha = fade * appear
                val tSlide = ((slide - bead.slidePhase * 0.25f) / 0.75f).coerceIn(0f, 1f)
                val posWorld = TemplePoint(
                    bead.start.x + (bead.end.x - bead.start.x) * tSlide,
                    bead.start.y + (bead.end.y - bead.start.y) * tSlide
                )
                val posPx = viewport.pixel(posWorld).let { Offset(it.x, it.y) }
                val r = u * 0.0028f * bead.radiusScale * (0.85f + 0.15f * appear)
                val ry = r * (1.18f + 0.18f * tSlide)
                val topLeft = Offset(posPx.x - r, posPx.y - ry)
                val dims = Size(r * 2f, ry * 2f)
                val rim = (r * 0.16f).coerceAtLeast(0.7f)

                // Soft refractive shadow & crystalline body tinted to harmonize with warm sanctum lighting.
                drawOval(
                    color = Color(0xFF1A1410).copy(alpha = 0.16f * alpha),
                    topLeft = topLeft + Offset(r * 0.12f, ry * 0.18f),
                    size = dims
                )
                drawOval(
                    brush = Brush.radialGradient(
                        listOf(
                            Color(0xFFEAF6F7).copy(alpha = 0.10f * alpha),
                            Color(0xFFCDE6EA).copy(alpha = 0.22f * alpha),
                            Color(0xFF32484C).copy(alpha = 0.34f * alpha)
                        ),
                        center = posPx - Offset(r * 0.18f, ry * 0.20f),
                        radius = maxOf(r, ry) * 1.25f
                    ),
                    topLeft = topLeft,
                    size = dims
                )
                drawArc(
                    color = Color(0xFFFFFDF8).copy(alpha = 0.78f * alpha),
                    startAngle = 205f,
                    sweepAngle = 78f,
                    useCenter = false,
                    topLeft = topLeft + Offset(r * 0.20f, ry * 0.16f),
                    size = Size(r * 1.50f, ry * 1.54f),
                    style = Stroke(width = rim * 1.15f, cap = StrokeCap.Round)
                )
                drawCircle(
                    color = Color(0xFFFFFDF8).copy(alpha = 0.74f * alpha),
                    radius = (r * 0.22f).coerceAtLeast(0.8f),
                    center = posPx - Offset(r * 0.28f, ry * 0.32f)
                )
            }
        }
    }
}
