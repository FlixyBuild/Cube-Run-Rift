package cube.run.game.world

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import cube.run.data.Worlds
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Small, offline panorama layers for every biome. Drawn on a camera-facing far
 * plane, behind the 3D road, so mountains, volcanoes and skyline silhouettes
 * enrich the horizon without consuming the gameplay box batch or its capacity.
 */
internal class WorldSkyBackdrop {
    private val plane = Matrix4()
    private val right = Vector3()
    private val up = Vector3()
    private val back = Vector3()
    private val centre = Vector3()
    private val candyColors = arrayOf(
        floatArrayOf(1f, .28f, .35f),
        floatArrayOf(.30f, .82f, .95f),
        floatArrayOf(.98f, .73f, .22f),
    )

    fun render(shapes: ShapeRenderer, camera: Camera, worldId: Int, time: Float, opacity: Float) {
        if (opacity <= .01f || camera.viewportHeight <= 0f || camera.viewportWidth <= 0f) return
        val fov = (camera as? PerspectiveCamera)?.fieldOfView ?: 67f
        val depth = 300f
        val scale = depth * tan(fov * .5f * DEG)
        centre.set(camera.direction).scl(depth).add(camera.position)
        right.set(camera.direction).crs(camera.up).nor()
        up.set(right).crs(camera.direction).nor()
        back.set(camera.direction).scl(-scale)
        plane.set(right.scl(scale), up.scl(scale), back, centre)
        val halfWidth = (camera.viewportWidth / camera.viewportHeight).coerceIn(.45f, 1.75f) * 1.22f

        shapes.end()
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.transformMatrix = plane
        drawStars(shapes, halfWidth, time, opacity)
        when (worldId) {
            Worlds.LAVA_ID -> drawVolcano(shapes, halfWidth, time, opacity)
            Worlds.WINTER_ID -> drawFrostPeaks(shapes, halfWidth, time, opacity)
            Worlds.JUNGLE_ID -> drawJungle(shapes, halfWidth, time, opacity)
            Worlds.NEON_CITY_ID -> drawCity(shapes, halfWidth, time, opacity)
            Worlds.GALAXY_ID -> drawGalaxy(shapes, halfWidth, time, opacity)
            Worlds.RAILWAY_ID -> drawRailway(shapes, halfWidth, time, opacity)
            Worlds.BEACH_ID -> drawBeach(shapes, halfWidth, time, opacity)
            Worlds.NIGHT_ID -> drawMoonlit(shapes, halfWidth, time, opacity)
            Worlds.HACKER_ID -> drawMatrix(shapes, halfWidth, time, opacity)
            Worlds.AIR_ID -> drawAir(shapes, halfWidth, time, opacity)
            Worlds.NEXUS_ID -> drawNexus(shapes, halfWidth, time, opacity)
            Worlds.HIGHWAY_ID -> drawHighway(shapes, halfWidth, time, opacity)
            else -> drawCandy(shapes, halfWidth, time, opacity)
        }
        shapes.identity()
    }

    private fun drawStars(shapes: ShapeRenderer, w: Float, time: Float, a: Float) {
        for (i in 0 until 25) {
            val x = (((i * 37 + 11) % 101) / 50f - 1f) * w
            val y = .10f + ((i * 29 + 7) % 61) / 100f
            val blink = .35f + .45f * (.5f + .5f * sin(time * (1.1f + i % 4 * .22f) + i * 2.1f))
            fill(shapes, .80f, .91f, 1f, a * blink)
            val size = .0045f + (i % 4) * .0014f
            shapes.circle(x, y, size)
            if (i % 7 == 0) {
                shapes.rect(x - size * .22f, y - size * 2.4f, size * .44f, size * 4.8f)
                shapes.rect(x - size * 2.4f, y - size * .22f, size * 4.8f, size * .44f)
            }
        }
    }

