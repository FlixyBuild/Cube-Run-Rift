package cube.run.game.world

import cube.run.data.Bonus
import cube.run.data.Worlds
import cube.run.game.track.CoinType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldMissionTest {
    @Test fun cityRequiresBlueCoinsAndCompletesOnlyOnce() {
        val mission = WorldMission(cityCoinTarget = 2)
        mission.enterWorld(Worlds.CITY_ID)
        assertFalse(mission.collectCoin(CoinType.RED))
        assertFalse(mission.collectCoin(CoinType.GREEN))
        assertFalse(mission.collectCoin(CoinType.BLUE))
        assertEquals(1, mission.collected)
        assertTrue(mission.collectCoin(CoinType.BLUE))
        assertTrue(mission.completed)
        assertEquals(1f, mission.progress, 0f)
        assertFalse(mission.collectCoin(CoinType.BLUE))
    }

    @Test fun winterProgressesOnScoredRowsAndLeavingClearsIt() {
        val mission = WorldMission(winterRowTarget = 2)
        mission.enterWorld(Worlds.WINTER_ID)
        assertFalse(mission.collectCoin(CoinType.BLUE))
        assertFalse(mission.scoreRow())
        assertTrue(mission.scoreRow())
        assertTrue(mission.completed)
        mission.enterWorld(Worlds.GALAXY_ID)
        assertFalse(mission.active)
        assertEquals(0, mission.collected)
    }

    @Test fun nightAndHackerObjectivesAdvanceOnlyOnTheirThemedChallenges() {
        val night = WorldMission(nightPredatorTarget = 2)
        night.enterWorld(Worlds.NIGHT_ID)
        assertFalse(night.collectCoin(CoinType.MIXED))
        assertFalse(night.evadePredator())
        assertTrue(night.evadePredator())
        assertTrue(night.completed)

        val hacker = WorldMission(hackerDataTarget = 2)
        hacker.enterWorld(Worlds.HACKER_ID)
        assertFalse(hacker.collectCoin(CoinType.RED))
        assertFalse(hacker.collectCoin(CoinType.BLUE))
        assertFalse(hacker.collectCoin(CoinType.MIXED))
        assertTrue(hacker.collectCoin(CoinType.MIXED))
        assertTrue(hacker.completed)
    }

    @Test fun galaxyObjectivePausesInsideOtherBonusRoutesAndResumesOnReturn() {
        assertTrue(WorldMissionRoute.usesGalaxyMission(Bonus.NONE, Worlds.GALAXY_ID))
        assertTrue(WorldMissionRoute.usesGalaxyMission(Bonus.GALAXY, Worlds.JUNGLE_ID))
        assertFalse(WorldMissionRoute.usesGalaxyMission(Bonus.NEXUS, Worlds.GALAXY_ID))
        assertFalse(WorldMissionRoute.usesGalaxyMission(Bonus.WIDE, Worlds.GALAXY_ID))
        assertFalse(WorldMissionRoute.usesGalaxyMission(Bonus.NONE, Worlds.NIGHT_ID))
    }

    @Test fun spaceMissionCountsAllColouredCoinsWithoutReplacingCampaignMissionState() {
        val city = WorldMission(cityCoinTarget = 2)
        val space = WorldMission(stardustCoinTarget = 3)
        city.enterWorld(Worlds.CITY_ID)
        city.collectCoin(CoinType.BLUE)
        space.enterSpace()
        assertFalse(space.collectCoin(CoinType.RED))
        assertFalse(space.collectCoin(CoinType.PINK))
        assertTrue(space.collectCoin(CoinType.MIXED))
        assertEquals(1, city.collected)
        assertEquals(3, space.collected)
    }
    @Test fun newBiomeMissionsAdvanceOnTheirOwnThemedEvents() {
        val lava = WorldMission(lavaCrystalTarget = 1)
        lava.enterWorld(Worlds.LAVA_ID)
        assertFalse(lava.collectCoin(CoinType.BLUE))
        assertTrue(lava.collectCoin(CoinType.PINK))

        val nexus = WorldMission(nexusFragmentTarget = 1)
        nexus.enterWorld(Worlds.NEXUS_ID)
        assertFalse(nexus.collectCoin(CoinType.GREEN))
        assertTrue(nexus.collectCoin(CoinType.MIXED))

        val jungle = WorldMission(dinosaurTarget = 1)
        jungle.enterWorld(Worlds.JUNGLE_ID)
        assertTrue(jungle.evadeDinosaur())

        val air = WorldMission(airBirdTarget = 1)
        air.enterWorld(Worlds.AIR_ID)
        assertTrue(air.evadeAirBird())
    }

    @Test fun beachAndRailwaySurvivalMissionsCountOnlyClearedRows() {
        val beach = WorldMission(beachRowTarget = 2)
        beach.enterWorld(Worlds.BEACH_ID)
        assertFalse(beach.scoreRow())
        assertTrue(beach.scoreRow())

        val railway = WorldMission(railwayRowTarget = 1)
        railway.enterWorld(Worlds.RAILWAY_ID)
        assertTrue(railway.scoreRow())
    }

}
