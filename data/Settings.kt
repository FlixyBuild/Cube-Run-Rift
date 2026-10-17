package cube.run.data

import android.content.Context
import android.content.SharedPreferences

/** Optional soundtrack modes; the original world-specific score stays the default. */
enum class MusicStyle(val id: Int) {
    WORLD_MIX(0),
    PHONK_FUNK(1),
    URDU_THEME(2);

    companion object {
        fun fromId(id: Int): MusicStyle = entries.firstOrNull { it.id == id } ?: WORLD_MIX
    }
}

/** Persistent player-facing options. Safe to read from the GL thread (values are @Volatile). */
object Settings {
    private lateinit var prefs: SharedPreferences

    /**
     * Master flag for the experimental "smooth control" feature *and* its UI.
     * While false the feature is fully off and its toggle/sensitivity controls are
     * hidden. To bring it back, flip this to true — the in-game toggle then drives
     * the feature (we re-enable the UI, not the feature itself).
     */
    const val SMOOTH_CONTROL_UI = false

    /** "Smooth control": steer continuously without lifting your finger between moves. */
    @Volatile private var smoothControlPref: Boolean = false

    /** Effective smooth-control state — always false while [SMOOTH_CONTROL_UI] is off. */
    val smoothControl: Boolean get() = SMOOTH_CONTROL_UI && smoothControlPref

    /** Smooth-control sensitivity, 0..1 (higher = smaller finger movement per move). */
    @Volatile var smoothSensitivity: Float = 0.5f
        private set

    /** Master mute for all procedural sound effects. Read from the GL thread. */
    @Volatile var soundEnabled: Boolean = true
        private set

    /** Master toggle for haptic feedback. Read from the GL thread. */
    @Volatile var hapticsEnabled: Boolean = true
        private set

    /** Music and effects have independent controls; [soundEnabled] remains the quick master mute. */
    @Volatile var musicEnabled: Boolean = true
        private set
    @Volatile var musicVolume: Float = .46f
        private set
    @Volatile var musicStyle: MusicStyle = MusicStyle.WORLD_MIX
        private set
    @Volatile var effectsVolume: Float = .86f
        private set

    const val GRAPHICS_LOW = 0
    const val GRAPHICS_MEDIUM = 1
    const val GRAPHICS_HIGH = 2
    /** Persisted render-cost preset. It never changes movement, spawns or collision rules. */
    @Volatile var graphicsPreset: Int = GRAPHICS_MEDIUM
        private set

    /** Persistent gameplay preset applied to the next run in both campaign worlds. */
    @Volatile var runDifficulty: RunDifficulty = RunDifficulty.MEDIUM
        private set

    @Volatile var audioHapticRevision: Int = 0
        private set

    /**
     * Dev mode: the section director cycles the sections under review and the
     * bank is filled. A test tool, so it is process-scoped on purpose: it
     * survives an in-task RESTART but never a fresh launch.
     */
    @Volatile var devMode: Boolean = false
        private set

    /**
     * Section explorer: when ≥ 0 the director serves only this section id, on
     * loop. Process-scoped on purpose (RESTART relaunches in-process), never persisted.
     */
    @Volatile var testSection: Int = -1

    /** Debug: when ≥ 0 every portal opens to this bonus world. Process-scoped. */
    @Volatile var testBonus: Int = -1

    /** Debug: when ≥ 0 the run starts in this world. Process-scoped. */
    /** Debug: start the run already inside this bonus world (dev mode only; -1 = off). */
    @Volatile var testBonusNow = -1
    /** Debug: mystery boxes the next run starts with (dev mode only), so the box stage is one END RUN away. */
    @Volatile var testBoxes = 0
    @Volatile var testPillWorld = false
    /** Section explorer: the run starts inside Galaxy and every portal leads back there. Process-scoped. */
    @Volatile var testSpaceWorld = false
    @Volatile var testWorld: Int = -1

    /** Persistent normal-run starting world; -1 keeps the shuffled/random start. */
    @Volatile var selectedWorld: Int = -1
        private set

    @Volatile private var performanceCourseSelected = false
    val performanceCourse: Boolean get() = cube.run.BuildConfig.DEBUG && performanceCourseSelected
    private var performancePreviousDev = false
    private var performancePreviousWorld = -1

    /** A process-scoped course: retries retain it, a fresh app process does not. */
    fun selectPerformanceCourse() {
        if (!cube.run.BuildConfig.DEBUG) return
        if (!performanceCourseSelected) {
            performancePreviousDev = devMode
            performancePreviousWorld = testWorld
        }
        performanceCourseSelected = true
        setDevMode(true); Progress.enterDev()
        testSection = 56; testWorld = 2; testPillWorld = false; testSpaceWorld = false
        testBonus = -1; testBonusNow = -1; testBoxes = 0
    }

