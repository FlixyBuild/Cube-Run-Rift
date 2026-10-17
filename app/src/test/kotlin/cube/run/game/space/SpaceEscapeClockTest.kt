package cube.run.game.space

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Deterministic edge coverage for the timed singularity route. */
class SpaceEscapeClockTest {
    @Test fun startsOnceAndEscapeWinsADeadlineTie() {
        val clock = SpaceEscapeClock(durationSeconds = 14.5f, routeDistance = 150f)
        assertEquals(SpaceEscapeClock.State.READY, clock.state)
        assertTrue(clock.start())
        assertFalse(clock.start())
        assertEquals(SpaceEscapeClock.State.ESCAPED, clock.advance(14.5f, 150f))
        assertEquals(0f, clock.secondsLeft, 0f)
        assertEquals(0f, clock.distanceLeft, 0f)
        assertEquals(1f, clock.routeProgress, 0f)
    }

    @Test fun safeArrivalMovesTheRiftWithTheTrackButPausesTheDeadline() {
        val clock = SpaceEscapeClock(durationSeconds = 14.5f, routeDistance = 150f)
        clock.start()
        assertEquals(SpaceEscapeClock.State.ACTIVE, clock.advance(1.5f, 18.6f, countSeconds = false))
        assertEquals(14.5f, clock.secondsLeft, 0f)
        assertEquals(131.4f, clock.distanceLeft, .0001f)
    }

    @Test fun deadlineCapturesIfTheRiftWasNotReached() {
        val clock = SpaceEscapeClock(durationSeconds = 14.5f, routeDistance = 150f)
        clock.start()
        assertEquals(SpaceEscapeClock.State.CAPTURED, clock.advance(14.5f, 149.9f))
        assertEquals(0f, clock.timerProgress, 0f)
    }

    @Test fun resetAndInvalidDeltasCannotAdvanceTheEncounter() {
        val clock = SpaceEscapeClock()
        clock.start()
        assertEquals(SpaceEscapeClock.State.ACTIVE, clock.advance(Float.NaN, Float.POSITIVE_INFINITY))
        assertEquals(clock.durationSeconds, clock.secondsLeft, 0f)
        assertEquals(clock.routeDistance, clock.distanceLeft, 0f)
        clock.advance(1f, 1f)
        clock.reset()
        assertEquals(SpaceEscapeClock.State.READY, clock.state)
        assertEquals(clock.durationSeconds, clock.secondsLeft, 0f)
        assertEquals(clock.routeDistance, clock.distanceLeft, 0f)
    }
}
