package cube.run.game.track

import com.badlogic.gdx.graphics.Color
import cube.run.core.Gdx3DGame
import cube.run.core.gfx.FacetShapes
import cube.run.core.hsvInto
import cube.run.data.Settings
import cube.run.data.Worlds
import cube.run.game.world.Fog
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws the track: every obstacle, platform, pickup and coin, all through
 * the engine's batched passes (boxes + coins), so the whole road costs two
 * draw calls. Rows stream in: everything scales by the row's [Row.pop], so
 * obstacles spring up out of the floor and coins bloom as they come into
 * view. Pure rendering — no state, no gameplay.
 */
class TrackRenderer(private val game: Gdx3DGame) {

    private val cues = ObstacleCues(game)
    private val duckArrows = DuckArrows()
    /** Review candidates use the same live renderer; choose a final style after comparison. */
    var cueStyle: ObstacleCueStyle
        get() = cues.style
        set(value) { cues.style = value }

    private val shardCols = cube.run.data.Shards.all.map { hsvInto(Color(), it.hue, .6f, 1f) }
    private val coinCol = Color()
    private val coinKindCols = Array(CoinType.values().size) { Color() }
    private val coinKindFaceCols = Array(CoinType.values().size) { Color() }
    private val cityBarrierRed = Color(.92f, .035f, .045f, 1f)
    private val cityBarrierWhite = Color(.96f, .90f, .78f, 1f)
    private val cityCarRed = Color(.88f, .09f, .12f, 1f)
    private val cityCarBlue = Color(.08f, .42f, .82f, 1f)
    private val cityCarGold = Color(1f, .54f, .08f, 1f)
    private val cityBikeFrame = Color(.96f, .26f, .055f, 1f)
    private val cityWheel = Color(.025f, .035f, .052f, 1f)
    private val citySign = Color(.04f, .54f, .28f, 1f)
    private val winterSnow = Color(.84f, .94f, 1f, 1f)
    private val winterIce = Color(.34f, .67f, .86f, 1f)
    private val winterDeep = Color(.12f, .27f, .42f, 1f)
    private val winterSnowPalette = arrayOf(winterIce, winterSnow)
    /** Set from the actual world's spawn palette; markings are visual only. */
    var stripedCityHazards = false
    /** The scenery family currently spawning ahead of the world gate. */
    var worldStyle = Worlds.TOWERS
    private val trainBody = Color(.045f, .06f, .08f, 1f)
    private val trainStripe = Color(.95f, .35f, .08f, 1f)
    private val trainWindow = Color(.05f, .75f, .95f, 1f)
    private val trainWheel = Color(.035f, .04f, .05f, 1f)
    private val trafficBody = Color(.08f, .10f, .15f, 1f)
    private val trafficGlass = Color(.10f, .58f, .74f, 1f)
    private val trafficLamp = Color(1f, .74f, .18f, 1f)
    private val trafficRed = Color(.92f, .045f, .035f, 1f)
    private val signalGreen = Color(.10f, .92f, .38f, 1f)
    private val barrierYellow = Color(1f, .62f, .05f, 1f)
    private val barrierBlack = Color(.018f, .022f, .03f, 1f)
    private val jungleBark = Color(.28f, .13f, .045f, 1f)
    private val jungleMoss = Color(.18f, .58f, .12f, 1f)
    private val jungleLeaf = Color(.38f, .78f, .19f, 1f)
    private val jungleThorn = Color(.91f, .86f, .52f, 1f)
    private val jungleFruit = Color(.96f, .16f, .12f, 1f)
    private val jungleSpot = Color(1f, .83f, .40f, 1f)
    private val jungleMud = Color(.055f, .16f, .045f, 1f)
    private val beachWater = Color(.035f, .39f, .70f, 1f)
    private val beachFoam = Color(.62f, .96f, 1f, 1f)
    private val beachShell = Color(1f, .63f, .23f, 1f)
    private val beachBoard = Color(.43f, .22f, .095f, 1f)
    private val beachCrab = Color(.84f, .18f, .10f, 1f)
    private val beachCrabLight = Color(1f, .56f, .23f, 1f)
    private val jungleDinoSkin = Color(.18f, .43f, .12f, 1f)
    private val jungleDinoBelly = Color(.62f, .72f, .28f, 1f)
    private val jungleBushPalette = arrayOf(jungleMoss)
    private val jungleFruitPalette = arrayOf(jungleFruit)
    private val jungleLeafPalette = arrayOf(jungleLeaf)
    private val spaceRock = arrayOf(Color(.24f, .20f, .36f, 1f), Color(.48f, .43f, .58f, 1f))
    private val spaceRingColors = arrayOf(Color(.08f, .76f, 1f, 1f), Color(.84f, .32f, 1f, 1f))
    private val spaceLaser = Color(.08f, .85f, 1f, 1f)
    private val spacePanel = Color(.10f, .27f, .44f, 1f)
    private val spacePanelLight = Color(.32f, .92f, 1f, 1f)
    private val spaceWarning = Color(1f, .27f, .56f, 1f)
    private val nightWolf = Color(.19f, .22f, .30f, 1f)
    private val nightWolfLight = Color(.43f, .48f, .58f, 1f)
    private val nightTiger = Color(.98f, .39f, .055f, 1f)
    private val nightStripe = Color(.035f, .025f, .04f, 1f)
    private val nightEye = Color(1f, .82f, .27f, 1f)
    private val hackerBlock = Color(.012f, .065f, .035f, 1f)
    private val hackerGreen = Color(.06f, 1f, .34f, 1f)
    private val hackerMint = Color(.55f, 1f, .76f, 1f)
    private val hackerPink = Color(.96f, .08f, .72f, 1f)
    private val lavaBasalt = Color(.13f, .055f, .045f, 1f)
    private val lavaCrust = Color(.055f, .022f, .025f, 1f)
    private val lavaGlow = Color(1f, .24f, .025f, 1f)
    private val lavaHot = Color(1f, .72f, .10f, 1f)
    private val nexusBody = Color(.008f, .025f, .032f, 1f)
    private val nexusCyan = Color(.08f, .92f, 1f, 1f)
    private val nexusMint = Color(.28f, 1f, .68f, 1f)
    private val nexusViolet = Color(.72f, .18f, 1f, 1f)
    private val airEagleBody = Color(.31f, .20f, .11f, 1f)
    private val airEagleWing = Color(.12f, .095f, .08f, 1f)
    private val airEagleHead = Color(.78f, .64f, .36f, 1f)
    private val airHawkBody = Color(.37f, .43f, .49f, 1f)
    private val airHawkWing = Color(.15f, .21f, .28f, 1f)
    private val airHawkHead = Color(.70f, .78f, .84f, 1f)
    private val airGullBody = Color(.92f, .97f, 1f, 1f)
    private val airGullWing = Color(.50f, .66f, .78f, 1f)
    private val airGullHead = Color(.98f, .99f, 1f, 1f)
    private val airBeak = Color(1f, .57f, .10f, 1f)
    private val airEye = Color(1f, .86f, .18f, 1f)
    private val airCloud = Color(.88f, .96f, 1f, 1f)
    private val airCloudShade = Color(.42f, .70f, .85f, 1f)
    private val jungleBushShape = FacetShapes.voxelBall(6)
    private val jungleMushroomShape = FacetShapes.voxelBall(6)
    private val spaceRockShapes = arrayOf(FacetShapes.clump(0), FacetShapes.clump(1), FacetShapes.clump(2))
    private val spaceRingShape = FacetShapes.voxelRing(18, .56f, 3)
    private var obstacleShapesPrepared = false
    private var winterShapePrepared = false
    /** Cosmetic detail never consumes the bounded box batch without limit. */
    private var detailBoxesQueued = 0
    private val detailBoxLimit = 180
    private val toxicCoinCol = Color(.18f, 1f, .03f, 1f)
    private val toxicCoinFace = Color(.55f, 1f, .12f, 1f)
    private val coinFace = Color()
    private val coalBody = Color(0.13f, 0.14f, 0.16f, 1f)
    private val coalFacet = Color(0.235f, 0.255f, 0.295f, 1f)

    /** Latched by the run owner; browsing another cube must not recolour an active run. */
    var coalCoins = false

    /** A charcoal burst with enough light to remain legible against the road. */
    val gem = Color(.6f, .86f, 1f, 1f)
    val coal: Color get() = coalFacet
    private val boxCol = Color()
    private val bandCol = Color()
    private val magnetCol = Color()
    private val multCol = Color()
    private val jetCol = Color()
    private val flameCol = Color()
    private val skateboardDeck = Color(1f, .32f, .055f, 1f)
    private val skateboardStripe = Color(.08f, .9f, 1f, 1f)
    private val skateboardWheel = Color(.025f, .035f, .055f, 1f)
    private val jumpShoeBody = Color(1f, .66f, .12f, 1f)
    private val jumpShoeSole = Color(.12f, .08f, .20f, 1f)
    private val jumpShoeLace = Color(.12f, .82f, 1f, 1f)
    private val phasePowerCol = Color(.60f, .34f, 1f, 1f)
    private val timeWarpCol = Color(.12f, .92f, .82f, 1f)
    private val padTop = Color()
    private val rod = Color(0.32f, 0.28f, 0.4f, 1f)
    /** On the glass road, a full-width pit is an open gap (see SpaceRifts), not a black slab. */
    var glassRifts = false
    private val portalA = Color()
    private val portalB = Color()
    private val tmpCol = Color()

    init {
        hsvInto(coinCol, 44f, 0.9f, 1f)
        hsvInto(coinFace, 50f, 0.55f, 1f)
        for (kind in CoinType.values()) {
            hsvInto(coinKindCols[kind.ordinal], kind.hue, .88f, 1f)
            hsvInto(coinKindFaceCols[kind.ordinal], kind.hue + 18f, .42f, 1f)
        }
        hsvInto(boxCol, 282f, 0.72f, 0.95f)
        hsvInto(bandCol, 46f, 0.8f, 1f)
        hsvInto(magnetCol, 4f, 0.9f, 1f)
        hsvInto(multCol, 328f, 0.8f, 1f)
        hsvInto(jetCol, 215f, 0.75f, 1f)
        hsvInto(flameCol, 28f, 0.9f, 1f)
        hsvInto(padTop, 0f, 0f, 1f)
    }

    /** The coin colour (for bursts). */
    val gold: Color get() = coinCol

    private fun prepareObstacleShapes() {
        if (!obstacleShapesPrepared && (worldStyle == Worlds.JUNGLE || worldStyle == Worlds.RINGS)) {
            game.facets.prepare(jungleBushShape, jungleMushroomShape, spaceRockShapes[0], spaceRockShapes[1], spaceRockShapes[2], spaceRingShape)
            obstacleShapesPrepared = true
        }
        if (!winterShapePrepared && worldStyle == Worlds.SPIKES) {
            game.facets.prepare(jungleBushShape)
            winterShapePrepared = true
        }
    }

    fun renderCues(shapes: com.badlogic.gdx.graphics.glutils.ShapeRenderer, track: Track, opacity: Float, matrix: Float) {
        if (cueStyle.duckOnly) duckArrows.render(shapes, track, game.fogColor, opacity, matrix, cueStyle)
    }

