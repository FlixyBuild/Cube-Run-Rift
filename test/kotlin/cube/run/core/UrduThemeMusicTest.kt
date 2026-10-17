package cube.run.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UrduThemeMusicTest {
    @Test fun originalThemeIsAudibleDeterministicAndHasHeadroom() {
        val rate = 8_000
        val first = UrduThemeMusic.synthesize(rate)
        val second = UrduThemeMusic.synthesize(rate)
        val peak = first.maxOf { kotlin.math.abs(it.toInt()) }

        assertEquals(rate * 16 * 60 / 102, first.size)
        assertEquals(first.toList(), second.toList())
        assertTrue("Theme is silent", peak > 1_000)
        assertTrue("Theme should retain headroom", peak < Short.MAX_VALUE.toInt())
        assertTrue(first.any { it > 0 })
        assertTrue(first.any { it < 0 })
    }
}
