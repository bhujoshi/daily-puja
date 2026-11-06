package com.worship.nityamandir.engine

import com.worship.nityamandir.data.ShrineSelection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Pose of a single clay diya in the post-Aarti Deepmala sequence. */
data class DeepmalaDiyaPose(
    val index: Int,
    val tier: Int,
    val position: TemplePoint,
    val scale: Float,
    val vesselOpacity: Float,
    val flameIgnite: Float,
    val sparkBurst: Float,
    val floorReflection: Float
)

/** Pose of a single auspicious footprint (e.g., Lakshmi Charan) stepping toward the sanctum. */
data class SacredCharanPose(
    val stepIndex: Int,
    val isLeftFoot: Boolean,
    val position: TemplePoint,
    val scale: Float,
    val angleDegrees: Float,
    val imprintOpacity: Float,
    val rippleProgress: Float,
    val rippleAlpha: Float
)

/** Pose of the floor Mandala / Ashtadala Kamal Rangoli. */
data class RangoliPose(
    val center: TemplePoint,
    val radiusX: Float,
    val verticalSquash: Float,
    val innerBloom: Float,
    val middleBloom: Float,
    val outerBloom: Float,
    val ambientGlow: Float,
    val shimmerPhase: Float
)

/** Complete deterministic snapshot of the post-Aarti occasion celebration at a given time. */
data class OccasionAartiPose(
    val occasionId: String,
    val style: OccasionCelebrationStyle,
    val rangoli: RangoliPose,
    val diyas: List<DeepmalaDiyaPose>,
    val charanSteps: List<SacredCharanPose>,
    val sanctumAura: Float,
    val phaseSeconds: Float
)

/**
 * Pure, deterministic animation engine for post-Aarti occasion celebrations
 * (Diwali Deepmala, Rangoli & Lakshmi Charan, extensible to other Hindu festivals).
 */
object OccasionAartiAnimation {
    const val DURATION_MILLIS = 12_000
    const val ALTAR_DIYA_COUNT = 7
    const val FLOOR_DIYA_COUNT = 6
    const val TOTAL_DIYA_COUNT = ALTAR_DIYA_COUNT + FLOOR_DIYA_COUNT
    const val CHARAN_STEP_COUNT = 6

    private const val RANGOLI_START = 0.0f
    private const val RANGOLI_END = 3.2f
    private const val ALTAR_DIYAS_START = 0.7f
    private const val FLOOR_DIYAS_START = 3.1f
    private const val DIYA_IGNITE_DURATION = 0.65f
    private const val CHARAN_START = 3.6f
    private const val CHARAN_STEP_INTERVAL = 0.75f
    private const val CHARAN_IMPRINT_DURATION = 0.60f
    private const val CHARAN_RIPPLE_DURATION = 1.15f

    /** Resolves which occasion celebration should run for [selection], if any. */
    fun occasionFor(
        selection: ShrineSelection,
        activeOccasion: TempleOccasion? = null
    ): TempleOccasion? = TempleOccasions.forSelection(selection, activeOccasion)

    /** True when [selection] triggers an occasion celebration after Aarti. */
    fun isEligible(
        selection: ShrineSelection,
        activeOccasion: TempleOccasion? = null
    ): Boolean = occasionFor(selection, activeOccasion) != null

