package com.worship.nityamandir.ui.components

import android.animation.ValueAnimator
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.worship.nityamandir.engine.DeepmalaDiyaPose
import com.worship.nityamandir.engine.TemplePoint
import com.worship.nityamandir.engine.TempleViewport
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Permanent festive dressing, behind idols and ritual objects, independent of puja progress. */
@Composable
internal fun DiwaliDecorations(viewport: TempleViewport, animate: Boolean) {
    val orange = rememberCachedAssetImage("shrine/flowers/sunflower.png")
    val yellow = rememberCachedAssetImage("shrine/flowers/marigold_yellow.png")
    val rose = rememberCachedAssetImage("shrine/flowers/rose.png")
    val flowers = remember(orange, yellow, rose) { listOf(orange, yellow, rose) }
    val petals = remember {
        val random = Random(108)
        List(110) { index ->
            // A petal carpet below the step and two trails beside the central ritual space.
            val onStep = index < 64
            val x = if (onStep) .22f + random.nextFloat() * .56f
                else (if (index % 2 == 0) .12f else .82f) + random.nextFloat() * .06f
            val y = if (onStep) .81f + random.nextFloat() * .045f
                else .79f + random.nextFloat() * .35f
            FestivalPetal(TemplePoint(x, y), random.nextFloat() * 360f,
                .004f + random.nextFloat() * .004f, index % 4)
        }
    }
    val lights = remember {
        buildList {
            repeat(29) { index ->
                val t = index / 28f
                add(TemplePoint(.18f + .64f * t, .13f + .075f * sin(PI.toFloat() * t)))
            }
            for (x in listOf(.125f, .875f)) {
                repeat(11) { index ->
                    add(TemplePoint(x + .008f * sin(index * 1.4f), .22f + index * .043f))
                }
            }
        }
    }
    val diyas = remember {
        buildList {
            for (side in listOf(-1f, 1f)) {
                repeat(4) { index ->
                    add(DeepmalaDiyaPose(
                        index = size, tier = 1,
                        position = TemplePoint(.5f + side * (.335f + index * .008f), .72f + index * .125f),
                        scale = 1.05f + index * .07f, vesselOpacity = 1f,
                        flameIgnite = 1f, sparkBurst = 0f, floorReflection = .85f
                    ))
                }
            }
        }
    }

    // Only this small lighting layer redraws during flicker. Flowers remain static.
    var seconds by remember { mutableFloatStateOf(0f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(animate, lifecycle) {
        if (!animate) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val start = SystemClock.uptimeMillis() - (seconds * 1000).toLong()
            while (isActive) {
                if (ValueAnimator.areAnimatorsEnabled()) {
                    seconds = (SystemClock.uptimeMillis() - start) / 1000f
                }
                delay(if (ValueAnimator.areAnimatorsEnabled()) 33L else 500L)
            }
        }
    }

    Box(Modifier.fillMaxSize().drawWithCache {
        val u = viewport.imageWidth
        val cables = Path().apply {
            lights.forEachIndexed { index, point ->
                val p = viewport.pixel(point)
                if (index == 0 || index == 29 || index == 40) moveTo(p.x, p.y)
                else lineTo(p.x, p.y)
            }
        }
        val petalShape = Path().apply {
            moveTo(-1f, 0f)
            cubicTo(-.4f, -1f, .8f, -.7f, 1f, 0f)
            cubicTo(.4f, .8f, -.6f, .6f, -1f, 0f)
            close()
        }
        val petalColors = listOf(Color(0xFFE26A8D), Color(0xFFFFC837), Color(0xFFD85816), Color(0xFFA82424))
        onDrawBehind {
            drawPath(cables, Color(0xFFB3833B).copy(alpha = .7f), style = Stroke(u * .0015f))

            // Twin flower swags follow the lintel, keeping the centre of the sanctum open.
            repeat(2) { swag ->
                repeat(23) { index ->
                    val t = index / 22f
                    val point = TemplePoint(.19f + swag * .31f + .31f * t,
                        .085f + .066f * sin(PI.toFloat() * t))
                    drawFestivalFlower(flowers[if (index % 5 == 0) 2 else index % 2],
                        viewport, point, .032f, index * 27f)
                }
            }
            // Layered genda and rose clusters at each pillar foot and across the step.
            repeat(31) { index ->
                drawFestivalFlower(flowers[if (index % 7 == 0) 2 else index % 2], viewport,
                    TemplePoint(.205f + index * .0197f, .716f + .006f * sin(index * 2.4f)),
                    .030f + .004f * (index % 3), index * 31f)
            }
            for (x in listOf(.185f, .815f)) {
                repeat(5) { index ->
                    drawFestivalFlower(flowers[index % 2], viewport,
                        TemplePoint(x + .008f * sin(index * 2f), .60f + index * .019f),
                        .035f, index * 41f)
                }
            }
            petals.forEach { petal ->
                val p = viewport.pixel(petal.point)
                val center = Offset(p.x, p.y)
                val radius = u * petal.radius
                drawOval(Color(0x550D0B0A), center + Offset(-radius, radius * .1f), Size(radius * 2, radius * .65f))
                withTransform({
                    translate(center.x, center.y)
                    rotate(petal.angle, Offset.Zero)
                    scale(radius, radius * .65f, Offset.Zero)
                }) {
                    drawPath(petalShape, petalColors[petal.color])
                    drawPath(petalShape, Color(0x33FFFDF8), style = Stroke(.12f))
                }
            }
        }
    })
    Canvas(Modifier.fillMaxSize()) {
        val time = seconds
        val u = viewport.imageWidth
        lights.forEachIndexed { index, point ->
            val p = viewport.pixel(point)
            val center = Offset(p.x, p.y + u * .006f)
            val glow = .78f + .12f * sin(time * 1.7f + index * .61f)
            drawLine(Color(0xFFC5A059), Offset(p.x, p.y), center, u * .002f)
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFF9E2C).copy(alpha = .32f * glow),
                Color.Transparent), center, u * .022f), u * .022f, center)
            drawCircle(Color(0xFFFFC76A).copy(alpha = glow), u * .0045f, center)
            drawCircle(Color(0xFFFFF2D5), u * .0022f, center)
        }
        diyas.forEach { drawDeepmalaDiya(it, time, viewport) }
    }
}

private data class FestivalPetal(val point: TemplePoint, val angle: Float, val radius: Float, val color: Int)

private fun DrawScope.drawFestivalFlower(
    bitmap: ImageBitmap, viewport: TempleViewport, point: TemplePoint, width: Float, angle: Float
) {
    val p = viewport.pixel(point)
    val w = viewport.imageWidth * width
    val h = w * bitmap.height / bitmap.width
    rotate(angle, Offset(p.x, p.y)) {
        drawImage(bitmap, dstOffset = IntOffset((p.x - w / 2).toInt(), (p.y - h / 2).toInt()),
            dstSize = IntSize(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1)))
    }
}
