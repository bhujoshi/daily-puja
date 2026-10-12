package com.worship.nityamandir.engine

import com.worship.nityamandir.data.ShrineSelection
import kotlin.math.PI
import kotlin.math.sin

/** A short visit after laddu is offered; positions use the shrine's image-width units. */
data class GaneshMousePose(
    val position: TemplePoint,
    val opacity: Float,
    val scale: Float,
    val eating: Float,
    val ladduRemaining: Float,
    val scurry: Float,
    val hop: Float,
    val bow: Float,
    val facingRight: Boolean,
    val phaseSeconds: Float
)

object GaneshMouseAnimation {
    const val DURATION_MILLIS = 12_000
    private const val EATING_END = 4.6f
    private const val WALK_END = 8.4f
    private const val HOP_END = 9.7f

    fun isEligible(selection: ShrineSelection): Boolean =
        selection["idols"] in setOf("original", "ganesh_hanuman") &&
            selection["prasad"] in setOf("original", "laddu")

    /** Landmarks on the alpha-cropped idol image, beside Ganesh ji's exposed foot. */
    fun feet(placement: IdolPlacement, selection: ShrineSelection): TemplePoint {
        val local = if (selection["idols"] == "ganesh_hanuman")
            TemplePoint(.26f, .88f) else TemplePoint(.20f, .85f)
        return TemplePoint(
            placement.left + local.x * placement.width,
            placement.top + local.y * placement.height
        )
    }

    fun pose(progress: Float, placement: IdolPlacement, selection: ShrineSelection): GaneshMousePose {
        val seconds = progress.coerceIn(0f, 1f) * (DURATION_MILLIS / 1000f)
        val bowl = placement.prasadFloor.copy(x = placement.prasadFloor.x - .08f)
        val foot = feet(placement, selection)
        val takeoff = TemplePoint(foot.x - .012f, placement.space.floorY)
        val walk = interval(seconds, EATING_END, WALK_END)
        val leap = interval(seconds, WALK_END, HOP_END)
        val hop = if (seconds in WALK_END..HOP_END) sin(PI.toFloat() * leap).coerceAtLeast(0f) else 0f
        val position = when {
            seconds <= EATING_END -> bowl
            seconds < WALK_END -> travel(bowl, takeoff, smooth(walk))
            seconds < HOP_END -> travel(takeoff, foot, smooth(leap)).let {
                it.copy(y = it.y - .045f * hop)
            }
            else -> foot
        }
        return GaneshMousePose(
            position = position,
            opacity = smooth(seconds / .65f),
            scale = 1f - .26f * smooth(interval(seconds, EATING_END, HOP_END)),
            eating = smooth(seconds / .65f) * (1f - smooth(interval(seconds, 4f, EATING_END))),
            ladduRemaining = 1f - smooth(interval(seconds, .65f, 4.5f)),
            scurry = smooth(interval(seconds, EATING_END, EATING_END + .3f)) *
                (1f - smooth(interval(seconds, WALK_END - .3f, WALK_END))),
            hop = hop,
            bow = smooth(interval(seconds, HOP_END, 10.05f)) *
                (1f - smooth(interval(seconds, 10.55f, 11f))),
            facingRight = seconds <= EATING_END || foot.x >= bowl.x,
            phaseSeconds = seconds
        )
    }

    private fun interval(seconds: Float, start: Float, end: Float): Float =
        ((seconds - start) / (end - start)).coerceIn(0f, 1f)

    private fun smooth(value: Float): Float {
        val t = value.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun travel(from: TemplePoint, to: TemplePoint, progress: Float) = TemplePoint(
        from.x + (to.x - from.x) * progress,
        from.y + (to.y - from.y) * progress
    )
}
