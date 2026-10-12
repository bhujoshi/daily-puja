package com.worship.nityamandir.ui.components

import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.worship.nityamandir.data.ShrineSelection
import com.worship.nityamandir.engine.GaneshMouseAnimation
import com.worship.nityamandir.engine.GaneshMousePose
import com.worship.nityamandir.engine.IdolPlacement
import com.worship.nityamandir.engine.TempleViewport
import kotlin.math.sin

/** A single visit after the laddu is offered; the mouse then rests at the altar. */
@Composable
fun GaneshMouseOverlay(
    placement: IdolPlacement,
    selection: ShrineSelection,
    offered: Boolean,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = remember(placement, offered) { Animatable(0f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(progress, visible, lifecycle) {
        if (!offered || !visible) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Resume the remaining visit after backgrounding, without replaying the meal.
            val remaining = ((1f - progress.value) * GaneshMouseAnimation.DURATION_MILLIS).toInt()
            if (remaining > 0) progress.animateTo(1f, tween(remaining, easing = LinearEasing))
        }
    }
    if (!offered || !visible) return
    Canvas(modifier.fillMaxSize().zIndex(2f).semantics {
        contentDescription = "मूषक लड्डू खाकर गणेश जी के चरणों में जाता है · Mouse eats a laddu and returns to Ganesh ji’s feet"
    }) {
        val viewport = TempleViewport(size.width, size.height)
        val pose = GaneshMouseAnimation.pose(progress.value, placement, selection)
        drawGaneshMouse(pose, viewport)
    }
}

/** Shaded vector artwork stays crisp at every temple size, with no bitmap frames to load. */
private fun DrawScope.drawGaneshMouse(pose: GaneshMousePose, viewport: TempleViewport) {
    val point = viewport.pixel(pose.position)
    val unit = viewport.imageWidth * .085f * pose.scale
    val alpha = pose.opacity
    val stride = sin(pose.phaseSeconds * 22f) * pose.scurry
    val nibble = sin(pose.phaseSeconds * 17f) * pose.eating
    val bounce = kotlin.math.abs(stride) * 2.5f
    fun ink(hex: Long) = Color(hex).copy(alpha = alpha)

    // A soft contact shadow stays on the surface while the mouse hops.
    repeat(5) { i ->
        val width = unit * (1.02f - i * .13f)
        drawOval(Color(0xFF38261A).copy(alpha = .035f * alpha * (1f - pose.hop * .65f)),
            Offset(point.x - width * .52f, point.y + viewport.imageWidth * .045f * pose.hop - unit * .025f),
            Size(width, unit * .12f))
    }
    translate(point.x, point.y - bounce * unit / 100f) {
        scale(unit / 100f, unit / 100f, pivot = Offset.Zero) {
            scale(if (pose.facingRight) 1f else -1f, 1f, pivot = Offset.Zero) {
                val crouch = pose.scurry
                val headX = 17f + 14f * crouch
                val headY = -58f + 16f * crouch + nibble * 1.2f
                val tail = Path().apply {
                    moveTo(-26f, -13f)
                    cubicTo(-49f, -4f, -67f, -4f + stride * 4f, -76f, -16f)
                    cubicTo(-87f, -33f, -98f, -21f, -94f, -11f + stride * 5f)
                }
                drawPath(tail, ink(0xFF695145), style = Stroke(5.5f, cap = StrokeCap.Round))
                drawPath(tail, ink(0xFFC39783), style = Stroke(3f, cap = StrokeCap.Round))

                // Far feet and ear sit behind the body.
                drawOval(ink(0xFF927260), Offset(-23f - stride * 5f, -8f), Size(24f, 8f))
                drawOval(ink(0xFF927260), Offset(13f + stride * 5f, -7f), Size(20f, 7f))
                drawCircle(ink(0xFF695F54), 12f, Offset(headX + 10f, headY - 20f))
                drawCircle(ink(0xFFB48B7D), 8f, Offset(headX + 10f, headY - 20f))

                val body = Path().apply {
                    moveTo(-29f, -9f)
                    cubicTo(-45f, -27f, -33f, -50f + 13f * crouch, -17f, -55f + 12f * crouch)
                    cubicTo(1f, -66f + 18f * crouch, 18f, -48f + 12f * crouch, 23f, -24f)
                    cubicTo(34f, -5f, 5f, 4f, -29f, -9f)
                    close()
                }
                drawPath(body, Brush.radialGradient(listOf(ink(0xFFB5A695), ink(0xFF87786A), ink(0xFF51463D)),
                    center = Offset(-15f, -39f), radius = 46f))
                drawOval(Brush.verticalGradient(listOf(ink(0xFFD4C4AB), ink(0xFF9C8871)),
                    startY = -43f, endY = -4f), Offset(-2f, -43f + 10f * crouch), Size(23f, 37f - 9f * crouch))
                // Fine flank strokes suggest fur without busy detail at phone scale.
                repeat(7) { i ->
                    drawLine(ink(0xFFC2B4A0).copy(alpha = alpha * .30f),
                        Offset(-28f + i * 4f, -29f - (i % 3) * 5f + crouch * 8f),
                        Offset(-25f + i * 4f, -33f - (i % 3) * 5f + crouch * 8f), 1f, StrokeCap.Round)
                }

                // Near feet alternate during the scurry; all four settle on landing.
                drawOval(ink(0xFFBD9882), Offset(-20f + stride * 6f, -7f), Size(25f, 8f))
                drawOval(ink(0xFFC6A28C), Offset(12f - stride * 6f, -6f), Size(23f, 7f))
                repeat(3) { toe ->
                    drawLine(ink(0xFF806253), Offset(25f + toe * 3f - stride * 6f, -3f),
                        Offset(26f + toe * 3f - stride * 6f, -.5f), .8f)
                }

                rotate(pose.bow * 19f - pose.hop * 10f, pivot = Offset(headX - 8f, headY + 12f)) {
                    val head = Path().apply {
                        moveTo(headX - 20f, headY + 9f)
                        cubicTo(headX - 27f, headY - 10f, headX - 9f, headY - 25f, headX + 8f, headY - 14f)
                        cubicTo(headX + 16f, headY - 8f, headX + 27f, headY + 1f, headX + 31f, headY + 7f)
                        cubicTo(headX + 19f, headY + 19f, headX - 8f, headY + 23f, headX - 20f, headY + 9f)
                        close()
                    }
                    drawPath(head, Brush.radialGradient(listOf(ink(0xFFBBAD99), ink(0xFF8E7D6A), ink(0xFF5D4D40)),
                        center = Offset(headX + 2f, headY - 5f), radius = 33f))
                    drawCircle(Brush.radialGradient(listOf(ink(0xFFB8AA96), ink(0xFF65584B)),
                        center = Offset(headX - 12f, headY - 20f), radius = 15f), 15f, Offset(headX - 13f, headY - 20f))
                    drawCircle(Brush.radialGradient(listOf(ink(0xFFD8AE9E), ink(0xFF967163)),
                        center = Offset(headX - 13f, headY - 22f), radius = 11f), 10.5f, Offset(headX - 13f, headY - 20f))
                    drawOval(ink(0xFFDBC8AE), Offset(headX + 12f, headY + 4f), Size(18f, 11f + nibble))
                    drawOval(ink(0xFF3B2820), Offset(headX + 27f, headY + 4f), Size(7f, 5f))
                    drawCircle(ink(0xFF181311), 4f, Offset(headX + 10f, headY - 5f))
                    drawCircle(ink(0xFFFFF9E9), 1.25f, Offset(headX + 11f, headY - 6.5f))
                    drawLine(ink(0xFF594234), Offset(headX + 22f, headY + 13f),
                        Offset(headX + 28f, headY + 12f + nibble), 1.4f, StrokeCap.Round)
                    repeat(3) { whisker ->
                        val y = headY + 8f + whisker * 2.4f
                        drawLine(ink(0xFFDCCDB9).copy(alpha = alpha * .85f), Offset(headX + 25f, y),
                            Offset(headX + 44f, y + (whisker - 1) * 5f), .8f, StrokeCap.Round)
                    }
                }

                val pawY = -30f + crouch * 13f + nibble * 1.7f
                drawLine(ink(0xFF786352), Offset(7f, -35f + crouch * 8f), Offset(25f, pawY), 7f, StrokeCap.Round)
                if (pose.ladduRemaining > 0f) {
                    val radius = 10f * pose.ladduRemaining
                    val laddu = Offset(29f, pawY - 6f)
                    drawCircle(Brush.radialGradient(listOf(ink(0xFFFFD573), ink(0xFFE6A032), ink(0xFFA9651D)),
                        center = laddu - Offset(radius * .3f, radius * .4f), radius = radius * 1.5f), radius, laddu)
                    // Small besan grains give the sweet its familiar rough texture.
                    repeat(11) { grain ->
                        val angle = grain * 2.4f
                        val distance = radius * (.25f + (grain % 3) * .20f)
                        drawCircle(ink(if (grain % 2 == 0) 0xFFFFE5A1 else 0xFFC08127), radius * .10f,
                            laddu + Offset(kotlin.math.cos(angle) * distance, sin(angle) * distance))
                    }
                    if (pose.eating > 0f) repeat(3) { crumb ->
                        val fall = ((pose.phaseSeconds * 1.7f + crumb * .32f) % 1f)
                        drawCircle(ink(0xFFE9AE45).copy(alpha = alpha * (1f - fall)), 1.5f,
                            Offset(28f + crumb * 4f, pawY + fall * 25f))
                    }
                }
                drawOval(ink(0xFFD0AD94), Offset(22f, pawY - 1f), Size(10f, 6f))
                drawLine(ink(0xFF9E8872), Offset(14f, -32f + crouch * 7f), Offset(31f, pawY + 4f), 5f, StrokeCap.Round)
                drawOval(ink(0xFFE0BCA2), Offset(28f, pawY + 1f), Size(9f, 5f))
            }
        }
    }
}
