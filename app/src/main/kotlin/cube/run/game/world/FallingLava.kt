package cube.run.game.world

/** Deterministic, reusable falling molten-rock motion for Lava Caves scenery. */
object FallingLava {
    const val TOP = 10.2f
    const val TRAVEL = 9.75f
    const val FALLS_PER_SECOND = .72f

    fun y(time: Float, phase: Float): Float {
        if (!time.isFinite() || !phase.isFinite()) return TOP
        val cycle = time * FALLS_PER_SECOND + phase
        val fall = ((cycle % 1f) + 1f) % 1f
        return TOP - fall * TRAVEL
    }
}
