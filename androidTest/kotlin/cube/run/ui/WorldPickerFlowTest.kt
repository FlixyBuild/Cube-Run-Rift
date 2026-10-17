package cube.run.ui

import android.content.Context
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import cube.run.GameActivity
import cube.run.bot.LiveBotDriver
import cube.run.core.Stage
import cube.run.data.Settings
import cube.run.data.Worlds
import cube.run.game.CubeRun
import cube.run.game.world.WorldRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Exercise the real menu → picker → live starting biome → persisted campaign rotation path. */
class WorldPickerFlowTest {
    @Test fun beachSelectionIsAppliedNowSavedAndKeepsOtherCampaignWorldsInTheRotation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val previous = Settings.selectedWorld
        try {
            Settings.selectWorld(-1)
            ActivityScenario.launch(GameActivity::class.java).use { scenario ->
                scenario.onActivity { it.setShowWhenLocked(true); it.setTurnScreenOn(true) }
                scenario.onActivity { activity ->
                    val root = activity.window.decorView
                    val open = root.findViewWithTag<View>("open_world_picker")
                    assertNotNull("World picker is reachable from the real main menu", open)
                    open.performClick()
                    val beach = root.findViewWithTag<View>("world_option_${Worlds.BEACH_ID}")
                    val city = root.findViewWithTag<View>("world_option_${Worlds.NEON_CITY_ID}")
                    val winter = root.findViewWithTag<View>("world_option_${Worlds.WINTER_ID}")
                    val space = root.findViewWithTag<View>("world_option_${Worlds.SPACE_ID}")
                    val railway = root.findViewWithTag<View>("world_option_${Worlds.RAILWAY_ID}")
                    val night = root.findViewWithTag<View>("world_option_${Worlds.NIGHT_ID}")
                    val hacker = root.findViewWithTag<View>("world_option_${Worlds.HACKER_ID}")
                    val air = root.findViewWithTag<View>("world_option_${Worlds.AIR_ID}")
                    assertNotNull("The new Tropical Beach map is a real selectable world", beach)
                    assertNotNull("Neon City is a real selectable world", city)
                    assertNotNull("Frost Peaks is a real selectable winter world", winter)
                    assertNotNull("Riftbound Space remains a real selectable world", space)
                    assertNotNull("Railway Yard is a selectable campaign world", railway)
                    assertNotNull("Moonlit Wilds is a selectable campaign world", night)
                    assertNotNull("Matrix Circuit is a selectable campaign world", hacker)
                    assertNotNull("Air is a selectable campaign world", air)
                    beach.performClick()
                    assertEquals(Worlds.BEACH_ID, Settings.selectedWorld)
                    assertEquals(Worlds.BEACH_ID, Stage.worldPreview)
                    assertTrue("The choice is committed to preferences", settings.getInt("selected_world", -1) == Worlds.BEACH_ID)
                    val done = root.findViewWithTag<View>("apply_world_selection")
                    assertNotNull(done)
                    done.performClick()
                    assertEquals(Stage.NONE, Stage.mode)
                    assertEquals(-1, Stage.worldPreview)
                }

                val result = LiveBotDriver.gl { game ->
                    val field = CubeRun::class.java.getDeclaredField("worlds").apply { isAccessible = true }
                    val worlds = field.get(game) as WorldRunner
                    val orderField = WorldRunner::class.java.getDeclaredField("order").apply { isAccessible = true }
                    worlds.world.id to (orderField.get(worlds) as List<Int>).toSet()
                }
                assertEquals("The preview changed the world the live run will start in", Worlds.BEACH_ID, result.first)
                assertEquals("Every non-selected campaign world remains in the rotation",
                    Worlds.campaignIds.filter { it != Worlds.BEACH_ID }.toSet(), result.second)
                Settings.init(context) // same persisted value the next process reads
                assertEquals(Worlds.BEACH_ID, Settings.selectedWorld)
            }
        } finally {
            Settings.selectWorld(previous)
            Stage.mode = Stage.NONE
            Stage.worldPreview = -1
        }
    }
}