    fun render(track: Track, time: Float, kaleido: Float = 0f, kaleidoHue: Float = 0f) {
        detailBoxesQueued = 0
        prepareObstacleShapes()
        val yaw = (time * 240f) % 360f
        // coins spin in lockstep, a little out of phase down the line, so every line glints in a wave
        val coinYaw = time * 190f
        for (r in track.rows) { // obstacles first: batch overflow drops from the back
            val p = r.pop
            if (p <= 0.001f) continue
            val fog = Fog.at(r.z)
            if (r.portal >= 0) renderPortal(r, time, fog, p)
            for (ob in r.obs) {
                val art = ObstacleArtResolver.resolve(worldStyle, ob.type, ob.anim, ob.cue, ob.pit, ob.sx, ob.sy, ob.nightAnimal, ob.airBird)
                val source = cues.body(ob, if (kaleido > 0.01f) tmpCol.set(ob.col).lerp(hsvInto(portalB, kaleidoHue + r.z * 6f, 0.9f, 1f), kaleido) else ob.col)
                when {
                    ob.type == ObType.PLAT && art == ObstacleArt.JUNGLE_ROOT_BRIDGE -> renderJunglePlatform(ob, r.z, fog, p)
                    ob.type == ObType.PLAT && art == ObstacleArt.SPACE_STATION_PANEL -> renderSpacePlatform(ob, r.z, fog, p)
                    ob.type == ObType.PLAT && art == ObstacleArt.CITY_ROOFTOP -> renderCityPlatform(ob, r.z, fog, p)
                    ob.type == ObType.PLAT && art == ObstacleArt.WINTER_ICE_BRIDGE -> renderWinterPlatform(ob, r.z, fog, p)
                    ob.type == ObType.PLAT && art == ObstacleArt.HACKER_PLATFORM -> renderHackerPlatform(ob, r.z, fog, p)
                    ob.type == ObType.PLAT && art == ObstacleArt.LAVA_BRIDGE -> renderLavaBridge(ob, r.z, fog, p)
                    ob.type == ObType.PLAT && art == ObstacleArt.NEXUS_PLATFORM -> renderNexusPlatform(ob, r.z, fog, p)
                    ob.type == ObType.PLAT && art == ObstacleArt.BEACH_BOARDWALK -> renderBeachBoardwalk(ob, r.z, fog, p)
                    ob.type == ObType.PLAT && art == ObstacleArt.AIR_CLOUD_BRIDGE -> renderAirPlatform(ob, r.z, fog, p)
                    ob.type == ObType.PLAT -> renderPlatform(ob, r.z, fog, p)
                    ob.type == ObType.PAD && art == ObstacleArt.JUNGLE_MUSHROOM -> renderJunglePad(ob, r.z, fog, time, p)
                    ob.type == ObType.PAD && art == ObstacleArt.SPACE_GRAVITY_RING -> renderSpaceRing(ob, r.z, fog, time, p)
                    ob.type == ObType.PAD && art == ObstacleArt.CITY_PAD -> renderCityPad(ob, r.z, fog, time, p)
                    ob.type == ObType.PAD && art == ObstacleArt.WINTER_SPRING -> renderWinterPad(ob, r.z, fog, time, p)
                    ob.type == ObType.PAD && art == ObstacleArt.HACKER_PAD -> renderHackerPad(ob, r.z, fog, time, p)
                    ob.type == ObType.PAD && art == ObstacleArt.LAVA_PAD -> renderLavaPad(ob, r.z, fog, time, p)
                    ob.type == ObType.PAD && art == ObstacleArt.NEXUS_PAD -> renderNexusPad(ob, r.z, fog, time, p)
                    ob.type == ObType.PAD && art == ObstacleArt.BEACH_SHELL_PAD -> renderBeachShellPad(ob, r.z, fog, time, p)
                    ob.type == ObType.PAD && art == ObstacleArt.AIR_THERMAL_PAD -> renderAirPad(ob, r.z, fog, time, p)
                    ob.type == ObType.PAD -> renderPad(ob, r.z, fog, time, p)
                    ob.anim == ObAnim.PISTON && ob.sy < 0.03f -> {} // sunk into the floor
                    ob.pit -> if (!glassRifts || ob.sx < cube.run.game.space.SpaceRifts.WIDE) {
                        when (art) {
                            ObstacleArt.JUNGLE_SWAMP -> renderJunglePit(ob, r.z, fog, time, p, r.visualPhase)
                            ObstacleArt.WINTER_ICE_HOLE -> renderWinterPit(ob, r.z, fog, p)
                            ObstacleArt.CITY_PIT -> renderCityPit(ob, r.z, fog, p)
                            ObstacleArt.NIGHT_SHADOW_GAP -> renderNightShadowGap(ob, r.z, fog, p)
                            ObstacleArt.HACKER_VOID -> renderHackerVoid(ob, r.z, fog, p)
                            ObstacleArt.LAVA_CRUST -> renderLavaGap(ob, r.z, fog, p)
                            ObstacleArt.NEXUS_VOID -> renderNexusVoid(ob, r.z, fog, p)
                            ObstacleArt.BEACH_TIDAL_GAP -> renderBeachTidalGap(ob, r.z, fog, p)
                            ObstacleArt.AIR_CLOUD_GAP -> renderAirCloudGap(ob, r.z, fog, p)
                            else -> renderPit(ob, r.z, fog, time, p, r.visualPhase)
                        }
                    }
                    ob.anim == ObAnim.PENDULUM && art == ObstacleArt.JUNGLE_VINE_PENDULUM -> renderJungleVinePendulum(ob, r.z, fog, p, time)
                    ob.anim == ObAnim.PENDULUM && art == ObstacleArt.JUNGLE_DINO -> renderJungleDinoHazard(ob, r.z, fog, p, time)
                    ob.anim == ObAnim.PENDULUM && art == ObstacleArt.LAVA_BOULDER -> renderLavaBoulder(ob, r.z, fog, p, time)
                    ob.anim == ObAnim.PENDULUM && art == ObstacleArt.NEXUS_DRONE -> renderNexusDrone(ob, r.z, fog, p, time)
                    ob.anim == ObAnim.PENDULUM && art == ObstacleArt.BEACH_CRAB -> renderBeachCrab(ob, r.z, fog, p, time)
                    ob.anim == ObAnim.PENDULUM && art == ObstacleArt.SPACE_ORBITAL_DRONE -> renderSpaceDrone(ob, r.z, fog, time, p)
                    ob.anim == ObAnim.PENDULUM && art == ObstacleArt.CITY_BIKE -> renderCityBike(ob, r.z, fog, p, time)
                    ob.anim == ObAnim.PENDULUM && art == ObstacleArt.WINTER_SNOWBALL -> renderWinterSnowball(ob, r.z, fog, p, time)
                    ob.anim == ObAnim.PENDULUM && art == ObstacleArt.HACKER_DRONE -> renderHackerDrone(ob, r.z, fog, p, time)
                    ob.anim == ObAnim.PENDULUM && art == ObstacleArt.NIGHT_WOLF -> renderNightAnimal(ob, r.z, fog, p, time, RoadAnimal.WOLF)
                    ob.anim == ObAnim.PENDULUM && art == ObstacleArt.NIGHT_TIGER -> renderNightAnimal(ob, r.z, fog, p, time, RoadAnimal.TIGER)
                    ob.anim == ObAnim.PENDULUM -> renderGenericPendulum(ob, r.z, fog, p)
                    else -> renderWorldObstacle(ob, r.z, fog, p, time, source)
                }
                if (stripedCityHazards && art != ObstacleArt.CITY_BARRIER && ob.cue == ObCue.JUMP && ob.type == ObType.SOLID)
                    renderCityBarrierStripe(ob, r.z, fog, p)
            }
            if (r.pickup != Pickup.NONE && r.pickup != Pickup.BUBBLE) renderPickup(r, yaw, time, p)
        }
        // Every solid is queued before review markings, preserving collision visibility at capacity.
        for (r in track.rows) {
            if (r.pop <= .001f) continue
            val fog = Fog.at(r.z)
            for (ob in r.obs) cues.render(ob, r.z, fog, r.pop)
        }
        // Coins and coal share pickup positions, magnet motion, fog and stream-in animation.
        for (r in track.rows) {
            val coins = r.coins ?: continue
            val p = r.pop
            if (p <= 0.001f) continue
            for (c in coins) {
                if (c.taken) continue
                val cz = r.z + c.dz
                val y = c.y + 0.06f * sin(time * 4f + c.dz * 0.9f)
                val fog = Fog.at(cz)
                val yaw = coinYaw + (r.visualPhase + c.dz) * 14f
                if (coalCoins && c.gem) {
                    game.worldGem(c.x, y, cz, 1.05f * p, yaw, gem, fog)
                } else if (coalCoins) {
                    val variant = r.visualPhase.toBits() xor c.dz.toBits() xor c.restY.toBits()
                    game.worldCoal(c.x, y, cz, 1.05f * p, time * 65f + (r.visualPhase + c.dz) * 23f, variant, coalBody, fog)
                } else {
                    val body = if (c.toxic) toxicCoinCol else coinKindCols[c.kind.ordinal]
                    val face = if (c.toxic) toxicCoinFace else coinKindFaceCols[c.kind.ordinal]
                    game.worldCoin(c.x, y, cz, 0.36f * p, 0.14f, yaw, body, fog)
                    game.worldCoin(c.x, y, cz, 0.23f * p, 0.2f, yaw, face, fog)
                    if (c.kind == CoinType.MIXED && !c.toxic) {
                        // Three tiny colour facets make the rare mixed coin readable at runner distance.
                        game.worldCoin(c.x - .105f * p, y - .015f * p, cz + .012f, .075f * p, .205f, yaw, coinKindCols[CoinType.RED.ordinal], fog)
                        game.worldCoin(c.x + .105f * p, y - .015f * p, cz + .012f, .075f * p, .205f, yaw, coinKindCols[CoinType.GREEN.ordinal], fog)
                        game.worldCoin(c.x, y + .10f * p, cz + .012f, .075f * p, .205f, yaw, coinKindCols[CoinType.BLUE.ordinal], fog)
                    }
                }
            }
        }
    }

    /** In-run board model; it follows the cube's feet and is part of the timed skateboard pickup. */
    fun renderSkateboard(x: Float, y: Float, z: Float, time: Float) {
        val wobble = 4f * sin(time * 8f)
        game.worldBoxSpin(x, y, z, 1.12f, .12f, .54f, wobble, skateboardDeck, 0f)
        game.worldBoxSpin(x, y + .065f, z, .74f, .035f, .095f, wobble, skateboardStripe, 0f)
        for (side in -1..1 step 2) for (front in -1..1 step 2) {
            game.worldBoxSpin(x + side * .34f, y - .105f, z + front * .18f,
                .16f, .14f, .16f, wobble, skateboardWheel, 0f)
        }
    }

    /** Pair of springy shoes stays visible under the cube while the jump boost is active. */
    fun renderJumpShoes(x: Float, y: Float, z: Float, time: Float) {
        val bounce = .025f * sin(time * 8f)
        for (side in -1..1 step 2) {
            val sx = x + side * .23f
            game.worldBoxSpin(sx, y + .09f + bounce, z, .34f, .18f, .45f, side * 4f, jumpShoeBody, 0f)
            game.worldBoxSpin(sx, y - .01f + bounce, z, .36f, .08f, .48f, side * 4f, jumpShoeSole, 0f)
            game.worldBoxSpin(sx, y + .17f + bounce, z + .02f, .20f, .045f, .24f, side * 4f, jumpShoeLace, 0f)
        }
    }

    /** Map-themed Rift Gate: a solid monolith arch with a pulsing prism, not the old bead-ring portal. */
    private fun renderPortal(r: Row, time: Float, fog: Float, p: Float) {
        val style = r.portalStyle.takeIf { it >= 0 } ?: worldStyle
        val mapHue = when (style) {
            Worlds.LOLLIPOPS -> 326f
            Worlds.TOWERS -> 190f
            Worlds.CRYSTALS -> 282f
            Worlds.SPIKES -> 205f
            Worlds.CACTI -> 34f
            Worlds.RINGS -> 266f
            Worlds.RAILWAY -> 46f
            Worlds.JUNGLE -> 116f
            Worlds.HIGHWAY -> 18f
            Worlds.BEACH -> 184f
            else -> 195f
        }
        val targetHue = if (r.portalExit) mapHue + 150f else cube.run.data.Bonus.all.getOrNull(r.portal)?.hue ?: 195f
        hsvInto(portalA, mapHue, .82f, 1f)
        hsvInto(portalB, targetHue + 28f, .88f, 1f)
        val s = p.coerceIn(0f, 1f)
        val pulse = .06f * sin(time * 3.4f)
        val z = r.z
        val half = 2.22f * s
        val height = 4.15f * s
        // Heavy side pillars, a lintel and a sill form a clear doorway silhouette from far away.
        game.worldBox(-half, height * .5f, z, .42f * s, height, .46f * s, portalA, fog)
        game.worldBox(half, height * .5f, z, .42f * s, height, .46f * s, portalA, fog)
        game.worldBox(0f, height - .12f * s, z, 4.86f * s, .38f * s, .46f * s, portalA, fog)
        game.worldBox(0f, .22f * s, z, 4.86f * s, .24f * s, .46f * s, portalA, fog)
        // The gate's energy is a rotating cross and floating prism, not a second ring of beads.
        game.worldBoxSpin(0f, 2.14f * s + pulse, z - .17f * s, 1.48f * s, .15f * s, .16f * s, time * 38f, portalB, fog)
        game.worldBoxSpin(0f, 2.14f * s + pulse, z - .20f * s, .15f * s, 2.55f * s, .16f * s, -time * 38f, Color.WHITE, fog)
        game.worldBoxSpin(0f, 2.14f * s + pulse, z - .28f * s, .50f * s, .50f * s, .50f * s, time * 72f, portalB, fog)
        for (side in -1..1 step 2) {
            game.worldBoxSpin(side * 1.73f * s, 3.16f * s, z - .08f * s, .48f * s, .18f * s, .34f * s,
                time * 24f * side, portalB, fog)
            game.worldBoxSpin(side * 1.73f * s, 1.18f * s, z - .08f * s, .48f * s, .18f * s, .34f * s,
                -time * 24f * side, portalB, fog)
        }
        // Every biome keeps the same readable doorway, but gets its own signature gate ornaments.
        when (style) {
            Worlds.LOLLIPOPS -> for (i in 0..2) game.worldBoxSpin((i - 1) * .92f * s, 3.98f * s, z - .25f * s,
                .32f * s, .30f * s, .30f * s, 45f + time * 18f, if (i % 2 == 0) Color.WHITE else portalB, fog)
            Worlds.TOWERS -> for (i in 0..2) {
                val y = (1.42f + i * .60f) * s
                game.worldBox(-1.99f * s, y, z - .25f * s, .08f * s, .34f * s, .07f * s, portalB, fog)
                game.worldBox(1.99f * s, y, z - .25f * s, .08f * s, .34f * s, .07f * s, Color.WHITE, fog)
            }
            Worlds.HIGHWAY -> for (i in 0..3) {
                val x = (i - 1.5f) * .72f * s
                game.worldBoxSpin(x, .72f * s, z - .28f * s, .12f * s, .42f * s, .07f * s,
                    0f, if (i % 2 == 0) portalB else Color.WHITE, fog)
            }
            Worlds.CRYSTALS -> for (side in -1..1 step 2) {
                game.worldBoxSpin(side * 2.20f * s, 3.90f * s, z, .44f * s, .62f * s, .42f * s,
                    side * 30f + time * 18f, Color.WHITE, fog)
                game.worldBoxSpin(side * 2.20f * s, .48f * s, z, .26f * s, .42f * s, .26f * s,
                    -side * 25f - time * 16f, portalB, fog)
            }
            Worlds.SPIKES -> for (i in 0..4) {
                val x = (i - 2) * .72f * s
                val angle = if (i % 2 == 0) 28f else -28f
                game.worldBoxSpin(x, 3.78f * s, z - .23f * s, .26f * s, .66f * s, .26f * s,
                    angle + time * 12f, if (i % 2 == 0) Color.WHITE else portalB, fog)
            }
            Worlds.CACTI -> for (side in -1..1 step 2) for (i in 0..1) {
                game.worldBox(side * 2.22f * s, (1.45f + i * 1.12f) * s, z - .26f * s,
                    .66f * s, .12f * s, .06f * s, if (i == 0) portalB else portalA, fog)
            }
            Worlds.RINGS -> for (i in 0..3) {
                val side = if (i % 2 == 0) -1f else 1f
                val y = if (i < 2) 3.72f else .52f
                game.worldBoxSpin(side * 1.88f * s, y * s, z - .22f * s, .28f * s, .28f * s, .28f * s,
                    time * 48f + i * 90f, Color.WHITE, fog)
            }
            Worlds.RAILWAY -> {
                for (i in 0..2) {
                    val lamp = when (i) { 0 -> trafficRed; 1 -> trafficLamp; else -> signalGreen }
                    game.worldBox(1.98f * s, (3.44f - i * .35f) * s, z - .29f * s, .20f * s, .16f * s, .05f * s, lamp, fog)
                }
                game.worldBox(0f, 3.62f * s, z - .24f * s, 3.45f * s, .09f * s, .08f * s, trainStripe, fog)
            }
            Worlds.JUNGLE -> for (side in -1..1 step 2) for (i in 0..2) {
                val x = side * (2.08f - i * .10f) * s
                val y = (1.05f + i * .83f) * s
                game.worldBoxSpin(x, y, z - .25f * s, .36f * s, .18f * s, .30f * s,
                    side * (18f + time * 9f), if (i % 2 == 0) jungleLeaf else jungleMoss, fog)
            }
            Worlds.BEACH -> for (i in 0..2) {
                val y = (.80f + i * .22f) * s
                game.worldBoxSpin(0f, y, z - .26f * s, 2.95f * s, .09f * s, .07f * s,
                    sin(time * 1.8f + i) * 8f, if (i % 2 == 0) portalB else Color.WHITE, fog)
            }
        }
    }

