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
    val conch = TemplePoint(.28f,1.06f)
    const val CONCH_SIZE = .34f
    const val CONCH_REST_DEPTH = .58f
    const val FLOWER_COUNT = 10
    val bell = TemplePoint(.15f,.98f)
    const val AARTI_MODEL_SIZE = AARTI_WIDTH * 2f
    const val AARTI_DEPTH = 1.6f
    // The camera is at Z=4.5. Keep carried objects ahead of the full flower beds
    // while leaving enough room for their model bounds before the near plane.
    const val FLOWER_FLIGHT_DEPTH = 3.2f
    const val ACTIVE_RITUAL_DEPTH = 3.4f
    const val AARTI_YAW = -90f
    const val AARTI_TILT = 40f
    // The GLB's broad bowl points along -X; yaw first, then tilt toward the altar.
    // Project the same transformed wick into the screen-space flame overlay.
    fun aartiWick(lamp: TemplePoint): TemplePoint {
        val yaw = AARTI_YAW * PI.toFloat() / 180f
        val tilt = AARTI_TILT * PI.toFloat() / 180f
        // Keep the flame slightly above the bowl, both in motion and at rest.
        val x = -.052f
        val y = .070f
        return TemplePoint(lamp.x+x*cos(yaw),lamp.y-(y*cos(tilt)+x*sin(yaw)*sin(tilt)))
    }
    val aartiRest = TemplePoint(.79f,1.08f)
    fun feet(deity: Int, petal: Int = 0) = TemplePoint((if(deity==0) .405f else .604f)+(petal-2f)*.015f,.689f)
    fun plateFlower(index: Int): TemplePoint {
        // Natural tiered devotional thali:
        // Tier 0 (0..5): outer ring resting on the plate bed
        // Tier 1 (6..8): middle cluster resting on top of the outer ring
        // Tier 2 (9): peak crowning bloom at the apex
        val cx = plate.x
        val cy = plate.y
        return when (index) {
            0 -> TemplePoint(cx + .090f, cy)
            1 -> TemplePoint(cx - .090f, cy)
            2 -> TemplePoint(cx + .045f, cy + .050f)
            3 -> TemplePoint(cx - .045f, cy - .050f)
            4 -> TemplePoint(cx - .045f, cy + .050f)
            5 -> TemplePoint(cx + .045f, cy - .050f)
            6 -> TemplePoint(cx + .036f, cy + .013f)
            7 -> TemplePoint(cx - .036f, cy + .013f)
            8 -> TemplePoint(cx, cy - .027f)
            9 -> TemplePoint(cx, cy)
            else -> {
                val ang = index * (2f * PI.toFloat() / FLOWER_COUNT)
                TemplePoint(cx + .065f * cos(ang), cy + .042f * sin(ang))
            }
        }
    }
    const val PRASAD_SIZE = .18f
    const val PRASAD_DEPTH = 1.45f
    val prasadRest = TemplePoint(.66f,1.08f)
    val prasadFloor = TemplePoint(.28f,.915f)

    /** Lift, offer one clockwise circle, then set the bowl on the floor. */
    fun prasadPosition(progress: Float): TemplePoint {
        val p = progress.coerceIn(0f,1f)
        val circleStart = TemplePoint(.65f,.56f)
        fun travel(from: TemplePoint, to: TemplePoint, t: Float): TemplePoint {
            val eased = smooth(t)
            return TemplePoint(from.x+(to.x-from.x)*eased,from.y+(to.y-from.y)*eased)
        }
        return when {
            p < .22f -> travel(prasadRest,circleStart,p/.22f)
            p <= .78f -> {
                val angle = (p-.22f)/.56f*2f*PI.toFloat()
                TemplePoint(.51f+.14f*cos(angle),.56f+.10f*sin(angle))
            }
            else -> travel(circleStart,prasadFloor,(p-.78f)/.22f)
        }
    }

    const val OFFERED_FLOWER_SLOTS = 8
    const val FLOWER_SPACING = .045f
    fun flowerSize(index: Int) = (.10f+(index%3)*.008f)*1.3f

    fun offeredFlowerSize(index: Int) = flowerSize(index)

    // Multiple visible offerings per deity keep full-size blooms in a tiered bed.
    fun offeredFlower(deity: Int, count: Int): TemplePoint {
        val slot = count % OFFERED_FLOWER_SLOTS
        val center = if(deity == 0) .395f else .615f
        val cols = floatArrayOf(0f, -.045f, .040f, -.022f, .022f, 0f, -.045f, .040f)
        val rows = floatArrayOf(.685f, .685f, .685f, .699f, .699f, .713f, .713f, .713f)
        return TemplePoint(center + cols[slot], rows[slot])
    }

    /** Conservative spheres enclose any rotation of a model fitted to its largest dimension.
     * Reuse free depth between blooms instead of stacking every overlapping pair in
     * front of the previous one; a full two-deity bed must fit before the camera.
     * Points use image-width fractions; scene X/Y use twice that scale.
     */
    fun flowerDepths(points: List<TemplePoint>, sizes: List<Float>, base: Float): List<Float> {
        require(points.size == sizes.size)
        val depths=MutableList(points.size) {base}
        points.indices.forEach {i ->
            val occupied=mutableListOf<Pair<Float,Float>>()
            for(j in 0 until i) {
                val dx=2f*(points[i].x-points[j].x)
                val dy=2f*(points[i].y-points[j].y)
                val separation=sqrt(3f)*(sizes[i]+sizes[j])/2f+.004f
                val remaining=separation*separation-dx*dx-dy*dy
                if(remaining>0f) {
                    val radius=sqrt(remaining)
                    occupied.add(depths[j]-radius to depths[j]+radius)
                }
            }
            for((start,end) in occupied.sortedBy {it.first}) {
                if(depths[i]<start) break
                if(depths[i]<=end) depths[i]=end+.001f
            }
        }
        return depths
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
        return TemplePoint(conch.x+.14f*travel, conch.y-.10f*lift-.24f*travel)
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
