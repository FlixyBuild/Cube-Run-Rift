package cube.run.game.world

import com.badlogic.gdx.graphics.Color
import cube.run.core.Gdx3DGame
import cube.run.core.hsvInto
import cube.run.data.Settings
import cube.run.data.Worlds
import cube.run.game.Lanes
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Distance haze: crisp until [start], fully dissolved into the sky by [end]. Rows spawn beyond [end]. */
object Fog {
    const val start = 58f
    const val end = 96f

    /** Fog factor for world geometry at [z] (negative = ahead of the player). */
    fun at(z: Float) = ((-z - start) / (end - start)).coerceIn(0f, 1f)
}

/**
 * The scrolling backdrop: a seamless two-tone floor, roadside decoration in
 * the current world's style, wind streaks that fly past faster than the
 * world at speed, and the occasional world gate. All plain data drawn
 * through the batched world-box pass. Pieces are recoloured/restyled for
 * the *current* world when they wrap, so a new world grows in from the
 * horizon behind its gate instead of snapping. Every decoration is built
 * from boxes that touch or stack — never intersect — so nothing shimmers.
 */
class Scenery(private val game: Gdx3DGame, private val rnd: Random) {

    private class Tile(@JvmField val col: Color, @JvmField val col2: Color, @JvmField val ground: Color, @JvmField var z: Float, @JvmField val parity: Int)
    private class Post(@JvmField val col: Color, @JvmField val col2: Color, @JvmField val col3: Color, @JvmField var z: Float, @JvmField var x: Float, @JvmField var style: Int, @JvmField var h: Float, @JvmField var seed: Float, @JvmField var kind: Int)
    private class Streak(@JvmField var x: Float, @JvmField var y: Float, @JvmField var z: Float, @JvmField var len: Float)
    private class Gate(@JvmField var z: Float, @JvmField val col: Color, @JvmField var passed: Boolean = false, @JvmField var held: Boolean = false, @JvmField val start: Boolean = false)

    private val tileD = 3f
    private val tileRows = 40       // floor reaches past the fog wall (8 - 39*3 = -109)
    private val postGap = 6.6f
    private val postPairs = 17      // last pair ≈ -100, born inside the fog
    private val streakCount = 22
    private val snowflakeCount = 56

    private val tiles = ArrayList<Tile>()
    private val posts = ArrayList<Post>()
    private val streaks = ArrayList<Streak>()
    private val snowflakes = ArrayList<Streak>()
    private val gates = ArrayList<Gate>()
    private val streakCol = Color(1f, 1f, 1f, 1f)
    private val white = Color(1f, 1f, 1f, 1f)
    private val edgeCol = Color()
    private val dark = Color(0.10f, 0.08f, 0.18f, 1f)
    private val cityRoadA = Color(.032f, .045f, .067f, 1f)
    private val cityRoadB = Color(.048f, .063f, .088f, 1f)
    private val cityGround = Color(.018f, .026f, .043f, 1f)
    private val cityCyan = Color(.10f, .86f, 1f, 1f)
    private val railwayRoadA = Color(.045f, .055f, .066f, 1f)
    private val railwayRoadB = Color(.060f, .071f, .083f, 1f)
    private val railSteel = Color(.30f, .39f, .48f, 1f)
    private val railwayTie = Color(.15f, .12f, .12f, 1f)
    private val jungleRoadA = Color(.075f, .14f, .075f, 1f)
    private val jungleRoadB = Color(.095f, .18f, .085f, 1f)
    private val highwayRoadA = Color(.028f, .037f, .050f, 1f)
    private val highwayRoadB = Color(.043f, .052f, .067f, 1f)
    private val highwayWhite = Color(.80f, .87f, .90f, 1f)
    private val highwayGold = Color(1f, .68f, .11f, 1f)
    private val winterRoadA = Color(.12f, .18f, .27f, 1f)
    private val winterRoadB = Color(.18f, .26f, .36f, 1f)
    private val winterGround = Color(.78f, .88f, .96f, 1f)
    private val winterSnow = Color(.88f, .96f, 1f, 1f)
    private val winterIce = Color(.32f, .76f, 1f, 1f)
    private val winterPine = Color(.045f, .24f, .22f, 1f)
    private val winterBark = Color(.19f, .14f, .12f, 1f)
    private val beachRoadA = Color(.68f, .48f, .27f, 1f)
    private val beachRoadB = Color(.77f, .57f, .33f, 1f)
    private val beachSand = Color(.88f, .72f, .46f, 1f)
    private val beachWaterA = Color(.035f, .52f, .73f, 1f)
    private val beachWaterB = Color(.035f, .70f, .82f, 1f)
    private val beachFoam = Color(.72f, .96f, 1f, 1f)
    private val beachPalm = Color(.32f, .19f, .08f, 1f)
    private val beachLeaf = Color(.035f, .43f, .20f, 1f)
    private val beachLeafBright = Color(.11f, .72f, .27f, 1f)
    private val beachUmbrella = Color(1f, .23f, .11f, 1f)
    private val beachUmbrellaAlt = Color(1f, .78f, .16f, 1f)
    private val beachCabana = Color(.96f, .91f, .76f, 1f)
    private val nightRoadA = Color(.012f, .016f, .033f, 1f)
    private val nightRoadB = Color(.020f, .025f, .048f, 1f)
    private val nightGround = Color(.006f, .008f, .018f, 1f)
    private val nightReflector = Color(.78f, .86f, .98f, 1f)
    private val nightLamp = Color(1f, .76f, .34f, 1f)
    private val nightLampCore = Color(1f, .94f, .73f, 1f)
    private val hackerRoadA = Color(.006f, .025f, .017f, 1f)
    private val hackerRoadB = Color(.010f, .040f, .025f, 1f)
    private val hackerGround = Color(.004f, .014f, .010f, 1f)
    private val hackerGreen = Color(.08f, 1f, .40f, 1f)
    private val hackerMint = Color(.52f, 1f, .74f, 1f)
    private val hackerMagenta = Color(.95f, .12f, .76f, 1f)
    private val hackerPanel = Color(.02f, .13f, .075f, 1f)
    private val airRoadA = Color(.18f, .35f, .51f, 1f)
    private val airRoadB = Color(.24f, .45f, .62f, 1f)
    private val airEdge = Color(.78f, .96f, 1f, 1f)
    private val airCloud = Color(.95f, .99f, 1f, 1f)
    private val airCloudShade = Color(.57f, .78f, .90f, 1f)
    private val airBalloon = Color(1f, .52f, .18f, 1f)
    private val airSun = Color(1f, .72f, .25f, 1f)
    private val airSunCore = Color(1f, .92f, .57f, 1f)
    private val airMoon = Color(.82f, .94f, 1f, 1f)
    private val airMoonShade = Color(.44f, .68f, .82f, 1f)
    private val airRotor = Color(.12f, .29f, .40f, 1f)
    private val lavaRoadA = Color(.10f, .035f, .04f, 1f)
    private val lavaRoadB = Color(.17f, .055f, .045f, 1f)
    private val lavaGround = Color(.075f, .035f, .035f, 1f)
    private val lavaFlow = Color(.90f, .16f, .025f, 1f)
    private val lavaCore = Color(1f, .50f, .055f, 1f)
    private val nexusRoadA = Color(.008f, .020f, .030f, 1f)
    private val nexusRoadB = Color(.014f, .035f, .052f, 1f)
    private val nexusGround = Color(.004f, .012f, .024f, 1f)
    private val nexusCyan = Color(.08f, .90f, 1f, 1f)
    private val nexusMint = Color(.26f, 1f, .66f, 1f)
    private val nexusViolet = Color(.80f, .16f, .95f, 1f)
    private val planeBody = Color(.90f, .95f, .98f, 1f)
    private val planeWing = Color(.09f, .46f, .78f, 1f)
    private val planeGlass = Color(.035f, .20f, .32f, 1f)
    private val planeAccent = Color(1f, .48f, .07f, 1f)
    private val signalRed = Color(1f, .055f, .025f, 1f)
    private val signalAmber = Color(1f, .52f, .035f, 1f)
    private val signalGreen = Color(.12f, 1f, .34f, 1f)
    private val workerVest = Color(1f, .31f, .045f, 1f)
    private val workerTrousers = Color(.035f, .075f, .12f, 1f)
    private val workerFace = Color(.91f, .62f, .39f, 1f)
    private val workerHelmet = Color(1f, .78f, .08f, 1f)
    private val bark = Color(.24f, .115f, .045f, 1f)
    private val leafA = Color(.14f, .53f, .13f, 1f)
    private val leafB = Color(.32f, .76f, .20f, 1f)
    private var world: Worlds.World = Worlds.get(0)
    private var nexusBonus = false
    /** Galaxy (null outside a run's reach): the land falls away under it, the road takes its colours. */
    var space: cube.run.game.space.SpaceWorld? = null
    private val tileA = Color(); private val tileB = Color()
    private var dropY = 0f          // how far the land (and everything on it) has fallen away

