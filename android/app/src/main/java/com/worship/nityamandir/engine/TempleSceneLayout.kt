package com.worship.nityamandir.engine

import kotlin.math.*

/** Coordinates are fractions of the portrait image WIDTH, preserving scale on every display. */
data class TemplePoint(val x: Float, val y: Float)
class TempleViewport(val width: Float, val height: Float) {
    val imageWidth = max(width, height / IMAGE_RATIO) * ZOOM
    val left = (width-imageWidth)/2f
    // Anchor the bottom of the puja thali near the bottom of tall displays while keeping top headroom above the shrine.
    val top = (height - height * BOTTOM_MARGIN_RATIO - imageWidth * PLATE_BOTTOM_FACTOR)
        .coerceIn(0f, height * MAX_TOP_SPACE_RATIO)
    fun pixel(point: TemplePoint) = TemplePoint(left+point.x*imageWidth,top+point.y*imageWidth)
    fun point(pixelX: Float, pixelY: Float) = TemplePoint((pixelX - left) / imageWidth, (pixelY - top) / imageWidth)
    /** Keep the whole item inside the visible crop, with the same anchor for its tap target. */
    fun keepInsideHorizontalBounds(point: TemplePoint, halfWidth: Float, padding: Float): TemplePoint {
        val inset = halfWidth + padding / imageWidth
        val minX = -left / imageWidth + inset
        val maxX = (width-left) / imageWidth - inset
        return point.copy(x=if(minX<=maxX) point.x.coerceIn(minX,maxX) else .5f)
    }
    companion object {
        const val ZOOM = 1.145f
        const val IMAGE_RATIO = 1672f / 940f
        const val PLATE_BOTTOM_FACTOR = 1.325f
        const val BOTTOM_MARGIN_RATIO = 0.068f
        const val MAX_TOP_SPACE_RATIO = 0.072f
        // Keep the background arch/ledge aligned with the 1.08x / 0.25 origin calibration when zooming deeper.
        const val BACKGROUND_ORIGIN_Y = 0.02f * (ZOOM / 1.08f) / (ZOOM - 1f)
    }
}

