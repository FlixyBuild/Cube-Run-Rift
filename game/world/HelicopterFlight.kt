package cube.run.game.world

import kotlin.math.PI
import kotlin.math.sin

/** A deterministic distant helicopter fly-by for the elevated Air route. */
object HelicopterFlight {
    const val PERIOD = 52f
    const val START = 17f
    const val DURATION = 18f
    private const val HALF_SPAN = 23f

    /** Returns progress in 0..1 during a pass, or -1 while the helicopter is off-screen. */
    fun progress(time: Float): Float {
        if (!time.isFinite()) return -1f
        val phase = ((time % PERIOD) + PERIOD) % PERIOD
        if (phase < START || phase >= START + DURATION) return -1f
        return (phase - START) / DURATION
    }

    fun x(progress: Float): Float = HALF_SPAN - 2f * HALF_SPAN * progress.coerceIn(0f, 1f)

    fun y(time: Float, progress: Float): Float =
        6.2f + .18f * sin(time * .9f) + .10f * sin(progress.coerceIn(0f, 1f) * PI.toFloat())

    fun bank(progress: Float): Float = 8f * sin(progress.coerceIn(0f, 1f) * PI.toFloat())
}
