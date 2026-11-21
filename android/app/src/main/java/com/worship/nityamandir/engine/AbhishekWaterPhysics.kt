package com.worship.nityamandir.engine

import kotlin.math.*
import kotlin.random.Random

/**
 * Compact normalized occupancy & outward surface-normal field extracted from an idol's
 * alpha silhouette so Abhishek water streams, rivulets, and deflected droplets interact
 * realistically with each deity's true sculpted contours.
 */
class IdolWaterSurface(
    val cols: Int,
    val rows: Int,
    private val solid: FloatArray,
    private val smoothSolid: FloatArray
) {
    init {
        require(cols >= 8 && rows >= 8)
        require(solid.size == cols * rows && smoothSolid.size == cols * rows)
    }

    fun isSolid(u: Float, v: Float): Boolean {
        if (u < 0f || u > 1f || v < 0f || v > 1f) return false
        val c = (u * (cols - 1)).roundToInt().coerceIn(0, cols - 1)
        val r = (v * (rows - 1)).roundToInt().coerceIn(0, rows - 1)
        return solid[r * cols + c] >= 0.5f
    }

    /** Bilinear smoothed occupancy in [0, 1]. */
    fun smoothOccupancy(u: Float, v: Float): Float {
        if (u < 0f || u > 1f || v < 0f || v > 1f) return 0f
        val fx = u * (cols - 1)
        val fy = v * (rows - 1)
        val x0 = floor(fx).toInt().coerceIn(0, cols - 1)
        val y0 = floor(fy).toInt().coerceIn(0, rows - 1)
        val x1 = (x0 + 1).coerceAtMost(cols - 1)
        val y1 = (y0 + 1).coerceAtMost(rows - 1)
        val tx = fx - x0
        val ty = fy - y0
        val top = smoothSolid[y0 * cols + x0] * (1f - tx) + smoothSolid[y0 * cols + x1] * tx
        val bot = smoothSolid[y1 * cols + x0] * (1f - tx) + smoothSolid[y1 * cols + x1] * tx
        return top * (1f - ty) + bot * ty
    }

    /**
     * Outward unit surface normal (pointing from solid idol toward open air) scaled to
     * isotropic world units using the idol's width and height.
     */
    fun normalAt(u: Float, v: Float, idolWidth: Float, idolHeight: Float): Pair<Float, Float> {
        val c = (u * (cols - 1)).roundToInt().coerceIn(1, cols - 2)
        val r = (v * (rows - 1)).roundToInt().coerceIn(1, rows - 2)
        val du = 2f / (cols - 1).toFloat()
        val dv = 2f / (rows - 1).toFloat()
        val gx = (smoothSolid[r * cols + (c + 1)] - smoothSolid[r * cols + (c - 1)]) / (du * idolWidth.coerceAtLeast(0.01f))
        val gy = (smoothSolid[(r + 1) * cols + c] - smoothSolid[(r - 1) * cols + c]) / (dv * idolHeight.coerceAtLeast(0.01f))
        // Solid is 1, air is 0 => outward normal points along -gradient.
        val nx = -gx
        val ny = -gy
        val len = hypot(nx, ny)
        return if (len > 1e-4f) (nx / len) to (ny / len) else (0f to -1f)
    }

    /**
     * Snaps the configured crown coordinate onto the exact top solid pixel of the deity's mukut
     * so the pouring stream makes crisp physical contact without floating above or penetrating through.
     */
    fun impactV(crownU: Float, crownV: Float, minU: Float, maxU: Float): Float {
        val cCenter = (crownU * (cols - 1)).roundToInt().coerceIn(0, cols - 1)
        val cMin = (minU * (cols - 1)).roundToInt().coerceIn(0, cols - 1)
        val cMax = (maxU * (cols - 1)).roundToInt().coerceIn(cMin, cols - 1)
        val searchStart = ((crownV - 0.045f) * (rows - 1)).roundToInt().coerceIn(0, rows - 1)
        val searchEnd = ((crownV + 0.065f) * (rows - 1)).roundToInt().coerceIn(searchStart, rows - 1)
        val loV = (crownV - 0.025f).coerceIn(0f, 0.45f)
        val hiV = (crownV + 0.035f).coerceIn(loV, 0.45f)
        for (r in searchStart..searchEnd) {
            for (dc in intArrayOf(0, -1, 1)) {
                val c = (cCenter + dc).coerceIn(cMin, cMax)
                if (solid[r * cols + c] >= 0.5f) {
                    val foundV = r.toFloat() / (rows - 1).toFloat()
                    return foundV.coerceIn(loV, hiV)
                }
            }
        }
        return crownV.coerceIn(0f, 0.45f)
    }

    data class EdgeSample(val u: Float, val clampedAtSharedSeam: Boolean)

    /**
     * Finds the outer silhouette edge connected to the solid region near (u, v) on [side] (-1 left, +1 right),
     * stopping at transparent gaps so water never teleports across empty air to detached weapons or another deity.
     */
    fun connectedEdge(u: Float, v: Float, side: Int, minU: Float, maxU: Float): EdgeSample? {
        val r = (v * (rows - 1)).roundToInt().coerceIn(0, rows - 1)
        val c0 = (u * (cols - 1)).roundToInt().coerceIn(0, cols - 1)
        val cMin = (minU * (cols - 1)).roundToInt().coerceIn(0, cols - 1)
        val cMax = (maxU * (cols - 1)).roundToInt().coerceIn(cMin, cols - 1)

        var seed = -1
        for (d in 0..5) {
            val leftCand = c0 - d
            if (leftCand in cMin..cMax && solid[r * cols + leftCand] >= 0.5f) {
                seed = leftCand
                break
            }
            val rightCand = c0 + d
            if (rightCand in cMin..cMax && solid[r * cols + rightCand] >= 0.5f) {
                seed = rightCand
                break
            }
        }
        if (seed < 0) return null

        var c = seed
        val step = if (side < 0) -1 else 1
        while (c + step in cMin..cMax && solid[r * cols + (c + step)] >= 0.5f) {
            c += step
        }
        val hitsSharedBoundary =
            (step < 0 && c == cMin && cMin > 0 && solid[r * cols + (c - 1)] >= 0.5f) ||
            (step > 0 && c == cMax && cMax < cols - 1 && solid[r * cols + (c + 1)] >= 0.5f)
        return EdgeSample(c.toFloat() / (cols - 1).toFloat(), hitsSharedBoundary)
    }

    /** Measures the real base contact width [leftU, rightU] of this deity's lotus pedestal. */
    fun pedestalSpan(crownU: Float, minU: Float, maxU: Float): Pair<Float, Float> {
        val cMin = (minU * (cols - 1)).roundToInt().coerceIn(0, cols - 1)
        val cMax = (maxU * (cols - 1)).roundToInt().coerceIn(cMin, cols - 1)
        val safeMaxU = max(minU, maxU)
        var bestLeft = (crownU - 0.14f).coerceIn(minU, safeMaxU)
        var bestRight = (crownU + 0.14f).coerceIn(minU, safeMaxU)
        var found = false
        for (r in ((rows * 0.88f).toInt()) until rows) {
            var rowMin = Int.MAX_VALUE
            var rowMax = Int.MIN_VALUE
            for (c in cMin..cMax) {
                if (solid[r * cols + c] >= 0.5f) {
                    if (c < rowMin) rowMin = c
                    if (c > rowMax) rowMax = c
                }
            }
            if (rowMin <= rowMax) {
                val lu = rowMin.toFloat() / (cols - 1).toFloat()
                val ru = rowMax.toFloat() / (cols - 1).toFloat()
                if (!found) {
                    bestLeft = lu
                    bestRight = ru
                    found = true
                } else {
                    bestLeft = min(bestLeft, lu)
                    bestRight = max(bestRight, ru)
                }
            }
        }
        return bestLeft to bestRight
    }

    companion object {
        const val DEFAULT_COLS = 64
        const val DEFAULT_ROWS = 88

        fun fromSolidGrid(cols: Int, rows: Int, solid: FloatArray): IdolWaterSurface {
            var cur = solid.copyOf()
            val next = FloatArray(solid.size)
            repeat(2) {
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        var acc = 0f
                        var wt = 0f
                        for (dr in -1..1) {
                            val rr = (r + dr).coerceIn(0, rows - 1)
                            val wy = if (dr == 0) 2f else 1f
                            for (dc in -1..1) {
                                val cc = (c + dc).coerceIn(0, cols - 1)
                                val w = wy * if (dc == 0) 2f else 1f
                                acc += cur[rr * cols + cc] * w
                                wt += w
                            }
                        }
                        next[r * cols + c] = acc / wt
                    }
                }
                cur = next.copyOf()
            }
            return IdolWaterSurface(cols, rows, solid, cur)
        }

        /**
         * Analytic contoured deity silhouette (mukut dome, flared shoulders, outstretched arms,
         * seated torso, wide lotus pedestal) used for JVM unit tests and safe fallback.
         */
        fun syntheticDome(cols: Int = DEFAULT_COLS, rows: Int = DEFAULT_ROWS, dualDeity: Boolean = false): IdolWaterSurface {
            val solid = FloatArray(cols * rows)
            val centers = if (dualDeity) floatArrayOf(0.30f, 0.70f) else floatArrayOf(0.50f)
            val widthScale = if (dualDeity) 0.56f else 1.0f
            for (r in 0 until rows) {
                val v = r.toFloat() / (rows - 1).toFloat()
                val halfW = when {
                    v < 0.03f -> 0.03f + (v / 0.03f) * 0.06f
                    v < 0.16f -> 0.09f + sin(((v - 0.03f) / 0.13f) * PI.toFloat() * 0.5f) * 0.07f
                    v < 0.25f -> 0.14f - ((v - 0.16f) / 0.09f) * 0.02f
                    v < 0.44f -> 0.12f + sin(((v - 0.25f) / 0.19f) * PI.toFloat() * 0.72f) * 0.24f
                    v < 0.56f -> 0.26f - ((v - 0.44f) / 0.12f) * 0.06f
                    v < 0.78f -> 0.20f + ((v - 0.56f) / 0.22f) * 0.20f
                    else -> 0.40f + sin(((v - 0.78f) / 0.22f) * PI.toFloat() * 0.5f) * 0.07f
                } * widthScale
                for (c in 0 until cols) {
                    val u = c.toFloat() / (cols - 1).toFloat()
                    var inside = false
                    for (cx in centers) {
                        if (abs(u - cx) <= halfW && v >= 0.01f) {
                            inside = true
                            break
                        }
                    }
                    solid[r * cols + c] = if (inside) 1f else 0f
                }
            }
            return fromSolidGrid(cols, rows, solid)
        }
    }
}

