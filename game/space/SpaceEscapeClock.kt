package cube.run.game.space

/** Deterministic clock/distance state for the timed singularity escape. */
class SpaceEscapeClock(
    val durationSeconds: Float = 14.5f,
    val routeDistance: Float = 150f,
) {
    enum class State { READY, ACTIVE, ESCAPED, CAPTURED }

    init {
        require(durationSeconds.isFinite() && durationSeconds > 0f)
        require(routeDistance.isFinite() && routeDistance > 0f)
    }

    var state: State = State.READY
        private set
    var secondsLeft: Float = durationSeconds
        private set
    var distanceLeft: Float = routeDistance
        private set

    val active: Boolean get() = state == State.ACTIVE
    val timerProgress: Float get() = (secondsLeft / durationSeconds).coerceIn(0f, 1f)
    val routeProgress: Float get() = (1f - distanceLeft / routeDistance).coerceIn(0f, 1f)

    fun start(): Boolean {
        if (state != State.READY) return false
        state = State.ACTIVE
        return true
    }

    /**
     * Integrate real gameplay seconds and forward movement. Reaching the gate
     * wins a tie with timer expiry, so a gate crossed on the final frame counts.
     */
    fun advance(dt: Float, moved: Float, countSeconds: Boolean = true): State {
        if (!active) return state
        val seconds = if (countSeconds) dt.takeIf { it.isFinite() && it > 0f } ?: 0f else 0f
        val distance = moved.takeIf { it.isFinite() && it > 0f } ?: 0f
        secondsLeft = (secondsLeft - seconds).coerceAtLeast(0f)
        distanceLeft = (distanceLeft - distance).coerceAtLeast(0f)
        state = when {
            distanceLeft <= 0f -> State.ESCAPED
            secondsLeft <= 0f -> State.CAPTURED
            else -> State.ACTIVE
        }
        return state
    }

    fun reset() {
        state = State.READY
        secondsLeft = durationSeconds
        distanceLeft = routeDistance
    }
}