    /** A box scaled by the stream-in [p]: grounded ones grow up out of the floor, hanging ones swell in place. */
    private fun box(x: Float, cy: Float, z: Float, sx: Float, sy: Float, sz: Float, col: Color, fog: Float, p: Float, grounded: Boolean) {
        if (p >= 0.999f) { game.worldBox(x, cy, z, sx, sy, sz, col, fog); return }
        val h = sy * p
        val y = if (grounded) cy - sy / 2f + h / 2f else cy
        game.worldBox(x, y, z, sx * (0.4f + 0.6f * p), h, sz, col, fog)
    }

    private fun renderPlatform(ob: Ob, front: Float, fog: Float, p: Float) {
        val top = ob.clear * p
        val bodyLen = ob.sz - ob.ramp
        game.worldBox(ob.x, top / 2f, front - ob.ramp - bodyLen / 2f, ob.sx, top, bodyLen, ob.col, fog)
        if (ob.ramp > 0f) { // the ramp as 4 rising steps
            val n = 4
            val d = ob.ramp / n
            for (i in 0 until n) {
                val h = top * (i + 1) / n
                game.worldBox(ob.x, h / 2f, front - d * (i + 0.5f), ob.sx, h, d, ob.col, fog)
            }
        }
    }

    /** Queue only a bounded number of cosmetic pieces; collision geometry is never budgeted here. */
    private fun detailBox(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, color: Color, fog: Float) {
        val limit = if (Settings.graphicsPreset == Settings.GRAPHICS_LOW) 96 else 180
        if (detailBoxesQueued >= limit) return
        detailBoxesQueued++
        game.worldBox(x, y, z, sx, sy, sz, color, fog)
    }

    /** Biome art re-skins existing colliders only; spawn timing and hitboxes are untouched. */
    private fun renderWorldObstacle(ob: Ob, z: Float, fog: Float, p: Float, time: Float, source: Color) {
        if (ob.type != ObType.SOLID) {
            box(ob.x, ob.cy, z, ob.sx, ob.sy, ob.sz, source, fog, p, ob.grounded)
            return
        }
        when (ObstacleArtResolver.resolve(worldStyle, ob.type, ob.anim, ob.cue, ob.pit, ob.sx, ob.sy, ob.nightAnimal, ob.airBird)) {
            ObstacleArt.AIR_EAGLE -> renderAirBird(ob, z, fog, p, time, AirBird.EAGLE)
            ObstacleArt.AIR_HAWK -> renderAirBird(ob, z, fog, p, time, AirBird.HAWK)
            ObstacleArt.AIR_GULL -> renderAirBird(ob, z, fog, p, time, AirBird.GULL)
            ObstacleArt.AIR_FLOCK -> renderAirBird(ob, z, fog, p, time, AirBird.FLOCK)
            ObstacleArt.NIGHT_WOLF -> renderNightAnimal(ob, z, fog, p, time, RoadAnimal.WOLF)
            ObstacleArt.NIGHT_TIGER -> renderNightAnimal(ob, z, fog, p, time, RoadAnimal.TIGER)
            ObstacleArt.NIGHT_SHADOW_GAP -> renderNightShadowGap(ob, z, fog, p)
            ObstacleArt.HACKER_GLITCH_BLOCK -> renderHackerGlitchBlock(ob, z, fog, p, time)
            ObstacleArt.HACKER_FIREWALL -> renderHackerFirewall(ob, z, fog, p, time)
            ObstacleArt.HACKER_DRONE -> renderHackerDrone(ob, z, fog, p, time)
            ObstacleArt.HACKER_DATA_GATE -> renderHackerDataGate(ob, z, fog, p, time)
            ObstacleArt.HACKER_VOID -> renderHackerVoid(ob, z, fog, p)
            ObstacleArt.HACKER_PLATFORM -> renderHackerPlatform(ob, z, fog, p)
            ObstacleArt.HACKER_PAD -> renderHackerPad(ob, z, fog, p, time)
            ObstacleArt.JUNGLE_FALLEN_LOG -> renderJungleLog(ob, z, fog, p, time)
            ObstacleArt.JUNGLE_VINE_GATE -> renderJungleVineGate(ob, z, fog, p, time)
            ObstacleArt.JUNGLE_VINE_SWEEP -> renderJungleVineSweep(ob, z, fog, p, time)
            ObstacleArt.JUNGLE_SNAP_PLANT -> renderJungleSnapPlant(ob, z, fog, p, time)
            ObstacleArt.JUNGLE_FALLING_POD -> renderJungleFallingPod(ob, z, fog, p, time)
            ObstacleArt.JUNGLE_THORN_BUSH -> renderJungleThornBush(ob, z, fog, p, time)
            ObstacleArt.JUNGLE_DINO -> renderJungleDinoHazard(ob, z, fog, p, time)
            ObstacleArt.LAVA_BOULDER -> renderLavaBoulder(ob, z, fog, p, time)
            ObstacleArt.LAVA_VENT -> renderLavaVent(ob, z, fog, p, time)
            ObstacleArt.LAVA_CRUST -> renderLavaBoulder(ob, z, fog, p, time)
            ObstacleArt.NEXUS_GLITCH_BLOCK -> renderNexusBlock(ob, z, fog, p, time)
            ObstacleArt.NEXUS_FIREWALL -> renderNexusFirewall(ob, z, fog, p, time)
            ObstacleArt.NEXUS_DRONE -> renderNexusDrone(ob, z, fog, p, time)
            ObstacleArt.BEACH_CRAB -> renderBeachCrab(ob, z, fog, p, time)
            ObstacleArt.BEACH_SURF -> renderBeachSurf(ob, z, fog, p, time)
            ObstacleArt.BEACH_GEYSER -> renderBeachGeyser(ob, z, fog, p, time)
            ObstacleArt.BEACH_DRIFTWOOD -> renderBeachDriftwood(ob, z, fog, p, time)
            ObstacleArt.BEACH_TIDAL_GAP -> renderBeachTidalGap(ob, z, fog, p)
            ObstacleArt.BEACH_SHELL -> renderBeachShell(ob, z, fog, p, time)
            ObstacleArt.SPACE_METEOR -> renderSpaceMeteor(ob, z, fog, p, time)
            ObstacleArt.SPACE_GRAVITY_MINE -> renderSpaceGravityMine(ob, z, fog, p, time)
            ObstacleArt.SPACE_CRYSTAL_WALL -> renderSpaceCrystalWall(ob, z, fog, p, time)
            ObstacleArt.SPACE_ENERGY_WALL -> renderSpaceEnergyWall(ob, z, fog, p, time)
            ObstacleArt.SPACE_LASER -> renderSpaceLaser(ob, z, fog, p, time)
            ObstacleArt.SPACE_SOLAR_ARRAY -> renderSpaceSolarArray(ob, z, fog, p, time)
            ObstacleArt.SPACE_PLASMA_SPIRE -> renderSpacePlasmaSpire(ob, z, fog, p, time)
            ObstacleArt.SPACE_DROP_POD -> renderSpaceDropPod(ob, z, fog, p, time)
            ObstacleArt.SPACE_STATION_HULL -> renderSpaceHull(ob, z, fog, p)
            ObstacleArt.SPACE_ASTEROID -> renderSpaceAsteroid(ob, z, fog, p, time)
            ObstacleArt.CITY_CAR -> renderCityCar(ob, z, fog, p)
            ObstacleArt.CITY_BUS -> renderCityBus(ob, z, fog, p)
            ObstacleArt.CITY_BIKE -> renderCityBike(ob, z, fog, p, time)
            ObstacleArt.CITY_BARRIER -> renderCityBarrier(ob, z, fog, p)
            ObstacleArt.CITY_SIGN -> renderCitySign(ob, z, fog, p)
            ObstacleArt.CITY_BOLLARD -> renderCityBollard(ob, z, fog, p)
            ObstacleArt.WINTER_SNOWBALL -> renderWinterSnowball(ob, z, fog, p, time)
            ObstacleArt.WINTER_SNOWBANK -> renderWinterSnowbank(ob, z, fog, p)
            ObstacleArt.WINTER_ICICLE -> renderWinterIcicle(ob, z, fog, p)
            ObstacleArt.WINTER_ICE_SPIKE -> renderWinterIceSpike(ob, z, fog, p)
            else -> when (worldStyle) {
                Worlds.RAILWAY -> renderRailwayTrain(ob, z, fog, p)
                Worlds.HIGHWAY -> if (ob.cue == ObCue.JUMP) renderHighwayBarrier(ob, z, fog, p)
                    else renderTraffic(ob, z, fog, p)
                else -> box(ob.x, ob.cy, z, ob.sx, ob.sy, ob.sz, source, fog, p, ob.grounded)
            }
        }
    }

    private fun renderRailwayTrain(ob: Ob, z: Float, fog: Float, p: Float) {
        box(ob.x, ob.cy, z, ob.sx, ob.sy, ob.sz, trainBody, fog, p, ob.grounded)
        if (p < .56f || fog > .84f || ob.sy < .16f || ob.sz < .28f) return
        val width = ob.sx * (.4f + .6f * p)
        val height = ob.sy * p
        val centreY = if (ob.grounded) ob.bottom + height / 2f else ob.cy
        val faceZ = z + ob.sz / 2f + .035f
        val count = (width / 1.15f).toInt().coerceIn(1, 4)
        val windowW = (width / count * .48f).coerceAtLeast(.09f)
        val windowH = (height * .16f).coerceIn(.08f, .25f)
        for (i in 0 until count) {
            val x = ob.x - width / 2f + width * (i + .5f) / count
            detailBox(x, centreY + height * .13f, faceZ, windowW, windowH, .035f, trainWindow, fog)
        }
        detailBox(ob.x, centreY - height * .30f, faceZ, width * .92f, .07f, .04f, trainStripe, fog)
        if (width < 3.2f) for (side in -1..1 step 2) {
            val wheelX = ob.x + side * width * .36f
            detailBox(wheelX, centreY - height / 2f + .11f, z + ob.sz * .22f,
                .20f, .18f, .16f, trainWheel, fog)
        }
        detailBox(ob.x, centreY + height * .43f, faceZ, .20f, .10f, .035f, trainStripe, fog)
    }

    private fun renderTraffic(ob: Ob, z: Float, fog: Float, p: Float) {
        box(ob.x, ob.cy, z, ob.sx, ob.sy, ob.sz, trafficBody, fog, p, ob.grounded)
        if (p < .58f || fog > .84f || ob.sy < .22f || ob.sz < .3f) return
        val width = ob.sx * (.4f + .6f * p)
        val height = ob.sy * p
        val centreY = if (ob.grounded) ob.bottom + height / 2f else ob.cy
        val faceZ = z + ob.sz / 2f + .035f
        val glassH = (height * .20f).coerceIn(.08f, .28f)
        detailBox(ob.x, centreY + height * .16f, faceZ, width * .54f, glassH, .04f, trafficGlass, fog)
        val lampY = centreY - height * .18f
        for (side in -1..1 step 2) {
            detailBox(ob.x + side * width * .34f, lampY, faceZ + .006f,
                (width * .10f).coerceAtLeast(.08f), .09f, .035f, trafficLamp, fog)
            detailBox(ob.x + side * width * .35f, centreY - height * .39f, z + ob.sz * .18f,
                .18f, .20f, .17f, barrierBlack, fog)
        }
        detailBox(ob.x, centreY - height * .34f, faceZ + .012f, width * .72f, .055f, .04f, trafficRed, fog)
    }