    private val laneW: Float get() = Lanes.NORMAL_W

    private fun paintTile(t: Tile) {
        // two candy tones a few degrees apart: a soft check, no dark seams
        hsvInto(t.col, world.floorH, world.floorS, world.floorAltV)
        hsvInto(t.col2, world.floorH + 14f, world.floorS, world.floorV)
        // the land the road runs through: the same family, quieter, striped with the tiles
        hsvInto(t.ground, world.floorH - 22f, world.floorS * 0.6f, world.floorAltV * (if (t.parity == 0) 0.8f else 0.74f))
    }

    private fun seedPost(p: Post) {
        val deco = if (nexusBonus) Worlds.NEXUS else world.deco
        p.style = deco
        p.seed = rnd.nextFloat()
        p.kind = rnd.nextInt(3)
        p.h = when (deco) {
            Worlds.TOWERS -> 2.5f + rnd.nextFloat() * 4f
            Worlds.CRYSTALS -> 1.8f + rnd.nextFloat() * 2.4f
            Worlds.SPIKES -> 1.6f + rnd.nextFloat() * 1.2f
            Worlds.CACTI -> 1.8f + rnd.nextFloat() * 1f
            Worlds.RINGS -> 2f + rnd.nextFloat() * 2.5f
            Worlds.RAILWAY -> 2.6f + rnd.nextFloat() * 2.3f
            Worlds.JUNGLE -> 2.4f + rnd.nextFloat() * 3.2f
            Worlds.HIGHWAY -> 3f + rnd.nextFloat() * 3f
            Worlds.BEACH -> 2.2f + rnd.nextFloat() * 2.4f
            Worlds.NIGHT -> 4.2f + rnd.nextFloat() * 1.2f
            Worlds.HACKER -> 2.6f + rnd.nextFloat() * 2.4f
            Worlds.AIR -> 2.8f + rnd.nextFloat() * 1.8f
            Worlds.NEXUS -> 3f + rnd.nextFloat() * 3f
            else -> 0.9f + rnd.nextFloat() * 0.8f
        }
        val postH = if (nexusBonus) 184f else world.postH
        val postS = if (nexusBonus) .96f else world.postS
        val postV = if (nexusBonus) .98f else world.postV
        hsvInto(p.col, postH, postS, postV)
        hsvInto(p.col2, postH + 50f + p.seed * 60f, postS, postV)
        hsvInto(p.col3, postH + 130f + p.seed * 40f, postS * 0.9f, postV)
        val side = if (p.x < 0f) -1f else 1f
        val wideSetback = deco == Worlds.TOWERS || deco == Worlds.RINGS || deco == Worlds.HIGHWAY || deco == Worlds.NEXUS
        p.x = side * (laneW * 2.5f + 1.1f + (if (wideSetback) rnd.nextFloat() * 2.5f else rnd.nextFloat() * 0.6f))
    }

    private fun seedStreak(s: Streak) {
        val side = if (rnd.nextBoolean()) -1f else 1f
        s.x = side * (laneW * 1.5f + 2.2f + rnd.nextFloat() * 4f)
        s.y = 0.4f + rnd.nextFloat() * 4.5f
        s.len = 3f + rnd.nextFloat() * 5f
    }

    private fun seedSnowflake(s: Streak, index: Int) {
        // Most flakes pass directly over the three playable lanes; a few soften the shoulders.
        s.x = if (index % 4 == 0) (rnd.nextFloat() - .5f) * 8.5f else (rnd.nextFloat() - .5f) * 6.2f
        s.y = 0f
        s.z = -3f - rnd.nextFloat() * 60f
        s.len = .045f + rnd.nextFloat() * .055f
    }

    fun init(world: Worlds.World) {
        tiles.clear(); posts.clear(); streaks.clear(); gates.clear()
        this.world = world
        nexusBonus = false
        for (r in 0 until tileRows) {
            val t = Tile(Color(), Color(), Color(), 8f - r * tileD, r % 2)
            paintTile(t)
            tiles.add(t)
        }
        var pz = 6f
        repeat(postPairs) {
            for (side in intArrayOf(-1, 1)) {
                val p = Post(Color(), Color(), Color(), pz, side.toFloat(), 0, 1f, 0f, 0)
                seedPost(p)
                posts.add(p)
            }
            pz -= postGap
        }
        repeat(streakCount) { streaks.add(Streak(0f, 0f, -rnd.nextFloat() * 100f, 0f).also { seedStreak(it) }) }
        snowflakes.clear()
        repeat(snowflakeCount) { index -> snowflakes.add(Streak(0f, 0f, 0f, 0f).also { seedSnowflake(it, index) }) }
    }

    /** Pieces born from now on take this world's colours and style. */
    fun setWorld(world: Worlds.World) { this.world = world }

    /** Portal-only Nexus styling is separate from the selectable campaign world. */
    fun setNexusBonus(active: Boolean) {
        if (nexusBonus == active) return
        nexusBonus = active
        for (post in posts) seedPost(post)
    }

    /** Drop a gate at the horizon in [col]: the doorway to the next world. */
    fun spawnGate(col: Color) { gates.add(Gate(-102f, Color(col))) }

    /** The start gate: waits a little way down the road until the run begins ([release]), then comes at you. */
    fun spawnStartGate(col: Color) { gates.add(Gate(-30f, Color(col), held = true, start = true)) }