data class WaterDeflectionPoint(
    val point: TemplePoint,
    val normalX: Float,
    val normalY: Float,
    val arrivalProgress: Float,
    val strength: Float
)

data class WaterRivulet(
    val points: List<TemplePoint>,
    val arrivals: FloatArray,
    val isOuter: Boolean,
    val widthScale: Float,
    val shimmerOffset: Float
)

data class DropletSample(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val bounced: Boolean
)

data class DeflectedDroplet(
    val samples: List<DropletSample>,
    val durationProgress: Float,
    val phaseOffset: Float,
    val minArrivalProgress: Float,
    val maxEmitProgress: Float,
    val radiusScale: Float,
    val bounceCount: Int
) {
    /** Smoothly samples the droplet's position and velocity at normalized lifetime t in [0, 1]. */
    fun sampleAt(localT: Float): DropletSample {
        if (samples.size <= 1) return samples.first()
        val scaled = localT.coerceIn(0f, 1f) * (samples.size - 1)
        val i0 = floor(scaled).toInt().coerceIn(0, samples.size - 2)
        val f = scaled - i0
        val a = samples[i0]
        val b = samples[i0 + 1]
        return DropletSample(
            x = a.x + (b.x - a.x) * f,
            y = a.y + (b.y - a.y) * f,
            vx = a.vx + (b.vx - a.vx) * f,
            vy = a.vy + (b.vy - a.vy) * f,
            bounced = a.bounced || b.bounced
        )
    }
}