    /**
     * Computes the full celebration pose at [progress] (`0f..1f` over [DURATION_MILLIS])
     * and continuous [elapsedSeconds] for ongoing flame flicker after the intro completes.
     */
    fun pose(
        progress: Float,
        placement: IdolPlacement,
        selection: ShrineSelection,
        activeOccasion: TempleOccasion? = null,
        elapsedSeconds: Float? = null
    ): OccasionAartiPose {
        val clamped = progress.coerceIn(0f, 1f)
        val introSeconds = clamped * (DURATION_MILLIS / 1000f)
        val totalSeconds = elapsedSeconds?.coerceAtLeast(introSeconds) ?: introSeconds
        val occasion = occasionFor(selection, activeOccasion) ?: TempleOccasions.DIWALI
        val space = placement.space
        val hasPermanentDeepmala = selection["shrine"] == "diwali"

        // 1. Floor Ashtadala Kamal Rangoli centered between altar base and prasad bowl
        val rangoliCenterY = if (hasPermanentDeepmala) space.altarY + .115f
            else (space.altarY + placement.prasadFloor.y) * 0.5f + 0.005f
        val rangoliCenter = TemplePoint(0.50f, rangoliCenterY)
        val innerBloom = smooth(interval(introSeconds, RANGOLI_START, RANGOLI_START + 1.2f))
        val middleBloom = smooth(interval(introSeconds, RANGOLI_START + 0.6f, RANGOLI_START + 2.1f))
        val outerBloom = smooth(interval(introSeconds, RANGOLI_START + 1.3f, RANGOLI_END))
        val pulse = 0.92f + 0.08f * sin(totalSeconds * 1.8f)
        val rangoli = RangoliPose(
            center = rangoliCenter,
            radiusX = if (hasPermanentDeepmala) .10f else .135f,
            verticalSquash = if (hasPermanentDeepmala) .30f else .38f,
            innerBloom = innerBloom,
            middleBloom = middleBloom,
            outerBloom = outerBloom,
            ambientGlow = outerBloom * pulse,
            shimmerPhase = totalSeconds
        )

        // 2. Sequential Deepmala Diyas:
        // Tier 0: 7 diyas along the altar step ledge, igniting from the center outward in pairs.
        // Keep the Diwali lamps in the clear central space below the flower offerings.
        val ledgeSpanLeft = if (hasPermanentDeepmala) .39f else space.left + .02f
        val ledgeSpanRight = if (hasPermanentDeepmala) .61f else space.right - .02f
        val altarDiyas = (0 until ALTAR_DIYA_COUNT).map { i ->
            val fraction = i.toFloat() / (ALTAR_DIYA_COUNT - 1).toFloat()
            val x = ledgeSpanLeft + (ledgeSpanRight - ledgeSpanLeft) * fraction
            val distanceFromCenter = abs(i - ALTAR_DIYA_COUNT / 2) // 0, 1, 2, 3
            val igniteStart = ALTAR_DIYAS_START + distanceFromCenter * 0.55f
            val rawIgnite = interval(introSeconds, igniteStart, igniteStart + DIYA_IGNITE_DURATION)
            val ignite = smooth(rawIgnite)
            val burst = if (rawIgnite in 0.01f..0.99f) sin(PI.toFloat() * rawIgnite).coerceAtLeast(0f) else 0f
            val vesselFade = smooth(interval(introSeconds, igniteStart - 0.35f, igniteStart + 0.15f))
            DeepmalaDiyaPose(
                index = i,
                tier = 0,
                position = TemplePoint(x, if (hasPermanentDeepmala) space.altarY + .074f else space.ledgeY + .004f),
                scale = if (hasPermanentDeepmala) .65f else if (distanceFromCenter == 0) 1.06f else .94f,
                vesselOpacity = vesselFade,
                flameIgnite = ignite,
                sparkBurst = burst,
                floorReflection = ignite * 0.75f
            )
        }

        // Tier 1: 6 diyas arranged in a sacred semicircular arc on the marble floor around the Rangoli.
        val floorOffsets = listOf(
            TemplePoint(-0.155f, -0.022f) to 0,
            TemplePoint(0.155f, -0.022f) to 0,
            TemplePoint(-0.235f, 0.012f) to 1,
            TemplePoint(0.235f, 0.012f) to 1,
            TemplePoint(-0.305f, 0.048f) to 2,
            TemplePoint(0.305f, 0.048f) to 2
        )
        // Diwali already has permanent side lamps; a second floor arc crowded the ritual objects.
        val floorDiyas = (if (hasPermanentDeepmala) emptyList() else floorOffsets).mapIndexed { idx, (offset, pairIndex) ->
            val igniteStart = FLOOR_DIYAS_START + pairIndex * 0.60f
            val rawIgnite = interval(introSeconds, igniteStart, igniteStart + DIYA_IGNITE_DURATION)
            val ignite = smooth(rawIgnite)
            val burst = if (rawIgnite in 0.01f..0.99f) sin(PI.toFloat() * rawIgnite).coerceAtLeast(0f) else 0f
            val vesselFade = smooth(interval(introSeconds, igniteStart - 0.35f, igniteStart + 0.15f))
            DeepmalaDiyaPose(
                index = ALTAR_DIYA_COUNT + idx,
                tier = 1,
                position = TemplePoint(rangoliCenter.x + offset.x, rangoliCenter.y + offset.y),
                scale = 0.96f + pairIndex * 0.05f,
                vesselOpacity = vesselFade,
                flameIgnite = ignite,
                sparkBurst = burst,
                floorReflection = ignite
            )
        }

        // 3. Sacred Lakshmi Charan (6 alternating left/right footprints walking from foreground to altar)
        val startY = rangoliCenter.y + (if (hasPermanentDeepmala) .018f else .058f)
        val endY = if (hasPermanentDeepmala) rangoliCenter.y - .022f else space.altarY + .048f
        val charanSteps = (0 until CHARAN_STEP_COUNT).map { step ->
            val isLeft = step % 2 == 0
            val pairFraction = step.toFloat() / (CHARAN_STEP_COUNT - 1).toFloat()
            val y = startY + (endY - startY) * pairFraction
            val xOffset = if (isLeft) -0.026f else 0.026f
            // Slight inward convergence as the footsteps approach the altar
            val perspectiveScale = (1f - 0.16f * pairFraction) * (if (hasPermanentDeepmala) .5f else 1f)
            val x = 0.50f + xOffset * perspectiveScale
            val stepStart = CHARAN_START + step * CHARAN_STEP_INTERVAL
            val imprint = smooth(interval(introSeconds, stepStart, stepStart + CHARAN_IMPRINT_DURATION))
            val rippleRaw = interval(introSeconds, stepStart, stepStart + CHARAN_RIPPLE_DURATION)
            val rippleProgress = smooth(rippleRaw)
            val rippleAlpha = sin(PI.toFloat() * rippleRaw).coerceAtLeast(0f) * imprint
            SacredCharanPose(
                stepIndex = step,
                isLeftFoot = isLeft,
                position = TemplePoint(x, y),
                scale = perspectiveScale,
                angleDegrees = if (isLeft) -6f else 6f,
                imprintOpacity = imprint,
                rippleProgress = rippleProgress,
                rippleAlpha = rippleAlpha
            )
        }

        val sanctumAura = smooth(interval(introSeconds, 0.4f, 4.8f)) * pulse

        return OccasionAartiPose(
            occasionId = occasion.id,
            style = occasion.celebrationStyle,
            rangoli = rangoli,
            diyas = altarDiyas + floorDiyas,
            charanSteps = charanSteps,
            sanctumAura = sanctumAura,
            phaseSeconds = totalSeconds
        )
    }

    private fun interval(seconds: Float, start: Float, end: Float): Float =
        ((seconds - start) / (end - start)).coerceIn(0f, 1f)

    private fun smooth(value: Float): Float {
        val t = value.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
