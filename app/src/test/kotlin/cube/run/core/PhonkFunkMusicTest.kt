package cube.run.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhonkFunkMusicTest {
    @Test fun generatedBrazilianGrooveIsAnAudibleLoopWithHeadroom() {
        val sampleRate = 8_000
        val samples = PhonkFunkMusic.synthesize(sampleRate)
        val peak = samples.maxOf { kotlin.math.abs(it.toInt()) }

        assertEquals(sampleRate * 16 * 60 / 132, samples.size)
        assertTrue("Loop is silent", peak > 1_000)
        assertTrue("Loop should preserve headroom", peak < Short.MAX_VALUE.toInt())
        assertTrue("Loop has no positive waveform", samples.any { it > 0 })
        assertTrue("Loop has no negative waveform", samples.any { it < 0 })
    }
}
