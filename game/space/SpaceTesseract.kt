package cube.run.game.space

import com.badlogic.gdx.graphics.Color
import cube.run.core.Gdx3DGame
import cube.run.core.gfx.FacetShapes
import cube.run.core.hsvInto
import cube.run.game.world.Fog
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Convert a 3D edge vector to the FacetBatch local-Z axis (yaw then pitch). */
internal object SpaceTesseractMath {
    private const val RAD_TO_DEG = 57.29578f

    /** Writes yaw, pitch, and half-length into a reusable [out] array. */
    fun segmentPose(dx: Float, dy: Float, dz: Float, out: FloatArray) {
        require(out.size >= 3)
        val horizontal = sqrt(dx * dx + dz * dz)
        out[0] = atan2(dx, dz) * RAD_TO_DEG
        out[1] = -atan2(dy, horizontal) * RAD_TO_DEG
        out[2] = sqrt(dx * dx + dy * dy + dz * dz) * 0.5f
    }
}

/** A roaming, 4D-rotated hypercube projected into the space scene. */
class SpaceTesseract(private val game: Gdx3DGame) {
    private val cube = FacetShapes.cube()
    private val vertices = FloatArray(16 * 3)
    private val links = ArrayList<IntArray>(32).apply {
        for (a in 0 until 16) for (b in a + 1 until 16) {
            if (Integer.bitCount(a xor b) == 1) add(intArrayOf(a, b))
        }
    }
    private val cyan = arrayOf(Color(), Color())
    private val pink = arrayOf(Color(), Color())
    private val pearl = arrayOf(Color())
    private val linePose = FloatArray(3)
    private var prepared = false

    init {
        hsvInto(cyan[0], 184f, .9f, 1f); hsvInto(cyan[1], 196f, .55f, .95f)
        hsvInto(pink[0], 320f, .9f, 1f); hsvInto(pink[1], 286f, .6f, 1f)
        pearl[0].set(.88f, .96f, 1f, 1f)
    }

    /** Drawn only in the actual Space portal, as a slow-moving 4D structure. */
    fun render(time: Float, reach: Float) {
        if (reach <= .002f) return
        if (!prepared) { game.facets.prepare(cube); prepared = true }
        project(time)

        // It wanders across and above the road while the two independent 4D rotations keep changing its silhouette.
        val cx = 13f * sin(time * .37f)
        val cy = 11f + 3.2f * sin(time * .29f + .7f)
        val cz = -42f + 11f * cos(time * .23f)
        val size = 4.2f
        val fog = Fog.at(cz) * .42f
        val alpha = reach.coerceIn(0f, 1f)

        for (i in links.indices) {
            val edge = links[i]
            val a = edge[0] * 3
            val b = edge[1] * 3
            val ax = vertices[a]; val ay = vertices[a + 1]; val az = vertices[a + 2]
            val bx = vertices[b]; val by = vertices[b + 1]; val bz = vertices[b + 2]
            val dx = bx - ax; val dy = by - ay; val dz = bz - az
            SpaceTesseractMath.segmentPose(dx, dy, dz, linePose)
            if (linePose[2] <= .001f) continue
            val palette = when (i % 5) { 0, 3 -> pink; else -> cyan }
            game.facets.add(cube,
                cx + (ax + bx) * .5f * size,
                cy + (ay + by) * .5f * size,
                cz + (az + bz) * .5f * size,
                .075f, .075f, linePose[2] * size, linePose[0], linePose[1], 0f,
                palette, fog, game.fogColor, glow = .82f, opacity = alpha)
        }
        for (vertex in 0 until 16) {
            val p = vertex * 3
            val palette = if (vertex % 3 == 0) pearl else cyan
            val s = if (vertex % 3 == 0) .22f else .15f
            game.facets.add(cube, cx + vertices[p] * size, cy + vertices[p + 1] * size,
                cz + vertices[p + 2] * size, s, s, s, time * 90f + vertex * 9f,
                0f, 0f, palette, fog, game.fogColor, glow = .95f, opacity = alpha)
        }
    }

    /** Rotate in several 4D planes, then perspective-project w into visible 3D coordinates. */
    private fun project(time: Float) {
        val ax = time * .48f; val ay = time * .31f; val az = time * .22f; val twist = time * .17f
        val c1 = cos(ax); val s1 = sin(ax)
        val c2 = cos(ay); val s2 = sin(ay)
        val c3 = cos(az); val s3 = sin(az)
        val c4 = cos(twist); val s4 = sin(twist)
        for (index in 0 until 16) {
            var x = if (index and 1 == 0) -1f else 1f
            var y = if (index and 2 == 0) -1f else 1f
            var z = if (index and 4 == 0) -1f else 1f
            var w = if (index and 8 == 0) -1f else 1f
            val x1 = x * c1 - w * s1
            w = x * s1 + w * c1
            x = x1
            val y1 = y * c2 - w * s2
            w = y * s2 + w * c2
            y = y1
            val z1 = z * c3 - w * s3
            w = z * s3 + w * c3
            z = z1
            val x2 = x * c4 - y * s4
            y = x * s4 + y * c4
            x = x2
            val perspective = 3.6f / (3.6f - w).coerceAtLeast(.8f)
            val p = index * 3
            vertices[p] = x * perspective
            vertices[p + 1] = y * perspective
            vertices[p + 2] = z * perspective
        }
    }
}
