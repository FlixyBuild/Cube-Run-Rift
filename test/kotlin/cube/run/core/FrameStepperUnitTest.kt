package cube.run.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameStepperUnitTest {
    @Test fun nonFiniteDeltaIsIgnoredAndHitchesAreSlicedInsteadOfAppliedAtOnce() {
        val stepper = FrameStepper()
        assertEquals(0.0, stepper.take(Float.NaN, paused = false), 0.0)
        assertEquals(0.0, stepper.take(Float.POSITIVE_INFINITY, paused = false), 0.0)
        val first = stepper.take(10f, paused = false)
        assertTrue(first <= 32.0 / 60.0)
        assertTrue(stepper.lastSteps > 1)
        assertEquals(10.0 - first, stepper.pendingSeconds, .000001)
        assertEquals(0.0, stepper.take(5f, paused = true), 0.0)
        assertEquals(0.0, stepper.pendingSeconds, 0.0)
    }
}