    /** Cars are full-width batched boxes with readable glass, lamps, and wheels; hitboxes stay authored. */
    private fun renderCityCar(ob: Ob, z: Float, fog: Float, p: Float) {
        val width = ob.sx * (.4f + .6f * p)
        val height = ob.sy * p
        if (height <= .03f) return
        val bodyH = (height * .54f).coerceAtLeast(.12f)
        val bodyY = if (ob.grounded) ob.bottom + bodyH / 2f else ob.cy
        val body = when (((ob.sx * 7f).toInt() + kotlin.math.abs(ob.x * 4f).toInt()) % 3) {
            0 -> cityCarRed
            1 -> cityCarBlue
            else -> cityCarGold
        }
        game.worldBoxSpin(ob.x, bodyY, z, width * .88f, bodyH, ob.sz * .82f, 0f, body, fog)
        if (p < .48f || fog > .84f) return
        val cabinH = (height * .23f).coerceAtLeast(.08f)
        val cabinY = bodyY + bodyH * .44f + cabinH * .5f
        game.worldBoxSpin(ob.x, cabinY, z - ob.sz * .02f, width * .52f, cabinH, ob.sz * .56f, 0f, trafficGlass, fog)
        val wheelY = bodyY - bodyH * .34f
        for (side in -1..1 step 2) {
            detailBox(ob.x + side * width * .34f, wheelY, z + ob.sz * .22f,
                (width * .17f).coerceAtLeast(.12f), (height * .22f).coerceAtLeast(.12f), .18f, cityWheel, fog)
            detailBox(ob.x + side * width * .33f, bodyY - bodyH * .08f, z + ob.sz * .43f,
                (width * .09f).coerceAtLeast(.08f), .09f, .035f, trafficLamp, fog)
        }
        detailBox(ob.x, bodyY - bodyH * .28f, z + ob.sz * .43f, width * .65f, .055f, .035f, trafficRed, fog)
    }

    /** Longer van/bus silhouette for the heavier traffic steps. */
    private fun renderCityBus(ob: Ob, z: Float, fog: Float, p: Float) {
        renderCityCar(ob, z, fog, p)
        if (p < .5f || fog > .84f) return
        val width = ob.sx * (.4f + .6f * p)
        val height = ob.sy * p
        val centreY = visualCentreY(ob, p)
        val faceZ = z + ob.sz * .43f
        val count = (width / 1.05f).toInt().coerceIn(2, 6)
        val windowW = width / count * .42f
        for (i in 0 until count) {
            detailBox(ob.x - width * .40f + width * (i + .5f) / count,
                centreY + height * .12f, faceZ, windowW, (height * .16f).coerceAtLeast(.08f), .035f,
                if (i % 2 == 0) trafficGlass else citySign, fog)
        }
    }

    /** Two wheels and a bright frame make the moving pendulum/sweeper read as a motorbike. */
    private fun renderCityBike(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val width = ob.sx * (.4f + .6f * p)
        val height = ob.sy * p
        if (height <= .03f) return
        val centreY = visualCentreY(ob, p)
        val wheelY = centreY - height * .25f
        val wheel = (width * .16f).coerceIn(.10f, .24f)
        for (side in -1..1 step 2) {
            detailBox(ob.x + side * width * .32f, wheelY, z + ob.sz * .22f,
                wheel * 2f, wheel * 2f, .14f, cityWheel, fog)
        }
        game.worldBoxSpin(ob.x, centreY, z, width * .58f, (height * .42f).coerceAtLeast(.12f), ob.sz * .5f,
            4f * sin(time * 3f + ob.phase), cityBikeFrame, fog)
        detailBox(ob.x, centreY + height * .20f, z + ob.sz * .18f, width * .24f, .14f, .17f, trafficBody, fog)
        detailBox(ob.x + width * .12f, centreY + height * .35f, z + ob.sz * .20f,
            .20f, .18f, .18f, trafficLamp, fog)
    }

    private fun renderCityBarrier(ob: Ob, z: Float, fog: Float, p: Float) {
        box(ob.x, ob.cy, z, ob.sx, ob.sy, ob.sz, barrierBlack, fog, p, ob.grounded)
        if (p < .48f || fog > .84f) return
        val width = ob.sx * (.4f + .6f * p)
        val height = ob.sy * p
        val centreY = visualCentreY(ob, p)
        val faceZ = z + ob.sz / 2f + .035f
        val bands = (width / .55f).toInt().coerceIn(2, 12)
        val bandW = width / bands
        for (i in 0 until bands) {
            detailBox(ob.x - width / 2f + bandW * (i + .5f), centreY, faceZ,
                bandW * .78f, height * .78f, .04f, if (i % 2 == 0) cityBarrierRed else cityBarrierWhite, fog)
        }
    }

    private fun renderCitySign(ob: Ob, z: Float, fog: Float, p: Float) {
        box(ob.x, ob.cy, z, ob.sx, ob.sy, ob.sz, trafficBody, fog, p, false)
        if (p < .48f || fog > .84f) return
        val width = ob.sx * (.4f + .6f * p)
        val height = ob.sy * p
        val centreY = visualCentreY(ob, p)
        val faceZ = z + ob.sz / 2f + .04f
        detailBox(ob.x, centreY, faceZ, width * .76f, height * .5f, .05f, citySign, fog)
        for (i in -1..1) detailBox(ob.x + i * width * .18f, centreY, faceZ + .03f,
            .11f, .10f, .025f, if (i == 0) cityCarGold else cityBarrierWhite, fog)
    }

    private fun renderCityBollard(ob: Ob, z: Float, fog: Float, p: Float) {
        val height = ob.sy * p
        if (height <= .03f) return
        val centreY = visualCentreY(ob, p)
        game.worldBox(ob.x, ob.bottom + height * .48f, z, .42f, height * .96f, .42f, trafficBody, fog)
        if (p > .45f && fog < .84f) for (i in 0..1) {
            detailBox(ob.x, ob.bottom + height * (.34f + i * .36f), z + .22f,
                .40f, .10f, .035f, if (i == 0) cityBarrierRed else cityBarrierWhite, fog)
        }
    }

    private fun renderCityPlatform(ob: Ob, front: Float, fog: Float, p: Float) {
        renderPlatform(ob, front, fog, p)
        if (p > .5f && fog < .86f) {
            detailBox(ob.x, ob.clear * p + .025f, front - ob.sz * .48f, ob.sx * .8f, .045f, ob.sz * .42f, cityBarrierWhite, fog)
        }
    }

    private fun renderCityPad(ob: Ob, z: Float, fog: Float, time: Float, p: Float) {
        renderPad(ob, z, fog, time, p)
        if (p > .52f && fog < .84f) detailBox(ob.x, .20f, z + ob.sz * .20f, ob.sx * .60f, .045f, .04f, cityCarGold, fog)
    }

    private fun renderCityPit(ob: Ob, z: Float, fog: Float, p: Float) {
        val color = if (ob.type == ObType.DECO) cityBarrierWhite else barrierBlack
        game.worldBox(ob.x, if (ob.type == ObType.DECO) -.01f else -.10f, z,
            ob.sx * p, if (ob.type == ObType.DECO) .06f else .26f, ob.sz, color, fog)
        if (ob.type == ObType.SOLID && p > .45f && fog < .84f) {
            detailBox(ob.x, .025f, z + ob.sz * .38f, ob.sx * .78f, .035f, .04f, trafficLamp, fog)
        }
    }

    private fun renderWinterSnowball(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val size = (.48f * p).coerceAtLeast(.04f)
        game.facets.add(jungleBushShape, ob.x, visualCentreY(ob, p), z, size, size * .9f, size,
            time * 8f + ob.phase * 12f, 0f, time * 5f, winterSnowPalette, fog, game.fogColor, glow = .08f)
        if (p > .55f && fog < .84f) detailBox(ob.x, visualCentreY(ob, p) + size * .35f, z + ob.sz * .40f,
            size * .58f, .055f, .035f, Color.WHITE, fog)
    }

    private fun renderWinterSnowbank(ob: Ob, z: Float, fog: Float, p: Float) {
        val width = ob.sx * (.4f + .6f * p)
        val height = ob.sy * p
        val centreY = visualCentreY(ob, p)
        game.worldBox(ob.x, centreY, z, width, height, ob.sz, winterIce, fog)
        if (p > .45f && fog < .86f) {
            detailBox(ob.x, centreY + height * .31f, z + ob.sz * .06f, width * .92f, (height * .26f).coerceAtLeast(.08f), ob.sz * .82f, winterSnow, fog)
            for (side in -1..1 step 2) detailBox(ob.x + side * width * .36f, centreY - height * .12f,
                z + ob.sz * .48f, width * .13f, .06f, .035f, Color.WHITE, fog)
        }
    }

    private fun renderWinterIcicle(ob: Ob, z: Float, fog: Float, p: Float) {
        val width = ob.sx * (.4f + .6f * p)
        val height = ob.sy * p
        val centreY = visualCentreY(ob, p)
        game.worldBox(ob.x, centreY, z, width, height, ob.sz, winterDeep, fog)
        if (p < .48f || fog > .84f) return
        if (ob.cue == ObCue.DUCK) {
            val drop = (ob.bottom - .12f).coerceIn(.08f, .32f)
            val count = (width / 1.2f).toInt().coerceIn(2, 7)
            for (i in 0 until count) {
                val x = ob.x - width * .42f + width * (i + .5f) / count
                detailBox(x, ob.bottom - drop * .5f, z + ob.sz * .34f, .15f, drop, .13f,
                    if (i % 2 == 0) winterIce else winterSnow, fog)
            }
        } else {
            val tipY = ob.bottom + height * .14f
            detailBox(ob.x, tipY, z + ob.sz * .34f, width * .25f, (height * .28f).coerceAtLeast(.08f), .12f, winterSnow, fog)
        }
    }

    private fun renderWinterIceSpike(ob: Ob, z: Float, fog: Float, p: Float) {
        val height = ob.sy * p
        if (height <= .03f) return
        game.worldBox(ob.x, ob.bottom + height * .5f, z, .42f, height, .42f, winterIce, fog)
        if (p > .45f && fog < .84f) {
            detailBox(ob.x, ob.bottom + height * .8f, z + .22f, .22f, .10f, .035f, winterSnow, fog)
        }
    }

    private fun renderWinterPlatform(ob: Ob, front: Float, fog: Float, p: Float) {
        val top = ob.clear * p
        val bodyLen = ob.sz - ob.ramp
        game.worldBox(ob.x, top / 2f, front - ob.ramp - bodyLen / 2f, ob.sx, top, bodyLen, winterIce, fog)
        if (ob.ramp > 0f) {
            val n = 4; val d = ob.ramp / n
            for (i in 0 until n) game.worldBox(ob.x, top * (i + 1) / n / 2f, front - d * (i + .5f), ob.sx, top * (i + 1) / n, d, winterIce, fog)
        }
        if (p > .5f && fog < .86f) detailBox(ob.x, top + .025f, front - ob.sz * .47f, ob.sx * .75f, .045f, ob.sz * .4f, winterSnow, fog)
    }

    private fun renderWinterPad(ob: Ob, z: Float, fog: Float, time: Float, p: Float) {
        val breath = .025f * sin(time * 5f)
        game.worldBox(ob.x, .06f, z, ob.sx * (.45f + .55f * p), .12f, ob.sz * .72f, winterIce, fog)
        game.worldBox(ob.x, .15f + breath, z, ob.sx * .78f * p, .08f, ob.sz * .68f * p, winterSnow, fog)
        if (p > .55f && fog < .84f) detailBox(ob.x, .20f + breath, z + ob.sz * .15f, ob.sx * .4f, .035f, .04f, Color.WHITE, fog)
    }

    private fun renderWinterPit(ob: Ob, z: Float, fog: Float, p: Float) {
        if (ob.type == ObType.DECO) game.worldBox(ob.x, -.01f, z, ob.sx * p, .06f, ob.sz, winterSnow, fog)
        else {
            game.worldBox(ob.x, -.10f, z, ob.sx * p, .26f, ob.sz, winterDeep, fog)
            if (p > .45f && fog < .84f) detailBox(ob.x, .02f, z + ob.sz * .36f, ob.sx * .72f, .035f, .04f, winterIce, fog)
        }
    }

    private fun renderHighwayBarrier(ob: Ob, z: Float, fog: Float, p: Float) {
        box(ob.x, ob.cy, z, ob.sx, ob.sy, ob.sz, barrierBlack, fog, p, ob.grounded)
        if (p < .48f || fog > .84f) return
        val width = ob.sx * (.4f + .6f * p)
        val height = ob.sy * p
        val centreY = if (ob.grounded) ob.bottom + height / 2f else ob.cy
        val faceZ = z + ob.sz / 2f + .035f
        val bands = (width / .52f).toInt().coerceIn(2, 12)
        val bandW = width / bands
        for (i in 0 until bands step 2) {
            detailBox(ob.x - width / 2f + bandW * (i + .5f), centreY, faceZ,
                bandW * .84f, height * .78f, .04f, barrierYellow, fog)
        }
        detailBox(ob.x, centreY + height * .46f, faceZ, width, .055f, .04f, trafficLamp, fog)
    }

    private fun visualCentreY(ob: Ob, p: Float): Float {
        val h = ob.sy * p
        return if (ob.grounded) ob.bottom + h / 2f else ob.cy
    }

    /** Rounded thorn thickets replace the old lane-by-lane cubes; thorns sit inside the unchanged hitbox. */
    private fun renderJungleThornBush(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val h = ob.sy * p
        val halfW = minOf(ob.halfW * .88f, ob.sx * (.4f + .6f * p) * .46f).coerceAtLeast(.12f)
        val halfD = (ob.sz * .44f).coerceAtLeast(.12f)
        game.facets.add(jungleBushShape, ob.x, visualCentreY(ob, p), z, halfW, h * .48f, halfD,
            time * 8f + ob.phase * 12f, 0f, time * 3f, jungleBushPalette, fog, game.fogColor, glow = .03f)
        if (p < .48f || fog > .86f) return
        val centreY = visualCentreY(ob, p)
        val tipY = centreY + h * .34f
        for (i in -1..1) {
            val x = ob.x + i * halfW * .48f
            game.worldBoxSpin(x, tipY, z + halfD * .55f, .12f, (h * .30f).coerceAtLeast(.12f), .11f,
                i * 19f + sin(time * 1.7f + i) * 5f, jungleThorn, fog)
        }
        detailBox(ob.x, centreY - h * .08f, z + halfD * .60f, halfW * 1.35f, .08f, .035f, jungleLeaf, fog)
    }

