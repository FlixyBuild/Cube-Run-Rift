package cube.run.data

/** Player-selected run profiles. These tune the ramp, course spacing and available obstacle mix. */
enum class RunDifficulty(
    val id: Int,
    val startDiff: Float,
    val autoCeiling: Float,
    val rampSeconds: Float,
    val speedScale: Float,
    val spacingScale: Float,
    val spaceMinimumTier: Int,
) {
    EASY(0, startDiff = .04f, autoCeiling = .58f, rampSeconds = 1100f, speedScale = .90f, spacingScale = 1.15f, spaceMinimumTier = 0),
    MEDIUM(1, startDiff = .12f, autoCeiling = 1f, rampSeconds = 700f, speedScale = 1f, spacingScale = 1f, spaceMinimumTier = 1),
    HARD(2, startDiff = .34f, autoCeiling = 1f, rampSeconds = 500f, speedScale = 1.06f, spacingScale = .92f, spaceMinimumTier = 2);

    companion object {
        fun fromId(id: Int): RunDifficulty = entries.firstOrNull { it.id == id } ?: MEDIUM
    }
}
