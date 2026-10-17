package cube.run.game.world

import cube.run.data.Bonus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpacePortalTransitionTest {
    @Test fun liveSpaceStateSurvivesTrackClearingBonusWhenExitRowSpawns() {
        assertTrue(SpacePortalTransition.isReturningFromSpace(spaceInside = true, activeBonus = Bonus.NONE))
    }

    @Test fun bonusFlagStillRecognizesSpaceBeforeTheVisualWorldEnters() {
        assertTrue(SpacePortalTransition.isReturningFromSpace(spaceInside = false, activeBonus = Bonus.SPACE))
    }

    @Test fun ordinaryBonusExitDoesNotLookLikeASpaceWarp() {
        assertFalse(SpacePortalTransition.isReturningFromSpace(spaceInside = false, activeBonus = Bonus.NONE))
        assertFalse(SpacePortalTransition.isReturningFromSpace(spaceInside = false, activeBonus = Bonus.WIDE))
    }
}