    /** Wide, low jump hazards read as fallen logs with bark rings and a moss seam. */
    private fun renderJungleLog(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val width = ob.sx * (.4f + .6f * p)
        val h = (ob.sy * p).coerceAtLeast(.12f)
        val cy = visualCentreY(ob, p)
        val bodyH = maxOf(.30f, h * .72f)
        game.worldBoxSpin(ob.x, cy, z, width, bodyH, ob.sz * .78f, 2.5f * sin(time * 1.4f), jungleBark, fog)
        if (p < .48f || fog > .86f) return
        val bands = (width / 1.35f).toInt().coerceIn(2, 7)
        val bandW = width / bands
        for (i in 0 until bands) {
            val x = ob.x - width / 2f + bandW * (i + .5f)
            detailBox(x, cy, z + ob.sz * .40f, bandW * .12f, bodyH * .88f, .035f, jungleMoss, fog)
        }
        detailBox(ob.x, cy + bodyH * .31f, z + ob.sz * .42f, width * .72f, .07f, .035f, jungleLeaf, fog)
    }

    /** Roll-under hazards are leafy, sagging vine curtains rather than another horizontal block. */
    private fun renderJungleVineGate(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val width = ob.sx * (.4f + .6f * p)
        val cy = visualCentreY(ob, p)
        game.worldBoxSpin(ob.x, cy, z, width, ob.sy * p * .62f, ob.sz * .68f,
            2f * sin(time * 1.2f), jungleMoss, fog)
        if (p < .48f || fog > .86f) return
        val count = (width / 1.15f).toInt().coerceIn(2, 7)
        for (i in 0 until count) {
            val x = ob.x - width * .42f + width * (i + .5f) / count
            val sway = sin(time * 2.4f + i * 1.7f) * .08f
            val drop = .18f + .13f * (i % 3)
            detailBox(x + sway, cy - drop, z + ob.sz * .38f, .10f, drop * 2f, .08f, jungleLeaf, fog)
            detailBox(x + sway, cy - drop * 1.85f, z + ob.sz * .39f, .22f, .12f, .16f,
                if (i % 2 == 0) jungleMoss else jungleLeaf, fog)
        }
    }

    /** The sweep is a live vine lash with leaves, following the authored sideways motion. */
    private fun renderJungleVineSweep(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val height = ob.sy * p
        game.worldBoxSpin(ob.x, visualCentreY(ob, p), z, ob.sx * (.4f + .6f * p), height, ob.sz,
            8f * sin(time * 2f + ob.phase), jungleMoss, fog)
        if (p < .52f || fog > .84f) return
        for (i in -1..1) {
            val x = ob.x + i * ob.sx * .34f
            detailBox(x, ob.cy + .22f, z + ob.sz * .56f, .24f, .11f, .06f, jungleLeaf, fog)
        }
    }

    /** A timed piston becomes a snapping carnivorous flower, still on the exact old piston collider. */
    private fun renderJungleSnapPlant(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val h = ob.sy * p
        if (h < .04f) return
        val cy = visualCentreY(ob, p)
        val stemH = maxOf(.08f, h * .62f)
        game.worldBox(ob.x, ob.bottom + stemH / 2f, z, .22f, stemH, .22f, jungleBark, fog)
        game.facets.add(jungleBushShape, ob.x, ob.bottom + h * .79f, z, .48f, h * .22f, .42f,
            time * 18f, 0f, time * 12f, jungleFruitPalette, fog, game.fogColor, glow = .08f)
        if (p > .54f && fog < .84f) {
            for (side in -1..1 step 2) detailBox(ob.x + side * .30f, ob.bottom + h * .85f, z + .31f,
                .24f, .08f, .06f, jungleLeaf, fog)
        }
    }

    /** Slamming blocks are rendered as low-poly jungle fruit pods, not square crushers. */
    private fun renderJungleFallingPod(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val h = ob.sy * p
        val cy = visualCentreY(ob, p)
        game.facets.add(jungleMushroomShape, ob.x, cy, z, .48f * p, .48f * p, .43f * p,
            time * 20f, 9f, time * 13f, jungleFruitPalette, fog, game.fogColor, glow = .08f)
        if (p > .56f && fog < .84f) {
            detailBox(ob.x, cy + h * .40f, z, .32f, .10f, .18f, jungleLeaf, fog)
        }
    }

    /** A block-built dinosaur crossing uses the existing solid collider and never changes its safe lane. */
    private fun renderJungleDinoHazard(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        if (ob.sy <= .02f || p <= .01f) return
        val width = ob.sx * (.4f + .6f * p)
        val scale = (width / 2.3f).coerceIn(.48f, 1.35f) * p
        val cy = visualCentreY(ob, p)
        val bodyY = if (ob.grounded) ob.bottom + .48f * scale else cy
        val stride = sin(time * 7.2f + ob.phase)
        game.worldBoxSpin(ob.x, bodyY, z, width * .78f, .62f * scale, ob.sz * .72f,
            stride * 5f, jungleDinoSkin, fog)
        game.worldBox(ob.x + .34f * scale, bodyY + .32f * scale, z + .03f * scale,
            .42f * scale, .53f * scale, .42f * scale, jungleDinoSkin, fog)
        game.worldBox(ob.x + .55f * scale, bodyY + .42f * scale, z + .16f * scale,
            .34f * scale, .22f * scale, .34f * scale, jungleDinoBelly, fog)
        game.worldBox(ob.x - width * .42f, bodyY + .06f * scale, z - .30f * scale,
            width * .34f, .16f * scale, .17f * scale, jungleDinoSkin, fog)
        for (leg in 0..1) {
            val side = if (leg == 0) -1f else 1f
            val lift = maxOf(0f, stride * side) * .12f * scale
            game.worldBoxSpin(ob.x + side * .25f * scale, bodyY - .32f * scale + lift, z,
                .18f * scale, .45f * scale, .20f * scale, -stride * side * 14f, jungleDinoSkin, fog)
        }
        if (p > .48f && fog < .88f) {
            detailBox(ob.x + .47f * scale, bodyY + .51f * scale, z + ob.sz * .42f,
                .055f * scale, .055f * scale, .035f, jungleFruit, fog)
            for (i in 0..2) detailBox(ob.x - .18f * scale + i * .18f * scale,
                bodyY + .53f * scale, z - .02f * scale, .11f * scale, .16f * scale, .10f * scale,
                jungleLeaf, fog)
        }
    }

    /** A blocky shore crab gives the map a moving, recognizable encounter without a new collider. */
    private fun renderBeachCrab(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val scale = (.55f + .45f * p).coerceAtLeast(.25f)
        val cy = visualCentreY(ob, p)
        val bob = .08f * sin(time * 6f + ob.phase)
        game.worldBoxSpin(ob.x, cy + bob, z, ob.sx * .64f, ob.sy * .50f * p, ob.sz * .70f,
            4f * sin(time * 3f + ob.phase), beachCrab, fog)
        game.worldBox(ob.x, cy + bob + ob.sy * .22f * scale, z + ob.sz * .36f,
            ob.sx * .32f, ob.sy * .16f * scale, .08f, beachCrabLight, fog)
        for (side in -1..1 step 2) {
            val clawX = ob.x + side * ob.sx * .43f * scale
            game.worldBoxSpin(clawX, cy + .17f * scale, z + .06f, .25f * scale, .18f * scale, .19f * scale,
                side * (18f + 5f * sin(time * 5f + ob.phase)), beachCrabLight, fog)
            for (leg in 0..1) {
                val lx = ob.x + side * ob.sx * (.30f + leg * .17f) * scale
                game.worldBoxSpin(lx, cy - .20f * scale, z + (leg - .5f) * .22f,
                    .075f * scale, .28f * scale, .07f * scale, side * 22f, beachCrab, fog)
            }
            if (p > .5f && fog < .86f) detailBox(ob.x + side * .13f * scale, cy + .13f * scale,
                z + ob.sz * .43f, .045f * scale, .045f * scale, .03f, beachFoam, fog)
        }
    }

    private fun renderBeachSurf(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val width = ob.sx * (.4f + .6f * p)
        val height = ob.sy * p
        val cy = visualCentreY(ob, p)
        game.worldBoxSpin(ob.x, cy, z, width, height * .58f, ob.sz * .72f,
            3f * sin(time * 1.6f + ob.phase), beachWater, fog)
        if (p < .45f || fog > .88f) return
        val crest = cy + height * .27f + .07f * sin(time * 2.2f + ob.phase)
        detailBox(ob.x, crest, z + ob.sz * .40f, width * .88f, .11f, .05f, beachFoam, fog)
        detailBox(ob.x - width * .22f, crest + .12f, z + ob.sz * .39f, width * .36f, .07f, .045f, beachFoam, fog)
    }

    private fun renderBeachGeyser(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val h = (ob.sy * p).coerceAtLeast(.03f)
        val cy = ob.bottom + h * .46f
        game.worldBox(ob.x, cy, z, ob.sx * .62f, h * .92f, ob.sz * .70f, beachWater, fog)
        if (p < .46f || fog > .88f) return
        val pulse = .07f + .035f * sin(time * 8f + ob.phase)
        detailBox(ob.x, ob.bottom + h * .74f, z + ob.sz * .40f,
            ob.sx * .48f, pulse, .045f, beachFoam, fog)
        for (side in -1..1 step 2) detailBox(ob.x + side * ob.sx * .30f, ob.bottom + .12f, z + ob.sz * .40f,
            .09f, .22f, .045f, beachShell, fog)
    }

    private fun renderBeachDriftwood(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val width = ob.sx * (.4f + .6f * p)
        val height = ob.sy * p
        val cy = visualCentreY(ob, p)
        game.worldBoxSpin(ob.x, cy, z, width, height * .62f, ob.sz * .72f,
            2.5f * sin(time * 1.1f + ob.phase), beachBoard, fog)
        if (p > .48f && fog < .88f) {
            for (i in -1..1) detailBox(ob.x + i * width * .27f, cy + height * .35f, z + ob.sz * .39f,
                .10f, .08f, .05f, beachShell, fog)
        }
    }

    private fun renderBeachTidalGap(ob: Ob, z: Float, fog: Float, p: Float) {
        game.worldBox(ob.x, -.12f, z, ob.sx * p, .25f, ob.sz * p, beachWater, fog)
        if (p > .4f && fog < .90f) {
            game.worldBox(ob.x, -.015f, z + ob.sz * .39f, ob.sx * .82f, .045f, .04f, beachFoam, fog)
            for (side in -1..1 step 2) detailBox(ob.x + side * ob.sx * .43f, .02f, z,
                .07f, .08f, ob.sz * .82f, beachShell, fog)
        }
    }

    private fun renderBeachBoardwalk(ob: Ob, front: Float, fog: Float, p: Float) {
        renderPlatform(ob, front, fog, p)
        if (p > .44f && fog < .88f) for (i in -1..1) {
            val x = ob.x + i * ob.sx * .30f
            detailBox(x, ob.clear * p + .035f, front - ob.sz * .38f,
                .07f, .045f, ob.sz * .78f, if (i == 0) beachShell else beachBoard, fog)
        }
    }

    private fun renderBeachShellPad(ob: Ob, z: Float, fog: Float, time: Float, p: Float) {
        renderPad(ob, z, fog, time, p)
        if (p > .43f && fog < .88f) {
            detailBox(ob.x, .24f + .025f * sin(time * 5f), z + ob.sz * .15f,
                ob.sx * .55f, .06f, .045f, beachShell, fog)
            for (side in -1..1 step 2) detailBox(ob.x + side * ob.sx * .30f, .16f,
                z + ob.sz * .14f, .07f, .09f, .045f, beachFoam, fog)
        }
    }

    private fun renderBeachShell(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val width = ob.sx * (.4f + .6f * p)
        val height = ob.sy * p
        val cy = visualCentreY(ob, p)
        game.worldBoxSpin(ob.x, cy, z, width * .88f, height * .80f, ob.sz * .70f,
            3f * sin(time * 1.3f + ob.phase), beachShell, fog)
        if (p > .5f && fog < .88f) {
            val front = z + ob.sz * .38f
            for (i in -1..1) detailBox(ob.x + i * width * .18f, cy, front,
                .045f, height * .42f, .04f, beachFoam, fog)
        }
    }

    /** Tumbling basalt keeps the cave's hitbox but exposes a bright molten seam. */
    private fun renderLavaBoulder(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val width = ob.sx * (.4f + .6f * p)
        val height = ob.sy * p
        val cy = visualCentreY(ob, p)
        game.worldBoxSpin(ob.x, cy, z, width, height, ob.sz * .82f,
            9f * sin(time * 1.7f + ob.phase), lavaBasalt, fog)
        if (p < .46f || fog > .88f) return
        val front = z + ob.sz * .43f
        val seam = .035f + .025f * (1f + sin(time * 6f + ob.phase))
        detailBox(ob.x, cy + height * .10f, front, width * .72f, seam, .045f, lavaGlow, fog)
        detailBox(ob.x - width * .22f, cy - height * .19f, front + .006f,
            width * .24f, seam * 1.25f, .04f, lavaHot, fog)
    }

    /** Erupting pistons become volcanic vents; animation/collider timing remains authored by Track. */
    private fun renderLavaVent(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val h = (ob.sy * p).coerceAtLeast(.025f)
        val width = ob.sx * (.4f + .6f * p)
        val base = ob.bottom
        game.worldBox(ob.x, base + h * .46f, z, width * .74f, h * .92f, ob.sz * .74f, lavaBasalt, fog)
        if (p < .46f || fog > .88f) return
        val front = z + ob.sz * .41f
        val pulse = .07f + .035f * sin(time * 10f + ob.phase)
        detailBox(ob.x, base + h * .77f, front, width * .52f, pulse, .045f, lavaHot, fog)
        for (side in -1..1 step 2) detailBox(ob.x + side * width * .31f, base + .12f,
            front, .10f, .23f, .045f, lavaGlow, fog)
    }

