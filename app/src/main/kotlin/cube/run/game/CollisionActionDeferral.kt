package cube.run.game

/**
 * Structural Track.rows mutations requested during CubeRun's collision walk
 * must run after the ArrayList iterator finishes. Keep every action in order:
 * multiple portals can be present in one pass, and silently replacing an
 * earlier deferred reset would leave challenge rows stale.
 */
internal class CollisionActionDeferral {
    private var passActive = false
    private val pending = ArrayDeque<() -> Unit>()

    fun beginPass() {
        check(!passActive) { "Collision row pass already active" }
        check(pending.isEmpty()) { "Deferred collision actions leaked between passes" }
        passActive = true
    }

    /** Returns true when [action] was queued for the end of the active pass. */
    fun defer(action: () -> Unit): Boolean {
        if (!passActive) return false
        pending.addLast(action)
        return true
    }

    fun endPass() {
        if (!passActive) return
        passActive = false
        // The rows iterator is now closed. Run queued mutations only after that point.
        while (pending.isNotEmpty()) pending.removeFirst().invoke()
    }
}