    private fun drawVolcano(shapes: ShapeRenderer, w: Float, time: Float, a: Float) {
        ridge(shapes, w, -.38f, .18f, .10f, .16f, .22f, a)
        fill(shapes, .095f, .035f, .045f, a * .88f)
        shapes.triangle(-w * .62f, -.40f, -w * .15f, .38f, w * .22f, -.40f)
        fill(shapes, .20f, .055f, .035f, a * .92f)
        shapes.triangle(-w * .32f, -.40f, w * .28f, .54f, w * .82f, -.40f)
        fill(shapes, .035f, .018f, .028f, a)
        shapes.rect(w * .20f, .43f, w * .18f, .045f)
        fill(shapes, 1f, .19f, .025f, a * .92f)
        val pulse = .018f * sin(time * 2.4f)
        shapes.rect(w * .245f, .415f + pulse, w * .095f, .018f)
        fill(shapes, 1f, .42f, .045f, a * .9f)
        shapes.triangle(w * .27f, .42f, w * .33f, -.30f, w * .37f, .42f)
        fill(shapes, .85f, .12f, .025f, a * .82f)
        shapes.triangle(w * .39f, .42f, w * .57f, -.20f, w * .62f, .42f)
        // Smoke and a small, repeating eruption remain safely behind the playable road.
        val drift = .045f * sin(time * .35f)
        fill(shapes, .18f, .16f, .19f, a * .30f)
        shapes.circle(w * .28f + drift, .57f, .12f)
        shapes.circle(w * .34f + drift, .67f, .15f)
        shapes.circle(w * .23f + drift, .77f, .11f)
        val erupt = .5f + .5f * sin(time * 2.2f)
        fill(shapes, 1f, .37f, .035f, a * (.25f + .5f * erupt))
        shapes.circle(w * .31f + drift, .51f + .04f * erupt, .025f + .018f * erupt)
        fill(shapes, 1f, .12f, .02f, a * .55f)
        shapes.circle(w * .27f + drift * 1.3f, .53f + .07f * erupt, .012f)
    }

    private fun drawFrostPeaks(shapes: ShapeRenderer, w: Float, time: Float, a: Float) {
        ridge(shapes, w, -.42f, .19f, .20f, .12f, .20f, a)
        fill(shapes, .20f, .34f, .48f, a * .72f)
        shapes.triangle(-w, -.40f, -w * .63f, .44f, -w * .25f, -.40f)
        shapes.triangle(-w * .46f, -.40f, w * .10f, .58f, w * .69f, -.40f)
        shapes.triangle(w * .25f, -.40f, w * .67f, .40f, w, -.40f)
        fill(shapes, .82f, .94f, 1f, a * .9f)
        shapes.triangle(-w * .76f, .20f, -w * .63f, .44f, -w * .51f, .18f)
        shapes.triangle(-w * .09f, .34f, w * .10f, .58f, w * .29f, .32f)
        shapes.triangle(w * .55f, .17f, w * .67f, .40f, w * .79f, .17f)
        fill(shapes, .025f, .13f, .19f, a * .82f)
        for (i in 0 until 9) {
            val x = -w + (i + .5f) * (2f * w / 9f)
            val height = .11f + (i % 3) * .045f
            shapes.triangle(x - .055f, -.38f, x, -.38f + height, x + .055f, -.38f)
            shapes.triangle(x - .04f, -.31f, x, -.31f + height * .72f, x + .04f, -.31f)
        }
        fill(shapes, .65f, .88f, 1f, a * (.12f + .08f * (.5f + .5f * sin(time * .5f))))
        shapes.rect(-w, .69f, 2f * w, .012f)
    }

    private fun drawJungle(shapes: ShapeRenderer, w: Float, time: Float, a: Float) {
        ridge(shapes, w, -.42f, .12f, .13f, .18f, .24f, a)
        fill(shapes, .045f, .20f, .085f, a * .94f)
        for (i in 0 until 11) {
            val x = -w + i * (2f * w / 10f)
            val y = -.27f + (i % 3) * .035f
            val r = .12f + (i % 4) * .025f
            shapes.circle(x, y, r)
            shapes.circle(x + r * .75f, y + .03f, r * .82f)
        }
        fill(shapes, .10f, .38f, .12f, a * .84f)
        for (i in 0 until 8) {
            val x = -w + (i + .5f) * (2f * w / 8f)
            val sway = .018f * sin(time * .6f + i)
            shapes.rect(x - .012f + sway, -.44f, .024f, .25f + (i % 3) * .04f)
            shapes.triangle(x - .12f + sway, -.20f, x + sway, .02f + (i % 2) * .04f, x + .12f + sway, -.20f)
        }
    }