    /** The run began: everything held still starts moving with the world. */
    fun release() { for (g in gates) g.held = false }

    /** Take down gates already behind the player: a camera swung round behind the cube would look through them. */
    fun clearPassedGates() { gates.removeAll { it.passed } }

    companion object {
        const val PASSED_WORLD = 1
        const val PASSED_START = 2
        /** How see-through the road gets in space (0 solid … 1 gone). */
        const val GLASS = 0.55f
    }

    /** Scroll by [mv]. Returns PASSED_WORLD / PASSED_START on the frame a gate passes the player, else 0. */
    fun scroll(mv: Float): Int {
        for (t in tiles) {
            t.z += mv
            if (t.z > 8f) { t.z -= tileRows * tileD; paintTile(t) }
        }
        for (p in posts) {
            p.z += mv
            if (p.z > 8f) { p.z -= postPairs * postGap; seedPost(p) }
        }
        for (s in streaks) { // wind streaks fly past faster than the world (parallax sells the speed)
            s.z += mv * 1.6f
            if (s.z > 10f) { s.z = -90f - rnd.nextFloat() * 30f; seedStreak(s) }
        }
        for (flake in snowflakes) {
            flake.z += mv * .72f
            if (flake.z > 8f) flake.z -= 72f
        }
        var passed = 0
        var i = gates.size - 1
        while (i >= 0) {
            val g = gates[i]
            if (!g.held) g.z += mv
            if (!g.passed && g.z > 0f) { g.passed = true; passed = if (g.start) PASSED_START else PASSED_WORLD }
            if (g.z > 14f) gates.removeAt(i)
            i--
        }
        return passed
    }

