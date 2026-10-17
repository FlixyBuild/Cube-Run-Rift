package cube.run.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PowerUpsTest {
    @Test fun skateboardIsTimedRefreshableAndResettable() {
        val powerUps = PowerUps()
        assertFalse(powerUps.skateboard.active)
        powerUps.skateboard.start(9f)
        assertTrue(powerUps.skateboard.active)
        assertFalse(powerUps.skateboard.tick(4f))
        assertEquals(5f, powerUps.skateboard.left, 0f)
        powerUps.skateboard.start(6f)
        assertEquals(6f, powerUps.skateboard.duration, 0f)
        assertTrue(powerUps.skateboard.tick(6f))
        assertFalse(powerUps.skateboard.active)
        powerUps.skateboard.start(2f)
        powerUps.reset()
        assertFalse(powerUps.skateboard.active)
    }

    @Test fun jumpShoesAreTimedRefreshableAndResettable() {
        val powerUps = PowerUps()
        assertFalse(powerUps.jumpShoes.active)
        powerUps.jumpShoes.start(10f)
        assertTrue(powerUps.jumpShoes.active)
        assertFalse(powerUps.jumpShoes.tick(3f))
        assertEquals(7f, powerUps.jumpShoes.left, 0f)
        powerUps.jumpShoes.start(5f)
        assertEquals(5f, powerUps.jumpShoes.duration, 0f)
        assertTrue(powerUps.jumpShoes.tick(5f))
        assertFalse(powerUps.jumpShoes.active)
        powerUps.jumpShoes.start(2f)
        powerUps.reset()
        assertFalse(powerUps.jumpShoes.active)
    }

    @Test fun phaseDriveAndTimeWarpRefreshAndClearBetweenRuns() {
        val powerUps = PowerUps()
        powerUps.phase.start(5f)
        powerUps.timeWarp.start(4f)
        assertTrue(powerUps.phase.active)
        assertTrue(powerUps.timeWarp.active)
        assertFalse(powerUps.phase.tick(2f))
        assertEquals(3f, powerUps.phase.left, 0f)
        assertTrue(powerUps.timeWarp.tick(4f))
        assertFalse(powerUps.timeWarp.active)
        powerUps.reset()
        assertFalse(powerUps.phase.active)
        assertFalse(powerUps.timeWarp.active)
    }
}
