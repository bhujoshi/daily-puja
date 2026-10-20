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
import kotlin.math.sin

/** Screen-space flames stay attached to the moving wick, with independent, smooth flicker. */
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
    incenseEmbers: List<TemplePoint> = emptyList()
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
        fun flame(point: TemplePoint, phase: Float, scale: Float = 1f, flicker: Float = 1f) {
            val pixel=viewport.pixel(point)
            val base=Offset(pixel.x,pixel.y)
            val t=seconds+phase
            val height=unit*.047f*scale*(1f+flicker*(.06f*sin(t*11f)+.025f*sin(t*23f)))
            val width=unit*.008f*scale
            val bend=width*flicker*(.32f*sin(t*7f)+.15f*sin(t*17f))
            val glowCenter=base-Offset(0f,height*.35f)
            drawCircle(Brush.radialGradient(listOf(Color(0x50FFB23E),Color(0x18FF7C18),Color.Transparent),glowCenter,height*1.25f),height*1.25f,glowCenter)
            fun tongue(w: Float,h: Float,b: Float): Path = Path().apply {
                moveTo(base.x,base.y)
                cubicTo(base.x-w*1.3f,base.y-h*.12f,base.x-w,base.y-h*.49f,base.x+b,base.y-h)
                cubicTo(base.x+b+w*.10f,base.y-h*.66f,base.x+w*1.7f,base.y-h*.20f,base.x,base.y)
                close()
            }
            drawPath(tongue(width,height,bend),Brush.verticalGradient(listOf(Color(0x00FF7B16),Color(0xDDFFB32A),Color(0xFFFFDB62),Color(0xB8FF8A12)),base.y-height,base.y))
            drawPath(tongue(width*.58f,height*.79f,bend*.65f),Brush.verticalGradient(listOf(Color(0x66FFF6C1),Color(0xFFFFF4CC),Color(0xFFFFFCEB)),base.y-height*.79f,base.y))
            drawOval(Color(0xAA689AF0),base-Offset(width*.35f,height*.10f),androidx.compose.ui.geometry.Size(width*.7f,height*.12f))
            drawLine(Color(0xFF482819),base+Offset(0f,unit*.004f),base-Offset(0f,unit*.004f),unit*.0025f,StrokeCap.Round)
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
            val aartiScale = if(aartiLit) TempleSceneLayout.AARTI_SCALE else TempleSceneLayout.AARTI_SCALE * .85f
            if(aartiWicks.isNotEmpty()) {
                aartiWicks.forEachIndexed { index, wickPoint ->
                    val wickScale = if (aartiWicks.size == 1) aartiScale else .58f * aartiScale
                    flame(wickPoint, 1.71f + index * .7f, scale = wickScale, flicker = .25f)
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
                        scale=.58f*aartiScale,
                        flicker=.25f
                    )
                }
            } else flame(TempleSceneLayout.aartiWick(lamp),1.71f,scale=aartiScale,flicker=.25f)
        }
    }
}
