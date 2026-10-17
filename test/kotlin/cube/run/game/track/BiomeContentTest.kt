package cube.run.game.track

import cube.run.data.Worlds
import cube.run.game.space.SpaceSections
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BiomeContentTest {
    @Test fun airRouteIncludesBirdEncountersAndDistinctEagleAndFlockArt() {
        assertTrue(Worlds.isCampaignWorld(Worlds.AIR_ID))
        assertTrue(AirSections.pool.any { it.name == "EAGLE DIVE" && it.steps.contains(Step.SW) })
        assertTrue(AirSections.pool.any { it.name == "FLOCK CROSSING" })
        assertTrue(AirSections.pool.any { it.steps.any { step -> Step.isJump(step) } })
        assertEquals(AirBird.EAGLE, AirBirdSelection.choose(0, 0, ObAnim.NONE, 1.5f))
        assertEquals(AirBird.GULL, AirBirdSelection.choose(1, 0, ObAnim.NONE, 1.5f))
        assertEquals(AirBird.HAWK, AirBirdSelection.choose(2, 0, ObAnim.NONE, 1.5f))
        assertEquals(AirBird.FLOCK, AirBirdSelection.choose(0, 0, ObAnim.NONE, 4.2f))
        assertEquals(AirBird.EAGLE, AirBirdSelection.choose(0, 0, ObAnim.SWEEP, 1.5f))
        assertEquals(ObstacleArt.AIR_EAGLE,
            ObstacleArtResolver.resolve(Worlds.AIR, ObType.SOLID, ObAnim.NONE, ObCue.NONE, false, 1.5f, 1.8f, bird = AirBird.EAGLE))
        assertEquals(ObstacleArt.AIR_FLOCK,
            ObstacleArtResolver.resolve(Worlds.AIR, ObType.SOLID, ObAnim.NONE, ObCue.NONE, false, 4.2f, 1.8f, bird = AirBird.FLOCK))
    }

    @Test fun cityAndWinterPoolsIncludeDodgeJumpAndRollPatterns() {
        assertTrue(Sections.cityPool.any { section -> section.steps.any { Step.isJump(it) } })
        assertTrue(Sections.cityPool.any { section -> section.steps.any { Step.isDuck(it) } })
        assertTrue(Sections.cityPool.any { section -> section.steps.any { it in 10..22 } })

        assertTrue(Sections.winterPool.any { section -> section.steps.any { Step.isJump(it) } })
        assertTrue(Sections.winterPool.any { section -> section.steps.any { Step.isDuck(it) } })
        assertTrue(Sections.winterPool.any { section -> section.steps.any { it in 10..22 } })
    }

    @Test fun cityAndWinterObstaclesResolveToDistinctReadableArt() {
        assertEquals(
            ObstacleArt.CITY_CAR,
            ObstacleArtResolver.resolve(Worlds.TOWERS, ObType.SOLID, ObAnim.NONE, ObCue.NONE, false, 1.4f, 1f),
        )
        assertEquals(
            ObstacleArt.CITY_BIKE,
            ObstacleArtResolver.resolve(Worlds.TOWERS, ObType.SOLID, ObAnim.SWEEP, ObCue.NONE, false, 1.4f, 1f),
        )
        assertEquals(
            ObstacleArt.WINTER_ICICLE,
            ObstacleArtResolver.resolve(Worlds.SPIKES, ObType.SOLID, ObAnim.NONE, ObCue.DUCK, false, 1.1f, .55f),
        )
        assertEquals(
            ObstacleArt.WINTER_ICE_HOLE,
            ObstacleArtResolver.resolve(Worlds.SPIKES, ObType.SOLID, ObAnim.NONE, ObCue.NONE, true, 3f, .8f),
        )
    }

    @Test fun nightAndHackerWorldsHaveTheirOwnRoutesAndObstacleArt() {
        assertTrue(NightSections.pool.any { it.steps.any { step -> step == Step.ft(0) } })
        assertTrue(NightSections.pool.any { it.steps.any { step -> step == Step.ft(2) } })
        assertTrue(HackerSections.pool.any { it.steps.any { step -> step == Step.ft(0) } })
        assertTrue(HackerSections.pool.any { it.steps.any { step -> step == Step.ft(2) } })
        assertTrue(HackerSections.pool.any { it.name.contains("FORK") || it.name.contains("BRANCH") })

        assertEquals(
            ObstacleArt.NIGHT_WOLF,
            ObstacleArtResolver.resolve(Worlds.NIGHT, ObType.SOLID, ObAnim.NONE, ObCue.NONE, false, 1.5f, 1.8f, RoadAnimal.WOLF),
        )
        assertEquals(
            ObstacleArt.NIGHT_TIGER,
            ObstacleArtResolver.resolve(Worlds.NIGHT, ObType.SOLID, ObAnim.NONE, ObCue.NONE, false, 1.5f, 1.8f, RoadAnimal.TIGER),
        )
        assertEquals(
            ObstacleArt.HACKER_FIREWALL,
            ObstacleArtResolver.resolve(Worlds.HACKER, ObType.SOLID, ObAnim.NONE, ObCue.JUMP, false, 5f, .62f),
        )
        assertEquals(
            ObstacleArt.HACKER_DRONE,
            ObstacleArtResolver.resolve(Worlds.HACKER, ObType.SOLID, ObAnim.PENDULUM, ObCue.NONE, false, 1.5f, 1f),
        )
    }

    @Test fun lavaAndNexusHaveIndependentRoutesAndObstacleSets() {
        assertTrue(Worlds.isCampaignWorld(Worlds.LAVA_ID))
        assertTrue(Worlds.isCampaignWorld(Worlds.NEXUS_ID))
        assertTrue(LavaSections.pool.any { it.name == "GEYSER LEAP" })
        assertTrue(LavaSections.pool.any { it.steps.any { step -> step in 120..122 } })
        assertTrue(NexusSections.pool.any { it.name == "ROOT OVERRIDE" })
        assertTrue(NexusSections.pool.any { it.steps.any { step -> step in 20..22 } })
        assertTrue(Sections.byId(280) != null)
        assertTrue(Sections.byId(290) != null)
        assertEquals(Worlds.HACKER, Worlds.get(Worlds.HACKER_ID).deco)
        assertEquals(Worlds.NEXUS, Worlds.get(Worlds.NEXUS_ID).deco)
        assertEquals(ObstacleArt.LAVA_VENT,
            ObstacleArtResolver.resolve(Worlds.CRYSTALS, ObType.SOLID, ObAnim.PISTON, ObCue.NONE, false, 1.2f, 1.4f))
        assertEquals(ObstacleArt.LAVA_CRUST,
            ObstacleArtResolver.resolve(Worlds.CRYSTALS, ObType.SOLID, ObAnim.NONE, ObCue.NONE, true, 3.4f, .3f))
        assertEquals(ObstacleArt.NEXUS_DRONE,
            ObstacleArtResolver.resolve(Worlds.NEXUS, ObType.SOLID, ObAnim.PENDULUM, ObCue.NONE, false, 1.4f, .8f))
        assertEquals(ObstacleArt.NEXUS_VOID,
            ObstacleArtResolver.resolve(Worlds.NEXUS, ObType.SOLID, ObAnim.NONE, ObCue.NONE, true, 3.4f, .3f))
    }

    @Test fun beachAndRailwayHaveAuthoredRoutesAndMapSpecificHazards() {
        assertTrue(BeachSections.pool.any { it.name == "CRAB CROSSING" })
        assertTrue(BeachSections.pool.any { it.steps.any { step -> step in 120..122 } })
        assertTrue(RailwaySections.pool.any { it.name == "NIGHT EXPRESS" })
        assertTrue(RailwaySections.pool.any { it.steps.any { step -> step in 50..52 } })
        assertTrue(Sections.explorerPool.any { it.id == 300 })
        assertTrue(Sections.explorerPool.any { it.id == 310 })
        assertEquals(ObstacleArt.BEACH_CRAB,
            ObstacleArtResolver.resolve(Worlds.BEACH, ObType.SOLID, ObAnim.PENDULUM, ObCue.NONE, false, 1.5f, 1f))
        assertEquals(ObstacleArt.BEACH_TIDAL_GAP,
            ObstacleArtResolver.resolve(Worlds.BEACH, ObType.SOLID, ObAnim.NONE, ObCue.NONE, true, 3.2f, .4f))
        assertEquals(ObstacleArt.BEACH_SURF,
            ObstacleArtResolver.resolve(Worlds.BEACH, ObType.SOLID, ObAnim.STOMP, ObCue.NONE, false, 2f, 1.3f))
    }

    @Test fun spaceCampaignAddsGravityMinesAsAReadableNewHazard() {
        assertTrue(SpaceSections.pool.any { section -> section.steps.any { it in 217..219 } })
        assertEquals(
            ObstacleArt.SPACE_GRAVITY_MINE,
            ObstacleArtResolver.resolve(Worlds.RINGS, ObType.SOLID, ObAnim.NONE, ObCue.SPACE_MINE, false, 1.36f, 1.32f),
        )
    }

    @Test fun selectableGalaxyUsesItsOwnNormalGravityRouteInsteadOfBonusSpacePatterns() {
        assertEquals(5, Worlds.GALAXY_ID)
        assertTrue(Worlds.isCampaignWorld(Worlds.GALAXY_ID))
        assertTrue(GalaxySections.pool.size >= 8)
        assertTrue(GalaxySections.pool.any { it.name == "ASTEROID BELT" })
        assertTrue(GalaxySections.pool.any { it.steps.any { step -> Step.isJump(step) } })
        assertTrue(GalaxySections.pool.any { it.steps.any { step -> Step.isDuck(step) } })
        assertTrue(GalaxySections.pool.flatMap { it.steps.toList() }.none { step ->
            step in 200..219 || step == Step.RF || step == Step.HW || step == Step.CT
        })
        assertTrue(Sections.byId(320) != null)
        assertTrue(Sections.byId(327) != null)
        assertTrue(Sections.explorerPool.any { it.id == 320 })
        assertTrue(LavaSections.pool.any { it.id == 287 && it.steps.any { step -> step in 200..202 } })
        assertTrue(Sections.winterPool.any { it.id == 199 })
    }
}
