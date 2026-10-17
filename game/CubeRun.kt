package cube.run.game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import cube.run.BuildConfig
import cube.run.core.Gdx3DGame
import cube.run.core.GameSession
import cube.run.core.Stage
import cube.run.core.SoundFx
import cube.run.core.Haptics
import cube.run.data.Skins
import cube.run.game.track.Ob
import com.badlogic.gdx.math.Vector3
import cube.run.data.Bonus
import cube.run.data.BubbleSkins
import cube.run.data.Progress
import cube.run.data.Scores
import cube.run.data.Settings
import cube.run.game.stage.GiftStage
import cube.run.game.stage.Showcase
import cube.run.game.space.SpaceWorld
import cube.run.game.space.SpaceTesseract
import cube.run.game.space.BlackHoleChallenge
import cube.run.game.space.SpaceEscapeClock
import cube.run.game.track.Coin
import cube.run.game.track.CoinType
import cube.run.game.track.ZenDissolve
import cube.run.game.track.Debris
import cube.run.game.track.ObType
import cube.run.game.track.ObstacleFactory
import cube.run.game.track.Pickup
import cube.run.game.track.RoadAnimal
import cube.run.game.track.AirBird
import cube.run.game.track.Row
import cube.run.game.track.Track
import cube.run.game.track.TrackRenderer
import cube.run.game.world.Fog
import cube.run.game.world.JungleHarvest
import cube.run.game.world.Scenery
import cube.run.game.world.WorldRunner
import cube.run.game.world.WorldMission
import cube.run.game.world.WorldMissionRoute
import cube.run.game.world.SpacePortalTransition
import cube.run.game.world.PortalArrivalSafety
import cube.run.core.hsvInto
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Cube Run — 3-lane endless runner. The world rushes toward the camera;
 * swipe LEFT/RIGHT to snap lanes, UP to jump, DOWN to roll (on the ground)
 * or slam (mid-air). Double-tap to pop a bubble shield (if you have one).
 *
 * This class is the conductor: it owns the run state (started / dead), routes
 * input, resolves collisions + pickups + scoring against the [Track], and
 * hands everything else to its pieces:
 *  - [Track] + [TrackRenderer] — the lane-walk director and how it looks
 *  - [Player] — physics (platforms, bounces, jetpack flight), pose, skin, trail
 *  - [Bubble] — the shield; [PowerUps] — magnet / 2× / jetpack timers + HUD bars
 *  - [Difficulty] + [FireBoost] — speed / tier and the opening boost button
 *  - [WorldRunner] + [Scenery] — the worlds, their sky and roadside, the gates
 *  - [RunCamera] — the chase rig; [RunFx] — every sound / flash / burst
 *  - [Jackpot] — the Gambler's jackpot show, which holds the run while it plays
 *  - [GiftStage] / [Showcase] — the 3D stages the run-over and wardrobe screens use
 *
 * +1 per row (2× with the multiplier), near-miss bonus for shaving an
 * obstacle; coins and mystery boxes are handed to the session and banked at
 * game over.
 */