object TempleSceneLayout {
    const val LAMP_SCALE = .5f
    const val OIL_SIZE = .225f * 1.5f * 1.6f * LAMP_SCALE
    const val PLATE_SIZE = .52f
    const val AARTI_SCALE = 1.25f
    const val AARTI_WIDTH = .15f * AARTI_SCALE
    const val AARTI_HEIGHT = .135f * AARTI_SCALE
    val oil = TemplePoint(.505f,.665f)
    val incense = TemplePoint(.325f,.875f)
    const val INCENSE_WIDTH = .096f
    const val BELL_DISPLAY_WIDTH = .090f
    const val STANDING_LAMP_WIDTH = .098f
    val waterLota = TemplePoint(.638f,.902f)
    const val WATER_LOTA_WIDTH = .094f
    const val WATER_LOTA_POUR_WIDTH = .115f
    const val WATER_LOTA_DEFAULT_ASPECT = 512f / 390f
    const val WATER_LOTA_PIVOT_X = .30f
    const val WATER_LOTA_PIVOT_Y = .26f
    const val WATER_LOTA_POUR_ANGLE_DEG = -62f
    // Normalized sprite anchors on copper_lota.png: outer rolled left rim crest and inner throat meniscus.
    const val WATER_LOTA_LIP_OUTER_U = .128f
    const val WATER_LOTA_LIP_OUTER_V = .144f
    const val WATER_LOTA_LIP_INNER_U = .215f
    const val WATER_LOTA_LIP_INNER_V = .135f
    // Slight horizontal lead so water exiting the left-tilted lip curves naturally under gravity onto crown.x.
    const val WATER_LOTA_SPOUT_LEAD_X = .014f
    val plate = TemplePoint(.50f,1.14f)
    val kumkumKatori = TemplePoint(plate.x - .031f, plate.y - .112f)
    val chandanKatori = TemplePoint(plate.x + .0335f, plate.y - .112f)
    val akshatKatori = TemplePoint(plate.x + .001f, plate.y - .090f)
    val conch = TemplePoint(.625f,1.125f)
    val conchVisual = TemplePoint(.633f,1.147f)
    const val CONCH_DISPLAY_WIDTH = .132f
    const val CONCH_STAND_WIDTH = .130f
    const val CONCH_SCALE = 1.15f
    const val CONCH_SIZE = .26f * CONCH_SCALE
    const val CONCH_WIDTH = .16f * CONCH_SCALE
    const val CONCH_HEIGHT = .20f * CONCH_SCALE
    const val CONCH_REST_DEPTH = .58f
    const val FLOWER_COUNT = 10
    val bell = TemplePoint(.22f,.93f)
    const val BELL_SIZE = .22f
    // A sphere around the fitted model contains its bounds throughout the ringing swing.
    val bellHalfWidth = BELL_SIZE * sqrt(3f) / 8f
    const val AARTI_MODEL_SIZE = AARTI_WIDTH * 2f
    const val AARTI_DEPTH = 1.6f
    // The camera is at Z=4.5. Keep carried objects ahead of the full flower beds
    // while leaving enough room for their model bounds before the near plane.
    const val FLOWER_FLIGHT_DEPTH = 3.2f
    const val ACTIVE_RITUAL_DEPTH = 3.4f
    const val AARTI_YAW = -90f
    const val AARTI_TILT = 40f
    // Align the flame with the cotton wick spout at the top-center of the brass diya facing the idols.
    fun aartiWick(lamp: TemplePoint): TemplePoint {
        return TemplePoint(lamp.x, lamp.y - .090f)
    }
    val aartiRest = TemplePoint(.765f,1.01f)
    fun feet(deity: Int, petal: Int = 0) = TemplePoint((if(deity==0) .405f else .604f)+(petal-2f)*.015f,.689f)
    fun plateFlower(index: Int): TemplePoint {
        // Non-overlapping floral crescent along the lower-left betel-leaf well of the Puja Thali,
        // staying strictly below and left of the upper Roli & Chandan katoris (y >= 1.092f):
        // 0..8: vibrant orange & golden-yellow marigold cluster over the betel leaves
        // 9: crowning crimson hibiscus at the bottom-left beside the prasad bowl
        val cx = plate.x
        val cy = plate.y
        return when (index) {
            0 -> TemplePoint(cx - .162f, cy - .042f)
            1 -> TemplePoint(cx - .180f, cy - .004f)
            2 -> TemplePoint(cx - .125f, cy - .022f)
            3 -> TemplePoint(cx - .148f, cy + .018f)
            4 -> TemplePoint(cx - .172f, cy + .042f)
            5 -> TemplePoint(cx - .112f, cy + .026f)
            6 -> TemplePoint(cx - .142f, cy - .012f)
            7 -> TemplePoint(cx - .126f, cy + .006f)
            8 -> TemplePoint(cx - .102f, cy + .056f)
            9 -> TemplePoint(cx - .122f, cy + .078f)
            else -> {
                val ang = index * (2f * PI.toFloat() / FLOWER_COUNT)
                TemplePoint(cx - .138f + .044f * cos(ang), cy + .016f + .036f * sin(ang))
            }
        }
    }
    const val PRASAD_SIZE = .155f
    const val PRASAD_DEPTH = 1.45f
    val prasadRest = TemplePoint(.50f,1.215f)
    val prasadFloor = TemplePoint(.50f,.88f)

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
        return TemplePoint(conch.x-.125f*travel, conch.y-.10f*lift-.31f*travel)
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

    /** Smoothly travels from the floor resting spot to above the deity crown, tilts to pour, and returns. */
    fun waterLotaTravel(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return when {
            p < .14f -> smooth(p / .14f)
            p > .86f -> smooth((1f - p) / .14f)
            else -> 1f
        }
    }