    /**
     * The road and the land, queued FIRST so the batch never drops them:
     * seamless tiles, a bright kerb along each edge, and the ground running
     * out to the haze on both sides (what the roadside stands on).
     */
    fun renderRoad() {
        val visualWorldId = if (nexusBonus) Worlds.NEXUS_ID else world.id
        when (visualWorldId) {
            Worlds.NEON_CITY_ID -> edgeCol.set(cityCyan)
            Worlds.RAILWAY_ID -> edgeCol.set(railSteel)
            Worlds.HIGHWAY_ID -> edgeCol.set(highwayGold)
            Worlds.WINTER_ID -> edgeCol.set(winterIce)
            Worlds.LAVA_ID -> edgeCol.set(lavaCore)
            Worlds.NEXUS_ID -> edgeCol.set(nexusCyan)
            Worlds.BEACH_ID -> edgeCol.set(beachFoam)
            Worlds.NIGHT_ID -> edgeCol.set(nightReflector)
            Worlds.HACKER_ID -> edgeCol.set(hackerGreen)
            Worlds.AIR_ID -> edgeCol.set(airEdge)
            else -> hsvInto(edgeCol, world.floorH + 30f, world.floorS * 0.6f, 1f)
        }
        val sp = space?.blend ?: 0f
        space?.let { if (sp > 0f) it.kerb(edgeCol, edgeCol) }
        dropY = sp * sp * 70f // the ground falls away beneath the road
        val w = Lanes.w
        val u = Lanes.unfold
        val kerb = Lanes.halfRoadDrawn + 0.12f
        val landW = 30f
        val landX = kerb + 0.12f + landW / 2f
        for (t in tiles) {
            val fog = Fog.at(t.z)
            val c1 = if (sp > 0f) space!!.roadTile(tileA, t.col, false) else when (visualWorldId) {
                Worlds.NEON_CITY_ID -> cityRoadA
                Worlds.RAILWAY_ID -> railwayRoadA
                Worlds.JUNGLE_ID -> jungleRoadA
                Worlds.HIGHWAY_ID -> highwayRoadA
                Worlds.WINTER_ID -> winterRoadA
                Worlds.LAVA_ID -> lavaRoadA
                Worlds.NEXUS_ID -> nexusRoadA
                Worlds.BEACH_ID -> beachRoadA
                Worlds.NIGHT_ID -> nightRoadA
                Worlds.HACKER_ID -> hackerRoadA
                Worlds.AIR_ID -> airRoadA
                else -> t.col
            }
            val c2 = if (sp > 0f) space!!.roadTile(tileB, t.col2, true) else when (visualWorldId) {
                Worlds.NEON_CITY_ID -> cityRoadB
                Worlds.RAILWAY_ID -> railwayRoadB
                Worlds.JUNGLE_ID -> jungleRoadB
                Worlds.HIGHWAY_ID -> highwayRoadB
                Worlds.WINTER_ID -> winterRoadB
                Worlds.LAVA_ID -> lavaRoadB
                Worlds.NEXUS_ID -> nexusRoadB
                Worlds.BEACH_ID -> beachRoadB
                Worlds.NIGHT_ID -> nightRoadB
                Worlds.HACKER_ID -> hackerRoadB
                Worlds.AIR_ID -> airRoadB
                else -> t.col2
            }
            // the three core lanes, then the two outer ones growing out from the edges as the road unfolds
            if (sp > 0f) { // in space the road turns to glass: stars, rocks and planets show through beneath you
                val alpha = 1f - GLASS * sp
                // the tile, minus any rift crossing it: the gap stays open to space
                val rifts = space!!.rifts
                var top = t.z + tileD / 2f
                val bottom = t.z - tileD / 2f
                for (i in 0 until rifts.count) {
                    val hn = rifts.near(i); val hf = rifts.far(i)
                    if (hf >= top || hn <= bottom) continue
                    if (hn < top) glassPiece(hn, top, w, t.parity, c1, c2, fog, alpha)
                    top = min(top, hf)
                }
                if (top > bottom) glassPiece(bottom, top, w, t.parity, c1, c2, fog, alpha)
            } else for (l in 0 until 3) game.worldGround((l - 1) * w, -0.14f, t.z, w, 0.26f, tileD, if ((l + 1 + t.parity) % 2 == 0) c1 else c2, fog)
            if (u > 0.01f) {
                val ow = w * u
                game.worldGround(-(1.5f * w + ow / 2f), -0.14f, t.z, ow, 0.26f, tileD, if (t.parity == 0) c1 else c2, fog)
                game.worldGround(1.5f * w + ow / 2f, -0.14f, t.z, ow, 0.26f, tileD, if (t.parity == 0) c1 else c2, fog)
            }
            if (sp <= 0.001f) when (visualWorldId) {
                Worlds.NEON_CITY_ID -> {
                    // Four crisp cyan borders make the three-lane route readable at a glance.
                    for (boundary in 1 until Lanes.count) {
                        val x = (boundary - Lanes.count / 2f) * w
                        game.worldGround(x, .018f, t.z, .075f, .055f, tileD, cityCyan, fog)
                    }
                }
                Worlds.RAILWAY_ID -> {
                    // Compact sleepers and twin rails: decorative only; gameplay colliders stay on Track rows.
                    game.worldGround(0f, .006f, t.z, Lanes.halfRoadDrawn * 1.82f, .035f, .22f, railwayTie, fog)
                    game.worldGround(-.48f, .028f, t.z, .055f, .045f, tileD, railSteel, fog)
                    game.worldGround(.48f, .028f, t.z, .055f, .045f, tileD, railSteel, fog)
                }
                Worlds.HIGHWAY_ID -> {
                    for (boundary in 1 until Lanes.count) {
                        val x = (boundary - Lanes.count / 2f) * w
                        game.worldGround(x, .012f, t.z, .045f, .035f, 1.25f, highwayWhite, fog)
                    }
                    game.worldGround(-kerb + .20f, .012f, t.z, .045f, .035f, tileD, highwayGold, fog)
                    game.worldGround(kerb - .20f, .012f, t.z, .045f, .035f, tileD, highwayGold, fog)
                }
                Worlds.WINTER_ID -> {
                    for (boundary in 1 until Lanes.count) {
                        val x = (boundary - Lanes.count / 2f) * w
                        game.worldGround(x, .012f, t.z, .055f, .04f, tileD, winterIce, fog)
                    }
                    game.worldGround(-kerb - .1f, -.015f, t.z, .45f, .12f, tileD, winterSnow, fog)
                    game.worldGround(kerb + .1f, -.015f, t.z, .45f, .12f, tileD, winterSnow, fog)
                }
                Worlds.LAVA_ID -> {
                    for (boundary in 1 until Lanes.count) {
                        val x = (boundary - Lanes.count / 2f) * w
                        game.worldGround(x, .014f, t.z, .065f, .05f, tileD, lavaCore, fog)
                    }
                    for (side in -1..1 step 2) {
                        game.worldGround(side * (kerb - .14f), .01f, t.z, .12f, .07f, tileD, lavaFlow, fog)
                        game.worldGround(side * (kerb + 3.3f), -.02f, t.z, 1.2f, .10f, tileD,
                            if (t.parity == 0) lavaFlow else lavaCore, fog)
                    }
                }
                Worlds.NEXUS_ID -> {
                    for (boundary in 1 until Lanes.count) {
                        val x = (boundary - Lanes.count / 2f) * w
                        game.worldGround(x, .014f, t.z, .065f, .05f, tileD, nexusCyan, fog)
                    }
                    game.worldBoxSpin(-.60f, .038f, t.z, .07f, .045f, .68f, -24f, nexusMint, fog)
                    game.worldBoxSpin(.60f, .038f, t.z, .07f, .045f, .68f, 24f, nexusCyan, fog)
                    if (t.parity == 0) game.worldGround(0f, .015f, t.z, .055f, .04f, .36f, nexusViolet, fog)
                }
                Worlds.BEACH_ID -> {
                    for (boundary in 1 until Lanes.count) {
                        val x = (boundary - Lanes.count / 2f) * w
                        game.worldGround(x, .012f, t.z, .065f, .04f, tileD, beachFoam, fog)
                    }
                }
                Worlds.NIGHT_ID -> {
                    for (boundary in 1 until Lanes.count) {
                        val x = (boundary - Lanes.count / 2f) * w
                        game.worldGround(x, .012f, t.z, .06f, .04f, tileD, nightReflector, fog)
                    }
                    for (side in -1..1 step 2) game.worldGround(side * (kerb - .12f), .012f, t.z, .10f, .05f, .30f,
                        if (t.parity == 0) nightLamp else nightLampCore, fog)
                }
                Worlds.HACKER_ID -> {
                    for (boundary in 1 until Lanes.count) {
                        val x = (boundary - Lanes.count / 2f) * w
                        game.worldGround(x, .014f, t.z, .065f, .05f, tileD, hackerGreen, fog)
                    }
                    // Repeated V marks make the fork-left / fork-right choice legible from a distance.
                    game.worldBoxSpin(-.63f, .035f, t.z, .075f, .045f, .70f, -24f, hackerMint, fog)
                    game.worldBoxSpin(.63f, .035f, t.z, .075f, .045f, .70f, 24f, hackerGreen, fog)
                    game.worldGround(0f, .012f, t.z, .06f, .04f, .42f, hackerMagenta, fog)
                }
                Worlds.AIR_ID -> {
                    for (boundary in 1 until Lanes.count) {
                        val x = (boundary - Lanes.count / 2f) * w
                        game.worldGround(x, .014f, t.z, .065f, .05f, tileD, airEdge, fog)
                    }
                    // Short white dashes and a turquoise centerline make the elevated route easy to read.
                    if (t.parity == 0) game.worldGround(0f, .018f, t.z, .075f, .04f, .92f, cityCyan, fog)
                }
            }
            game.worldGround(-kerb, -0.1f, t.z, 0.24f, 0.34f, tileD, edgeCol, fog)
            game.worldGround(kerb, -0.1f, t.z, 0.24f, 0.34f, tileD, edgeCol, fog)
            if (dropY < 60f) {
                if (visualWorldId == Worlds.BEACH_ID) {
                    // A warm sandy shoulder, a white surf line, and broad blue water make this coast unmistakable.
                    val shoreStart = kerb + .18f
                    val shoreWidth = 4.55f
                    val shoreX = shoreStart + shoreWidth / 2f
                    val waterStart = shoreStart + shoreWidth - .04f
                    val waterWidth = 20f
                    val waterX = waterStart + waterWidth / 2f
                    val groundY = -.16f - dropY
                    game.worldGround(-shoreX, groundY, t.z, shoreWidth, .30f, tileD, beachSand, fog)
                    game.worldGround(shoreX, groundY, t.z, shoreWidth, .30f, tileD, beachSand, fog)
                    val water = if (t.parity == 0) beachWaterA else beachWaterB
                    game.worldGround(-waterX, groundY - .04f, t.z, waterWidth, .22f, tileD, water, fog)
                    game.worldGround(waterX, groundY - .04f, t.z, waterWidth, .22f, tileD, water, fog)
                    val surfX = waterStart + .12f
                    game.worldGround(-surfX, groundY + .015f, t.z, .20f, .08f, tileD, beachFoam, fog)
                    game.worldGround(surfX, groundY + .015f, t.z, .20f, .08f, tileD, beachFoam, fog)
                } else {
                    val groundColor = when (visualWorldId) {
                        Worlds.NEON_CITY_ID -> cityGround
                        Worlds.WINTER_ID -> winterGround
                        Worlds.LAVA_ID -> lavaGround
                        Worlds.NEXUS_ID -> nexusGround
                        Worlds.NIGHT_ID -> nightGround
                        Worlds.HACKER_ID -> hackerGround
                        Worlds.AIR_ID -> airCloudShade
                        else -> t.ground
                    }
                    game.worldGround(-landX, -0.16f - dropY, t.z, landW, 0.3f, tileD, groundColor, fog)
                    game.worldGround(landX, -0.16f - dropY, t.z, landW, 0.3f, tileD, groundColor, fog)
                }
            }
        }
    }

    /** One stretch of glass road, from [far] to [near], three lanes wide. */
    private fun glassPiece(far: Float, near: Float, w: Float, parity: Int, c1: Color, c2: Color, fog: Float, alpha: Float) {
        val len = near - far
        if (len < 0.02f) return
        for (l in 0 until 3) game.glassGround((l - 1) * w, -0.04f, (near + far) / 2f, w, 0.06f, len, if ((l + 1 + parity) % 2 == 0) c1 else c2, fog, alpha)
    }

