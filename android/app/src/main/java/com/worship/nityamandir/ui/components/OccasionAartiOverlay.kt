package com.worship.nityamandir.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.worship.nityamandir.data.ShrineSelection
import com.worship.nityamandir.engine.DeepmalaDiyaPose
import com.worship.nityamandir.engine.IdolPlacement
import com.worship.nityamandir.engine.OccasionAartiAnimation
import com.worship.nityamandir.engine.OccasionAartiPose
import com.worship.nityamandir.engine.OccasionCelebrationStyle
import com.worship.nityamandir.engine.RangoliPose
import com.worship.nityamandir.engine.SacredCharanPose
import com.worship.nityamandir.engine.TempleOccasion
import com.worship.nityamandir.engine.TempleViewport
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders the post-Aarti occasion celebration in the temple sanctum:
 * - Blooming floor Ashtadala Kamal Rangoli
 * - Sacred stepping Lakshmi Charan (auspicious footprints)
 * - Sequential Deepmala clay diyas lighting up along the altar ledge and floor arc
 */
@Composable
fun OccasionAartiOverlay(
    placement: IdolPlacement,
    selection: ShrineSelection,
    activeOccasion: TempleOccasion?,
    aartiComplete: Boolean,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val occasion = remember(selection, activeOccasion) {
        OccasionAartiAnimation.occasionFor(selection, activeOccasion)
    } ?: return
    if (!aartiComplete || !visible) return

    val progress = remember(placement, occasion.id, aartiComplete) { Animatable(0f) }
    var elapsedSeconds by remember(placement, occasion.id, aartiComplete) {
        mutableFloatStateOf(0f)
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(progress, visible, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val remaining = ((1f - progress.value) * OccasionAartiAnimation.DURATION_MILLIS).toInt()
            if (remaining > 0) {
                progress.animateTo(1f, tween(remaining, easing = LinearEasing))
            }
        }
    }

    LaunchedEffect(lifecycle, occasion.id) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val startMs = android.os.SystemClock.uptimeMillis() - (elapsedSeconds * 1000f).toLong()
            while (isActive) {
                delay(33L)
                elapsedSeconds = (android.os.SystemClock.uptimeMillis() - startMs) / 1000f
            }
        }
    }

    Canvas(
        modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "${occasion.titleHi} · ${occasion.subtitleHi} · ${occasion.titleEn} celebration"
            }
    ) {
        val viewport = TempleViewport(size.width, size.height)
        val pose = OccasionAartiAnimation.pose(
            progress = progress.value,
            placement = placement,
            selection = selection,
            activeOccasion = occasion,
            elapsedSeconds = elapsedSeconds
        )
        drawOccasionCelebration(pose, viewport)
    }
}

private fun DrawScope.drawOccasionCelebration(pose: OccasionAartiPose, viewport: TempleViewport) {
    drawOccasionRangoli(pose.rangoli, pose.style, viewport)
    pose.charanSteps.forEach { step ->
        if (step.imprintOpacity > 0.005f) {
            drawLakshmiCharan(step, pose.style, viewport)
        }
    }
    pose.diyas.forEach { diya ->
        if (diya.vesselOpacity > 0.005f) {
            drawDeepmalaDiya(diya, pose.phaseSeconds, viewport)
        }
    }
}

