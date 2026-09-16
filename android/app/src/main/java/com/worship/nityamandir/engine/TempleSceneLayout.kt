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
    const val OIL_SIZE = .225f * 1.5f * 1.6f
    const val PLATE_SIZE = .46f * 1.6f * 1.5f
    const val AARTI_SIZE = .29f * 1.4f
    val oil = TemplePoint(.505f,.665f)
    val plate = TemplePoint(.50f,1.14f)
    val conch = TemplePoint(.66f,1.14f)
    const val FLOWER_COUNT = 24
    val bell = TemplePoint(.22f,1.055f)
    val aartiRest = TemplePoint(.79f,1.065f)
    fun feet(deity: Int, petal: Int = 0) = TemplePoint((if(deity==0) .405f else .604f)+(petal-.5f)*.033f,.689f)
    fun plateFlower(index: Int): TemplePoint {
        val angle = index*2.39996f
        val radius = .018f + .021f*sqrt(index.toFloat())
        return TemplePoint(plate.x-.065f+cos(angle)*radius,plate.y+sin(angle)*radius*.45f-.012f)
    }
    fun flowerFlight(start: TemplePoint, end: TemplePoint, progress: Float): TemplePoint {
        val t = progress.coerceIn(0f,1f)
        return TemplePoint(start.x+(end.x-start.x)*t,start.y+(end.y-start.y)*t-.22f*sin(PI.toFloat()*t))
    }
    fun aartiPosition(progress: Float): TemplePoint {
        val p = progress.coerceIn(0f,1f)
        val center = TemplePoint(.51f,.54f)
        val angle = ((p-.12f)/.76f).coerceIn(0f,1f)*6f*PI.toFloat()
        val orbit = TemplePoint(center.x+.145f*cos(angle),center.y+.063f*sin(angle))
        val lift = min(p/.12f,(1-p)/.12f).coerceIn(0f,1f)
        val smooth = lift*lift*(3-2*lift)
        return TemplePoint(aartiRest.x+(orbit.x-aartiRest.x)*smooth,aartiRest.y+(orbit.y-aartiRest.y)*smooth)
    }
}
