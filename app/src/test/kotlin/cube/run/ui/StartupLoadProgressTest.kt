package cube.run.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class StartupLoadProgressTest {
    @Test fun progressIsBoundedAndNeverMovesBackwards() {
        val progress = StartupLoadProgress()
        progress.advanceTo(18)
        progress.advanceTo(72)
        progress.advanceTo(41)
        assertEquals(72, progress.targetPercent)
        progress.advanceTo(120)
        assertEquals(100, progress.targetPercent)
        progress.advanceTo(0)
        assertEquals(100, progress.targetPercent)
    }

    @Test fun negativeMilestonesCannotCreateNegativeProgress() {
        val progress = StartupLoadProgress()
        progress.advanceTo(-8)
        assertEquals(0, progress.targetPercent)
    }
}