private fun DrawScope.drawOccasionRangoli(
    rangoli: RangoliPose,
    style: OccasionCelebrationStyle,
    viewport: TempleViewport
) {
    if (rangoli.innerBloom <= 0.005f) return
    val center = viewport.pixel(rangoli.center)
    val baseRadius = viewport.imageWidth * rangoli.radiusX
    val primaryPetal = when (style) {
        OccasionCelebrationStyle.DIWALI_DEEPMALA -> Color(0xFFFF7A18)
        OccasionCelebrationStyle.NAVRATRI_SHAKTI -> Color(0xFFD85816)
        OccasionCelebrationStyle.JANMASHTAMI_MORPANKH -> Color(0xFF2B6CB0)
        OccasionCelebrationStyle.GANESH_MODAK -> Color(0xFFFF9E2C)
        OccasionCelebrationStyle.SHIVRATRI_JYOTI -> Color(0xFF7986CB)
        OccasionCelebrationStyle.RAM_SURYA -> Color(0xFFFF9E2C)
    }
    val secondaryPetal = when (style) {
        OccasionCelebrationStyle.DIWALI_DEEPMALA -> Color(0xFFE26A8D)
        OccasionCelebrationStyle.NAVRATRI_SHAKTI -> Color(0xFFA82424)
        OccasionCelebrationStyle.JANMASHTAMI_MORPANKH -> Color(0xFF26A69A)
        OccasionCelebrationStyle.GANESH_MODAK -> Color(0xFFA82424)
        OccasionCelebrationStyle.SHIVRATRI_JYOTI -> Color(0xFFD4C4AB)
        OccasionCelebrationStyle.RAM_SURYA -> Color(0xFFD85816)
    }
    val kumkum = Color(0xFFA82424)
    val brassGold = Color(0xFFD8AF52)
    val pitambar = Color(0xFFFFC837)
    val ivory = Color(0xFFFFFDF8)

    translate(center.x, center.y) {
        scale(1f, rangoli.verticalSquash, pivot = Offset.Zero) {
            // Soft warm ambient floor illumination beneath the Rangoli
            if (rangoli.ambientGlow > 0.01f) {
                val glowR = baseRadius * 1.35f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            Color(0x55FFB32A).copy(alpha = 0.28f * rangoli.ambientGlow),
                            Color(0x22FF7A18).copy(alpha = 0.14f * rangoli.ambientGlow),
                            Color.Transparent
                        ),
                        center = Offset.Zero,
                        radius = glowR
                    ),
                    radius = glowR,
                    center = Offset.Zero,
                    blendMode = BlendMode.Screen
                )
            }

            // 1. Outer scalloped marigold & kumkum geometric border
            if (rangoli.outerBloom > 0.01f) {
                val rOuter = baseRadius * rangoli.outerBloom
                val alpha = rangoli.outerBloom
                drawCircle(
                    color = kumkum.copy(alpha = 0.62f * alpha),
                    radius = rOuter,
                    center = Offset.Zero
                )
                drawCircle(
                    color = brassGold.copy(alpha = 0.88f * alpha),
                    radius = rOuter,
                    center = Offset.Zero,
                    style = Stroke(width = baseRadius * 0.032f)
                )
                val scallopCount = 16
                for (i in 0 until scallopCount) {
                    val angle = (i * 2.0 * PI / scallopCount).toFloat()
                    val px = cos(angle) * rOuter * 0.92f
                    val py = sin(angle) * rOuter * 0.92f
                    drawCircle(
                        color = (if (i % 2 == 0) pitambar else primaryPetal).copy(alpha = 0.85f * alpha),
                        radius = baseRadius * 0.095f * rangoli.outerBloom,
                        center = Offset(px, py)
                    )
                    // Rice-flour Karpura Ivory bindu dots around the outer rim
                    val dotX = cos(angle) * rOuter * 1.07f
                    val dotY = sin(angle) * rOuter * 1.07f
                    drawCircle(
                        color = ivory.copy(alpha = 0.82f * alpha),
                        radius = baseRadius * 0.028f,
                        center = Offset(dotX, dotY)
                    )
                }
            }

            // 2. Middle 8-petaled Ashtadala Kamal ring
            if (rangoli.middleBloom > 0.01f) {
                val rMid = baseRadius * 0.74f * rangoli.middleBloom
                val alpha = rangoli.middleBloom
                drawCircle(
                    color = Color(0xFF5A1212).copy(alpha = 0.72f * alpha),
                    radius = rMid * 0.96f,
                    center = Offset.Zero
                )
                val petalCount = 8
                for (step in 0 until petalCount * 2) {
                    val isSecondary = step % 2 == 1
                    val angleDeg = step * (360f / (petalCount * 2))
                    val reach = if (isSecondary) rMid * 0.92f else rMid
                    val width = if (isSecondary) rMid * 0.28f else rMid * 0.34f
                    rotate(angleDeg, pivot = Offset.Zero) {
                        val petal = Path().apply {
                            moveTo(0f, 0f)
                            cubicTo(-width, reach * 0.35f, -width * 0.85f, reach * 0.78f, 0f, reach)
                            cubicTo(width * 0.85f, reach * 0.78f, width, reach * 0.35f, 0f, 0f)
                            close()
                        }
                        val fill = if (isSecondary) secondaryPetal else primaryPetal
                        drawPath(
                            path = petal,
                            brush = Brush.verticalGradient(
                                listOf(
                                    ivory.copy(alpha = 0.92f * alpha),
                                    pitambar.copy(alpha = 0.90f * alpha),
                                    fill.copy(alpha = 0.92f * alpha)
                                ),
                                startY = reach,
                                endY = 0f
                            )
                        )
                        drawPath(
                            path = petal,
                            color = brassGold.copy(alpha = 0.85f * alpha),
                            style = Stroke(width = baseRadius * 0.018f)
                        )
                    }
                }
            }

            // 3. Inner golden lotus core & auspicious Kumkum Bindu
            val rInner = baseRadius * 0.34f * rangoli.innerBloom
            val innerAlpha = rangoli.innerBloom
            drawCircle(
                color = kumkum.copy(alpha = 0.92f * innerAlpha),
                radius = rInner,
                center = Offset.Zero
            )
            drawCircle(
                color = brassGold.copy(alpha = 0.95f * innerAlpha),
                radius = rInner,
                center = Offset.Zero,
                style = Stroke(width = baseRadius * 0.026f)
            )
            for (i in 0 until 8) {
                val ang = (i * PI / 4.0).toFloat()
                drawCircle(
                    color = pitambar.copy(alpha = 0.92f * innerAlpha),
                    radius = rInner * 0.16f,
                    center = Offset(cos(ang) * rInner * 0.64f, sin(ang) * rInner * 0.64f)
                )
            }
            drawCircle(
                color = ivory.copy(alpha = 0.95f * innerAlpha),
                radius = rInner * 0.24f,
                center = Offset.Zero
            )
        }
    }
}

