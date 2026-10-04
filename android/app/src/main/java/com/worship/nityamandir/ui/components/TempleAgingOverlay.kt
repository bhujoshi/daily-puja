package com.worship.nityamandir.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import com.worship.nityamandir.engine.*
import kotlin.math.*

/** Dust follows each shrine's recess and ledge; swipes reveal the surface locally. */
@Composable
fun ShrineAgingOverlay(viewport:TempleViewport,dust:Float,webs:Float,space:ShrineSpace,idol:IdolPlacement,cleaned:List<TemplePoint>) {
    Canvas(Modifier.fillMaxSize().zIndex(1f)) {
        val u=viewport.imageWidth
        fun at(x:Float,y:Float)=viewport.pixel(TemplePoint(x,y)).let {Offset(it.x,it.y)}
        val wiped=Path().apply {cleaned.forEach {p -> val c=at(p.x,p.y);addOval(androidx.compose.ui.geometry.Rect(c.x-u*.06f,c.y-u*.06f,c.x+u*.06f,c.y+u*.06f))}}
        clipPath(wiped,ClipOp.Difference) {
            val span=space.right-space.left;val height=space.altarY-space.archTop
            fun dustPatch(point:TemplePoint,width:Float,height:Float,amount:Float=1f) {
                if(dust<=0f) return
                val center=at(point.x,point.y)
                val tint=Color(0xFF796A54)
                drawOval(Brush.radialGradient(listOf(tint.copy(alpha=dust*.27f*amount),Color.Transparent),center,u*width*.55f),
                    center-Offset(u*width/2,u*height/2),Size(u*width,u*height))
                repeat(48) {i ->
                    val x=point.x+((i*37%97)/96f-.5f)*width
                    val y=point.y+((i*53%89)/88f-.5f)*height
                    if((i*17%47)/47f<dust) drawCircle(tint.copy(alpha=dust*.38f*amount),u*.0012f,at(x,y))
                }
            }
            fun web(point:TemplePoint,direction:Float,radius:Float) {
                if(webs<=0f) return
                val origin=at(point.x,point.y)
                val r=u*radius*(.35f+.65f*webs)
                val silk=Color(0xFFA79B86).copy(alpha=(webs*.65f).coerceIn(0f,1f))
                fun ray(n:Int,extent:Float)=origin+Offset(direction*cos(n/6f*PI.toFloat()/2)*extent,sin(n/6f*PI.toFloat()/2)*extent)
                repeat(7) {drawLine(silk,origin,ray(it,r),u*.00065f)}
                repeat(5) {ring -> val extent=r*(ring+1)/5;val path=Path().apply {
                    val first=ray(0,extent);moveTo(first.x,first.y)
                    repeat(6) {i -> val a=ray(i,extent);val b=ray(i+1,extent);val mid=origin+((a+b)*.5f-origin)*.92f;quadraticBezierTo(mid.x,mid.y,b.x,b.y)}
                };drawPath(path,silk,style=Stroke(u*.0006f))}
            }
            val interior=Path().apply {
                val l=at(space.left,space.archTop);val r=at(space.right,space.altarY+.035f)
                addRect(androidx.compose.ui.geometry.Rect(l,r))
            }
            clipPath(interior) {
                repeat(100) {i ->
                    val x=space.left+(i*37%101)/101f*span
                    val y=space.archTop+(i*53%103)/103f*height
                    val center=at(x,y);val radius=u*(.012f+(i%6)*.006f)
                    drawCircle(Brush.radialGradient(listOf(Color(0xFF81735E).copy(alpha=dust*(.12f+(i%4)*.03f)),Color.Transparent),center,radius),radius,center)
                }
                repeat(1700) {i ->
                    if((i%101)/101f<dust) {
                        val x=space.left+(i*73%1699)/1699f*span
                        val y=space.archTop+(i*137%1693)/1693f*height
                        drawOval(Color(0xFF796A54).copy(alpha=dust*(.20f+(i%5)*.04f)),at(x,y),Size(u*.0011f,u*.0008f))
                    }
                }
                // A narrow dust deposit rests on the horizontal shelf.
                drawRect(Brush.verticalGradient(listOf(Color.Transparent,Color(0xFFB3A183).copy(alpha=dust*.6f),Color.Transparent),u*(space.altarY-.003f),u*(space.altarY+.025f)),at(space.left,space.altarY-.003f),Size(u*span,u*.028f))
            }
            // Separate deposits follow the idols and every object on the lower altar.
            dustPatch(TemplePoint(.5f,idol.top+idol.height*.48f),idol.width*.88f,idol.height*.86f,.65f)
            dustPatch(TemplePoint(.5f,space.ledgeY),span,.035f)
            listOf(TemplePoint(.24f,space.floorY),TemplePoint(.5f,space.floorY),TemplePoint(.76f,space.floorY))
                .forEach {dustPatch(it,.22f,.075f,.65f)}
            dustPatch(space.lamp,.11f,.15f)
            dustPatch(TempleSceneLayout.plate,.39f,.16f)
            dustPatch(TempleSceneLayout.conch,.16f,.11f)
            dustPatch(TempleSceneLayout.bell,.09f,.15f)
            dustPatch(TempleSceneLayout.aartiRest,.16f,.13f)
            dustPatch(TempleSceneLayout.prasadRest,.15f,.13f)
            web(TemplePoint(space.left,space.archTop+.025f),1f,.095f)
            web(TemplePoint(space.right,space.archTop+.025f),-1f,.095f)
            web(TemplePoint(space.left,space.altarY-.055f),1f,.075f)
            web(TemplePoint(space.right,space.altarY-.055f),-1f,.075f)
            web(TemplePoint(.14f,space.floorY-.02f),1f,.09f)
            web(TemplePoint(.86f,space.floorY-.02f),-1f,.09f)
            web(TemplePoint(.25f,1.02f),1f,.065f)
            web(TemplePoint(.77f,1.02f),-1f,.065f)
        }
        cleaned.lastOrNull()?.let {p ->
            val c=at(p.x,p.y)
            drawCircle(Color(0xFFFFE1A2).copy(alpha=.12f),u*.043f,c)
        }
    }
}

