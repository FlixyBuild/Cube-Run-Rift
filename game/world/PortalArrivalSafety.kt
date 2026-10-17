package cube.run.game.world

/** Shared invulnerable arrival window and streamed-empty corridor for portal-like transitions. */
internal object PortalArrivalSafety {
    const val DURATION_SECONDS = 1.5f

    /** Enough empty road to cover the grace window, plus a two-unit visual/recovery buffer. */
    fun corridorDistance(speed: Float): Float {
        val safeSpeed = if (speed.isFinite()) speed.coerceAtLeast(0f) else 0f
        return maxOf(6f, safeSpeed * DURATION_SECONDS + 2f)
    }
}
