package cube.run.game.track

import cube.run.data.Bonus
import cube.run.data.Worlds
import cube.run.game.Lanes
import cube.run.game.space.SpaceEscapeClock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class TrackPortalSafetyTest {
    @Before fun resetLanes() = Lanes.reset()
    @After fun restoreLanes() = Lanes.reset()

    @Test fun everyExitReturnsToThreeLanesAndClearsTheTrackBonus() {
        val random = Random(17)
        val track = Track(random, ObstacleFactory(random))
        track.forceBonus(Bonus.WIDE)
        assertEquals(5, Lanes.count)

        val exit = Row(0f, ArrayList()).apply {
            portal = Bonus.WIDE
            portalExit = true
        }
        assertEquals(Bonus.NONE, track.crossPortal(exit))
        assertEquals(Bonus.NONE, track.bonus)
        assertEquals(3, Lanes.count)
        assertEquals(Lanes.NORMAL_W, Lanes.targetW, 0f)
    }

    @Test fun anInvalidPortalIdFailsClosedInsteadOfIndexingTheBonusCatalog() {
        val random = Random(23)
        val track = Track(random, ObstacleFactory(random))
        val corrupted = Row(0f, ArrayList()).apply { portal = 99 }

        assertEquals(Bonus.NONE, track.crossPortal(corrupted))
        assertEquals(Bonus.NONE, track.bonus)
        assertEquals(3, Lanes.count)
        assertEquals(Lanes.NORMAL_W, Lanes.targetW, 0f)
    }

    @Test fun everyBonusWorldAutomaticallyQueuesAnExitPortal() {
        for (world in Bonus.all) {
            Lanes.reset()
            val random = Random(900 + world.id)
            val track = Track(random, ObstacleFactory(random)).apply { portalPool = emptyList() }
            track.reset(0.16f, world.hue, initialBonus = world.id)
            var ticks = 0
            while (track.rows.none { it.portalExit } && ticks++ < 3_000) {
                track.spawn(1f, world.hue, score = 0, dt = 1f / 60f)
            }
            assertTrue("${world.name} did not schedule its automatic return gate", track.rows.any { it.portalExit })
        }
    }

    @Test fun portalSafetyClearsNearbyRowsAndStreamsScorelessRowsWithoutPickups() {
        val random = Random(37)
        val track = Track(random, ObstacleFactory(random))
        track.reset(0.16f, 210f)
        track.beginPortalSafety(18f)

        assertTrue(track.rows.filter { it.z in -20f..1f }.all { row ->
            row.obs.isEmpty() && row.pickup == cube.run.game.track.Pickup.NONE && row.coins == null && row.scoreless
        })

        val before = track.rows.toSet()
        track.spawn(120f, 210f, score = 0, dt = 1f / 60f)
        val streamed = track.rows.filter { it !in before }
        assertTrue("Expected rows to be streamed during the portal corridor", streamed.isNotEmpty())
        assertTrue(streamed.all { it.obs.isEmpty() && it.pickup == cube.run.game.track.Pickup.NONE && it.coins == null && it.scoreless })
    }

    @Test fun safeArrivalKeepsTheBlackHoleClockAndMovingExitGateAligned() {
        val random = Random(43)
        val track = Track(random, ObstacleFactory(random)).apply { portalPool = emptyList() }
        track.reset(0.16f, 210f)
        track.beginPortalSafety(20f)
        val gate = track.addChallengeExit(150f)
        val clock = SpaceEscapeClock(durationSeconds = 14.5f, routeDistance = 150f)
        clock.start()

        val movedDuringGrace = 18.6f
        track.scroll(movedDuringGrace, time = 0f, dt = 1.5f)
        assertEquals(SpaceEscapeClock.State.ACTIVE,
            clock.advance(1.5f, movedDuringGrace, countSeconds = false))
        assertEquals("The escape distance must match the gate's live track position", -gate.z, clock.distanceLeft, .0001f)
        assertEquals(14.5f, clock.secondsLeft, 0f)
    }

    @Test fun escapePortalKeepsTheOriginBiomeFrameStyle() {
        val random = Random(41)
        val track = Track(random, ObstacleFactory(random)).apply { themeWorldId = Worlds.RAILWAY_ID }
        val gate = track.addChallengeExit(80f)
        assertEquals(Worlds.RAILWAY, gate.portalStyle)
        assertTrue(gate.portalExit)
    }
}
