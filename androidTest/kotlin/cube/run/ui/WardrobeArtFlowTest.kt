package cube.run.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import cube.run.GameActivity
import cube.run.bot.LiveBotDriver
import cube.run.bot.value
import cube.run.core.Stage
import cube.run.data.Progress
import cube.run.data.Settings
import cube.run.data.Skins
import cube.run.data.Wardrobe
import cube.run.game.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Real wardrobe previews, zero-cost claims, equip state, and persisted skin IDs. */
class WardrobeArtFlowTest {
    private fun restore(prefs: SharedPreferences, saved: Map<String, *>) {
        val edit = prefs.edit().clear()
        for ((key, value) in saved) when (value) {
            is Int -> edit.putInt(key, value)
            is Long -> edit.putLong(key, value)
            is Float -> edit.putFloat(key, value)
            is Boolean -> edit.putBoolean(key, value)
            is String -> edit.putString(key, value)
            is Set<*> -> edit.putStringSet(key, value.filterIsInstance<String>().toSet())
        }
        assertTrue(edit.commit())
    }

    private fun awaitDisplayedSkin(id: Int) {
        val deadline = SystemClock.uptimeMillis() + 5000
        var actual = -1
        while (SystemClock.uptimeMillis() < deadline) {
            actual = LiveBotDriver.gl { game -> value<Player>(game, "player").skin.id }
            if (actual == id) return
            SystemClock.sleep(25)
        }
        assertEquals("The actual live player model must preview cosmetic $id", id, actual)
    }

    @Test fun featuredSkinsCanBePreviewedClaimedEquippedAndReloaded() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val progress = context.getSharedPreferences("progress", Context.MODE_PRIVATE)
        val savedProgress = progress.all
        val oldSound = Settings.soundEnabled
        val oldHaptics = Settings.hapticsEnabled
        val oldSkin = Progress.skin
        val oldCoins = Progress.coins
        val targets = listOf(Skins.DANGER_ID, Skins.DINO_ID, Skins.RENEGADE_ID)
        try {
            Settings.setSoundEnabled(false); Settings.setHapticsEnabled(false)
            Progress.equip(Wardrobe.CUBE, 0)
            val order = Wardrobe.shopItems(Wardrobe.CUBE).filter { Progress.secretAvailable(Wardrobe.CUBE, it) }
            ActivityScenario.launch(GameActivity::class.java).use { scenario ->
                scenario.onActivity { it.setShowWhenLocked(true); it.setTurnScreenOn(true) }
                scenario.onActivity { activity ->
                    val root = activity.window.decorView
                    val open = root.findViewWithTag<View>("open_skin_wardrobe")
                    assertNotNull("The cube wardrobe is reachable from the live menu", open)
                    open.performClick()
                    assertEquals(0, Stage.previewSkin)
                }
                awaitDisplayedSkin(0)

                var current = 0
                for (target in targets) {
                    val hops = (order.indexOf(target) - order.indexOf(current) + order.size) % order.size
                    assertTrue("Featured skin $target belongs to the real wardrobe order", order.indexOf(target) >= 0)
                    scenario.onActivity { activity ->
                        val root = activity.window.decorView
                        val next = root.findViewWithTag<View>("wardrobe_next")
                        assertNotNull(next)
                        repeat(hops) { assertTrue("Next button is a functional control", next.performClick()) }
                        assertEquals("Selection changes the real stage preview", target, Stage.previewSkin)
                    }
                    awaitDisplayedSkin(target)
                    val before = Progress.coins
                    scenario.onActivity { activity ->
                        val action = activity.window.decorView.findViewWithTag<View>("wardrobe_action_button")
                        assertNotNull(action)
                        assertTrue("Free skin has a real claim/equip action", action.performClick())
                        assertEquals(target, Progress.skin)
                        assertTrue(Progress.owns(Wardrobe.CUBE, target))
                        assertEquals("The starter styles must not charge coins", before, Progress.coins)
                    }
                    assertEquals(target, progress.getInt("skin", -1))
                    assertTrue((progress.getInt("owned_skins", 0) and (1 shl target)) != 0)
                    assertEquals("Changing presentation never grants gameplay perks", emptyList<Skins.Ability>(), Skins.get(target).abilities)
                    current = target
                }
                assertEquals(oldCoins, Progress.coins)
                Progress.init(context)
                assertEquals("The equipped choice survives reloading persistent progress", Skins.RENEGADE_ID, Progress.skin)
            }
        } finally {
            restore(progress, savedProgress)
            Progress.init(context)
            Settings.setSoundEnabled(oldSound); Settings.setHapticsEnabled(oldHaptics)
            Settings.init(context)
            Stage.clearPreview(); Stage.mode = Stage.NONE
        }
        assertEquals("Tests restore the player's equipment", oldSkin, Progress.skin)
    }
}
