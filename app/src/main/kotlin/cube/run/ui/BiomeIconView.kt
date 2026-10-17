package cube.run.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import cube.run.data.Worlds
import kotlin.math.min

/** Small, offline biome glyph used by the starting-world cards. */
@Suppress("ViewConstructor")
internal class BiomeIconView(context: Context, private val worldId: Int) : View(context) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private val accent = if (worldId >= 0) {
        val world = Worlds.get(worldId)
        Color.HSVToColor(floatArrayOf(world.postH, world.postS.coerceIn(.48f, .95f), world.postV.coerceIn(.68f, 1f)))
    } else Color.rgb(155, 112, 255)

    init {
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        if (size <= 0f) return
        val cx = width * .5f
        val cy = height * .5f
        val pad = size * .04f
        fill.style = Paint.Style.FILL
        fill.color = Color.argb(82, Color.red(accent), Color.green(accent), Color.blue(accent))
        canvas.drawRoundRect(pad, pad, width - pad, height - pad, size * .28f, size * .28f, fill)
        stroke.color = Color.argb(150, Color.red(accent), Color.green(accent), Color.blue(accent))
        stroke.strokeWidth = size * .045f
        canvas.drawRoundRect(pad, pad, width - pad, height - pad, size * .28f, size * .28f, stroke)
        fill.color = Color.WHITE
        stroke.color = Color.WHITE
        stroke.strokeWidth = size * .075f

        val deco = if (worldId >= 0) Worlds.get(worldId).deco else -1
        when (deco) {
            Worlds.LOLLIPOPS -> { // a candy lollipop
                canvas.drawCircle(cx - size * .08f, cy - size * .10f, size * .20f, fill)
                canvas.drawLine(cx - size * .08f, cy + size * .08f, cx + size * .07f, cy + size * .32f, stroke)
                canvas.drawCircle(cx + size * .20f, cy - size * .20f, size * .055f, fill)
            }
            Worlds.TOWERS -> for (i in 0..2) {
                val w = size * .18f
                val x = cx - size * .31f + i * size * .22f
                val h = size * (.30f + (i % 2) * .18f)
                canvas.drawRoundRect(x, cy + size * .31f - h, x + w, cy + size * .31f, size * .035f, size * .035f, fill)
                canvas.drawRect(x + w * .28f, cy + size * .18f - h, x + w * .48f, cy + size * .23f - h, stroke)
            }
            Worlds.CRYSTALS, Worlds.SPIKES -> {
                path.reset(); path.moveTo(cx - size * .31f, cy + size * .27f); path.lineTo(cx - size * .11f, cy - size * .25f)
                path.lineTo(cx + size * .02f, cy + size * .27f); path.close(); canvas.drawPath(path, fill)
                path.reset(); path.moveTo(cx - size * .03f, cy + size * .27f); path.lineTo(cx + size * .16f, cy - size * .34f)
                path.lineTo(cx + size * .34f, cy + size * .27f); path.close(); canvas.drawPath(path, fill)
            }
            Worlds.CACTI -> {
                canvas.drawRoundRect(cx - size * .06f, cy - size * .28f, cx + size * .06f, cy + size * .32f, size * .05f, size * .05f, fill)
                canvas.drawLine(cx - size * .06f, cy - size * .02f, cx - size * .24f, cy - size * .02f, stroke)
                canvas.drawLine(cx - size * .24f, cy - size * .02f, cx - size * .24f, cy - size * .18f, stroke)
                canvas.drawLine(cx + size * .06f, cy + size * .10f, cx + size * .24f, cy + size * .10f, stroke)
                canvas.drawLine(cx + size * .24f, cy + size * .10f, cx + size * .24f, cy - size * .10f, stroke)
            }
            Worlds.RINGS -> {
                canvas.drawCircle(cx, cy, size * .20f, stroke)
                canvas.drawOval(cx - size * .34f, cy - size * .09f, cx + size * .34f, cy + size * .09f, stroke)
                canvas.drawCircle(cx + size * .23f, cy - size * .20f, size * .045f, fill)
            }
            Worlds.RAILWAY -> {
                canvas.drawLine(cx - size * .13f, cy - size * .31f, cx - size * .29f, cy + size * .31f, stroke)
                canvas.drawLine(cx + size * .13f, cy - size * .31f, cx + size * .29f, cy + size * .31f, stroke)
                for (i in 0..3) {
                    val q = (i + 1) / 5f
                    val half = size * (.12f + .16f * q)
                    val y = cy - size * .25f + i * size * .16f
                    canvas.drawLine(cx - half, y, cx + half, y, stroke)
                }
                fill.color = Color.rgb(255, 194, 57)
                canvas.drawCircle(cx + size * .30f, cy - size * .12f, size * .055f, fill)
            }
            Worlds.JUNGLE -> {
                canvas.drawRoundRect(cx - size * .055f, cy - size * .04f, cx + size * .055f, cy + size * .32f, size * .03f, size * .03f, fill)
                canvas.drawCircle(cx - size * .17f, cy - size * .12f, size * .17f, fill)
                canvas.drawCircle(cx + size * .15f, cy - size * .14f, size * .19f, fill)
            }
            Worlds.HIGHWAY -> {
                path.reset(); path.moveTo(cx - size * .08f, cy - size * .30f); path.lineTo(cx - size * .28f, cy + size * .31f)
                path.lineTo(cx + size * .28f, cy + size * .31f); path.lineTo(cx + size * .08f, cy - size * .30f); path.close()
                canvas.drawPath(path, fill)
                fill.color = accent
                canvas.drawRect(cx - size * .025f, cy - size * .19f, cx + size * .025f, cy - size * .04f, fill)
                canvas.drawRect(cx - size * .055f, cy + size * .06f, cx + size * .055f, cy + size * .21f, fill)
            }
            Worlds.BEACH -> {
                canvas.drawCircle(cx + size * .19f, cy - size * .20f, size * .12f, fill)
                stroke.strokeWidth = size * .055f
                for (i in 0..1) {
                    val y = cy + size * (.04f + i * .15f)
                    canvas.drawLine(cx - size * .30f, y, cx - size * .08f, y + size * .045f, stroke)
                    canvas.drawLine(cx - size * .08f, y + size * .045f, cx + size * .13f, y, stroke)
                    canvas.drawLine(cx + size * .13f, y, cx + size * .31f, y + size * .045f, stroke)
                }
            }
            Worlds.NIGHT -> {
                fill.color = Color.rgb(221, 234, 255)
                canvas.drawCircle(cx - size * .10f, cy - size * .13f, size * .20f, fill)
                fill.color = Color.rgb(12, 14, 34)
                canvas.drawCircle(cx - size * .03f, cy - size * .19f, size * .18f, fill)
                stroke.strokeWidth = size * .055f
                canvas.drawLine(cx + size * .22f, cy + size * .28f, cx + size * .22f, cy - size * .20f, stroke)
                canvas.drawLine(cx + size * .30f, cy + size * .28f, cx + size * .30f, cy - size * .28f, stroke)
                fill.color = Color.rgb(255, 220, 141)
                canvas.drawCircle(cx + size * .22f, cy - size * .21f, size * .045f, fill)
                canvas.drawCircle(cx + size * .30f, cy - size * .29f, size * .045f, fill)
            }
            Worlds.HACKER -> {
                fill.color = Color.rgb(4, 30, 19)
                canvas.drawRoundRect(cx - size * .34f, cy - size * .30f, cx + size * .34f, cy + size * .30f,
                    size * .08f, size * .08f, fill)
                fill.color = Color.rgb(62, 255, 139)
                for (column in 0..3) for (row in 0..2) {
                    if ((column + row) % 3 != 0) {
                        val x = cx - size * .25f + column * size * .15f
                        val y = cy - size * .23f + row * size * .17f
                        canvas.drawRect(x, y, x + size * .045f, y + size * .06f, fill)
                    }
                }
                stroke.color = Color.rgb(177, 255, 207)
                stroke.strokeWidth = size * .06f
                canvas.drawLine(cx, cy + size * .28f, cx, cy + size * .02f, stroke)
                canvas.drawLine(cx, cy + size * .02f, cx - size * .25f, cy - size * .19f, stroke)
                canvas.drawLine(cx, cy + size * .02f, cx + size * .25f, cy - size * .19f, stroke)
            }
            Worlds.NEXUS -> {
                fill.color = Color.rgb(4, 20, 31)
                canvas.drawRoundRect(cx - size * .31f, cy - size * .30f, cx + size * .31f, cy + size * .30f,
                    size * .08f, size * .08f, fill)
                stroke.color = Color.rgb(52, 236, 255)
                stroke.strokeWidth = size * .055f
                path.reset(); path.moveTo(cx - size * .23f, cy - size * .20f)
                path.lineTo(cx - size * .02f, cy - size * .20f); path.lineTo(cx - size * .02f, cy - size * .02f)
                path.lineTo(cx + size * .20f, cy - size * .02f); path.lineTo(cx + size * .20f, cy + size * .20f)
                canvas.drawPath(path, stroke)
                stroke.color = Color.rgb(220, 77, 255)
                path.reset(); path.moveTo(cx - size * .22f, cy + size * .19f)
                path.lineTo(cx - size * .12f, cy + size * .03f); path.lineTo(cx + size * .02f, cy + size * .14f)
                path.lineTo(cx + size * .11f, cy + size * .04f)
                canvas.drawPath(path, stroke)
                fill.color = Color.rgb(127, 255, 191)
                canvas.drawRect(cx + size * .08f, cy - size * .25f, cx + size * .17f, cy - size * .16f, fill)
            }
            Worlds.AIR -> {
                fill.color = Color.rgb(224, 248, 255)
                canvas.drawCircle(cx - size * .17f, cy + size * .05f, size * .14f, fill)
                canvas.drawCircle(cx, cy - size * .02f, size * .20f, fill)
                canvas.drawCircle(cx + size * .18f, cy + size * .06f, size * .14f, fill)
                canvas.drawRect(cx - size * .18f, cy + size * .04f, cx + size * .19f, cy + size * .20f, fill)
                fill.color = Color.rgb(23, 92, 144)
                stroke.color = Color.rgb(23, 92, 144)
                stroke.strokeWidth = size * .06f
                path.reset(); path.moveTo(cx - size * .24f, cy - size * .19f)
                path.lineTo(cx - size * .07f, cy - size * .10f); path.lineTo(cx, cy - size * .23f)
                path.lineTo(cx + size * .08f, cy - size * .11f); path.lineTo(cx + size * .27f, cy - size * .20f)
                canvas.drawPath(path, stroke)
                fill.color = Color.rgb(255, 184, 54)
                canvas.drawCircle(cx + size * .28f, cy - size * .20f, size * .045f, fill)
            }
            else -> { // random rotation: a simple four-point star
                path.reset(); path.moveTo(cx, cy - size * .29f); path.lineTo(cx + size * .08f, cy - size * .08f)
                path.lineTo(cx + size * .29f, cy); path.lineTo(cx + size * .08f, cy + size * .08f)
                path.lineTo(cx, cy + size * .29f); path.lineTo(cx - size * .08f, cy + size * .08f)
                path.lineTo(cx - size * .29f, cy); path.lineTo(cx - size * .08f, cy - size * .08f)
                path.close(); canvas.drawPath(path, fill)
            }
        }
    }
}
