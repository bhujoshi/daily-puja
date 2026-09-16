package com.worship.nityamandir.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import com.worship.nityamandir.engine.*
import kotlin.math.sin

/** Screen-space flames stay attached to the moving wick, with independent, smooth flicker. */
@Composable
fun RitualFlamesOverlay(oilLit: Boolean, aartiLit: Boolean, lamp: TemplePoint) {
    if(!oilLit && !aartiLit) return
    var seconds by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start=withFrameNanos { it }
        while(true) withFrameNanos { seconds=(it-start)/1_000_000_000f }
    }
    Canvas(Modifier.fillMaxSize()) {
        val viewport=TempleViewport(size.width,size.height)
        fun flame(point: TemplePoint, phase: Float) {
            val pixel=viewport.pixel(point)
            val base=Offset(pixel.x,pixel.y)
            val unit=viewport.imageWidth
            val t=seconds+phase
            val height=unit*.047f*(1f+.06f*sin(t*11f)+.025f*sin(t*23f))
            val width=unit*.008f
            val bend=width*(.32f*sin(t*7f)+.15f*sin(t*17f))
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
        if(oilLit) flame(TemplePoint(.505f,TempleSceneLayout.oil.y-.06f*1.6f),0f)
        if(aartiLit) flame(TemplePoint(lamp.x,lamp.y-.088f*1.8f),1.71f)
    }
}