/** Delicate residual water bead anchored strictly on a bathed deity's solid idol surface. */
data class IdolResidualBead(
    val start: TemplePoint,
    val end: TemplePoint,
    val radiusScale: Float,
    val arrivalProgress: Float,
    val slidePhase: Float,
    val deity: Int
)

data class AbhishekWaterPlan(
    val impact: TemplePoint,
    val pedestalLeft: Float,
    val pedestalRight: Float,
    val pedestalY: Float,
    val domainMinX: Float,
    val domainMaxX: Float,
    val deflections: List<WaterDeflectionPoint>,
    val rivulets: List<WaterRivulet>,
    val droplets: List<DeflectedDroplet>,
    val residualBeads: List<IdolResidualBead>
)

/**
 * Deterministic, physically grounded Abhishek water simulation.
 * Computes gravity-curved pour trajectories, contour-hugging rivulets with overhang freefall,
 * multi-bounce idol surface deflections, and strictly idol-confined wet beads.
 */
object AbhishekWaterPhysics {
    const val STREAM_REACHES_CROWN_PROGRESS = 0.205f
    const val STREAM_LEAVES_CROWN_PROGRESS = 0.835f
    private const val GRAVITY = 0.86f
    private const val WATER_RESTITUTION = 0.34f
    private const val BOUNCE_DAMPING = 0.74f

    /**
     * Exact ballistic gravity parabola connecting the copper lota's tilted outer lip to the idol crown.
     * Linear in X (constant horizontal exit speed from tilted lip), quadratic in Y (downward gravity).
     */
    fun streamPoint(lipOuter: TemplePoint, impact: TemplePoint, t: Float): TemplePoint {
        val s = t.coerceIn(0f, 1f)
        val x = lipOuter.x + (impact.x - lipOuter.x) * s
        val y = lipOuter.y + (impact.y - lipOuter.y) * (0.34f * s + 0.66f * s * s)
        return TemplePoint(x, y)
    }