    /** Everything beside and above the road: the roadside, the gates, the wind. [wind] 0..1 = how vivid the speed streaks are. */
    fun render(wind: Float, time: Float) {
        val low = Settings.graphicsPreset == Settings.GRAPHICS_LOW
        if (dropY < 1f && world.id == Worlds.AIR_ID) renderAirSky(time)
        if (dropY < 60f) for (i in posts.indices) {
            val p = posts[i]
            // Retain every near object, but halve distant paired scenery on Low.
            if (low && p.z < -20f && (i / 2) % 2 == 1) continue
            renderPost(p, time)
        }
        if (world.id == Worlds.WINTER_ID && dropY < 1f) {
            val count = when (Settings.graphicsPreset) {
                Settings.GRAPHICS_LOW -> snowflakes.size / 3
                Settings.GRAPHICS_HIGH -> snowflakes.size
                else -> snowflakes.size * 2 / 3
            }
            for (i in 0 until count) {
                val flake = snowflakes[i]
                val phase = (i * .618034f) % 1f
                val drift = Snowfall.drift(time, phase)
                val y = Snowfall.y(time, phase)
                val size = flake.len * (.78f + .22f * (.5f + .5f * sin(time * 2.3f + i)))
                val col = if (i % 4 == 0) winterIce else winterSnow
                game.worldBox(flake.x + drift, y, flake.z, size, size, size, col, Fog.at(flake.z))
            }
        }
        if (world.id == Worlds.LAVA_ID && dropY < 1f) {
            renderFallingLava(time, low)
            renderLavaEmbers(time, low)
        }
        if (nexusBonus || world.id == Worlds.HACKER_ID || world.id == Worlds.NEXUS_ID)
            renderCodeRain(time, low, nexusBonus || world.id == Worlds.NEXUS_ID)
        for (g in gates) renderGate(g, time)
        if (!low && world.id != Worlds.WINTER_ID && wind > 0f && dropY < 1f) { // streaks are decoration; lane and obstacle cues stay unchanged
            for (s in streaks) {
                val fade = max(Fog.at(s.z), 1f - wind * 0.75f)
                game.worldBox(s.x, s.y, s.z, 0.05f, 0.05f, s.len * (0.6f + wind * 0.8f), streakCol, fade)
            }
        }
    }

    /** Roadside pieces stand on the land: when it falls away (Galaxy) they fall with it. */
    private fun postBox(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, col: Color, fog: Float = 0f) =
        game.worldBox(x, y - dropY, z, sx, sy, sz, col, fog)

    private fun postSpin(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, yaw: Float, col: Color, fog: Float = 0f) =
        game.worldBoxSpin(x, y - dropY, z, sx, sy, sz, yaw, col, fog)

    /** A compact bank of molten droplets falls above the roadside, using one elongated box per particle. */
    private fun renderFallingLava(time: Float, low: Boolean) {
        val count = when {
            low -> 8
            Settings.graphicsPreset == Settings.GRAPHICS_HIGH -> 16
            else -> 12
        }
        for (i in 0 until count) {
            val phase = (i * .618034f) % 1f
            val y = FallingLava.y(time + (i % 3) * .09f, phase)
            val side = if (i % 2 == 0) -1f else 1f
            val x = side * (3.5f + (i % 4) * .92f)
            val z = -5f - (i * 7 % 10) * 3.5f
            val size = .075f + (i % 3) * .018f
            val spin = (time * 95f + i * 37f) % 360f
            game.worldBoxSpin(x, y, z, size, size * 2.8f, size * .78f, spin,
                if (i % 3 == 0) lavaCore else lavaFlow, Fog.at(z))
        }
    }

    /** Low-allocation rising embers make the lava shores feel hot without covering gameplay cues. */
    private fun renderLavaEmbers(time: Float, low: Boolean) {
        val count = if (low) 7 else 14
        for (i in 0 until count) {
            val side = if (i % 2 == 0) -1f else 1f
            val x = side * (4.8f + (i / 2 % 5) * 1.75f)
            val z = -((time * (3.2f + (i % 3) * .35f) + i * 8.7f) % 90f) - 8f
            val y = .22f + ((time * (.72f + (i % 4) * .11f) + i * .63f) % 4.2f)
            val size = .055f + .035f * (.5f + .5f * sin(time * 6f + i * 1.9f))
            game.worldBoxSpin(x, y, z, size, size * 1.45f, size, i * 19f + time * 35f,
                if (i % 3 == 0) lavaCore else lavaFlow, Fog.at(z))
        }
    }

    /** Falling green code fragments distinguish Hacker; the new Nexus uses a denser cyan/violet field. */
    private fun renderCodeRain(time: Float, low: Boolean, nexus: Boolean) {
        val columns = when {
            nexus && low -> 16
            nexus -> 30
            low -> 9
            else -> 18
        }
        val perColumn = if (low) 3 else 4
        for (column in 0 until columns) {
            val half = columns / 2
            val local = if (column < half) column else column - half
            val side = if (column < half) -1f else 1f
            val spacing = if (nexus) .72f else .82f
            val x = side * (5.3f + local * spacing)
            for (row in 0 until perColumn) {
                val z = -((time * (if (nexus) 2.1f else 1.65f) + column * 7.7f + row * 4.9f) % 88f) - 9f
                val y = .55f + ((column * 1.53f + row * 1.87f + time * 1.05f) % 7.3f)
                val width = .065f + .035f * ((column + row) % 3)
                val height = .12f + .025f * ((column + 2 * row) % 2)
                val color = when {
                    nexus && (column + row) % 7 == 0 -> nexusViolet
                    nexus && row == 0 -> nexusMint
                    nexus -> nexusCyan
                    (column + row) % 8 == 0 -> hackerMagenta
                    row == 0 -> hackerMint
                    else -> hackerGreen
                }
                game.worldBox(x, y, z, width, height, .045f, color, Fog.at(z))
            }
        }
    }

    /** Air's stylized sun and moon stay in the far sky; neither is a gameplay object. */
    private fun renderAirSky(time: Float) {
        renderAirCelestialBodies(time)
        renderPlane(time)
        renderHelicopter(time)
    }

    private fun renderAirCelestialBodies(time: Float) {
        val sunX = -15.5f; val sunY = 17f; val sunZ = -70f
        val sunFog = Fog.at(sunZ)
        for (i in 0..5) {
            val width = when (i) {
                0, 5 -> .75f
                1, 4 -> 1.45f
                else -> 1.9f
            }
            val pulse = .015f * sin(time * 1.8f + i)
            game.worldBox(sunX, sunY + (i - 2.5f) * .22f, sunZ, width, .23f + pulse, .045f,
                if (i in 2..3) airSunCore else airSun, sunFog)
        }
        val moonX = 15f; val moonY = 13.8f; val moonZ = -68f
        val moonFog = Fog.at(moonZ)
        for (i in 0..5) {
            val width = when (i) {
                0, 5 -> .55f
                1, 4 -> 1.02f
                else -> 1.35f
            }
            game.worldBox(moonX, moonY + (i - 2.5f) * .19f, moonZ, width, .20f, .04f, airMoon, moonFog)
        }
        // Cool offset bars make a crescent-like shade rather than a plain white disc.
        game.worldBox(moonX + .30f, moonY + .08f, moonZ + .025f, .52f, .68f, .035f, airMoonShade, moonFog)
        for (i in 0..3) {
            val sx = (i - 1.5f) * 8.2f
            val sy = 20f + (i % 2) * 2.1f
            val flicker = .045f + .018f * (.5f + .5f * sin(time * 3f + i * 2f))
            game.worldBox(sx, sy, -74f, flicker, flicker, .035f, airCloud, Fog.at(-74f))
        }
    }

