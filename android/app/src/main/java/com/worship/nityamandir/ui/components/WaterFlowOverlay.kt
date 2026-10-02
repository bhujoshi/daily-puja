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

/** Continuous clear pour, crown splash, runoff and a few foreground water beads. */
@Composable
fun WaterFlowOverlay(deity:Int?,progress:Float,placement:IdolPlacement,modifier:Modifier=Modifier) {
    if(deity==null) return
    Canvas(modifier.fillMaxSize()) {
        val viewport=TempleViewport(size.width,size.height);val u=viewport.imageWidth
        fun at(p:TemplePoint)=viewport.pixel(p).let {Offset(it.x,it.y)}
        val crown=placement.crowns[deity];val source=at(placement.bathSource(deity));val impact=at(crown)
        val pour=(min((progress-.12f)/.14f,(.86f-progress)/.15f)).coerceIn(0f,1f)
        val settle=min(progress/.12f,(1-progress)/.15f).coerceIn(0f,1f)
        // Connected edges narrow under gravity, with travelling ripples in the highlights.
        fun streamPoint(t:Float,side:Float):Offset {
            val radius=u*(.0045f-.0017f*t)*(1f+.13f*sin(t*24f-progress*48f))*(.65f+.35f*pour)
            val center=source.x+(impact.x-source.x)*t+u*.0007f*sin(t*17f-progress*21f)*sin(PI.toFloat()*t)
            return Offset(center+side*radius,source.y+(impact.y-source.y)*t)
        }
        val stream=Path().apply {
            val first=streamPoint(0f,-1f);moveTo(first.x,first.y)
            for(i in 1..36) {val p=streamPoint(i/36f,-1f);lineTo(p.x,p.y)}
            for(i in 36 downTo 0) {val p=streamPoint(i/36f,1f);lineTo(p.x,p.y)}
            close()
        }
        drawPath(stream,Brush.horizontalGradient(listOf(
            Color(0xFFEAF4EB).copy(alpha=.66f*pour),
            Color(0xFFB4D5D4).copy(alpha=.18f*pour),
            Color(0xFFFFFFEF).copy(alpha=.78f*pour)
        ),source.x-u*.005f,source.x+u*.005f))
        for(side in listOf(-.78f,.72f)) {
            val edge=Path().apply {
                for(i in 0..36) {
                    val p=streamPoint(i/36f,side)
                    if(i==0) moveTo(p.x,p.y) else lineTo(p.x,p.y)
                }
            }
            drawPath(edge,Color.White.copy(alpha=.58f*pour),style=Stroke(u*.0009f,cap=StrokeCap.Round))
        }
        repeat(9) {i ->
            val t=(progress*3.5f+i/9f)%1f
            val p=streamPoint(t,0f)
            drawOval(Color.White.copy(alpha=pour*.32f),p-Offset(u*.0027f,u*.0007f),Size(u*.0054f,u*.0014f))
        }
        repeat(12) {i ->
            val t=(progress*4f+i/12f)%1f
            val side=if(i%2==0) -1f else 1f
            val spread=.012f+(i%5)*.004f
            val p=impact+Offset(side*u*spread*t,u*(-.030f*t+.041f*t*t))
            drawOval(Color(0xFFEDF7F5).copy(alpha=pour*(1-t)*.62f),p,Size(u*.002f,u*.0034f))
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