    /** Normalized [minU, maxU] slice belonging to [deity] inside the idol image. */
    fun deityDomainU(placement: IdolPlacement, deity: Int): Pair<Float, Float> {
        if (placement.heads.size <= 1) return 0f to 1f
        val u0 = ((placement.heads[0].x - placement.left) / placement.width).coerceIn(0f, 1f)
        val u1 = ((placement.heads[1].x - placement.left) / placement.width).coerceIn(0f, 1f)
        val split = ((u0 + u1) * 0.5f).coerceIn(0.35f, 0.65f)
        return if (deity == 0) (0f to split) else (split to 1f)
    }

    fun buildPlan(placement: IdolPlacement, deity: Int, surface: IdolWaterSurface): AbhishekWaterPlan {
        val safeDeity = deity.coerceIn(0, (placement.crowns.size - 1).coerceAtLeast(0))
        val rawCrown = placement.crowns[safeDeity]
        val head = placement.heads[safeDeity]
        val (minU, maxU) = deityDomainU(placement, safeDeity)
        val crownU = ((rawCrown.x - placement.left) / placement.width).coerceIn(minU, maxU)
        val crownV = ((rawCrown.y - placement.top) / placement.height).coerceIn(0f, 0.5f)
        val snappedImpactV = surface.impactV(crownU, crownV, minU, maxU)
        val impact = TemplePoint(rawCrown.x, placement.top + snappedImpactV * placement.height)

        val domainMinX = placement.left + minU * placement.width
        val domainMaxX = placement.left + maxU * placement.width
        val (pedLeftU, pedRightU) = surface.pedestalSpan(crownU, minU, maxU)
        val pedestalLeft = placement.left + pedLeftU * placement.width
        val pedestalRight = placement.left + pedRightU * placement.width
        val pedestalY = placement.bottom

        val rawDeflections = mutableListOf<WaterDeflectionPoint>()
        rawDeflections.add(WaterDeflectionPoint(impact, 0f, -1f, STREAM_REACHES_CROWN_PROGRESS, 1f))

        val rivulets = mutableListOf<WaterRivulet>()
        // 1. Two outer left & two outer right contour-hugging cascades (with overhang free-fall & re-impact deflection).
        val outerSpecs = listOf(
            Triple(-1, 0.002f, 1.00f),
            Triple(-1, 0.011f, 0.78f),
            Triple(1, 0.002f, 1.00f),
            Triple(1, 0.011f, 0.78f)
        )
        outerSpecs.forEachIndexed { idx, (side, insetFrac, widthScale) ->
            val rivulet = traceOuterRivulet(
                placement = placement,
                surface = surface,
                impact = impact,
                side = side,
                insetWorld = placement.width * insetFrac,
                minU = minU,
                maxU = maxU,
                widthScale = widthScale,
                shimmerOffset = idx * 0.23f,
                deflectionsOut = if (insetFrac < 0.005f) rawDeflections else null
            )
            rivulets.add(rivulet)
        }

        // 2. Six inner sculptural rivulets gliding across shoulders, garlands, drapes & pedestal while parting around the face.
        val innerBiases = floatArrayOf(-0.62f, -0.36f, -0.16f, 0.16f, 0.36f, 0.62f)
        innerBiases.forEachIndexed { idx, bias ->
            val rivulet = traceInnerRivulet(
                placement = placement,
                surface = surface,
                impact = impact,
                head = head,
                bias = bias,
                minU = minU,
                maxU = maxU,
                shimmerOffset = 0.11f + idx * 0.17f
            )
            rivulets.add(rivulet)
        }

        // 3. Filter well-spaced primary & secondary deflection ledges (crown + up to 8 distinct idol ledges).
        val curatedDeflections = selectDistinctDeflections(rawDeflections, placement)

        // 4. Precompute physically simulated multi-bounce ballistic droplets strictly bounded around this deity.
        val droplets = simulateDeflectedDroplets(
            placement = placement,
            surface = surface,
            deflections = curatedDeflections,
            domainMinX = domainMinX,
            domainMaxX = domainMaxX,
            deitySeed = safeDeity * 131 + (crownU * 1000f).toInt()
        )

        // 5. Generate delicate residual glistening beads strictly inside this deity's solid idol silhouette.
        val residualBeads = buildIdolResidualBeads(
            placement = placement,
            surface = surface,
            deity = safeDeity,
            head = head,
            impact = impact,
            minU = minU,
            maxU = maxU
        )

        return AbhishekWaterPlan(
            impact = impact,
            pedestalLeft = pedestalLeft,
            pedestalRight = pedestalRight,
            pedestalY = pedestalY,
            domainMinX = domainMinX,
            domainMaxX = domainMaxX,
            deflections = curatedDeflections,
            rivulets = rivulets,
            droplets = droplets,
            residualBeads = residualBeads
        )
    }

