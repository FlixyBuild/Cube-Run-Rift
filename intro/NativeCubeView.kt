package cube.run.intro

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.view.View
import cube.run.core.LaunchTrace
import cube.run.data.Skins
import cube.run.data.Worlds
import kotlin.math.*

/** Antialiased lit 3D cube: twelve projected quads, no GL, textures, or media decoder. */
@android.annotation.SuppressLint("ViewConstructor") // Constructed with the shared shot clock, never inflated from XML.
class NativeCubeView(context: Context, val clock: OpeningClock, private val skin: Skins.Skin,
                     private val worldHue: Float) : View(context) {
    private val pose = OpeningPose()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val backdropFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backdropStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val backdropPath = Path()
    private val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
    }
    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textScaleX = 1.05f
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val vertices = FloatArray(24)
    private val projected = FloatArray(16)
    private val hsv = FloatArray(3)
    private var first = true
    private var moving = false
    private var traceName = "native cube"
    var onFirstDraw: (() -> Unit)? = null
    var secondsForTest: Float? = null
    var drawingCube = true
        set(value) { field = value; invalidate() }

    /** Same live pose, briefly drawn inside Android's copied splash during surface transfer. */
    internal fun handoffCopy() = NativeCubeView(context, clock, skin, worldHue).apply { traceName = "splash cube" }

    override fun onDraw(canvas: Canvas) {
        if (!drawingCube) return
        clock.start()
        val t = secondsForTest ?: clock.sceneSeconds()
        val spin = secondsForTest ?: clock.motionSeconds()
        pose.update(t, height/resources.displayMetrics.density, spin)
        drawBiomeBackdrop(canvas, t)
        val yaw = Math.toRadians(pose.yaw.toDouble()).toFloat()
        val tilt = Math.toRadians(pose.tilt.toDouble()).toFloat()
        val cy = cos(yaw); val sy = sin(yaw); val ct = cos(tilt); val st = sin(tilt)
        val pitchSin = sin(pose.pitch); val pitchCos = cos(pose.pitch)
        val focal = height/(2f*tan(pose.fov/2f))
        val mix = clock.skinAmount()
        val source = clock.launchAppearance
        val opacity = source.skin.opacity+(skin.opacity-source.skin.opacity)*mix
        for (shell in 0..1) {
            val sx = if (shell == 0) pose.scaleX else pose.shellScale
            val sz = sx
            val scaleY = if (shell == 0) pose.scaleY else pose.shellScale
            for (i in 0..7) {
                val x = (if (i and 1 == 0) -.5f else .5f)*sx
                val y = (if (i and 2 == 0) -.5f else .5f)*scaleY
                val z = (if (i and 4 == 0) -.5f else .5f)*sz
                val rx = x*ct-y*st; val ry = x*st+y*ct
                val wx = rx*cy+z*sy; val wy = ry+.45f; val wz = -rx*sy+z*cy
                vertices[i*3] = wx; vertices[i*3+1] = wy; vertices[i*3+2] = wz
                val dy = wy-pose.cameraY; val dz = wz-pose.cameraZ
                val depth = dy*pitchSin-dz*pitchCos
                projected[i*2] = width/2f+wx*focal/depth
                projected[i*2+1] = height/2f-(dy*pitchCos+dz*pitchSin)*focal/depth
            }
            hsv[0] = ((skin.hueAt(spin, worldHue)%360f)+360f)%360f
            hsv[1] = skin.sat*(if (shell == 0) 1f else .9f)
            hsv[2] = if (shell == 0) skin.valueAt(spin) else 1f
            val equipped = Color.HSVToColor(hsv)
            val from = source.color(spin, shell == 1)
            val base = Color.rgb(
                (Color.red(from)+(Color.red(equipped)-Color.red(from))*mix).roundToInt(),
                (Color.green(from)+(Color.green(equipped)-Color.green(from))*mix).roundToInt(),
                (Color.blue(from)+(Color.blue(equipped)-Color.blue(from))*mix).roundToInt())
            val targetShell = ((.22f+.08f*sin(spin*6f))*skin.glow).coerceAtMost(.75f)*(if (skin.opacity < 1f) .35f else 1f)
            val alpha = if (shell == 0) opacity else
                source.shellOpacity(spin)+(targetShell-source.shellOpacity(spin))*mix
            for (face in 0..5) {
                val normal = normals[face]
                val rx = normal[0]*ct-normal[1]*st; val ny = normal[0]*st+normal[1]*ct
                val nx = rx*cy+normal[2]*sy; val nz = -rx*sy+normal[2]*cy
                val point = faces[face][0]*3
                if (nx*(-vertices[point])+ny*(pose.cameraY-vertices[point+1])+nz*(pose.cameraZ-vertices[point+2]) <= 0f) continue
                val l1 = max(0f, (nx*.45f+ny*.85f+nz*.35f)/L1_LENGTH)
                val l2 = max(0f, (-nx*.6f+ny*.2f-nz*.5f)/L2_LENGTH)
                val emissive = if (shell == 1) 0f else (if (source.skin.id == 13) 1f-mix else 0f)+(if (skin.id == 13) mix else 0f)
                fun channel(value: Int, ambient: Float, light1: Float, light2: Float, emission: Float): Int =
                    (value*(ambient+l1*light1+l2*light2)+emissive*emission*255f).roundToInt().coerceIn(0, 255)
                paint.color = Color.argb((alpha*255f).roundToInt(),
                    channel(Color.red(base), .55f, .85f, .25f, .24f),
                    channel(Color.green(base), .55f, .85f, .22f, .25f),
                    channel(Color.blue(base), .6f, .8f, .3f, .26f))
                path.rewind()
                for ((index, vertex) in faces[face].withIndex()) {
                    if (index == 0) path.moveTo(projected[vertex*2], projected[vertex*2+1])
                    else path.lineTo(projected[vertex*2], projected[vertex*2+1])
                }
                path.close(); canvas.drawPath(path, paint)
            }
        }
        if (first) { first = false; LaunchTrace.mark("$traceName draw"); onFirstDraw?.invoke() }
        if (!moving && t > .016f) { moving = true; LaunchTrace.mark("$traceName motion") }
        if (secondsForTest == null && t < OpeningPose.DURATION) postInvalidateOnAnimation()
    }

    /** Redesigned launch mural: eight campaign worlds, preserved classic themes, a clean road and a route-progress bar. */
    private fun drawBiomeBackdrop(canvas: Canvas, seconds: Float) {
        canvas.drawColor(OpeningPose.INK)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val reveal = OpeningPose.ease((seconds - .05f) / .72f)
        if (reveal <= 0f) return

        brandPaint.color = backdropColor(0xfff7f6ff.toInt(), .96f * reveal)
        brandPaint.textSize = w * .055f
        brandPaint.setShadowLayer(w * .018f, 0f, w * .006f, backdropColor(0xff4ce4ff.toInt(), .7f * reveal))
        canvas.drawText("CUBE RUN", w * .5f, h * .047f, brandPaint)
        brandPaint.clearShadowLayer()
        subtitlePaint.color = backdropColor(0xffa9dfff.toInt(), .84f * reveal)
        subtitlePaint.textSize = w * .018f
        canvas.drawText("PORTAL RUNNER  •  EIGHT WORLDS IN ROTATION", w * .5f, h * .073f, subtitlePaint)

        val accents = BIOME_ACCENTS
        val cardW = w * .15f
        val cardHeight = h * .215f
        val firstCardTop = h * .095f
        val rowStep = h * .245f
        val radius = cardW * .16f
        for (world in Worlds.all.indices) {
            val column = world % 6
            val row = world / 6
            val cx = w * (column + .5f) / 6f
            val cardTop = firstCardTop + row * rowStep
            val cardBottom = cardTop + cardHeight
            val campaignMap = Worlds.isCampaignWorld(world)
            backdropFill.style = Paint.Style.FILL
            backdropFill.color = backdropColor(0xff211d48.toInt(), (if (campaignMap) .58f else .36f) * reveal)
            canvas.drawRoundRect(cx - cardW * .5f, cardTop, cx + cardW * .5f, cardBottom, radius, radius, backdropFill)
            backdropFill.color = backdropColor(accents[world], (if (campaignMap) .12f else .06f) * reveal)
            canvas.drawRoundRect(cx - cardW * .5f, cardTop, cx + cardW * .5f, cardBottom, radius, radius, backdropFill)
            backdropStroke.color = backdropColor(accents[world], (if (campaignMap) .68f else .30f) * reveal)
            backdropStroke.strokeWidth = max(1f, w * .005f)
            canvas.drawRoundRect(cx - cardW * .5f, cardTop, cx + cardW * .5f, cardBottom, radius, radius, backdropStroke)
            drawBiomeMark(canvas, world, cx, cardTop, cardBottom, cardW, accents[world], reveal)
        }

        // The shared runner's road converges beneath the foreground cube.
        val roadTop = h * .60f
        backdropPath.reset()
        backdropPath.moveTo(w * .5f, roadTop)
        backdropPath.lineTo(w * .075f, h)
        backdropPath.lineTo(w * .925f, h)
        backdropPath.close()
        backdropFill.color = backdropColor(0xff100e26.toInt(), .93f * reveal)
        canvas.drawPath(backdropPath, backdropFill)
        backdropStroke.strokeWidth = max(1f, w * .006f)
        backdropStroke.color = backdropColor(0xff4ce4ff.toInt(), .58f * reveal)
        canvas.drawLine(w * .5f, roadTop, w * .075f, h, backdropStroke)
        backdropStroke.color = backdropColor(0xffff6ac8.toInt(), .52f * reveal)
        canvas.drawLine(w * .5f, roadTop, w * .925f, h, backdropStroke)
        backdropStroke.color = backdropColor(0xffb8caff.toInt(), .32f * reveal)
        for (dash in 1..4) {
            val q = dash / 5f
            val y = roadTop + (h - roadTop) * q
            val half = w * (.012f + .13f * q)
            canvas.drawLine(w * .5f - half, y, w * .5f + half, y, backdropStroke)
        }

        // The opening sequence itself doubles as a lightweight route-progress cue.
        val barW = w * .42f
        val barH = max(2f, w * .012f)
        val barX = (w - barW) * .5f
        val barY = h * .965f
        progressPaint.color = backdropColor(0xff8eb2d6.toInt(), .32f * reveal)
        canvas.drawRoundRect(barX, barY, barX + barW, barY + barH, barH, barH, progressPaint)
        val progress = (seconds / OpeningPose.DURATION).coerceIn(0f, 1f)
        progressPaint.color = backdropColor(0xff55e6ff.toInt(), .96f * reveal)
        canvas.drawRoundRect(barX, barY, barX + barW * progress, barY + barH, barH, barH, progressPaint)
    }

    private fun drawBiomeMark(canvas: Canvas, world: Int, cx: Float, top: Float, bottom: Float,
                              span: Float, accent: Int, reveal: Float) {
        val sceneH = bottom - top
        val ground = bottom - sceneH * .10f
        backdropFill.style = Paint.Style.FILL
        backdropStroke.strokeWidth = max(1f, span * .035f)
        when (world) {
            0 -> { // original Candy Fields: gumdrops and lollipops
                backdropFill.color = backdropColor(0xffff78c8.toInt(), .84f * reveal)
                canvas.drawCircle(cx - span * .25f, top + sceneH * .33f, span * .15f, backdropFill)
                backdropFill.color = backdropColor(0xff74edc1.toInt(), .82f * reveal)
                canvas.drawCircle(cx + span * .22f, top + sceneH * .46f, span * .18f, backdropFill)
                backdropFill.color = backdropColor(0xffffcf63.toInt(), .84f * reveal)
                canvas.drawCircle(cx + span * .08f, top + sceneH * .69f, span * .12f, backdropFill)
                backdropStroke.color = backdropColor(0xfff8e8ff.toInt(), .88f * reveal)
                backdropStroke.strokeWidth = max(1f, span * .04f)
                canvas.drawLine(cx - span * .25f, top + sceneH * .49f, cx - span * .20f, ground, backdropStroke)
                canvas.drawLine(cx + span * .22f, top + sceneH * .64f, cx + span * .25f, ground, backdropStroke)
            }
            1 -> { // Neon City towers with lit windows
                for (tower in 0..2) {
                    val tw = span * .22f
                    val x = cx - span * .38f + tower * span * .29f
                    val th = sceneH * (.34f + (tower % 2) * .15f)
                    backdropFill.color = backdropColor(if (tower == 1) 0xff43316f.toInt() else 0xff29254f.toInt(), .92f * reveal)
                    canvas.drawRoundRect(x, ground - th, x + tw, ground, span * .035f, span * .035f, backdropFill)
                    backdropFill.color = backdropColor(accent, .82f * reveal)
                    val window = span * .035f
                    for (row in 0..2) {
                        val wy = ground - th + sceneH * .07f + row * sceneH * .075f
                        canvas.drawRect(x + tw * .28f, wy, x + tw * .28f + window, wy + window, backdropFill)
                        canvas.drawRect(x + tw * .66f, wy, x + tw * .66f + window, wy + window, backdropFill)
                    }
                }
            }
            2 -> { // original Lava Caves: dark rock walls and a glowing magma seam
                backdropFill.color = backdropColor(0xff4b2247.toInt(), .88f * reveal)
                backdropPath.reset()
                backdropPath.moveTo(cx - span * .49f, ground)
                backdropPath.lineTo(cx - span * .36f, top + sceneH * .20f)
                backdropPath.lineTo(cx - span * .19f, top + sceneH * .49f)
                backdropPath.lineTo(cx, top + sceneH * .25f)
                backdropPath.lineTo(cx + span * .18f, top + sceneH * .47f)
                backdropPath.lineTo(cx + span * .36f, top + sceneH * .14f)
                backdropPath.lineTo(cx + span * .49f, ground)
                backdropPath.close()
                canvas.drawPath(backdropPath, backdropFill)
                backdropFill.color = backdropColor(0xffff6b36.toInt(), .92f * reveal)
                backdropPath.reset()
                backdropPath.moveTo(cx - span * .45f, ground - sceneH * .05f)
                backdropPath.quadTo(cx - span * .18f, ground - sceneH * .18f, cx, ground - sceneH * .05f)
                backdropPath.quadTo(cx + span * .23f, ground + sceneH * .01f, cx + span * .45f, ground - sceneH * .13f)
                backdropPath.lineTo(cx + span * .45f, ground)
                backdropPath.lineTo(cx - span * .45f, ground)
                backdropPath.close()
                canvas.drawPath(backdropPath, backdropFill)
            }
            3 -> { // Frost Peaks
                backdropFill.color = backdropColor(0xff5489bb.toInt(), .66f * reveal)
                backdropPath.reset()
                backdropPath.moveTo(cx - span * .49f, ground)
                backdropPath.lineTo(cx - span * .22f, top + sceneH * .36f)
                backdropPath.lineTo(cx - span * .06f, top + sceneH * .57f)
                backdropPath.lineTo(cx + span * .18f, top + sceneH * .22f)
                backdropPath.lineTo(cx + span * .49f, ground)
                backdropPath.close()
                canvas.drawPath(backdropPath, backdropFill)
                backdropFill.color = backdropColor(0xffe8f8ff.toInt(), .88f * reveal)
                backdropPath.reset()
                backdropPath.moveTo(cx - span * .22f, top + sceneH * .36f)
                backdropPath.lineTo(cx - span * .10f, top + sceneH * .51f)
                backdropPath.lineTo(cx - span * .035f, top + sceneH * .46f)
                backdropPath.close()
                canvas.drawPath(backdropPath, backdropFill)
                backdropPath.reset()
                backdropPath.moveTo(cx + span * .18f, top + sceneH * .22f)
                backdropPath.lineTo(cx + span * .30f, top + sceneH * .42f)
                backdropPath.lineTo(cx + span * .09f, top + sceneH * .35f)
                backdropPath.close()
                canvas.drawPath(backdropPath, backdropFill)
            }
            4 -> { // original Sunset Dunes: layered sand and a cactus silhouette
                backdropFill.color = backdropColor(0xffffdf79.toInt(), .90f * reveal)
                canvas.drawCircle(cx + span * .23f, top + sceneH * .23f, span * .12f, backdropFill)
                backdropFill.color = backdropColor(0xffb96152.toInt(), .76f * reveal)
                backdropPath.reset()
                backdropPath.moveTo(cx - span * .49f, ground)
                backdropPath.quadTo(cx - span * .18f, ground - sceneH * .23f, cx + span * .49f, ground - sceneH * .10f)
                backdropPath.lineTo(cx + span * .49f, ground)
                backdropPath.close()
                canvas.drawPath(backdropPath, backdropFill)
                backdropFill.color = backdropColor(0xffe8a95c.toInt(), .86f * reveal)
                backdropPath.reset()
                backdropPath.moveTo(cx - span * .49f, ground - sceneH * .05f)
                backdropPath.quadTo(cx + span * .03f, ground - sceneH * .30f, cx + span * .49f, ground - sceneH * .16f)
                backdropPath.lineTo(cx + span * .49f, ground)
                backdropPath.lineTo(cx - span * .49f, ground)
                backdropPath.close()
                canvas.drawPath(backdropPath, backdropFill)
                backdropStroke.color = backdropColor(0xff3c9a62.toInt(), .94f * reveal)
                backdropStroke.strokeWidth = max(1f, span * .065f)
                canvas.drawLine(cx - span * .20f, ground, cx - span * .20f, ground - sceneH * .25f, backdropStroke)
                canvas.drawLine(cx - span * .20f, ground - sceneH * .13f, cx - span * .34f, ground - sceneH * .13f, backdropStroke)
                canvas.drawLine(cx - span * .34f, ground - sceneH * .13f, cx - span * .34f, ground - sceneH * .20f, backdropStroke)
                canvas.drawLine(cx - span * .20f, ground - sceneH * .19f, cx - span * .06f, ground - sceneH * .19f, backdropStroke)
                canvas.drawLine(cx - span * .06f, ground - sceneH * .19f, cx - span * .06f, ground - sceneH * .26f, backdropStroke)
            }
            5 -> { // Riftbound Space: starfield and ringed planet
                backdropFill.color = backdropColor(0xfff2e8ff.toInt(), .84f * reveal)
                val starRadius = max(1f, span * .025f)
                canvas.drawCircle(cx - span * .34f, top + sceneH * .18f, starRadius, backdropFill)
                canvas.drawCircle(cx + span * .31f, top + sceneH * .16f, starRadius, backdropFill)
                canvas.drawCircle(cx - span * .27f, top + sceneH * .42f, starRadius, backdropFill)
                canvas.drawCircle(cx + span * .39f, top + sceneH * .49f, starRadius, backdropFill)
                canvas.drawCircle(cx + span * .02f, top + sceneH * .63f, starRadius, backdropFill)
                canvas.drawCircle(cx - span * .41f, top + sceneH * .59f, starRadius, backdropFill)
                backdropFill.color = backdropColor(0xff8954d8.toInt(), .84f * reveal)
                canvas.drawCircle(cx, top + sceneH * .38f, span * .19f, backdropFill)
                backdropStroke.color = backdropColor(accent, .92f * reveal)
                backdropStroke.strokeWidth = max(1f, span * .04f)
                canvas.drawOval(cx - span * .31f, top + sceneH * .32f, cx + span * .31f, top + sceneH * .45f, backdropStroke)
            }
            6 -> { // original Railway Yard: converging rails, sleepers and signal lights
                backdropStroke.color = backdropColor(0xff87d8e7.toInt(), .88f * reveal)
                backdropStroke.strokeWidth = max(1f, span * .035f)
                canvas.drawLine(cx - span * .10f, top + sceneH * .12f, cx - span * .44f, ground, backdropStroke)
                canvas.drawLine(cx + span * .10f, top + sceneH * .12f, cx + span * .44f, ground, backdropStroke)
                for (tie in 0..5) {
                    val q = (tie + 1) / 7f
                    val y = top + sceneH * (.12f + .76f * q)
                    val half = span * (.11f + .27f * q)
                    canvas.drawLine(cx - half, y, cx + half, y, backdropStroke)
                }
                backdropStroke.color = backdropColor(0xffffd46a.toInt(), .95f * reveal)
                backdropStroke.strokeWidth = max(1f, span * .045f)
                canvas.drawLine(cx + span * .34f, top + sceneH * .20f, cx + span * .34f, ground, backdropStroke)
                backdropFill.color = backdropColor(0xff49e48a.toInt(), .92f * reveal)
                canvas.drawCircle(cx + span * .34f, top + sceneH * .22f, span * .06f, backdropFill)
                backdropFill.color = backdropColor(0xffff596e.toInt(), .92f * reveal)
                canvas.drawCircle(cx + span * .34f, top + sceneH * .39f, span * .06f, backdropFill)
            }
            7 -> { // Jungle Canopy: blocky trees and layered foliage
                backdropFill.color = backdropColor(0xff126f54.toInt(), .78f * reveal)
                for (tree in 0..2) {
                    val tx = cx + (tree - 1) * span * .30f
                    val treeHeight = sceneH * (.26f + (tree % 2) * .10f)
                    canvas.drawRoundRect(tx - span * .045f, ground - treeHeight * .64f, tx + span * .045f, ground,
                        span * .035f, span * .035f, backdropFill)
                    backdropFill.color = backdropColor(if (tree == 1) 0xff54e98d.toInt() else accent, .72f * reveal)
                    canvas.drawRoundRect(tx - span * .20f, ground - treeHeight, tx + span * .20f, ground - treeHeight * .42f,
                        span * .12f, span * .12f, backdropFill)
                    backdropFill.color = backdropColor(0xff126f54.toInt(), .78f * reveal)
                }
                backdropFill.color = backdropColor(0xff32aa76.toInt(), .72f * reveal)
                canvas.drawRoundRect(cx - span * .45f, ground - sceneH * .14f, cx + span * .45f, ground,
                    span * .07f, span * .07f, backdropFill)
            }
            8 -> { // original Highway Rush: open asphalt, a marked lane and an overhead sign
                backdropFill.color = backdropColor(0xff17172b.toInt(), .94f * reveal)
                backdropPath.reset()
                backdropPath.moveTo(cx - span * .10f, top + sceneH * .06f)
                backdropPath.lineTo(cx + span * .10f, top + sceneH * .06f)
                backdropPath.lineTo(cx + span * .46f, ground)
                backdropPath.lineTo(cx - span * .46f, ground)
                backdropPath.close()
                canvas.drawPath(backdropPath, backdropFill)
                backdropStroke.color = backdropColor(0xffffd56d.toInt(), .88f * reveal)
                backdropStroke.strokeWidth = max(1f, span * .035f)
                canvas.drawLine(cx, top + sceneH * .09f, cx, ground, backdropStroke)
                backdropStroke.color = backdropColor(accent, .95f * reveal)
                backdropStroke.strokeWidth = max(1f, span * .03f)
                canvas.drawLine(cx + span * .32f, top + sceneH * .18f, cx + span * .32f, top + sceneH * .55f, backdropStroke)
                backdropFill.color = backdropColor(0xffef6e4c.toInt(), .95f * reveal)
                canvas.drawRoundRect(cx + span * .12f, top + sceneH * .15f, cx + span * .47f, top + sceneH * .34f,
                    span * .04f, span * .04f, backdropFill)
            }
            10 -> { // Moonlit Wilds: black highway, moon, lamps and predator eyes
                backdropFill.color = backdropColor(0xff060915.toInt(), .98f * reveal)
                canvas.drawRoundRect(cx - span * .48f, top + sceneH * .13f, cx + span * .48f, ground,
                    span * .04f, span * .04f, backdropFill)
                backdropFill.color = backdropColor(0xffdce9ff.toInt(), .94f * reveal)
                canvas.drawCircle(cx + span * .23f, top + sceneH * .24f, span * .12f, backdropFill)
                backdropFill.color = backdropColor(0xff060915.toInt(), .96f * reveal)
                canvas.drawCircle(cx + span * .28f, top + sceneH * .20f, span * .10f, backdropFill)
                backdropStroke.color = backdropColor(0xffffd98a.toInt(), .95f * reveal)
                backdropStroke.strokeWidth = max(1f, span * .04f)
                canvas.drawLine(cx - span * .34f, top + sceneH * .28f, cx - span * .34f, ground, backdropStroke)
                canvas.drawLine(cx + span * .38f, top + sceneH * .22f, cx + span * .38f, ground, backdropStroke)
                backdropFill.color = backdropColor(0xffffe6a3.toInt(), .94f * reveal)
                canvas.drawCircle(cx - span * .34f, top + sceneH * .27f, span * .07f, backdropFill)
                canvas.drawCircle(cx + span * .38f, top + sceneH * .21f, span * .07f, backdropFill)
                backdropStroke.color = backdropColor(0xffd4e7ff.toInt(), .84f * reveal)
                backdropStroke.strokeWidth = max(1f, span * .035f)
                for (dash in 0..2) {
                    val y = ground - sceneH * (.08f + dash * .055f)
                    canvas.drawLine(cx - span * (.40f - dash * .04f), y, cx + span * (.40f - dash * .04f), y, backdropStroke)
                }
            }
            11 -> { // Matrix Circuit: falling code columns and a forked left/right route
                backdropFill.color = backdropColor(0xff020b08.toInt(), .98f * reveal)
                canvas.drawRoundRect(cx - span * .48f, top + sceneH * .12f, cx + span * .48f, ground,
                    span * .04f, span * .04f, backdropFill)
                backdropFill.color = backdropColor(0xff20ff82.toInt(), .88f * reveal)
                for (column in 0..4) {
                    val x = cx - span * .37f + column * span * .18f
                    for (bit in 0..3) {
                        val y = top + sceneH * (.18f + bit * .12f + (column % 2) * .035f)
                        if ((column + bit) % 3 != 0) canvas.drawRect(x, y, x + span * .045f, y + sceneH * .055f, backdropFill)
                    }
                }
                backdropStroke.color = backdropColor(0xffa0ffcf.toInt(), .96f * reveal)
                backdropStroke.strokeWidth = max(1f, span * .055f)
                canvas.drawLine(cx, ground - sceneH * .06f, cx, top + sceneH * .58f, backdropStroke)
                canvas.drawLine(cx, top + sceneH * .58f, cx - span * .30f, top + sceneH * .35f, backdropStroke)
                canvas.drawLine(cx, top + sceneH * .58f, cx + span * .30f, top + sceneH * .35f, backdropStroke)
                canvas.drawLine(cx - span * .30f, top + sceneH * .35f, cx - span * .30f, top + sceneH * .22f, backdropStroke)
                canvas.drawLine(cx + span * .30f, top + sceneH * .35f, cx + span * .30f, top + sceneH * .22f, backdropStroke)
                backdropFill.color = backdropColor(0xffff4dd2.toInt(), .96f * reveal)
                canvas.drawCircle(cx, top + sceneH * .58f, span * .065f, backdropFill)
            }
            12 -> { // Air: a high blue sky, broken cloudbank and a small migrating flock
                backdropFill.color = backdropColor(0xffb4eaff.toInt(), .72f * reveal)
                backdropPath.reset()
                backdropPath.moveTo(cx - span * .49f, ground - sceneH * .05f)
                backdropPath.quadTo(cx - span * .22f, ground - sceneH * .28f, cx + span * .49f, ground - sceneH * .10f)
                backdropPath.lineTo(cx + span * .49f, ground)
                backdropPath.lineTo(cx - span * .49f, ground)
                backdropPath.close()
                canvas.drawPath(backdropPath, backdropFill)
                backdropFill.color = backdropColor(0xffeefaff.toInt(), .90f * reveal)
                canvas.drawRoundRect(cx - span * .44f, ground - sceneH * .16f, cx + span * .44f, ground,
                    span * .12f, span * .12f, backdropFill)
                canvas.drawCircle(cx - span * .25f, ground - sceneH * .16f, span * .10f, backdropFill)
                canvas.drawCircle(cx + span * .02f, ground - sceneH * .20f, span * .14f, backdropFill)
                canvas.drawCircle(cx + span * .27f, ground - sceneH * .15f, span * .10f, backdropFill)
                backdropStroke.color = backdropColor(0xff286f9d.toInt(), .92f * reveal)
                backdropStroke.strokeWidth = max(1f, span * .035f)
                for (bird in 0..2) {
                    val bx = cx + (bird - 1) * span * .20f
                    val by = top + sceneH * (.30f + (bird % 2) * .10f)
                    canvas.drawLine(bx - span * .06f, by, bx, by + sceneH * .035f, backdropStroke)
                    canvas.drawLine(bx, by + sceneH * .035f, bx + span * .06f, by, backdropStroke)
                }
                backdropFill.color = backdropColor(0xffffbd58.toInt(), .95f * reveal)
                canvas.drawCircle(cx + span * .32f, top + sceneH * .18f, span * .08f, backdropFill)
            }
            else -> { // Tropical Beach: sun, surf, sand and palm
                backdropFill.color = backdropColor(0xffffd36e.toInt(), .90f * reveal)
                canvas.drawCircle(cx, top + sceneH * .22f, span * .12f, backdropFill)
                backdropFill.color = backdropColor(0xff138ba7.toInt(), .84f * reveal)
                canvas.drawRect(cx - span * .48f, top + sceneH * .52f, cx + span * .48f, ground, backdropFill)
                backdropFill.color = backdropColor(0xffffcc79.toInt(), .90f * reveal)
                canvas.drawRect(cx - span * .48f, ground, cx + span * .48f, bottom, backdropFill)
                backdropStroke.color = backdropColor(0xff91f2e9.toInt(), .90f * reveal)
                backdropStroke.strokeWidth = max(1f, span * .025f)
                for (wave in 0..2) {
                    val y = top + sceneH * (.59f + wave * .075f)
                    backdropPath.reset()
                    backdropPath.moveTo(cx - span * .43f, y)
                    backdropPath.quadTo(cx - span * .23f, y - span * .06f, cx - span * .03f, y)
                    backdropPath.quadTo(cx + span * .20f, y + span * .06f, cx + span * .43f, y)
                    canvas.drawPath(backdropPath, backdropStroke)
                }
                backdropStroke.color = backdropColor(0xff634d37.toInt(), .92f * reveal)
                backdropStroke.strokeWidth = max(1f, span * .055f)
                canvas.drawLine(cx + span * .22f, ground, cx + span * .14f, top + sceneH * .29f, backdropStroke)
                backdropStroke.color = backdropColor(0xff48d28c.toInt(), .95f * reveal)
                backdropStroke.strokeWidth = max(1f, span * .045f)
                for (leaf in -1..1) {
                    val endX = cx + span * (.14f + leaf * .22f)
                    val endY = top + sceneH * (.20f + abs(leaf) * .07f)
                    canvas.drawLine(cx + span * .14f, top + sceneH * .31f, endX, endY, backdropStroke)
                }
            }
        }
    }

    private fun backdropColor(color: Int, alpha: Float): Int = Color.argb(
        (alpha.coerceIn(0f, 1f) * 255f).roundToInt(), Color.red(color), Color.green(color), Color.blue(color))

    private companion object {
        val BIOME_ACCENTS = intArrayOf(
            0xffed79c7.toInt(), // Candy Fields
            0xffa174ff.toInt(), // Neon City
            0xffff793e.toInt(), // Lava Caves
            0xffa8eaff.toInt(), // Frost Peaks
            0xffffc46a.toInt(), // Sunset Dunes
            0xffdb68ff.toInt(), // Riftbound Space
            0xff83dce9.toInt(), // Railway Yard
            0xff53e88e.toInt(), // Jungle Canopy
            0xffff6d52.toInt(), // Highway Rush
            0xffffc46a.toInt(), // Tropical Beach
            0xffffd98a.toInt(), // Moonlit Wilds
            0xff37ff8b.toInt(), // Matrix Circuit
            0xff8ce4ff.toInt(), // Air
        )
        val L1_LENGTH = sqrt(.45f*.45f+.85f*.85f+.35f*.35f)
        val L2_LENGTH = sqrt(.6f*.6f+.2f*.2f+.5f*.5f)
        val normals = arrayOf(floatArrayOf(1f,0f,0f), floatArrayOf(-1f,0f,0f), floatArrayOf(0f,1f,0f),
            floatArrayOf(0f,-1f,0f), floatArrayOf(0f,0f,1f), floatArrayOf(0f,0f,-1f))
        val faces = arrayOf(intArrayOf(1,5,7,3), intArrayOf(4,0,2,6), intArrayOf(2,3,7,6),
            intArrayOf(0,4,5,1), intArrayOf(5,4,6,7), intArrayOf(0,1,3,2))
    }
}
