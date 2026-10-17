package cube.run.game.world

import cube.run.data.Bonus
import cube.run.data.Worlds

/** Keeps the Galaxy campaign objective dormant in unrelated bonus routes. */
internal object WorldMissionRoute {
    fun usesGalaxyMission(bonusId: Int, baseWorldId: Int): Boolean =
        bonusId == Bonus.GALAXY || (bonusId == Bonus.NONE && baseWorldId == Worlds.GALAXY_ID)
}
