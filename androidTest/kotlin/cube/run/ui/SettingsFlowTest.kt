package cube.run.ui

import android.content.Context
import android.view.View
import android.widget.SeekBar
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import cube.run.GameActivity
import cube.run.core.SoundFx
import cube.run.core.Stage
import cube.run.data.MusicStyle
import cube.run.data.RunDifficulty
import cube.run.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verify that the real menu exposes persistent audio, graphics and run-difficulty controls. */
class SettingsFlowTest {
    @Test fun settingsPageSavesMusicToggleAndGraphicsPresetAndExposesSeparateVolumes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val oldMusicEnabled = Settings.musicEnabled
        val oldMusicVolume = Settings.musicVolume
        val oldEffectsVolume = Settings.effectsVolume
        val oldMusicStyle = Settings.musicStyle
        val oldPreset = Settings.graphicsPreset
        val oldDifficulty = Settings.runDifficulty
        try {
            ActivityScenario.launch(GameActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    val root = activity.window.decorView
                    val open = root.findViewWithTag<View>("open_settings")
                    assertNotNull("Settings are reachable from the main menu", open)
                    open.performClick()
                    assertNotNull(root.findViewWithTag<View>("settings_music_toggle"))
                    assertNotNull(root.findViewWithTag<SeekBar>("settings_music_volume"))
                    assertNotNull(root.findViewWithTag<SeekBar>("settings_effects_volume"))
                    assertNotNull(root.findViewWithTag<View>("settings_haptics_toggle"))
                    assertNotNull("A crash/OS-exit report can be copied if the issue repeats",
                        root.findViewWithTag<View>("copy_crash_report"))

                    val wasOn = Settings.musicEnabled
                    root.findViewWithTag<View>("settings_music_toggle").performClick()
                    assertNotEquals(wasOn, Settings.musicEnabled)
                    assertEquals(Settings.musicEnabled, prefs.getBoolean("music_enabled", true))

                    for (style in MusicStyle.entries) {
                        assertNotNull("${style.name} music style is available in Settings",
                            root.findViewWithTag<View>("settings_music_style_${style.id}"))
                    }
                    val phonkFunk = root.findViewWithTag<View>("settings_music_style_${MusicStyle.PHONK_FUNK.id}")
                    phonkFunk.performClick()
                    assertEquals(MusicStyle.PHONK_FUNK, Settings.musicStyle)
                    assertEquals(MusicStyle.PHONK_FUNK.id, prefs.getInt("music_style", -1))
                    root.findViewWithTag<View>("settings_music_style_${MusicStyle.WORLD_MIX.id}").performClick()
                    assertEquals(MusicStyle.WORLD_MIX, Settings.musicStyle)

                    val high = root.findViewWithTag<View>("graphics_preset_${Settings.GRAPHICS_HIGH}")
                    assertNotNull(high)
                    high.performClick()
                    assertEquals(Settings.GRAPHICS_HIGH, Settings.graphicsPreset)
                    assertEquals(Settings.GRAPHICS_HIGH, prefs.getInt("graphics_preset", -1))

                    for (mode in RunDifficulty.entries) {
                        assertNotNull("${mode.name} difficulty is available in Settings",
                            root.findViewWithTag<View>("run_difficulty_${mode.id}"))
                    }
                    val hard = root.findViewWithTag<View>("run_difficulty_${RunDifficulty.HARD.id}")
                    hard.performClick()
                    assertEquals(RunDifficulty.HARD, Settings.runDifficulty)
                    assertEquals(RunDifficulty.HARD.id, prefs.getInt("run_difficulty", -1))
                    assertTrue("Settings do not start or alter the live page", Stage.mode == Stage.SETTINGS)
                }
            }
        } finally {
            Settings.setMusicEnabled(oldMusicEnabled)
            Settings.setMusicVolume(oldMusicVolume)
            Settings.setMusicStyle(oldMusicStyle)
            Settings.setEffectsVolume(oldEffectsVolume)
            Settings.setGraphicsPreset(oldPreset)
            Settings.setRunDifficulty(oldDifficulty)
            SoundFx.refreshMusicSettings()
            Stage.mode = Stage.NONE
            Stage.homeScreen = true
        }
    }
}
