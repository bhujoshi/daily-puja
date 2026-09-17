package com.worship.nityamandir.engine

import kotlin.math.*

/** Coordinates are fractions of the portrait image WIDTH, preserving scale on every display. */
data class TemplePoint(val x: Float, val y: Float)
class TempleViewport(val width: Float, val height: Float) {
    val imageWidth = max(width, height / IMAGE_RATIO) * ZOOM
    val left = (width-imageWidth)/2f
    fun pixel(point: TemplePoint) = TemplePoint(left+point.x*imageWidth,point.y*imageWidth)
    companion object { const val ZOOM = 1.08f; const val IMAGE_RATIO = 1672f / 940f }
}

object TempleSceneLayout {
    const val LAMP_SCALE = .5f
    const val OIL_SIZE = .225f * 1.5f * 1.6f * LAMP_SCALE
    const val PLATE_SIZE = .46f * 1.6f * 1.5f
    const val AARTI_WIDTH = .36f * LAMP_SCALE
    const val AARTI_HEIGHT = .45f * LAMP_SCALE
    val oil = TemplePoint(.505f,.665f)
    val plate = TemplePoint(.50f,1.14f)
    val conch = TemplePoint(.50f,1.10f)
    const val CONCH_SIZE = .34f
    const val CONCH_REST_DEPTH = .58f
    const val FLOWER_COUNT = 24
    val bell = TemplePoint(.16f,.97f)
    val aartiRest = TemplePoint(.79f,1.08f)
    fun feet(deity: Int, petal: Int = 0) = TemplePoint((if(deity==0) .405f else .604f)+(petal-2f)*.015f,.689f)
    fun plateFlower(index: Int): TemplePoint {
        val angle = index*2.39996f
        val radius = .012f + .014f*sqrt(index.toFloat())
        return TemplePoint(plate.x-.16f+cos(angle)*radius,plate.y+sin(angle)*radius*.45f-.012f)
    }
    const val LADDU_COUNT = 4
    const val LADDU_SIZE = .085f
    fun plateLaddu(index: Int) = TemplePoint(.635f+(index%2)*.05f,1.105f+(index/2)*.048f)
    fun offeredLaddu(index: Int) = TemplePoint(.59f+(index%2)*.05f,.705f+(index/2)*.045f)

    // Lower torso and feet only; include a flower-radius margin below either face.
    fun randomFlowerPosition(deity: Int): TemplePoint {
        val center = if(deity==0) .405f else .604f
        return TemplePoint(center+kotlin.random.Random.nextFloat()*.09f-.045f,
            .585f+kotlin.random.Random.nextFloat()*.105f)
    }

    fun flowerFlight(start: TemplePoint, end: TemplePoint, progress: Float): TemplePoint {
        val t = progress.coerceIn(0f,1f)
        return TemplePoint(start.x+(end.x-start.x)*t,start.y+(end.y-start.y)*t-.22f*sin(PI.toFloat()*t))
    }
    private fun smooth(value: Float): Float {
        val t = value.coerceIn(0f, 1f)
        return t*t*(3f-2f*t)
    }

    /** Clear the rim before traveling or turning; reverse the path to set down. */
    fun pickup(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return smooth(min(p/.22f, (1f-p)/.22f))
    }

    fun conchPosition(progress: Float): TemplePoint {
        val lift = pickup(progress)
        val travel = smooth((lift-.3f)/.7f)
        return TemplePoint(conch.x-.09f*travel, conch.y-.10f*lift-.24f*travel)
    }

    fun conchTurn(progress: Float): Float = smooth((pickup(progress)-.3f)/.7f)

    fun aartiPosition(progress: Float): TemplePoint {
        val p = progress.coerceIn(0f,1f)
        val center = TemplePoint(.51f,.58f)
        val angle = ((p-.22f)/.56f).coerceIn(0f,1f)*6f*PI.toFloat()
        val orbit = TemplePoint(center.x+.145f*cos(angle),center.y+.063f*(sin(angle)+.25f*max(0f,sin(angle))))
        val lift = pickup(p)
        val travel = smooth((lift-.3f)/.7f)
        val raised = TemplePoint(aartiRest.x, aartiRest.y-.10f*lift)
        return TemplePoint(raised.x+(orbit.x-raised.x)*travel,raised.y+(orbit.y-raised.y)*travel)
    }
}