    private fun renderHelicopter(time: Float) {
        val progress = HelicopterFlight.progress(time)
        if (progress < 0f) return
        val x = HelicopterFlight.x(progress)
        val y = HelicopterFlight.y(time, progress)
        val z = -59f
        val bank = HelicopterFlight.bank(progress)
        val fog = Fog.at(z)
        game.worldBoxSpin(x, y, z, 1.25f, .22f, .34f, bank, planeBody, fog)
        game.worldBoxSpin(x + .72f, y + .025f, z - .02f, .64f, .12f, .13f, bank, planeWing, fog)
        game.worldBoxSpin(x + 1.02f, y + .12f, z - .02f, .13f, .36f, .13f, bank, planeWing, fog)
        game.worldBoxSpin(x - .14f, y + .16f, z + .16f, .55f, .12f, .06f, bank, planeGlass, fog)
        game.worldBoxSpin(x - .72f, y + .18f, z, .11f, .045f, 1.55f, time * 560f, airRotor, fog)
        game.worldBoxSpin(x - 1.45f, y + .16f, z, .36f, .05f, .06f, time * 720f, airRotor, fog)
        game.worldBox(x - .05f, y - .20f, z + .20f, .72f, .055f, .06f, planeAccent, fog)
    }

    /** One small, clearly winged airliner crosses the far sky on a slow repeating fly-by. */
    private fun renderPlane(time: Float) {
        val progress = PlaneFlight.progress(time)
        if (progress < 0f) return
        val x = PlaneFlight.x(progress)
        val y = PlaneFlight.y(time, progress)
        val z = -52f
        val bank = PlaneFlight.bank(progress)
        val fog = Fog.at(z)
        game.worldBoxSpin(x, y, z, 2.35f, .18f, .24f, bank, planeBody, fog)
        game.worldBoxSpin(x + .12f, y + .035f, z, .16f, .09f, 2.05f, bank, planeWing, fog)
        game.worldBoxSpin(x - .83f, y + .04f, z, .15f, .09f, .82f, bank, planeWing, fog)
        game.worldBoxSpin(x + .42f, y + .13f, z, .57f, .13f, .22f, bank, planeGlass, fog)
        game.worldBoxSpin(x - .98f, y + .18f, z, .42f, .34f, .08f, bank, planeBody, fog)
        game.worldBoxSpin(x + 1.05f, y + .015f, z, .12f, .12f, .20f, bank, planeAccent, fog)
    }

