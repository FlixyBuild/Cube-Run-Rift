package cube.run.game.world

import cube.run.data.Bonus

/** State guard for an exit row generated after Track has already cleared its bonus ID. */
internal object SpacePortalTransition {
    fun isReturningFromSpace(spaceInside: Boolean, activeBonus: Int): Boolean =
        spaceInside || activeBonus == Bonus.SPACE
}
