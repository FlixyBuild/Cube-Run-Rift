package cube.run.game.track

import cube.run.data.Bonus
import cube.run.game.Lanes
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class TrackJetpackIntroTest {
    @Before fun resetLanes() = Lanes.reset()
    @After fun restoreLanes() = Lanes.reset()

    private fun track(seed: Int): Track {
        val random = Random(seed)
        return Track(random, ObstacleFactory(random)).apply { reset(.2f, 205f) }
    }

    private fun generate(track: Track, until: Int, score: Int) {
        while (track.rowsSpawned < until) track.spawn(1f, 205f, score)
    }

    @Test fun firstJetpackIsVisibleBeforeScoreOneHundredAndLaterDropsKeepTheirGate() {
        for (seed in 0..12) {
            val track = track(seed)
            generate(track, 400, score = 0)
            val earlyJets = track.rows.count { it.pickup == Pickup.JET }
            assertEquals("Only the one-time intro jetpack appears before score 100 (seed $seed)", 1, earlyJets)
            assertTrue("Mystery boxes remain locked before score 100 (seed $seed)", track.rows.none { it.pickup == Pickup.BOX })

            val beforeUnlock = track.rows.size
            generate(track, 1000, score = 100)
            assertTrue("Later jetpacks return after the normal score gate (seed $seed)",
                track.rows.drop(beforeUnlock).any { it.pickup == Pickup.JET })
            assertTrue("Mystery boxes return after the normal score gate (seed $seed)",
                track.rows.drop(beforeUnlock).any { it.pickup == Pickup.BOX })

            track.reset(.2f, 205f, initialBonus = Bonus.NONE)
            generate(track, 400, score = 0)
            assertEquals("The one-time introduction resets for each new run", 1,
                track.rows.count { it.pickup == Pickup.JET })
        }
    }
}
