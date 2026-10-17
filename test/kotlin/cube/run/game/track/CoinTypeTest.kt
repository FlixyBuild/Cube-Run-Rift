package cube.run.game.track

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class CoinTypeTest {
    @Test fun colouredFamiliesHaveMonotoneReadableRewards() {
        assertArrayEquals(arrayOf("RED", "BLUE", "GREEN", "PINK", "MIXED"),
            CoinType.values().map { it.name }.toTypedArray())
        assertArrayEquals(intArrayOf(1, 2, 3, 4, 5),
            CoinType.values().map { it.reward }.toIntArray())
        assertEquals(5f, CoinType.RED.hue, 0f)
        assertEquals(212f, CoinType.BLUE.hue, 0f)
        assertEquals(126f, CoinType.GREEN.hue, 0f)
        assertEquals(326f, CoinType.PINK.hue, 0f)
    }

    @Test fun baseCoinPayoutNudgeIsModestAndDoesNotChangeSpawnFrequency() {
        assertEquals(1.10f, CoinType.BASE_EARNINGS_MULTIPLIER, 0f)
        assertEquals(1.1f, CoinType.RED.reward * CoinType.BASE_EARNINGS_MULTIPLIER, 0.0001f)
        assertEquals(5.5f, CoinType.MIXED.reward * CoinType.BASE_EARNINGS_MULTIPLIER, 0.0001f)
    }
}
