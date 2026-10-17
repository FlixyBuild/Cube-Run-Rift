package cube.run.game.world

import cube.run.data.Worlds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneryMotionTest {
    @Test fun planeCrossesTheSkyOnARepeatableOffscreenToOffscreenCycle() {
        assertTrue(PlaneFlight.START + PlaneFlight.DURATION < PlaneFlight.PERIOD)
        assertEquals(0f, PlaneFlight.progress(PlaneFlight.START), 0f)
        assertEquals(1f, PlaneFlight.progress(PlaneFlight.START + PlaneFlight.DURATION - .001f), .001f)
        assertEquals(-1f, PlaneFlight.progress(PlaneFlight.START + PlaneFlight.DURATION), 0f)
        assertEquals(0f, PlaneFlight.progress(PlaneFlight.PERIOD + PlaneFlight.START), 0f)
        assertEquals(-25f, PlaneFlight.x(0f), 0f)
        assertEquals(25f, PlaneFlight.x(1f), 0f)
        assertEquals(0f, PlaneFlight.bank(0f), 0f)
        assertEquals(0f, PlaneFlight.bank(1f), .0001f)
    }

    @Test fun winterSnowFallsDownwardOntoTheRoadAndRepeatsWithStaggeredPhases() {
        val start = Snowfall.y(0f, 0f)
        val later = Snowfall.y(1f, 0f)
        assertTrue(later < start)
        assertEquals(start, Snowfall.y(1f / Snowfall.FALLS_PER_SECOND, 0f), .0001f)
        assertEquals(Snowfall.TOP - .99f * Snowfall.TRAVEL, Snowfall.y(.99f / Snowfall.FALLS_PER_SECOND, 0f), .0001f)
        assertNotEquals(Snowfall.y(1f, 0f), Snowfall.y(1f, .5f), .001f)
        for (t in 0..100) for (phase in 0..10) {
            assertTrue(Snowfall.y(t / 10f, phase / 10f) in (Snowfall.TOP - Snowfall.TRAVEL)..Snowfall.TOP)
        }
    }

    @Test fun lavaDropsFallFromAboveAndLoopWithoutAllocatingNewParticles() {
        val start = FallingLava.y(0f, 0f)
        assertTrue(FallingLava.y(.5f, 0f) < start)
        assertEquals(start, FallingLava.y(1f / FallingLava.FALLS_PER_SECOND, 0f), .0001f)
        assertEquals(FallingLava.TOP - .99f * FallingLava.TRAVEL,
            FallingLava.y(.99f / FallingLava.FALLS_PER_SECOND, 0f), .0001f)
    }

    @Test fun tropicalBeachUsesItsOwnRoadsideStyleAndPalette() {
        val beach = Worlds.get(Worlds.BEACH_ID)
        assertEquals("Tropical Beach", beach.name)
        assertEquals(Worlds.BEACH, beach.deco)
        assertTrue(Worlds.isCampaignWorld(Worlds.BEACH_ID))
        assertNotEquals(Worlds.get(Worlds.NEON_CITY_ID).hue, beach.hue, 0f)
        assertEquals(11, Worlds.campaign.size)
    }
}
