package cube.run.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import cube.run.R
import cube.run.core.Haptics
import cube.run.core.SoundFx
import cube.run.core.Stage
import cube.run.data.Settings
import cube.run.data.Worlds

/** Persistent starting-world picker; all other catalog worlds stay in the normal shuffled rotation. */
@SuppressLint("SetTextI18n", "ViewConstructor")
class WorldPickerView(activity: Activity, kit: UiKit, onClose: () -> Unit) :
    Page(activity, kit, kit.ctx.getString(R.string.text_worlds), dark = false, onClosed = onClose) {

    private val current = kit.text("", 12f, Theme.INK_SOFT, 600)
    private val choices = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12f), dp(6f), dp(12f), dp(10f))
        clipChildren = false; clipToPadding = false
    }

    init {
        addTitleIcon(R.drawable.ic_world, Theme.CYAN)
        content.addView(LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f), dp(6f), dp(16f), dp(10f))
            addView(kit.text(kit.ctx.getString(R.string.text_world_picker_help), 13f, Theme.INK, 600, Gravity.START))
            addView(current, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(5f) })
        }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP
        })
        content.addView(ScrollView(activity).apply {
            isVerticalScrollBarEnabled = false
            clipChildren = false; clipToPadding = false
            addView(choices)
        }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT).apply {
            topMargin = dp(66f); bottomMargin = dp(66f)
        })
        content.addView(kit.button(kit.ctx.getString(R.string.text_apply_world), Theme.PLAY, UiKit.Size.NORMAL) {
            finishSelection()
        }.apply { tag = "apply_world_selection" }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM
            setMargins(dp(18f), dp(6f), dp(18f), dp(10f))
        })
        renderChoices()
    }

    override fun onCompactChanged(compact: Boolean) {
        val scroll = (content.getChildAt(1).layoutParams as FrameLayout.LayoutParams)
        scroll.topMargin = dp(if (compact) 56f else 66f)
        scroll.bottomMargin = dp(if (compact) 58f else 66f)
    }

    override fun onNavigationShown() {
        Stage.homeScreen = false
        Stage.mode = Stage.WORLD_PICKER
        Stage.worldPreview = Settings.selectedWorld.takeIf { it >= 0 } ?: Stage.RANDOM_WORLD_PREVIEW
        renderChoices()
    }

    private fun choose(id: Int) {
        Settings.selectWorld(id)
        Stage.worldPreview = id.takeIf { it >= 0 } ?: Stage.RANDOM_WORLD_PREVIEW
        SoundFx.play("tap")
        Haptics.tick()
        renderChoices()
    }

    private fun finishSelection() {
        Stage.mode = Stage.NONE
        Stage.worldPreview = -1
        SoundFx.play("success")
        Haptics.click()
        close()
    }

    private fun renderChoices() {
        val selected = Settings.selectedWorld
        val selectedName = if (selected < 0) activity.getString(R.string.text_random_world)
            else activity.gameText(Worlds.get(selected).name)
        current.text = activity.getString(R.string.text_world_choice, selectedName)
        choices.removeAllViews()
        choices.addView(option(-1, activity.getString(R.string.text_random_world),
            intArrayOf(Color.rgb(47, 31, 85), Color.rgb(17, 65, 91), Color.rgb(84, 38, 80)), selected < 0), optionParams())
        // Keep the newest campaign maps together near the top of short lists.
        val orderedWorlds = Worlds.campaign.sortedBy {
            when (it.id) {
                Worlds.GALAXY_ID -> 0
                Worlds.NEXUS_ID -> 1
                Worlds.LAVA_ID -> 2
                Worlds.AIR_ID -> 3
                Worlds.NIGHT_ID -> 4
                Worlds.HACKER_ID -> 5
                Worlds.BEACH_ID -> 6
                else -> 7 + it.id
            }
        }
        for (world in orderedWorlds) {
            val top = Color.HSVToColor(floatArrayOf(world.skyTopH, world.skyTopS.coerceIn(.35f, .9f), world.skyTopV.coerceIn(.25f, 1f)))
            val ground = Color.HSVToColor(floatArrayOf(world.floorH, world.floorS.coerceIn(.35f, .95f), world.postV.coerceIn(.45f, 1f)))
            val accent = Color.HSVToColor(floatArrayOf(world.postH, world.postS.coerceIn(.4f, 1f), world.postV.coerceIn(.5f, 1f)))
            val worldName = activity.gameText(world.name)
            val isNewWorld = world.id == Worlds.LAVA_ID || world.id == Worlds.GALAXY_ID || world.id == Worlds.NEXUS_ID ||
                world.id == Worlds.AIR_ID || world.id == Worlds.BEACH_ID || world.id == Worlds.NIGHT_ID || world.id == Worlds.HACKER_ID
            val label = if (isNewWorld) activity.getString(R.string.text_world_new_label, worldName) else worldName
            choices.addView(option(world.id, label, intArrayOf(top, ground, accent), selected == world.id), optionParams())
        }
    }

    private fun optionParams() = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54f)).apply {
        bottomMargin = dp(7f)
    }

    private fun option(id: Int, label: String, colors: IntArray, selected: Boolean): View =
        LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            contentDescription = label
            tag = "world_option_$id"
            setPadding(dp(16f), dp(10f), dp(16f), dp(10f))
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors).apply {
                cornerRadius = dpf(15f)
                setStroke(dp(if (selected) 2f else 1f), if (selected) Theme.MINT else Color.argb(75, 255, 255, 255))
            }
            addView(BiomeIconView(activity, id), LinearLayout.LayoutParams(dp(36f), dp(36f)).apply { marginEnd = dp(12f) })
            addView(kit.text(label.uppercase(resources.configuration.locales[0]), 15f, Color.WHITE, 700, Gravity.START),
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(kit.text(if (selected) activity.getString(R.string.text_selected) else "›", 12f,
                if (selected) Theme.MINT else Color.WHITE, 700, Gravity.END))
            setOnClickListener { choose(id) }
        }

    override fun onBack() = finishSelection()
}
