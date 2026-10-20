package com.worship.nityamandir.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as CanvasSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.worship.nityamandir.data.ShrineCatalog
import com.worship.nityamandir.data.ShrineSelection
import com.worship.nityamandir.engine.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

class FlowerFlight(val id: Long, val offering: FlowerOffering) {
    val progress = androidx.compose.animation.core.Animatable(0f)
}

@Composable
private fun rememberAssetImage(path: String): ImageBitmap = rememberCachedAssetImage(path)

@Composable
private fun GarlandLayer(
    bitmap: ImageBitmap,
    space: ShrineSpace,
    viewport: TempleViewport
) {
    Canvas(Modifier.fillMaxSize()) {
        val spec = space.garlandSpec
        val fLeft = spec[0]; val fTop = spec[1]; val fRight = spec[2]
        val fBottom = spec[3]; val pillarW = spec[4]; val topH = spec[5]
        val sw = bitmap.width
        val sh = bitmap.height
        val sideSrcW = (sw * 0.125f).toInt().coerceAtLeast(1)
        val topSrcX = (sw * 0.1065f).toInt()
        val topSrcW = (sw * (1f - 2f * 0.1065f)).toInt().coerceAtLeast(1)
        val topSrcH = (sh * 0.275f).toInt().coerceAtLeast(1)

        val u = viewport.imageWidth
        val topLeftPx = viewport.pixel(TemplePoint(fLeft, fTop))
        val rightPillarLeftPx = viewport.pixel(TemplePoint(fRight - pillarW, fTop))
        val sideDstW = (pillarW * u).toInt().coerceAtLeast(1)
        val sideDstH = ((fBottom - fTop) * u).toInt().coerceAtLeast(1)

        // 1. Left temple pillar strand
        drawImage(
            image = bitmap,
            srcOffset = IntOffset(0, 0),
            srcSize = IntSize(sideSrcW, sh),
            dstOffset = IntOffset(topLeftPx.x.toInt(), topLeftPx.y.toInt()),
            dstSize = IntSize(sideDstW, sideDstH)
        )
        // 2. Right temple pillar strand
        drawImage(
            image = bitmap,
            srcOffset = IntOffset(sw - sideSrcW, 0),
            srcSize = IntSize(sideSrcW, sh),
            dstOffset = IntOffset(rightPillarLeftPx.x.toInt(), rightPillarLeftPx.y.toInt()),
            dstSize = IntSize(sideDstW, sideDstH)
        )
        // 3. Upper temple arch toran swag
        val archLeft = fLeft + pillarW * 0.85f
        val archRight = fRight - pillarW * 0.85f
        val archTopLeftPx = viewport.pixel(TemplePoint(archLeft, fTop))
        val archDstW = ((archRight - archLeft) * u).toInt().coerceAtLeast(1)
        val archDstH = (topH * u).toInt().coerceAtLeast(1)
        drawImage(
            image = bitmap,
            srcOffset = IntOffset(topSrcX, 0),
            srcSize = IntSize(topSrcW, topSrcH),
            dstOffset = IntOffset(archTopLeftPx.x.toInt(), archTopLeftPx.y.toInt()),
            dstSize = IntSize(archDstW, archDstH)
        )
    }
}

@Composable
private fun SpriteLayer(
    bitmap: ImageBitmap,
    center: TemplePoint,
    widthFraction: Float,
    viewport: TempleViewport,
    density: Density,
    modifier: Modifier = Modifier,
    bottomAligned: Boolean = false,
    fitLongestSide: Boolean = false,
    rotationZ: Float = 0f,
    scale: Float = 1f,
    alpha: Float = 1f,
    pivot: TransformOrigin = TransformOrigin.Center,
    colorFilter: ColorFilter? = null
) {
    val aspect = bitmap.height.toFloat() / bitmap.width.toFloat().coerceAtLeast(1f)
    val baseW = viewport.imageWidth * widthFraction
    val (pw, ph) = if (fitLongestSide && aspect > 1f) {
        (baseW / aspect) to baseW
    } else {
        baseW to (baseW * aspect)
    }
    val p = viewport.pixel(center)
    val leftPx = p.x - pw / 2f
    val topPx = if (bottomAligned) p.y - ph else p.y - ph / 2f
    with(density) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = colorFilter,
            modifier = modifier
                .offset(leftPx.toDp(), topPx.toDp())
                .size(pw.toDp(), ph.toDp())
                .graphicsLayer {
                    this.scaleX = scale
                    this.scaleY = scale
                    this.rotationZ = rotationZ
                    this.alpha = alpha
                    this.transformOrigin = pivot
                }
        )
    }
}

