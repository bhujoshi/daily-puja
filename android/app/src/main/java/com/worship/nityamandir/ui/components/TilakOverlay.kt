package com.worship.nityamandir.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.worship.nityamandir.engine.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TilakOverlay(
    applied: Set<Int>,
    placement: IdolPlacement,
    step: WorshipStep
) {
    // Subtle warm readiness aura on the miniature Kumkum, Chandan & Akshat katoris during the Tilak step
    if (step == WorshipStep.TILAK && applied.size < placement.heads.size) {
        Canvas(Modifier.fillMaxSize()) {
            val view = TempleViewport(size.width, size.height)
            val u = view.imageWidth
            val kumkumPt = view.pixel(TempleSceneLayout.kumkumKatori)
            val chandanPt = view.pixel(TempleSceneLayout.chandanKatori)
            val akshatPt = view.pixel(TempleSceneLayout.akshatKatori)
            val haloR = u * .036f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x55E03226), Color(0x22D8AF52), Color.Transparent),
                    center = Offset(kumkumPt.x, kumkumPt.y),
                    radius = haloR
                ),
                radius = haloR,
                center = Offset(kumkumPt.x, kumkumPt.y)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x66F3C65F), Color(0x28D8AF52), Color.Transparent),
                    center = Offset(chandanPt.x, chandanPt.y),
                    radius = haloR
                ),
                radius = haloR,
                center = Offset(chandanPt.x, chandanPt.y)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x44FFFDF8), Color(0x18D8AF52), Color.Transparent),
                    center = Offset(akshatPt.x, akshatPt.y),
                    radius = haloR * .82f
                ),
                radius = haloR * .82f,
                center = Offset(akshatPt.x, akshatPt.y)
            )
        }
    }

    placement.heads.forEachIndexed { index, head ->
        key(index) {
            val reveal = remember { Animatable(0f) }
            LaunchedEffect(index in applied) {
                if (index in applied) {
                    reveal.animateTo(1f, tween(1350, easing = FastOutSlowInEasing))
                } else {
                    reveal.snapTo(0f)
                }
            }
            if (reveal.value > 0f) {
                Canvas(Modifier.fillMaxSize()) {
                    val view = TempleViewport(size.width, size.height)
                    val u = view.imageWidth
                    val point = view.pixel(head)
                    val center = Offset(point.x, point.y)
                    val t = reveal.value
                    val mark = (placement.height * .020f).coerceIn(.0065f, .0095f) * u

                    val kumkumOriginPx = view.pixel(TempleSceneLayout.kumkumKatori).let { Offset(it.x, it.y) }
                    val chandanOriginPx = view.pixel(TempleSceneLayout.chandanKatori).let { Offset(it.x, it.y) }
                    val akshatOriginPx = view.pixel(TempleSceneLayout.akshatKatori).let { Offset(it.x, it.y) }

                    // 1. Wave 1: Sacred Kesar-Chandan Offering Flight from the Chandan Katori to the Forehead
                    if (t < 0.50f) {
                        val cf = (t / 0.44f).coerceIn(0f, 1f)
                        val ripple = (t / 0.24f).coerceIn(0f, 1f)
                        if (ripple < 1f) {
                            drawCircle(
                                color = Color(0xFFF3C65F).copy(alpha = (1f - ripple) * 0.65f),
                                radius = u * (.016f + .026f * ripple),
                                center = chandanOriginPx,
                                style = Stroke(width = u * .0022f)
                            )
                        }
                        val ctrl = Offset(
                            (chandanOriginPx.x + center.x) * 0.5f + u * 0.048f,
                            (chandanOriginPx.y + center.y) * 0.5f - u * 0.055f
                        )
                        drawOfferingStream(
                            start = chandanOriginPx,
                            ctrl = ctrl,
                            end = center,
                            progress = cf,
                            unit = u,
                            coreColor = Color(0xFFFFF3C4),
                            midColor = Color(0xFFF3C65F),
                            outerColor = Color(0xFFD89628),
                            withAkshat = false
                        )
                    }

                    // 2. Wave 2: Ceremonial Kumkum & Akshat Offering Flight from the Kumkum & Akshat Katoris
                    if (t in 0.16f..0.70f) {
                        val kf = ((t - 0.16f) / 0.46f).coerceIn(0f, 1f)
                        val ripple = ((t - 0.16f) / 0.24f).coerceIn(0f, 1f)
                        if (ripple < 1f) {
                            drawCircle(
                                color = Color(0xFFE03226).copy(alpha = (1f - ripple) * 0.65f),
                                radius = u * (.016f + .026f * ripple),
                                center = kumkumOriginPx,
                                style = Stroke(width = u * .0022f)
                            )
                            drawCircle(
                                color = Color(0xFFFFFDF8).copy(alpha = (1f - ripple) * 0.55f),
                                radius = u * (.012f + .020f * ripple),
                                center = akshatOriginPx,
                                style = Stroke(width = u * .0018f)
                            )
                        }
                        val ctrl = Offset(
                            (kumkumOriginPx.x + center.x) * 0.5f - u * 0.048f,
                            (kumkumOriginPx.y + center.y) * 0.5f - u * 0.055f
                        )
                        drawOfferingStream(
                            start = kumkumOriginPx,
                            ctrl = ctrl,
                            end = center,
                            progress = kf,
                            unit = u,
                            coreColor = Color(0xFFFF8A75),
                            midColor = Color(0xFFD92B22),
                            outerColor = Color(0xFF8E1111),
                            withAkshat = true
                        )
                    }

                    // 3. Multi-layered Sacred Chandan + Kumkum + Akshat Tilak on the Deity's Forehead
                    val chandanT = ((t - 0.34f) / 0.28f).coerceIn(0f, 1f)
                    val kumkumT = ((t - 0.54f) / 0.28f).coerceIn(0f, 1f)
                    val akshatT = ((t - 0.74f) / 0.22f).coerceIn(0f, 1f)

                    if (chandanT > 0f) {
                        // Gentle golden-sandalwood blessing ripple on forehead contact
                        if (t < 0.82f) {
                            val pulse = ((t - 0.34f) / 0.46f).coerceIn(0f, 1f)
                            drawCircle(
                                color = Color(0xFFF3C65F).copy(alpha = (1f - pulse) * 0.55f),
                                radius = mark * (1.8f + 3.6f * pulse),
                                center = center,
                                style = Stroke(width = u * .0014f)
                            )
                        }

                        // Soft warm ambient halo behind the tilak for natural optical blending
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0x55F3C65F).copy(alpha = 0.34f * chandanT),
                                    Color(0x33D85816).copy(alpha = 0.18f * chandanT),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = mark * 2.5f
                            ),
                            radius = mark * 2.5f,
                            center = center
                        )

                        // Layer A: Sacred Malayagiri Kesar-Chandan Paste Base (sculpted crescent-winged sandalwood arc)
                        val span = mark * 1.48f * chandanT
                        val arcY = center.y + mark * 0.22f
                        val wingLift = mark * 0.36f * chandanT

                        // 3D warm dark sandalwood-brown contact rim so the creamy Chandan stands out on golden idols
                        val chandanShadowPath = Path().apply {
                            moveTo(center.x - span * 1.06f, arcY - wingLift)
                            quadraticTo(center.x, arcY + mark * 0.58f, center.x + span * 1.06f, arcY - wingLift)
                            quadraticTo(center.x, arcY - mark * 0.24f, center.x - span * 1.06f, arcY - wingLift)
                            close()
                        }
                        drawPath(chandanShadowPath, Color(0xFF4E2A06).copy(alpha = 0.68f * chandanT))

                        // Main Malayagiri Kesar-Chandan sandalwood paste crescent (luminous creamy sandalwood-gold)
                        val chandanBodyPath = Path().apply {
                            moveTo(center.x - span, arcY - wingLift)
                            quadraticTo(center.x, arcY + mark * 0.46f, center.x + span, arcY - wingLift)
                            quadraticTo(center.x, arcY - mark * 0.18f, center.x - span, arcY - wingLift)
                            close()
                        }
                        drawPath(
                            path = chandanBodyPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFFFF9DF), Color(0xFFF9E08E), Color(0xFFE5B248)),
                                startY = arcY - wingLift - mark * 0.2f,
                                endY = arcY + mark * 0.46f
                            )
                        )

                        // Upper parallel sandalwood रेखा (Tripundra / Chandan Rekha) with subtle dark contact shadow
                        val upperSpan = span * 0.80f
                        val upperY = arcY - mark * 0.44f
                        drawLine(
                            color = Color(0xFF4E2A06).copy(alpha = 0.52f * chandanT),
                            start = Offset(center.x - upperSpan, upperY - wingLift * 0.45f + mark * 0.04f),
                            end = Offset(center.x + upperSpan, upperY - wingLift * 0.45f + mark * 0.04f),
                            strokeWidth = mark * 0.34f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(0xFFFFF5CC).copy(alpha = 0.95f * chandanT),
                            start = Offset(center.x - upperSpan, upperY - wingLift * 0.45f),
                            end = Offset(center.x + upperSpan, upperY - wingLift * 0.45f),
                            strokeWidth = mark * 0.26f,
                            cap = StrokeCap.Round
                        )
                        // Creamy sandalwood highlight along crest
                        drawLine(
                            color = Color(0xFFFFFFFF).copy(alpha = 0.82f * chandanT),
                            start = Offset(center.x - span * 0.55f, arcY + mark * 0.04f),
                            end = Offset(center.x + span * 0.55f, arcY + mark * 0.04f),
                            strokeWidth = mark * 0.16f,
                            cap = StrokeCap.Round
                        )
                    }

                    // Layer B: Ceremonial Kumkum / Roli Urdhva Tilak rising over the center of the Chandan base
                    if (kumkumT > 0f) {
                        if (t < 0.98f) {
                            val pulse = ((t - 0.54f) / 0.44f).coerceIn(0f, 1f)
                            drawCircle(
                                color = Color(0xFFE03226).copy(alpha = (1f - pulse) * 0.48f),
                                radius = mark * (1.4f + 3.2f * pulse),
                                center = center,
                                style = Stroke(width = u * .0012f)
                            )
                        }

                        val baseY = center.y + mark * 0.88f
                        val tipY = baseY - mark * 2.40f * kumkumT
                        val halfBaseW = mark * 0.50f * (0.4f + 0.6f * kumkumT)

                        // Deep crimson contact shadow rim
                        val shadowPath = buildUrdhvaTilakPath(
                            cx = center.x,
                            baseY = baseY + mark * 0.06f,
                            tipY = tipY - mark * 0.04f,
                            halfBaseW = halfBaseW * 1.14f
                        )
                        drawPath(shadowPath, Color(0xFF5A0909).copy(alpha = 0.62f))

                        // Velvety Ceremonial Kumkum body
                        val kumkumPath = buildUrdhvaTilakPath(
                            cx = center.x,
                            baseY = baseY,
                            tipY = tipY,
                            halfBaseW = halfBaseW
                        )
                        drawPath(
                            path = kumkumPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFE53528), Color(0xFFC21E1A), Color(0xFF941111)),
                                startY = tipY,
                                endY = baseY
                            )
                        )

                        // Subtle central vermilion sheen
                        if (kumkumT > 0.35f) {
                            drawLine(
                                color = Color(0xFFFF7A66).copy(alpha = 0.42f * kumkumT),
                                start = Offset(center.x, baseY - mark * 0.28f),
                                end = Offset(center.x, tipY + mark * 0.45f),
                                strokeWidth = mark * 0.18f,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // Layer C: Sacred Pearl-White Akshat (unbroken rice grains) pressed at the tilak bindu base
                    if (akshatT > 0f) {
                        val binduY = center.y + mark * 0.56f
                        val grainW = mark * 0.21f * akshatT
                        val grainH = mark * 0.44f * akshatT
                        val grains = listOf(
                            Triple(-mark * 0.20f, -mark * 0.04f, -24f),
                            Triple(0f, mark * 0.05f, 4f),
                            Triple(mark * 0.20f, -mark * 0.03f, 26f)
                        )
                        grains.forEach { (dx, dy, angle) ->
                            val gx = center.x + dx
                            val gy = binduY + dy
                            withTransform({
                                rotate(degrees = angle, pivot = Offset(gx, gy))
                            }) {
                                drawOval(
                                    color = Color(0xFF5A180C).copy(alpha = 0.55f * akshatT),
                                    topLeft = Offset(gx - grainW * 0.62f, gy - grainH * 0.55f + mark * 0.03f),
                                    size = Size(grainW * 1.24f, grainH * 1.10f)
                                )
                                drawOval(
                                    color = Color(0xFFFFFDF6).copy(alpha = 0.96f * akshatT),
                                    topLeft = Offset(gx - grainW * 0.5f, gy - grainH * 0.5f),
                                    size = Size(grainW, grainH)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildUrdhvaTilakPath(cx: Float, baseY: Float, tipY: Float, halfBaseW: Float): Path {
    val height = (baseY - tipY).coerceAtLeast(1f)
    return Path().apply {
        moveTo(cx, tipY)
        // Right curve from pointed sacred tip down to rounded base
        cubicTo(
            cx + halfBaseW * 0.35f, tipY + height * 0.38f,
            cx + halfBaseW * 1.08f, baseY - height * 0.28f,
            cx + halfBaseW * 0.85f, baseY - halfBaseW * 0.25f
        )
        // Rounded fuller bottom bindu curve
        quadraticTo(cx, baseY + halfBaseW * 0.55f, cx - halfBaseW * 0.85f, baseY - halfBaseW * 0.25f)
        // Left curve back up to pointed tip
        cubicTo(
            cx - halfBaseW * 1.08f, baseY - height * 0.28f,
            cx - halfBaseW * 0.35f, tipY + height * 0.38f,
            cx, tipY
        )
        close()
    }
}

private fun DrawScope.drawOfferingStream(
    start: Offset,
    ctrl: Offset,
    end: Offset,
    progress: Float,
    unit: Float,
    coreColor: Color,
    midColor: Color,
    outerColor: Color,
    withAkshat: Boolean
) {
    fun bezier(t: Float): Offset {
        val inv = 1f - t
        return Offset(
            inv * inv * start.x + 2f * inv * t * ctrl.x + t * t * end.x,
            inv * inv * start.y + 2f * inv * t * ctrl.y + t * t * end.y
        )
    }

    val headT = progress.coerceIn(0f, 1f)
    val tailT = (headT - 0.32f).coerceIn(0f, 1f)
    val fade = if (headT > 0.82f) (1f - headT) / 0.18f else 1f
    if (fade <= 0.01f) return

    val steps = 14
    for (i in 0 until steps) {
        val f0 = tailT + (headT - tailT) * (i.toFloat() / steps)
        val f1 = tailT + (headT - tailT) * ((i + 1).toFloat() / steps)
        val p0 = bezier(f0)
        val p1 = bezier(f1)
        val segFraction = (i + 1).toFloat() / steps
        val strokeW = unit * (.002f + .0065f * segFraction)
        drawLine(
            color = midColor.copy(alpha = 0.68f * segFraction * fade),
            start = p0,
            end = p1,
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }

    val headPt = bezier(headT)
    val haloRadius = unit * .018f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(coreColor.copy(alpha = 0.95f * fade), midColor.copy(alpha = 0.65f * fade), outerColor.copy(alpha = 0f)),
            center = headPt,
            radius = haloRadius
        ),
        radius = haloRadius,
        center = headPt
    )
    drawCircle(
        color = coreColor.copy(alpha = 0.95f * fade),
        radius = unit * .0052f,
        center = headPt
    )

    if (withAkshat) {
        // 3 glistening pearl-white Akshat rice grains travelling along the Kumkum stream
        for (g in 0..2) {
            val gt = (headT - g * 0.065f).coerceIn(0f, 1f)
            if (gt > 0.04f) {
                val gp = bezier(gt)
                val wobble = sin((gt * 4f + g) * PI.toFloat()) * unit * .006f
                val grainCenter = Offset(gp.x + wobble, gp.y - g * unit * .003f)
                withTransform({
                    rotate(degrees = -20f + g * 22f, pivot = grainCenter)
                }) {
                    drawOval(
                        color = Color(0xFFFFFDF8).copy(alpha = 0.92f * fade),
                        topLeft = Offset(grainCenter.x - unit * .0022f, grainCenter.y - unit * .0045f),
                        size = Size(unit * .0044f, unit * .0090f)
                    )
                }
            }
        }
    }
}
