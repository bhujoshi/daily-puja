package com.worship.nityamandir.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import com.worship.nityamandir.engine.*
import kotlin.math.*

/** Crown-targeted pour, followed by runoff and a gentle settling ripple. */
@Composable
fun WaterFlowOverlay(deity:Int?,progress:Float,placement:IdolPlacement,modifier:Modifier=Modifier) {
    if(deity==null) return
    Canvas(modifier.fillMaxSize()) {
        val viewport=TempleViewport(size.width,size.height);val u=viewport.imageWidth
        fun at(p:TemplePoint)=viewport.pixel(p).let {Offset(it.x,it.y)}
        val crown=placement.crowns[deity];val source=at(placement.bathSource(deity));val impact=at(crown)
        val pour=(min((progress-.12f)/.14f,(.86f-progress)/.15f)).coerceIn(0f,1f)
        val settle=min(progress/.12f,(1-progress)/.15f).coerceIn(0f,1f)
        val stream=Path().apply {moveTo(source.x,source.y);cubicTo(source.x+u*.023f,source.y+u*.008f,impact.x,impact.y-u*.04f,impact.x,impact.y)}
        drawPath(stream,Color(0xFF98D4DF).copy(alpha=.40f*pour),style=Stroke(u*.008f,cap=StrokeCap.Round))
        drawPath(stream,Color.White.copy(alpha=.80f*pour),style=Stroke(u*.0022f,cap=StrokeCap.Round))
        repeat(18) {i ->
            val t=(progress*3.5f+i/18f)%1f
            // Cubic Bezier samples keep moving droplets attached to the curved stream.
            val q=1-t
            val x=q*q*q*source.x+3*q*q*t*(source.x+u*.023f)+3*q*t*t*impact.x+t*t*t*impact.x
            val y=q*q*q*source.y+3*q*q*t*(source.y+u*.008f)+3*q*t*t*(impact.y-u*.04f)+t*t*t*impact.y
            drawCircle(Color.White.copy(alpha=pour*.8f),u*.0014f,Offset(x,y))
        }
        val feet=at(TemplePoint(crown.x,placement.bottom))
        repeat(5) {i ->
            val side=(i-2)/2f
            val end=feet+Offset(side*u*.025f,0f)
            val path=Path().apply {moveTo(impact.x,impact.y);cubicTo(impact.x+side*u*.026f,impact.y+u*.055f,end.x,end.y-u*.04f,end.x,end.y)}
            drawPath(path,Color(0xFFDEF7FA).copy(alpha=.16f*pour),style=Stroke(u*.0016f,cap=StrokeCap.Round))
        }
        repeat(16) {i ->
            val t=(progress*2.7f+i*.061f)%1f;val side=sin(i*7.3f)
            drawCircle(Color(0xFFE3FAFF).copy(alpha=pour*(1-t)*.7f),u*.0011f,feet+Offset(side*u*.04f*t,-u*.025f*sin(PI.toFloat()*t)+u*.012f*t))
        }
        repeat(3) {i ->
            val t=(progress*2f+i/3f)%1f;val r=u*(.008f+.035f*t)
            drawOval(Color(0xFFBCE9EF).copy(alpha=settle*(1-t)*.35f),feet-Offset(r,r*.18f),Size(r*2,r*.36f),style=Stroke(u*.001f))
        }
    }
}
