package cube.run.game.world

import cube.run.game.track.CoinType

/** One optional biome mission: collect eight green coins during a Jungle visit. */
class JungleHarvest(val target: Int = 8) {
    init { require(target > 0) }

    var active: Boolean = false
        private set
    var collected: Int = 0
        private set
    var completed: Boolean = false
        private set

    val progress: Float get() = (collected.toFloat() / target).coerceIn(0f, 1f)

    fun enter(isJungle: Boolean) {
        active = isJungle
        collected = 0
        completed = false
    }

    /** Returns true exactly once, on the coin that completes the mission. */
    fun collect(type: CoinType): Boolean {
        if (!active || completed || type != CoinType.GREEN) return false
        collected = (collected + 1).coerceAtMost(target)
        if (collected < target) return false
        completed = true
        return true
    }
}
