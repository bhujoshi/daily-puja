package com.worship.nityamandir.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import com.worship.nityamandir.engine.*
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin

/** Screen-space & 3D depth-scaled flames stay attached to the moving wick, with 4-stop sacred diya gradients, camphor smoke plumes, and rising golden embers. */
@Composable
fun RitualFlamesOverlay(
    oilLit: Boolean,
    aartiLit: Boolean,
    lamp: TemplePoint,
    oilPoint: TemplePoint = TempleSceneLayout.oil,
    traditional: Boolean = false,
    brass: Boolean = false,
    oilWick: TemplePoint? = null,
    oilWicks: List<TemplePoint> = emptyList(),
    aartiWicks: List<TemplePoint> = emptyList(),
    incenseEmbers: List<TemplePoint> = emptyList(),
    aartiRunning: Boolean = false,
    aartiDepthScale: Float = 1f,
    aartiBendX: Float = 0f,
    karpuraMode: Boolean = false
) {
    if(!oilLit && !aartiLit) return
    var seconds by remember { mutableFloatStateOf(0f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val startMs = android.os.SystemClock.uptimeMillis() - (seconds * 1000f).toLong()
            while (isActive) {
                // Decorative flame flicker runs smoothly at 30 FPS while allowing CPU to sleep between ticks.
                kotlinx.coroutines.delay(33)
                seconds = (android.os.SystemClock.uptimeMillis() - startMs) / 1000f
            }
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val viewport=TempleViewport(size.width,size.height)
        val unit=viewport.imageWidth
        fun flame(
            point: TemplePoint,
            phase: Float,
            scale: Float = 1f,
            flicker: Float = 1f,
            motionBendFactor: Float = 0f,
            emitKarpuraSmoke: Boolean = false
        ) {
            val pixel=viewport.pixel(point)
            val base=Offset(pixel.x,pixel.y)
            val t=seconds+phase
            val height=unit*.048f*scale*(1f+flicker*(.065f*sin(t*11f)+.028f*sin(t*23f)))
            val width=unit*.0084f*scale
            val bend=width*flicker*(.32f*sin(t*7f)+.15f*sin(t*17f)) + unit * motionBendFactor * scale
            val glowCenter=base-Offset(bend*.25f,height*.38f)

            // 4-Stop Sacred Diya Ambient Halo: rgba(255, 158, 44, 0.27) -> Transparent
            drawCircle(
                Brush.radialGradient(
                    listOf(
                        Color(0x45FF9E2C),
                        Color(0x1DFF6D00),
                        Color.Transparent
                    ),
                    glowCenter,
                    height*1.38f
                ),
                height*1.38f,
                glowCenter,
                blendMode = BlendMode.Screen
            )

            // Subtle camphor (Karpura) wisp & rising golden spark above active 3D Aarti flames
            if (emitKarpuraSmoke) {
                val smokeH = height * (if (karpuraMode) 1.65f else 1.25f)
                val tipX = base.x + bend
                val tipY = base.y - height
                val wispSway1 = width * 1.6f * sin(t * 2.4f) + bend * 0.45f
                val wispSway2 = width * 2.3f * cos(t * 1.9f + phase) + bend * 0.85f
                val smokePath = Path().apply {
                    moveTo(tipX, tipY)
                    cubicTo(
                        tipX + wispSway1, tipY - smokeH * 0.36f,
                        tipX - wispSway2, tipY - smokeH * 0.72f,
                        tipX + wispSway1 * 0.7f, tipY - smokeH
                    )
                }
                drawPath(
                    path = smokePath,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0x24FFFDF8),
                            Color(0x38E8DFD5),
                            Color(0x14FF9E2C)
                        ),
                        startY = tipY - smokeH,
                        endY = tipY
                    ),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = width * (if (karpuraMode) 0.85f else 0.58f),
                        cap = StrokeCap.Round
                    )
                )
                // Rising micro-ember from camphor flame tip
                val sparkCycle = (t * 1.35f + phase * 0.7f) % 1f
                val sparkPos = Offset(
                    tipX + sin(t * 5f + phase) * width * 0.9f + bend * sparkCycle * 0.5f,
                    tipY - smokeH * 0.75f * sparkCycle
                )
                drawCircle(
                    color = Color(0xFFFFC107).copy(alpha = (1f - sparkCycle) * 0.65f),
                    radius = width * 0.24f * (1f - sparkCycle * 0.5f),
                    center = sparkPos,
                    blendMode = BlendMode.Screen
                )
            }

            fun tongue(w: Float,h: Float,b: Float): Path = Path().apply {
                moveTo(base.x,base.y)
                cubicTo(base.x-w*1.32f,base.y-h*.12f,base.x-w,base.y-h*.49f,base.x+b,base.y-h)
                cubicTo(base.x+b+w*.10f,base.y-h*.66f,base.x+w*1.72f,base.y-h*.20f,base.x,base.y)
                close()
            }
            // Outer Mantle (#FF6D00 Deep Saffron) -> Inner Body (#FFC107 Pure Marigold)
            drawPath(
                tongue(width,height,bend),
                Brush.verticalGradient(
                    listOf(
                        Color(0x00FF6D00),
                        Color(0xE6FF6D00),
                        Color(0xFFFFC107),
                        Color(0xC8FF9E2C)
                    ),
                    base.y-height,
                    base.y
                )
            )
            // Flame Core (#FFFDF8 Karpura Ivory -> #FFF4CC Warm Ivory)
            drawPath(
                tongue(width*.58f,height*.79f,bend*.65f),
                Brush.verticalGradient(
                    listOf(
                        Color(0x66FFF4CC),
                        Color(0xFFFFF8E1),
                        Color(0xFFFFFDF8)
                    ),
                    base.y-height*.79f,
                    base.y
                )
            )
            drawOval(Color(0xAA689AF0),base-Offset(width*.35f,height*.10f),androidx.compose.ui.geometry.Size(width*.7f,height*.12f))
            drawLine(Color(0xFF482819),base+Offset(0f,unit*.004f),base-Offset(0f,unit*.004f),unit*.0025f*scale.coerceIn(0.8f, 1.3f),StrokeCap.Round)
        }
        fun incenseSmoke(point: TemplePoint, index: Int, isCone: Boolean) {
            val pixel = viewport.pixel(point)
            val base = Offset(pixel.x, pixel.y)
            val t = seconds * 1.55f + index * 1.13f
            val pulse = .82f + .18f * sin(seconds * 4.2f + index * .9f)
            val emberRadius = unit * (if (isCone) .011f else .0065f) * pulse
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xCCFFD54F), Color(0x88FF5722), Color.Transparent),
                    base,
                    emberRadius * 2.1f
                ),
                radius = emberRadius * 2.1f,
                center = base
            )
            val plumeHeight = unit * (if (isCone) .145f else .115f)
            val sway1 = unit * .014f * sin(t)
            val sway2 = unit * .022f * sin(t * .85f - 1.2f)
            val topDrift = unit * .018f * sin(t * .65f + .7f)
            val wisp = Path().apply {
                moveTo(base.x, base.y - unit * .003f)
                cubicTo(
                    base.x + sway1, base.y - plumeHeight * .33f,
                    base.x - sway2, base.y - plumeHeight * .68f,
                    base.x + topDrift, base.y - plumeHeight
                )
            }
            drawPath(
                path = wisp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color(0x26FFF8E7),
                        Color(0x44E8DFD5),
                        Color(0x18D7CCC8)
                    ),
                    startY = base.y - plumeHeight,
                    endY = base.y
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = unit * (if (isCone) .011f else .0065f),
                    cap = StrokeCap.Round
                )
            )
        }
        if(oilLit) {
            if(oilWicks.size > 1) {
                oilWicks.forEachIndexed { index, wickPoint ->
                    val spoutScale = if(index == 1 || index == 2) .82f else .92f
                    flame(wickPoint, index * .67f, scale = spoutScale, flicker = .55f)
                }
            } else {
                val singleWick = oilWicks.firstOrNull() ?: oilWick ?: TemplePoint(oilPoint.x, oilPoint.y - (if(brass) .125f else .11f))
                flame(singleWick, 0f, scale = 1.08f)
            }
            incenseEmbers.forEachIndexed { index, emberPoint ->
                val isCone = incenseEmbers.size == 1 || (incenseEmbers.size > 1 && index == incenseEmbers.lastIndex && incenseEmbers.size != 5)
                incenseSmoke(emberPoint, index, isCone = isCone)
            }
        }
        if(aartiLit || oilLit) {
            val baseAartiScale = (if(aartiLit) TempleSceneLayout.AARTI_SCALE else TempleSceneLayout.AARTI_SCALE * .85f) * aartiDepthScale
            if(aartiWicks.isNotEmpty()) {
                aartiWicks.forEachIndexed { index, wickPoint ->
                    // In 3D Panch-Aarti perspective, front/outer wicks appear slightly closer than the upper-center wick
                    val perspectiveWickFactor = when {
                        aartiWicks.size == 1 -> 1f
                        index == 0 || index == 4 -> 0.66f
                        index == 2 -> 0.56f
                        else -> 0.60f
                    }
                    val wickScale = perspectiveWickFactor * baseAartiScale
                    flame(
                        point = wickPoint,
                        phase = 1.71f + index * .7f,
                        scale = wickScale,
                        flicker = if (aartiRunning) .42f else .25f,
                        motionBendFactor = aartiBendX,
                        emitKarpuraSmoke = aartiRunning || karpuraMode
                    )
                }
            } else if(traditional) {
                listOf(
                    TemplePoint(-.037f, -.056f),
                    TemplePoint(-.018f, -.038f),
                    TemplePoint(0f, -.074f),
                    TemplePoint(.018f, -.038f),
                    TemplePoint(.037f, -.056f)
                ).forEachIndexed {index,offset ->
                    flame(
                        TemplePoint(
                            lamp.x+offset.x*TempleSceneLayout.AARTI_SCALE,
                            lamp.y+offset.y*TempleSceneLayout.AARTI_SCALE
                        ),
                        1.71f+index*.7f,
                        scale=.58f*baseAartiScale,
                        flicker=if (aartiRunning) .42f else .25f,
                        motionBendFactor = aartiBendX,
                        emitKarpuraSmoke = aartiRunning || karpuraMode
                    )
                }
            } else {
                flame(
                    TempleSceneLayout.aartiWick(lamp),
                    1.71f,
                    scale = baseAartiScale,
                    flicker = if (aartiRunning) .42f else .25f,
                    motionBendFactor = aartiBendX,
                    emitKarpuraSmoke = aartiRunning || karpuraMode
                )
            }
        }
    }
}