    private fun renderLavaGap(ob: Ob, z: Float, fog: Float, p: Float) {
        game.worldBox(ob.x, -.12f, z, ob.sx * p, .26f, ob.sz * p, lavaCrust, fog)
        if (p > .4f && fog < .9f) {
            game.worldBox(ob.x, -.035f, z + ob.sz * .38f, ob.sx * .72f, .035f, .045f, lavaGlow, fog)
            for (side in -1..1 step 2) detailBox(ob.x + side * ob.sx * .43f, .02f, z,
                .08f, .09f, ob.sz * .84f, lavaHot, fog)
        }
    }

    private fun renderLavaBridge(ob: Ob, front: Float, fog: Float, p: Float) {
        renderPlatform(ob, front, fog, p)
        if (p > .43f && fog < .88f) for (i in -1..1)
            detailBox(ob.x + i * ob.sx * .30f, ob.clear * p + .035f, front - ob.sz * .40f,
                .07f, .045f, ob.sz * .76f, if (i == 0) lavaHot else lavaGlow, fog)
    }

    private fun renderLavaPad(ob: Ob, z: Float, fog: Float, time: Float, p: Float) {
        renderPad(ob, z, fog, time, p)
        if (p > .42f && fog < .88f) detailBox(ob.x, .23f + .025f * sin(time * 7f), z + ob.sz * .15f,
            ob.sx * .58f, .055f, .04f, lavaHot, fog)
    }

    /** Corrupted blocks flicker between two safe-to-read code colours without touching collision. */
    private fun renderNexusBlock(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val width = ob.sx * (.4f + .6f * p)
        val height = ob.sy * p
        val cy = visualCentreY(ob, p)
        game.worldBoxSpin(ob.x, cy, z, width, height, ob.sz * .84f,
            2.5f * sin(time * 4f + ob.phase), nexusBody, fog)
        if (p < .42f || fog > .88f) return
        val n = (width / .42f).toInt().coerceIn(2, 10)
        for (i in 0 until n) {
            val x = ob.x - width * .45f + width * (i + .5f) / n
            val y = cy + sin(time * 8f + ob.phase + i * 1.3f) * height * .16f
            detailBox(x, y, z + ob.sz * .51f, .06f, .12f + .04f * (i % 2), .04f,
                if ((i + ob.phase.toInt()) % 5 == 0) nexusViolet else nexusCyan, fog)
        }
        detailBox(ob.x, cy + height * .38f, z + ob.sz * .52f, width * .88f, .045f, .04f, nexusMint, fog)
    }

    private fun renderNexusFirewall(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val width = ob.sx * (.4f + .6f * p)
        val height = ob.sy * p
        val cy = visualCentreY(ob, p)
        game.worldBox(ob.x, cy, z, width, height * .70f, ob.sz * .62f, nexusBody, fog)
        if (p < .44f || fog > .88f) return
        val bars = (width / .62f).toInt().coerceIn(2, 12)
        for (i in 0 until bars) {
            val x = ob.x - width * .47f + width * (i + .5f) / bars
            val lit = sin(time * 13f + i * 1.7f + ob.phase) > -.1f
            detailBox(x, cy, z + ob.sz * .37f, .10f, height * .62f, .045f,
                if (lit) nexusCyan else nexusViolet, fog)
        }
        detailBox(ob.x, cy + height * .43f, z + ob.sz * .38f, width, .055f, .045f, nexusMint, fog)
    }

    private fun renderNexusDrone(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val size = .42f * p
        val cy = visualCentreY(ob, p)
        game.worldBoxSpin(ob.x, cy, z, size, size, size, time * 85f + ob.phase, nexusBody, fog)
        for (side in -1..1 step 2) {
            game.worldBoxSpin(ob.x + side * .34f * p, cy, z, .46f * p, .065f * p, .08f * p,
                time * 110f * side, if (side < 0) nexusCyan else nexusViolet, fog)
            detailBox(ob.x + side * .32f * p, cy, z + .07f, .08f * p, .08f * p, .10f,
                nexusMint, fog)
        }
    }

    private fun renderNexusVoid(ob: Ob, z: Float, fog: Float, p: Float) {
        game.worldBox(ob.x, -.12f, z, ob.sx * p, .25f, ob.sz * p, nexusBody, fog)
        if (p > .4f && fog < .88f) {
            detailBox(ob.x, .014f, z + ob.sz * .41f, ob.sx * .88f, .045f, .04f, nexusCyan, fog)
            for (side in -1..1 step 2) detailBox(ob.x + side * ob.sx * .43f, .025f, z,
                .06f, .08f, ob.sz * .8f, nexusViolet, fog)
        }
    }

    private fun renderNexusPlatform(ob: Ob, front: Float, fog: Float, p: Float) {
        renderPlatform(ob, front, fog, p)
        if (p > .45f && fog < .88f) for (lane in -1..1)
            detailBox(ob.x + lane * ob.sx * .30f, ob.clear * p + .035f, front - ob.sz * .42f,
                .055f, .045f, ob.sz * .75f, if (lane == 0) nexusViolet else nexusMint, fog)
    }

    private fun renderNexusPad(ob: Ob, z: Float, fog: Float, time: Float, p: Float) {
        renderPad(ob, z, fog, time, p)
        if (p > .45f && fog < .88f) detailBox(ob.x, .22f, z + ob.sz * .15f,
            ob.sx * .62f, .055f, .04f, nexusCyan, fog)
    }

    /** Floating rocks are low-poly asteroid clumps. Their collider dimensions and walk remain unchanged. */
    private fun renderSpaceAsteroid(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val variant = (abs(ob.x * 3f).toInt() + ob.sx.toInt() + ob.sz.toInt()).mod(spaceRockShapes.size)
        val h = ob.sy * p
        game.facets.add(spaceRockShapes[variant], ob.x, visualCentreY(ob, p), z,
            minOf(ob.halfW * .88f, ob.sx * .43f).coerceAtLeast(.12f), (h * .46f).coerceAtLeast(.12f),
            (ob.sz * .48f).coerceAtLeast(.12f), time * (19f + variant * 5f), 7f + variant * 9f,
            time * (11f + variant * 3f), spaceRock, fog, game.fogColor, glow = .16f)
    }

    /** Small lane hazards become tumbling meteorites with a bright charged core. */
    private fun renderSpaceMeteor(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        renderSpaceAsteroid(ob, z, fog, p, time)
        if (p < .52f || fog > .86f) return
        val front = z + ob.sz * .40f
        detailBox(ob.x, visualCentreY(ob, p) + ob.sy * .10f, front, .24f, .11f, .05f, spaceWarning, fog)
        detailBox(ob.x - ob.sx * .22f, visualCentreY(ob, p), front + .01f, .12f, .08f, .05f, spacePanelLight, fog)
    }

    private fun renderSpaceCrystalWall(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val width = ob.sx * (.4f + .6f * p)
        val cy = visualCentreY(ob, p)
        game.worldBox(ob.x, cy, z, width, ob.sy * p, ob.sz * .66f, spacePanel, fog)
        if (p < .5f || fog > .86f) return
        val n = (width / 1.1f).toInt().coerceIn(2, 8)
        for (i in 0 until n) {
            val x = ob.x - width * .42f + width * (i + .5f) / n
            val phase = time * 2.5f + i * 1.4f
            detailBox(x, cy + .08f + sin(phase) * .09f, z + ob.sz * .38f,
                .12f, ob.sy * .68f, .05f, if (i % 2 == 0) spaceRingColors[0] else spaceRingColors[1], fog)
        }
    }

    /** An eagle, hawk, gull or flock of birds flies across the existing collider with animated block wings. */
    private fun renderAirBird(ob: Ob, z: Float, fog: Float, p: Float, time: Float, species: AirBird) {
        if (ob.sy <= .02f || p <= .01f) return
        val width = ob.sx * (.4f + .6f * p)
        val count = if (species == AirBird.FLOCK || width > 4.4f) 3 else if (width > 2.65f) 2 else 1
        val scale = (minOf(1f, ob.sx / (count * 1.26f)).coerceAtLeast(.44f) * p).coerceAtLeast(.05f)
        val centerY = visualCentreY(ob, p)
        val bodyZ = z + ob.sz * .08f
        val frontZ = z + ob.sz * .42f
        for (i in 0 until count) {
            val kind = if (species == AirBird.FLOCK) when (i % 3) {
                0 -> AirBird.GULL
                1 -> AirBird.HAWK
                else -> AirBird.EAGLE
            } else species
            val (body, wing, head) = when (kind) {
                AirBird.EAGLE -> Triple(airEagleBody, airEagleWing, airEagleHead)
                AirBird.HAWK -> Triple(airHawkBody, airHawkWing, airHawkHead)
                else -> Triple(airGullBody, airGullWing, airGullHead)
            }
            val x = ob.x + (i - (count - 1) * .5f) * 1.10f * scale
            val y = centerY + .035f * sin(time * 3f + ob.phase + i)
            val flap = sin(time * 8.4f + ob.phase + i * 1.7f)
            val wingY = y + .10f * scale + flap * .10f * scale
            game.worldBoxSpin(x - .30f * scale, wingY, bodyZ, .58f * scale, .11f * scale, .27f * scale,
                -17f + flap * 10f, wing, fog)
            game.worldBoxSpin(x + .30f * scale, wingY, bodyZ, .58f * scale, .11f * scale, .27f * scale,
                17f - flap * 10f, wing, fog)
            game.worldBox(x - .64f * scale, y + .12f * scale + flap * .15f * scale, bodyZ - .04f * scale,
                .34f * scale, .075f * scale, .19f * scale, wing, fog)
            game.worldBox(x + .64f * scale, y + .12f * scale + flap * .15f * scale, bodyZ - .04f * scale,
                .34f * scale, .075f * scale, .19f * scale, wing, fog)
            game.worldBox(x, y, bodyZ, .72f * scale, .30f * scale, .34f * scale, body, fog)
            game.worldBox(x + .31f * scale, y + .12f * scale, bodyZ + .08f * scale,
                .31f * scale, .27f * scale, .30f * scale, head, fog)
            game.worldBox(x + .55f * scale, y + .08f * scale, frontZ,
                .22f * scale, .11f * scale, .13f * scale, airBeak, fog)
            game.worldBox(x - .45f * scale, y + .02f * scale, bodyZ - .06f * scale,
                .34f * scale, .14f * scale, .22f * scale, wing, fog)
            if (p > .40f && fog < .90f) {
                detailBox(x + .39f * scale, y + .18f * scale, frontZ + .012f,
                    .055f * scale, .055f * scale, .035f * scale, airEye, fog)
                for (side in -1..1 step 2) detailBox(x + side * .18f * scale, y - .19f * scale, frontZ,
                    .055f * scale, .14f * scale, .055f * scale, airBeak, fog)
            }
        }
    }

    /** A cloud-hole lip keeps the jumpable gap visible without replacing its collision box. */
    private fun renderAirCloudGap(ob: Ob, z: Float, fog: Float, p: Float) {
        if (ob.type == ObType.DECO) {
            game.worldBox(ob.x, -.01f, z, ob.sx * p, .06f, ob.sz, airCloud, fog)
            return
        }
        game.worldBox(ob.x, -.10f, z, ob.sx * p, .24f, ob.sz, airCloudShade, fog)
        if (p > .42f && fog < .88f) for (side in -1..1 step 2) {
            detailBox(ob.x + side * ob.sx * .39f, .055f, z + side * .12f,
                .26f, .08f, .18f, airCloud, fog)
        }
    }

    private fun renderAirPlatform(ob: Ob, front: Float, fog: Float, p: Float) {
        renderPlatform(ob, front, fog, p)
        if (p > .42f && fog < .88f) {
            detailBox(ob.x, ob.clear * p + .035f, front - ob.sz * .45f,
                ob.sx * .88f, .045f, .06f, airCloud, fog)
            detailBox(ob.x, ob.clear * p + .036f, front - ob.sz * .40f,
                ob.sx * .48f, .045f, .045f, airCloudShade, fog)
        }
    }

    private fun renderAirPad(ob: Ob, z: Float, fog: Float, time: Float, p: Float) {
        renderPad(ob, z, fog, time, p)
        if (p > .40f && fog < .88f) {
            val pulse = .04f * sin(time * 5f)
            detailBox(ob.x, .24f + pulse, z + ob.sz * .15f,
                ob.sx * .50f, .055f, .04f, airCloud, fog)
        }
    }

