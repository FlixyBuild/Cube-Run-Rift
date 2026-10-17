package cube.run.game

import cube.run.game.track.ObstacleFactory
import cube.run.game.track.Row
import cube.run.game.track.Track
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollisionActionDeferralTest {
    @Test fun restartingSpaceExitDefersTrackRemovalAndAppendUntilTheRowsIteratorFinishes() {
        val random = Random(23)
        val track = Track(random, ObstacleFactory(random))
        val oldExit = track.addChallengeExit(60f)
        val portal = Row(1f, ArrayList())
        track.rows.add(portal)
        track.rows.add(Row(2f, ArrayList()))
        val visited = ArrayList<Row>()
        val deferral = CollisionActionDeferral()

        deferral.beginPass()
        try {
            for (row in track.rows) {
                visited += row
                if (row === portal) {
                    assertTrue(deferral.defer {
                        track.removeChallengeExit(oldExit)
                        track.addChallengeExit(80f)
                    })
                }
            }
        } finally {
            deferral.endPass()
        }

        assertEquals(3, visited.size)
        assertFalse(track.rows.contains(oldExit))
        assertEquals(1, track.rows.count { it.challengeExit })
        assertTrue(track.rows.last().challengeExit)
    }

    @Test fun challengeRowChangesRemainImmediateOutsideCollisionPass() {
        val rows = arrayListOf("current")
        val deferral = CollisionActionDeferral()
        var executed = false

        val action = {
            rows += "exit"
            executed = true
        }
        if (!deferral.defer(action)) action()

        assertTrue(executed)
        assertEquals(listOf("current", "exit"), rows)
    }

    @Test fun multipleDeferredActionsRunInOrderAfterThePass() {
        val deferral = CollisionActionDeferral()
        val order = ArrayList<Int>()
        deferral.beginPass()
        assertTrue(deferral.defer { order += 1 })
        assertTrue(deferral.defer { order += 2 })
        assertTrue(order.isEmpty())
        deferral.endPass()
        assertEquals(listOf(1, 2), order)
    }
}
