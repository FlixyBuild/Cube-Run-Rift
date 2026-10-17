package cube.run.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RiftRangerGaitTest {
    @Test fun limbsAlternateInAnOpposedRunningStride() {
        val time = 0.11f
        assertEquals(-RiftRangerGait.armSwing(time, 1f), RiftRangerGait.armSwing(time, -1f), .0001f)
        assertEquals(-RiftRangerGait.legSwing(time, 1f), RiftRangerGait.legSwing(time, -1f), .0001f)
        assertTrue(RiftRangerGait.armSwing(time, 1f) * RiftRangerGait.legSwing(time, 1f) < 0f)
        assertTrue(RiftRangerGait.footLift(time, 1f) >= 0f)
        assertTrue(RiftRangerGait.footLift(time, -1f) >= 0f)
    }

    @Test fun idleTimeIsFiniteAndMalformedTimeCannotPoisonThePose() {
        assertEquals(0f, RiftRangerGait.cycle(0f), .0001f)
        assertEquals(0f, RiftRangerGait.cycle(Float.NaN), 0f)
        assertEquals(0f, RiftRangerGait.armSwing(Float.POSITIVE_INFINITY, 1f), 0f)
    }
}
