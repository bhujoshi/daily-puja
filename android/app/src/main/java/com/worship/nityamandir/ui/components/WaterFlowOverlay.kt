package com.worship.nityamandir.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import com.worship.nityamandir.engine.TempleViewport
import com.worship.nityamandir.engine.TemplePoint
import kotlin.math.*

/** A continuous transparent stream, surface run-off, droplets and expanding impact ripples. */
@Composable
fun WaterFlowOverlay(deity: Int?, progress: Float, modifier: Modifier = Modifier) {
    if(deity==null) return
    Canvas(modifier.fillMaxSize().testTag("bath-animation")) {
        val viewport = TempleViewport(size.width,size.height)
        val unit = viewport.imageWidth
        val x = if(deity==0) .405f else .614f
        val headY = if(deity==0) .483f else .449f
        fun at(px: Float, py: Float): Offset = viewport.pixel(TemplePoint(px,py)).let { Offset(it.x,it.y) }
        val opacity = min(progress*9f,(1-progress)*9f).coerceIn(0f,1f)
        val source = at(x-.040f,headY-.105f)
        val impact = at(x,headY)
        val stream = Path().apply {
            moveTo(source.x,source.y)
            cubicTo(source.x+unit*.022f,source.y+unit*.015f,impact.x-unit*.009f,impact.y-unit*.040f,impact.x,impact.y)
        }
        drawPath(stream,Color(0xFF76B5C7).copy(alpha=.30f*opacity),style=Stroke(unit*.012f,cap=StrokeCap.Round))
        drawPath(stream,Brush.horizontalGradient(listOf(Color(0x88DAFAFF),Color(0xCCFFFFFF),Color(0x5576B5C7)),impact.x-unit*.008f,impact.x+unit*.008f),alpha=opacity,style=Stroke(unit*.006f,cap=StrokeCap.Round))
        // Longitudinal glints and moving droplets retain the sense of gravity rather than a static blue line.
        repeat(28) { i ->
            val t = ((progress*4.3f+i*.137f)%1f)
            val px = source.x+(impact.x-source.x)*t + sin(i*1.7f+progress*28)*unit*.0015f
            val py = source.y+(impact.y-source.y)*t*t
            drawLine(Color.White.copy(alpha=.75f*opacity),Offset(px,py),Offset(px+unit*.001f,py+unit*.009f),unit*.0018f,StrokeCap.Round)
        }
        repeat(7) { i ->
            val side = (i-3)/3f
            val runoff = Path().apply {
                moveTo(impact.x,impact.y)
                cubicTo(impact.x+side*unit*.026f,impact.y+unit*.034f,impact.x+side*unit*.016f,unit*.650f,impact.x+side*unit*.038f,unit*.688f)
            }
            drawPath(runoff,Color(0xFFE1FAFF).copy(alpha=(.12f+.04f*(i%3))*opacity),style=Stroke(unit*(.002f+(i%2)*.0015f),cap=StrokeCap.Round))
        }
        repeat(32) { i ->
            val t = ((progress*3.4f+i*.071f)%1f)
            val side = sin(i*14.31f)
            val start = at(x,.689f)
            val point = Offset(start.x+side*unit*.060f*t,start.y-unit*.040f*sin(PI.toFloat()*t)+unit*.016f*t*t)
            drawCircle(Color(0xFFE8FCFF).copy(alpha=opacity*(1-t)*.8f),unit*(.0009f+(i%3)*.00045f),point)
        }
        repeat(4) { i ->
            val t = (progress*2.6f+i*.25f)%1f
            val radius = unit*(.012f+.044f*t)
            drawOval(Color(0xFFBCE9EF).copy(alpha=opacity*(1-t)*.38f),at(x,.694f)-Offset(radius,radius*.2f),Size(radius*2,radius*.4f),style=Stroke(unit*.0012f))
        }
    }
}