    private fun drawCity(shapes: ShapeRenderer, w: Float, time: Float, a: Float) {
        fill(shapes, .025f, .07f, .14f, a * .85f)
        shapes.rect(-w, -.43f, 2f * w, .30f)
        for (i in 0 until 13) {
            val width = 2f * w / 15f
            val x = -w + i * (width + width * .16f)
            val h = .17f + ((i * 13 + 5) % 6) * .055f
            fill(shapes, .025f + (i % 3) * .012f, .10f, .22f + (i % 2) * .06f, a * .86f)
            shapes.rect(x, -.22f, width, h)
            if (i % 4 == 0) {
                fill(shapes, .08f, .85f, 1f, a * (.38f + .16f * sin(time + i)))
                shapes.rect(x + width * .48f, -.20f + h, width * .08f, .08f)
            }
            fill(shapes, .25f, .72f, 1f, a * .56f)
            for (row in 0..2) if ((i + row) % 3 != 0)
                shapes.rect(x + width * .22f, -.16f + row * .055f, width * .12f, .022f)
        }
        fill(shapes, .02f, .7f, .95f, a * .52f)
        shapes.rect(-w, -.435f, 2f * w, .012f)
    }

    private fun drawGalaxy(shapes: ShapeRenderer, w: Float, time: Float, a: Float) {
        // Milky nebula band, rotating orbit arcs and three changing planet vistas.
        fill(shapes, .17f, .035f, .39f, a * .12f)
        shapes.circle(-w * .12f, .22f, .47f)
        fill(shapes, .035f, .34f, .60f, a * .12f)
        shapes.circle(w * .27f, .32f, .33f)
        fill(shapes, .75f, .18f, .42f, a * .10f)
        shapes.circle(w * .57f, .20f, .25f)
        drawRing(shapes, -w * .46f, .35f, .36f, .11f, .015f, .30f, .64f, 1f, a * .72f, 28)
        drawPlanet(shapes, -w * .60f, .19f, .12f, .12f, .54f, .98f, .82f, a * .86f, false)
        val orbit = .025f * sin(time * .32f)
        drawPlanet(shapes, w * .15f, .25f + orbit, .19f, .08f, .58f, .22f, 1f, a * .92f, true)
        drawPlanet(shapes, w * .68f, .37f - orbit, .13f, .45f, .19f, .82f, 1f, a * .90f, true)
        drawPlanet(shapes, -w * .06f, -.05f, .085f, .98f, .28f, .12f, 1f, a * .88f, false)
        fill(shapes, .70f, .87f, 1f, a * .75f)
        for (i in 0 until 5) {
            val x = -w * .82f + (i * .37f) * w + .02f * sin(time * .3f + i)
            val y = .54f + (i % 2) * .12f
            shapes.circle(x, y, .012f + (i % 2) * .004f)
        }
        fill(shapes, .28f, .89f, 1f, a * .55f)
        shapes.rect(-w, -.43f, 2f * w, .018f)
        shapes.rect(-w, -.47f, 2f * w, .008f)
    }

    private fun drawRailway(shapes: ShapeRenderer, w: Float, time: Float, a: Float) {
        ridge(shapes, w, -.40f, .08f, .11f, .15f, .18f, a)
        fill(shapes, .09f, .13f, .18f, a * .94f)
        shapes.rect(-w, -.37f, 2f * w, .16f)
        for (i in 0 until 5) {
            val x = -w * .9f + i * (w * 1.8f / 4f)
            val h = .24f + (i % 3) * .06f
            fill(shapes, .13f, .20f, .26f, a * .9f)
            shapes.rect(x, -.22f, .12f, h)
            shapes.rect(x - .06f, -.22f + h, .24f, .025f)
        }
        fill(shapes, .95f, .35f, .08f, a * (.52f + .16f * sin(time * 2f)))
        shapes.rect(-w * .75f, -.32f, w * 1.48f, .016f)
        fill(shapes, .42f, .52f, .60f, a * .88f)
        shapes.rect(-w, -.40f, 2f * w, .022f)
    }

    private fun drawBeach(shapes: ShapeRenderer, w: Float, time: Float, a: Float) {
        fill(shapes, .96f, .54f, .20f, a * .84f)
        shapes.circle(w * .56f, .48f, .14f)
        fill(shapes, .025f, .37f, .70f, a * .82f)
        shapes.rect(-w, -.42f, 2f * w, .28f)
        fill(shapes, .07f, .68f, .84f, a * .82f)
        for (i in 0..2) shapes.rect(-w, -.38f + i * .075f, 2f * w, .014f)
        fill(shapes, .11f, .34f, .24f, a * .94f)
        shapes.triangle(-w * .75f, -.17f, -w * .35f, .10f, w * .08f, -.17f)
        shapes.triangle(w * .05f, -.17f, w * .44f, .04f, w * .94f, -.17f)
        fill(shapes, .29f, .17f, .075f, a)
        for (i in 0..1) {
            val px = w * if (i == 0) -.72f else .84f
            shapes.rect(px - .012f, -.18f, .024f, .38f)
            fill(shapes, .06f, .53f, .22f, a)
            shapes.triangle(px - .20f, .16f, px, .27f, px + .18f, .11f)
            shapes.triangle(px - .16f, .20f, px - .02f, .30f, px + .06f, .12f)
            fill(shapes, .29f, .17f, .075f, a)
        }
        fill(shapes, .76f, .95f, .98f, a * (.45f + .12f * sin(time * 1.2f)))
        shapes.rect(-w, -.115f, 2f * w, .009f)
    }

