package cube.run.game.world

import org.junit.Assert.assertEquals
import org.junit.Test

class PortalArrivalSafetyTest {
    @Test fun corridorCoversTheWholeArrivalGraceAtRunSpeed() {
        assertEquals(1.5f, PortalArrivalSafety.DURATION_SECONDS, 0f)
        assertEquals(20.6f, PortalArrivalSafety.corridorDistance(12.4f), .0001f)
    }

    @Test fun slowOrInvalidSpeedStillLeavesAMinimumClearRunway() {
        assertEquals(6f, PortalArrivalSafety.corridorDistance(0f), 0f)
        assertEquals(6f, PortalArrivalSafety.corridorDistance(Float.NaN), 0f)
        assertEquals(6f, PortalArrivalSafety.corridorDistance(-3f), 0f)
    }
}