    /** Restore the pre-test bank/developer state without changing equipment. */
    fun leavePerformanceCourse() {
        if (!performanceCourseSelected) return
        performanceCourseSelected = false
        testWorld = performancePreviousWorld
        setDevMode(performancePreviousDev)
        if (!performancePreviousDev) Progress.leaveDev()
    }

    fun init(ctx: Context) {
        prefs = ctx.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        smoothControlPref = prefs.getBoolean("smooth_control", false)
        smoothSensitivity = prefs.getFloat("smooth_sensitivity", 0.5f)
        soundEnabled = prefs.getBoolean("sound_enabled", true)
        hapticsEnabled = prefs.getBoolean("haptics_enabled", true)
        musicEnabled = prefs.getBoolean("music_enabled", true)
        musicVolume = prefs.getFloat("music_volume", .46f).takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: .46f
        musicStyle = MusicStyle.fromId(prefs.getInt("music_style", MusicStyle.WORLD_MIX.id))
        effectsVolume = prefs.getFloat("effects_volume", .86f).takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: .86f
        graphicsPreset = prefs.getInt("graphics_preset", GRAPHICS_MEDIUM).coerceIn(GRAPHICS_LOW, GRAPHICS_HIGH)
        runDifficulty = RunDifficulty.fromId(prefs.getInt("run_difficulty", RunDifficulty.MEDIUM.id))
        // The new campaign opens in Jungle. Migrate the previous default once, while keeping later player choices.
        val worldMigration = prefs.getInt("rift_world_migration", 0) < 1
        selectedWorld = if (worldMigration) Worlds.JUNGLE_ID else
            prefs.getInt("selected_world", Worlds.JUNGLE_ID).takeIf { it == -1 || Worlds.isCampaignWorld(it) } ?: Worlds.JUNGLE_ID
        if (worldMigration) prefs.edit().putInt("selected_world", selectedWorld).putInt("rift_world_migration", 1).apply()
        if (prefs.contains("dev_mode")) prefs.edit().remove("dev_mode").apply() // was persisted once; never again
    }

    fun setSmoothControl(v: Boolean) {
        smoothControlPref = v
        prefs.edit().putBoolean("smooth_control", v).apply()
    }

    fun setSmoothSensitivity(v: Float) {
        smoothSensitivity = v.coerceIn(0f, 1f)
        prefs.edit().putFloat("smooth_sensitivity", smoothSensitivity).apply()
    }

    fun setSoundEnabled(v: Boolean) {
        if (soundEnabled != v) audioHapticRevision++
        soundEnabled = v
        prefs.edit().putBoolean("sound_enabled", v).apply()
    }

    fun setHapticsEnabled(v: Boolean) {
        if (hapticsEnabled != v) audioHapticRevision++
        hapticsEnabled = v
        prefs.edit().putBoolean("haptics_enabled", v).apply()
    }

    fun setMusicEnabled(v: Boolean) {
        musicEnabled = v
        prefs.edit().putBoolean("music_enabled", v).apply()
    }

    fun setMusicVolume(v: Float) {
        musicVolume = (if (v.isFinite()) v else .46f).coerceIn(0f, 1f)
        prefs.edit().putFloat("music_volume", musicVolume).apply()
    }

    fun setMusicStyle(v: MusicStyle) {
        musicStyle = v
        prefs.edit().putInt("music_style", v.id).apply()
    }

    fun setEffectsVolume(v: Float) {
        effectsVolume = (if (v.isFinite()) v else .86f).coerceIn(0f, 1f)
        prefs.edit().putFloat("effects_volume", effectsVolume).apply()
    }

    fun setGraphicsPreset(v: Int) {
        graphicsPreset = v.coerceIn(GRAPHICS_LOW, GRAPHICS_HIGH)
        prefs.edit().putInt("graphics_preset", graphicsPreset).apply()
    }

    fun setRunDifficulty(value: RunDifficulty) {
        runDifficulty = value
        prefs.edit().putInt("run_difficulty", value.id).apply()
    }

    fun selectWorld(id: Int) {
        require(id == -1 || Worlds.isCampaignWorld(id)) { "Unknown campaign world id: $id" }
        selectedWorld = id
        prefs.edit().putInt("selected_world", id).apply()
    }

    fun setDevMode(v: Boolean) {
        devMode = v
    }
}
