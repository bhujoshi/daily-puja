package com.worship.nityamandir.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import com.worship.nityamandir.engine.*
import kotlin.math.*

/** Dirt stays inside the shrine, over its backdrop, idols and wooden surfaces. */
@Composable
fun ShrineAgingOverlay(viewport: TempleViewport, dust: Float, webs: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val unit=viewport.imageWidth
        fun at(x:Float,y:Float)=viewport.pixel(TemplePoint(x,y)).let {Offset(it.x,it.y)}
        fun polygon(vararg points: Pair<Float,Float>)=Path().apply {
            points.forEachIndexed { i,(x,y) -> val p=at(x,y);if(i==0) moveTo(p.x,p.y) else lineTo(p.x,p.y) }
            close()
        }
        // The inset backing and photographed idols share this interior silhouette.
        // Keep the surrounding room, carpet and fresh offering plate untouched.
        val interior=polygon(
            .302f to .285f,.36f to .285f,.40f to .254f,.46f to .244f,
            .50f to .252f,.54f to .244f,.60f to .254f,.65f to .285f,
            .70f to .305f,.714f to .352f,.714f to .697f,.294f to .697f,
            .294f to .352f
        )
        if(dust>0f) clipPath(interior) {
            drawRect(Color(0xFF82745F).copy(alpha=dust*.23f))
            // Uneven deposits dull the white backing and idol surfaces as the days pass.
            repeat(95) { i ->
                val x=.294f+(i*37%97)/97f*.42f
                val y=.25f+(i*53%101)/101f*.45f
                val center=at(x,y)
                val radius=unit*(.012f+(i%6)*.005f)
                drawCircle(Brush.radialGradient(listOf(
                    Color(0xFF695B48).copy(alpha=dust*(.10f+(i%4)*.025f)),
                    Color.Transparent
                ),center,radius),radius,center)
            }
            repeat(2400) { i ->
                if((i%101)/101f<dust) {
                    val x=.294f+(i*73%2399)/2399f*.42f
                    val y=.245f+(i*137%2377)/2377f*.455f
                    drawOval(Color(0xFF5D5141).copy(alpha=dust*(.22f+(i%5)*.065f)),
                        at(x,y),Size(unit*(.0006f+(i%3)*.0005f),unit*.001f))
                }
            }
        }
        // Horizontal ledges catch a heavier layer than the upright surfaces.
        val surfaces=listOf(
            polygon(.225f to .158f,.775f to .158f,.781f to .171f,.23f to .171f),
            polygon(.235f to .337f,.29f to .337f,.29f to .347f,.235f to .347f),
            polygon(.716f to .337f,.77f to .337f,.77f to .349f,.716f to .349f),
            polygon(.295f to .699f,.706f to .699f,.719f to .719f,.286f to .719f),
            polygon(.246f to .724f,.754f to .724f,.789f to .746f,.215f to .746f)
        )
        if(dust>0f) surfaces.forEachIndexed { surface,mask ->
            clipPath(mask) {
                // A matte pale film visibly dulls polished wood from day one.
                drawRect(Color(0xFFC8B99D).copy(alpha=dust*.68f))
                repeat(130) { i ->
                    val x=.215f+(i*73%127)/127f*.575f
                    val y=when(surface) {0 -> .164f;1,2 -> .342f;3 -> .707f;else -> .736f}
                    val center=at(x,y+(i%5-2)*.003f)
                    val radius=unit*(.006f+(i%7)*.003f)
                    drawCircle(Brush.radialGradient(listOf(Color(0xFFB5A180).copy(alpha=dust*.33f),Color.Transparent),center,radius),radius,center)
                }
                // Fixed grains gain coverage and contrast each day; existing dirt never jumps around.
                repeat(1800) { i ->
                    val x=.215f+(i*73%1789)/1789f*.575f
                    val y=when(surface) {0 -> .158f;1,2 -> .337f;3 -> .699f;else -> .724f}+(i*137%1777)/1777f*.026f
                    if((i%101)/101f<dust) drawOval(
                        Color(if(i%3==0) 0xFFE0D3B9 else 0xFF675740).copy(alpha=(.2f+dust*.45f)),
                        at(x,y),Size(unit*(.0008f+(i%3)*.0006f),unit*.0009f))
                }
            }
        }
        // Thin grime follows the inside wood seams, not the room or the deity silhouettes.
        if(dust>0f) listOf(.294f,.714f).forEach { x ->
            val start=at(x,.352f);val end=at(x,.698f)
            drawLine(Color(0xFF71604B).copy(alpha=dust*.34f),start,end,unit*.006f)
        }
        if(webs>0f) {
            // Fan webs attach to architectural corners; extra corners fill in over later days.
            listOf(Triple(.295f,.222f,1f),Triple(.705f,.222f,-1f),Triple(.295f,.59f,1f),Triple(.705f,.59f,-1f)).forEachIndexed { index,(x,y,direction) ->
                val strength=(webs-index*.045f).coerceIn(0f,1f)
                if(strength>0f) {
                    val origin=at(x,y)
                    val radius=unit*(.035f+strength*.105f)
                    val silk=Color.Black.copy(alpha=(.35f+strength*.6f)*min(1f,strength*12f))
                    fun strand(ray:Int,r:Float):Offset {
                        val a=ray/7f*PI.toFloat()/2
                        return origin+Offset(direction*cos(a)*r,sin(a)*r)
                    }
                    repeat(8) { ray -> drawLine(silk,origin,strand(ray,radius),unit*.0009f) }
                    repeat(7) { ring ->
                        val r=radius*(ring+1)/7
                        val path=Path().apply {
                            val first=strand(0,r);moveTo(first.x,first.y)
                            repeat(7) { ray ->
                                val a=strand(ray,r);val b=strand(ray+1,r)
                                val mid=origin+((a+b)*.5f-origin)*.91f
                                quadraticBezierTo(mid.x,mid.y,b.x,b.y)
                            }
                        }
                        drawPath(path,silk,style=androidx.compose.ui.graphics.drawscope.Stroke(unit*.0007f))
                    }
                }
            }
        }
    }
}

/** Only flowers already placed at the deities' feet can wither. */
@Composable
fun OfferedFlowerAgingOverlay(viewport: TempleViewport, wilt: Float, offerings: List<FlowerOffering>) {
    if(wilt<=0f || offerings.isEmpty()) return
    Canvas(Modifier.fillMaxSize()) {
        val unit=viewport.imageWidth
        fun at(x:Float,y:Float)=viewport.pixel(TemplePoint(x,y)).let {Offset(it.x,it.y)}
        // Brown curled petals sit directly on each flower, rather than tinting the whole plate.
        val points=(0..1).flatMap { deity -> offerings.filter {it.deity==deity}.takeLast(TempleSceneLayout.OFFERED_FLOWER_SLOTS).map {it.position} }
        points.forEachIndexed { i,p ->
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