@Composable
fun MandirAltarView(
    session: WorshipSession,
    dustLevel: Float,
    flowerWitherFactor: Float,
    bathTarget: Int?, bathProgress: Float,
    flowerFlights: List<FlowerFlight>,
    aartiRunning: Boolean, aartiProgress: Float,
    onDeityClick: (Int) -> Unit, onDiyaClick: () -> Unit, onBellClick: () -> Unit,
    bellRunning: Boolean, bellProgress: Float,
    conchRunning: Boolean, conchProgress: Float,
    prasadRunning: Boolean, prasadProgress: Float,
    onFlowerClick: (Int) -> Unit, onConchClick: () -> Unit, onAartiClick: () -> Unit,
    onPrasadClick: () -> Unit = {},
    cobwebLevel: Float = 0f,
    selection: ShrineSelection = ShrineSelection(),
    modifier: Modifier = Modifier,
    cleanedAreas: List<TemplePoint> = emptyList(),
    showModels: Boolean = true
) {
    val context = LocalContext.current
    val catalog = remember { ShrineCatalog(context) }
    val single = selection.deityCount == 1
    val idolPlacement = remember(selection) { IdolPlacement(selection) }
    val oilPoint = idolPlacement.space.lamp
    val plateFlowerAngles = remember {
        listOf(-18f, 12f, -8f, 22f, -14f, 16f, 6f, -12f, 18f, -6f)
    }

    // High-resolution 2D ritual & marketplace sprites
    val plateBitmap = rememberAssetImage("shrine/accessories/plate.png")
    val bellBitmap = rememberAssetImage("shrine/accessories/bell.png")
    val shankhStandBitmap = rememberAssetImage("shrine/accessories/shankh_stand.png")
    val copperLotaBitmap = rememberAssetImage("shrine/accessories/copper_lota.png")
    val garlandBitmap = rememberAssetImage(catalog.option("garland", selection).thumbnail)
    val incenseBitmap = rememberAssetImage(catalog.option("incense", selection).thumbnail)
    val lampBitmap = rememberAssetImage(catalog.option("lamp", selection).thumbnail)
    val shankhBitmap = rememberAssetImage(catalog.option("shankh", selection).thumbnail)
    val aartiBitmap = rememberAssetImage(catalog.option("aarti", selection).thumbnail)
    val prasadBitmap = rememberAssetImage(catalog.option("prasad", selection).thumbnail)
    val peonyBitmap = rememberAssetImage("shrine/flowers/peony.png")
    val flowerSpritePaths = remember(selection) {
        catalog.flowerSpritePaths(selection).filterNot { it.endsWith("peony.png") }
            .ifEmpty { listOf("shrine/flowers/sunflower.png") }
    }
    val flowerBitmaps = flowerSpritePaths.map { rememberAssetImage(it) }

    fun flowerBitmapForSlot(index: Int, onPlate: Boolean = true): ImageBitmap {
        if (onPlate && index == 9 && "original" in selection.flowerIds) {
            return peonyBitmap
        }
        if (selection.flowerIds == listOf("original") && flowerBitmaps.size >= 2) {
            return when (index) {
                2, 5, 7, 8 -> flowerBitmaps[1] // Pitambar golden-yellow Marigolds
                else -> flowerBitmaps[0] // Vibrant orange Marigolds
            }
        }
        return flowerBitmaps[index % flowerBitmaps.size]
    }

    val incensePoint = idolPlacement.space.incense
    val incenseWidth = when (selection["incense"]) {
        "dhoop" -> .084f
        "kamal_dhoop" -> .102f
        else -> TempleSceneLayout.INCENSE_WIDTH
    }
    val incenseAspect = incenseBitmap.height.toFloat() / incenseBitmap.width.toFloat().coerceAtLeast(1f)
    val incenseHeight = incenseWidth * incenseAspect
    val incenseEmbers = remember(incensePoint, incenseWidth, incenseHeight, selection["incense"]) {
        when (selection["incense"]) {
            "dhoop" -> listOf(
                TemplePoint(incensePoint.x, incensePoint.y - incenseHeight * .96f)
            )
            "kamal_dhoop" -> listOf(
                TemplePoint(incensePoint.x - incenseWidth * .23f, incensePoint.y - incenseHeight * .94f),
                TemplePoint(incensePoint.x - incenseWidth * .11f, incensePoint.y - incenseHeight * .97f),
                TemplePoint(incensePoint.x + incenseWidth * .11f, incensePoint.y - incenseHeight * .97f),
                TemplePoint(incensePoint.x + incenseWidth * .23f, incensePoint.y - incenseHeight * .94f),
                TemplePoint(incensePoint.x, incensePoint.y - incenseHeight * .62f)
            )
            "mayur_dhoop" -> listOf(
                TemplePoint(incensePoint.x - incenseWidth * .47f, incensePoint.y - incenseHeight * .86f),
                TemplePoint(incensePoint.x - incenseWidth * .25f, incensePoint.y - incenseHeight * .92f),
                TemplePoint(incensePoint.x - incenseWidth * .01f, incensePoint.y - incenseHeight * .98f),
                TemplePoint(incensePoint.x + incenseWidth * .24f, incensePoint.y - incenseHeight * .92f),
                TemplePoint(incensePoint.x + incenseWidth * .46f, incensePoint.y - incenseHeight * .86f),
                TemplePoint(incensePoint.x - incenseWidth * .01f, incensePoint.y - incenseHeight * .44f)
            )
            else -> listOf(
                TemplePoint(incensePoint.x - incenseWidth * .42f, incensePoint.y - incenseHeight * .92f),
                TemplePoint(incensePoint.x - incenseWidth * .21f, incensePoint.y - incenseHeight * .95f),
                TemplePoint(incensePoint.x, incensePoint.y - incenseHeight * .98f),
                TemplePoint(incensePoint.x + incenseWidth * .20f, incensePoint.y - incenseHeight * .96f),
                TemplePoint(incensePoint.x + incenseWidth * .41f, incensePoint.y - incenseHeight * .93f)
            )
        }
    }

    val standingLampWidth = TempleSceneLayout.STANDING_LAMP_WIDTH
    val standingLampAspect = lampBitmap.height.toFloat() / lampBitmap.width.toFloat().coerceAtLeast(1f)
    val standingLampHeight = standingLampWidth * standingLampAspect
    val oilWicks = remember(oilPoint, standingLampWidth, standingLampHeight, selection["lamp"]) {
        when (selection["lamp"]) {
            "brass", "peacock" -> listOf(
                TemplePoint(oilPoint.x - standingLampWidth * .44f, oilPoint.y - standingLampHeight * .77f),
                TemplePoint(oilPoint.x - standingLampWidth * .15f, oilPoint.y - standingLampHeight * .73f),
                TemplePoint(oilPoint.x - standingLampWidth * .14f, oilPoint.y - standingLampHeight * .81f),
                TemplePoint(oilPoint.x + standingLampWidth * .41f, oilPoint.y - standingLampHeight * .80f),
                TemplePoint(oilPoint.x + standingLampWidth * .39f, oilPoint.y - standingLampHeight * .74f)
            )
            else -> listOf(
                TemplePoint(oilPoint.x, oilPoint.y - standingLampHeight * .80f)
            )
        }
    }

    val wiltFilter = remember(flowerWitherFactor) {
        if (flowerWitherFactor <= 0.01f) null
        else {
            val sat = (1f - 0.65f * flowerWitherFactor).coerceIn(0.2f, 1f)
            val darken = 1f - 0.28f * flowerWitherFactor
            val m = ColorMatrix().apply { setToSaturation(sat) }
            val scaleM = ColorMatrix(
                floatArrayOf(
                    darken, 0f, 0f, 0f, 0f,
                    0f, darken * 0.95f, 0f, 0f, 0f,
                    0f, 0f, darken * 0.82f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            m.timesAssign(scaleM)
            ColorFilter.colorMatrix(m)
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val viewport = remember(maxWidth, maxHeight, density) { with(density) { TempleViewport(maxWidth.toPx(), maxHeight.toPx()) } }
        val background = remember(selection["shrine"]) {
            AssetImageCache.image(context, catalog.option("shrine", selection).path)
        }
        Image(
            background, null,
            Modifier.fillMaxSize().graphicsLayer {
                scaleX = TempleViewport.ZOOM
                scaleY = TempleViewport.ZOOM
                transformOrigin = TransformOrigin(.5f, 0f)
            },
            alignment = Alignment.TopCenter,
            contentScale = ContentScale.Crop
        )

        // Sanctum arch Garland (Toran / Mala) framing the upper arch frieze and side pillars of the temple
        GarlandLayer(
            bitmap = garlandBitmap,
            space = idolPlacement.space,
            viewport = viewport
        )

        if (showModels && session.aartiComplete && !aartiRunning) {
            DeityHalosOverlay(heads = idolPlacement.heads)
        }

        val idol = remember(selection["idols"]) {
            AssetImageCache.trimmedIdol(context, catalog.option("idols", selection).path)
        }
        val idolPoint = viewport.pixel(TemplePoint(idolPlacement.left, idolPlacement.top))
        Image(
            idol, selection.deityNames(false).joinToString(),
            with(density) {
                Modifier.offset(idolPoint.x.toDp(), idolPoint.y.toDp())
                    .size((viewport.imageWidth * idolPlacement.width).toDp(), (viewport.imageWidth * idolPlacement.height).toDp())
            },
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomCenter
        )

        TilakOverlay(session.tilak, idolPlacement)

        val prasadLift = if (prasadRunning) TempleSceneLayout.pickup(prasadProgress) else if (session.prasadOffered) 1f else 0f
        val aartiLift = if (aartiRunning) TempleSceneLayout.pickup(aartiProgress) else 0f
        val conchLift = if (conchRunning) TempleSceneLayout.pickup(conchProgress) else 0f
        val bellLift = if (bellRunning || aartiRunning) {
            val t = if (aartiRunning) aartiProgress else bellProgress
            min(t / .15f, (1f - t) / .15f).coerceIn(0f, 1f)
        } else 0f

        // Floor contact shadows beneath the Puja Thali, standing oil lamp, bell, aarti diya, and offered prasad.
        Canvas(Modifier.fillMaxSize()) {
            fun shadow(point: TemplePoint, width: Float, height: Float, intensity: Float = 1f) {
                if (intensity <= 0.01f) return
                val p = viewport.pixel(point); val u = viewport.imageWidth
                val center = Offset(p.x, p.y)
                repeat(9) { i ->
                    val scale = 1f - i * .08f
                    val w = u * width * scale; val h = u * height * scale
                    drawOval(
                        Color(0xFF3A2412).copy(alpha = .026f * intensity),
                        center - Offset(w / 2, h / 2),
                        CanvasSize(w, h)
                    )
                }
            }
            shadow(incensePoint, .115f, .030f, 1.05f)
            shadow(oilPoint, .14f, .034f, 1.15f)
            shadow(TempleSceneLayout.plate.copy(y = TempleSceneLayout.plate.y + .028f), .52f, .33f, 1.35f)
            shadow(TempleSceneLayout.bell.copy(y = TempleSceneLayout.bell.y + .108f), .115f, .038f, 1.10f * (1f - .6f * bellLift))
            shadow(TempleSceneLayout.aartiRest.copy(y = TempleSceneLayout.aartiRest.y + .035f), .155f, .052f, 1.10f * (1f - aartiLift))
            if (prasadRunning || session.prasadOffered) {
                val prasadShadowPoint = if (prasadRunning) idolPlacement.prasadPosition(prasadProgress) else idolPlacement.prasadFloor
                val prasadShadowProgress = if (prasadRunning) prasadProgress else 1f
                val prasadSettle = ((prasadShadowProgress - .78f) / .22f).coerceIn(0f, 1f)
                val prasadShadowScale = 1f - .28f * prasadSettle
                shadow(
                    prasadShadowPoint.copy(y = prasadShadowPoint.y + .035f * prasadShadowScale),
                    .14f * prasadShadowScale,
                    .036f * prasadShadowScale
                )
            }
        }

        if (showModels) {
            // 1a. Sacred Dhoop & Agarbatti Stand on the left altar step
            SpriteLayer(
                bitmap = incenseBitmap,
                center = incensePoint,
                widthFraction = incenseWidth,
                viewport = viewport,
                density = density,
                bottomAligned = true
            )

            // 1b. Standing Temple Oil Lamp (Deepstambh / Kuthuvilakku) on the right altar floor
            SpriteLayer(
                bitmap = lampBitmap,
                center = oilPoint,
                widthFraction = standingLampWidth,
                viewport = viewport,
                density = density,
                bottomAligned = true
            )

            // 2. Offered 2D flowers arranged at the deities' lotus feet
            val visibleOfferings = remember(session.offeredFlowers, selection.deityCount) {
                (0 until selection.deityCount).flatMap { deity ->
                    session.offeredFlowers.filter { it.deity == deity }.takeLast(TempleSceneLayout.OFFERED_FLOWER_SLOTS)
                }
            }
            visibleOfferings.forEach { offering ->
                val bmp = flowerBitmapForSlot(offering.flowerIndex, onPlate = false)
                val baseSize = TempleSceneLayout.offeredFlowerSize(offering.flowerIndex) * .68f
                val wiltScale = 1f - .20f * flowerWitherFactor
                SpriteLayer(
                    bitmap = bmp,
                    center = offering.position,
                    widthFraction = baseSize,
                    viewport = viewport,
                    density = density,
                    fitLongestSide = true,
                    rotationZ = offering.rotation * .25f,
                    scale = wiltScale,
                    colorFilter = wiltFilter
                )
            }
            OfferedFlowerAgingOverlay(viewport, flowerWitherFactor, session.offeredFlowers)

            // 3. Ornate Engraved Brass Puja Thali (with Roli/Chandan katoris & betel leaves)
            SpriteLayer(
                bitmap = plateBitmap,
                center = TempleSceneLayout.plate,
                widthFraction = TempleSceneLayout.PLATE_SIZE,
                viewport = viewport,
                density = density
            )

            // 4. Internal contact shadows on the brass Puja Thali surface
            Canvas(Modifier.fillMaxSize()) {
                val u = viewport.imageWidth
                fun plateShadow(point: TemplePoint, width: Float, height: Float, alphaScale: Float = 1f) {
                    if (alphaScale <= 0.01f) return
                    val p = viewport.pixel(point)
                    val c = Offset(p.x, p.y)
                    repeat(5) { i ->
                        val s = 1f - i * .14f
                        val w = u * width * s; val h = u * height * s
                        drawOval(
                            Color(0xFF2E1A08).copy(alpha = .045f * alphaScale),
                            c - Offset(w / 2, h / 2),
                            CanvasSize(w, h)
                        )
                    }
                }
                // Flower bed shadow on left of thali
                plateShadow(TemplePoint(TempleSceneLayout.plate.x - .138f, TempleSceneLayout.plate.y + .022f), .18f, .15f, .85f)
                // Shankh stand contact shadow on right of thali
                plateShadow(TempleSceneLayout.conchVisual.copy(y = TempleSceneLayout.conchVisual.y + .046f), .128f, .048f, 1f - .4f * conchLift)
                // Prasad bowl contact shadow at bottom-center of thali
                plateShadow(TempleSceneLayout.prasadRest.copy(y = TempleSceneLayout.prasadRest.y + .024f), .142f, .058f, 1f - prasadLift)
            }

            // 5. Ornate Brass Shankh Stand + Sacred Shankh on the right side of the Puja Thali
            SpriteLayer(
                bitmap = shankhStandBitmap,
                center = TempleSceneLayout.conchVisual.copy(y = TempleSceneLayout.conchVisual.y + .024f),
                widthFraction = TempleSceneLayout.CONCH_STAND_WIDTH,
                viewport = viewport,
                density = density
            )
            if (!conchRunning) {
                SpriteLayer(
                    bitmap = shankhBitmap,
                    center = TempleSceneLayout.conchVisual.copy(y = TempleSceneLayout.conchVisual.y - .002f),
                    widthFraction = TempleSceneLayout.CONCH_DISPLAY_WIDTH,
                    viewport = viewport,
                    density = density
                )
            }

            // 6. Lush 2D Flower Cluster along the left & lower-left arc of the Puja Thali
            repeat(TempleSceneLayout.FLOWER_COUNT) { i ->
                val bmp = flowerBitmapForSlot(i, onPlate = true)
                val point = TempleSceneLayout.plateFlower(i)
                val isCrowningHibiscus = i == 9 && "original" in selection.flowerIds
                val sizeFraction = if (isCrowningHibiscus) {
                    .102f
                } else {
                    TempleSceneLayout.flowerSize(i) * .64f
                }
                SpriteLayer(
                    bitmap = bmp,
                    center = point,
                    widthFraction = sizeFraction,
                    viewport = viewport,
                    density = density,
                    fitLongestSide = !isCrowningHibiscus,
                    rotationZ = plateFlowerAngles[i]
                )
            }

            // 7. Ornate 2D Brass Ghanti (Bell) on the upper-left of the Puja Thali
            val bellActive = bellRunning || aartiRunning
            val bellTime = if (aartiRunning) aartiProgress else bellProgress
            val bellSwing = if (bellActive) sin(bellTime * 3.5f * 18f * 1.5f) * 14f * bellLift else 0f
            val bellCenter = TempleSceneLayout.bell.copy(y = TempleSceneLayout.bell.y - .040f * bellLift)
            if (!bellActive) {
                SpriteLayer(
                    bitmap = bellBitmap,
                    center = bellCenter,
                    widthFraction = TempleSceneLayout.BELL_DISPLAY_WIDTH,
                    viewport = viewport,
                    density = density,
                    rotationZ = bellSwing,
                    pivot = TransformOrigin(.5f, .15f)
                )
            }

            // 8. 2D Prasad Katori Bowl (at rest on bottom-center of Thali, or settled at altar after offering)
            if (!prasadRunning) {
                val prasadPoint = if (session.prasadOffered) idolPlacement.prasadFloor else TempleSceneLayout.prasadRest
                val prasadScale = if (session.prasadOffered) .72f else .88f
                SpriteLayer(
                    bitmap = prasadBitmap,
                    center = prasadPoint,
                    widthFraction = TempleSceneLayout.PRASAD_SIZE,
                    viewport = viewport,
                    density = density,
                    fitLongestSide = true,
                    scale = prasadScale
                )
            }

            // 9. 2D Brass Aarti Diya at rest outside the right of the Puja Thali, facing towards the idols
            val aartiSpriteWidth = if (selection["aarti"] == "original") TempleSceneLayout.AARTI_WIDTH * .68f else TempleSceneLayout.AARTI_WIDTH * .64f
            if (!aartiRunning) {
                SpriteLayer(
                    bitmap = aartiBitmap,
                    center = TempleSceneLayout.aartiRest,
                    widthFraction = aartiSpriteWidth,
                    viewport = viewport,
                    density = density
                )
            }
        }

        // 10. Active Foreground 2D Ritual Animations
        if (showModels) {
            Box(Modifier.fillMaxSize().zIndex(9f)) {
                // Active Sacred Shankh & sonic aura waves when sounding
                if (conchRunning) {
                    val conchPoint = TempleSceneLayout.conchPosition(conchProgress)
                    Canvas(Modifier.fillMaxSize()) {
                        val p = viewport.pixel(conchPoint)
                        val center = Offset(p.x, p.y)
                        val u = viewport.imageWidth
                        repeat(3) { ring ->
                            val phase = (conchProgress * 6f + ring / 3f) % 1f
                            val radius = u * (.06f + .16f * phase)
                            drawCircle(
                                color = Color(0xFFFFE082).copy(alpha = (1f - phase) * .55f),
                                radius = radius,
                                center = center,
                                style = Stroke(width = u * .0035f)
                            )
                        }
                    }
                    val turn = TempleSceneLayout.conchTurn(conchProgress)
                    SpriteLayer(
                        bitmap = shankhBitmap,
                        center = conchPoint,
                        widthFraction = TempleSceneLayout.CONCH_DISPLAY_WIDTH,
                        viewport = viewport,
                        density = density,
                        rotationZ = -28f * turn,
                        scale = 1f + .48f * conchLift
                    )
                }

                // Active swinging Bell when rung
                val bellActive = bellRunning || aartiRunning
                if (bellActive) {
                    val bellTime = if (aartiRunning) aartiProgress else bellProgress
                    val bellSwing = sin(bellTime * 3.5f * 18f * 1.5f) * 14f * bellLift
                    val bellCenter = TempleSceneLayout.bell.copy(y = TempleSceneLayout.bell.y - .040f * bellLift)
                    SpriteLayer(
                        bitmap = bellBitmap,
                        center = bellCenter,
                        widthFraction = TempleSceneLayout.BELL_DISPLAY_WIDTH,
                        viewport = viewport,
                        density = density,
                        rotationZ = bellSwing,
                        scale = 1f + .08f * bellLift,
                        pivot = TransformOrigin(.5f, .15f)
                    )
                }

                // Flying 2D Flowers in parabolic arc from Thali to Deities
                flowerFlights.forEach { flight ->
                    val progress = flight.progress.value
                    val start = TempleSceneLayout.plateFlower(flight.offering.flowerIndex)
                    val point = TempleSceneLayout.flowerFlight(start, flight.offering.position, progress)
                    val bmp = flowerBitmapForSlot(flight.offering.flowerIndex, onPlate = false)
                    val startAngle = plateFlowerAngles[flight.offering.flowerIndex]
                    val currentAngle = startAngle + (flight.offering.rotation * .25f - startAngle) * progress
                    val flightScale = 1f + .18f * sin(PI.toFloat() * progress)
                    SpriteLayer(
                        bitmap = bmp,
                        center = point,
                        widthFraction = TempleSceneLayout.flowerSize(flight.offering.flowerIndex) * .72f,
                        viewport = viewport,
                        density = density,
                        fitLongestSide = true,
                        rotationZ = currentAngle,
                        scale = flightScale
                    )
                }

                // Active 2D Copper Kalash / Lota pouring Abhishek water above deity crown
                val target = bathTarget
                if (target != null) {
                    val t = bathProgress
                    val lift = min(t / .12f, (1f - t) / .14f).coerceIn(0f, 1f)
                    val source = idolPlacement.bathSource(target)
                    val lotaPoint = TemplePoint(source.x + .038f * lift, source.y - .045f + .08f * (1f - lift))
                    SpriteLayer(
                        bitmap = copperLotaBitmap,
                        center = lotaPoint,
                        widthFraction = .115f,
                        viewport = viewport,
                        density = density,
                        rotationZ = -54f * lift,
                        pivot = TransformOrigin(.25f, .25f)
                    )
                }

                // Active 2D Prasad Bowl orbiting deities
                if (prasadRunning) {
                    val settle = ((prasadProgress - .78f) / .22f).coerceIn(0f, 1f)
                    val scaleFactor = 1f - .28f * settle + .10f * sin(PI.toFloat() * (1f - settle))
                    val point = idolPlacement.prasadPosition(prasadProgress)
                    SpriteLayer(
                        bitmap = prasadBitmap,
                        center = point,
                        widthFraction = TempleSceneLayout.PRASAD_SIZE,
                        viewport = viewport,
                        density = density,
                        fitLongestSide = true,
                        scale = scaleFactor
                    )
                }

                // Active 2D Brass Aarti Diya orbiting deities, oriented towards the idols
                if (aartiRunning) {
                    val point = idolPlacement.aartiPosition(aartiProgress)
                    val aartiSpriteWidth = if (selection["aarti"] == "original") TempleSceneLayout.AARTI_WIDTH * .68f else TempleSceneLayout.AARTI_WIDTH * .64f
                    SpriteLayer(
                        bitmap = aartiBitmap,
                        center = point,
                        widthFraction = aartiSpriteWidth,
                        viewport = viewport,
                        density = density,
                        scale = 1.05f
                    )
                }
            }
        }

        ShrineAgingOverlay(viewport, dustLevel, cobwebLevel, space = idolPlacement.space, idol = idolPlacement, cleaned = cleanedAreas)
        if (GaneshMouseAnimation.isEligible(selection)) GaneshMouseOverlay(
            placement = idolPlacement, selection = selection,
            offered = session.prasadOffered && !prasadRunning,
            visible = showModels
        )
        val aartiPoint = if (aartiRunning) idolPlacement.aartiPosition(aartiProgress) else TempleSceneLayout.aartiRest
        val aartiRenderScale = if (aartiRunning) 1.05f else 1f
        val aartiW = (if (selection["aarti"] == "original") TempleSceneLayout.AARTI_WIDTH * .68f else TempleSceneLayout.AARTI_WIDTH * .64f) * aartiRenderScale
        val aartiH = aartiW * (aartiBitmap.height.toFloat() / aartiBitmap.width.toFloat().coerceAtLeast(1f))
        val aartiWicks = when (selection["aarti"]) {
            "traditional" -> listOf(
                TemplePoint(aartiPoint.x - aartiW * .45f, aartiPoint.y - aartiH * .06f),
                TemplePoint(aartiPoint.x - aartiW * .36f, aartiPoint.y - aartiH * .34f),
                TemplePoint(aartiPoint.x, aartiPoint.y - aartiH * .47f),
                TemplePoint(aartiPoint.x + aartiW * .36f, aartiPoint.y - aartiH * .34f),
                TemplePoint(aartiPoint.x + aartiW * .45f, aartiPoint.y - aartiH * .06f)
            )
            "karpura" -> listOf(
                TemplePoint(aartiPoint.x - aartiW * .45f, aartiPoint.y - aartiH * .02f),
                TemplePoint(aartiPoint.x - aartiW * .36f, aartiPoint.y - aartiH * .28f),
                TemplePoint(aartiPoint.x, aartiPoint.y - aartiH * .22f),
                TemplePoint(aartiPoint.x + aartiW * .36f, aartiPoint.y - aartiH * .28f),
                TemplePoint(aartiPoint.x + aartiW * .45f, aartiPoint.y - aartiH * .02f)
            )
            else -> listOf(
                TemplePoint(aartiPoint.x, aartiPoint.y - aartiH * .46f)
            )
        }
        if (showModels) Box(Modifier.fillMaxSize().zIndex(10f)) {
            RitualFlamesOverlay(
                session.lit,
                session.aartiLit,
                aartiPoint,
                oilPoint = oilPoint,
                traditional = selection["aarti"] != "original",
                brass = selection["lamp"] != "original",
                oilWick = oilWicks.firstOrNull(),
                oilWicks = oilWicks,
                aartiWicks = aartiWicks,
                incenseEmbers = incenseEmbers
            )
        }
        WaterFlowOverlay(bathTarget, bathProgress, placement = idolPlacement, modifier = Modifier.zIndex(10.5f))
        ScreenWaterDroplets(
            bathTarget = bathTarget, progress = bathProgress, bathed = session.bathed,
            deityCount = session.deityCount, modifier = Modifier.zIndex(11f)
        )

        fun target(point: TemplePoint, width: Float, height: Float): Modifier {
            val p = viewport.pixel(point)
            return with(density) {
                Modifier.offset(p.x.toDp(), p.y.toDp())
                    .size((viewport.imageWidth * width).toDp(), (viewport.imageWidth * height).toDp())
            }
        }
        val names = selection.deityNames(false)
        idolPlacement.heads.forEachIndexed { index, head ->
            val width = if (single) idolPlacement.width else idolPlacement.width / 2
            Box(
                target(TemplePoint(head.x - width / 2, idolPlacement.top), width, idolPlacement.height)
                    .zIndex(12f)
                    .clickable(onClickLabel = names[index]) { onDeityClick(index) }
            )
        }
        // Kumkum & Chandan katoris in the upper-center of the Puja Thali also trigger deity tilak/action when tapped
        if (session.step == WorshipStep.TILAK) {
            val nextDeity = (0 until selection.deityCount).firstOrNull { it !in session.tilak } ?: 0
            Box(
                target(TemplePoint(TempleSceneLayout.plate.x - .09f, TempleSceneLayout.plate.y - .11f), .18f, .10f)
                    .zIndex(12f)
                    .clickable(onClickLabel = "तिलक · Apply tilak") { onDeityClick(nextDeity) }
            )
        }
        Box(
            target(TemplePoint(incensePoint.x - .055f, incensePoint.y - incenseHeight), .11f, incenseHeight + .02f)
                .zIndex(12f)
                .clickable(onClickLabel = "धूप जलाएँ · Light incense", onClick = onDiyaClick)
        )
        Box(
            target(TemplePoint(oilPoint.x - .06f, oilPoint.y - standingLampHeight), .12f, standingLampHeight + .02f)
                .zIndex(12f)
                .clickable(onClickLabel = "दीप जलाएँ · Light oil lamp", onClick = onDiyaClick)
        )
        Box(
            target(TemplePoint(TempleSceneLayout.bell.x - .065f, TempleSceneLayout.bell.y - .10f), .13f, .19f)
                .zIndex(12f)
                .clickable(onClickLabel = "घंटी · Bell", onClick = onBellClick)
        )
        val offerFlower by rememberUpdatedState(onFlowerClick)
        repeat(TempleSceneLayout.FLOWER_COUNT) { i ->
            val point = TempleSceneLayout.plateFlower(i)
            Box(
                target(TemplePoint(point.x - .038f, point.y - .038f), .076f, .076f)
                    .zIndex(12f)
                    .pointerInput(i) {
                        var drag = Offset.Zero
                        detectDragGestures(
                            onDragStart = { drag = Offset.Zero },
                            onDragCancel = { drag = Offset.Zero },
                            onDragEnd = {
                                if (drag.y < -24.dp.toPx() && -drag.y > abs(drag.x)) offerFlower(i)
                            },
                            onDrag = { change, amount -> change.consume(); drag += amount }
                        )
                    }
                    .clickable(onClickLabel = "पुष्प ${i + 1} · Offer flower ${i + 1}") { onFlowerClick(i) }
            )
        }
        Box(
            target(
                TemplePoint(
                    TempleSceneLayout.conch.x - TempleSceneLayout.CONCH_WIDTH / 2f,
                    TempleSceneLayout.conch.y - TempleSceneLayout.CONCH_HEIGHT / 2f
                ),
                TempleSceneLayout.CONCH_WIDTH,
                TempleSceneLayout.CONCH_HEIGHT
            )
                .zIndex(12f)
                .clickable(onClickLabel = "शंख · Sound conch", onClick = onConchClick)
        )
        val currentPrasadPoint = when {
            prasadRunning -> idolPlacement.prasadPosition(prasadProgress)
            session.prasadOffered -> idolPlacement.prasadFloor
            else -> TempleSceneLayout.prasadRest
        }
        Box(
            target(TemplePoint(currentPrasadPoint.x - .085f, currentPrasadPoint.y - .085f), .17f, .17f)
                .zIndex(12f)
                .pointerInput(Unit) {
                    var drag = Offset.Zero
                    detectDragGestures(
                        onDragStart = { drag = Offset.Zero },
                        onDragCancel = { drag = Offset.Zero },
                        onDragEnd = {
                            if (drag.y < -20.dp.toPx() || drag.getDistance() > 16.dp.toPx()) {
                                onPrasadClick()
                            }
                        },
                        onDrag = { change, amount -> change.consume(); drag += amount }
                    )
                }
                .clickable(onClickLabel = "प्रसाद अर्पित करें · Offer prasad", onClick = onPrasadClick)
        )
        Box(
            target(
                TemplePoint(
                    TempleSceneLayout.aartiRest.x - TempleSceneLayout.AARTI_WIDTH / 2f,
                    TempleSceneLayout.aartiRest.y - TempleSceneLayout.AARTI_HEIGHT / 2f
                ),
                TempleSceneLayout.AARTI_WIDTH,
                TempleSceneLayout.AARTI_HEIGHT
            )
                .zIndex(12f)
                .clickable(onClickLabel = "दीप आरती · Diya aarti", onClick = onAartiClick)
        )
    }
}
