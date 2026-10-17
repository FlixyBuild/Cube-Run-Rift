package cube.run.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CampaignWorldsTest {
    @Test fun galaxyAndGlitchNexusBonusRoutesUnlockAsIntended() {
        assertEquals(Bonus.SPACE, Bonus.GALAXY)
        assertEquals("Galaxy", Bonus.get(Bonus.GALAXY).name)
        assertTrue(Bonus.unlocked(0).any { it.id == Bonus.GALAXY })
        assertFalse(Bonus.unlocked(519).any { it.id == Bonus.NEXUS })
        assertTrue(Bonus.unlocked(520).any { it.id == Bonus.NEXUS })
    }

    @Test fun activeCampaignReplacesSpaceWithGalaxyAndRetainsTheOtherMaps() {
        assertEquals(listOf(Worlds.JUNGLE_ID, Worlds.NEON_CITY_ID, Worlds.LAVA_ID, Worlds.WINTER_ID,
            Worlds.RAILWAY_ID, Worlds.GALAXY_ID, Worlds.BEACH_ID, Worlds.NIGHT_ID,
            Worlds.HACKER_ID, Worlds.AIR_ID, Worlds.NEXUS_ID), Worlds.campaignIds)
        assertEquals(listOf("Jungle Canopy", "Neon City", "Lava Caves", "Frost Peaks", "Railway Yard",
            "Galaxy", "Tropical Beach", "Moonlit Wilds", "Matrix Circuit", "Air", "Glitch Nexus"),
            Worlds.campaign.map { it.name })
        assertFalse("Space must not be a second selectable map", Worlds.campaign.any { it.name == "Space" })
        assertTrue(Worlds.isCampaignWorld(Worlds.JUNGLE_ID))
        assertTrue(Worlds.isCampaignWorld(Worlds.NEON_CITY_ID))
        assertTrue(Worlds.isCampaignWorld(Worlds.WINTER_ID))
        assertTrue(Worlds.isCampaignWorld(Worlds.LAVA_ID))
        assertTrue(Worlds.isCampaignWorld(Worlds.RAILWAY_ID))
        assertTrue(Worlds.isCampaignWorld(Worlds.GALAXY_ID))
        assertTrue(Worlds.isCampaignWorld(Worlds.NEXUS_ID))
        assertTrue(Worlds.isCampaignWorld(Worlds.BEACH_ID))
        assertTrue(Worlds.isCampaignWorld(Worlds.NIGHT_ID))
        assertTrue(Worlds.isCampaignWorld(Worlds.HACKER_ID))
        assertTrue(Worlds.isCampaignWorld(Worlds.AIR_ID))
        assertFalse(Worlds.isCampaignWorld(Worlds.HIGHWAY_ID))
        assertEquals(Worlds.GALAXY_ID, Worlds.SPACE_ID)
        assertEquals("Galaxy", Worlds.get(Worlds.GALAXY_ID).name)
        assertEquals(Worlds.GALAXY_ID, Worlds.get(Worlds.GALAXY_ID).id)
    }
}
