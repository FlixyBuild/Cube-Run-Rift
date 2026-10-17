package cube.run.game.world

import cube.run.game.track.CoinType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JungleHarvestTest {
    @Test fun missionCountsOnlyGreenAndPaysExactlyOnce() {
        val harvest = JungleHarvest(target = 2)
        assertFalse(harvest.collect(CoinType.GREEN))
        harvest.enter(isJungle = true)
        assertFalse(harvest.collect(CoinType.RED))
        assertFalse(harvest.collect(CoinType.BLUE))
        assertFalse(harvest.collect(CoinType.GREEN))
        assertTrue(harvest.active)
        assertEquals(1, harvest.collected)
        assertTrue(harvest.collect(CoinType.GREEN))
        assertTrue(harvest.completed)
        assertEquals(1f, harvest.progress, 0f)
        assertFalse(harvest.collect(CoinType.GREEN))
    }

    @Test fun leavingJungleDisablesAndResetsTheObjective() {
        val harvest = JungleHarvest(target = 3)
        harvest.enter(isJungle = true)
        harvest.collect(CoinType.GREEN)
        harvest.enter(isJungle = false)
        assertFalse(harvest.active)
        assertEquals(0, harvest.collected)
        assertFalse(harvest.collect(CoinType.GREEN))
    }
}
