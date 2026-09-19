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
    const val AARTI_MODEL_SIZE = AARTI_WIDTH * 2f
    const val AARTI_DEPTH = 1.1f
    const val AARTI_YAW = -90f
    const val AARTI_TILT = 40f
    // The GLB's broad bowl points along -X; yaw first, then tilt toward the altar.
    // Project the same transformed wick into the screen-space flame overlay.
    fun aartiWick(lamp: TemplePoint): TemplePoint {
        val yaw = AARTI_YAW * PI.toFloat() / 180f
        val tilt = AARTI_TILT * PI.toFloat() / 180f
        // Seat the wick inside the bowl rather than above its far rim.
        val x = -.052f
        val y = .018f
        return TemplePoint(lamp.x+x*cos(yaw),lamp.y-(y*cos(tilt)+x*sin(yaw)*sin(tilt)))
    }
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
    fun offeredLaddu(index: Int) = TemplePoint(.59f+(index%2)*.055f,.775f+(index/2)*.048f)

    const val OFFERED_FLOWER_SLOTS = 10
    fun flowerSize(index: Int) = (.10f+(index%3)*.008f)*1.3f

    // Two orderly rows at the feet, leaving the central lamp and prasad clear.
    fun offeredFlower(deity: Int, count: Int): TemplePoint {
        val slot=count%OFFERED_FLOWER_SLOTS
        val center=if(deity==0) .395f else .615f
        val column=when(slot%5) {0 -> 0;1 -> -1;2 -> 1;3 -> -2;else -> 2}
        return TemplePoint(center+column*.030f,.685f+(slot/5)*.028f)
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
        // Extend the lower half of the circle by 50%, keeping its upper reach fixed.
        val vertical = sin(angle)
        val orbit = TemplePoint(center.x+.145f*cos(angle),center.y+.063f*(vertical+.875f*max(0f,vertical)))
        val lift = pickup(p)
        val travel = smooth((lift-.3f)/.7f)
        val raised = TemplePoint(aartiRest.x, aartiRest.y-.10f*lift)
        return TemplePoint(raised.x+(orbit.x-raised.x)*travel,raised.y+(orbit.y-raised.y)*travel)
    }
}
