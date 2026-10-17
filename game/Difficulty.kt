package cube.run.game

import cube.run.data.RunDifficulty
import cube.run.game.track.Sections
import kotlin.math.min

/**
 * A normalised live challenge level (0..1) driving pace and unlocked section tier.
 * The selected [RunDifficulty] supplies its starting level, auto-ramp, speed scale
 * and ceiling while the original fire-boost mechanic remains intact.
 */
class Difficulty(initialPreset: RunDifficulty = RunDifficulty.MEDIUM) {
    val minSpd = 10f
    val maxSpd = 30f
    val blueLine = 0.85f
    val cruiseSpd = minSpd + (maxSpd - minSpd) * blueLine

    var preset: RunDifficulty = initialPreset
        private set
    val rampSeconds: Float get() = preset.rampSeconds
    val startDiff: Float get() = preset.startDiff
    val speedScale: Float get() = preset.speedScale
    val spacingScale: Float get() = preset.spacingScale

    var diff = preset.startDiff
        private set
    /** A run preset caps its auto-ramp; Zen may lower this further. Fire boost can still go past it. */
    var ceiling = preset.autoCeiling

    fun configure(value: RunDifficulty) {
        preset = value
        ceiling = value.autoCeiling
        reset()
    }

    fun reset() { diff = min(startDiff, ceiling) }

    /** Auto-climb toward the current run's ceiling. */
    fun ramp(dt: Float) {
        if (dt.isFinite() && dt > 0f && diff < ceiling) diff = min(ceiling, diff + dt / rampSeconds)
    }

    /** Jump straight to [d] if that's higher than where we are (fire boost). */
    fun boostTo(d: Float) { if (d.isFinite() && d > diff) diff = d.coerceIn(0f, 1f) }

    /** Speed for the current level; preset scaling preserves a clear Easy / Medium / Hard pace. */
    fun speed(): Float {
        val raw = if (diff <= blueLine) {
            minSpd + (maxSpd - minSpd) * diff
        } else {
            val o = (diff - blueLine) / (1f - blueLine)
            cruiseSpd + (maxSpd - cruiseSpd) * (1f - (1f - o) * (1f - o))
        }
        return raw * speedScale
    }

    fun tier(): Int = Sections.tierFor(diff)
}