    /** Low-poly wolves and tigers stride through the dark road; their eyes are the brightest predator cues. */
    private fun renderNightAnimal(ob: Ob, z: Float, fog: Float, p: Float, time: Float, animal: RoadAnimal) {
        if (ob.sy <= .02f || p <= .01f) return
        val scale = p.coerceIn(.05f, 1f) * minOf(1f, ob.sx / 1.35f).coerceAtLeast(.58f)
        val count = if (ob.sx > 4.4f) 4 else if (ob.sx > 3.4f) 3 else if (ob.sx > 2.2f) 2 else 1
        val centerY = visualCentreY(ob, p)
        val stride = sin(time * 8f + ob.phase)
        val body = if (animal == RoadAnimal.TIGER) nightTiger else nightWolf
        val lit = if (animal == RoadAnimal.TIGER) nightStripe else nightWolfLight
        val faceZ = z + ob.sz * .43f
        for (i in 0 until count) {
            val x = ob.x + (i - (count - 1) * .5f) * 1.20f * scale
            val bodyY = if (ob.grounded) ob.bottom + .43f * scale else centerY
            game.worldBoxSpin(x, bodyY, z, 1.02f * scale, .34f * scale, .40f * scale, stride * 3f, body, fog)
            game.worldBox(x + .38f * scale, bodyY + .13f * scale, z + .05f * scale,
                .36f * scale, .32f * scale, .36f * scale, body, fog)
            detailBox(x + .58f * scale, bodyY + .07f * scale, faceZ,
                .20f * scale, .12f * scale, .18f * scale, lit, fog)
            detailBox(x + .32f * scale, bodyY + .33f * scale, z + .04f * scale,
                .11f * scale, .19f * scale, .11f * scale, if (animal == RoadAnimal.TIGER) nightTiger else nightWolf, fog)
            detailBox(x + .48f * scale, bodyY + .25f * scale, faceZ + .04f * scale,
                .055f * scale, .055f * scale, .035f * scale, nightEye, fog)
            detailBox(x + .67f * scale, bodyY + .25f * scale, faceZ + .04f * scale,
                .055f * scale, .055f * scale, .035f * scale, nightEye, fog)
            for (leg in 0..3) {
                val side = if (leg < 2) -1f else 1f
                val along = if (leg % 2 == 0) -.30f else .30f
                val swing = stride * if (leg % 2 == 0) 10f else -10f
                game.worldBoxSpin(x + along * scale, bodyY - .21f * scale, z + side * .13f * scale,
                    .12f * scale, .38f * scale, .12f * scale, swing, nightWolf, fog)
            }
            if (animal == RoadAnimal.TIGER) {
                for (stripe in -1..1) detailBox(x + stripe * .23f * scale, bodyY + .01f * scale, faceZ,
                    .07f * scale, .30f * scale, .025f * scale, nightStripe, fog)
                detailBox(x - .60f * scale, bodyY + (.12f + stride * .05f) * scale, z - .18f * scale,
                    .42f * scale, .07f * scale, .07f * scale, nightTiger, fog)
            } else {
                detailBox(x - .58f * scale, bodyY + (.13f + stride * .05f) * scale, z - .18f * scale,
                    .36f * scale, .09f * scale, .09f * scale, nightWolfLight, fog)
            }
        }
    }

    private fun renderNightShadowGap(ob: Ob, z: Float, fog: Float, p: Float) {
        game.worldBox(ob.x, -.10f, z, ob.sx * p, .24f, ob.sz * p, Color(0.002f, .003f, .008f, 1f), fog)
        if (p > .45f) for (side in -1..1 step 2)
            detailBox(ob.x + side * ob.sx * .43f, .025f, z + ob.sz * .36f, .08f, .05f, .04f, nightEye, fog)
    }

    /** Matrix obstacles are dark collision-safe shapes with readable code-green/magenta glitch edges. */
    private fun renderHackerGlitchBlock(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val width = ob.sx * (.4f + .6f * p)
        val height = ob.sy * p
        val cy = visualCentreY(ob, p)
        game.worldBox(ob.x, cy, z, width, height, ob.sz, hackerBlock, fog)
        if (p < .42f || fog > .88f) return
        val n = (width / .46f).toInt().coerceIn(2, 12)
        for (i in 0 until n) {
            val x = ob.x - width * .46f + width * (i + .5f) / n
            val y = cy + sin(time * 7f + i * 1.8f + ob.phase) * height * .18f
            detailBox(x, y, z + ob.sz * .53f, .07f, .10f + .05f * (i % 2), .035f,
                if ((i + ob.phase.toInt()) % 4 == 0) hackerPink else hackerGreen, fog)
        }
    }

    private fun renderHackerFirewall(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val width = ob.sx * (.4f + .6f * p)
        val height = ob.sy * p
        val cy = visualCentreY(ob, p)
        game.worldBox(ob.x, cy, z, width, height * .72f, ob.sz * .62f, hackerBlock, fog)
        if (p < .44f || fog > .88f) return
        val bars = (width / .62f).toInt().coerceIn(2, 12)
        for (i in 0 until bars) {
            val x = ob.x - width * .47f + width * (i + .5f) / bars
            val blink = sin(time * 12f + i * 1.7f) > -.25f
            detailBox(x, cy, z + ob.sz * .36f, .10f, height * .64f, .05f,
                if (blink) hackerGreen else hackerPink, fog)
        }
        detailBox(ob.x, cy + height * .40f, z + ob.sz * .38f, width, .08f, .05f, hackerMint, fog)
    }

    private fun renderHackerDrone(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val size = .42f * p
        val cy = visualCentreY(ob, p)
        game.worldBoxSpin(ob.x, cy, z, size, size, size, time * 70f, hackerBlock, fog)
        for (side in -1..1 step 2) {
            game.worldBoxSpin(ob.x + side * .32f * p, cy, z, .44f * p, .07f * p, .08f * p,
                time * 90f * side, if (side < 0) hackerGreen else hackerPink, fog)
            game.worldBoxSpin(ob.x, cy + side * .32f * p, z, .08f * p, .44f * p, .08f * p,
                -time * 90f * side, hackerMint, fog)
        }
    }

    private fun renderHackerDataGate(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val width = ob.sx * (.4f + .6f * p)
        val cy = visualCentreY(ob, p)
        val pulse = .07f + .035f * (1f + sin(time * 12f))
        game.worldBox(ob.x, cy, z, width, pulse, ob.sz * .5f, hackerGreen, fog)
        for (side in -1..1 step 2) game.worldBoxSpin(ob.x + side * width * .46f, cy, z,
            .18f, .30f, .18f, time * 65f, hackerPink, fog)
    }

    private fun renderHackerVoid(ob: Ob, z: Float, fog: Float, p: Float) {
        game.worldBox(ob.x, -.12f, z, ob.sx * p, .26f, ob.sz * p, hackerBlock, fog)
        if (p > .4f && fog < .88f) {
            detailBox(ob.x, .015f, z + ob.sz * .4f, ob.sx * .9f, .05f, .04f, hackerGreen, fog)
            for (side in -1..1 step 2) detailBox(ob.x + side * ob.sx * .45f, .02f, z, .06f, .09f, ob.sz, hackerPink, fog)
        }
    }

    private fun renderHackerPlatform(ob: Ob, front: Float, fog: Float, p: Float) {
        renderPlatform(ob, front, fog, p)
        if (p > .45f && fog < .88f) for (lane in -1..1)
            detailBox(ob.x + lane * ob.sx * .30f, ob.clear * p + .035f, front - ob.sz * .42f,
                .055f, .05f, ob.sz * .75f, if (lane == 0) hackerPink else hackerGreen, fog)
    }

    private fun renderHackerPad(ob: Ob, z: Float, fog: Float, time: Float, p: Float) {
        renderPad(ob, z, fog, time, p)
        if (p > .45f && fog < .88f) detailBox(ob.x, .22f, z + ob.sz * .15f, ob.sx * .62f, .05f, .04f, hackerMint, fog)
    }

    /** New orbital mines: a bright core, crossed braces and a moving warning halo. */
    private fun renderSpaceGravityMine(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val cy = visualCentreY(ob, p)
        val size = p.coerceAtLeast(.08f)
        game.facets.add(spaceRockShapes[2], ob.x, cy, z, .48f * size, .43f * size, .46f * size,
            time * 22f, 12f, -time * 17f, spaceRock, fog, game.fogColor, glow = .28f)
        game.worldBoxSpin(ob.x, cy, z, ob.sx * .84f * size, .11f * size, .12f * size,
            time * 42f + ob.phase, spaceRingColors[0], fog)
        game.worldBoxSpin(ob.x, cy, z, .12f * size, ob.sy * .86f * size, .12f * size,
            -time * 42f - ob.phase, spaceWarning, fog)
        if (p > .5f && fog < .86f) {
            game.worldBoxSpin(ob.x, cy, z + ob.sz * .38f, .18f * size, .18f * size, .08f * size,
                time * 75f, spacePanelLight, fog)
        }
    }

    /** Full-width jump gates are bright force-fields with emitters at each road edge. */
    private fun renderSpaceEnergyWall(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val width = ob.sx * (.4f + .6f * p)
        val h = ob.sy * p
        val cy = visualCentreY(ob, p)
        game.worldBox(ob.x, cy, z, width, h * .58f, ob.sz * .55f, spacePanel, fog)
        val pulse = .07f + .025f * (1f + sin(time * 8f))
        game.worldBox(ob.x, cy + h * .08f, z + ob.sz * .30f, width * .98f, pulse, .05f, spaceWarning, fog)
        for (side in -1..1 step 2) {
            val x = ob.x + side * (width * .47f)
            game.worldBoxSpin(x, cy, z, .19f, h * .98f, .22f, time * 32f, spaceRingColors[0], fog)
        }
    }

    /** Overhead hazards in Space are thin blue lasers with unmistakable end emitters. */
    private fun renderSpaceLaser(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val width = ob.sx * (.4f + .6f * p)
        val cy = visualCentreY(ob, p)
        val beam = (.10f + .025f * sin(time * 12f)).coerceAtLeast(.055f)
        game.worldBox(ob.x, cy, z, width, beam, ob.sz * .36f, spaceLaser, fog)
        for (side in -1..1 step 2) {
            game.worldBoxSpin(ob.x + side * width * .47f, cy, z, .24f, .28f, .28f,
                time * 45f, spaceWarning, fog)
        }
    }

    /** A sweeping panel arm with illuminated solar cells follows the same authored movement. */
    private fun renderSpaceSolarArray(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val width = ob.sx * (.4f + .6f * p)
        val cy = visualCentreY(ob, p)
        game.worldBoxSpin(ob.x, cy, z, width, ob.sy * p, ob.sz, 7f * sin(time * 2f + ob.phase), spacePanel, fog)
        if (p < .48f || fog > .86f) return
        for (side in -1..1 step 2) {
            game.worldBoxSpin(ob.x + side * width * .34f, cy, z, width * .25f, .07f, ob.sz * .8f,
                11f * sin(time * 2f + ob.phase), spaceRingColors[0], fog)
        }
    }

    /** Animated pistons read as charged plasma columns rising from the glass road. */
    private fun renderSpacePlasmaSpire(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val h = ob.sy * p
        if (h < .035f) return
        val cy = visualCentreY(ob, p)
        game.worldBox(ob.x, ob.bottom + h * .42f, z, .34f, h * .84f, .34f, spacePanel, fog)
        game.worldBoxSpin(ob.x, ob.bottom + h * .76f, z, .38f, .12f, .38f,
            time * 30f, spaceRingColors[0], fog)
        if (p > .52f) game.worldBox(ob.x, cy, z + ob.sz * .28f, .10f, h * .72f, .06f, spaceRingColors[1], fog)
    }

    private fun renderSpaceDropPod(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        renderSpaceAsteroid(ob, z, fog, p, time)
        if (p > .5f && fog < .84f) {
            game.worldBoxSpin(ob.x, visualCentreY(ob, p) + .45f * p, z + ob.sz * .26f,
                .28f * p, .10f * p, .08f, time * 50f, spaceWarning, fog)
        }
    }

    /** Wide unavoidable hazards become legible station-hull silhouettes with porthole lights. */
    private fun renderSpaceHull(ob: Ob, z: Float, fog: Float, p: Float) {
        val width = ob.sx * (.4f + .6f * p)
        val h = ob.sy * p
        val cy = visualCentreY(ob, p)
        game.worldBox(ob.x, cy, z, width, h, ob.sz, spacePanel, fog)
        if (p < .5f || fog > .86f) return
        val count = (width / .72f).toInt().coerceIn(4, 12)
        for (i in 0 until count) {
            val x = ob.x - width * .45f + width * (i + .5f) / count
            detailBox(x, cy + h * .2f, z + ob.sz * .52f, .15f, .12f, .04f,
                if (i % 3 == 0) spaceWarning else spaceRingColors[0], fog)
        }
        detailBox(ob.x, cy - h * .34f, z + ob.sz * .52f, width * .9f, .055f, .04f, spaceRingColors[1], fog)
    }

    private fun renderJunglePlatform(ob: Ob, front: Float, fog: Float, p: Float) {
        renderPlatform(ob, front, fog, p)
        if (p < .5f || fog > .86f) return
        val top = ob.clear * p + .025f
        detailBox(ob.x, top, front - ob.sz * .46f, ob.sx * .72f, .045f, ob.sz * .46f, jungleMoss, fog)
        for (side in -1..1 step 2) {
            detailBox(ob.x + side * ob.sx * .39f, top - .13f, front - ob.sz * .26f,
                .10f, .30f, .09f, jungleLeaf, fog)
        }
    }

    private fun renderSpacePlatform(ob: Ob, front: Float, fog: Float, p: Float) {
        val top = ob.clear * p
        val bodyLen = ob.sz - ob.ramp
        game.worldBox(ob.x, top / 2f, front - ob.ramp - bodyLen / 2f, ob.sx, top, bodyLen, spacePanel, fog)
        if (ob.ramp > 0f) {
            val n = 4; val d = ob.ramp / n
            for (i in 0 until n) game.worldBox(ob.x, top * (i + 1) / n / 2f, front - d * (i + .5f), ob.sx, top * (i + 1) / n, d, spacePanel, fog)
        }
        if (p < .5f || fog > .86f) return
        detailBox(ob.x, top + .035f, front - ob.sz * .52f, ob.sx * .72f, .045f, ob.sz * .45f, spaceRingColors[0], fog)
        for (side in -1..1 step 2) {
            detailBox(ob.x + side * ob.sx * .42f, top + .05f, front - ob.sz * .45f,
                .09f, .08f, .18f, spaceWarning, fog)
        }
    }

    private fun renderJunglePad(ob: Ob, z: Float, fog: Float, time: Float, p: Float) {
        val breath = .025f * sin(time * 6f)
        game.worldBox(ob.x, .07f, z, ob.sx * (.45f + .55f * p), .14f, ob.sz * .72f, jungleBark, fog)
        game.facets.add(jungleMushroomShape, ob.x, .29f + breath, z, ob.sx * .48f * p, .26f * p,
            ob.sz * .46f * p, time * 6f, 0f, 0f, jungleFruitPalette, fog, game.fogColor, glow = .06f)
        if (p > .56f && fog < .84f) for (i in -1..1) {
            detailBox(ob.x + i * .24f, .37f + breath, z + .34f, .10f, .06f, .035f, jungleSpot, fog)
        }
    }

