package cube.run.game.world

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HelicopterFlightTest {
    @Test fun passIsHiddenOutsideItsScheduledWindow() {
        assertEquals(-1f, HelicopterFlight.progress(0f), 0f)
        assertEquals(-1f, HelicopterFlight.progress(16.99f), 0f)
        assertEquals(-1f, HelicopterFlight.progress(35f), 0f)
        assertEquals(-1f, HelicopterFlight.progress(Float.NaN), 0f)
    }

    @Test fun passMovesSmoothlyAcrossTheSkyAndRepeats() {
        assertEquals(0f, HelicopterFlight.progress(17f), 0.0001f)
        assertEquals(.5f, HelicopterFlight.progress(26f), 0.0001f)
        assertEquals(1f, HelicopterFlight.progress(34.999f), 0.0001f)
        assertEquals(0f, HelicopterFlight.progress(69f), 0.0001f)
        assertTrue(HelicopterFlight.x(0f) > HelicopterFlight.x(1f))
        assertEquals(0f, HelicopterFlight.bank(0f), 0.0001f)
        assertEquals(0f, HelicopterFlight.bank(1f), 0.0001f)
    }
}