/** Only flowers already placed at the deities' feet can wither. */
@Composable
fun OfferedFlowerAgingOverlay(viewport: TempleViewport, wilt: Float, offerings: List<FlowerOffering>) {
    if(wilt<=0f || offerings.isEmpty()) return
    Canvas(Modifier.fillMaxSize()) {
        fun at(x:Float,y:Float)=viewport.pixel(TemplePoint(x,y)).let {Offset(it.x,it.y)}
        // Brown curled petals sit directly on each flower, rather than tinting the whole plate.
        val points=(0..1).flatMap { deity -> offerings.filter {it.deity==deity}.takeLast(TempleSceneLayout.OFFERED_FLOWER_SLOTS) }
        points.forEachIndexed { i,offering ->
            val p=offering.position
            val unit=viewport.imageWidth*TempleSceneLayout.offeredFlowerSize(offering.flowerIndex)/.13f
            val center=at(p.x,p.y)
            repeat(7) { petal ->
                val angle=petal*PI.toFloat()*2/7+i
                val radius=unit*.016f
                val point=center+Offset(cos(angle)*radius,sin(angle)*radius*.45f)
                drawOval(Color(if(petal%2==0) 0xFF73502F else 0xFF9A783E).copy(alpha=wilt*.8f),point,Size(unit*.015f,unit*(.005f+.004f*(1-wilt))))
            }
            if(wilt>.45f) repeat(3) { n ->
                drawOval(Color(0xFF785330).copy(alpha=(wilt-.45f)*.8f),center+Offset((n-1)*unit*.025f,unit*(.026f+(i%3)*.004f)),Size(unit*.012f,unit*.004f))
            }
        }
    }
}