    private fun renderSpaceRing(ob: Ob, z: Float, fog: Float, time: Float, p: Float) {
        val y = .20f + .025f * sin(time * 5f)
        game.facets.add(spaceRingShape, ob.x, y, z, ob.sx * .58f * p, .12f, ob.sz * .58f * p,
            time * 24f, 0f, 0f, spaceRingColors, fog, game.fogColor, glow = .92f)
        game.worldBoxSpin(ob.x, .12f, z, .28f * p, .12f, .28f * p, -time * 50f, spacePanelLight, fog)
    }

    private fun renderSpaceDrone(ob: Ob, z: Float, fog: Float, time: Float, p: Float) {
        val shape = spaceRockShapes[1]
        game.facets.add(shape, ob.x, ob.cy, z, .46f * p, .40f * p, .38f * p,
            time * 34f, time * 18f, time * 21f, spaceRock, fog, game.fogColor, glow = .2f)
        game.facets.add(spaceRingShape, ob.x, ob.cy, z, .76f * p, .09f * p, .72f * p,
            -time * 75f, 0f, 0f, spaceRingColors, fog, game.fogColor, glow = .86f)
        game.worldBox(ob.x * .5f, ob.top + 1.1f, z, abs(ob.x) + .16f, .12f, .12f, spacePanel, fog)
        game.worldBox(0f, ob.top + 2.2f, z, 6.4f * p, .18f, .18f, spacePanel, fog)
    }

    private fun renderJungleVinePendulum(ob: Ob, z: Float, fog: Float, p: Float, time: Float) {
        val yTop = ob.top + 2.15f
        for (i in 0..4) {
            val t = i / 4f
            val x = ob.x * t
            val y = ob.top + (yTop - ob.top) * t
            game.worldBox(x, y, z, .10f, .24f, .10f, jungleMoss, fog)
        }
        game.worldBox(0f, yTop, z, 6.4f * p, .22f, .22f, jungleBark, fog)
        game.facets.add(jungleBushShape, ob.x, ob.cy, z, .56f * p, .46f * p, .42f * p,
            0f, 0f, time * 8f, jungleLeafPalette, fog, game.fogColor, glow = .02f)
    }

    private fun renderGenericPendulum(ob: Ob, z: Float, fog: Float, p: Float) {
        box(ob.x, ob.cy, z, ob.sx, ob.sy, ob.sz, cues.body(ob, ob.col), fog, p, false)
        game.worldBox(ob.x * .5f, ob.top + 1.1f, z, abs(ob.x) + .16f, .16f, .16f, rod, fog)
        game.worldBox(0f, ob.top + 2.2f, z, 6.4f * p, .22f, .22f, rod, fog)
    }

    private fun renderJunglePit(ob: Ob, z: Float, fog: Float, time: Float, p: Float, phase: Float) {
        if (ob.type == ObType.DECO) {
            game.worldBox(ob.x, -.01f, z, ob.sx * p, .06f, ob.sz, jungleMoss, fog)
            return
        }
        game.worldBox(ob.x, -.10f, z, ob.sx * p, .25f, ob.sz, jungleMud, fog)
        for (side in -1..1 step 2) {
            val x = ob.x + side * ob.sx * .38f
            game.worldBoxSpin(x, .12f + .05f * sin(time * 2f + side), z, .32f, .09f, .14f,
                side * 18f + phase * 12f, jungleLeaf, fog)
        }
    }

    /** Red/ivory bands on Neon City's existing jump barriers; never add or move a collider. */
    private fun renderCityBarrierStripe(ob: Ob, z: Float, fog: Float, p: Float) {
        val width = ob.sx * if (p >= .999f) 1f else .4f + .6f * p
        val height = ob.sy * if (p >= .999f) 1f else p
        if (width <= .1f || height <= .1f) return
        val cy = if (ob.grounded) ob.bottom + height / 2f else ob.cy
        val faceZ = z + ob.sz / 2f + .025f
        val terrainOffset = cube.run.game.Terrain.y(z) - cube.run.game.Terrain.y(faceZ)
        val bandHeight = (height * .13f).coerceAtLeast(.045f)
        for (i in 0..2) {
            val y = cy - height / 2f + bandHeight * (i + .5f) + terrainOffset
            detailBox(ob.x, y, faceZ, width + .025f, bandHeight, .034f,
                if (i % 2 == 0) cityBarrierRed else cityBarrierWhite, fog)
        }
    }

    /**
     * A tar pit as a void: a black slab sunk below the road surface inside a
     * thin glowing lip, with dark motes drifting up out of it. The lip is the
     * DECO half of the pair, the slab the SOLID one (drawn lower than it
     * collides, so it reads as a hole rather than a block).
     */
    private fun renderPit(ob: Ob, z: Float, fog: Float, time: Float, p: Float, phase: Float) {
        if (ob.type == ObType.DECO) { game.worldBox(ob.x, -0.01f, z, ob.sx * p, 0.06f, ob.sz, ob.col, fog); return } // the lip: top at 0.02
        game.worldBox(ob.x, -0.1f, z, ob.sx * p, 0.26f, ob.sz, ob.col, fog)                                        // the void: top at 0.03, black
        hsvInto(tmpCol, 270f, 0.6f, 0.18f)
        for (i in 0 until 4) { // motes rising out of the dark
            val k = ((time * 0.7f + i * 0.25f + phase) % 1f + 1f) % 1f
            val mx = ob.x + (i - 1.5f) * ob.sx * 0.22f + 0.15f * sin(time * 3f + i)
            val s = 0.1f * (1f - k)
            game.worldBoxSpin(mx, 0.1f + k * 0.9f, z + (i % 2 - 0.5f) * 0.5f, s, s, s, time * 90f + i * 50f, tmpCol, fog)
        }
    }

    /** A springy slab: a coloured base with a pale top that breathes. */
    private fun renderPad(ob: Ob, z: Float, fog: Float, time: Float, p: Float) {
        val breath = 0.03f * sin(time * 6f)
        val s = 0.5f + 0.5f * p
        game.worldBox(ob.x, 0.05f, z, ob.sx * s, 0.1f, ob.sz * s, ob.col, fog)
        game.worldBox(ob.x, 0.13f + breath, z, ob.sx * 0.8f * s, 0.08f, ob.sz * 0.8f * s, padTop, fog)
        game.worldBox(ob.x, 0.11f + breath * 2f, z, ob.sx * 0.35f * s, 0.16f, ob.sz * 0.35f * s, ob.col, fog)
    }

    /** A box that orbits a pickup's centre as the whole thing spins (offset [dx] along the spun x axis). */
    private fun spinPart(cx: Float, cz: Float, dx: Float, y: Float, sx: Float, sy: Float, sz: Float, yaw: Float, col: Color, fog: Float) {
        val rad = Math.toRadians(yaw.toDouble())
        game.worldBoxSpin(cx + dx * cos(rad).toFloat(), y, cz - dx * sin(rad).toFloat(), sx, sy, sz, yaw, col, fog)
    }

    private fun renderPickup(r: Row, yaw: Float, time: Float, p: Float) {
        val cz = r.z + Row.PICKUP_DZ
        val x = r.pickupX
        val y = 0.85f + 0.1f * sin(time * 3f + r.visualPhase)
        val fog = Fog.at(cz)
        val s = p
        when (r.pickup) {
            Pickup.SHARD_EMBER, Pickup.SHARD_FROST, Pickup.SHARD_VOID ->
                game.worldCrystal(x, y, cz, s * .85f, yaw * .6f, shardCols[Pickup.shardType(r.pickup)], fog)
            Pickup.RED_PILL -> game.worldPill(x, y + .1f, cz, s * .68f, time * 100f + r.visualPhase * 57.29578f, fog)
            Pickup.BOX -> { // a spinning gift: purple cube with a gold ribbon
                game.worldBoxSpin(x, y, cz, 0.62f * s, 0.62f * s, 0.62f * s, yaw * 0.5f, boxCol, fog)
                game.worldBoxSpin(x, y, cz, 0.66f * s, 0.16f * s, 0.66f * s, yaw * 0.5f, bandCol, fog)
                game.worldBoxSpin(x, y, cz, 0.16f * s, 0.66f * s, 0.66f * s, yaw * 0.5f, bandCol, fog)
            }
            Pickup.MAGNET -> { // a red horseshoe with white tips
                spinPart(x, cz, -0.26f * s, y + 0.1f, 0.2f * s, 0.62f * s, 0.2f * s, yaw, magnetCol, fog)
                spinPart(x, cz, 0.26f * s, y + 0.1f, 0.2f * s, 0.62f * s, 0.2f * s, yaw, magnetCol, fog)
                game.worldBoxSpin(x, y - 0.2f, cz, 0.72f * s, 0.2f * s, 0.2f * s, yaw, magnetCol, fog)
                spinPart(x, cz, -0.26f * s, y + 0.45f, 0.2f * s, 0.12f * s, 0.2f * s, yaw, Color.WHITE, fog)
                spinPart(x, cz, 0.26f * s, y + 0.45f, 0.2f * s, 0.12f * s, 0.2f * s, yaw, Color.WHITE, fog)
            }
            Pickup.MULT -> { // two stacked pink slabs: "×2"
                game.worldBoxSpin(x, y - 0.18f, cz, 0.7f * s, 0.22f * s, 0.7f * s, yaw, multCol, fog)
                game.worldBoxSpin(x, y + 0.18f, cz, 0.7f * s, 0.22f * s, 0.7f * s, yaw + 30f, multCol, fog)
                game.worldBoxSpin(x, y + 0.45f, cz, 0.2f * s, 0.2f * s, 0.2f * s, yaw, Color.WHITE, fog)
            }
            Pickup.JET -> { // twin blue tanks with flickering flames
                val flick = 0.18f + 0.1f * abs(sin(time * 21f + r.visualPhase))
                spinPart(x, cz, -0.2f * s, y + 0.1f, 0.3f * s, 0.8f * s, 0.3f * s, yaw, jetCol, fog)
                spinPart(x, cz, 0.2f * s, y + 0.1f, 0.3f * s, 0.8f * s, 0.3f * s, yaw, jetCol, fog)
                spinPart(x, cz, -0.2f * s, y - 0.42f, 0.2f * s, flick, 0.2f * s, yaw, flameCol, fog)
                spinPart(x, cz, 0.2f * s, y - 0.42f, 0.2f * s, flick, 0.2f * s, yaw, flameCol, fog)
            }
            Pickup.SKATEBOARD -> { // a tiny orange deck on four dark wheels
                game.worldBoxSpin(x, y, cz, .72f * s, .13f * s, .42f * s, yaw, skateboardDeck, fog)
                game.worldBoxSpin(x, y + .075f * s, cz, .43f * s, .035f * s, .10f * s, yaw, skateboardStripe, fog)
                for (side in -1..1 step 2) for (front in -1..1 step 2)
                    spinPart(x, cz, side * .24f * s, y - .12f * s + front * .08f * s,
                        .13f * s, .13f * s, .13f * s, yaw, skateboardWheel, fog)
            }
            Pickup.JUMP_SHOES -> {
                for (side in -1..1 step 2) {
                    spinPart(x, cz, side * .18f * s, y + .06f * s, .32f * s, .22f * s, .44f * s, yaw, jumpShoeBody, fog)
                    spinPart(x, cz, side * .18f * s, y - .08f * s, .35f * s, .09f * s, .46f * s, yaw, jumpShoeSole, fog)
                    spinPart(x, cz, side * .18f * s, y + .18f * s, .18f * s, .045f * s, .18f * s, yaw, jumpShoeLace, fog)
                }
            }
            Pickup.PHASE -> { // a violet core held in a shifting pair of translucent-looking frames
                game.worldBoxSpin(x, y, cz, .42f * s, .42f * s, .42f * s, -yaw, phasePowerCol, fog)
                game.worldBoxSpin(x, y, cz, .80f * s, .10f * s, .80f * s, yaw, Color.WHITE, fog)
                game.worldBoxSpin(x, y, cz, .10f * s, .80f * s, .80f * s, -yaw, phasePowerCol, fog)
            }
            Pickup.TIME_WARP -> { // a mint hourglass / clock with a bright central hand
                game.worldBoxSpin(x, y + .22f * s, cz, .68f * s, .12f * s, .48f * s, yaw, timeWarpCol, fog)
                game.worldBoxSpin(x, y - .22f * s, cz, .68f * s, .12f * s, .48f * s, yaw + 90f, timeWarpCol, fog)
                game.worldBoxSpin(x, y, cz, .14f * s, .50f * s, .14f * s, -yaw * .65f, Color.WHITE, fog)
            }
        }
    }

    /** Colour used for coin bursts and the HUD's mission cues. */
    fun colorOfCoin(kind: CoinType): Color = coinKindCols[kind.ordinal]

    /** Colours for the pickup bursts. */
    fun colorOf(kind: Int): Color = when (kind) {
        Pickup.SHARD_EMBER, Pickup.SHARD_FROST, Pickup.SHARD_VOID -> shardCols[Pickup.shardType(kind)]
        Pickup.BOX -> boxCol
        Pickup.RED_PILL -> magnetCol
        Pickup.MAGNET -> magnetCol
        Pickup.MULT -> multCol
        Pickup.JET -> jetCol
        Pickup.SKATEBOARD -> skateboardDeck
        Pickup.JUMP_SHOES -> jumpShoeBody
        Pickup.PHASE -> phasePowerCol
        Pickup.TIME_WARP -> timeWarpCol
        else -> bandCol
    }
}