    private fun drawMoonlit(shapes: ShapeRenderer, w: Float, time: Float, a: Float) {
        drawPlanet(shapes, w * .55f, .54f, .14f, .72f, .82f, .98f, 1f, a * .93f, false)
        fill(shapes, .012f, .018f, .04f, a * .90f)
        shapes.circle(w * .60f, .58f, .13f)
        ridge(shapes, w, -.39f, .12f, .16f, .12f, .23f, a)
        fill(shapes, .014f, .022f, .05f, a * .96f)
        for (i in 0 until 4) {
            val px = w * (-.72f + i * .535f)
            shapes.rect(px - .012f, -.40f, .024f, .28f)
            shapes.triangle(px - .12f, -.18f, px, .02f, px + .12f, -.18f)
            shapes.triangle(px - .09f, -.08f, px, .12f, px + .09f, -.08f)
        }
        fill(shapes, 1f, .75f, .34f, a * (.45f + .18f * (.5f + .5f * sin(time * 2f))))
        shapes.circle(-w * .18f, -.22f, .009f)
        shapes.circle(w * .42f, -.25f, .009f)
    }

    private fun drawMatrix(shapes: ShapeRenderer, w: Float, time: Float, a: Float) {
        fill(shapes, .005f, .055f, .035f, a * .96f)
        for (column in 0 until 18) {
            val x = -w + (column + .5f) * (2f * w / 18f)
            val phase = (time * (.10f + column % 4 * .035f) + column * .137f) % 1f
            val base = .46f - phase * .82f
            for (row in 0 until 4) {
                val y = base - row * .045f
                if (y < -.30f || y > .58f) continue
                fill(shapes, .14f, 1f, .39f, a * (row == 0).let { if (it) .78f else .36f })
                shapes.rect(x, y, .008f + (column % 2) * .004f, .028f)
            }
        }
        fill(shapes, .02f, .16f, .09f, a * .78f)
        shapes.rect(-w, -.38f, 2f * w, .13f)
        fill(shapes, .96f, .08f, .67f, a * (.28f + .2f * sin(time * 1.7f)))
        shapes.rect(-w, -.245f, 2f * w, .008f)
    }

    private fun drawAir(shapes: ShapeRenderer, w: Float, time: Float, a: Float) {
        fill(shapes, 1f, .82f, .43f, a * .80f)
        shapes.circle(-w * .62f, .53f, .13f)
        fill(shapes, .82f, .95f, 1f, a * .72f)
        for (i in 0 until 5) {
            val x = -w * .9f + i * (w * .42f) + .03f * sin(time * .3f + i)
            val y = .10f + (i % 3) * .095f
            shapes.circle(x, y, .11f + (i % 2) * .035f)
            shapes.circle(x + .09f, y - .015f, .08f)
            shapes.rect(x - .02f, y - .07f, .18f, .08f)
        }
        fill(shapes, .34f, .58f, .75f, a * .56f)
        ridge(shapes, w, -.42f, .08f, .08f, .09f, .10f, a)
    }

    private fun drawNexus(shapes: ShapeRenderer, w: Float, time: Float, a: Float) {
        ridge(shapes, w, -.39f, .08f, .12f, .09f, .15f, a)
        fill(shapes, .008f, .025f, .04f, a * .95f)
        shapes.rect(-w, -.40f, 2f * w, .16f)
        drawRing(shapes, -w * .46f, .19f, .27f, .36f, .014f, .10f, .90f, 1f, a * .86f, 18)
        drawRing(shapes, w * .55f, .25f, .20f, .27f, .012f, .78f, .17f, 1f, a * .82f, 18)
        fill(shapes, .18f, .95f, 1f, a * (.3f + .25f * sin(time * 2f)))
        for (i in 0 until 8) {
            val x = -w * .86f + i * (w * 1.72f / 7f)
            val y = -.22f + (i % 3) * .10f
            shapes.rect(x, y, .018f, .07f + (i % 2) * .05f)
        }
        fill(shapes, .85f, .12f, .93f, a * .65f)
        shapes.triangle(-w * .08f, .39f, w * .03f, .08f, w * .13f, .39f)
    }

