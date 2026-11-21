package com.worship.nityamandir.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.worship.nityamandir.engine.*
import kotlin.math.*

/**
 * Physically grounded Abhishek water overlay:
 * - Zero-gap meniscus & gravity-curved pour stream locked directly to the copper lota's tilted lip,
 * - Real contour-hugging rivulets with overhang detachment & re-impact deflection across the idol,
 * - Multi-bounce ballistic water droplets deflecting off the idol's actual silhouette normals,
 * - Gentle concentric altar ripples right at the deity's lotus pedestal base.
 */
@Composable
fun WaterFlowOverlay(
    deity: Int?,
    progress: Float,
    placement: IdolPlacement,
    surface: IdolWaterSurface,
    lotaRestCenter: TemplePoint,
    lotaAspect: Float = TempleSceneLayout.WATER_LOTA_DEFAULT_ASPECT,
    modifier: Modifier = Modifier
) {
    if (deity == null) return
    val plan = remember(placement, deity, surface) {
        AbhishekWaterPhysics.buildPlan(placement, deity, surface)
    }

    Canvas(modifier.fillMaxSize()) {
        if (size.width <= 1f || size.height <= 1f) return@Canvas
        val viewport = TempleViewport(size.width, size.height)
        val u = viewport.imageWidth
        fun at(p: TemplePoint): Offset = viewport.pixel(p).let { Offset(it.x, it.y) }

        val p = progress.coerceIn(0f, 1f)
        val source = placement.bathSource(deity)
        val pourCenter = TempleSceneLayout.waterLotaPourCenter(
            source = source,
            width = TempleSceneLayout.WATER_LOTA_POUR_WIDTH,
            aspect = lotaAspect
        )
        val lotaCenter = TempleSceneLayout.waterLotaPosition(lotaRestCenter, pourCenter, p)
        val lotaWidth = TempleSceneLayout.waterLotaWidth(p)
        val lotaTilt = TempleSceneLayout.waterLotaTilt(p)
        val lipInner = at(TempleSceneLayout.waterLotaInnerLip(lotaCenter, lotaWidth, lotaAspect, lotaTilt))
        val lipOuterWorld = TempleSceneLayout.waterLotaLip(lotaCenter, lotaWidth, lotaAspect, lotaTilt)
        val pourLipOuterWorld = TempleSceneLayout.waterLotaLip(
            pourCenter,
            TempleSceneLayout.WATER_LOTA_POUR_WIDTH,
            lotaAspect,
            tilt = 1f
        )
        val lipOuter = at(lipOuterWorld)
        val impact = at(plan.impact)

        val streamHead = TempleSceneLayout.waterStreamHead(p)
        val streamTail = TempleSceneLayout.waterStreamTail(p)
        val pour = TempleSceneLayout.waterPourIntensity(p)
        val streamActive = streamHead > streamTail + 0.008f
        // Once detached from the rising lip, the falling ribbon keeps its ballistic pour trajectory.
        val streamOriginWorld = if (streamTail > 0f) pourLipOuterWorld else lipOuterWorld
        val streamOriginPx = at(streamOriginWorld)

        // 1. Seamless rim-overflow meniscus + continuous gravity-curved pour stream from lota lip to crown.
        if (streamActive) {
            val lipAttached = (1f - streamTail / 0.10f).coerceIn(0f, 1f)
            val flowThickness = (0.45f + 0.55f * pour).coerceIn(0.25f, 1f)

            if (lipAttached > 0.02f) {
                // Translucent water sheet curling directly out of the flared copper mouth over the rolled rim.
                val meniscusMid = Offset(
                    lipInner.x * 0.45f + lipOuter.x * 0.55f,
                    lipInner.y * 0.45f + lipOuter.y * 0.55f - u * 0.0014f
                )
                val meniscusPath = Path().apply {
                    moveTo(lipInner.x, lipInner.y)
                    quadraticTo(meniscusMid.x, meniscusMid.y, lipOuter.x, lipOuter.y)
                }
                drawPath(
                    path = meniscusPath,
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFFD5E9EC).copy(alpha = 0.24f * lipAttached * flowThickness),
                            Color(0xFFF3FAFB).copy(alpha = 0.72f * lipAttached * flowThickness),
                            Color(0xFFFFFDF8).copy(alpha = 0.80f * lipAttached * flowThickness)
                        ),
                        start = lipInner,
                        end = lipOuter
                    ),
                    style = Stroke(width = u * 0.0082f * flowThickness, cap = StrokeCap.Round)
                )
                drawPath(
                    path = meniscusPath,
                    color = Color(0xFFFFFDF8).copy(alpha = 0.62f * lipAttached * flowThickness),
                    style = Stroke(width = u * 0.0020f * flowThickness, cap = StrokeCap.Round)
                )
            }

            fun streamCenterPx(t: Float): Offset {
                val base = at(AbhishekWaterPhysics.streamPoint(streamOriginWorld, plan.impact, t))
                val wobble = u * 0.00055f * sin(t * 16f - p * 28f) * sin(PI.toFloat() * t)
                return Offset(base.x + wobble, base.y)
            }

            fun streamRadius(t: Float): Float {
                val headTaper = ((streamHead - t) / 0.14f).coerceIn(0.22f, 1f)
                val tailTaper = if (streamTail > 0f) ((t - streamTail) / 0.16f).coerceIn(0.18f, 1f) else 1f
                val ripple = 1f + 0.11f * sin(t * 25f - p * 54f)
                return u * (0.0044f - 0.0015f * t) * ripple * flowThickness * headTaper * tailTaper
            }

            val segments = 32
            val leftPts = ArrayList<Offset>(segments + 1)
            val rightPts = ArrayList<Offset>(segments + 1)
            val centerPts = ArrayList<Offset>(segments + 1)
            for (i in 0..segments) {
                val frac = i.toFloat() / segments.toFloat()
                val t = streamTail + (streamHead - streamTail) * frac
                val cPt = streamCenterPx(t)
                val nextPt = streamCenterPx((t + 0.015f).coerceAtMost(1f))
                val tx = nextPt.x - cPt.x
                val ty = nextPt.y - cPt.y
                val tLen = hypot(tx, ty).coerceAtLeast(1e-3f)
                // Unit perpendicular to stream tangent
                val perpX = -ty / tLen
                val perpY = tx / tLen
                val r = streamRadius(t)
                centerPts.add(cPt)
                leftPts.add(Offset(cPt.x - perpX * r, cPt.y - perpY * r))
                rightPts.add(Offset(cPt.x + perpX * r, cPt.y + perpY * r))
            }

            val streamPath = Path().apply {
                moveTo(leftPts.first().x, leftPts.first().y)
                for (i in 1..segments) lineTo(leftPts[i].x, leftPts[i].y)
                for (i in segments downTo 0) lineTo(rightPts[i].x, rightPts[i].y)
                close()
            }
            val streamAlpha = (0.38f + 0.62f * pour).coerceIn(0f, 1f) * (1f - streamTail * 0.35f)
            val gradMinX = min(streamOriginPx.x, impact.x) - u * 0.006f
            val gradMaxX = max(streamOriginPx.x, impact.x) + u * 0.006f
            // Subtle refractive outer edge keeps the clear water stream crisp against light marble shrines too.
            drawPath(
                path = streamPath,
                color = Color(0xFF53757E).copy(alpha = 0.18f * streamAlpha),
                style = Stroke(width = u * 0.0016f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = streamPath,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFFE7F4F6).copy(alpha = 0.68f * streamAlpha),
                        Color(0xFFA9CFD6).copy(alpha = 0.26f * streamAlpha),
                        Color(0xFFFFFDF8).copy(alpha = 0.84f * streamAlpha)
                    ),
                    startX = gradMinX,
                    endX = gradMaxX
                )
            )

            // Crisp twin refractive glass edges catching warm Garbhagriha diya light
            for (sideFactor in floatArrayOf(-0.76f, 0.72f)) {
                val rimPath = Path().apply {
                    for (i in 0..segments) {
                        val c = centerPts[i]
                        val edgePt = if (sideFactor < 0f) leftPts[i] else rightPts[i]
                        val px = c.x + (edgePt.x - c.x) * abs(sideFactor)
                        val py = c.y + (edgePt.y - c.y) * abs(sideFactor)
                        if (i == 0) moveTo(px, py) else lineTo(px, py)
                    }
                }
                val edgeColor = if (sideFactor > 0f) Color(0xFFFFFDF8) else Color(0xFFE6F4F6)
                drawPath(
                    path = rimPath,
                    color = edgeColor.copy(alpha = 0.56f * streamAlpha),
                    style = Stroke(width = u * 0.00095f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // Downward-racing specular glints inside the water column
            repeat(7) { i ->
                val t = (p * 4.2f + i / 7f) % 1f
                if (t in streamTail..streamHead) {
                    val c = streamCenterPx(t)
                    val r = streamRadius(t)
                    drawOval(
                        color = Color(0xFFFFFDF8).copy(alpha = 0.38f * streamAlpha),
                        topLeft = Offset(c.x - r * 0.45f, c.y - u * 0.0024f),
                        size = Size(r * 0.90f, u * 0.0048f)
                    )
                }
            }
        }

        // 2. Crown impact collar & gentle mukut deflection sheet once stream reaches the crown.
        val crownHit = ((p - (AbhishekWaterPhysics.STREAM_REACHES_CROWN_PROGRESS - 0.012f)) / 0.035f).coerceIn(0f, 1f)
        val crownRelease = ((AbhishekWaterPhysics.STREAM_LEAVES_CROWN_PROGRESS + 0.03f - p) / 0.055f).coerceIn(0f, 1f)
        val crownSplash = min(crownHit, crownRelease)
        if (crownSplash > 0.01f) {
            val collarRx = u * (0.011f + 0.003f * sin(p * 42f))
            val collarRy = u * 0.0045f
            drawOval(
                brush = Brush.radialGradient(
                    listOf(
                        Color(0xFFFFFDF8).copy(alpha = 0.62f * crownSplash),
                        Color(0xFFDFF4F6).copy(alpha = 0.30f * crownSplash),
                        Color.Transparent
                    ),
                    center = impact,
                    radius = collarRx * 1.4f
                ),
                topLeft = Offset(impact.x - collarRx * 1.4f, impact.y - collarRy * 1.3f),
                size = Size(collarRx * 2.8f, collarRy * 2.6f)
            )
            // Delicate twin spray-wing filaments curving off the mukut dome
            for (side in floatArrayOf(-1f, 1f)) {
                val phase = (p * 5.2f + if (side > 0f) 0.35f else 0f) % 1f
                val arcLen = u * (0.018f + 0.011f * phase)
                val wing = Path().apply {
                    moveTo(impact.x, impact.y)
                    quadraticTo(
                        impact.x + side * arcLen * 0.55f,
                        impact.y - u * 0.012f * (1f - phase * 0.45f),
                        impact.x + side * arcLen,
                        impact.y + u * (0.008f + 0.016f * phase)
                    )
                }
                drawPath(
                    path = wing,
                    color = Color(0xFF4C6F78).copy(alpha = 0.22f * crownSplash * (1f - phase * 0.55f)),
                    style = Stroke(width = u * 0.0022f, cap = StrokeCap.Round)
                )
                drawPath(
                    path = wing,
                    color = Color(0xFFFFFDF8).copy(alpha = 0.56f * crownSplash * (1f - phase * 0.55f)),
                    style = Stroke(width = u * 0.0012f, cap = StrokeCap.Round)
                )
            }
        }

        // 3. Contour-hugging cascading rivulets with progressive arrival, top-down drain & sliding glints.
        val pourSpan = AbhishekWaterPhysics.STREAM_LEAVES_CROWN_PROGRESS - AbhishekWaterPhysics.STREAM_REACHES_CROWN_PROGRESS
        plan.rivulets.forEach { rivulet ->
            val pts = rivulet.points
            val arrivals = rivulet.arrivals
            if (pts.size < 2) return@forEach

            var firstVisible = -1
            var lastVisible = -1
            for (i in pts.indices) {
                val arr = arrivals[i]
                val drain = arr + pourSpan + 0.045f
                if (p >= arr && p <= drain) {
                    if (firstVisible < 0) firstVisible = i
                    lastVisible = i
                }
            }
            if (firstVisible < 0 || lastVisible <= firstVisible) return@forEach

            val drainFade = (1f - ((p - (arrivals[lastVisible] + pourSpan - 0.02f)) / 0.065f).coerceIn(0f, 1f))
            val baseAlpha = (if (rivulet.isOuter) 0.28f else 0.16f) * drainFade
            if (baseAlpha <= 0.005f) return@forEach

            val rivPath = Path()
            for (i in firstVisible..lastVisible) {
                val ptPx = at(pts[i])
                if (i == firstVisible) rivPath.moveTo(ptPx.x, ptPx.y) else rivPath.lineTo(ptPx.x, ptPx.y)
            }
            if (rivulet.isOuter) {
                drawPath(
                    path = rivPath,
                    color = Color(0xFF476972).copy(alpha = 0.16f * drainFade),
                    style = Stroke(
                        width = u * 0.0028f * rivulet.widthScale,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
            drawPath(
                path = rivPath,
                color = Color(0xFFE8F7F9).copy(alpha = baseAlpha),
                style = Stroke(
                    width = u * 0.0018f * rivulet.widthScale,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            // Soft warm-ivory sheen glints visibly sliding downward along the real idol contour
            val glintCount = if (rivulet.isOuter) 3 else 2
            repeat(glintCount) { g ->
                val glintFrac = (p * 2.8f + rivulet.shimmerOffset + g.toFloat() / glintCount) % 1f
                val idxF = glintFrac * (pts.size - 1)
                val i0 = floor(idxF).toInt().coerceIn(0, pts.size - 2)
                if (i0 in firstVisible until lastVisible) {
                    val f = idxF - i0
                    val aPx = at(pts[i0])
                    val bPx = at(pts[i0 + 1])
                    val gx = aPx.x + (bPx.x - aPx.x) * f
                    val gy = aPx.y + (bPx.y - aPx.y) * f
                    val angle = atan2(bPx.y - aPx.y, bPx.x - aPx.x) * (180f / PI.toFloat())
                    val glintLen = u * (if (rivulet.isOuter) 0.0074f else 0.0052f)
                    val glintThick = u * 0.0017f * rivulet.widthScale
                    rotate(angle, Offset(gx, gy)) {
                        drawOval(
                            color = Color(0xFFFFFDF8).copy(alpha = (if (rivulet.isOuter) 0.68f else 0.44f) * drainFade),
                            topLeft = Offset(gx - glintLen * 0.5f, gy - glintThick * 0.5f),
                            size = Size(glintLen, glintThick)
                        )
                    }
                }
            }
        }

        // 4. Subtle sparkling micro-splashes at real detected idol deflection ledges (ears, shoulders, arms, pedestal).
        plan.deflections.drop(1).forEachIndexed { idx, defl ->
            val startP = defl.arrivalProgress
            val endP = (startP + pourSpan + 0.03f).coerceAtMost(0.93f)
            val env = min(((p - startP) / 0.035f), ((endP - p) / 0.055f)).coerceIn(0f, 1f)
            if (env <= 0.01f) return@forEachIndexed
            val pulse = 0.65f + 0.35f * sin(p * 34f + idx * 2.1f)
            val ptPx = at(defl.point)
            val glowR = u * (0.0075f + 0.0030f * defl.strength) * pulse
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        Color(0xFFFFFDF8).copy(alpha = 0.55f * env * pulse),
                        Color(0xFFD2EEF2).copy(alpha = 0.24f * env),
                        Color.Transparent
                    ),
                    center = ptPx,
                    radius = glowR.coerceAtLeast(1f)
                ),
                radius = glowR,
                center = ptPx
            )
            // Tiny outward deflection mist streak along the idol's true surface normal
            val streakLen = u * 0.011f * defl.strength * pulse
            drawLine(
                color = Color(0xFF4C6E77).copy(alpha = 0.24f * env * pulse),
                start = ptPx,
                end = Offset(ptPx.x + defl.normalX * streakLen, ptPx.y + defl.normalY * streakLen),
                strokeWidth = u * 0.0019f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFFFFDF8).copy(alpha = 0.52f * env * pulse),
                start = ptPx,
                end = Offset(ptPx.x + defl.normalX * streakLen, ptPx.y + defl.normalY * streakLen),
                strokeWidth = u * 0.0011f,
                cap = StrokeCap.Round
            )
        }

        // 5. Physically simulated ballistic droplets deflecting off idol contours (strictly bounded around the idol).
        plan.droplets.forEach { droplet ->
            val firstLaunch = droplet.minArrivalProgress + droplet.phaseOffset
            if (p < firstLaunch) return@forEach
            val cycle = floor((p - firstLaunch) / droplet.durationProgress).toInt()
            if (cycle < 0) return@forEach
            val launchP = firstLaunch + cycle * droplet.durationProgress
            if (launchP > droplet.maxEmitProgress) return@forEach

            val localT = ((p - launchP) / droplet.durationProgress).coerceIn(0f, 1f)
            val fadeIn = (localT / 0.09f).coerceIn(0f, 1f)
            val fadeOut = ((1f - localT) / 0.24f).coerceIn(0f, 1f)
            val alpha = fadeIn * fadeOut
            if (alpha <= 0.02f) return@forEach

            val sample = droplet.sampleAt(localT)
            val pos = at(TemplePoint(sample.x, sample.y))
            val speed = hypot(sample.vx, sample.vy)
            val baseR = u * 0.00235f * droplet.radiusScale
            val stretch = (1.18f + speed * 2.8f).coerceIn(1.12f, 2.45f)
            val angleDeg = atan2(sample.vy, sample.vx) * (180f / PI.toFloat())

            rotate(angleDeg, pos) {
                val rx = baseR * stretch
                val ry = baseR * 0.88f
                // Subtle cool-glass refractive rim ensures deflected droplets read crisply on both light marble and dark shrines.
                drawOval(
                    color = Color(0xFF3E616A).copy(alpha = 0.34f * alpha),
                    topLeft = Offset(pos.x - rx * 1.08f, pos.y - ry * 1.05f + ry * 0.22f),
                    size = Size(rx * 2.16f, ry * 2.16f)
                )
                drawOval(
                    color = Color(0xFFDFF4F7).copy(alpha = 0.70f * alpha),
                    topLeft = Offset(pos.x - rx, pos.y - ry),
                    size = Size(rx * 2f, ry * 2f)
                )
                drawOval(
                    color = Color(0xFFFFFDF8).copy(alpha = 0.90f * alpha),
                    topLeft = Offset(pos.x - rx * 0.55f, pos.y - ry * 0.65f),
                    size = Size(rx * 0.98f, ry * 0.82f)
                )
            }
            if (sample.bounced) {
                drawCircle(
                    color = Color(0xFFFFFDF8).copy(alpha = 0.68f * alpha),
                    radius = baseR * 1.55f,
                    center = pos
                )
            }
        }

        // 6. Gentle concentric altar ripples & settling sheen spanning the deity's real lotus pedestal base.
        val baseArrival = AbhishekWaterPhysics.STREAM_REACHES_CROWN_PROGRESS + 0.16f
        val baseEnv = min(((p - baseArrival) / 0.06f), ((0.97f - p) / 0.10f)).coerceIn(0f, 1f)
        if (baseEnv > 0.01f) {
            val baseCenter = at(TemplePoint((plan.pedestalLeft + plan.pedestalRight) * 0.5f, plan.pedestalY))
            val pedHalfW = ((plan.pedestalRight - plan.pedestalLeft) * 0.5f * u).coerceAtLeast(u * 0.03f)
            repeat(4) { i ->
                val phase = (p * 2.4f + i * 0.25f) % 1f
                val rx = pedHalfW * (0.48f + 0.62f * phase)
                val ry = rx * 0.16f
                val ringAlpha = baseEnv * (1f - phase) * 0.44f
                drawOval(
                    color = Color(0xFF567880).copy(alpha = ringAlpha * 0.55f),
                    topLeft = Offset(baseCenter.x - rx, baseCenter.y - ry + u * 0.0009f),
                    size = Size(rx * 2f, ry * 2f),
                    style = Stroke(width = u * 0.0017f)
                )
                drawOval(
                    color = Color(0xFFEAF7F8).copy(alpha = ringAlpha),
                    topLeft = Offset(baseCenter.x - rx, baseCenter.y - ry),
                    size = Size(rx * 2f, ry * 2f),
                    style = Stroke(width = u * 0.0012f)
                )
            }
            // Subtle glossy wet sheen hugging the marble directly beneath the lotus base
            val sheenRx = pedHalfW * 0.95f
            val sheenRy = u * 0.008f
            drawOval(
                brush = Brush.radialGradient(
                    listOf(
                        Color(0xFFFFF8E8).copy(alpha = 0.16f * baseEnv),
                        Color(0xFFCDE9ED).copy(alpha = 0.10f * baseEnv),
                        Color.Transparent
                    ),
                    center = baseCenter,
                    radius = sheenRx
                ),
                topLeft = Offset(baseCenter.x - sheenRx, baseCenter.y - sheenRy),
                size = Size(sheenRx * 2f, sheenRy * 2f)
            )
        }
    }
}