    private fun toU(placement: IdolPlacement, x: Float) = (x - placement.left) / placement.width
    private fun toV(placement: IdolPlacement, y: Float) = (y - placement.top) / placement.height
    private fun isSolidWorld(placement: IdolPlacement, surface: IdolWaterSurface, x: Float, y: Float): Boolean =
        surface.isSolid(toU(placement, x), toV(placement, y))

    private fun traceOuterRivulet(
        placement: IdolPlacement,
        surface: IdolWaterSurface,
        impact: TemplePoint,
        side: Int,
        insetWorld: Float,
        minU: Float,
        maxU: Float,
        widthScale: Float,
        shimmerOffset: Float,
        deflectionsOut: MutableList<WaterDeflectionPoint>?
    ): WaterRivulet {
        val pts = mutableListOf<TemplePoint>()
        val times = mutableListOf<Float>()
        val stepY = (placement.height / 76f).coerceIn(0.003f, 0.007f)
        val totalDrop = (placement.bottom - impact.y).coerceAtLeast(0.08f)

        var x = impact.x + side * 0.004f
        var y = impact.y + 0.001f
        var vx = side * 0.018f
        var vy = 0.08f
        var airborne = false
        var seamDetached = false
        var elapsed = 0f

        pts.add(TemplePoint(impact.x, impact.y))
        times.add(STREAM_REACHES_CROWN_PROGRESS)

        for (step in 0 until 130) {
            if (y >= placement.bottom - 0.002f) break
            val progressFrac = ((y - impact.y) / totalDrop).coerceIn(0f, 1f)
            val arrival = STREAM_REACHES_CROWN_PROGRESS + progressFrac * 0.21f

            if (!airborne) {
                val nextY = (y + stepY).coerceAtMost(placement.bottom)
                val edge = surface.connectedEdge(toU(placement, x), toV(placement, nextY), side, minU, maxU)
                if (edge == null) {
                    airborne = true
                    vx = side * 0.016f
                    vy = 0.09f
                } else if (edge.clampedAtSharedSeam) {
                    // Shared dual-deity base seam: stay softly tucked inside this deity instead of gluing to a vertical seam.
                    seamDetached = true
                    val tuckX = placement.left + edge.u * placement.width - side * (placement.width * 0.032f + insetWorld)
                    x += (tuckX - x) * 0.35f
                    y = nextY
                } else {
                    val edgeWorldX = placement.left + edge.u * placement.width
                    val targetX = if (seamDetached) {
                        edgeWorldX - side * (placement.width * 0.024f + insetWorld)
                    } else {
                        edgeWorldX - side * insetWorld
                    }
                    val dxEdge = targetX - x
                    // Sharp inward retreat under an ear, elbow, hand, or lotus petal => detach under gravity!
                    if (side * dxEdge < -placement.width * 0.032f && progressFrac in 0.08f..0.90f) {
                        airborne = true
                        vx = side * 0.022f
                        vy = 0.07f
                        deflectionsOut?.add(
                            WaterDeflectionPoint(
                                point = TemplePoint(x, y),
                                normalX = side * 0.76f,
                                normalY = -0.65f,
                                arrivalProgress = arrival,
                                strength = 0.85f
                            )
                        )
                    } else {
                        // Significant outward widening => real shoulder/arm/knee/pedestal deflection ledge!
                        if (side * dxEdge > placement.width * 0.016f && progressFrac in 0.06f..0.92f) {
                            val (nx, ny) = surface.normalAt(edge.u, toV(placement, nextY), placement.width, placement.height)
                            deflectionsOut?.add(
                                WaterDeflectionPoint(
                                    point = TemplePoint(edgeWorldX, nextY),
                                    normalX = if (abs(nx) < 0.2f) side * 0.65f else nx,
                                    normalY = ny.coerceAtMost(-0.28f),
                                    arrivalProgress = arrival,
                                    strength = (abs(dxEdge) / (placement.width * 0.04f)).coerceIn(0.55f, 1.0f)
                                )
                            )
                        }
                        x += dxEdge * 0.52f
                        y = nextY
                    }
                }
            } else {
                val dt = 0.014f
                vy += GRAVITY * dt
                val nx = x + vx * dt
                val ny = (y + vy * dt).coerceAtMost(placement.bottom)
                val u = toU(placement, nx)
                val v = toV(placement, ny)
                if (u in minU..maxU && surface.isSolid(u, v)) {
                    val (normX, normY) = surface.normalAt(u, v, placement.width, placement.height)
                    deflectionsOut?.add(
                        WaterDeflectionPoint(
                            point = TemplePoint(nx, ny),
                            normalX = if (abs(normX) < 0.2f) side * 0.68f else normX,
                            normalY = normY.coerceAtMost(-0.35f),
                            arrivalProgress = arrival,
                            strength = 0.92f
                        )
                    )
                    airborne = false
                }
                x = nx
                y = ny
            }

            elapsed += stepY
            if (pts.isEmpty() || hypot(x - pts.last().x, y - pts.last().y) >= stepY * 0.8f) {
                pts.add(TemplePoint(x, y))
                times.add(arrival)
            }
        }
        if (pts.last().y < placement.bottom) {
            pts.add(TemplePoint(pts.last().x, placement.bottom))
            times.add(STREAM_REACHES_CROWN_PROGRESS + 0.22f)
        }
        return WaterRivulet(pts, times.toFloatArray(), isOuter = true, widthScale = widthScale, shimmerOffset = shimmerOffset)
    }