    private fun drawHighway(shapes: ShapeRenderer, w: Float, time: Float, a: Float) {
        fill(shapes, .08f, .13f, .19f, a * .84f)
        for (i in 0 until 9) {
            val x = -w + i * (2f * w / 9f)
            val h = .12f + (i % 4) * .075f
            shapes.rect(x, -.40f, .035f, h)
            fill(shapes, 1f, .60f, .14f, a * (.4f + .15f * sin(time * 1.5f + i)))
            shapes.circle(x + .018f, -.40f + h, .012f)
        }
        ridge(shapes, w, -.44f, .05f, .07f, .09f, .10f, a)
        fill(shapes, .84f, .87f, .89f, a * .76f)
        shapes.rect(-w, -.25f, 2f * w, .008f)
    }

    private fun drawCandy(shapes: ShapeRenderer, w: Float, time: Float, a: Float) {
        fill(shapes, .24f, .08f, .28f, a * .75f)
        ridge(shapes, w, -.42f, .12f, .12f, .15f, .18f, a)
        for (i in 0 until 5) {
            val x = -w * .82f + i * (w * .41f)
            val c = candyColors[i % candyColors.size]
            fill(shapes, c[0], c[1], c[2], a * .9f)
            shapes.circle(x, -.18f + .02f * sin(time + i), .09f)
            fill(shapes, .92f, .88f, 1f, a * .85f)
            shapes.circle(x + .025f, -.15f, .026f)
        }
    }

    private fun ridge(shapes: ShapeRenderer, w: Float, base: Float, r: Float, g: Float, b: Float, peak: Float, a: Float) {
        fill(shapes, r, g, b, a * .50f)
        shapes.rect(-w, base - .015f, 2f * w, .04f)
        for (i in 0 until 7) {
            val x = -w + (i + .4f) * (2f * w / 7f)
            val height = peak * (.60f + (i % 3) * .17f)
            val half = 2f * w / 8.3f
            shapes.triangle(x - half, base, x, base + height, x + half, base)
        }
    }

    private fun drawPlanet(shapes: ShapeRenderer, x: Float, y: Float, r: Float, red: Float, green: Float, blue: Float,
                           light: Float, a: Float, ring: Boolean) {
        if (ring) drawRing(shapes, x, y, r * 1.65f, r * .48f, r * .12f, red, green, blue, a * .72f, 22)
        fill(shapes, red, green, blue, a)
        shapes.circle(x, y, r)
        fill(shapes, minOf(1f, red * light + .12f), minOf(1f, green * light + .12f), minOf(1f, blue * light + .12f), a * .62f)
        shapes.circle(x - r * .28f, y + r * .24f, r * .64f)
        fill(shapes, .025f, .045f, .12f, a * .42f)
        shapes.circle(x + r * .38f, y - r * .25f, r * .50f)
    }

    private fun drawRing(shapes: ShapeRenderer, cx: Float, cy: Float, rx: Float, ry: Float, thick: Float,
                         red: Float, green: Float, blue: Float, a: Float, segments: Int) {
        fill(shapes, red, green, blue, a)
        val step = TAU / segments
        for (i in 0 until segments) {
            val a0 = i * step
            val a1 = (i + 1) * step
            val x0 = cx + cos(a0) * rx
            val y0 = cy + sin(a0) * ry
            val x1 = cx + cos(a1) * rx
            val y1 = cy + sin(a1) * ry
            val ix0 = cx + cos(a0) * (rx - thick)
            val iy0 = cy + sin(a0) * (ry - thick * .55f)
            val ix1 = cx + cos(a1) * (rx - thick)
            val iy1 = cy + sin(a1) * (ry - thick * .55f)
            shapes.triangle(x0, y0, x1, y1, ix0, iy0)
            shapes.triangle(x1, y1, ix1, iy1, ix0, iy0)
        }
    }

    private fun fill(shapes: ShapeRenderer, r: Float, g: Float, b: Float, a: Float) {
        shapes.color.set(r, g, b, a.coerceIn(0f, 1f))
    }

    private companion object {
        const val DEG = .008726646f
        const val TAU = 6.2831855f
    }
}
