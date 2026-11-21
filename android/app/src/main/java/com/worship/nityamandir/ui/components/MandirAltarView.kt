package com.worship.nityamandir.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
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
import kotlin.math.cos
import kotlin.math.hypot
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
    viewport: TempleViewport,
    modifier: Modifier = Modifier
) {
    Canvas(modifier.fillMaxSize()) {
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
    rotationX: Float = 0f,
    rotationY: Float = 0f,
    rotationZ: Float = 0f,
    scale: Float = 1f,
    alpha: Float = 1f,
    cameraDistanceFactor: Float = 14f,
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
                    this.rotationX = rotationX
                    this.rotationY = rotationY
                    this.rotationZ = rotationZ
                    this.cameraDistance = cameraDistanceFactor * density.density
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
    onWaterClick: (Int?) -> Unit = {},
    onTilakClick: () -> Unit = {},
    cobwebLevel: Float = 0f,
    selection: ShrineSelection = ShrineSelection(),
    modifier: Modifier = Modifier,
    cleanedAreas: List<TemplePoint> = emptyList(),
    showModels: Boolean = true,
    activeOccasion: TempleOccasion? = null,
    interactionsEnabled: Boolean = true
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
    val plateBitmap = rememberAssetImage("shrine/accessories/plate.webp")
    val bellBitmap = rememberAssetImage("shrine/accessories/bell.webp")
    val shankhStandBitmap = rememberAssetImage("shrine/accessories/shankh_stand.webp")
    val copperLotaBitmap = rememberAssetImage("shrine/accessories/copper_lota.webp")
    val garlandBitmap = if (idolPlacement.space.showGarland) {
        rememberAssetImage(catalog.option("garland", selection).thumbnail)
    } else null
    val incenseBitmap = rememberAssetImage(catalog.option("incense", selection).thumbnail)
    val lampBitmap = rememberAssetImage(catalog.option("lamp", selection).thumbnail)
    val shankhBitmap = rememberAssetImage(catalog.option("shankh", selection).thumbnail)
    // Preserve Paramparik's five-bowl design; its GLB is a different silhouette.
    val traditionalAartiBitmap = if (selection["aarti"] == "traditional") {
        rememberAssetImage(catalog.option("aarti", selection).thumbnail)
    } else null
    val prasadBitmap = rememberAssetImage(catalog.option("prasad", selection).thumbnail)
    val peonyBitmap = rememberAssetImage("shrine/flowers/peony.webp")
    val flowerSpritePaths = remember(selection) {
        catalog.flowerSpritePaths(selection).filterNot { it.endsWith("peony.webp") }
            .ifEmpty { listOf("shrine/flowers/sunflower.webp") }
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

    val waterLotaAspect = copperLotaBitmap.height.toFloat() / copperLotaBitmap.width.toFloat().coerceAtLeast(1f)
    val waterLotaWidth = TempleSceneLayout.WATER_LOTA_WIDTH
    val waterLotaHeight = waterLotaWidth * waterLotaAspect
    val waterLotaRestCenter = TemplePoint(TempleSceneLayout.waterLota.x, TempleSceneLayout.waterLota.y - waterLotaHeight / 2f)

    val altarActive = interactionsEnabled && !aartiRunning
    val allEnabled = altarActive

    val lampActive = altarActive
    val incenseActive = altarActive
    val waterActive = altarActive && bathTarget == null
    val tilakActive = altarActive
    val flowersActive = altarActive
    val conchActive = altarActive && !conchRunning
    val prasadActive = altarActive && !prasadRunning
    val aartiActive = interactionsEnabled && !aartiRunning
    val bellEnabled = altarActive && !bellRunning

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

    // Interactive 3D Hand-Held Aarti state (devotee can touch & drag to wave the 3D Aarti lamp around the deities)
    var interactiveAartiPoint by remember { mutableStateOf<TemplePoint?>(null) }
    var interactiveAartiVelX by remember { mutableFloatStateOf(0f) }
    val aartiTrailHistory = remember { mutableStateListOf<TemplePoint>() }

    LaunchedEffect(aartiRunning) {
        if (!aartiRunning) {
            interactiveAartiPoint = null
            interactiveAartiVelX = 0f
            aartiTrailHistory.clear()
        }
    }

    val idolCenterX = idolPlacement.left + idolPlacement.width * 0.5f
    val idolCenterY = idolPlacement.top + idolPlacement.height * 0.48f
    val autoAartiPoint = if (aartiRunning) idolPlacement.aartiPosition(aartiProgress) else TempleSceneLayout.aartiRest
    val targetAartiPoint = if (aartiRunning) (interactiveAartiPoint ?: autoAartiPoint) else TempleSceneLayout.aartiRest
    // Damped follow-through keeps the lamp, flames and shadow together on handoff.
    // Compose animations also respect the system animator duration scale.
    val heldPosition by animateOffsetAsState(
        targetValue = Offset(targetAartiPoint.x, targetAartiPoint.y),
        animationSpec = spring(dampingRatio = 1f, stiffness = 500f),
        label = "Aarti hand position"
    )
    val activeAartiPoint = TemplePoint(heldPosition.x, heldPosition.y)
    val aartiPresence by animateFloatAsState(
        targetValue = if (aartiRunning) 1f else 0f,
        animationSpec = spring(dampingRatio = 1f, stiffness = 180f),
        label = "Aarti sanctum depth"
    )

    LaunchedEffect(activeAartiPoint, aartiRunning) {
        if (aartiRunning) {
            if (aartiTrailHistory.isEmpty() || hypot(
                    activeAartiPoint.x - aartiTrailHistory.first().x,
                    activeAartiPoint.y - aartiTrailHistory.first().y
                ) > 0.004f
            ) {
                aartiTrailHistory.add(0, activeAartiPoint)
                if (aartiTrailHistory.size > 14) {
                    aartiTrailHistory.removeAt(aartiTrailHistory.lastIndex)
                }
            }
        }
    }

    val aartiMesh = remember(selection["aarti"]) {
        GlbAartiModelCache.getOrLoad(context, selection["aarti"])
    }

    val aartiOrbitPhase = ((aartiProgress - .22f) / .56f).coerceIn(0f, 1f) * 6f * PI.toFloat()
    val targetAartiDepth = when {
        !aartiRunning -> 0f
        interactiveAartiPoint != null -> ((activeAartiPoint.y - idolCenterY) / 0.24f).coerceIn(-0.85f, 1.0f)
        aartiProgress in 0.22f..0.78f -> sin(aartiOrbitPhase)
        aartiProgress < 0.22f -> 0.55f * sin((aartiProgress / 0.22f) * PI.toFloat())
        else -> 0.55f * sin(((1f - aartiProgress) / 0.22f) * PI.toFloat())
    }
    val aartiZDepth by animateFloatAsState(
        targetValue = targetAartiDepth,
        animationSpec = spring(dampingRatio = 1f, stiffness = 300f),
        label = "Aarti orbit depth"
    )
    val aartiScale3D = 1f + 0.10f * aartiPresence + 0.18f * aartiZDepth
    val aartiRelX = activeAartiPoint.x - idolCenterX
    // Keep the flame side facing the idols during the orbit, with a gentle
    // inward turn and wrist bank rather than a pronounced sideways angle.
    val targetAartiYaw = if (aartiRunning) {
        (aartiRelX * 18f + interactiveAartiVelX * 3f).coerceIn(-10f, 10f)
    } else {
        // Canonical -Z is the flame side: zero yaw faces it into the shrine,
        // with the handle toward the devotee instead of angled sideways.
        0f
    }
    val targetAartiPitch = if (aartiRunning) {
        (21f + 7.5f * aartiZDepth).coerceIn(13f, 30f)
    } else {
        20f
    }
    val targetAartiRoll = when {
        !aartiRunning -> 0f
        interactiveAartiPoint != null -> (-interactiveAartiVelX * 2.5f).coerceIn(-3f, 3f)
        aartiProgress in 0.22f..0.78f -> -cos(aartiOrbitPhase) * 2f
        else -> 0f
    }
    val aartiModelYaw by animateFloatAsState(targetAartiYaw,
        spring(dampingRatio = 1f, stiffness = 280f), label = "Aarti wrist yaw")
    val aartiModelPitch by animateFloatAsState(targetAartiPitch,
        spring(dampingRatio = 1f, stiffness = 280f), label = "Aarti wrist pitch")
    val aartiModelRoll by animateFloatAsState(targetAartiRoll,
        spring(dampingRatio = 1f, stiffness = 280f), label = "Aarti wrist roll")
    val aartiFlameBendX = when {
        !aartiRunning -> 0f
        interactiveAartiPoint != null -> (-interactiveAartiVelX * 0.016f).coerceIn(-0.015f, 0.015f)
        aartiProgress in 0.22f..0.78f -> -cos(aartiOrbitPhase) * 0.010f
        else -> 0f
    }
    val aartiBaseWidth = if (selection["aarti"] == "traditional") {
        TempleSceneLayout.AARTI_WIDTH * 0.64f
    } else {
        TempleSceneLayout.AARTI_WIDTH * 0.84f
    }
    val aartiModelWidth = aartiBaseWidth * aartiScale3D

    // 3D Sanctum Multi-Plane Perspective & Parallax driven by active 3D Aarti position
    val sanctumRelX = (activeAartiPoint.x - idolCenterX).coerceIn(-0.45f, 0.45f) * aartiPresence
    val sanctumRelY = (activeAartiPoint.y - idolCenterY).coerceIn(-0.45f, 0.45f) * aartiPresence
    val sanctumYawY = -sanctumRelX * 6.5f
    val sanctumPitchX = sanctumRelY * 4.0f + aartiZDepth * 1.6f

    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val viewport = remember(maxWidth, maxHeight, density) { with(density) { TempleViewport(maxWidth.toPx(), maxHeight.toPx()) } }
        val parallaxPx = viewport.imageWidth * 0.020f
        val background = remember(selection["shrine"]) {
            AssetImageCache.image(context, catalog.option("shrine", selection).path)
        }
        // Plane 1 (Deep Sanctum Arch, z = -1.0): Extended top wall + 3D perspective tilt & counter-parallax
        Canvas(
            Modifier.fillMaxSize().graphicsLayer {
                val extraZoom = 1f + 0.018f * aartiPresence
                scaleX = extraZoom
                scaleY = extraZoom
                rotationX = sanctumPitchX * 0.55f
                rotationY = sanctumYawY * 0.55f
                translationX = -sanctumRelX * parallaxPx * 0.85f
                translationY = -sanctumRelY * parallaxPx * 0.55f
                cameraDistance = 22f * density.density
                transformOrigin = TransformOrigin(.5f, 0.30f)
            }
        ) {
            val dstLeft = viewport.left.toInt()
            val dstW = viewport.imageWidth.toInt().coerceAtLeast(1)
            val dstH = (viewport.imageWidth * (background.height.toFloat() / background.width.toFloat())).toInt().coerceAtLeast(1)
            val dstTop = (viewport.top - 0.02f * (TempleViewport.ZOOM / 1.08f) * size.height).toInt()
            if (dstTop > -24) {
                // Clamp-extend the exact top pixel row (y = 0) seamlessly up to the screen top edge.
                val extTop = -24
                val extHeight = (dstTop - extTop + 1).coerceAtLeast(1)
                drawImage(
                    image = background,
                    srcOffset = IntOffset(0, 0),
                    srcSize = IntSize(background.width, 1),
                    dstOffset = IntOffset(dstLeft, extTop),
                    dstSize = IntSize(dstW, extHeight),
                    filterQuality = FilterQuality.Low
                )
            }
            drawImage(
                image = background,
                srcOffset = IntOffset(0, 0),
                srcSize = IntSize(background.width, background.height),
                dstOffset = IntOffset(dstLeft, dstTop),
                dstSize = IntSize(dstW, dstH),
                filterQuality = FilterQuality.High
            )
        }

        // Plane 2 (Sanctum Arch Garland / Toran, z = -0.55): Mid-back 3D parallax (omitted on big-idol shrines)
        if (garlandBitmap != null) {
            GarlandLayer(
                bitmap = garlandBitmap,
                space = idolPlacement.space,
                viewport = viewport,
                modifier = Modifier
                    .testTag("altar-garland")
                    .graphicsLayer {
                        rotationX = sanctumPitchX * 0.68f
                        rotationY = sanctumYawY * 0.68f
                        translationX = -sanctumRelX * parallaxPx * 0.45f
                        translationY = -sanctumRelY * parallaxPx * 0.30f
                        cameraDistance = 20f * density.density
                    }
            )
        }

        if (selection["shrine"] == "diwali") {
            DiwaliDecorations(viewport = viewport, animate = showModels)
        }

        // Floor decorations sit behind idols, offerings and standing ritual objects.
        if (OccasionAartiAnimation.isEligible(selection, activeOccasion)) OccasionAartiOverlay(
            placement = idolPlacement,
            selection = selection,
            activeOccasion = activeOccasion,
            aartiComplete = session.aartiComplete && !aartiRunning,
            visible = showModels
        )

        if (showModels && session.aartiComplete && !aartiRunning) {
            DeityHalosOverlay(heads = idolPlacement.heads)
        }

        // 3D Volumetric Garbhagriha Aura & Sacred Light Rays behind Deity Idols during Aarti / Lit Sanctum
        if (showModels && (aartiRunning || session.lit || session.aartiLit)) {
            Canvas(Modifier.fillMaxSize()) {
                val u = viewport.imageWidth
                val depthBoost = if (aartiRunning) (0.75f + 0.35f * (1f - aartiZDepth * 0.5f)) else 0.42f
                idolPlacement.heads.forEachIndexed { idx, head ->
                    val hp = viewport.pixel(TemplePoint(head.x, head.y + 0.04f))
                    val auraRadius = u * (if (single) 0.32f else 0.24f) * (if (aartiRunning) 1.12f else 0.92f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                Color(0x44FF9E2C).copy(alpha = 0.26f * depthBoost),
                                Color(0x28D8AF52).copy(alpha = 0.14f * depthBoost),
                                Color.Transparent
                            ),
                            center = Offset(hp.x, hp.y),
                            radius = auraRadius
                        ),
                        radius = auraRadius,
                        center = Offset(hp.x, hp.y),
                        blendMode = BlendMode.Screen
                    )
                    // Subtle volumetric sanctum light rays fanning behind the deity crown during 3D Aarti
                    if (aartiRunning) {
                        repeat(7) { rayIdx ->
                            val angle = (-0.75f + rayIdx * 0.25f) + sin(aartiProgress * 18f + idx + rayIdx) * 0.04f
                            val rayLen = auraRadius * (1.15f + 0.15f * sin(aartiProgress * 12f + rayIdx))
                            val endX = hp.x + sin(angle) * rayLen
                            val endY = hp.y - cos(angle) * rayLen * 0.82f
                            drawLine(
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color(0x36FFD54F).copy(alpha = 0.18f * depthBoost),
                                        Color.Transparent
                                    ),
                                    start = Offset(hp.x, hp.y),
                                    end = Offset(endX, endY)
                                ),
                                start = Offset(hp.x, hp.y),
                                end = Offset(endX, endY),
                                strokeWidth = u * 0.024f,
                                cap = StrokeCap.Round,
                                blendMode = BlendMode.Screen
                            )
                        }
                    }
                }
            }
        }

        val idolAssetPath = catalog.option("idols", selection).path
        val idol = remember(idolAssetPath) {
            AssetImageCache.trimmedIdol(context, idolAssetPath)
        }
        val idolWaterSurface = remember(idolAssetPath) {
            AssetImageCache.idolWaterSurface(context, idolAssetPath)
        }
        val idolPoint = viewport.pixel(TemplePoint(idolPlacement.left, idolPlacement.top))
        // Plane 3 (Sacred Deity Idols, z = 0.0): 3D perspective & subtle depth parallax
        Image(
            idol, selection.deityNames(false).joinToString(),
            with(density) {
                Modifier.offset(idolPoint.x.toDp(), idolPoint.y.toDp())
                    .size((viewport.imageWidth * idolPlacement.width).toDp(), (viewport.imageWidth * idolPlacement.height).toDp())
                    .graphicsLayer {
                        rotationX = sanctumPitchX * 0.78f
                        rotationY = sanctumYawY * 0.85f
                        translationX = -sanctumRelX * parallaxPx * 0.16f
                        translationY = -sanctumRelY * parallaxPx * 0.10f
                        cameraDistance = 20f * density.density
                        transformOrigin = TransformOrigin(0.5f, 0.75f)
                    }
            },
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomCenter
        )

        // Real-Time 3D Point-Light Illumination on Deity Idols & Sanctum Pillars as the 3D Aarti Lamp Orbits
        if (showModels && aartiRunning) {
            Canvas(Modifier.fillMaxSize()) {
                val u = viewport.imageWidth
                val lampPx = viewport.pixel(activeAartiPoint)
                val flameOrigin = Offset(lampPx.x, lampPx.y - u * 0.048f * aartiScale3D)

                // 1. Dynamic 3D Point-Light Specular & Warm Kesariya Illumination across each Deity Idol
                idolPlacement.heads.forEach { head ->
                    val targetCenter = TemplePoint(head.x, head.y + idolPlacement.height * 0.28f)
                    val targetPx = viewport.pixel(targetCenter)
                    val dist2D = hypot(activeAartiPoint.x - targetCenter.x, activeAartiPoint.y - targetCenter.y)
                    val proximity = (1f - (dist2D / 0.52f)).coerceIn(0.15f, 1f)
                    // Shifts slightly toward the 3D lamp position to simulate directional specular highlight on the idol
                    val highlightCenter = Offset(
                        targetPx.x + (flameOrigin.x - targetPx.x) * 0.28f,
                        targetPx.y + (flameOrigin.y - targetPx.y) * 0.28f
                    )
                    val illumRadius = u * 0.28f * (0.85f + 0.30f * proximity)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                Color(0xFFFFFDF8).copy(alpha = 0.22f * proximity),
                                Color(0xFFFFC107).copy(alpha = 0.18f * proximity),
                                Color(0xFFFF9E2C).copy(alpha = 0.09f * proximity),
                                Color.Transparent
                            ),
                            center = highlightCenter,
                            radius = illumRadius
                        ),
                        radius = illumRadius,
                        center = highlightCenter,
                        blendMode = BlendMode.Screen
                    )
                }

                // 2. Dynamic 3D Rim Light on Left & Right Temple Pillars when Aarti swings near them
                val spec = idolPlacement.space.garlandSpec
                val leftPillarX = spec[0] + spec[4] * 0.5f
                val rightPillarX = spec[2] - spec[4] * 0.5f
                val pillarMidY = (spec[1] + spec[3]) * 0.5f
                listOf(leftPillarX, rightPillarX).forEach { pxNorm ->
                    val pDist = abs(activeAartiPoint.x - pxNorm)
                    val pillarProx = (1f - (pDist / 0.42f)).coerceIn(0f, 1f)
                    if (pillarProx > 0.05f) {
                        val pillarPx = viewport.pixel(TemplePoint(pxNorm, activeAartiPoint.y.coerceIn(spec[1], spec[3])))
                        val rimR = u * 0.18f * (0.7f + 0.3f * pillarProx)
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(
                                    Color(0xFFFF9E2C).copy(alpha = 0.20f * pillarProx),
                                    Color(0xFFD8AF52).copy(alpha = 0.08f * pillarProx),
                                    Color.Transparent
                                ),
                                center = Offset(pillarPx.x, pillarPx.y.coerceIn(viewport.top + pillarMidY * u * 0.4f, size.height)),
                                radius = rimR
                            ),
                            radius = rimR,
                            center = Offset(pillarPx.x, pillarPx.y),
                            blendMode = BlendMode.Screen
                        )
                    }
                }
            }
        }

        val prasadLift = if (prasadRunning) TempleSceneLayout.pickup(prasadProgress) else if (session.prasadOffered) 1f else 0f
        val aartiLift = if (aartiRunning) TempleSceneLayout.pickup(aartiProgress) else 0f
        val conchLift = if (conchRunning) TempleSceneLayout.pickup(conchProgress) else 0f
        val waterTravel = if (bathTarget != null) TempleSceneLayout.waterLotaTravel(bathProgress) else 0f
        val bellLift = if (bellRunning || aartiRunning) {
            val t = if (aartiRunning) aartiProgress else bellProgress
            min(t / .15f, (1f - t) / .15f).coerceIn(0f, 1f)
        } else 0f

        // Floor contact shadows beneath the Puja Thali, standing oil lamp, water lota, bell, aarti diya, and offered prasad.
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
            shadow(TempleSceneLayout.waterLota.copy(y = TempleSceneLayout.waterLota.y - .002f), .105f, .028f, 1.05f * (1f - waterTravel))
            shadow(TempleSceneLayout.plate.copy(y = TempleSceneLayout.plate.y + .028f), .52f, .33f, 1.35f)
            shadow(TempleSceneLayout.bell.copy(y = TempleSceneLayout.bell.y + .108f), .115f, .038f, 1.10f * (1f - .6f * bellLift))
            shadow(TempleSceneLayout.aartiRest.copy(y = TempleSceneLayout.aartiRest.y + .035f), .155f, .052f, 1.10f * (1f - aartiLift))
            // Dynamic 3D Floor Shadow cast by the orbiting 3D Aarti Lamp on the altar marble
            if (aartiRunning) {
                val floorY = (TempleSceneLayout.aartiRest.y + 0.025f + 0.035f * aartiZDepth).coerceIn(0.72f, 0.94f)
                val heightAboveFloor = (floorY - activeAartiPoint.y).coerceAtLeast(0.02f)
                val shadowSpread = (1f + heightAboveFloor * 0.85f).coerceIn(0.85f, 1.55f)
                val shadowIntensity = (0.72f - heightAboveFloor * 0.75f + 0.18f * aartiZDepth).coerceIn(0.18f, 0.85f)
                val dynamicShadowPt = TemplePoint(
                    x = activeAartiPoint.x + (activeAartiPoint.x - idolCenterX) * 0.12f,
                    y = floorY
                )
                shadow(
                    dynamicShadowPt,
                    .155f * shadowSpread,
                    .048f * shadowSpread,
                    shadowIntensity
                )
            }
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

            // 2b. Sacred Copper Water Kalash / Lota resting on the altar floor when not pouring
            if (bathTarget == null) {
                SpriteLayer(
                    bitmap = copperLotaBitmap,
                    center = TempleSceneLayout.waterLota,
                    widthFraction = waterLotaWidth,
                    viewport = viewport,
                    density = density,
                    bottomAligned = true
                )
            }

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

            // 9. 3D GLB Brass Aarti Lamp at rest outside the right of the Puja Thali, angled towards the idols
            if (!aartiRunning) {
                if (traditionalAartiBitmap != null) {
                    SpriteLayer(
                        bitmap = traditionalAartiBitmap,
                        center = activeAartiPoint,
                        widthFraction = aartiModelWidth,
                        viewport = viewport,
                        density = density
                    )
                } else GlbAarti3DModelLayer(
                    mesh = aartiMesh,
                    center = activeAartiPoint,
                    widthFraction = aartiModelWidth,
                    viewport = viewport,
                    yawDeg = aartiModelYaw,
                    pitchDeg = aartiModelPitch,
                    rollDeg = aartiModelRoll,
                    lit = session.aartiLit || session.lit,
                    aartiRunning = false
                )
            }
        }

        // Sacred Chandan, Kumkum & Akshat Tilak overlay and offering flight from the Puja Thali katoris
        TilakOverlay(session.tilak, idolPlacement)

        // 10. Active Foreground 2D & 3D Ritual Animations
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

                // Active swinging Bell when rung (with 3D Kashi-style sonic golden ripples)
                val bellActive = bellRunning || aartiRunning
                if (bellActive) {
                    val bellTime = if (aartiRunning) aartiProgress else bellProgress
                    val bellSwing = sin(bellTime * 3.5f * 18f * 1.5f) * 14f * bellLift
                    val bellCenter = TempleSceneLayout.bell.copy(y = TempleSceneLayout.bell.y - .040f * bellLift)
                    Canvas(Modifier.fillMaxSize()) {
                        val p = viewport.pixel(bellCenter)
                        val u = viewport.imageWidth
                        repeat(3) { ring ->
                            val phase = ((bellTime * (if (aartiRunning) 8.5f else 4.5f)) + ring / 3f) % 1f
                            val rx = u * (.048f + .115f * phase) * bellLift
                            val ry = rx * 0.68f
                            drawOval(
                                color = Color(0xFFD8AF52).copy(alpha = (1f - phase) * 0.38f * bellLift),
                                topLeft = Offset(p.x - rx, p.y - ry + u * 0.02f),
                                size = CanvasSize(rx * 2f, ry * 2f),
                                style = Stroke(width = u * 0.0026f),
                                blendMode = BlendMode.Screen
                            )
                        }
                    }
                    SpriteLayer(
                        bitmap = bellBitmap,
                        center = bellCenter,
                        widthFraction = TempleSceneLayout.BELL_DISPLAY_WIDTH,
                        viewport = viewport,
                        density = density,
                        rotationX = 6f * bellLift,
                        rotationY = -bellSwing * 0.35f,
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

                // Active 2D Copper Kalash / Lota traveling from resting spot to deity crown & pouring Abhishek water
                val target = bathTarget
                if (target != null) {
                    val t = bathProgress
                    val tilt = TempleSceneLayout.waterLotaTilt(t)
                    val source = idolPlacement.bathSource(target)
                    val pourCenter = TempleSceneLayout.waterLotaPourCenter(
                        source = source,
                        width = TempleSceneLayout.WATER_LOTA_POUR_WIDTH,
                        aspect = waterLotaAspect
                    )
                    val lotaPoint = TempleSceneLayout.waterLotaPosition(waterLotaRestCenter, pourCenter, t)
                    val lotaWidth = TempleSceneLayout.waterLotaWidth(t)
                    SpriteLayer(
                        bitmap = copperLotaBitmap,
                        center = lotaPoint,
                        widthFraction = lotaWidth,
                        viewport = viewport,
                        density = density,
                        rotationZ = TempleSceneLayout.WATER_LOTA_POUR_ANGLE_DEG * tilt,
                        pivot = TransformOrigin(
                            TempleSceneLayout.WATER_LOTA_PIVOT_X,
                            TempleSceneLayout.WATER_LOTA_PIVOT_Y
                        )
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

                // Active 3D GLB Panch-Aarti Model orbiting deities (or guided interactively in 3D space)
                if (aartiRunning) {
                    val point = activeAartiPoint

                    // Dynamic 3D Point-Light Radiance, Brass Thali Specular Sheen & 3D Golden Ribbon Trail
                    Canvas(Modifier.fillMaxSize()) {
                        val p = viewport.pixel(point)
                        val u = viewport.imageWidth
                        val lightCenter = Offset(p.x, p.y - u * 0.048f * aartiScale3D)
                        val lightR = u * (0.38f + 0.08f * aartiZDepth)

                        // 1. 4-Stop Sacred Diya Volumetric Light Sphere around the 3D Aarti Lamp
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(
                                    Color(0x58FFFDF8),
                                    Color(0x4CFFC107),
                                    Color(0x28FF9E2C),
                                    Color.Transparent
                                ),
                                center = lightCenter,
                                radius = lightR
                            ),
                            radius = lightR,
                            center = lightCenter,
                            blendMode = BlendMode.Screen
                        )

                        // 2. 3D Perspective Brass Plinth Halo directly beneath the floating Aarti Diya
                        val plinthRx = u * 0.082f * aartiScale3D
                        val plinthRy = plinthRx * (0.36f + 0.08f * aartiZDepth.coerceIn(-0.6f, 0.8f))
                        drawOval(
                            brush = Brush.radialGradient(
                                listOf(
                                    Color(0x66D8AF52),
                                    Color(0x28FF9E2C),
                                    Color.Transparent
                                ),
                                center = Offset(p.x, p.y + u * 0.014f * aartiScale3D),
                                radius = plinthRx
                            ),
                            topLeft = Offset(p.x - plinthRx, p.y + u * 0.014f * aartiScale3D - plinthRy),
                            size = CanvasSize(plinthRx * 2f, plinthRy * 2f),
                            blendMode = BlendMode.Screen
                        )

                        // 3. Moving Golden Specular Reflection on the Foreground Brass Puja Thali
                        val platePx = viewport.pixel(TempleSceneLayout.plate)
                        val plateSpecCenter = Offset(
                            platePx.x + (p.x - platePx.x) * 0.24f,
                            platePx.y + (p.y - platePx.y) * 0.14f
                        )
                        val plateSpecR = u * 0.18f
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(
                                    Color(0x28FFD54F),
                                    Color(0x12D8AF52),
                                    Color.Transparent
                                ),
                                center = plateSpecCenter,
                                radius = plateSpecR
                            ),
                            radius = plateSpecR,
                            center = plateSpecCenter,
                            blendMode = BlendMode.Screen
                        )

                        // 4. 3D Tapered Golden Light Ribbon & Rising Spark Embers along the 3D Circumambulation Wake
                        val trailPoints = if (interactiveAartiPoint != null && aartiTrailHistory.size >= 2) {
                            aartiTrailHistory.toList()
                        } else {
                            List(11) { idx ->
                                val lagProgress = (aartiProgress - (idx + 1) * 0.0075f).coerceIn(0f, 1f)
                                idolPlacement.aartiPosition(lagProgress)
                            }
                        }
                        var prevOffset = lightCenter
                        trailPoints.forEachIndexed { idx, trailPt ->
                            val tp = viewport.pixel(trailPt)
                            val trailFade = (1f - (idx + 1).toFloat() / (trailPoints.size + 1).toFloat()).coerceIn(0f, 1f)
                            val curOffset = Offset(tp.x, tp.y - u * 0.042f * (0.94f + 0.06f * trailFade))
                            drawLine(
                                color = Color(0xFFFFC107).copy(alpha = 0.28f * trailFade),
                                start = prevOffset,
                                end = curOffset,
                                strokeWidth = u * 0.018f * trailFade * aartiScale3D,
                                cap = StrokeCap.Round,
                                blendMode = BlendMode.Screen
                            )
                            drawCircle(
                                color = Color(0xFFFFFDF8).copy(alpha = 0.42f * trailFade),
                                radius = u * 0.011f * trailFade * aartiScale3D,
                                center = curOffset,
                                blendMode = BlendMode.Screen
                            )
                            prevOffset = curOffset
                        }
                    }

                    if (traditionalAartiBitmap != null) {
                        SpriteLayer(
                            bitmap = traditionalAartiBitmap,
                            center = point,
                            widthFraction = aartiModelWidth,
                            viewport = viewport,
                            density = density
                        )
                    } else GlbAarti3DModelLayer(
                        mesh = aartiMesh,
                        center = point,
                        widthFraction = aartiModelWidth,
                        viewport = viewport,
                        yawDeg = aartiModelYaw,
                        pitchDeg = aartiModelPitch,
                        rollDeg = aartiModelRoll,
                        lit = true,
                        aartiRunning = true
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
        val aartiPoint = activeAartiPoint
        val aartiRenderScale = aartiScale3D
        val aartiWicks = if (traditionalAartiBitmap != null) {
            // Wick anchors belong to the original five-bowl artwork, not the GLB.
            val height = aartiModelWidth * traditionalAartiBitmap.height.toFloat() /
                traditionalAartiBitmap.width.toFloat().coerceAtLeast(1f)
            listOf(
                TemplePoint(aartiPoint.x - aartiModelWidth * .45f, aartiPoint.y - height * .06f),
                TemplePoint(aartiPoint.x - aartiModelWidth * .36f, aartiPoint.y - height * .34f),
                TemplePoint(aartiPoint.x, aartiPoint.y - height * .47f),
                TemplePoint(aartiPoint.x + aartiModelWidth * .36f, aartiPoint.y - height * .34f),
                TemplePoint(aartiPoint.x + aartiModelWidth * .45f, aartiPoint.y - height * .06f)
            )
        } else projectAarti3DWicks(
            mesh = aartiMesh,
            center = aartiPoint,
            widthFraction = aartiModelWidth,
            yawDeg = aartiModelYaw,
            pitchDeg = aartiModelPitch,
            rollDeg = aartiModelRoll
        )

        if (showModels) Box(Modifier.fillMaxSize().zIndex(10f)) {
            RitualFlamesOverlay(
                oilLit = session.lit,
                aartiLit = session.aartiLit,
                lamp = aartiPoint,
                oilPoint = oilPoint,
                traditional = true,
                brass = selection["lamp"] != "original",
                oilWick = oilWicks.firstOrNull(),
                oilWicks = oilWicks,
                aartiWicks = aartiWicks,
                incenseEmbers = incenseEmbers,
                aartiRunning = aartiRunning,
                aartiDepthScale = aartiRenderScale,
                aartiBendX = aartiFlameBendX,
                karpuraMode = selection["aarti"] == "karpura",
                showIdleAartiFlame = true
            )
        }
        WaterFlowOverlay(
            deity = bathTarget,
            progress = bathProgress,
            placement = idolPlacement,
            surface = idolWaterSurface,
            lotaRestCenter = waterLotaRestCenter,
            lotaAspect = waterLotaAspect,
            modifier = Modifier.zIndex(10.5f)
        )
        ScreenWaterDroplets(
            bathTarget = bathTarget,
            progress = bathProgress,
            bathed = session.bathed,
            deityCount = session.deityCount,
            placement = idolPlacement,
            surface = idolWaterSurface,
            modifier = Modifier.zIndex(11f)
        )

        fun target(point: TemplePoint, width: Float, height: Float): Modifier {
            val p = viewport.pixel(point)
            return with(density) {
                Modifier.offset(p.x.toDp(), p.y.toDp())
                    .size((viewport.imageWidth * width).toDp(), (viewport.imageWidth * height).toDp())
            }
        }
        fun Modifier.ritualItemTarget(
            tag: String,
            label: String,
            enabled: Boolean,
            exposeContentDescription: Boolean = true,
            onClick: () -> Unit
        ): Modifier =
            testTag(tag).let { tagged ->
                if (enabled) tagged.clickable(onClickLabel = label, onClick = onClick)
                else tagged.semantics {
                    if (exposeContentDescription) contentDescription = label
                    disabled()
                }
            }

        val names = selection.deityNames(false)
        idolPlacement.heads.forEachIndexed { index, head ->
            val width = if (single) idolPlacement.width else idolPlacement.width / 2
            Box(
                target(TemplePoint(head.x - width / 2, idolPlacement.top), width, idolPlacement.height)
                    .zIndex(12f)
                    .ritualItemTarget("altar-deity-$index", names[index], allEnabled, exposeContentDescription = false) {
                        onDeityClick(index)
                    }
            )
        }
        // Kumkum & Chandan katoris in the upper-center of the Puja Thali trigger tilak freely
        Box(
            target(TemplePoint(TempleSceneLayout.plate.x - .095f, TempleSceneLayout.plate.y - .152f), .19f, .105f)
                .zIndex(12f)
                .ritualItemTarget("altar-item-tilak", "तिलक · Apply tilak", tilakActive, onClick = onTilakClick)
        )
        // Copper Water Kalash / Lota on the right altar floor triggers sacred bathing during the Bath step
        val triggerWater by rememberUpdatedState(onWaterClick)
        val waterGesture = if (waterActive) {
            Modifier.pointerInput(viewport, selection.deityCount) {
                var drag = Offset.Zero
                detectDragGestures(
                    onDragStart = { drag = Offset.Zero },
                    onDragCancel = { drag = Offset.Zero },
                    onDragEnd = {
                        val draggedDeity = if (selection.deityCount > 1 && abs(drag.x) > 24.dp.toPx()) {
                            if (drag.x < -24.dp.toPx()) 0 else 1
                        } else null
                        triggerWater(draggedDeity)
                    },
                    onDrag = { change, amount -> change.consume(); drag += amount }
                )
            }
        } else Modifier
        Box(
            target(
                TemplePoint(TempleSceneLayout.waterLota.x - .065f, TempleSceneLayout.waterLota.y - waterLotaHeight - .015f),
                .13f,
                waterLotaHeight + .03f
            )
                .zIndex(12f)
                .then(waterGesture)
                .ritualItemTarget("altar-item-water", "जल स्नान · Bathe with water", waterActive) { onWaterClick(null) }
        )
        Box(
            target(TemplePoint(incensePoint.x - .055f, incensePoint.y - incenseHeight), .11f, incenseHeight + .02f)
                .zIndex(12f)
                .ritualItemTarget("altar-item-incense", "धूप जलाएँ · Light incense", incenseActive, onClick = onDiyaClick)
        )
        Box(
            target(TemplePoint(oilPoint.x - .06f, oilPoint.y - standingLampHeight), .12f, standingLampHeight + .02f)
                .zIndex(12f)
                .ritualItemTarget("altar-item-lamp", "दीप जलाएँ · Light oil lamp", lampActive, onClick = onDiyaClick)
        )
        Box(
            target(TemplePoint(TempleSceneLayout.bell.x - .065f, TempleSceneLayout.bell.y - .10f), .13f, .19f)
                .zIndex(12f)
                .ritualItemTarget("altar-item-bell", "घंटी · Bell", bellEnabled, onClick = onBellClick)
        )
        val offerFlower by rememberUpdatedState(onFlowerClick)
        repeat(TempleSceneLayout.FLOWER_COUNT) { i ->
            val point = TempleSceneLayout.plateFlower(i)
            val flowerDrag = if (flowersActive) {
                Modifier.pointerInput(i) {
                    detectDragGestures(
                        onDragEnd = { offerFlower(i) },
                        onDrag = { change, _ -> change.consume() }
                    )
                }
            } else Modifier
            Box(
                target(TemplePoint(point.x - .038f, point.y - .038f), .076f, .076f)
                    .zIndex(12f)
                    .then(flowerDrag)
                    .ritualItemTarget("altar-item-flower-$i", "पुष्प ${i + 1} · Offer flower ${i + 1}", flowersActive) { onFlowerClick(i) }
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
                .ritualItemTarget("altar-item-conch", "शंख · Sound conch", conchActive, onClick = onConchClick)
        )
        val currentPrasadPoint = when {
            prasadRunning -> idolPlacement.prasadPosition(prasadProgress)
            session.prasadOffered -> idolPlacement.prasadFloor
            else -> TempleSceneLayout.prasadRest
        }
        val triggerPrasad by rememberUpdatedState(onPrasadClick)
        val prasadDrag = if (prasadActive) {
            Modifier.pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { triggerPrasad() },
                    onDrag = { change, _ -> change.consume() }
                )
            }
        } else Modifier
        Box(
            target(TemplePoint(currentPrasadPoint.x - .085f, currentPrasadPoint.y - .085f), .17f, .17f)
                .zIndex(12f)
                .then(prasadDrag)
                .ritualItemTarget("altar-item-prasad", "प्रसाद अर्पित करें · Offer prasad", prasadActive, onClick = onPrasadClick)
        )
        val triggerAarti by rememberUpdatedState(onAartiClick)
        val isAartiRunning by rememberUpdatedState(aartiRunning)
        val aartiDrag = if (aartiActive || (interactionsEnabled && aartiRunning)) {
            Modifier.pointerInput(viewport) {
                detectDragGestures(
                    onDragStart = {
                        if (!isAartiRunning) triggerAarti()
                    },
                    onDragEnd = {
                        interactiveAartiPoint = null
                        interactiveAartiVelX = 0f
                    },
                    onDragCancel = {
                        interactiveAartiPoint = null
                        interactiveAartiVelX = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val cur = interactiveAartiPoint ?: TempleSceneLayout.aartiRest
                        val dx = dragAmount.x / viewport.imageWidth.coerceAtLeast(1f)
                        val dy = dragAmount.y / viewport.imageWidth.coerceAtLeast(1f)
                        interactiveAartiVelX = (interactiveAartiVelX * 0.6f + dx * 16f).coerceIn(-1.5f, 1.5f)
                        interactiveAartiPoint = TemplePoint(
                            (cur.x + dx).coerceIn(0.14f, 0.86f),
                            (cur.y + dy).coerceIn(0.22f, 0.88f)
                        )
                    }
                )
            }
        } else Modifier
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
                .then(aartiDrag)
                .ritualItemTarget("altar-item-aarti", "दीप आरती · Diya aarti", aartiActive, onClick = onAartiClick)
        )

        // Interactive 3D Hand-Held Aarti Canvas: during Aarti, devotee can touch & circle the 3D Aarti Thali anywhere around the deities
        if (aartiRunning) {
            Box(
                Modifier
                    .fillMaxSize()
                    .zIndex(13f)
                    .pointerInput(viewport) {
                        detectDragGestures(
                            onDragStart = { touchOffset ->
                                val pt = viewport.point(touchOffset.x, touchOffset.y)
                                val tx = pt.x.coerceIn(0.14f, 0.86f)
                                val ty = pt.y.coerceIn(0.22f, 0.88f)
                                interactiveAartiPoint = TemplePoint(tx, ty)
                                interactiveAartiVelX = 0f
                            },
                            onDragEnd = {
                                interactiveAartiPoint = null
                                interactiveAartiVelX = 0f
                            },
                            onDragCancel = {
                                interactiveAartiPoint = null
                                interactiveAartiVelX = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val pt = viewport.point(change.position.x, change.position.y)
                                val tx = pt.x.coerceIn(0.14f, 0.86f)
                                val ty = pt.y.coerceIn(0.22f, 0.88f)
                                val dx = dragAmount.x / viewport.imageWidth.coerceAtLeast(1f)
                                interactiveAartiVelX = (interactiveAartiVelX * 0.6f + dx * 16f).coerceIn(-1.5f, 1.5f)
                                interactiveAartiPoint = TemplePoint(tx, ty)
                            }
                        )
                    }
            )
        }
    }
}
