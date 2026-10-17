package cube.run.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import cube.run.R
import cube.run.core.CrashDiagnostics
import cube.run.core.SoundFx
import cube.run.core.Stage
import cube.run.data.MusicStyle
import cube.run.data.RunDifficulty
import cube.run.data.Settings

/** Real, persistent player settings; quality affects render cost only, never gameplay or collision. */
@SuppressLint("ViewConstructor", "SetTextI18n")
class SettingsView(activity: Activity, kit: UiKit, onClose: () -> Unit) :
    Page(activity, kit, activity.getString(R.string.text_settings_title), dark = false, onClosed = onClose) {

    private val presetButtons = ArrayList<CandyButton>(3)
    private val difficultyButtons = ArrayList<CandyButton>(3)
    private val musicStyleButtons = ArrayList<CandyButton>(MusicStyle.entries.size)

    init {
        addTitleIcon(R.drawable.ic_settings, Theme.LAVENDER)
        val scroll = ScrollView(activity).apply {
            isVerticalScrollBarEnabled = false
            clipChildren = false; clipToPadding = false
        }
        val stack = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18f), dp(10f), dp(18f), dp(20f))
            clipChildren = false; clipToPadding = false
        }
        stack.addView(kit.text(activity.getString(R.string.text_settings_help), 13f, Theme.INK_SOFT, 600, Gravity.START),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(14f) })

        val soundCard = card()
        soundCard.addView(kit.text(activity.getString(R.string.text_sound_title), 17f, Theme.INK, 700, Gravity.START),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8f) })
        soundCard.addView(toggleRow(
            activity.getString(R.string.cd_sound),
            R.drawable.ic_sound_on, R.drawable.ic_sound_off,
            { Settings.soundEnabled }, { enabled ->
                if (Settings.soundEnabled != enabled) {
                    Settings.setSoundEnabled(enabled)
                    SoundFx.refreshMusicSettings()
                }
            },
        ).apply { tag = "settings_master_sound_toggle" })
        soundCard.addView(toggleRow(
            activity.getString(R.string.text_music_enabled),
            R.drawable.ic_sound_on, R.drawable.ic_sound_off,
            { Settings.musicEnabled }, { enabled ->
                Settings.setMusicEnabled(enabled)
                SoundFx.refreshMusicSettings()
            },
        ).apply { tag = "settings_music_toggle" })
        soundCard.addView(sliderRow(R.string.text_music_volume, Settings.musicVolume, "settings_music_volume") {
            Settings.setMusicVolume(it)
            SoundFx.refreshMusicSettings()
        })
        soundCard.addView(kit.text(activity.getString(R.string.text_music_style_title), 13f, Theme.INK, 600, Gravity.START),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(6f); bottomMargin = dp(6f)
            })
        val musicStyleRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            clipChildren = false; clipToPadding = false
        }
        for (style in MusicStyle.entries) {
            val label = activity.getString(when (style) {
                MusicStyle.WORLD_MIX -> R.string.text_music_style_world
                MusicStyle.PHONK_FUNK -> R.string.text_music_style_phonk
                MusicStyle.URDU_THEME -> R.string.text_music_style_urdu
            })
            val button = kit.button(label, Theme.CARD_ALT, UiKit.Size.SMALL) {
                Settings.setMusicStyle(style)
                SoundFx.refreshMusicSettings()
                updateMusicStyleButtons()
            }.apply {
                tag = "settings_music_style_${style.id}"
                minWidth = 0
            }
            musicStyleButtons.add(button)
            musicStyleRow.addView(button, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                if (style != MusicStyle.WORLD_MIX) marginStart = dp(7f)
            })
        }
        soundCard.addView(musicStyleRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        soundCard.addView(sliderRow(R.string.text_effects_volume, Settings.effectsVolume, "settings_effects_volume") {
            Settings.setEffectsVolume(it)
        })
        stack.addView(soundCard, cardParams())

        val options = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false; clipToPadding = false
            background = kit.cardDrawable()
            setPadding(dp(14f), dp(8f), dp(14f), dp(8f))
        }
        options.addView(kit.text(activity.getString(R.string.cd_haptics), 15f, Theme.INK, 650, Gravity.START),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        options.addView(kit.toggle(R.drawable.ic_haptic_on, R.drawable.ic_haptic_off,
            activity.getString(R.string.cd_haptics), Theme.SKY,
            { Settings.hapticsEnabled }, { Settings.setHapticsEnabled(it) }).apply { tag = "settings_haptics_toggle" },
            LinearLayout.LayoutParams(dp(52f), dp(52f)))
        stack.addView(options, cardParams())

        val difficultyCard = card()
        difficultyCard.addView(kit.text(activity.getString(R.string.text_difficulty_title), 17f, Theme.INK, 700, Gravity.START),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(4f) })
        difficultyCard.addView(kit.text(activity.getString(R.string.text_difficulty_help), 12f, Theme.INK_SOFT, 500, Gravity.START),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(10f) })
        val difficultyRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            clipChildren = false; clipToPadding = false
        }
        val difficultyLabels = intArrayOf(R.string.text_difficulty_easy, R.string.text_difficulty_medium, R.string.text_difficulty_hard)
        for (mode in RunDifficulty.entries) {
            val button = kit.button(activity.getString(difficultyLabels[mode.id]), Theme.CARD_ALT, UiKit.Size.SMALL) {
                Settings.setRunDifficulty(mode)
                updateDifficultyButtons()
            }.apply {
                tag = "run_difficulty_${mode.id}"
                minWidth = 0
            }
            difficultyButtons.add(button)
            difficultyRow.addView(button, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                if (mode != RunDifficulty.EASY) marginStart = dp(7f)
            })
        }
        difficultyCard.addView(difficultyRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        stack.addView(difficultyCard, cardParams())

        val graphicsCard = card()
        graphicsCard.addView(kit.text(activity.getString(R.string.text_graphics_quality), 17f, Theme.INK, 700, Gravity.START),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(4f) })
        graphicsCard.addView(kit.text(activity.getString(R.string.text_graphics_help), 12f, Theme.INK_SOFT, 500, Gravity.START),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(10f) })
        val qualityRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            clipChildren = false; clipToPadding = false
        }
        val labels = intArrayOf(R.string.text_graphics_low, R.string.text_graphics_medium, R.string.text_graphics_high)
        for (preset in Settings.GRAPHICS_LOW..Settings.GRAPHICS_HIGH) {
            val button = kit.button(activity.getString(labels[preset]), Theme.CARD_ALT, UiKit.Size.SMALL) {
                Settings.setGraphicsPreset(preset)
                updatePresetButtons()
            }.apply {
                tag = "graphics_preset_$preset"
                minWidth = 0
            }
            presetButtons.add(button)
            qualityRow.addView(button, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                if (preset > Settings.GRAPHICS_LOW) marginStart = dp(7f)
            })
        }
        graphicsCard.addView(qualityRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        stack.addView(graphicsCard, cardParams())

        stack.addView(kit.text(activity.getString(R.string.text_graphics_restart_note), 11f, Theme.MUTED, 500, Gravity.START),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(4f), dp(2f), dp(4f), dp(8f))
            })

        val diagnosticsCard = card()
        diagnosticsCard.addView(kit.text(activity.getString(R.string.text_diagnostics_title), 17f, Theme.INK, 700, Gravity.START),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(4f) })
        diagnosticsCard.addView(kit.text(activity.getString(R.string.text_diagnostics_help), 12f, Theme.INK_SOFT, 500, Gravity.START),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(10f) })
        diagnosticsCard.addView(kit.button(activity.getString(R.string.text_copy_diagnostics), Theme.CARD_ALT, UiKit.Size.SMALL) {
            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Cube Run diagnostics", CrashDiagnostics.reportText(activity)))
            Toast.makeText(activity, R.string.text_diagnostics_copied, Toast.LENGTH_LONG).show()
        }.apply { tag = "copy_crash_report" })
        stack.addView(diagnosticsCard, cardParams())
        scroll.addView(stack)
        content.addView(scroll, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        updatePresetButtons()
        updateDifficultyButtons()
        updateMusicStyleButtons()
    }

    override fun onNavigationShown() {
        Stage.homeScreen = false
        Stage.mode = Stage.SETTINGS
        syncSlider("settings_music_volume", Settings.musicVolume)
        syncSlider("settings_effects_volume", Settings.effectsVolume)
        updatePresetButtons()
        updateDifficultyButtons()
        updateMusicStyleButtons()
    }

    private fun syncSlider(tagName: String, amount: Float) {
        val seek = findViewWithTag<SeekBar>(tagName) ?: return
        val progress = (amount * 100f).toInt().coerceIn(0, 100)
        seek.progress = progress
        findViewWithTag<TextView>("${tagName}_value")?.text = "$progress%"
    }

    private fun card() = kit.card(fill = Theme.CARD, radius = 20f).apply {
        setPadding(dp(16f), dp(14f), dp(16f), dp(15f))
    }

    private fun cardParams() = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
        bottomMargin = dp(12f)
    }

    private fun toggleRow(
        label: String,
        iconOn: Int,
        iconOff: Int,
        isOn: () -> Boolean,
        set: (Boolean) -> Unit,
    ) = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        clipChildren = false; clipToPadding = false
        addView(kit.text(label, 14f, Theme.INK, 600, Gravity.START),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val control = kit.toggle(iconOn, iconOff, label, Theme.SKY, isOn, set)
        addView(control, LinearLayout.LayoutParams(dp(52f), dp(52f)))
        isClickable = true
        isFocusable = true
        contentDescription = label
        setOnClickListener { control.performClick() }
    }

    private fun sliderRow(titleId: Int, initial: Float, tagName: String, changed: (Float) -> Unit) =
        LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4f), 0, dp(4f))
            val heading = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val title = kit.text(activity.getString(titleId), 13f, Theme.INK, 600, Gravity.START)
            val value = kit.text("${(initial * 100f).toInt()}%", 12f, Theme.INK_SOFT, 700, Gravity.END)
            value.tag = "${tagName}_value"
            heading.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            heading.addView(value)
            addView(heading, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(SeekBar(activity).apply {
                tag = tagName
                max = 100
                progress = (initial * 100f).toInt().coerceIn(0, 100)
                progressTintList = android.content.res.ColorStateList.valueOf(Theme.SKY)
                thumbTintList = android.content.res.ColorStateList.valueOf(Theme.CYAN)
                contentDescription = activity.getString(titleId)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                        val fraction = progress / 100f
                        value.text = "$progress%"
                        if (fromUser) changed(fraction)
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                    override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
                })
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42f)))
        }

    private fun updatePresetButtons() {
        for ((index, button) in presetButtons.withIndex()) {
            val selected = Settings.graphicsPreset == index
            button.color = if (selected) Theme.CYAN else Theme.CARD_ALT
            button.isSelected = selected
            button.contentDescription = activity.getString(
                if (selected) R.string.text_graphics_selected else R.string.text_graphics_choose,
                button.text,
            )
        }
    }

    private fun updateDifficultyButtons() {
        for (button in difficultyButtons) {
            val mode = RunDifficulty.fromId(button.tag.toString().substringAfterLast('_').toIntOrNull() ?: RunDifficulty.MEDIUM.id)
            val selected = Settings.runDifficulty == mode
            button.color = if (selected) Theme.CYAN else Theme.CARD_ALT
            button.isSelected = selected
            button.contentDescription = activity.getString(
                if (selected) R.string.text_difficulty_selected else R.string.text_difficulty_choose,
                button.text,
            )
        }
    }

    private fun updateMusicStyleButtons() {
        for (button in musicStyleButtons) {
            val id = button.tag.toString().substringAfterLast('_').toIntOrNull() ?: MusicStyle.WORLD_MIX.id
            val selected = Settings.musicStyle.id == id
            button.color = if (selected) Theme.CYAN else Theme.CARD_ALT
            button.isSelected = selected
            button.contentDescription = activity.getString(
                if (selected) R.string.text_music_style_selected else R.string.text_music_style_choose,
                button.text,
            )
        }
    }

    private fun leaveSettings() {
        if (Stage.mode == Stage.SETTINGS) Stage.mode = Stage.NONE
        close()
    }

    override fun onBack() = leaveSettings()
}