class CubeRun(session: GameSession, private var autoStart: Boolean = false, private val idleBotStart: Boolean = false, launchOpening: Boolean = false, openingClock: cube.run.intro.OpeningClock? = null,
              private val firstWorld: Int? = null) : Gdx3DGame(session) {

    private val opening = CubeOpening(launchOpening, openingClock)
    override val hasLaunchOpening = launchOpening
    var onOpeningProgress: ((Float) -> Unit)? = null
    fun finishOpening() { opening.finish() }

    private val tmpCol = Color()
    private val phasePosition = Vector3()

    private class CourseRandom : Random() {
        private var course = Settings.performanceCourse
        private var source = if (course) Random(73) else Random(System.nanoTime())
        override fun nextBits(bitCount: Int) = source.nextBits(bitCount)
        fun reset() {
            val selected = Settings.performanceCourse
            if (selected) source = Random(73)
            else if (course) source = Random(System.nanoTime())
            course = selected
        }
    }
    private val rnd = CourseRandom()
    private val obstacles = ObstacleFactory(rnd)
    private val track = Track(rnd, obstacles)
    private val trackArt = TrackRenderer(this)
    private val debris = Debris(this)
    private val zenDissolve = ZenDissolve(this)
    private val player = Player(this, rnd)
    private val bubble = Bubble(this)
    private val hound = HoundChaser(this)
    private var houndGrace = 0f
    private var shownBubbleCooldown = 0
    private val powerUps = PowerUps()
    private val redPill = RedPill()
    private val difficulty = Difficulty()
    private val fire = FireBoost(this, difficulty)
    private val scenery = Scenery(this, rnd)
    private val skyBackdrop = cube.run.game.world.WorldSkyBackdrop()
    private val worlds = WorldRunner(this, scenery, rnd)
    private val space = SpaceWorld(this)
    private val tesseract = SpaceTesseract(this)
    private val blackHole = BlackHoleChallenge(this)
    private val collisionActionDeferral = CollisionActionDeferral()
    private val jungleHarvest = JungleHarvest()
    private val worldMission = WorldMission()
    private val spaceMission = WorldMission()
    private val nexusMission = WorldMission()
    private var portalSafetyLeft = 0f
    private var objectiveToast = ""
    private var objectiveToastLeft = 0f
    private var lastPx = 0f          // the cube's x last frame (the space camera banks with its glide)
    private val gift = GiftStage(this)
    private val showcase = Showcase(this, player, bubble)
    private val fx = RunFx(this, rnd)
    private val jackpot = Jackpot(this, rnd)
    private lateinit var rig: RunCamera
    private var shopMenuIntroT = 0f
    private var appliedWorldPreview = -1

    // ---- run state ----
    private val idlePilot = IdlePilot()
    private var initialInteraction = 0
    private var started = false
    private var dead = false
    private var spd = 4.5f
    private var dist = 0f
    private var rowsPassed = 0
    private var curTier = 0          // last tier reached (a chime marks each unlock)
    private var runT = 0f            // seconds since the run began (the start ease)
    private var startGateRunT = -1f  // Stage Fright starts when the gate actually passes the cube
    private var introT = 0f          // seconds since launch (the menu shot's swoop in)
    private var introAtStart = 0f    // where the swoop was when the run began (the start eases on from there)
    private var skyBlend = 0f        // 1 → 0: the stage's sky fading back into the world's after a page closes
    private var bonus = Bonus.NONE   // the bonus world we are in
    private var kaleido = 0f         // eased 0..1: the Kaleidoscope's colour cycling + sway
    private var kaleidoHue = 0f
    private var coinsRunF = 0.0     // preserve fractional Rich coins / Gold rewards
    private var lottery = Lottery(rnd)
    private var runBubble = BubbleSkins.get(0)
    private var jetGrace = 0f        // safe landing window after a jetpack flight
    private var jetBoost = 0f        // eased 0..1: how much of the jetpack's speed boost is on
    private val jetGlide = 1.4f      // the last seconds of a flight glide back down to the ground
    private val jetSpeedUp = 0.75f   // the jetpack's speed boost (+75%)
    private val skateboardDuration = 9f
    private val skateboardSpeedBoost = 0.12f
    private val jumpShoesDuration = 10f
    private val phaseDuration = 5f
    private val timeWarpDuration = 5f
    private val timeWarpSpeed = 0.72f

    // ---- death: let the crash animation play before the run-over screens ----
    private val deathAnimTime = 1.5f
    private var deathT = 0f
    private var gameOverShown = false

    // ---- style points: tap mid-air for an ascending combo (purely for flair) ----
    private var styleCombo = 0
    private var runSkin = Skins.get(0)
    /** The Zen cube is out: obstacles melt away; only Monk counts. */
    private var zen = false
    private var zenStreak = 0
    private val zenCeiling = 0.4f     // the auto-climb rests at an unhurried pace
    private var phaseUsed = false
    private var phasedObstacle: Ob? = null
    private var groundObstacle: Ob? = null
    private var lastTapT = -9f       // for double-tap detection (bubble activation)

    // ---- goodies ----
    private var coinsRun = 0         // collected this run (banked by the session at game over)
    private var lastCollectedBox: Row? = null
    // Separate from the course generator: poison rolls do not change obstacle layouts.
    private var coalRandom: Random = Random.Default
    private var toxicRandom: Random = Random.Default
    private var boxesRun = 0         // mystery boxes collected this run (opened on the run-over screens)
    private var coinStreak = 0       // consecutive pickups without a miss (the milestone chimes)
    private var coinPitch = 0        // rising coin pitch; resets after a short gap without a coin
    private var lastCoinT = -9f
    private val nearMissBonus = 2

    // smooth-control gesture state (positional steering within one continuous touch)
    private var smoothAnchorX = 0f
    private var smoothAnchorLane = 1
    private var smoothVAccum = 0f
    private var sideBounces = 0
    private var smoothWall = 0

    /**
     * Count the same edge contact that plays the visible bonk. Classic input
     * already emits once per touch; smooth input calls this only on wall entry.
     * Do not debounce using simulation time: Android can queue several separate
     * flicks before one GL frame, and slow motion must not discard real touches.
     */
    private fun sideBounce(dir: Int) {
        player.bonk(dir)
        sideBounces++
        Progress.recordRunProgress(session.score, sideBounces)
    }

    /** Obstacles and the cube's skin key off the current world's hue. */
    private fun worldHue() = worlds.hue

    private var handledBlackHoleOutcome = SpaceEscapeClock.State.READY

    private fun beginWorldObjective(worldId: Int) {
        worldMission.enterWorld(worldId)
        spaceMission.clear()
        nexusMission.clear()
        if (worldId == cube.run.data.Worlds.SPACE_ID) spaceMission.enterSpace()
        jungleHarvest.enter(worldId == cube.run.data.Worlds.JUNGLE_ID)
        objectiveToast = ""; objectiveToastLeft = 0f
        publishObjective()
    }

    private fun publishObjective() {
        if (portalSafetyLeft > 0f) {
            Stage.runObjective = ""
            Stage.runObjectiveProgress = 0f; Stage.runObjectiveTone = 0
            return
        }
        if (objectiveToastLeft > 0f) {
            Stage.runObjective = objectiveToast
            Stage.runObjectiveProgress = if (objectiveToastTone == 2) 1f else 0f
            Stage.runObjectiveTone = objectiveToastTone
            return
        }
        when {
            blackHole.active -> {
                val seconds = (blackHole.clock.secondsLeft + .999f).toInt().coerceAtLeast(0)
                Stage.runObjective = "SINGULARITY • REACH THE RIFT  ${seconds}s"
                Stage.runObjectiveProgress = blackHole.clock.timerProgress
                Stage.runObjectiveTone = if (seconds <= 4) 1 else 0
            }
            blackHole.state == SpaceEscapeClock.State.CAPTURED -> {
                Stage.runObjective = "THE SINGULARITY TOOK YOU"
                Stage.runObjectiveProgress = 0f; Stage.runObjectiveTone = 1
            }
            spaceMission.active && WorldMissionRoute.usesGalaxyMission(bonus, worlds.world.id) -> {
                Stage.runObjective = if (spaceMission.completed) "STARDUST SECURED" else
                    "STARDUST • COINS ${spaceMission.collected}/${spaceMission.target}"
                Stage.runObjectiveProgress = spaceMission.progress
                Stage.runObjectiveTone = if (spaceMission.completed) 2 else 0
            }
            nexusMission.active && bonus == Bonus.NEXUS -> {
                Stage.runObjective = if (nexusMission.completed) "NEXUS FRAGMENTS SECURED" else
                    "NEXUS RUN • DATA ${nexusMission.collected}/${nexusMission.target}"
                Stage.runObjectiveProgress = nexusMission.progress
                Stage.runObjectiveTone = if (nexusMission.completed) 2 else 0
            }
            worldMission.active -> {
                Stage.runObjective = when (worldMission.kind) {
                    WorldMission.Kind.CITY_BLUE_COINS -> if (worldMission.completed) "CITY CACHE SECURED"
                        else "CITY RUN • BLUE COINS ${worldMission.collected}/${worldMission.target}"
                    WorldMission.Kind.WINTER_ROWS -> if (worldMission.completed) "WINTER ROUTE CLEARED"
                        else "WINTER RUN • SURVIVE ${worldMission.collected}/${worldMission.target}"
                    WorldMission.Kind.NIGHT_PREDATORS -> if (worldMission.completed) "PREDATORS OUTRUN"
                        else "MOONLIT WILD • EVADE ${worldMission.collected}/${worldMission.target}"
                    WorldMission.Kind.HACKER_DATA -> if (worldMission.completed) "DATA CACHE DECRYPTED"
                        else "MATRIX RUN • DATA ${worldMission.collected}/${worldMission.target}"
                    WorldMission.Kind.JUNGLE_DINOSAURS -> if (worldMission.completed) "DINOSAURS EVADED"
                        else "CANOPY RUN • EVADE ${worldMission.collected}/${worldMission.target}"
                    WorldMission.Kind.LAVA_CRYSTALS -> if (worldMission.completed) "LAVA CRYSTALS SECURED"
                        else "LAVA RUN • CRYSTALS ${worldMission.collected}/${worldMission.target}"
                    WorldMission.Kind.NEXUS_FRAGMENTS -> if (worldMission.completed) "NEXUS FRAGMENTS SECURED"
                        else "NEXUS RUN • DATA ${worldMission.collected}/${worldMission.target}"
                    WorldMission.Kind.RAILWAY_ROWS -> if (worldMission.completed) "TRAIN YARD CLEARED"
                        else "RAILWAY RUN • SURVIVE ${worldMission.collected}/${worldMission.target}"
                    WorldMission.Kind.BEACH_ROWS -> if (worldMission.completed) "SHORELINE CLEARED"
                        else "BEACH RUN • SURF ${worldMission.collected}/${worldMission.target}"
                    WorldMission.Kind.AIR_BIRDS -> if (worldMission.completed) "SKY ROUTE CLEARED"
                        else "AIR RUN • DODGE BIRDS ${worldMission.collected}/${worldMission.target}"
                    else -> ""
                }
                Stage.runObjectiveProgress = worldMission.progress
                Stage.runObjectiveTone = if (worldMission.completed) 2 else 0
            }
            jungleHarvest.active -> {
                Stage.runObjective = "CANOPY HARVEST • GREEN ${jungleHarvest.collected}/${jungleHarvest.target}"
                Stage.runObjectiveProgress = jungleHarvest.progress; Stage.runObjectiveTone = 0
            }
            else -> {
                Stage.runObjective = ""
                Stage.runObjectiveProgress = 0f; Stage.runObjectiveTone = 0
            }
        }
    }

    private var objectiveToastTone = 0
    private fun showObjectiveToast(text: String, tone: Int, seconds: Float) {
        objectiveToast = text
        objectiveToastTone = tone
        objectiveToastLeft = seconds
        publishObjective()
    }

    private fun beginSafePortalArrival() {
        portalSafetyLeft = max(portalSafetyLeft, PortalArrivalSafety.DURATION_SECONDS)
        track.beginPortalSafety(PortalArrivalSafety.corridorDistance(spd))
    }

    private fun tickObjective(dt: Float) {
        portalSafetyLeft = max(0f, portalSafetyLeft - dt)
        if (objectiveToastLeft > 0f) {
            objectiveToastLeft = max(0f, objectiveToastLeft - dt)
            if (objectiveToastLeft == 0f) objectiveToast = ""
        }
        publishObjective()
    }

    private fun awardRunCoins(amount: Int) {
        if (amount <= 0) return
        coinsRunF = (coinsRunF + amount).coerceAtMost(Int.MAX_VALUE.toDouble())
        coinsRun = (coinsRunF + .0000001).toInt()
        session.setCoins(coinsRun)
    }

    private fun completeWorldObjective(message: String) {
        awardRunCoins(25)
        session.addScore(15)
        showObjectiveToast("$message • +25 COINS", 2, 3f)
        SoundFx.play("success"); Haptics.success()
    }

    private fun worldMissionCoin(kind: CoinType) {
        if (portalSafetyLeft > 0f) return
        if (WorldMissionRoute.usesGalaxyMission(bonus, worlds.world.id)) {
            val before = spaceMission.collected
            if (spaceMission.collectCoin(kind)) completeWorldObjective("STARDUST COLLECTED")
            else if (spaceMission.collected > before) publishObjective()
            return
        }
        if (bonus == Bonus.NEXUS) {
            val before = nexusMission.collected
            if (nexusMission.collectCoin(kind)) completeWorldObjective("NEXUS FRAGMENTS SECURED")
            else if (nexusMission.collected > before) publishObjective()
            return
        }
        if (bonus != Bonus.NONE) return
        val before = worldMission.collected
        if (worldMission.collectCoin(kind)) {
            val message = when (worldMission.kind) {
                WorldMission.Kind.CITY_BLUE_COINS -> "CITY CACHE SECURED"
                WorldMission.Kind.HACKER_DATA -> "DATA CACHE DECRYPTED"
                WorldMission.Kind.JUNGLE_DINOSAURS -> "DINOSAURS EVADED"
                WorldMission.Kind.LAVA_CRYSTALS -> "LAVA CRYSTALS SECURED"
                WorldMission.Kind.NEXUS_FRAGMENTS -> "NEXUS FRAGMENTS SECURED"
                else -> "ROUTE OBJECTIVE CLEARED"
            }
            completeWorldObjective(message)
            return
        }
        if (worldMission.collected > before) publishObjective()
        if (jungleHarvest.collect(kind)) {
            completeWorldObjective("CANOPY CLEARED")
        } else if (kind == CoinType.GREEN && jungleHarvest.active) publishObjective()
    }

    /** World-gate entry to the campaign's Space stage; regular Space portals use crossPortal(). */
    private fun beginSpaceWorldStage() {
        if (space.inside || bonus != Bonus.NONE) return
        bonus = Bonus.SPACE
        track.forceBonus(Bonus.SPACE)
        Terrain.set(false)
        session.setBonus(Bonus.SPACE)
        space.enter(rnd.nextInt())
        worlds.hold = true
        fx.warp(space.neon)
        rig.punch(.6f)
        beginBlackHoleChallenge()
    }

    private fun beginBlackHoleChallenge() {
        // A Space portal can be crossed inside collide(), which is iterating
        // track.rows. Resetting the old exit and inserting the new one must be
        // deferred until that iterator has finished.
        if (collisionActionDeferral.defer { beginBlackHoleChallengeNow() }) return
        beginBlackHoleChallengeNow()
    }

    private fun beginBlackHoleChallengeNow() {
        blackHole.reset(track)
        handledBlackHoleOutcome = SpaceEscapeClock.State.READY
        beginSafePortalArrival()
        if (!blackHole.begin(track)) return
        publishObjective()
        SoundFx.play("boom", rate = .72f, vol = .62f)
        SoundFx.play("rise", rate = .7f, vol = .55f)
        Haptics.heavy()
        flash(Color(1f, .25f, .58f, 1f), .32f)
        shake(.36f)
    }

    private fun handleBlackHoleOutcome(state: SpaceEscapeClock.State) {
        if (state == handledBlackHoleOutcome) return
        if (state != SpaceEscapeClock.State.ESCAPED && state != SpaceEscapeClock.State.CAPTURED) return
        handledBlackHoleOutcome = state
        when (state) {
            SpaceEscapeClock.State.ESCAPED -> {
                awardRunCoins(40)
                session.addScore(30)
                showObjectiveToast("RIFT REACHED • +40 COINS", 2, 3.2f)
                SoundFx.play("success", rate = 1.25f)
                Haptics.success()
                flash(Color.CYAN, .35f)
                burst3d(phasePosition.set(player.px, player.py, 0f), Color.CYAN, n = 24, speed = 5f, size = .1f, life = .55f, gravity = 0f)
            }
            SpaceEscapeClock.State.CAPTURED -> {
                showObjectiveToast("THE SINGULARITY TOOK YOU", 1, 5f)
                SoundFx.play("boom", rate = .58f, vol = .9f)
                Haptics.heavy()
                flash(Color(1f, .08f, .16f, 1f), .68f)
                shake(.8f)
                die() // This timed encounter is fatal when its clock runs out; a stocked bubble cannot cancel the deadline.
            }
            else -> Unit
        }
    }

    override fun runtimeBreadcrumb(): String =
        "active=$started dead=$dead score=${session.score} rows=${track.rows.size} " +
            "world=${worlds.world.name} spawnWorld=${worlds.spawnWorld.name} bonus=$bonus lanes=${Lanes.count} " +
            "space=${space.inside} escape=${blackHole.state} portals=${track.rows.count { it.portal >= 0 }}"

    override fun init() {
        Stage.reset()
        initialInteraction = Stage.interactions.get()
        Lanes.reset(); Terrain.reset()
        setTerrain { z -> Terrain.y(z) }
        zen = false; zenStreak = 0; Progress.zenRun = false
        worlds.reset(firstWorld)
        player.setAutoMapSkin(worlds.world.id)
        track.themeWorldId = worlds.spawnWorld.id
        SoundFx.setMusicWorld(worlds.world.id)
        scenery.init(worlds.world)
        scenery.space = space
        bgTop.set(worlds.skyTop); bgBottom.set(worlds.skyBottom)
        player.init(worldHue(), time)
        bubble.init()
        hound.init()
        scenery.spawnStartGate(hsvInto(tmpCol, worldHue() + 180f, 0.7f, 1f))
        rig = RunCamera(cam)
        if (hasLaunchOpening) { introT = 1.8f; rig.intro = 0f }
        cam.position.set(0f, 3.7f, 6.4f)
        cam.lookAt(0f, 1.0f, -8f)
        cam.update()
        if (opening.active) {
            player.update(0f, 0f, time, worldHue(), trail = false, groundH = 0f)
            opening.pose(player, cam, worldHue())
            bgTop.set(CubeOpening.INK); bgBottom.set(CubeOpening.INK)
        }
        session.setBubbles(Progress.bubbles)
        session.setWorld(worlds.world.name)
    }

    /** GL-thread navigation: reset the run, retaining renderer allocations and the live surface. */
    fun resetToMenu() {
        lastCollectedBox = null
        finishRendererStartup()
        idlePilot.stop()
        opening.finish()
        if (gift.active) gift.exit()
        if (showcase.active) showcase.exit(bgTop, bgBottom)
        jackpot.reset(); hound.reset(); houndGrace = 0f
        Stage.reset(); Lanes.reset(); Terrain.reset()
        initialInteraction = Stage.interactions.get()
        autoStart = false; started = false; dead = false; gameOverShown = false
        spd = 0f; dist = 0f; rowsPassed = 0; curTier = 0; runT = 0f; deathT = 0f
        introT = 1.8f; introAtStart = 0f; skyBlend = 0f; bonus = Bonus.NONE
        kaleido = 0f; kaleidoHue = 0f; jetGrace = 0f; jetBoost = 0f
        phaseUsed = false; phasedObstacle = null; groundObstacle = null
        zen = false; zenStreak = 0; Progress.zenRun = false; difficulty.ceiling = 1f; calmWash = 0f
        styleCombo = 0; sideBounces = 0; smoothWall = 0; smoothVAccum = 0f
        smoothAnchorX = 0f; smoothAnchorLane = 1; lastTapT = -9f
        coinsRun = 0; coinsRunF = 0.0; boxesRun = 0; coinStreak = 0; coinPitch = 0; lastCoinT = -9f
        powerUps.reset(); redPill.reset(); bubble.reset(); shownBubbleCooldown = 0
        blackHole.reset(track); handledBlackHoleOutcome = SpaceEscapeClock.State.READY
        jungleHarvest.enter(false); worldMission.clear(); spaceMission.clear(); nexusMission.clear()
        objectiveToast = ""; objectiveToastLeft = 0f; portalSafetyLeft = 0f
        track.rows.clear(); debris.clear(); zenDissolve.clear()
        space.reset(); worlds.hold = false
        rnd.reset()
        worlds.reset(Settings.selectedWorld.takeIf { it >= 0 })
        player.setAutoMapSkin(worlds.world.id)
        track.themeWorldId = worlds.spawnWorld.id
        SoundFx.setMusicWorld(worlds.world.id)
        appliedWorldPreview = -1
        scenery.init(worlds.world)
        scenery.spawnStartGate(hsvInto(tmpCol, worldHue() + 180f, 0.7f, 1f))
        bgTop.set(worlds.skyTop); bgBottom.set(worlds.skyBottom)
        player.resetToMenu(worldHue(), time)
        rig.reset(); rig.intro = 0f; rig.roll = 0f; rig.wide = 0f
        rig.chase(0f, player.px, player.py, player.ground, 0f, 0f)
        resetPresentation()
        session.setWorld(worlds.world.name)
    }

    // --------------------------------------------------------------- events

    private fun live() = started && !dead && !session.isOver

    override fun paused(): Boolean {
        if (Stage.paused) { player.clearJumpInput(); idlePilot.stop(); space.pauseAudio() }
        return Stage.paused
    }

    private fun start() {
        if (started || session.isOver) return
        lastCollectedBox = null
        finishRendererStartup()
        opening.finish()
        started = true
        rnd.reset() // menu idle duration must not change the course sequence
        player.setAutoMapSkin(worlds.world.id)
        runSkin = Skins.get(Progress.skin)
        runBubble = BubbleSkins.get(Progress.bubbleSkin)
        zen = Skins.Ability.ZEN in runSkin.abilities
        difficulty.configure(Settings.runDifficulty)
        Progress.zenRun = zen
        zenStreak = 0
        player.zappyEnabled = Skins.Ability.ZAPPY in runSkin.abilities
        player.floaty = Skins.Ability.FLOATY in runSkin.abilities
        player.doubleJumpEnabled = false
        player.tripleJumpEnabled = false
        player.gigajumpEnabled = false
        trackArt.coalCoins = Skins.Ability.COAL in runSkin.abilities
        lottery = Lottery(rnd, if (Settings.devMode) Lottery.DEV_COIN_CHANCE else Lottery.COIN_CHANCE)
        phaseUsed = false; player.phaseSpent = false; phasedObstacle = null; lastTapT = -9f
        runT = 0f
        portalSafetyLeft = 0f
        startGateRunT = -1f
        sideBounces = 0; smoothWall = 0
        blackHole.reset(track); handledBlackHoleOutcome = SpaceEscapeClock.State.READY
        beginWorldObjective(worlds.world.id)
        introAtStart = rig.intro
        scenery.release() // the start gate comes at you
        fx.launch(player.px, player.py, player.trailCol())
        player.squashForLaunch()
        fire.reset(); styleCombo = 0
        coinsRun = 0; coinsRunF = 0.0; boxesRun = 0; coinStreak = 0
        if (Settings.devMode && Settings.testBoxes > 0) { boxesRun = Settings.testBoxes; session.setBoxes(boxesRun) } // dev: boxes to open
        track.portalPool = when {
            Settings.performanceCourse -> emptyList()
            Settings.testSpaceWorld -> listOf(Bonus.SPACE)
            Settings.testBonus >= 0 -> listOf(Settings.testBonus)
            Settings.devMode -> Bonus.all.map { it.id }
            else -> Bonus.unlocked(Progress.bestRunScore).map { it.id } // Space is always available; higher scores grow the destination pool
        }
        track.portalEvery = if (Settings.devMode || Settings.testSpaceWorld) 28 else (58 - 8 * Progress.level(Progress.PORTALS)).coerceAtLeast(38) // dev: portals galore too
        powerUps.reset(); redPill.reset(); jetGrace = 0f
        player.jumpShoesEnabled = false
        if (BuildConfig.DEBUG && BuildConfig.JACKPOT_TEST_WORLD && !Settings.performanceCourse) {
            track.portalPool = emptyList()
            powerUps.magnet.start(3600f)
        }
        bubble.reset(); shownBubbleCooldown = 0; session.setBubbleCooldown(0)
        bubble.cooldownDuration = 5f * runSkin.bubbleCooldownMultiplier
        bubble.duration = bubbleDuration()
        difficulty.ceiling = if (zen) minOf(zenCeiling, difficulty.preset.autoCeiling) else difficulty.preset.autoCeiling
        difficulty.reset()
        track.difficultyGapScale = difficulty.spacingScale
        track.spaceMinimumTier = difficulty.preset.spaceMinimumTier
        curTier = difficulty.tier()
        track.tier = curTier
        track.themeWorldId = worlds.spawnWorld.id
        val oldCount = Lanes.count
        val startBonus = when {
            Settings.testSpaceWorld -> Bonus.SPACE // section explorer: Galaxy from the first row
            worlds.world.id == cube.run.data.Worlds.SPACE_ID -> Bonus.SPACE
            Settings.devMode -> Settings.testBonusNow
            else -> Bonus.NONE
        }
        track.reset(coinTrailChance = 0.2f, hue = worldHue(), initialBonus = startBonus)
        prepareToxicCoins()
        space.reset()
        if (!track.isPillTest && startBonus >= 0) { // debug: begin inside a bonus world
            bonus = startBonus
            player.remapLane(oldCount, Lanes.count)
            Terrain.set(bonus == Bonus.HILLS)
            if (bonus == Bonus.SPACE) {
                space.enter(rnd.nextInt(), instant = true)
                worlds.hold = true
                beginBlackHoleChallenge()
            } else if (bonus == Bonus.NEXUS) {
                nexusMission.enterWorld(cube.run.data.Worlds.NEXUS_ID)
                worlds.hold = true
                scenery.setNexusBonus(true)
            }
            session.setBonus(bonus)
        }
        session.runStarted()
        if (Settings.devMode) session.setScore(950)
        if (bonus >= 0) session.setBonus(bonus)
        session.laneChanged(player.lane, Lanes.count)
        refreshJumpAbility()
        fx.runStart(worldHue())
        rig.punch(0.8f)
    }

    // Instrumentation installs this only for protected performance runs. Release
    // builds always use the normal fatal-collision path, even if reflection sets it.
    private var testCrashObserver: (() -> Unit)? = null
    // Observation only: counts contacts even when a legitimately collected shield saves them.
    internal var pilotContacts = 0
        private set

    private fun crash() {
        if (dead) return
        if (BuildConfig.DEBUG && testCrashObserver != null) { testCrashObserver!!.invoke(); return }
        if (Progress.useRevive()) { secondWind(); return }
        if (reachingForBox()) session.riskyBoxDeath()
        die()
    }

    /** Poison bypasses bubble saves, phase and revives; ordinary obstacles use crash(). */
    private fun die() {
        if (dead) return
        if (startGateRunT >= 0f) session.runCrashed(runT - startGateRunT)
        dead = true
        player.setFlying(false)
        fx.crash(player.px, player.py, player.col)
        // NB: the run-over screens are deferred (see tick) so the crash animation is visible.
    }

    private fun updateHound(dt: Float) {
        val event = hound.encounter.update(
            runT, dt, spd, player.lane, player.px, player.py, player.flying,
            allowed = started && !dead && bonus == Bonus.NONE && !jackpot.active,
        )
        when (event) {
            HoundEncounter.EVENT_STARTED -> {
                SoundFx.play("growl", rate = .72f, vol = .62f)
            }
            HoundEncounter.EVENT_LUNGE -> {
                SoundFx.play("growl", rate = 1.14f, vol = .78f)
                Haptics.tick()
            }
            HoundEncounter.EVENT_ESCAPED -> {
                SoundFx.play("whoosh", rate = .78f, vol = .6f)
                Haptics.success()
                burst3d(phasePosition.set(player.px, player.py, 0f), Color.CYAN,
                    n = 12, speed = 4f, size = .10f, life = .42f)
            }
            HoundEncounter.EVENT_HIT -> when {
                bubble.active -> {
                    bubble.pop(player.px, player.py)
                    houndGrace = .2f // one contact cannot also consume a nearby obstacle on this same step
                    hound.encounter.repel(runT)
                }
                zen -> {
                    houndGrace = .2f
                    hound.encounter.repel(runT)
                    SoundFx.play("success", rate = .78f, vol = .55f)
                }
                else -> {
                    crash()
                    if (!dead) {
                        houndGrace = .2f
                        hound.encounter.repel(runT) // revive/debug observer saved the run
                    }
                }
            }
        }
        Stage.houndAlert = hound.alert
    }

    private fun reachingForBox(): Boolean {
        if (!live() || player.py >= 1.7f) return false
        // A box sits 3.2 units behind its row. Include the obstacle immediately
        // before/after it, but never an old pickup or a box in another lane.
        fun near(row: Row) = !row.idle &&
            abs(row.z + Row.PICKUP_DZ) <= abs(Row.PICKUP_DZ) + .9f &&
            abs(player.px - row.pickupX) < .95f
        return lastCollectedBox?.let(::near) == true || track.rows.any {
            it.pickup == Pickup.BOX && !it.pickupMissed && near(it)
        }
    }

    /** A second wind: the crash becomes a smash — the row shatters, a bubble goes up, the run goes on. */
    private fun secondWind() {
        val zone = -(spd * 2.5f) - 4f
        for (r in track.rows) {
            if (r.z > zone && r.z < 1.2f && r.obs.isNotEmpty()) shatter(r, impact = r.z > -8f)
        }
        fx.smashFeedback()
        bubble.duration = 3f * bubbleDurationMultiplier()
        bubble.activate(player.px, player.py)
        bubble.duration = bubbleDuration()
        rig.punch(1f)
        slowMo(0.2f, 0.5f)
        flash(Color.WHITE, 0.6f)
    }

    private fun phase(ob: Ob): Boolean {
        if (phaseUsed || Skins.Ability.PHASE !in runSkin.abilities) return false
        phaseUsed = true
        player.phaseSpent = true
        phasedObstacle = ob
        SoundFx.play("whoosh", rate = 1.4f)
        Haptics.success()
        flash(Color.CYAN, 0.25f)
        burst3d(phasePosition.set(player.px, player.py, 0f), Color.CYAN, n = 20, speed = 3f, size = 0.12f, life = 0.6f)
        return true
    }

    /** A collision with a row: the bubble takes it if one is up; otherwise Zen melts it, or it is fatal. */
    private fun hit(row: Row) {
        when {
            bubble.active -> { zenStreak = 0; smash(row) } // a bubble keeps its job, even on the Zen cube
            zen -> melt(row)
            else -> crash()
        }
    }

    /** Zen: release a visual copy before removing the collision geometry. */
    private fun melt(row: Row) {
        for (ob in row.obs) if (ob.type == ObType.SOLID) zenDissolve.melt(ob, row.z)
        zenStreak = (zenStreak + row.obs.count { it.type == ObType.SOLID }).coerceAtMost(100)
        Progress.recordZenStreak(zenStreak)
        fx.melt(row)
        row.obs.removeAll { it.type == ObType.SOLID }
    }

    /** Ran into the side of a platform: fatal, or the bubble hoists you onto it. */
    private fun sideHit(groundH: Float) {
        if (idlePilot.active) pilotContacts++
        if (zen && !bubble.active) { // Zen lifts you onto the platform; a bubble still hoists (and pops) as usual
            player.forceGround(groundH)
            fx.zenLift(player.px, player.py)
            return
        }
        if (!bubble.active) {
            if (groundObstacle?.let { phase(it) } == true) return
            crash(); return
        }
        player.forceGround(groundH)
        bubble.pop(player.px, player.py)
        rig.punch(0.5f)
        fx.hoist()
    }

    /**
     * The bubble's one save: the row you hit shatters, everything in a short
     * breather zone ahead dissolves so you can recover, and the bubble pops.
     */
    private fun smash(row: Row) {
        val zone = -(spd * 2.2f) - 4f // ≈ 2 s of clean track
        shatter(row, impact = true)
        for (r in track.rows) {
            if (r !== row && r.z > zone && r.z < 0f && r.obs.isNotEmpty()) shatter(r, impact = false)
        }
        fx.smashFeedback()
        bubble.pop(player.px, player.py)
        rig.punch(0.6f)
        session.addScore(3)
    }

    private fun shatter(row: Row, impact: Boolean) {
        fx.smashRow(row, impact)
        for (ob in row.obs) if (ob.type == ObType.SOLID) debris.smash(ob, row.z)
        row.obs.removeAll { it.type == ObType.SOLID }
    }

    private fun scoreRow(row: Row) {
        // Stairs/platforms and empty stretches are exempt; dodging a solid breaks Monk.
        if (zen && row.obs.any { it.type == ObType.SOLID }) zenStreak = 0
        rowsPassed++
        val x2 = powerUps.mult.active || powerUps.skateboard.active
        session.addScore(if (x2) 2 else 1)
        fx.rowPassed()
        if (bonus == Bonus.NONE && portalSafetyLeft <= 0f && !worldMission.completed) {
            val rowObjective = when (worldMission.kind) {
                WorldMission.Kind.WINTER_ROWS -> "WINTER ROUTE CLEARED"
                WorldMission.Kind.RAILWAY_ROWS -> "TRAIN YARD CLEARED"
                WorldMission.Kind.BEACH_ROWS -> "SHORELINE CLEARED"
                else -> null
            }
            if (rowObjective != null && worldMission.scoreRow()) completeWorldObjective(rowObjective)
            else if (rowObjective != null) publishObjective()
        }
        if (bonus == Bonus.NONE && portalSafetyLeft <= 0f && !worldMission.completed &&
            worldMission.kind == WorldMission.Kind.NIGHT_PREDATORS && row.obs.any { it.nightAnimal == RoadAnimal.WOLF || it.nightAnimal == RoadAnimal.TIGER }) {
            if (worldMission.evadePredator()) completeWorldObjective("PREDATORS OUTRUN") else publishObjective()
        }
        if (bonus == Bonus.NONE && portalSafetyLeft <= 0f && !worldMission.completed &&
            worldMission.kind == WorldMission.Kind.JUNGLE_DINOSAURS && row.obs.any { it.nightAnimal == RoadAnimal.DINOSAUR }) {
            if (worldMission.evadeDinosaur()) completeWorldObjective("DINOSAURS EVADED") else publishObjective()
        }
        if (bonus == Bonus.NONE && portalSafetyLeft <= 0f && !worldMission.completed &&
            worldMission.kind == WorldMission.Kind.AIR_BIRDS && row.obs.any { it.airBird != AirBird.NONE }) {
            if (worldMission.evadeAirBird()) completeWorldObjective("SKY ROUTE CLEARED") else publishObjective()
        }
        if (row.minClear < 0.34f) { // shaved it — reward a close dodge with an air-rush
            session.nearMiss()
            val bonus = nearMissBonus * (if (Skins.Ability.CLOSE_SHAVE in runSkin.abilities) 3 else 1)
            session.addScore(if (x2) bonus * 2 else bonus)
            fx.nearMiss(player.px, player.py)
        }
        if (!track.isPillTest) {
            worlds.onRow(rowsPassed)
            track.themeWorldId = worlds.spawnWorld.id
        }
    }

    private fun prepareCoalGems() {
        if (Skins.Ability.COAL !in runSkin.abilities) return
        val chance = if (session.score < 1000) 0f else (.01f + .99f * (session.score - 1000) / 2000f).coerceAtMost(1f)
        for (row in track.rows) for (coin in row.coins.orEmpty()) {
            if (coin.taken || coin.missed) continue
            if (coin.coalGemRoll < 0f) coin.coalGemRoll = coalRandom.nextFloat()
            if (coin.coalGemRoll < chance) coin.gem = true
        }
    }

    private fun prepareToxicCoins() {
        if (Skins.Ability.TOXIC_FORTUNE !in runSkin.abilities) return
        for (row in track.rows) for (coin in row.coins.orEmpty()) {
            if (!coin.toxicAssigned) {
                coin.toxic = toxicRandom.nextFloat() < .01f
                coin.toxicAssigned = true
            }
        }
    }

    private fun collectCoin(coin: Coin, cz: Float) {
        if (dead || coin.taken || coin.missed) return
        coin.taken = true
        if (Skins.Ability.COAL !in runSkin.abilities || coin.gem) session.coinPickedUp()
        if (coin.toxic && Skins.Ability.TOXIC_FORTUNE in runSkin.abilities) {
            die()
            return
        }
        worldMissionCoin(coin.kind)
        val value = Progress.coinValue * CoinType.BASE_EARNINGS_MULTIPLIER * coin.kind.reward *
            (if (bonus == Bonus.KALEIDO) 2f else 1f) *
            (if (bubble.active && Skins.Ability.GOLD_BUBBLE in runBubble.abilities) 1.6f else 1f)
        if (Skins.Ability.COAL in runSkin.abilities && !coin.gem) {
            session.coalCollected()
            SoundFx.play("tap", rate = .75f, vol = .35f)
            burst3d(phasePosition.set(coin.x, coin.y, cz), trackArt.coal, n = 6, speed = 2.5f, size = .12f, life = .35f)
            return
        }
        if (Skins.Ability.LOTTERY in runSkin.abilities) {
            awardJackpot(lottery.collectCoin(value))
            SoundFx.play("tap", rate = 1.25f, vol = .25f)
            burst3d(phasePosition.set(coin.x, coin.y, cz), Color.RED, n = 3, speed = 2f, size = .07f, life = .2f)
            return
        }
        val preciseValue = if (coin.gem && Skins.Ability.COAL in runSkin.abilities) 3.0 else kotlin.math.round(value.toDouble() * runSkin.coinMultiplier * 1_000_000.0) / 1_000_000.0
        val before = coinsRun
        coinsRunF = (coinsRunF + preciseValue).coerceAtMost(Int.MAX_VALUE.toDouble())
        coinsRun = (coinsRunF + .0000001).toInt()
        if (Skins.Ability.BUBBLE_DIVIDEND in runSkin.abilities) {
            val earned = coinsRun / 200 - before / 200
            if (earned > 0) {
                Progress.addBubble(earned)
                session.setBubbles(Progress.bubbles)
            }
        }
        coinStreak++
        session.setCoins(coinsRun)
        if (time - lastCoinT > 0.4f) coinPitch = 0 // the pitch climbs coin after coin and falls back as soon as the line breaks
        lastCoinT = time
        coinPitch++
        fx.coin(coin.x, coin.y, cz, coinPitch, if (coin.gem) trackArt.gem else trackArt.colorOfCoin(coin.kind))
        if (coinStreak == 20 || coinStreak == 50 || coinStreak % 100 == 0) fx.coinMilestone(trackArt.gold)
    }

    private fun collectPickup(row: Row, cz: Float) {
        val kind = row.pickup
        row.pickup = Pickup.NONE
        if (zen && (kind == Pickup.BOX || Pickup.shardType(kind) >= 0)) return
        if (kind != Pickup.NONE && kind != Pickup.BOX && Pickup.shardType(kind) < 0) {
            Progress.recordPowerup()
            session.powerupPickedUp()
        }
        when (kind) {
            Pickup.SHARD_EMBER, Pickup.SHARD_FROST, Pickup.SHARD_VOID -> {
                session.addShard(Pickup.shardType(kind))
                fx.coin(row.pickupX, .85f, cz, 1, trackArt.colorOf(kind))
            }
            Pickup.BUBBLE -> { // Ocean doubles the pickup; its coin milestone reward is separate. Zen stocks nothing.
                if (!zen) Progress.addBubble(if (Skins.Ability.BUBBLE_HAUL in runSkin.abilities) 2 else 1)
                session.setBubbles(Progress.bubbles)
                fx.pickup(hsvInto(tmpCol, 190f, 0.5f, 1f), row.pickupX, cz)
            }
            Pickup.BOX -> {
                lastCollectedBox = row
                session.boxCollected()
                if (Skins.Ability.LOTTERY in runSkin.abilities) {
                    val won = lottery.collectBox()
                    awardJackpot(won)
                    if (won == 0) {
                        SoundFx.play("tap", rate = .7f, vol = .5f)
                        burst3d(phasePosition.set(row.pickupX, .8f, cz), Color.RED, n = 10, speed = 3f, size = .1f, life = .4f)
                    }
                } else {
                    if (!zen) { boxesRun++; session.setBoxes(boxesRun) } // a Zen run never reaches the boxes page
                    fx.boxPickup(trackArt.colorOf(Pickup.BOX), trackArt.colorOf(Pickup.NONE), row.pickupX, cz)
                }
            }
            Pickup.MAGNET -> { powerUps.magnet.start(Progress.MAGNET.duration(Progress.magnetLevel) * runSkin.powerupDurationMultiplier); fx.pickup(trackArt.colorOf(kind), row.pickupX, cz) }
            Pickup.MULT -> { powerUps.mult.start(Progress.MULT.duration(Progress.multLevel) * runSkin.powerupDurationMultiplier); fx.pickup(trackArt.colorOf(kind), row.pickupX, cz) }
            Pickup.RED_PILL -> {
                redPill.collect(RedPill.DURATION * runSkin.powerupDurationMultiplier)
                fx.pickup(trackArt.colorOf(kind), row.pickupX, cz)
                rig.punch(.5f)
            }
            Pickup.JET -> {
                val dur = Progress.JET.duration(Progress.jetLevel) * runSkin.powerupDurationMultiplier
                powerUps.jet.start(dur)
                player.setFlying(true)
                track.airCoins = true
                aimJetCoins(dur, difficulty.speed() * runSkin.speedMultiplier * (1f + jetSpeedUp)) // where the flight will end, at boosted speed
                track.liftCoins()
                fx.pickup(trackArt.colorOf(kind), row.pickupX, cz)
                rig.punch(1f)
            }
            Pickup.SKATEBOARD -> {
                powerUps.skateboard.start(skateboardDuration * runSkin.powerupDurationMultiplier)
                fx.pickup(trackArt.colorOf(kind), row.pickupX, cz)
                SoundFx.play("slide", rate = 1.35f, vol = .55f)
                rig.punch(.7f)
            }
            Pickup.JUMP_SHOES -> {
                powerUps.jumpShoes.start(jumpShoesDuration * runSkin.powerupDurationMultiplier)
                player.jumpShoesEnabled = true
                fx.pickup(trackArt.colorOf(kind), row.pickupX, cz)
                rig.punch(.55f)
            }
            Pickup.PHASE -> {
                powerUps.phase.start(phaseDuration * runSkin.powerupDurationMultiplier)
                fx.pickup(trackArt.colorOf(kind), row.pickupX, cz)
                SoundFx.play("whoosh", rate = 1.15f, vol = .6f)
                flash(Color(0.58f, 0.40f, 1f, 1f), .28f)
            }
            Pickup.TIME_WARP -> {
                powerUps.timeWarp.start(timeWarpDuration * runSkin.powerupDurationMultiplier)
                fx.pickup(trackArt.colorOf(kind), row.pickupX, cz)
                SoundFx.play("rise", rate = .88f, vol = .58f)
                rig.punch(.45f)
            }
        }
    }

    /** The win is the run's at once; the HUD only shows it once the show has counted it in. */
    private fun awardJackpot(amount: Int) {
        if (amount <= 0) return
        val before = coinsRun
        coinsRunF = (coinsRunF + amount).coerceAtMost(Int.MAX_VALUE.toDouble())
        coinsRun = coinsRunF.toInt()
        val won = coinsRun - before
        if (won <= 0) return
        val first = !jackpot.active
        jackpot.start(won, player.px, player.py + Terrain.y(0f), cam, bgTop, bgBottom)
        if (first) {
            // The show's camera stands behind the cube: whatever the run has already passed would block it.
            // The hit's flash covers them going.
            scenery.clearPassedGates()
            for (r in track.rows) if (r.z > 0.9f) r.obs.removeAll { it.type == ObType.SOLID } // platforms may still carry the cube
            session.jackpotWon(won)
        }
    }

    /**
     * The run holds while the jackpot plays: nothing scrolls, no timer runs,
     * no input lands. Only the shockwave shatters the road ahead of it.
     */
    private fun tickJackpot(dt: Float) {
        jackpot.update(dt)
        val front = jackpot.waveZ
        for (r in track.rows) {
            if (r.z > front && r.z < 1.2f && r.obs.any { it.type == ObType.SOLID }) shatter(r, impact = r.z > -16f)
        }
        debris.update(dt, 0f)
        zenDissolve.update(dt, 0f)
        if (jackpot.takeBanked()) session.setCoins(coinsRun)
        if (!jackpot.active) { session.setCoins(coinsRun); return }
        jackpot.tintSky(bgTop, bgBottom)
        jackpot.aimCamera(cam)
    }

    private fun mintSynergy() = Skins.mintSynergy(runSkin.id, runBubble.id)
    private fun bubbleDurationMultiplier() = runSkin.powerupDurationMultiplier * runBubble.durationMultiplier *
        (if (mintSynergy()) 1.3f else 1f)
    private fun bubbleDuration() = Progress.BUBBLE.duration(Progress.bubbleLevel) * bubbleDurationMultiplier()
    private fun refreshJumpAbility() {
        val shielded = live() && bubble.active
        player.doubleJumpEnabled = shielded && (Skins.Ability.DOUBLE_JUMP in runSkin.abilities || Skins.Ability.DOUBLE_JUMP in runBubble.abilities)
        player.tripleJumpEnabled = shielded && mintSynergy()
        player.gigajumpEnabled = shielded && Skins.Ability.GIGAJUMP in runBubble.abilities
    }

    /** Tell the track where the flight lands, so the coin line it lays glides down to meet the ground there. */
    private fun aimJetCoins(left: Float, speed: Float) {
        track.jetEndZ = -(left * speed)
        track.jetGlideLen = jetGlide * speed
    }

    private fun endJet() {
        player.setFlying(false)
        track.airCoins = false
        track.dropCoins()
        jetGrace = 1.2f
        fx.jetEnd()
    }

    /** Try to spend a stocked bubble (double tap). Returns true when one went up. */
    private fun tryBubble(): Boolean {
        if (!live() || !bubble.ready) return false
        if (!zen && !Progress.useBubble(runSkin.bubbleSaveChance)) { fx.emptyStock(); return false } // Zen bubbles are free
        session.setBubbles(Progress.bubbles)
        bubble.activate(player.px, player.py)
        refreshJumpAbility()
        rig.punch(1f)
        return true
    }

    // ---------------------------------------------------------------- input

    /** Called on the GL thread, through the same guarded actions as touch input. */
    fun onPhysicalAction(action: cube.run.core.PhysicalAction) {
        idlePilot.stop()
        if (opening.active) { finishOpening(); return }
        if (!started && !Stage.homeScreen) return
        if (action == cube.run.core.PhysicalAction.CONFIRM) onTap(0f, 0f)
        else action.swipe?.let { onSwipe(it) }
    }

    override fun onDown(x: Float, y: Float) {
        idlePilot.stop()
        if (jackpot.active || gift.active || showcase.active || Stage.paused) return
        smoothAnchorX = x; smoothAnchorLane = player.lane; smoothVAccum = 0f
        smoothWall = 0
    }

    override fun onDrag(x: Float, y: Float, dx: Float, dy: Float) {
        if (!Settings.smoothControl || !live() || jackpot.active || Stage.paused || Stage.mode != Stage.NONE) return
        refreshJumpAbility()
        val laneTravel = sw * (0.32f - 0.20f * Settings.smoothSensitivity) // finger px per lane
        val rawTarget = smoothAnchorLane + ((x - smoothAnchorX) / laneTravel).roundToInt()
        val target = rawTarget.coerceIn(0, Lanes.last)
        val fromLane = player.lane
        if (player.moveToLane(target)) session.userLaneSwipe(fromLane, target)
        val wall = when { rawTarget < 0 -> -1; rawTarget > Lanes.last -> 1; else -> 0 }
        if (wall != 0 && wall != smoothWall) sideBounce(wall)
        smoothWall = wall
        val vStep = sw * (0.16f - 0.08f * Settings.smoothSensitivity)
        if (abs(dy) > abs(dx)) smoothVAccum += dy else smoothVAccum *= 0.6f
        if (smoothVAccum <= -vStep) { smoothVAccum = 0f; player.jump() }
        else if (smoothVAccum >= vStep) { smoothVAccum = 0f; player.downAction() }
    }

    override fun onTap(x: Float, y: Float) {
        if (jackpot.active || Stage.paused || Stage.mode != Stage.NONE || gift.active || showcase.active) return
        if (!started) { start(); return }
        if (!live()) return
        if (time - lastTapT < 0.38f) { // a quick double tap pops a bubble shield (anywhere, any time)
            lastTapT = -9f
            if (tryBubble()) return
        } else {
            lastTapT = time
        }
        if (player.air) { styleCombo++; fx.styleTap(styleCombo, player.px, player.py) } // flair only — never scored
    }

    override fun smoothSwipeEnabled(): Boolean = Settings.smoothControl

    override fun onSwipe(dir: Int) {
        if (session.isOver || dead || jackpot.active || Stage.paused || gift.active || showcase.active) return
        if (!started || Stage.mode != Stage.NONE) { return }
        refreshJumpAbility()
        when (dir) {
            LEFT, RIGHT -> {
                val d = if (dir == LEFT) -1 else 1
                if (player.lane + d in 0..Lanes.last) {
                    val fromLane = player.lane
                    if (player.moveToLane(fromLane + d)) session.userLaneSwipe(fromLane, fromLane + d)
                }
                else sideBounce(d)
            }
            UP -> player.jump()
            DOWN -> player.downAction()
        }
    }

    // ----------------------------------------------------------------- loop

    /**
     * Walkable height under x = [px] at the player's z: the top of any platform
     * whose segment is passing (rising linearly along its front ramp), else 0.
     */
    private fun groundAt(px: Float): Float {
        if (player.hover) return 0f
        var h = 0f
        groundObstacle = null
        for (row in track.rows) {
            if (row.z < 0f || row.z > 6.6f) continue
            for (ob in row.obs) {
                if (ob === phasedObstacle || ob.type != ObType.PLAT || abs(px - ob.x) > ob.halfW + 0.3f) continue
                val local = row.z // how far the front has passed the player
                if (local > ob.sz) continue
                val top = ob.clear
                val here = if (ob.ramp > 0f && local < ob.ramp) top * (local / ob.ramp) else top
                if (here > h) { h = here; groundObstacle = ob }
            }
        }
        return h
    }

    override fun tickOpening() {
        opening.tick(0f)
        opening.pose(player, cam, worldHue())
        alignOpeningTime(opening.motionSeconds)
    }

    private fun applyWorldPreviewIfNeeded() {
        if (started || Stage.mode != Stage.WORLD_PICKER) return
        val requested = Stage.worldPreview
        if (requested == -1 || requested == appliedWorldPreview) return
        when {
            requested == Stage.RANDOM_WORLD_PREVIEW -> worlds.reset()
            cube.run.data.Worlds.isCampaignWorld(requested) -> worlds.reset(requested)
            else -> return
        }
        appliedWorldPreview = requested
        player.setAutoMapSkin(worlds.world.id)
        SoundFx.setMusicWorld(worlds.world.id)
        scenery.init(worlds.world)
        scenery.spawnStartGate(hsvInto(tmpCol, worldHue() + 180f, 0.7f, 1f))
        bgTop.set(worlds.skyTop); bgBottom.set(worlds.skyBottom)
        session.setWorld(worlds.world.name)
    }

    override fun tick(dt: Float) {
        // Navigation can become available before the intro finishes. Its camera
        // must not be applied again when returning from a wardrobe/shop preview.
        if (Stage.mode != Stage.NONE) opening.finish()
        applyWorldPreviewIfNeeded()
        val wasOpening = opening.active
        opening.tick(dt)
        if (wasOpening) alignOpeningTime(opening.motionSeconds)
        onOpeningProgress?.invoke(opening.uiAmount)
        if (!opening.active) onOpeningProgress = null
        if (idlePilot.ready(dt, !started && Stage.homeScreen && Stage.mode == Stage.NONE && !session.isOver)) {
            start()
            idlePilot.start(time)
        }
        // ---- showcase stages take over the scene when the HUD asks
        if (Stage.mode == Stage.BOX) {
            if (!gift.active) gift.enter(bgTop, bgBottom)
        } else if (gift.active) gift.exit()
        if (gift.active) {
            gift.update(dt, time)
            gift.aimCamera(cam)
            return
        }
        if (Stage.mode == Stage.SKINS || Stage.mode == Stage.RESULT || Stage.mode == Stage.SHOP) {
            if (!showcase.active) {
                if (Stage.mode == Stage.SHOP) shopMenuIntroT = introT
                showcase.enter(bgTop, bgBottom, worldHue())
            }
            showcase.update(dt, time, worldHue())
            showcase.tintSky(bgTop, bgBottom)
            showcase.aim(rig)
            return
        } else if (showcase.active) {
            showcase.exit(bgTop, bgBottom)
            introT = if (showcase.shop) shopMenuIntroT else 1.8f
            if (!showcase.shop) {
                rig.reset(); rig.intro = 0f; rig.roll = 0f; rig.wide = 0f
            }
            // The shop's reverse transition already restored the menu sky. Replaying the older
            // showcase fade here jumps back to a dark sky and flashes brightly in Sunset Dunes.
            skyBlend = if (showcase.shop) 0f else 1f
        }

        if (!started && autoStart && time > 0.05f) {
            start()
            if (idleBotStart && Settings.devMode && Stage.interactions.get() == initialInteraction) idlePilot.start(time)
        }
        // Player/track still describe the start of this simulation slice.
        if (idlePilot.active && live()) idlePilot.drive(time - dt, spd, timeScale, track,
            player.pilotBody(powerUps.jet.left).apply { landingGrace = jetGrace },
            if (runT >= 1.5f) cube.run.bot.JetMotion(difficulty.speed() * runSkin.speedMultiplier,
                jetBoost, if (player.flying) powerUps.jet.left else 0f) else null, ::onSwipe)
        if (jackpot.active) { tickJackpot(dt); if (jackpot.active) return }
        if (Stage.endRun) { Stage.endRun = false; if (live()) crash() } // dev tool: END RUN from the pause card

        // the Calm filter settles in with the run and lifts again for the menus
        val calmTarget = if (started && !dead && Skins.Ability.CALM in runSkin.abilities) 1f else 0f
        calmWash += (calmTarget - calmWash) * min(1f, dt * 0.8f)
        if (started && !dead) {
            jetBoost += ((if (player.flying) 1f else 0f) - jetBoost) * min(1f, dt * 2f)
            runT += dt
            session.runSeconds(runT.toInt())
            val ease = min(1f, runT / 1.5f).let { it * it * it * (it * (it * 6f - 15f) + 10f) } // the start: the road winds up, the camera drops in
            rig.intro = introAtStart + (1f - introAtStart) * ease
            val boardBoost = if (powerUps.skateboard.active) 1f + skateboardSpeedBoost else 1f
            spd = 4.5f + (difficulty.speed() * runSkin.speedMultiplier * (1f + jetSpeedUp * jetBoost) * boardBoost - 4.5f) * ease
            if (fire.tick(dt, player.px, player.py) > 0) rig.punch(0.45f)
            difficulty.ramp(dt)
        } else if (!started) {
            spd = 0f // nothing moves before the run: the cube waits at the line
            player.idle(dt)
            introT = if (opening.active) 1.8f else introT + dt
            val k = min(1f, introT / 1.8f).let { it * it * it * (it * (it * 6f - 15f) + 10f) }
            rig.intro = -2.2f + 2.2f * k // the menu shot swoops in from high and far back and settles
        } else {
            spd = max(0f, spd - spd * 2.4f * dt) // death: world glides to a stop
            deathT = min(deathT + dt, 2.5f)
            if (!gameOverShown && deathT >= deathAnimTime) { gameOverShown = true; session.gameOver() }
        }
        // Time Warp slows the moving world, but never the timed singularity escape route.
        val mv = spd * dt * if (powerUps.timeWarp.active && !blackHole.active) timeWarpSpeed else 1f
        dist += mv
        if (started && !dead) session.distanceCovered(dist.toInt())
        Lanes.tick(dt)
        track.alignLaneSpacing()
        Terrain.scroll(mv, dt)
        // the Kaleidoscope: the sky and the road never hold a colour; the camera sways
        kaleido += ((if (bonus == Bonus.KALEIDO) 1f else 0f) - kaleido) * min(1f, dt * 1.5f)
        val glide = if (dt > 0f) (player.px - lastPx) / dt else 0f
        lastPx = player.px
        if (kaleido > 0.001f) {
            kaleidoHue += dt * 70f
            rig.roll = 7f * kaleido * sin(time * 1.1f)
        } else rig.roll += (space.bank(glide) - rig.roll) * min(1f, dt * 4f)
        rig.wide += ((if (Lanes.count > 3) 1f else 0f) - rig.wide) * min(1f, dt * 2f)
        if (started && !dead && !Settings.devMode) {
            val t = difficulty.tier()
            if (t > curTier) { curTier = t; fx.tierUp(t, worldHue()); rig.punch(0.5f) }
        }
        track.tier = curTier
        if (jetGrace > 0f) jetGrace = max(0f, jetGrace - dt)

        // ---- the world: sky cross-fade, roadside, the next gate
        worlds.tick(dt)
        bgTop.set(worlds.skyTop); bgBottom.set(worlds.skyBottom)
        if (skyBlend > 0f) {
            skyBlend = max(0f, skyBlend - dt * 2.2f)
            bgTop.lerp(showcase.stageTop, skyBlend); bgBottom.lerp(showcase.stageBottom, skyBlend)
        }
        if (kaleido > 0.001f) {
            bgTop.lerp(hsvInto(tmpCol, kaleidoHue, 0.85f, 1f), kaleido * 0.85f)
            bgBottom.lerp(hsvInto(tmpCol, kaleidoHue + 120f, 0.9f, 0.55f), kaleido * 0.85f)
        }
        space.tintSky(bgTop, bgBottom)
        when (scenery.scroll(mv)) {
            Scenery.PASSED_WORLD -> worlds.gatePassed()?.let {
                track.themeWorldId = worlds.spawnWorld.id
                player.setAutoMapSkin(it.id)
                beginWorldObjective(it.id)
                fx.worldGate(worlds.gateColor()); rig.punch(0.7f); session.setWorld(it.name)
                SoundFx.setMusicWorld(it.id)
                if (it.id == cube.run.data.Worlds.SPACE_ID) beginSpaceWorldStage()
            }
            Scenery.PASSED_START -> {
                startGateRunT = runT
                fx.startGate(player.trailCol())
                rig.punch(0.9f)
                if (Progress.safeStartSeconds > 0f) { // the Safe start perk: a bubble is already up
                    bubble.duration = Progress.safeStartSeconds * bubbleDurationMultiplier()
                    bubble.activate(player.px, player.py, quiet = true)
                    bubble.duration = bubbleDuration()
                }
            }
        }
        if (live()) {
            track.speed = difficulty.speed() * runSkin.speedMultiplier *
                (if (powerUps.skateboard.active) 1f + skateboardSpeedBoost else 1f)
            track.spawn(mv, worldHue(), session.score, dt)
            prepareToxicCoins()
            prepareCoalGems()
        }

        val pillWasOn = redPill.timer.active
        redPill.tick(dt, started && !dead)
        // A crash stops the pill before it runs out, so only a survived trip ends on its own.
        if (pillWasOn && !redPill.timer.active && started && !dead) session.redPillSurvived()
        bgTop.lerp(Color.BLACK, redPill.blend)
        bgBottom.lerp(Color.BLACK, redPill.blend)
        if (opening.active) {
            bgTop.lerp(CubeOpening.INK, 1f-opening.worldAmount)
            bgBottom.lerp(CubeOpening.INK, 1f-opening.worldAmount)
        }

        // ---- timed power-ups
        if (started && !dead) {
            powerUps.magnet.tick(dt)
            powerUps.mult.tick(dt)
            powerUps.skateboard.tick(dt)
            if (powerUps.jumpShoes.tick(dt)) player.jumpShoesEnabled = false
            powerUps.phase.tick(dt)
            powerUps.timeWarp.tick(dt)
            if (bubble.active && powerUps.magnet.active && powerUps.mult.active && powerUps.jet.active) session.fullKitHeld()
            if (powerUps.jet.tick(dt)) endJet()
            else if (player.flying) { // keep the landing point current; glide down through the last seconds
                val left = powerUps.jet.left
                aimJetCoins(left, spd)
                val landingY = if (bonus == Bonus.FLOAT) Player.HOVER_Y else player.ground
                player.flyY = if (left < jetGlide) landingY + (Player.FLY_Y - landingY) * (left / jetGlide) else Player.FLY_Y
            }
        }

        if (!dead) {
            refreshJumpAbility()
            player.hover = bonus == Bonus.FLOAT
            player.lowGravity = bonus == Bonus.SPACE
            val gh = if (player.flying) 0f else groundAt(player.px)
            when (val event = player.update(dt, mv, time, worldHue(), trail = started, groundH = gh, stream = spd * 0.55f)) {
                Player.EV_LANDED, Player.EV_GROUND_POUND -> {
                    if (event == Player.EV_GROUND_POUND && live()) session.groundPounded()
                    if (styleCombo > 0) { fx.styleLand(styleCombo, player.px, player.py); styleCombo = 0 }
                }
                Player.EV_SIDE_HIT -> if (started && !powerUps.phase.active && portalSafetyLeft <= 0f) sideHit(gh)
            }
        }
        bubble.update(dt, time, player.px + player.nudge, player.py + Terrain.y(0f))
        refreshJumpAbility()
        val cooldown = kotlin.math.ceil(bubble.cooldownLeft).toInt()
        if (cooldown != shownBubbleCooldown) { shownBubbleCooldown = cooldown; session.setBubbleCooldown(cooldown) }

        // ---- obstacle rows: move, collide, score; coins: magnet + collect; pickups
        track.scroll(mv, time, dt)
        if (started && !dead && blackHole.active) {
            // Even while invulnerable, advance the route distance so its exit row stays synchronized;
            // pause only the deadline and gravity field during the safe-arrival interval.
            handleBlackHoleOutcome(blackHole.update(dt, mv, track, player, time, safeArrival = portalSafetyLeft > 0f))
        }
        tickObjective(dt)
        space.tick(dt, mv, time, alive = !dead)
        worlds.hold = space.inside || bonus == Bonus.NEXUS
        fx.stardust = bonus == Bonus.SPACE
        debris.update(dt, mv)
        zenDissolve.update(dt, mv)
        updateHound(dt)
        collide(dt)
        houndGrace = max(0f, houndGrace - dt)

        rig.chase(dt, player.px, player.py, player.ground, deathT, spd)
        if (opening.active) opening.pose(player, cam, worldHue())
    }

    /** One pass over the rows: obstacles (box overlap), pads, pickups, coins, scoring. */
    private fun collide(dt: Float) {
        val cubeBottom = player.cubeBottom
        val headY = player.headY
        val px = player.px; val py = player.py
        val immune = player.flying || jetGrace > 0f || houndGrace > 0f || powerUps.phase.active || portalSafetyLeft > 0f
        val pull = if (powerUps.magnet.active) 7.5f else 0f // only a MAGNET pulls; otherwise you have to touch them
        collisionActionDeferral.beginPass()
        try {
            for (row in track.rows) {
                if (row.portal >= 0 && started && !dead && row.z > 0f) crossPortal(row)
                if (started && !dead && abs(row.z) < 0.95f) {
                    for (ob in row.obs) {
                        if (ob.type == ObType.PAD) { // a springboard: harmless, launches you when you run onto it
                            if (!ob.used && !player.air && !player.flying && abs(row.z) < 0.75f && abs(px - ob.x) < 0.85f) {
                                ob.used = true
                                player.launch(12.5f)
                                fx.bounce(px, py, ob.col)
                                rig.punch(0.7f)
                            }
                            continue
                        }
                        if (immune || portalSafetyLeft > 0f || ob === phasedObstacle || ob.type != ObType.SOLID) continue
                        val lat = ob.lateral(px)
                        if (lat >= 0f) { row.minClear = min(row.minClear, lat); continue } // beside it: the lateral gap is the clearance
                        val vert = max(cubeBottom - ob.top, ob.bottom - headY)              // over / under it: the vertical gap (positive = clear)
                        row.minClear = min(row.minClear, vert)
                        if (abs(row.z) < 0.82f && vert < -0.02f) {
                            if (idlePilot.active) pilotContacts++
                            if (powerUps.skateboard.active && !player.air && ob.grounded && ob.sy <= .72f) {
                                if (!ob.used) {
                                    ob.used = true
                                    ob.sy = 0f; ob.cy = 0f
                                    fx.grind(player.px, player.py, trackArt.colorOf(Pickup.SKATEBOARD))
                                }
                                continue // a grounded board can grind small floor hazards; tall walls still demand a dodge
                            }
                            if (!bubble.active && phase(ob)) continue
                            hit(row); break
                        }
                    }
                }
                if (started && !dead && row.pickup != Pickup.NONE) { // run into it
                    val cz = row.z + Row.PICKUP_DZ
                    if (!row.pickupMissed && abs(cz) < 0.9f && abs(px - row.pickupX) < 0.95f && py < 1.7f) collectPickup(row, cz)
                    else if (!row.pickupMissed && cz > 1.1f) {
                        row.pickupMissed = true
                        if (row.pickup == Pickup.BOX && !row.idle) session.mysteryBoxMissed()
                    }
                }
                row.coins?.let { coins ->
                    if (row.z > -12f && started && !dead) {
                        for (c in coins) {
                            if (dead) break
                            if (c.taken) continue
                            val cz = row.z + c.dz
                            if (c.missed) continue
                            if (!c.toxic && ((pull > 0f && abs(cz) < pull) || c.pullStarted)) { // magnet: coins fly to you
                                c.pullStarted = true
                                val k = min(1f, dt * 11f)
                                c.x += (px - c.x) * k
                                c.y += (py - c.y) * k
                                c.dz += (-cz / 2) * k
                            }
                            if (abs(cz) < 0.8f && abs(px - c.x) < 0.85f && abs(py - c.y) < 0.85f) {
                                collectCoin(c, cz)
                            } else if (cz > 1.1f) { // it's behind you — the streak breaks; the coin keeps sliding past the camera
                                c.missed = true
                                coinStreak = 0
                            }
                        }
                    }
                }
                if (!row.scored && row.z > 1.2f) {
                    row.scored = true
                    if (started && !dead && !row.idle && !row.scoreless) scoreRow(row)
                }
            }
        } finally {
            collisionActionDeferral.endPass()
        }
    }

    /** Through a portal: the world changes shape (or back). */
    private fun crossPortal(row: Row) {
        // Give every portal a clean 1.5-second buffer, regardless of destination.
        // Track clears hazards ahead and streams blank rows while this timer runs.
        beginSafePortalArrival()
        val oldCount = Lanes.count
        // Track commits bonus=NONE when it spawns an exit row, before that row reaches the player.
        // Use the live SpaceWorld as well as the track flag so an ordinary Space exit fades home.
        val wasSpace = SpacePortalTransition.isReturningFromSpace(space.inside, bonus)
        val wasNexus = bonus == Bonus.NEXUS
        val id = track.crossPortal(row)
        bonus = id
        if (id == Bonus.SPACE && !spaceMission.active) spaceMission.enterSpace()
        if (wasSpace && id != Bonus.SPACE && worlds.world.id != cube.run.data.Worlds.SPACE_ID) spaceMission.clear()
        if (id == Bonus.NEXUS && !nexusMission.active) nexusMission.enterWorld(cube.run.data.Worlds.NEXUS_ID)
        if (wasNexus && id != Bonus.NEXUS) nexusMission.clear()
        worlds.hold = space.inside || id == Bonus.NEXUS
        scenery.setNexusBonus(id == Bonus.NEXUS)
        player.remapLane(oldCount, Lanes.count)
        if (id == Bonus.NONE) player.hover = false
        Terrain.set(id == Bonus.HILLS)
        session.setBonus(id)
        if (id == Bonus.SPACE || wasSpace) { // into space and out: a warp, not a bang
            if (id == Bonus.SPACE) {
                space.enter(rnd.nextInt())
                worlds.hold = true
                beginBlackHoleChallenge()
            } else space.exit()
            fx.warp(if (id == Bonus.SPACE) space.neon else hsvInto(tmpCol, 200f, 0.5f, 1f))
            rig.punch(0.6f)
            return
        }
        val col = hsvInto(tmpCol, if (id == Bonus.NONE) 200f else Bonus.get(id).hue, 0.5f, 1f)
        fx.worldGate(col)
        flash(Color.WHITE, 0.7f)
        slowMo(0.35f, 0.4f)
        rig.punch(1f)
    }

    // ------------------------------------------------------------- rendering

    override fun renderHud(shapes: ShapeRenderer, w: Float, h: Float) {
        if (gift.active || showcase.active || dead || Stage.paused || jackpot.active) return
        powerUps.drawBars(
            shapes, w, h, time, if (bubble.active) bubble.timer else null, redPill.timer,
            powerUps.skateboard, powerUps.jumpShoes, powerUps.phase, powerUps.timeWarp,
        )
    }

    override fun renderWorldBatched() {
        setMatrixAmount(if (gift.active || showcase.active) 0f else redPill.blend)
        if (gift.active) { fogColor.set(bgBottom); syncFog(); gift.render(time); return }
        if (showcase.active) {
            if (showcase.shop && showcase.menuVisibility > 0f) {
                setWorldOpacity(showcase.menuVisibility)
                renderTrackScene(backdrop = false)
                setWorldOpacity(1f)
            }
            fogColor.set(bgBottom); syncFog(); showcase.render(time)
            return
        }
        setWorldOpacity(opening.worldAmount)
        if (opening.worldAmount > .001f) renderTrackScene()
        setWorldOpacity(1f)
    }

    /** The road scene. [backdrop] false leaves out Space's opaque sky (the shop fades the road in and out). */
    private fun renderTrackScene(backdrop: Boolean = true) {
        // haze target ≈ the sky gradient at the horizon, so the far track end and
        // freshly spawned rows dissolve into the background instead of popping in
        fogColor.set(bgBottom).lerp(bgTop, space.fogMix(worlds.fogMix))
        syncFog()
        space.rifts.update(track.rows)
        trackArt.glassRifts = space.blend > 0f
        // A Space bonus is its own biome even when reached from a Jungle portal; keep its hazard art Space-themed.
        trackArt.worldStyle = when (bonus) {
            Bonus.SPACE -> cube.run.data.Worlds.RINGS
            Bonus.NEXUS -> cube.run.data.Worlds.NEXUS
            else -> worlds.spawnWorld.deco
        }
        trackArt.stripedCityHazards = worlds.spawnWorld.id == cube.run.data.Worlds.NEON_CITY_ID
        scenery.renderRoad()
        trackArt.render(track, time, kaleido, kaleidoHue)
        tesseract.render(time, space.blend)
        blackHole.render(time)
        if (!dead && !showcase.active && powerUps.skateboard.active)
            trackArt.renderSkateboard(player.px + player.nudge, player.cubeBottom - .08f, .32f, time)
        else if (!dead && !showcase.active && powerUps.jumpShoes.active)
            trackArt.renderJumpShoes(player.px + player.nudge, player.cubeBottom - .04f, .32f, time)
        if (!dead && !showcase.active) hound.renderBatched(time)
        if (space.blend > 0f) space.rifts.render()
        debris.render()
        zenDissolve.render()
        val wind = if (dead) 0f else ((spd - 13f) / 15f).coerceIn(0f, 1f)
        scenery.render(if (player.flying) 1f else wind, time)
        if (backdrop) space.sky.render(time, if (dead) 0f else ((spd - 8f) / 20f).coerceIn(0f, 1f))
        if (jackpot.active) jackpot.render()
    }

    override fun renderWorldBackdrop(shapes: ShapeRenderer) {
        if (showcase.active && showcase.shop) showcase.renderShapes(shapes, time)
        else if (!showcase.active && !gift.active) {
            bendShapes(shapes, on = false) // the painted sky never bends
            val backdropWorld = when (bonus) {
                Bonus.SPACE -> cube.run.data.Worlds.GALAXY_ID
                Bonus.NEXUS -> cube.run.data.Worlds.NEXUS_ID
                else -> worlds.world.id
            }
            skyBackdrop.render(shapes, cam, backdropWorld, time, 1f - space.blend)
            space.deco.render(shapes, cam, time)
            bendShapes(shapes, on = true)
        }
    }

    override fun renderWorldShapes(shapes: ShapeRenderer) {
        if (gift.active) gift.renderShapes(shapes, time)
        else if (showcase.active && !showcase.shop) showcase.renderShapes(shapes, time)
        else if (!showcase.active) {
            bendShapes(shapes, on = false)
            space.sky.renderShapes(shapes, time)
            bendShapes(shapes, on = true)
            trackArt.renderCues(shapes, track, opening.worldAmount, redPill.blend)
            if (jackpot.active) jackpot.renderShapes(shapes, time)
        }
    }

    override fun renderWorld(batch: ModelBatch, env: Environment) {
        if (gift.active) return
        if (showcase.active) { player.render(batch, env); return }
        if (jackpot.active) {
            player.jackpotPose(time, worldHue(), player.px, jackpot.cubeY, jackpot.cubeYaw, jackpot.cubeTip, jackpot.cubeScale, jackpot.cubeGold, trackArt.gold)
            player.render(batch, env)
            return
        }
        if (!dead) {
            hound.renderEye(batch, env)
            player.render(batch, env, Terrain.y(0f))
        }
    }

    override fun renderBlended() {
        if (gift.active) { gift.renderBlended(cam, time); return }
        if (showcase.active) { showcase.renderBlended(cam, time); return }
        if (!dead && !jackpot.active) bubble.render(cam, time)
        for (r in track.rows) { // floating bubble pickups: little soap bubbles
            if (r.pickup != Pickup.BUBBLE) continue
            val cz = r.z + Row.PICKUP_DZ
            if (cz < -Fog.end) continue
            val s = 1.25f + 0.06f * sin(time * 5f + r.visualPhase)
            bubbles.draw(cam, r.pickupX, 0.95f + 0.12f * sin(time * 3f + r.visualPhase), cz, s, s, s, time * 50f, time, 170f, 270f, 0.6f, 2.4f, 0.12f, 1f - Fog.at(cz), BubbleSkins.IRIS)
        }
    }
    override fun pause() { idlePilot.stop(); space.pauseAudio(); super.pause() }
    override fun dispose() { Progress.zenRun = false; idlePilot.close(); space.deco.dispose(); showcase.dispose(); super.dispose() }

}
