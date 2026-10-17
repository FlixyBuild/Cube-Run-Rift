package cube.run.game

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Allocation-free encounter state and collision rules; kept GL-free for deterministic unit tests. */
internal class HoundEncounter {
    companion object {
        const val EVENT_NONE = 0
        const val EVENT_STARTED = 1
        const val EVENT_LUNGE = 2
        const val EVENT_ESCAPED = 3
        const val EVENT_HIT = 4
        private const val IDLE = 0
        private const val APPROACH = 1
        private const val LUNGE = 2
        private const val RETREAT = 3
    }

    private var state = IDLE
    private var encounters = 0
    private var nextAt = 30f
    private var elapsed = 0f
    var x = 0f
        private set
    var z = -100f
        private set
    var targetLane = 1
        private set
    var alert = 0
        private set
    val active: Boolean get() = state == APPROACH || state == LUNGE
    val visible: Boolean get() = state != IDLE && z > -100f && z < 16f

    fun reset() {
        state = IDLE
        encounters = 0
        nextAt = 30f
        elapsed = 0f
        x = 0f; z = -100f; targetLane = 1; alert = 0
    }

    /** Codes returned here are consumed exactly once by the gameplay conductor. */
    fun update(
        runT: Float,
        dt: Float,
        speed: Float,
        playerLane: Int,
        playerX: Float,
        playerY: Float,
        flying: Boolean,
        allowed: Boolean,
    ): Int {
        if (!allowed) {
            if (state != IDLE) {
                state = IDLE
                alert = 0
                nextAt = max(nextAt, runT + 8f)
            }
            return EVENT_NONE
        }
        if (state == IDLE) {
            if (encounters >= 2 || runT < nextAt) return EVENT_NONE
            encounters++
            state = APPROACH
            elapsed = 0f
            targetLane = playerLane
            x = playerX
            // Roughly four seconds of approach warning; clamp for very fast runs.
            z = -max(38f, min(104f, speed * 3.8f))
            alert = 1
            return EVENT_STARTED
        }

        elapsed += dt
        val scroll = max(0f, speed) * dt
        when (state) {
            APPROACH -> {
                z += scroll * 1.05f
                val followX = Lanes.x(playerLane)
                x += (followX - x) * min(1f, dt * 1.7f)
                if (z >= -max(7f, speed * 1.25f)) {
                    targetLane = playerLane
                    state = LUNGE
                    elapsed = 0f
                    alert = 2
                    return EVENT_LUNGE
                }
            }
            LUNGE -> {
                z += scroll * 1.15f
                val targetX = Lanes.x(targetLane)
                x += (targetX - x) * min(1f, dt * 4f)
                if (z >= -1.45f) {
                    // A committed lane gives the player a readable sidestep window;
                    // a real jump clears the low-bodied hound. Collision bounds do not change.
                    val evaded = abs(playerX - targetX) > .98f || flying || playerY > 1.18f
                    state = RETREAT
                    elapsed = 0f
                    alert = if (evaded) 3 else 4
                    nextAt = runT + if (evaded) 82f else 92f
                    return if (evaded) EVENT_ESCAPED else EVENT_HIT
                }
            }
            RETREAT -> {
                z += scroll * 1.45f
                if (elapsed > 1.55f) alert = 0
                if (z > 12f) state = IDLE
            }
        }
        return EVENT_NONE
    }

    /** Shield/Zen contact: the hound recoils and cannot hit a second time. */
    fun repel(runT: Float) {
        if (state != IDLE) {
            state = RETREAT
            elapsed = 0f
            alert = 3
            nextAt = runT + 82f
        }
    }
}