private fun DrawScope.drawLakshmiCharan(
    step: SacredCharanPose,
    style: OccasionCelebrationStyle,
    viewport: TempleViewport
) {
    val px = viewport.pixel(step.position)
    val unit = viewport.imageWidth * 0.028f * step.scale
    val alpha = step.imprintOpacity
    val kumkum = when (style) {
        OccasionCelebrationStyle.SHIVRATRI_JYOTI -> Color(0xFFD8AF52)
        else -> Color(0xFFA82424)
    }.copy(alpha = alpha)
    val roli = Color(0xFFD85816).copy(alpha = alpha)
    val gold = Color(0xFFFFE082).copy(alpha = alpha)
    val ivory = Color(0xFFFFFDF8).copy(alpha = alpha * 0.92f)

    // Expanding golden lotus ripple when the footprint first touches the floor
    if (step.rippleAlpha > 0.01f) {
        val rippleW = unit * (1.5f + 1.8f * step.rippleProgress)
        val rippleH = rippleW * 0.55f
        drawOval(
            color = Color(0xFFFFCA3A).copy(alpha = 0.55f * step.rippleAlpha),
            topLeft = Offset(px.x - rippleW * 0.5f, px.y - rippleH * 0.5f),
            size = Size(rippleW, rippleH),
            style = Stroke(width = unit * 0.09f),
            blendMode = BlendMode.Screen
        )
    }

    translate(px.x, px.y) {
        rotate(step.angleDegrees, pivot = Offset.Zero) {
            // Mirror horizontally so the big toe is always on the inner side of each foot
            scale(if (step.isLeftFoot) 1f else -1f, 1f, pivot = Offset.Zero) {
                // Soft golden aura beneath the sacred footprint
                drawOval(
                    color = Color(0x44FFCA3A).copy(alpha = 0.32f * alpha),
                    topLeft = Offset(-unit * 0.65f, -unit * 0.85f),
                    size = Size(unit * 1.3f, unit * 1.7f)
                )

                // Main upper sole & arch
                val sole = Path().apply {
                    moveTo(0f, -unit * 0.52f)
                    cubicTo(unit * 0.42f, -unit * 0.52f, unit * 0.44f, -unit * 0.05f, unit * 0.26f, unit * 0.14f)
                    cubicTo(unit * 0.12f, unit * 0.22f, -unit * 0.14f, unit * 0.20f, -unit * 0.28f, unit * 0.06f)
                    cubicTo(-unit * 0.42f, -unit * 0.12f, -unit * 0.38f, -unit * 0.52f, 0f, -unit * 0.52f)
                    close()
                }
                drawPath(sole, Brush.verticalGradient(listOf(roli, kumkum)))
                drawPath(sole, gold, style = Stroke(width = unit * 0.06f))

                // Rounded heel pad below the arch
                val heelCenter = Offset(-unit * 0.02f, unit * 0.42f)
                drawOval(
                    brush = Brush.radialGradient(listOf(roli, kumkum), center = heelCenter, radius = unit * 0.32f),
                    topLeft = Offset(heelCenter.x - unit * 0.26f, heelCenter.y - unit * 0.22f),
                    size = Size(unit * 0.52f, unit * 0.44f)
                )
                drawOval(
                    color = gold,
                    topLeft = Offset(heelCenter.x - unit * 0.26f, heelCenter.y - unit * 0.22f),
                    size = Size(unit * 0.52f, unit * 0.44f),
                    style = Stroke(width = unit * 0.055f)
                )

                // Five toes arranged from inner big toe (+x) to outer little toe (-x)
                val toes = listOf(
                    Triple(0.24f, -0.70f, 0.125f),
                    Triple(0.08f, -0.74f, 0.100f),
                    Triple(-0.06f, -0.72f, 0.090f),
                    Triple(-0.19f, -0.67f, 0.082f),
                    Triple(-0.30f, -0.59f, 0.072f)
                )
                toes.forEach { (tx, ty, tr) ->
                    val toeCenter = Offset(unit * tx, unit * ty)
                    drawCircle(kumkum, radius = unit * tr, center = toeCenter)
                    drawCircle(gold, radius = unit * tr, center = toeCenter, style = Stroke(width = unit * 0.04f))
                }

                // Sacred golden Bindu & Karpura center mark on the sole
                drawCircle(gold, radius = unit * 0.11f, center = Offset(0f, -unit * 0.18f))
                drawCircle(ivory, radius = unit * 0.055f, center = Offset(0f, -unit * 0.18f))
            }
        }
    }
}