    private fun renderPost(p: Post, time: Float) {
        val fog = Fog.at(p.z)
        val x = p.x; val z = p.z
        when (p.style) {
            Worlds.LOLLIPOPS -> when (p.kind) { // candy: gumdrops, candy canes, cupcakes — clean stacked shapes
                0 -> { // gumdrop: a fat base with a smaller dome on top
                    postSpin(x, p.h * 0.35f, z, 1.1f, p.h * 0.7f, 1.1f, 45f, p.col)
                    postSpin(x, p.h * 0.7f + p.h * 0.18f, z, 0.7f, p.h * 0.36f, 0.7f, 45f, p.col2, fog)
                }
                1 -> { // candy cane: striped stack
                    val n = 5; val seg = (p.h + 1.2f) / n
                    for (i in 0 until n) postBox(x, seg * (i + 0.5f), z, 0.34f, seg, 0.34f, if (i % 2 == 0) white else p.col, fog)
                    postBox(x - 0.3f, seg * n + 0.1f, z, 0.9f, 0.34f, 0.34f, p.col, fog)
                }
                else -> { // cupcake: wrapper, frosting, cherry
                    postBox(x, 0.35f, z, 1.0f, 0.7f, 1.0f, p.col3, fog)
                    postSpin(x, 0.7f + 0.3f, z, 1.15f, 0.6f, 1.15f, 45f, p.col2, fog)
                    postSpin(x, 1.3f + 0.22f, z, 0.6f, 0.44f, 0.6f, 45f, white, fog)
                    postBox(x, 1.74f + 0.15f, z, 0.3f, 0.3f, 0.3f, p.col, fog)
                }
            }
            Worlds.TOWERS -> { // a dark block with lit windows on its face
                postBox(x, p.h / 2f, z, 1.3f, p.h, 1.3f, dark, fog)
                var y = 0.6f
                var k = 0
                val face = if (x < 0f) x + 0.66f else x - 0.66f
                while (y < p.h - 0.4f && k < 7) {
                    val lit = sin(p.seed * 20f + k * 3.1f) > -0.2f
                    if (lit) postBox(face, y, z, 0.06f, 0.28f, 0.6f, if (k % 2 == 0) p.col else p.col2, fog)
                    y += 0.7f; k++
                }
                postBox(x, p.h + 0.15f, z, 0.5f, 0.3f, 0.5f, p.col, fog) // a lit cap
            }
            Worlds.CRYSTALS -> { // a shard cluster: three tapering stacks side by side
                spike(x, z, p.h, 0.7f, 45f + p.seed * 30f, p.col, fog)
                spike(x + 0.7f, z + 0.4f, p.h * 0.55f, 0.45f, 20f + p.seed * 50f, p.col2, fog)
                spike(x - 0.65f, z - 0.35f, p.h * 0.4f, 0.38f, 60f + p.seed * 40f, p.col2, fog)
            }
            Worlds.SPIKES -> if (world.id == Worlds.WINTER_ID && p.kind == 1) winterTree(x, z, fog)
                else spike(x, z, p.h, 1.2f, 0f, p.col, fog, cap = white)
            Worlds.CACTI -> { // a trunk with two arms and a flower
                postBox(x, p.h / 2f, z, 0.5f, p.h, 0.5f, p.col3, fog)
                postBox(x - 0.55f, p.h * 0.55f, z, 0.6f, 0.34f, 0.34f, p.col3, fog)
                postBox(x - 0.68f, p.h * 0.55f + 0.55f, z, 0.34f, 0.8f, 0.34f, p.col3, fog)
                postBox(x + 0.55f, p.h * 0.4f, z, 0.6f, 0.34f, 0.34f, p.col3, fog)
                postBox(x + 0.68f, p.h * 0.4f + 0.5f, z, 0.34f, 0.7f, 0.34f, p.col3, fog)
                postSpin(x, p.h + 0.2f, z, 0.36f, 0.36f, 0.36f, time * 30f, p.col, fog)
            }
            Worlds.RINGS -> { // a floating square frame, slowly turning, over a rock
                val yaw = time * 25f + p.seed * 360f
                val y = p.h + 0.3f * sin(time * 1.3f + p.seed * 6f)
                val r = 1.1f
                postSpin(x, y + r, z, 2 * r + 0.14f, 0.14f, 0.14f, yaw, p.col, fog)
                postSpin(x, y - r, z, 2 * r + 0.14f, 0.14f, 0.14f, yaw, p.col, fog)
                val rad = Math.toRadians(yaw.toDouble())
                val dx = (r * Math.cos(rad)).toFloat(); val dz = (-r * Math.sin(rad)).toFloat()
                postSpin(x + dx, y, z + dz, 0.14f, 2 * r - 0.14f, 0.14f, yaw, p.col, fog)
                postSpin(x - dx, y, z - dz, 0.14f, 2 * r - 0.14f, 0.14f, yaw, p.col, fog)
                postSpin(x, 0.35f, z, 0.7f, 0.7f, 0.7f, yaw * 0.5f, dark, fog)
            }
            Worlds.RAILWAY -> { // lit railway signal; a worker keeps watch at some posts
                postBox(x, p.h * .48f, z, .28f, p.h * .96f, .34f, p.col3, fog)
                postBox(x, p.h - .36f, z, .78f, .78f, .44f, dark, fog)
                val front = z + .235f
                postBox(x, p.h - .16f, front, .20f, .13f, .035f, signalRed, fog)
                postBox(x, p.h - .36f, front, .20f, .13f, .035f, signalAmber, fog)
                postBox(x, p.h - .56f, front, .20f, .13f, .035f, signalGreen, fog)
                postBox(x, p.h + .12f, z, 1.45f, .14f, .18f, p.col, fog)
                if (p.kind == 2 && fog < .74f) renderRailwayWorker(x, z + .38f, p.seed, time, fog)
            }
            Worlds.JUNGLE -> { // layered square canopy, vines and a warm trunk
                postBox(x, p.h * .46f, z, .34f, p.h * .92f, .34f, bark, fog)
                val sway = 10f * sin(time * .7f + p.seed * 8f)
                postSpin(x, p.h - .08f, z, 1.45f, .62f, 1.35f, sway, leafA, fog)
                postSpin(x + .12f, p.h + .38f, z - .08f, 1.05f, .54f, 1.0f, -sway * .7f, leafB, fog)
                if (p.kind != 0) {
                    postBox(x - .48f, p.h * .48f, z + .2f, .07f, p.h * .66f, .07f, leafA, fog)
                    postBox(x + .45f, p.h * .36f, z - .18f, .07f, p.h * .52f, .07f, leafB, fog)
                }
                postSpin(x, p.h + .72f, z, .28f, .28f, .28f, time * 30f + p.seed * 360f, p.col, fog)
            }
            Worlds.HIGHWAY -> { // illuminated roadside sign over a steel lamp mast
                postBox(x, p.h * .52f, z, .20f, p.h * 1.04f, .20f, railSteel, fog)
                postBox(x, p.h - .44f, z, 1.75f, .88f, .20f, dark, fog)
                postBox(x, p.h - .44f, z + .115f, 1.48f, .62f, .035f, p.col, fog)
                for (i in 0..2) {
                    val sx = x + (i - 1) * .40f
                    postBox(sx, p.h - .44f, z + .14f, .12f, .34f, .035f, if (i == 1) highwayWhite else highwayGold, fog)
                }
                postBox(x, p.h + .08f, z, 1.15f, .12f, .16f, highwayWhite, fog)
            }
            Worlds.NIGHT -> { // a warm pool of light on a near-black road
                val towardRoad = if (x < 0f) 1f else -1f
                postBox(x, p.h * .50f, z, .15f, p.h, .15f, dark, fog)
                postBox(x + towardRoad * .38f, p.h - .12f, z, .72f, .12f, .14f, p.col3, fog)
                postBox(x + towardRoad * .68f, p.h - .22f, z, .40f, .16f, .26f, nightLamp, fog)
                postBox(x + towardRoad * .68f, p.h - .20f, z + .14f, .24f, .09f, .04f, nightLampCore, fog)
                // Reflective bands catch the lamp pools, while the roadside stays almost black.
                postBox(x, .72f, z, .20f, .09f, .21f, if (p.kind == 0) nightLamp else nightReflector, fog)
                if (p.kind == 2) {
                    postBox(x - .48f, .64f, z, .08f, 1.28f, .08f, dark, fog)
                    postBox(x + .46f, .49f, z - .08f, .08f, .98f, .08f, dark, fog)
                }
            }
            Worlds.HACKER -> { // black server towers with moving green and magenta code bars
                postBox(x, p.h * .50f, z, 1.12f, p.h, .66f, hackerPanel, fog)
                postBox(x - .60f, p.h * .50f, z, .10f, p.h * 1.02f, .72f, hackerGreen, fog)
                postBox(x + .60f, p.h * .50f, z, .10f, p.h * 1.02f, .72f, hackerMint, fog)
                for (i in 0..4) {
                    val y = .40f + i * .43f
                    val flash = (i + p.kind) % 3 == 0
                    postBox(x - .24f, y, z + .35f, .24f, .08f, .035f, if (flash) hackerMagenta else hackerGreen, fog)
                    postBox(x + .18f, y + .12f, z + .35f, .28f, .07f, .035f, if (flash) hackerMint else p.col, fog)
                }
                postSpin(x, p.h + .24f + .08f * sin(time * 2f + p.seed * 9f), z,
                    .34f, .34f, .34f, time * 38f + p.seed * 360f, hackerMagenta, fog)
            }
            Worlds.AIR -> { // stepped cloudbanks, drifting puffs and the occasional tethered sky balloon
                val bob = .07f * sin(time * .8f + p.seed * 9f)
                postBox(x, .34f + bob, z, 1.88f, .68f, 1.18f, airCloudShade, fog)
                postBox(x - .43f, .84f + bob, z, .74f, .32f, .84f, airCloud, fog)
                postBox(x + .43f, .84f + bob, z, .74f, .32f, .84f, airCloud, fog)
                postBox(x, 1.17f + bob, z, .82f, .34f, .76f, if (p.kind == 1) p.col else airCloud, fog)
                if (p.kind == 0) {
                    postBox(x, 1.55f + bob, z, .16f, .42f, .16f, p.col3, fog)
                    postSpin(x, 1.87f + bob, z, .62f, .52f, .56f, time * 24f + p.seed * 80f, airBalloon, fog)
                    postBox(x, 1.87f + bob, z + .29f, .22f, .20f, .035f, airCloud, fog)
                } else if (p.kind == 2) {
                    postBox(x - .40f, 1.47f + bob, z, .14f, .30f, .14f, p.col2, fog)
                    postBox(x + .40f, 1.47f + bob, z, .14f, .30f, .14f, p.col2, fog)
                    postBox(x, 1.63f + bob, z, .78f, .10f, .14f, airBalloon, fog)
                }
            }
            Worlds.NEXUS -> { // fractured server pylons with offset cyan and violet light bars
                postBox(x, p.h * .50f, z, 1.05f, p.h, .68f, nexusGround, fog)
                postBox(x - .56f, p.h * .50f, z + .03f, .09f, p.h * .94f, .73f, nexusCyan, fog)
                postBox(x + .56f, p.h * .50f, z + .03f, .09f, p.h * .94f, .73f, nexusViolet, fog)
                for (i in 0..4) {
                    val y = .42f + i * .48f
                    val color = when {
                        (i + p.kind) % 4 == 0 -> nexusViolet
                        i % 2 == 0 -> nexusMint
                        else -> p.col
                    }
                    postBox(x - .22f, y, z + .36f, .25f, .085f, .035f, color, fog)
                    postBox(x + .18f, y + .12f, z + .36f, .28f, .075f, .035f, nexusCyan, fog)
                }
                postSpin(x, p.h + .24f + .10f * sin(time * 1.8f + p.seed * 8f), z,
                    .36f, .36f, .36f, time * 48f + p.seed * 360f, nexusViolet, fog)
            }
            Worlds.BEACH -> when (p.kind) {
                0 -> { // bent palm trunk with a spreading crown of blocky fronds
                    postBox(x, .82f, z, .23f, 1.64f, .23f, beachPalm, fog)
                    postSpin(x + .08f, 1.92f, z, .18f, .56f, .18f, -8f, beachPalm, fog)
                    for (i in 0..3) {
                        val yaw = i * 90f + sin(time * .8f + p.seed * 7f) * 6f
                        val rad = Math.toRadians(yaw.toDouble())
                        val dx = (cos(rad) * .46).toFloat()
                        val dz = (-sin(rad) * .46).toFloat()
                        postSpin(x + dx, 2.27f, z + dz, .34f, .14f, 1.25f, yaw,
                            if (i % 2 == 0) beachLeaf else beachLeafBright, fog)
                    }
                }
                1 -> { // striped parasol in broad, stepped layers
                    postBox(x, .92f, z, .12f, 1.84f, .12f, beachPalm, fog)
                    postBox(x, 1.98f, z, 1.28f, .20f, 1.28f, p.col, fog)
                    postBox(x, 2.12f, z, .96f, .16f, .96f, p.col2, fog)
                    postBox(x, 2.23f, z, .56f, .14f, .56f, p.col, fog)
                    postBox(x, 2.34f, z, .14f, .12f, .14f, beachUmbrellaAlt, fog)
                }
                else -> { // tiny striped beach hut and a bright roof
                    postBox(x, .78f, z, 1.15f, 1.5f, .92f, beachCabana, fog)
                    postBox(x, 1.65f, z, 1.55f, .25f, 1.18f, beachUmbrella, fog)
                    postBox(x, .79f, z + .49f, .38f, .86f, .06f, beachUmbrellaAlt, fog)
                    postBox(x, .82f, z + .525f, .16f, .56f, .07f, p.col2, fog)
                }
            }
        }
    }