    private fun traceInnerRivulet(
        placement: IdolPlacement,
        surface: IdolWaterSurface,
        impact: TemplePoint,
        head: TemplePoint,
        bias: Float,
        minU: Float,
        maxU: Float,
        shimmerOffset: Float
    ): WaterRivulet {
        val pts = mutableListOf<TemplePoint>()
        val times = mutableListOf<Float>()
        val side = if (bias < 0f) -1 else 1
        val samples = 36
        val faceCenterY = head.y + placement.height * 0.055f
        val faceRadiusX = placement.width * (if (placement.heads.size > 1) 0.072f else 0.110f)
        val faceRadiusY = placement.height * 0.088f
        val totalDrop = (placement.bottom - impact.y).coerceAtLeast(0.08f)

        var x = impact.x
        for (i in 0..samples) {
            val frac = i.toFloat() / samples.toFloat()
            val y = impact.y + totalDrop * frac
            val v = toV(placement, y)
            val leftEdge = surface.connectedEdge(toU(placement, impact.x), v, -1, minU, maxU)?.u ?: minU
            val rightEdge = surface.connectedEdge(toU(placement, impact.x), v, 1, minU, maxU)?.u ?: maxU
            val rawLeftWorld = placement.left + leftEdge * placement.width
            val rawRightWorld = placement.left + rightEdge * placement.width
            val leftWorld = min(rawLeftWorld, rawRightWorld)
            val rightWorld = max(rawLeftWorld, rawRightWorld)
            val midWorld = (leftWorld + rightWorld) * 0.5f
            val halfSpan = ((rightWorld - leftWorld) * 0.44f).coerceAtLeast(0.008f)

            val spreadEnv = sin(min(1f, frac * 3.2f) * PI.toFloat() * 0.5f)
            var targetX = midWorld + bias * halfSpan * spreadEnv +
                sin(frac * 11f + bias * 5f) * placement.width * 0.006f

            // Gently part around the deity's face so sacred facial expression stays clear and serene.
            val dyFace = (y - faceCenterY) / faceRadiusY
            if (abs(dyFace) < 1.15f) {
                val pushEnv = cos((dyFace / 1.15f).coerceIn(-1f, 1f) * PI.toFloat() * 0.5f)
                val minClearX = head.x + side * faceRadiusX * (0.72f + 0.28f * abs(bias)) * pushEnv
                if (side < 0 && targetX > minClearX && abs(targetX - head.x) < faceRadiusX * 1.15f) {
                    targetX = min(targetX, minClearX)
                } else if (side > 0 && targetX < minClearX && abs(targetX - head.x) < faceRadiusX * 1.15f) {
                    targetX = max(targetX, minClearX)
                }
            }

            // Clamp safely inside the idol span (including pointed 1-pixel crown finials where leftWorld ~= rightWorld).
            val inset = min(placement.width * 0.01f, (rightWorld - leftWorld) * 0.45f).coerceAtLeast(0f)
            val clampMin = leftWorld + inset
            val clampMax = (rightWorld - inset).coerceAtLeast(clampMin)
            targetX = targetX.coerceIn(clampMin, clampMax)
            x = if (i == 0) impact.x else (x + (targetX - x) * 0.42f)
            pts.add(TemplePoint(x, y))
            times.add(STREAM_REACHES_CROWN_PROGRESS + frac * (0.20f + 0.04f * abs(bias)))
        }
        return WaterRivulet(pts, times.toFloatArray(), isOuter = false, widthScale = 0.65f, shimmerOffset = shimmerOffset)
    }

