package cube.run.game

import kotlin.math.max
import kotlin.math.sin

/** Small deterministic opposite-arm/opposite-leg stride used only by Rift Ranger's model parts. */
object RiftRangerGait {
    fun cycle(time: Float): Float = if (time.isFinite()) sin(time * 9.5f) else 0f

    /** Signed arm arc in degrees; the two arms swing in opposite directions. */
    fun armSwing(time: Float, side: Float): Float = -cycle(time) * side * 27f

    /** Signed leg arc in degrees, opposed to the arm on the same side. */
    fun legSwing(time: Float, side: Float): Float = cycle(time) * side * 23f

    /** Lift the forward foot at the peak of each alternating stride. */
    fun footLift(time: Float, side: Float): Float = max(0f, cycle(time) * side) * .14f
}