    /** Small, non-colliding block-built rail worker, placed safely outside the road shoulder. */
    private fun renderRailwayWorker(signalX: Float, z: Float, seed: Float, time: Float, fog: Float) {
        val towardRoad = if (signalX < 0f) 1f else -1f
        val x = signalX + towardRoad * 1.05f
        val bob = .035f * sin(time * 3f + seed * 11f)
        val swing = .07f * sin(time * 2.4f + seed * 7f)
        postBox(x - .14f, .23f, z, .13f, .46f, .14f, workerTrousers, fog)
        postBox(x + .14f, .23f, z, .13f, .46f, .14f, workerTrousers, fog)
        postBox(x, .72f + bob, z, .46f, .55f, .30f, workerVest, fog)
        postBox(x, .77f + bob, z + .16f, .40f, .11f, .035f, workerHelmet, fog) // reflective strip
        postBox(x - .30f, .72f + swing, z, .13f, .48f, .14f, workerVest, fog)
        postBox(x + .30f, .72f - swing, z, .13f, .48f, .14f, workerVest, fog)
        postBox(x, 1.13f + bob, z, .34f, .31f, .29f, workerFace, fog)
        postBox(x, 1.34f + bob, z, .43f, .13f, .37f, workerHelmet, fog)
    }

    /** A compact blocky fir with three snow-capped layers; winter roadside only. */
    private fun winterTree(x: Float, z: Float, fog: Float) {
        postBox(x, .6f, z, .24f, 1.2f, .24f, winterBark, fog)
        for (i in 0..2) {
            val y = 1.55f + i * .78f
            val size = 1.45f - i * .39f
            postBox(x, y, z, size, .72f, size, winterPine, fog)
            postBox(x, y + .33f, z, size * .82f, .07f, size * .82f, winterSnow, fog)
        }
    }

    /** A tapering stack of shrinking boxes (no intersections). */
    private fun spike(x: Float, z: Float, h: Float, base: Float, yaw: Float, col: Color, fog: Float, cap: Color = col) {
        val n = 3
        for (i in 0 until n) {
            val s = base * (1f - i * 0.3f)
            val seg = h / n
            postSpin(x, seg * (i + 0.5f), z, s, seg, s, yaw, if (i == n - 1) cap else col, fog)
        }
    }

    /** Two tall pylons and a beam across the road, in the coming world's colour. The start gate adds bunting and flags. */
    private fun renderGate(g: Gate, time: Float) {
        val fog = Fog.at(g.z)
        val x = Lanes.halfRoadDrawn + 1.1f
        val h = if (g.start) 5.4f else 4.0f // the start gate stands taller: the camera passes clean under its bunting
        game.worldBox(-x, h / 2f, g.z, 0.5f, h, 0.5f, g.col, fog)
        game.worldBox(x, h / 2f, g.z, 0.5f, h, 0.5f, g.col, fog)
        game.worldBox(0f, h + 0.15f, g.z, x * 2f + 0.5f, 0.4f, 0.5f, g.col, fog)
        game.worldBox(0f, h + 0.55f, g.z, 1.4f, 0.4f, 0.5f, white, fog)
        if (g.start) { // a chequered start banner under the beam, a chequered line on the road, a spinning star on each pylon
            val n = 12
            val cw = (2 * x - 0.6f) / n
            for (i in 0 until n) {
                val bx = -x + 0.3f + cw * (i + 0.5f)
                game.worldBox(bx, h - 0.22f, g.z, cw, 0.36f, 0.2f, if (i % 2 == 0) white else dark, fog)
            }
            val m = 8
            val lw = (Lanes.halfRoadDrawn * 2f) / m
            for (i in 0 until m) for (k in 0 until 2) {
                game.worldBox(-Lanes.halfRoadDrawn + lw * (i + 0.5f), 0.0f, g.z + (k - 0.5f) * 0.55f, lw, 0.03f, 0.55f, if ((i + k) % 2 == 0) white else dark, fog)
            }
            hsvInto(edgeCol, 50f, 0.8f, 1f)
            game.worldBoxSpin(-x, h + 0.55f, g.z, 0.55f, 0.55f, 0.55f, time * 140f, edgeCol, fog)
            game.worldBoxSpin(x, h + 0.55f, g.z, 0.55f, 0.55f, 0.55f, -time * 140f, edgeCol, fog)
        }
    }
}
