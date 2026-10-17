package cube.run.game.world

import kotlin.math.PI
import kotlin.math.sin

/** Deterministic fly-by path used by the low-poly plane in the roadside scenery. */
object PlaneFlight {
    const val PERIOD = 36f
    const val START = 6f
    const val DURATION = 14f
    private const val HALF_SPAN = 25f

    /** Returns the fly-by progress in 0..1, or -1 while the plane is off-screen. */
    fun progress(time: Float): Float {
        if (!time.isFinite()) return -1f
        val phase = ((time % PERIOD) + PERIOD) % PERIOD
        if (phase < START || phase >= START + DURATION) return -1f
        return (phase - START) / DURATION
    }

    fun x(progress: Float): Float = -HALF_SPAN + 2f * HALF_SPAN * progress.coerceIn(0f, 1f)

    fun y(time: Float, progress: Float): Float =
        7.8f + 0.22f * sin(time * 1.15f) + 0.12f * sin(progress.coerceIn(0f, 1f) * PI.toFloat())

    /** Gentle heading variation keeps the fly-by alive without a sudden direction flip. */
    fun bank(progress: Float): Float = 7f * sin(progress.coerceIn(0f, 1f) * PI.toFloat())
}
