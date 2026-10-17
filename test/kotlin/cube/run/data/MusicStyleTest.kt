package cube.run.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MusicStyleTest {
    @Test fun brazilianPhonkFunkIsASeparateOptionBesideTheWorldMix() {
        assertEquals(listOf(MusicStyle.WORLD_MIX, MusicStyle.PHONK_FUNK, MusicStyle.URDU_THEME), MusicStyle.entries)
    }

    @Test fun restoredIdsKeepWorldMixAsTheBackwardCompatibleDefault() {
        assertEquals(MusicStyle.WORLD_MIX, MusicStyle.fromId(0))
        assertEquals(MusicStyle.PHONK_FUNK, MusicStyle.fromId(1))
        assertEquals(MusicStyle.URDU_THEME, MusicStyle.fromId(2))
        assertEquals(MusicStyle.WORLD_MIX, MusicStyle.fromId(-1))
        assertEquals(MusicStyle.WORLD_MIX, MusicStyle.fromId(99))
    }
}
