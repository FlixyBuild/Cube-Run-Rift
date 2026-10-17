package cube.run.game.track

import kotlin.random.Random

/** Shuffled bonus-portal destinations: every pool member is drawn before a cycle can repeat. */
internal class PortalDestinationBag(private val random: Random) {
    private val remaining = ArrayList<Int>()
    private var activePool: Set<Int> = emptySet()
    private var lastDrawn: Int? = null

    fun reset() {
        remaining.clear()
        activePool = emptySet()
        lastDrawn = null
    }

    fun next(destinations: List<Int>): Int? {
        val unique = destinations.distinct()
        if (unique.isEmpty()) {
            reset()
            return null
        }
        val pool = unique.toSet()
        if (pool != activePool) {
            remaining.clear()
            activePool = pool
            lastDrawn = null
        }
        if (remaining.isEmpty()) {
            remaining.addAll(unique.shuffled(random))
            // Keep the boundary between full cycles non-repeating as well.
            if (remaining.size > 1 && remaining.first() == lastDrawn) {
                val different = remaining.indexOfFirst { it != lastDrawn }
                if (different > 0) {
                    val first = remaining[0]
                    remaining[0] = remaining[different]
                    remaining[different] = first
                }
            }
        }
        val selected = remaining.removeAt(0)
        lastDrawn = selected
        return selected
    }
}
