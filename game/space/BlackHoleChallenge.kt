package cube.run.game.space

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector3
import cube.run.core.Gdx3DGame
import cube.run.core.SoundFx
import cube.run.core.gfx.FacetShapes
import cube.run.core.hsvInto
import cube.run.game.Player
import cube.run.game.track.Pickup
import cube.run.game.track.Row
import cube.run.game.track.Track
import cube.run.game.world.Fog
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** The playable singularity encounter: a live gravity field and a timed escape gate. */
class BlackHoleChallenge(private val game: Gdx3DGame) {
    val clock = SpaceEscapeClock()
    val active: Boolean get() = clock.active
    val hasStarted: Boolean get() = clock.state != SpaceEscapeClock.State.READY
    val state: SpaceEscapeClock.State get() = clock.state

    private var exitRow: Row? = null
    private var prepared = false
    private var drainCooldown = 0f
    private val impact = Vector3()
    private val darkHole = FacetShapes.voxelBall(8)
    private val hotDisc = FacetShapes.voxelRing(18, .42f, 3)
    private val lens = FacetShapes.voxelRing(16, .82f, 1)
    private val cube = FacetShapes.cube()
    private val black = arrayOf(Color(.006f, .004f, .014f, 1f))
    private val accretion = arrayOf(Color(), Color())
    private val halo = arrayOf(Color(), Color())

    init {
        hsvInto(accretion[0], 14f, .96f, 1f); hsvInto(accretion[1], 326f, .86f, 1f)
        hsvInto(halo[0], 190f, .76f, 1f); hsvInto(halo[1], 284f, .78f, 1f)
    }

    fun begin(track: Track): Boolean {
        if (!clock.start()) return false
        exitRow = track.addChallengeExit(clock.routeDistance)
        return true
    }

    fun reset(track: Track) {
        exitRow?.let(track::removeChallengeExit)
        exitRow = null
        clock.reset()
        drainCooldown = 0f
    }

    /** Advance the deadline, tug collectibles, and drift the cube toward the moving centre. Course hitboxes stay anchored to preserve the authored safe route. */
    fun update(
        dt: Float,
        moved: Float,
        track: Track,
        player: Player,
        time: Float,
        safeArrival: Boolean = false,
    ): SpaceEscapeClock.State {
        // The player and exit gate move through the arrival corridor together. Keep the
        // route distance in lockstep, but suspend the deadline and gravitational field.
        val phase = clock.advance(dt, moved, countSeconds = !safeArrival)
        if (phase != SpaceEscapeClock.State.ACTIVE || safeArrival) return phase
        drainCooldown = max(0f, drainCooldown - dt)
        val hx = centreX(time)
        val hy = centreY(time)
        val hz = centreZ()
        val influence = 42f
        val influence2 = influence * influence

        for (row in track.rows) {
            if (abs(row.z - hz) > influence + 12f) continue
            row.coins?.let { coins ->
                for (coin in coins) {
                    if (coin.taken || coin.missed) continue
                    val cz = row.z + coin.dz
                    val dx = hx - coin.x; val dy = hy - coin.y; val dz = hz - cz
                    val d2 = dx * dx + dy * dy + dz * dz
                    if (d2 >= influence2) continue
                    val d = sqrt(d2)
                    val weight = ((influence - d) / influence).coerceIn(0f, 1f)
                    val k = min(.14f, dt * (.48f + 2.15f * weight))
                    // Tangential drift gives the pull a visible spiral instead of a straight-line slide.
                    val swirl = sin(time * 4f + cz * .09f) * k * .16f
                    coin.x += dx * k - swirl
                    coin.y = (coin.y + dy * k * .38f + swirl * .45f).coerceIn(.18f, 6.1f)
                    coin.dz += dz * k * .28f
                    if (d < 1.55f) {
                        coin.taken = true // swallowed, never credited as a player pickup
                        coin.missed = true
                        if (drainCooldown <= 0f) {
                            drainCooldown = .17f
                            SoundFx.play("drain", rate = .78f, vol = .32f)
                            game.burst3d(impact.set(coin.x, coin.y, cz), accretion[0], n = 3,
                                speed = 2.2f, size = .07f, life = .32f, gravity = 0f)
                        }
                    }
                }
            }

            if (row.pickup != Pickup.NONE) {
                val pz = row.z + Row.PICKUP_DZ
                val dx = hx - row.pickupX; val dy = hy - .85f; val dz = hz - pz
                val d2 = dx * dx + dy * dy + dz * dz
                if (d2 < influence2) {
                    val d = sqrt(d2)
                    val k = min(.1f, dt * (.3f + 1.45f * ((influence - d) / influence).coerceIn(0f, 1f)))
                    row.pickupX += dx * k
                    if (d < 1.2f) row.pickup = Pickup.NONE
                }
            }
        }

        val px = player.px
        val py = player.py
        val pdx = hx - px; val pdy = hy - py; val pdz = -hz
        val pd2 = pdx * pdx + pdy * pdy + pdz * pdz
        if (pd2 < influence2) {
            val pull = ((influence - sqrt(pd2)) / influence).coerceIn(0f, 1f)
            // Jet thrust resists suction, but can be overwhelmed if the player waits too long.
            player.applyGravityPull(hx, pull * if (player.flying) 4.2f else 7.8f, dt)
        }
        return phase
    }

    /** Render the same moving centre that drives collision and attraction. */
    fun render(time: Float) {
        if (clock.state != SpaceEscapeClock.State.ACTIVE && clock.state != SpaceEscapeClock.State.CAPTURED) return
        if (!prepared) { game.facets.prepare(darkHole, hotDisc, lens, cube); prepared = true }
        val x = centreX(time)
        val y = centreY(time)
        val z = centreZ()
        val scale = 4.1f + 1.2f * clock.routeProgress
        val fog = Fog.at(z) * .32f
        val alpha = if (clock.state == SpaceEscapeClock.State.CAPTURED) .98f else 1f
        game.facets.add(darkHole, x, y, z, scale, scale, scale,
            time * 13f, 9f, time * 7f, black, fog, game.fogColor, glow = .04f, opacity = alpha)
        game.facets.add(hotDisc, x, y, z - .45f, scale * 3.2f, scale * 3.2f, scale * 3.2f,
            time * 56f, 71f, time * 18f, accretion, fog, game.fogColor, glow = 1f, opacity = alpha)
        game.facets.add(lens, x, y, z - .9f, scale * 2.15f, scale * 2.15f, scale * 2.15f,
            -time * 34f, 78f, -time * 12f, halo, fog, game.fogColor, glow = .96f, opacity = alpha)
        // Six blocky light nodes make the gravitational lens legible even when its disc turns edge-on.
        for (i in 0 until 6) {
            val angle = time * .8f + i * (6.2831855f / 6f)
            val r = scale * 2.3f
            val s = .22f + .035f * sin(time * 5f + i)
            val palette = if (i % 2 == 0) halo else accretion
            game.facets.add(cube, x + kotlin.math.cos(angle) * r, y + kotlin.math.sin(angle) * r * .45f,
                z + kotlin.math.sin(angle) * r * .2f, s, s, s, time * 100f + i * 42f,
                0f, 0f, palette, fog, game.fogColor, glow = .95f, opacity = alpha)
        }
    }

    private fun centreX(time: Float): Float = 1.65f * sin(time * .62f + .4f)
    private fun centreY(time: Float): Float = 2.35f + .4f * sin(time * 1.1f)
    /** The exit ring is ahead; the singularity trails it and swells toward the runner. */
    private fun centreZ(): Float = -clock.distanceLeft + 22f
}
