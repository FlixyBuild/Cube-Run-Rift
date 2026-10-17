package cube.run.game.space

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GalaxyAtlasTest {
    @Test fun everyTripSchedulesAllEightNamedPlanetsInTheFirstAtlas() {
        assertEquals(listOf("Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune"),
            SpaceTrip.PLANET_NAMES)
        assertEquals(8, SpaceTrip.PLANET_LOOKS.size)
        val trip = SpaceTrip(0x4711)
        val planets = trip.landmarks.filter { it.kind == SpaceTrip.PLANET && it.at < SpaceTrip.ATLAS_REPEAT_DISTANCE }

        assertEquals(8, planets.size)
        assertEquals((0..7).toList(), planets.map { it.planetIndex })
        assertTrue(planets.zipWithNext().all { (a, b) -> b.at > a.at })
        assertTrue("The timed singularity remains part of the Galaxy journey", trip.firstBlackHoleAt.isFinite())
    }

    @Test fun everyPlanetHasItsOwnDistinctPaletteAndSatellitesArePresent() {
        val bodyHues = SpaceTrip.PLANET_LOOKS.map { it[0] }
        assertEquals(8, bodyHues.toSet().size)

        val trip = SpaceTrip(0x5A7)
        assertTrue(trip.landmarks.any { it.kind == SpaceTrip.MOONS })
        assertTrue(trip.landmarks.any { it.kind == SpaceTrip.STATION })
        assertTrue(trip.landmarks.any { it.kind == SpaceTrip.SUN })
        assertFalse(trip.landmarks.filter { it.kind == SpaceTrip.PLANET }.any { it.planetIndex !in 0..7 })
        assertTrue(trip.weather.any { it.kind == SpaceTrip.SHOWER || it.kind == SpaceTrip.NEBULA_CLOUD })
    }

    @Test fun randomizedTripsKeepTheFullFirstAtlasAndSingularity() {
        for (seed in 0..24) {
            val trip = SpaceTrip(seed)
            val firstAtlas = trip.landmarks.filter { it.kind == SpaceTrip.PLANET && it.at < SpaceTrip.ATLAS_REPEAT_DISTANCE }
            assertEquals("Missing planet for seed $seed", 8, firstAtlas.size)
            assertTrue("Missing singularity for seed $seed", trip.firstBlackHoleAt.isFinite())
        }
    }
}
