package com.worship.nityamandir.engine

import com.worship.nityamandir.data.ShrineSelection
import kotlin.math.min

/** The same fitted image rectangle drives drawing, ritual effects and touch targets. */
class IdolPlacement(selection:ShrineSelection) {
    private val id=selection["idols"]
    val space=ShrineSpace.forId(selection["shrine"])
    private val aspect=when(id) {
        "ganesh_hanuman" -> 1653f/927f
        "original" -> 1341f/1116f
        "shiva" -> 1185f/1209f
        "lakshmi" -> 1234f/1254f
        "durga" -> 1259f/1232f
        else -> 1236f/1209f
    }
    private val maxWidth=if(selection.deityCount==1) min(.31f,space.idolWidth) else space.idolWidth
    private val maxHeight=space.idolHeight
    val width=min(maxWidth,maxHeight*aspect)
    val height=width/aspect
    val bottom=space.altarY
    val left=.5f-width/2
    val top=bottom-height
    private fun map(point:TemplePoint)=TemplePoint(left+point.x*width,top+point.y*height)
    // Landmarks measured on the alpha-cropped source image, not its frame center.
    private val foreheads=when(id) {
        "original" -> listOf(TemplePoint(.304f,.264f),TemplePoint(.734f,.134f))
        "ganesh_hanuman" -> listOf(TemplePoint(.344f,.203f),TemplePoint(.687f,.163f))
        "shiva" -> listOf(TemplePoint(.505f,.184f))
        "lakshmi" -> listOf(TemplePoint(.510f,.230f))
        "durga" -> listOf(TemplePoint(.516f,.134f))
        else -> listOf(TemplePoint(.508f,.150f))
    }
    val heads=foreheads.map(::map)
    val crowns=foreheads.mapIndexed {index,p -> map(p.copy(y=when(id) {
        "original" -> if(index==0) .095f else .014f
        "shiva" -> .039f
        else -> .022f
    }))}
    fun bathSource(deity:Int)=crowns[deity].let {TemplePoint(it.x,it.y-.080f)}
    fun aartiPosition(progress:Float):TemplePoint {
        val base=TempleSceneLayout.aartiPosition(progress)
        val shift=(top+height*.60f)-.58f
        return base.copy(y=base.y+shift*TempleSceneLayout.pickup(progress))
    }
    val prasadFloor=TemplePoint(.60f,space.floorY+.065f)
    fun prasadPosition(progress:Float):TemplePoint {
        val base=TempleSceneLayout.prasadPosition(progress)
        val p=progress.coerceIn(0f,1f)
        val rise=(p/.22f).coerceIn(0f,1f)
        val settle=((p-.78f)/.22f).coerceIn(0f,1f)
        val altarShift=top+height*.60f-.56f
        return base.copy(y=base.y+altarShift*rise*(1-settle)+(prasadFloor.y-TempleSceneLayout.prasadFloor.y)*settle)
    }
    fun offering(deity:Int,count:Int):TemplePoint {
        val base=TempleSceneLayout.offeredFlower(deity,count)
        val oldCenter=if(deity==0) .395f else .615f
        return TemplePoint(base.x-oldCenter+heads[deity].x,bottom+.008f+(base.y-.685f))
    }
}
