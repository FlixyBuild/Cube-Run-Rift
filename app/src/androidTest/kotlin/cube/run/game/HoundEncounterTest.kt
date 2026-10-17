package cube.run.game

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Deterministic tests for the warning window, escape rules, impact, and bounded encounter schedule. */
@RunWith(AndroidJUnit4::class)
class HoundEncounterTest {
    @Before fun resetLanes() = Lanes.reset()

    private fun advanceToLunge(chase: HoundEncounter, start: Float = 30f): Float {
        assertEquals(HoundEncounter.EVENT_STARTED,
            chase.update(start, 0f, 12.5f, 1, Lanes.x(1), .45f, false, true))
        assertEquals("Approach has an explicit warning state", 1, chase.alert)
        var t = start
        repeat(200) {
            t += .05f
            val event = chase.update(t, .05f, 12.5f, 1, Lanes.x(1), .45f, false, true)
            if (event == HoundEncounter.EVENT_LUNGE) {
                assertEquals("Committed lunge is separately cued", 2, chase.alert)
                return t
            }
        }
        throw AssertionError("Hound failed to reach its telegraphed lunge")
    }

    @Test fun laneChangeDuringTheTelegraphIsARealEscape() {
        val chase = HoundEncounter()
        var t = advanceToLunge(chase)
        var result = 0
        repeat(40) {
            if (result == HoundEncounter.EVENT_NONE) {
                t += .05f
                result = chase.update(t, .05f, 12.5f, 2, Lanes.x(2), .45f, false, true)
            }
        }
        assertEquals(HoundEncounter.EVENT_ESCAPED, result)
        assertEquals(3, chase.alert)
        assertFalse(chase.active)
    }

    @Test fun aJumpAlsoClearsTheLowHoundAndAPlayerWhoDoesNeitherIsHit() {
        val jumping = HoundEncounter()
        var t = advanceToLunge(jumping)
        var result = 0
        repeat(40) {
            if (result == HoundEncounter.EVENT_NONE) {
                t += .05f
                result = jumping.update(t, .05f, 12.5f, 1, Lanes.x(1), 1.2f, false, true)
            }
        }
        assertEquals(HoundEncounter.EVENT_ESCAPED, result)

        val caught = HoundEncounter()
        t = advanceToLunge(caught)
        result = 0
        repeat(40) {
            if (result == HoundEncounter.EVENT_NONE) {
                t += .05f
                result = caught.update(t, .05f, 12.5f, 1, Lanes.x(1), .45f, false, true)
            }
        }
        assertEquals("Standing in the committed lane is the only fatal case", HoundEncounter.EVENT_HIT, result)
        assertEquals(4, caught.alert)
    }

    @Test fun powerFlightAndAWorldTransitionCannotLeaveAnUncuedActiveHound() {
        val flying = HoundEncounter()
        var t = advanceToLunge(flying)
        var result = 0
        repeat(40) {
            if (result == HoundEncounter.EVENT_NONE) {
                t += .05f
                result = flying.update(t, .05f, 12.5f, 1, Lanes.x(1), 5.2f, true, true)
            }
        }
        assertEquals(HoundEncounter.EVENT_ESCAPED, result)

        val transitioned = HoundEncounter()
        assertEquals(HoundEncounter.EVENT_STARTED,
            transitioned.update(30f, 0f, 12.5f, 1, 0f, .45f, false, true))
        transitioned.update(30.1f, .1f, 12.5f, 1, 0f, .45f, false, false)
        assertFalse(transitioned.active)
        assertEquals(0, transitioned.alert)
        assertFalse(transitioned.visible)
    }

    @Test fun onlyTwoEncountersCanStartAndEachIsSeparatedByCooldown() {
        val chase = HoundEncounter()
        var t = advanceToLunge(chase)
        var result = 0
        repeat(40) {
            if (result == HoundEncounter.EVENT_NONE) {
                t += .05f
                result = chase.update(t, .05f, 12.5f, 1, Lanes.x(1), .45f, false, true)
            }
        }
        assertEquals(HoundEncounter.EVENT_HIT, result)
        val firstHitAt = t
        // Let the first hound clear the camera; the next encounter is time-gated.
        repeat(100) {
            t += .05f
            chase.update(t, .05f, 12.5f, 1, Lanes.x(1), .45f, false, true)
        }
        val secondStart = firstHitAt + 92.1f
        assertEquals(HoundEncounter.EVENT_STARTED,
            chase.update(secondStart, 0f, 12.5f, 1, Lanes.x(1), .45f, false, true))
        t = secondStart
        result = 0
        repeat(200) {
            if (result == HoundEncounter.EVENT_NONE) {
                t += .05f
                result = chase.update(t, .05f, 12.5f, 1, Lanes.x(1), .45f, false, true)
            }
        }
        assertEquals(HoundEncounter.EVENT_LUNGE, result)
        result = 0
        repeat(40) {
            if (result == HoundEncounter.EVENT_NONE) {
                t += .05f
                result = chase.update(t, .05f, 12.5f, 1, Lanes.x(1), .45f, false, true)
            }
        }
        assertEquals(HoundEncounter.EVENT_HIT, result)
        repeat(100) {
            t += .05f
            chase.update(t, .05f, 12.5f, 1, Lanes.x(1), .45f, false, true)
        }
        assertEquals("Encounter limit prevents unbounded repeated spawns", HoundEncounter.EVENT_NONE,
            chase.update(t + 100f, 0f, 12.5f, 1, Lanes.x(1), .45f, false, true))
    }
}
