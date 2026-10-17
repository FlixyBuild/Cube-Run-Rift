package cube.run.intro

import cube.run.R
import cube.run.data.Skins
import cube.run.data.Worlds
import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchThemesTest {
    @Test fun newAndHighestSkinIdsUseTheirGeneratedSplashPalettes() {
        assertEquals(R.style.LaunchSkin30, LaunchThemes.skin(Skins.BASSLINE_ID))
        assertEquals(R.style.LaunchSkin16, LaunchThemes.skin(Skins.COMET_CORE_ID))
        assertEquals(R.style.LaunchSkin17, LaunchThemes.skin(Skins.SIGNAL_SHIFT_ID))
        assertEquals(R.style.LaunchSkin18, LaunchThemes.skin(Skins.CODEBREAKER_ID))
        assertEquals(R.style.LaunchSkin19, LaunchThemes.skin(Skins.RIFT_RANGER_ID))
        assertEquals(R.style.LaunchSkin20, LaunchThemes.skin(Skins.PORTRAIT_ONE_ID))
        assertEquals(R.style.LaunchSkin21, LaunchThemes.skin(Skins.PORTRAIT_TWO_ID))
        assertEquals(R.style.LaunchSkin22, LaunchThemes.skin(Skins.PORTRAIT_THREE_ID))
        assertEquals(R.style.LaunchSkin23, LaunchThemes.skin(Skins.SKY_SOARER_ID))
        assertEquals(R.style.LaunchSkin0, LaunchThemes.skin(Int.MAX_VALUE))
    }

    @Test fun galaxyLavaNexusAndExistingMapsUseGeneratedLaunchPalettes() {
        assertEquals(R.style.LaunchWorld2, LaunchThemes.world(Worlds.LAVA_ID))
        assertEquals(R.style.LaunchWorld5, LaunchThemes.world(Worlds.GALAXY_ID))
        assertEquals(R.style.LaunchWorld6, LaunchThemes.world(Worlds.RAILWAY_ID))
        assertEquals(R.style.LaunchWorld10, LaunchThemes.world(Worlds.NIGHT_ID))
        assertEquals(R.style.LaunchWorld11, LaunchThemes.world(Worlds.HACKER_ID))
        assertEquals(R.style.LaunchWorld12, LaunchThemes.world(Worlds.AIR_ID))
        assertEquals(R.style.LaunchWorld13, LaunchThemes.world(Worlds.NEXUS_ID))
    }
}
