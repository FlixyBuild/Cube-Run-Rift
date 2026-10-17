package cube.run.game.world

/** Pure, deterministic vertical motion for reusable Winter Peaks snow particles. */
object Snowfall {
    const val TOP = 4.8f
    const val TRAVEL = 4.76f
    const val FALLS_PER_SECOND = 0.62f

    /** A normalized phase in 0..1 gives each flake a staggered fall that reaches the road surface. */
    fun y(time: Float, phase: Float): Float {
        if (!time.isFinite() || !phase.isFinite()) return TOP
        val cycle = time * FALLS_PER_SECOND + phase
        val fall = ((cycle % 1f) + 1f) % 1f
        return TOP - fall * TRAVEL
    }

    fun drift(time: Float, phase: Float): Float =
        if (!time.isFinite() || !phase.isFinite()) 0f else .32f * kotlin.math.sin(time * 1.7f + phase * 6.2831855f)
}