    private fun selectDistinctDeflections(
        raw: List<WaterDeflectionPoint>,
        placement: IdolPlacement
    ): List<WaterDeflectionPoint> {
        if (raw.isEmpty()) return emptyList()
        val kept = mutableListOf(raw.first())
        val minSep = (placement.height * 0.095f).coerceAtLeast(0.032f)
        val candidates = raw.drop(1).sortedByDescending { it.strength }
        for (cand in candidates) {
            if (kept.size >= 9) break
            val farEnough = kept.all { existing ->
                hypot(cand.point.x - existing.point.x, cand.point.y - existing.point.y) >= minSep
            }
            if (farEnough) kept.add(cand)
        }
        return kept.sortedBy { it.arrivalProgress }
    }

    private fun simulateDeflectedDroplets(
        placement: IdolPlacement,
        surface: IdolWaterSurface,
        deflections: List<WaterDeflectionPoint>,
        domainMinX: Float,
        domainMaxX: Float,
        deitySeed: Int
    ): List<DeflectedDroplet> {
        val rng = Random(4099 + deitySeed)
        val droplets = mutableListOf<DeflectedDroplet>()
        // Strict horizontal leash around the idol so deflected drops NEVER fly randomly across the screen!
        val leashMargin = (placement.width * 0.14f).coerceIn(0.035f, 0.075f)
        val minAllowedX = domainMinX - leashMargin
        val maxAllowedX = domainMaxX + leashMargin
        val minAllowedY = (deflections.firstOrNull()?.point?.y ?: placement.top) - 0.045f

        deflections.forEachIndexed { idx, defl ->
            val isCrown = idx == 0
            val count = if (isCrown) 16 else 4
            repeat(count) { k ->
                val side = if (isCrown) {
                    if (k % 2 == 0) -1f else 1f
                } else {
                    if (defl.normalX >= 0f) 1f else -1f
                }
                // Calibrated gentle pour-deflection velocities (higher crisp spray at crown, gentler cascades off ledges).
                val speed = if (isCrown) {
                    0.068f + rng.nextFloat() * 0.078f
                } else {
                    0.038f + rng.nextFloat() * 0.048f
                }
                val spreadAngle = if (isCrown) {
                    // Fan outward & slightly upward off the curved mukut dome
                    (0.18f + rng.nextFloat() * 0.58f)
                } else {
                    (0.10f + rng.nextFloat() * 0.35f)
                }
                var vx = if (isCrown) {
                    side * speed * cos(spreadAngle)
                } else {
                    (defl.normalX * 0.65f + side * 0.35f) * speed
                }
                var vy = if (isCrown) {
                    -speed * sin(spreadAngle) * 0.85f - 0.015f
                } else {
                    (defl.normalY * 0.55f - 0.15f) * speed
                }

                var bx = defl.point.x + (if (isCrown) side * 0.002f else defl.normalX * 0.003f)
                var by = defl.point.y + (if (isCrown) -0.001f else defl.normalY * 0.003f)
                var bounces = 0
                var wasSolid = isSolidWorld(placement, surface, bx, by)
                val samples = mutableListOf(DropletSample(bx, by, vx, vy, bounced = false))
                val dt = 0.015f
                val maxSteps = if (isCrown) 34 else 28

                for (step in 1..maxSteps) {
                    vy += GRAVITY * dt
                    val nx = (bx + vx * dt).coerceIn(minAllowedX, maxAllowedX)
                    val ny = (by + vy * dt).coerceAtLeast(minAllowedY)
                    var bouncedHere = false

                    if (ny >= placement.bottom) {
                        // Gentle low micro-bounce off the altar floor at the pedestal base.
                        by = placement.bottom
                        bx = nx
                        if (bounces < 2 && abs(vy) > 0.05f) {
                            vy = -abs(vy) * 0.24f
                            vx *= 0.55f
                            bounces++
                            bouncedHere = true
                        } else {
                            vx *= 0.4f
                            vy = 0f
                        }
                        wasSolid = false
                        samples.add(DropletSample(bx, by, vx, vy, bouncedHere))
                        if (vy == 0f) break
                    } else {
                        val nowSolid = isSolidWorld(placement, surface, nx, ny)
                        if (!wasSolid && nowSolid && bounces < 2) {
                            // True physical deflection off the idol's real outward surface normal!
                            val (normX, normY) = surface.normalAt(
                                toU(placement, nx),
                                toV(placement, ny),
                                placement.width,
                                placement.height
                            )
                            val vn = vx * normX + vy * normY
                            if (vn < 0f) {
                                vx = (vx - (1f + WATER_RESTITUTION) * vn * normX) * BOUNCE_DAMPING
                                vy = (vy - (1f + WATER_RESTITUTION) * vn * normY) * BOUNCE_DAMPING
                            } else {
                                vx = (vx + normX * 0.03f) * BOUNCE_DAMPING
                                vy *= 0.8f
                            }
                            bx = (nx + normX * 0.004f).coerceIn(minAllowedX, maxAllowedX)
                            by = (ny + normY * 0.004f).coerceAtLeast(minAllowedY)
                            bounces++
                            bouncedHere = true
                            wasSolid = isSolidWorld(placement, surface, bx, by)
                        } else {
                            bx = nx
                            by = ny
                            wasSolid = nowSolid
                        }
                        samples.add(DropletSample(bx, by, vx, vy, bouncedHere))
                    }
                }

                val duration = (samples.size * dt * 0.42f).coerceIn(0.11f, 0.22f)
                val arrivalOffset = defl.arrivalProgress
                val maxEmit = (STREAM_LEAVES_CROWN_PROGRESS + (arrivalOffset - STREAM_REACHES_CROWN_PROGRESS) * 0.7f)
                    .coerceAtMost(0.89f)
                droplets.add(
                    DeflectedDroplet(
                        samples = samples,
                        durationProgress = duration,
                        phaseOffset = (k.toFloat() / count.toFloat()) * duration + rng.nextFloat() * 0.025f,
                        minArrivalProgress = arrivalOffset,
                        maxEmitProgress = maxEmit,
                        radiusScale = if (isCrown) (0.78f + rng.nextFloat() * 0.48f) else (0.64f + rng.nextFloat() * 0.42f),
                        bounceCount = bounces
                    )
                )
            }
        }
        return droplets
    }

