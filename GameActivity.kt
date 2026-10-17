package cube.run

import android.os.Bundle
import android.view.WindowManager
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.MotionEvent
import android.view.KeyEvent
import android.widget.FrameLayout
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import cube.run.core.GameHostSession
import cube.run.core.Stage
import cube.run.core.PhysicalInput
import cube.run.data.Progress
import cube.run.data.Scores
import cube.run.data.Settings
import cube.run.game.CubeRun
import cube.run.ui.Hud
import cube.run.ui.StartupLoadingView
import android.view.View

/** Single-game launcher host: builds the HUD over the libGDX surface and runs Cube Run. */
open class GameActivity : AndroidApplication() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(cube.run.data.Languages.wrap(newBase))
    }

    private var languageResources: android.content.res.Resources? = null
    override fun getResources(): android.content.res.Resources = languageResources ?: super.getResources()
    private lateinit var hostSession: GameHostSession
    private var changingLanguage = false

    /** Rebuild only the localized overlay; the GL surface and world keep running uninterrupted. */
    fun changeLanguage(code: String) {
        if (changingLanguage || code == cube.run.data.Languages.current(this)) return
        val parent = hud.parent as? FrameLayout ?: return
        require(cube.run.data.Languages.options.any { it.code == code })
        changingLanguage = true
        cube.run.data.Languages.select(this, code)
        val previous = hud
        previous.settleLanguageTransition()
        languageResources = cube.run.data.Languages.wrap(baseContext).resources
        hud = Hud(this).apply {
            layoutDirection = resources.configuration.layoutDirection
            setBest(Scores.best(SCORE_ID))
            showLanguagesAfterChange()
            alpha = 0f
        }
        hostSession.attach(hud)
        parent.addView(hud, FrameLayout.LayoutParams(-1, -1))
        parent.requestApplyInsets()
        // Block taps during the crossfade, including outside-menu taps, until both trees settle.
        val blocker = View(this).apply { isClickable = true; importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
        parent.addView(blocker, FrameLayout.LayoutParams(-1, -1))
        previous.animate().alpha(0f).setDuration(180).start()
        hud.animate().alpha(1f).setDuration(180).withEndAction {
            parent.removeView(previous)
            parent.removeView(blocker)
            changingLanguage = false
            hud.resumeLanguageIdle()
        }.start()
    }

    private lateinit var hud: Hud
    private lateinit var game: CubeRun
    private var returningToMenu = false

    /** Results and pause return to the live menu instead of rebuilding Android and GL. */
    fun returnToMenu() {
        if (returningToMenu || isFinishing || isDestroyed) return
        returningToMenu = true
        com.badlogic.gdx.Gdx.app.postRunnable {
            hostSession.resetToMenu()
            game.resetToMenu()
            runOnUiThread {
                if (!isFinishing && !isDestroyed) hud.finishMenuReturn()
                returningToMenu = false
            }
        }
    }
    private val physicalInput = PhysicalInput { action ->
        Stage.userInteraction()
        if (::hud.isInitialized && hud.handlePhysicalAction(action)) return@PhysicalInput
        if (::game.isInitialized) {
            val host = this
            com.badlogic.gdx.Gdx.app.postRunnable {
                if (!host.isFinishing && !host.isDestroyed) game.onPhysicalAction(action)
            }
        }
    }
    private var gameSurface: SurfaceView? = null
    private var backCallback: android.window.OnBackInvokedCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        cube.run.core.LaunchTrace.mark("activity start")
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Debug builds only: adb shortcuts for testing individual sections and worlds.
        if (BuildConfig.DEBUG) {
            if (intent.hasExtra("section") || intent.hasExtra("pillworld") || intent.hasExtra("spaceworld") || intent.hasExtra("bonusnow"))
                Settings.leavePerformanceCourse()
            // Opt-in APK preset: survives launcher starts and RESTART without adb extras.
            if (BuildConfig.JACKPOT_TEST_WORLD && !Settings.performanceCourse) {
                Settings.setDevMode(true)
                Progress.enterDev()
                Settings.testSection = 56 // MOTHERLODE: an obstacle-free field of coins.
                Settings.testPillWorld = false; Settings.testSpaceWorld = false
                Settings.testBonus = -1
                Settings.testBonusNow = -1
                Progress.buy(cube.run.data.Wardrobe.CUBE, 1)
                Progress.equip(cube.run.data.Wardrobe.CUBE, 1)
            }
            if (intent.getBooleanExtra("dev", false) && !Settings.devMode) { Settings.setDevMode(true); Progress.enterDev() }
            intent.getIntExtra("section", -2).let { if (it >= -1) { Settings.testSection = it; Settings.testPillWorld = false; Settings.testSpaceWorld = false } }
            if (intent.hasExtra("pillworld")) Settings.testPillWorld = intent.getBooleanExtra("pillworld", false)
            if (intent.hasExtra("spaceworld")) Settings.testSpaceWorld = intent.getBooleanExtra("spaceworld", false)
            intent.getIntExtra("bonus", -2).let { if (it >= -1) Settings.testBonus = it }
            intent.getIntExtra("world", -2).let { if (it >= -1) Settings.testWorld = it }
            intent.getIntExtra("boxes", -1).let { if (it >= 0) Settings.testBoxes = it }
            intent.getIntExtra("bonusnow", -2).let { if (it >= -1) Settings.testBonusNow = it }
        }
        val session = GameHostSession(this, SCORE_ID)
        hostSession = session
        // A real milestone-driven loader replaces the old timed opening animation.
        // Only a fresh Activity launch needs the cover; after loading, the main menu stays idle.
        val showStartupLoader = savedInstanceState == null
        val startupLoader = if (showStartupLoader) StartupLoadingView(this) else null
        startupLoader?.setProgress(0, getString(R.string.loading_status_start))
        val firstWorld = when {
            Settings.testPillWorld -> cube.run.data.Worlds.NEON_CITY_ID
            Settings.testWorld >= 0 -> Settings.testWorld
            Settings.selectedWorld >= 0 -> Settings.selectedWorld
            else -> cube.run.data.Worlds.campaign.random().id
        }
        cube.run.intro.LaunchAppearance.remember(this, firstWorld)
        game = CubeRun(session, autoStart = intent.getBooleanExtra(Hud.EXTRA_AUTOSTART, false),
            idleBotStart = intent.getBooleanExtra(Hud.EXTRA_IDLE_BOT, false), launchOpening = false,
            openingClock = null, firstWorld = firstWorld)
        hud = Hud(this, returningToMenu = intent.hasExtra(Hud.EXTRA_AUTOSTART) && !intent.getBooleanExtra(Hud.EXTRA_AUTOSTART, false))
        hud.setBest(Scores.best(SCORE_ID)); session.attach(hud)
        startupLoader?.setProgress(18, getString(R.string.loading_status_data))
        game.onFirstFrame = { startupLoader?.setProgress(72, getString(R.string.loading_status_graphics)) }

        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
            useAccelerometer = false
            useCompass = false
            useRotationVectorSensor = false
            // All game audio uses SoundFx. Avoid opening a second, unused SoundPool.
            disableAudio = true
            useGL30 = true // libGDX falls back to GLES 2 where GLES 3 is unavailable
            numSamples = 2
            r = 8; g = 8; b = 8; a = 8
            depth = 24 // 16-bit z-fights at the far end of the long draw distance
        }
        cube.run.core.LaunchTrace.mark("gdx host begin")
        startupLoader?.setProgress(28, getString(R.string.loading_status_graphics))
        val gameView = initializeForView(game, config)
        cube.run.core.LaunchTrace.mark("gdx host ready")
        startupLoader?.setProgress(42, getString(R.string.loading_status_world))
        gameSurface = gameView as? SurfaceView
        // Explicitly request the game's render cadence; Android can still lower
        // it for battery/thermal policy. Never change the user's display settings.
        @Suppress("DEPRECATION")
        val display = windowManager.defaultDisplay
        val mode = display.supportedModes.filter {
            it.physicalWidth == display.mode.physicalWidth && it.physicalHeight == display.mode.physicalHeight && it.refreshRate <= 90.5f
        }.maxByOrNull { it.refreshRate } ?: display.mode
        window.attributes = window.attributes.apply { preferredRefreshRate = mode.refreshRate }
        (gameView as? SurfaceView)?.holder?.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) = requestRate(holder)
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = requestRate(holder)
            override fun surfaceDestroyed(holder: SurfaceHolder) {}
            private fun requestRate(holder: SurfaceHolder) {
                if (android.os.Build.VERSION.SDK_INT >= 30 && holder.surface.isValid)
                    holder.surface.setFrameRate(mode.refreshRate, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)
            }
        })

        val root = FrameLayout(this).apply {
            setBackgroundColor(cube.run.intro.OpeningPose.INK)
            clipToPadding = false
            // Some devices (including keypad phones) cannot hide their navigation
            // bar. Keep both the GL viewport and controls inside the usable window.
            setOnApplyWindowInsetsListener { view, insets ->
                // Split screen can keep a status bar visible. Reserve it for the
                // HUD, but let the scene fill that area behind the status icons.
                fun extendScene(top: Int) {
                    for (scene in listOf(gameView)) {
                        val params = scene.layoutParams as? FrameLayout.LayoutParams ?: continue
                        if (params.topMargin != -top) {
                            params.topMargin = -top
                            scene.layoutParams = params
                        }
                    }
                }
                if (android.os.Build.VERSION.SDK_INT >= 30) {
                    val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars())
                    val gestureOverlay = insets.getInsets(android.view.WindowInsets.Type.systemGestures()).bottom > 0 &&
                        insets.getInsets(android.view.WindowInsets.Type.tappableElement()).bottom == 0
                    val bottom = if (gestureOverlay) insets.getInsets(android.view.WindowInsets.Type.captionBar()).bottom else bars.bottom
                    val reservedTop = insets.getInsets(android.view.WindowInsets.Type.navigationBars() or
                        android.view.WindowInsets.Type.captionBar()).top
                    // Revealing transient bars in fullscreen must not move the
                    // menu anchors. Permanent cutouts still reach the HUD.
                    val top = if (isInMultiWindowMode) bars.top else reservedTop
                    view.setPadding(bars.left, top, bars.right, bottom)
                    extendScene((top - reservedTop).coerceAtLeast(0))
                    insets.inset(bars.left, top, bars.right, bottom)
                } else {
                    @Suppress("DEPRECATION")
                    val bars = intArrayOf(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                        insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
                    val top = if (isInMultiWindowMode) bars[1] else 0
                    view.setPadding(bars[0], top, bars[2], bars[3])
                    extendScene(top)
                    if (android.os.Build.VERSION.SDK_INT >= 29) {
                        insets.inset(bars[0], top, bars[2], bars[3])
                    } else {
                        @Suppress("DEPRECATION")
                        insets.replaceSystemWindowInsets(0, 0, 0, 0)
                    }
                }
            }
        }
        root.addView(gameView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        root.addView(hud, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        startupLoader?.let { root.addView(it, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT) }
        if (android.os.Build.VERSION.SDK_INT >= 31) splashScreen.setOnExitAnimationListener { it.remove() }
        // TCL Flip 2's Activity.setContentView enables a proprietary 30px MenuBar
        // in DecorView, even in immersive mode and outside system-bar insets.
        // Install directly on Window to avoid that hook. Our NoActionBar theme
        // doesn't need Activity's action-bar initialization; Window still sends
        // onContentChanged and requests insets for the entire game/HUD tree.
        window.setContentView(root)
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            val decor = window.decorView as android.view.ViewGroup
            // DecorView forces status-bar consumption in split screen even with
            // decorFitsSystemWindows=false. Let the game own that inset instead:
            // its scene fills the pane and its HUD reserves space for the icons.
            decor.setOnApplyWindowInsetsListener { view, insets ->
                val applied = view.onApplyWindowInsets(insets)
                val status = insets.getInsets(android.view.WindowInsets.Type.statusBars())
                val remaining = applied.getInsets(android.view.WindowInsets.Type.statusBars())
                val consumedTop = (status.top - remaining.top).coerceAtLeast(0)
                val navigation = insets.getInsets(android.view.WindowInsets.Type.navigationBars())
                val gestureOverlay = insets.getInsets(android.view.WindowInsets.Type.systemGestures()).bottom > 0 &&
                    insets.getInsets(android.view.WindowInsets.Type.tappableElement()).bottom == 0
                val consumedBottom = if (gestureOverlay) (navigation.bottom - applied.getInsets(
                    android.view.WindowInsets.Type.navigationBars()).bottom).coerceAtLeast(0) else 0
                var content: View = root
                while (content.parent is View && content.parent !== decor) content = content.parent as View
                val params = content.layoutParams as? android.view.ViewGroup.MarginLayoutParams
                val restoreTop = params != null && consumedTop > 0 && params.topMargin >= consumedTop
                val restoreBottom = params != null && consumedBottom > 0 && params.bottomMargin >= consumedBottom
                if (params != null && (restoreTop || restoreBottom)) {
                    if (restoreTop) params.topMargin -= consumedTop
                    if (restoreBottom) params.bottomMargin -= consumedBottom
                    content.layoutParams = params
                    android.view.WindowInsets.Builder(applied)
                        .apply {
                            if (restoreTop) setInsets(android.view.WindowInsets.Type.statusBars(), status)
                            if (restoreBottom) setInsets(android.view.WindowInsets.Type.navigationBars(), navigation)
                        }.build()
                } else applied
            }
        }
        cube.run.core.LaunchTrace.mark("content attached")
        startupLoader?.setProgress(88, getString(R.string.loading_status_world))
        if (startupLoader != null) {
            game.afterFreshSceneFrame {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        cube.run.core.LaunchTrace.mark("menu scene ready")
                        startupLoader.finishLoading()
                    }
                }
            }
        }
        goFullscreen()
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            backCallback = android.window.OnBackInvokedCallback { if (::hud.isInitialized) hud.navigateBack() }.also {
                onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, it)
            }
        }
    }

    @Deprecated("Legacy Back dispatch")
    override fun onBackPressed() { if (::hud.isInitialized) hud.navigateBack() }

    override fun onDestroy() {
        if (android.os.Build.VERSION.SDK_INT >= 33) backCallback?.let { onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it) }
        super.onDestroy()
    }

    /** Edge to edge with every system bar hidden (they come back with a swipe and hide again). */
    private fun goFullscreen() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(android.view.WindowInsets.Type.systemBars())
                it.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) physicalInput.reset()
        if (hasFocus) goFullscreen()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        physicalInput.key(event) || super.dispatchKeyEvent(event)

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean =
        physicalInput.motion(event) || super.dispatchGenericMotionEvent(event)

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN && ::hud.isInitialized) hud.clearHardwareFocus()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> Stage.userInteraction(down = true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> Stage.pointerDown = false
        }
        // Discrete flicks need the first threshold crossing, not resampled drag
        // positions. Keep Android's normal batching for smooth positional control.
        if (event.actionMasked == MotionEvent.ACTION_DOWN && !Settings.smoothControl &&
            Stage.mode == Stage.NONE && !Stage.paused)
            gameSurface?.requestUnbufferedDispatch(event)
        return super.dispatchTouchEvent(event)
    }

    /** Leaving the app mid-run (home, a call) pauses it: the run resumes from the pause card. */
    override fun onPause() {
        physicalInput.reset()
        Stage.userInteraction()
        cube.run.core.SoundFx.pause()
        if (::hud.isInitialized) hud.autoPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        cube.run.core.SoundFx.resume()
        goFullscreen()
    }

    companion object {
        private const val SCORE_ID = "cuberun"
    }
}
