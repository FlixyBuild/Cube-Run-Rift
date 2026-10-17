package cube.run.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Fast JVM regression coverage for hound timing and fair escape/collision rules. */
class HoundEncounterLogicTest {
    @Before fun resetLanes() = Lanes.reset()

    private fun reachLunge(chase: HoundEncounter): Float {
        assertEquals(HoundEncounter.EVENT_STARTED,
            chase.update(30f, 0f, 12.5f, 1, Lanes.x(1), .45f, false, true))
        assertEquals(1, chase.alert)
        var time = 30f
        repeat(100) {
            time += .05f
            val event = chase.update(time, .05f, 12.5f, 1, Lanes.x(1), .45f, false, true)
            if (event == HoundEncounter.EVENT_LUNGE) {
                assertEquals(2, chase.alert)
                return time
            }
        }
        error("No lunge after the visible approach warning")
    }

    private fun resolve(chase: HoundEncounter, start: Float, lane: Int, x: Float, y: Float, flying: Boolean): Pair<Float, Int> {
        var time = start
        var event = HoundEncounter.EVENT_NONE
        repeat(40) {
            if (event == HoundEncounter.EVENT_NONE) {
                time += .05f
                event = chase.update(time, .05f, 12.5f, lane, x, y, flying, true)
            }
        }
        return time to event
    }

    @Test fun sidestepJumpAndJetpackAreFairEscapesButIgnoringBothCuesHits() {
        val sidestep = HoundEncounter()
        var time = reachLunge(sidestep)
        var result = resolve(sidestep, time, 2, Lanes.x(2), .45f, false).second
        assertEquals(HoundEncounter.EVENT_ESCAPED, result)
        assertEquals(3, sidestep.alert)
        assertFalse(sidestep.active)

        val jump = HoundEncounter()
        time = reachLunge(jump)
        result = resolve(jump, time, 1, Lanes.x(1), 1.2f, false).second
        assertEquals(HoundEncounter.EVENT_ESCAPED, result)

        val jet = HoundEncounter()
        time = reachLunge(jet)
        result = resolve(jet, time, 1, Lanes.x(1), 5.2f, true).second
        assertEquals(HoundEncounter.EVENT_ESCAPED, result)

        val hit = HoundEncounter()
        time = reachLunge(hit)
        result = resolve(hit, time, 1, Lanes.x(1), .45f, false).second
        assertEquals(HoundEncounter.EVENT_HIT, result)
        assertEquals(4, hit.alert)
    }

    @Test fun bonusTransitionCancelsTheEncounterAndRunCanSpawnAtMostTwo() {
        val canceled = HoundEncounter()
        assertEquals(HoundEncounter.EVENT_STARTED,
            canceled.update(30f, 0f, 12.5f, 1, 0f, .45f, false, true))
        canceled.update(30.1f, .1f, 12.5f, 1, 0f, .45f, false, false)
        assertFalse(canceled.active)
        assertFalse(canceled.visible)
        assertEquals(0, canceled.alert)

        val chase = HoundEncounter()
        var time = reachLunge(chase)
        val firstHit = resolve(chase, time, 1, Lanes.x(1), .45f, false)
        assertEquals(HoundEncounter.EVENT_HIT, firstHit.second)
        time = firstHit.first
        repeat(100) {
            time += .05f
            chase.update(time, .05f, 12.5f, 1, Lanes.x(1), .45f, false, true)
        }
        val secondStart = firstHit.first + 92.1f
        assertEquals(HoundEncounter.EVENT_STARTED,
            chase.update(secondStart, 0f, 12.5f, 1, Lanes.x(1), .45f, false, true))
        time = secondStart
        var lunge = HoundEncounter.EVENT_NONE
        repeat(100) {
            if (lunge == HoundEncounter.EVENT_NONE) {
                time += .05f
                lunge = chase.update(time, .05f, 12.5f, 1, Lanes.x(1), .45f, false, true)
            }
        }
        assertEquals(HoundEncounter.EVENT_LUNGE, lunge)
        val secondHit = resolve(chase, time, 1, Lanes.x(1), .45f, false)
        assertEquals(HoundEncounter.EVENT_HIT, secondHit.second)
        time = secondHit.first
        repeat(100) {
            time += .05f
            chase.update(time, .05f, 12.5f, 1, Lanes.x(1), .45f, false, true)
        }
        assertEquals(HoundEncounter.EVENT_NONE,
            chase.update(time + 100f, 0f, 12.5f, 1, Lanes.x(1), .45f, false, true))
    }
}
