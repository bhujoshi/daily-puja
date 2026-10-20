package com.worship.nityamandir.engine

import com.worship.nityamandir.data.ShrineSelection
import kotlin.math.min

/** The same fitted image rectangle drives drawing, ritual effects and touch targets. */
class IdolPlacement(selection:ShrineSelection) {
    private val id=selection["idols"]
    val space=ShrineSpace.forId(selection["shrine"])
    // True visible aspect ratios after stripping alpha < 24 edge noise (AssetImageCache.trimmedIdol)
    private val aspect=when(id) {
        "ganesh_hanuman" -> 930f/940f
        "original" -> 928f/914f
        "radha_krishna" -> 650f/944f
        "shiva" -> 725f/978f
        "lakshmi" -> 726f/985f
        "durga" -> 707f/968f
        "ram_darbar" -> 723f/924f
        "hanuman" -> 609f/969f
        else -> 723f/924f
    }
    private val maxWidth=if(selection.deityCount==1) min(.37f,space.idolWidth) else space.idolWidth
    private val maxHeight=min(space.idolHeight,space.altarY-space.bellCeilingY)
    val width=min(maxWidth,maxHeight*aspect)
    val height=width/aspect
    val bottom=space.altarY
    val left=.5f-width/2
    val top=bottom-height
    private fun map(point:TemplePoint)=TemplePoint(left+point.x*width,top+point.y*height)
    // Landmarks measured on the alpha>=24 cropped source image, not its frame center.
    private val foreheads=when(id) {
        "original" -> listOf(TemplePoint(.320f,.215f),TemplePoint(.710f,.218f))
        "ganesh_hanuman" -> listOf(TemplePoint(.272f,.218f),TemplePoint(.765f,.205f))
        "radha_krishna" -> listOf(TemplePoint(.330f,.222f),TemplePoint(.650f,.198f))
        "shiva" -> listOf(TemplePoint(.500f,.210f))
        "lakshmi" -> listOf(TemplePoint(.500f,.205f))
        "durga" -> listOf(TemplePoint(.500f,.180f))
        "ram_darbar" -> listOf(TemplePoint(.505f,.175f))
        "hanuman" -> listOf(TemplePoint(.500f,.212f))
        else -> listOf(TemplePoint(.505f,.175f))
    }
    val heads=foreheads.map(::map)
    val crowns=foreheads.mapIndexed {index,p -> map(p.copy(y=when(id) {
        "original" -> if(index==0) .028f else .052f
        "ganesh_hanuman" -> if(index==0) .028f else .038f
        "radha_krishna" -> if(index==0) .115f else .055f
        "shiva" -> .028f
        "lakshmi" -> .028f
        "durga" -> .028f
        "ram_darbar" -> .028f
        "hanuman" -> .028f
        else -> .028f
    }))}
    fun bathSource(deity:Int)=crowns[deity].let {TemplePoint(it.x,it.y-.080f)}
    fun aartiPosition(progress:Float):TemplePoint {
        val base=TempleSceneLayout.aartiPosition(progress)
        val shift=(top+height*.60f)-.58f
        return base.copy(y=base.y+shift*TempleSceneLayout.pickup(progress))
    }
    val prasadFloor = TemplePoint(
        .50f,
        space.lamp.y
    )
    fun prasadPosition(progress:Float):TemplePoint {
        val base=TempleSceneLayout.prasadPosition(progress)
        val p=progress.coerceIn(0f,1f)
        val rise=(p/.22f).coerceIn(0f,1f)
        val settle=((p-.78f)/.22f).coerceIn(0f,1f)
        val altarShift=top+height*.60f-.56f
        val targetX=base.x+(prasadFloor.x-TempleSceneLayout.prasadFloor.x)*settle
        val targetY=base.y+altarShift*rise*(1-settle)+(prasadFloor.y-TempleSceneLayout.prasadFloor.y)*settle
        return TemplePoint(targetX,targetY)
    }
    fun offering(deity:Int,count:Int):TemplePoint {
        val base=TempleSceneLayout.offeredFlower(deity,count)
        val oldCenter=if(deity==0) .395f else .615f
        val slot=count%TempleSceneLayout.OFFERED_FLOWER_SLOTS
        val spread=if(slot!=0 && heads.size==2 && heads[1].x-heads[0].x<.15f) {
            if(deity==0) -.045f else .045f
        } else 0f
        return TemplePoint(base.x-oldCenter+heads[deity].x+spread,bottom+.008f+(base.y-.685f))
    }
}