    /**
     * Generates delicate residual water beads strictly ON the solid idol silhouette
     * (never floating in empty screen air, never over walls/ceiling/thali, never cluttering the face).
     */
    private fun buildIdolResidualBeads(
        placement: IdolPlacement,
        surface: IdolWaterSurface,
        deity: Int,
        head: TemplePoint,
        impact: TemplePoint,
        minU: Float,
        maxU: Float
    ): List<IdolResidualBead> {
        val rng = Random(9137 + deity * 193)
        val beads = mutableListOf<IdolResidualBead>()
        val impactV = toV(placement, impact.y).coerceIn(0f, 0.4f)
        val faceU = toU(placement, head.x)
        val faceV = toV(placement, head.y) + 0.055f
        val faceExcludeU = if (placement.heads.size > 1) 0.075f else 0.115f
        val faceExcludeV = 0.095f

        var attempts = 0
        while (beads.size < 20 && attempts < 280) {
            attempts++
            val u = minU + rng.nextFloat() * (maxU - minU)
            val v = impactV + 0.02f + rng.nextFloat() * (0.94f - impactV).coerceAtLeast(0.1f)
            if (!surface.isSolid(u, v)) continue
            // Keep beads clear of the immediate eyes/nose/mouth so the deity's face remains crisp.
            if (abs(u - faceU) < faceExcludeU && abs(v - faceV) < faceExcludeV) continue
            // Verify interior stability (avoid 1px stray alpha specks).
            val slideDv = 0.022f + rng.nextFloat() * 0.038f
            val endV = (v + slideDv).coerceAtMost(0.97f)
            val endU = if (surface.isSolid(u, endV)) u else {
                val (nx, _) = surface.normalAt(u, v, placement.width, placement.height)
                (u - nx * 0.015f).coerceIn(minU, maxU)
            }
            if (!surface.isSolid(endU, endV)) continue

            val startPt = TemplePoint(placement.left + u * placement.width, placement.top + v * placement.height)
            // Keep beads nicely spaced so they never clump.
            if (beads.any { hypot(it.start.x - startPt.x, it.start.y - startPt.y) < placement.height * 0.055f }) continue

            val endPt = TemplePoint(placement.left + endU * placement.width, placement.top + endV * placement.height)
            val verticalFrac = ((v - impactV) / (1f - impactV).coerceAtLeast(0.1f)).coerceIn(0f, 1f)
            beads.add(
                IdolResidualBead(
                    start = startPt,
                    end = endPt,
                    radiusScale = 0.65f + rng.nextFloat() * 0.65f,
                    arrivalProgress = (STREAM_REACHES_CROWN_PROGRESS + verticalFrac * 0.26f + rng.nextFloat() * 0.18f)
                        .coerceIn(0.22f, 0.78f),
                    slidePhase = rng.nextFloat(),
                    deity = deity
                )
            )
        }
        return beads
    }
}