internal fun DrawScope.drawDeepmalaDiya(
    diya: DeepmalaDiyaPose,
    phaseSeconds: Float,
    viewport: TempleViewport
) {
    val px = viewport.pixel(diya.position)
    val unit = viewport.imageWidth * (if (diya.tier == 0) 0.036f else 0.042f) * diya.scale
    val vesselAlpha = diya.vesselOpacity

    // Warm reflection on the temple floor / altar marble below the diya
    if (diya.floorReflection > 0.01f) {
        val reflW = unit * 2.2f
        val reflH = unit * 0.75f
        drawOval(
            brush = Brush.radialGradient(
                listOf(
                    Color(0x66FFB32A).copy(alpha = 0.34f * diya.floorReflection),
                    Color(0x22FF7A18).copy(alpha = 0.14f * diya.floorReflection),
                    Color.Transparent
                ),
                center = Offset(px.x, px.y + unit * 0.22f),
                radius = reflW * 0.6f
            ),
            topLeft = Offset(px.x - reflW * 0.5f, px.y - reflH * 0.15f),
            size = Size(reflW, reflH),
            blendMode = BlendMode.Screen
        )
    }

    // Contact shadow beneath clay bowl
    drawOval(
        color = Color(0xFF1E110A).copy(alpha = 0.42f * vesselAlpha),
        topLeft = Offset(px.x - unit * 0.52f, px.y - unit * 0.08f),
        size = Size(unit * 1.04f, unit * 0.28f)
    )

    // Handcrafted terracotta & brass clay diya bowl
    val bowl = Path().apply {
        moveTo(px.x - unit * 0.56f, px.y - unit * 0.18f)
        cubicTo(
            px.x - unit * 0.48f, px.y + unit * 0.16f,
            px.x + unit * 0.48f, px.y + unit * 0.16f,
            px.x + unit * 0.56f, px.y - unit * 0.18f
        )
        quadraticTo(px.x, px.y - unit * 0.06f, px.x - unit * 0.56f, px.y - unit * 0.18f)
        close()
    }
    drawPath(
        path = bowl,
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFFD97236).copy(alpha = vesselAlpha),
                Color(0xFF9C3D18).copy(alpha = vesselAlpha),
                Color(0xFF5E220C).copy(alpha = vesselAlpha)
            ),
            startY = px.y - unit * 0.20f,
            endY = px.y + unit * 0.16f
        )
    )
    // Golden brass rim and oil well
    drawOval(
        color = Color(0xFF7A2E12).copy(alpha = vesselAlpha),
        topLeft = Offset(px.x - unit * 0.52f, px.y - unit * 0.26f),
        size = Size(unit * 1.04f, unit * 0.20f)
    )
    drawOval(
        color = Color(0xFFE5C168).copy(alpha = 0.90f * vesselAlpha),
        topLeft = Offset(px.x - unit * 0.52f, px.y - unit * 0.26f),
        size = Size(unit * 1.04f, unit * 0.20f),
        style = Stroke(width = unit * 0.055f)
    )

    // 4-stop Garbhagriha Chiaroscuro Diya Flame with organic micro-flicker
    if (diya.flameIgnite > 0.01f) {
        val wickBase = Offset(px.x, px.y - unit * 0.22f)
        val t = phaseSeconds + diya.index * 0.63f
        val heightScale = 0.92f + 0.08f * (1f + 0.65f * sin(t * 10.5f) + 0.35f * sin(t * 21f))
        val flameH = unit * 0.92f * diya.flameIgnite * heightScale
        val flameW = unit * 0.24f * diya.flameIgnite
        val wobble = unit * 0.065f * (0.65f * sin(t * 7.3f) + 0.35f * sin(t * 16.7f))

        // 1. Ambient Halo: rgba(255, 158, 44, 0.27) with smooth falloff
        val haloCenter = wickBase - Offset(0f, flameH * 0.42f)
        val haloRadius = flameH * 1.55f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    Color(0x45FF9E2C).copy(alpha = 0.27f * diya.flameIgnite),
                    Color(0x18FF6D00).copy(alpha = 0.10f * diya.flameIgnite),
                    Color.Transparent
                ),
                center = haloCenter,
                radius = haloRadius
            ),
            radius = haloRadius,
            center = haloCenter,
            blendMode = BlendMode.Screen
        )

        fun flamePath(w: Float, h: Float, bend: Float): Path = Path().apply {
            moveTo(wickBase.x, wickBase.y)
            cubicTo(
                wickBase.x - w * 1.25f, wickBase.y - h * 0.15f,
                wickBase.x - w * 0.95f, wickBase.y - h * 0.55f,
                wickBase.x + bend, wickBase.y - h
            )
            cubicTo(
                wickBase.x + bend + w * 0.12f, wickBase.y - h * 0.64f,
                wickBase.x + w * 1.25f, wickBase.y - h * 0.18f,
                wickBase.x, wickBase.y
            )
            close()
        }

        // 2 & 3. Outer Mantle (#FF6D00 Flame Tangerine) -> Inner Body (#FFC107 Honey Amber)
        drawPath(
            path = flamePath(flameW, flameH, wobble),
            brush = Brush.verticalGradient(
                listOf(
                    Color(0xFFFF6D00).copy(alpha = 0.92f * diya.flameIgnite),
                    Color(0xFFFFC107).copy(alpha = 0.98f * diya.flameIgnite),
                    Color(0xFFFF9E2C).copy(alpha = 0.90f * diya.flameIgnite)
                ),
                startY = wickBase.y - flameH,
                endY = wickBase.y
            )
        )

        // 4. Core (#FFFDF8 Karpura White)
        drawPath(
            path = flamePath(flameW * 0.56f, flameH * 0.74f, wobble * 0.6f),
            brush = Brush.verticalGradient(
                listOf(
                    Color(0xFFFFC107).copy(alpha = 0.90f * diya.flameIgnite),
                    Color(0xFFFFFDF8).copy(alpha = diya.flameIgnite)
                ),
                startY = wickBase.y - flameH * 0.74f,
                endY = wickBase.y
            )
        )

        // Ignition sparkler burst when the wick first lights up
        if (diya.sparkBurst > 0.02f) {
            val sparkOrigin = wickBase - Offset(0f, flameH * 0.55f)
            val sparkCount = 8
            for (s in 0 until sparkCount) {
                val ang = (s * 2.0 * PI / sparkCount + diya.index * 0.4).toFloat()
                val dist = unit * (0.35f + 0.85f * (1f - diya.sparkBurst * 0.3f))
                val sx = sparkOrigin.x + cos(ang) * dist
                val sy = sparkOrigin.y + sin(ang) * dist
                drawCircle(
                    color = Color(0xFFFFFDF8).copy(alpha = diya.sparkBurst),
                    radius = unit * 0.065f * diya.sparkBurst,
                    center = Offset(sx, sy),
                    blendMode = BlendMode.Screen
                )
            }
        }
    }
}