    fun waterLotaTilt(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return smooth(min((p - .10f) / .10f, (.86f - p) / .09f).coerceIn(0f, 1f))
    }

    fun waterLotaPosition(restCenter: TemplePoint, pourCenter: TemplePoint, progress: Float): TemplePoint {
        val travel = waterLotaTravel(progress)
        return TemplePoint(
            restCenter.x + (pourCenter.x - restCenter.x) * travel,
            restCenter.y + (pourCenter.y - restCenter.y) * travel - .03f * sin(PI.toFloat() * travel)
        )
    }

    fun waterLotaWidth(progress: Float): Float {
        val travel = waterLotaTravel(progress)
        return WATER_LOTA_WIDTH + (WATER_LOTA_POUR_WIDTH - WATER_LOTA_WIDTH) * travel
    }

    /** Exact world position of a normalized (u, v) point on the copper lota sprite at the given tilt. */
    fun waterLotaPointOnSprite(
        center: TemplePoint,
        width: Float,
        aspect: Float,
        tilt: Float,
        u: Float,
        v: Float
    ): TemplePoint {
        val height = width * aspect
        val pivotX = center.x + (WATER_LOTA_PIVOT_X - .5f) * width
        val pivotY = center.y + (WATER_LOTA_PIVOT_Y - .5f) * height
        val dx = (u - WATER_LOTA_PIVOT_X) * width
        val dy = (v - WATER_LOTA_PIVOT_Y) * height
        val rad = WATER_LOTA_POUR_ANGLE_DEG * tilt * (PI.toFloat() / 180f)
        val c = cos(rad)
        val s = sin(rad)
        return TemplePoint(pivotX + dx * c - dy * s, pivotY + dx * s + dy * c)
    }

    fun waterLotaLip(
        center: TemplePoint,
        width: Float,
        aspect: Float = WATER_LOTA_DEFAULT_ASPECT,
        tilt: Float = 1f
    ): TemplePoint = waterLotaPointOnSprite(center, width, aspect, tilt, WATER_LOTA_LIP_OUTER_U, WATER_LOTA_LIP_OUTER_V)

    fun waterLotaInnerLip(
        center: TemplePoint,
        width: Float,
        aspect: Float = WATER_LOTA_DEFAULT_ASPECT,
        tilt: Float = 1f
    ): TemplePoint = waterLotaPointOnSprite(center, width, aspect, tilt, WATER_LOTA_LIP_INNER_U, WATER_LOTA_LIP_INNER_V)

    /** Solves the exact lota center so the tilted pouring lip sits directly at the stream origin with zero gap. */
    fun waterLotaPourCenter(
        source: TemplePoint,
        width: Float = WATER_LOTA_POUR_WIDTH,
        aspect: Float = WATER_LOTA_DEFAULT_ASPECT
    ): TemplePoint {
        val desiredLip = TemplePoint(source.x + WATER_LOTA_SPOUT_LEAD_X, source.y)
        val offsetLip = waterLotaLip(TemplePoint(0f, 0f), width, aspect, 1f)
        return TemplePoint(desiredLip.x - offsetLip.x, desiredLip.y - offsetLip.y)
    }

    /** Normalized stream head progression (0 at lip -> 1 at crown) as water begins to pour. */
    fun waterStreamHead(progress: Float): Float = ((progress - .155f) / .055f).coerceIn(0f, 1f)

    /** Normalized stream tail progression (0 attached to lip -> 1 detached to crown) as lota tilts upright. */
    fun waterStreamTail(progress: Float): Float = ((progress - .775f) / .060f).coerceIn(0f, 1f)

    /** Smooth stream flow thickness factor synced to lota tilt. */
    fun waterPourIntensity(progress: Float): Float =
        smooth(min((progress - .150f) / .050f, (.815f - progress) / .050f).coerceIn(0f, 1f))
}
