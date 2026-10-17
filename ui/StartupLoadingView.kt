package cube.run.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import cube.run.R
import cube.run.data.Worlds
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** A native, asset-free boot screen shown above the first fully rendered menu scene. */
class StartupLoadingView(context: Context) : View(context) {
    private val progress = StartupLoadProgress()
    private var status = context.getString(R.string.loading_status_start)
    private var displayedPercent = 0f
    private var lastFrameAt = 0L
    private var dismissalScheduled = false

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val cubePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.25f)
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
    }
    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(159, 174, 211)
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        letterSpacing = .12f
    }
    private val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(194, 207, 232)
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
    }
    private val percentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.RIGHT
        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(42, 47, 78) }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val stars = floatArrayOf(
        .12f, .16f, .9f, .12f, .22f, .37f, .78f, .29f, .08f, .64f,
        .91f, .58f, .17f, .84f, .82f, .78f, .62f, .13f, .38f, .21f,
    )
    private val topFace = Path()
    private val leftFace = Path()
    private val rightFace = Path()
    private val cubeOutline = Path()
    private val barRect = RectF()
    private val mapRect = RectF()
    private val mapPath = Path()
    private val mapCardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mapIconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mapLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val mapTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        letterSpacing = .035f
    }
    private val atlasIds = Worlds.campaignIds.toIntArray()
    private val atlasNames = Array(atlasIds.size) { i -> context.gameText(Worlds.get(atlasIds[i]).name) }
    private val atlasSkyColors = IntArray(atlasIds.size)
    private val atlasGroundColors = IntArray(atlasIds.size)

    init {
        isClickable = true
        isFocusable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = context.getString(R.string.loading_accessibility, 0)
        alpha = 1f
        for (i in atlasIds.indices) {
            val world = Worlds.get(atlasIds[i])
            atlasSkyColors[i] = Color.HSVToColor(floatArrayOf(
                world.skyTopH, world.skyTopS.coerceIn(.35f, .9f), world.skyTopV.coerceIn(.35f, 1f),
            ))
            atlasGroundColors[i] = Color.HSVToColor(floatArrayOf(
                world.postH, world.postS.coerceIn(.4f, 1f), world.postV.coerceIn(.45f, 1f),
            ))
        }
    }

    /** Progress only moves forward; milestones are reported by the real startup path. */
    fun setProgress(percent: Int, message: String) {
        val update = Runnable {
            progress.advanceTo(percent)
            status = message
            contentDescription = context.getString(R.string.loading_accessibility, progress.targetPercent)
            invalidate()
        }
        if (Looper.myLooper() == Looper.getMainLooper()) update.run() else post(update)
    }

    /** Called only after a complete menu scene has rendered; 100% then fades to the menu. */
    fun finishLoading() = setProgress(100, context.getString(R.string.loading_status_ready))

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        backgroundPaint.shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(Color.rgb(10, 13, 31), Color.rgb(20, 17, 48), Color.rgb(11, 16, 38)),
            floatArrayOf(0f, .58f, 1f), Shader.TileMode.CLAMP,
        )
        fillPaint.shader = LinearGradient(
            0f, 0f, w.toFloat(), 0f,
            intArrayOf(Color.rgb(83, 232, 255), Color.rgb(102, 166, 255), Color.rgb(179, 115, 255)),
            null, Shader.TileMode.CLAMP,
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        canvas.drawRect(0f, 0f, w, h, backgroundPaint)

        val now = SystemClock.uptimeMillis()
        val dt = if (lastFrameAt == 0L) 0f else ((now - lastFrameAt).coerceIn(0L, 50L) / 1000f)
        lastFrameAt = now
        val target = progress.targetPercent.toFloat()
        val remaining = target - displayedPercent
        if (abs(remaining) > .01f) {
            displayedPercent += remaining.sign() * min(abs(remaining), 112f * dt)
            if (abs(target - displayedPercent) < .05f) displayedPercent = target
            postInvalidateOnAnimation()
        }

        // Sparse animated points keep the page atmospheric without texture assets.
        for (i in stars.indices step 2) {
            val phase = now * .001f + i
            starPaint.alpha = (if ((i / 2) % 3 == 0) 70 else 34) + (12f * (.5f + .5f * kotlin.math.sin(phase))).toInt()
            val radius = if ((i / 2) % 3 == 0) dp(1.5f) else dp(1f)
            canvas.drawCircle(stars[i] * w, stars[i + 1] * h, radius, starPaint)
        }

        drawWorldAtlas(canvas, w, h, now)

        val centerX = w * .5f
        val logoY = h * .105f
        val logoSize = dp(if (h < dp(560f)) 36f else 45f)
        haloPaint.color = Color.argb(18, 91, 224, 255)
        haloPaint.strokeWidth = dp(1f)
        canvas.drawCircle(centerX, logoY, logoSize * 1.45f, haloPaint)
        haloPaint.color = Color.argb(32, 139, 126, 255)
        canvas.drawCircle(centerX, logoY, logoSize * 1.78f, haloPaint)
        drawCube(canvas, centerX, logoY, logoSize)

        titlePaint.textSize = dp(if (h < dp(560f)) 23f else 27f)
        canvas.drawText(context.getString(R.string.loading_title), centerX, logoY + logoSize * 1.52f, titlePaint)
        subtitlePaint.textSize = dp(10.5f)
        canvas.drawText(context.getString(R.string.loading_subtitle), centerX, logoY + logoSize * 1.92f, subtitlePaint)

        val barLeft = w * .12f
        val barRight = w * .88f
        val barTop = h * .835f
        val barHeight = dp(8f)
        statusPaint.textSize = dp(13f)
        canvas.drawText(status, centerX, barTop - dp(20f), statusPaint)

        barRect.set(barLeft, barTop, barRight, barTop + barHeight)
        canvas.drawRoundRect(barRect, barHeight, barHeight, trackPaint)
        val fillRight = barLeft + (barRight - barLeft) * (displayedPercent.coerceIn(0f, 100f) / 100f)
        if (fillRight > barLeft) {
            barRect.set(barLeft, barTop, fillRight, barTop + barHeight)
            canvas.drawRoundRect(barRect, barHeight, barHeight, fillPaint)
        }
        percentPaint.textSize = dp(14f)
        canvas.drawText("${displayedPercent.roundToInt().coerceIn(0, 100)}%", barRight, barTop + dp(34f), percentPaint)

        if (progress.targetPercent == 100 && displayedPercent >= 99.95f && !dismissalScheduled) {
            dismissalScheduled = true
            postDelayed({
                if (isAttachedToWindow) {
                    animate().alpha(0f).setDuration(260L).withEndAction {
                        (parent as? ViewGroup)?.removeView(this)
                    }.start()
                }
            }, 300L)
        }
        if (!dismissalScheduled) postInvalidateDelayed(90L)
    }

    private fun drawCube(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val half = size / 2f
        val quarter = size / 4f
        val base = cy + half * .88f
        topFace.reset()
        topFace.moveTo(cx, cy - half)
        topFace.lineTo(cx + half, cy - quarter)
        topFace.lineTo(cx, cy + quarter)
        topFace.lineTo(cx - half, cy - quarter)
        topFace.close()
        leftFace.reset()
        leftFace.moveTo(cx - half, cy - quarter)
        leftFace.lineTo(cx, cy + quarter)
        leftFace.lineTo(cx, base)
        leftFace.lineTo(cx - half, cy + half * .36f)
        leftFace.close()
        rightFace.reset()
        rightFace.moveTo(cx + half, cy - quarter)
        rightFace.lineTo(cx, cy + quarter)
        rightFace.lineTo(cx, base)
        rightFace.lineTo(cx + half, cy + half * .36f)
        rightFace.close()
        cubePaint.color = Color.rgb(72, 218, 255)
        canvas.drawPath(topFace, cubePaint)
        cubePaint.color = Color.rgb(77, 143, 255)
        canvas.drawPath(leftFace, cubePaint)
        cubePaint.color = Color.rgb(164, 102, 255)
        canvas.drawPath(rightFace, cubePaint)
        outlinePaint.color = Color.argb(130, 229, 250, 255)
        cubeOutline.reset()
        cubeOutline.moveTo(cx, cy - half)
        cubeOutline.lineTo(cx + half, cy - quarter)
        cubeOutline.lineTo(cx + half, cy + half * .36f)
        cubeOutline.lineTo(cx, base)
        cubeOutline.lineTo(cx - half, cy + half * .36f)
        cubeOutline.lineTo(cx - half, cy - quarter)
        cubeOutline.close()
        cubeOutline.moveTo(cx - half, cy - quarter)
        cubeOutline.lineTo(cx, cy + quarter)
        cubeOutline.lineTo(cx + half, cy - quarter)
        cubeOutline.moveTo(cx, cy + quarter)
        cubeOutline.lineTo(cx, base)
        canvas.drawPath(cubeOutline, outlinePaint)
    }

    /** A two-column, six-row miniature atlas keeps every campaign biome visible during boot. */
    private fun drawWorldAtlas(canvas: Canvas, w: Float, h: Float, now: Long) {
        val density = resources.displayMetrics.density
        val margin = min(dp(18f), w * .065f)
        val gapX = min(dp(8f), w * .025f)
        val gapY = min(dp(6f), h * .012f)
        val cardWidth = (w - margin * 2f - gapX) * .5f
        if (cardWidth <= dp(54f)) return

        val logoY = h * .105f
        val logoSize = dp(if (h < dp(560f)) 36f else 45f)
        val atlasTop = maxOf(h * .25f, logoY + logoSize * 2.1f)
        val atlasBottom = h * .75f
        val available = (atlasBottom - atlasTop).coerceAtLeast(dp(116f))
        val cardHeight = ((available - gapY * 5f) / 6f).coerceAtMost(dp(56f))
        val left = margin
        val right = left + cardWidth + gapX
        val corner = min(dp(10f), cardHeight * .24f)
        val nowSeconds = now * .001f

        for (i in atlasIds.indices) {
            val column = i % 2
            val row = i / 2
            val x = if (column == 0) left else right
            val y = atlasTop + row * (cardHeight + gapY)
            mapRect.set(x, y, x + cardWidth, y + cardHeight)

            val sky = atlasSkyColors[i]
            val ground = atlasGroundColors[i]
            mapCardPaint.color = Color.argb(142, Color.red(sky), Color.green(sky), Color.blue(sky))
            canvas.drawRoundRect(mapRect, corner, corner, mapCardPaint)
            mapCardPaint.color = Color.argb(115, 7, 10, 24)
            canvas.drawRoundRect(mapRect, corner, corner, mapCardPaint)
            mapLinePaint.color = withAlpha(ground, 112)
            mapLinePaint.strokeWidth = maxOf(dp(.65f), density * .7f)
            canvas.drawRoundRect(mapRect, corner, corner, mapLinePaint)

            val iconWidth = min(cardHeight * .88f, dp(48f))
            val iconCenterX = x + dp(12f) + iconWidth * .5f
            val iconCenterY = y + cardHeight * .52f
            canvas.save()
            canvas.clipRect(x + dp(3f), y + dp(2f), x + dp(12f) + iconWidth, y + cardHeight - dp(2f))
            drawWorldIcon(canvas, atlasIds[i], iconCenterX, iconCenterY, iconWidth, cardHeight, ground, nowSeconds)
            canvas.restore()

            val textLeft = x + dp(12f) + iconWidth + dp(5f)
            val textRight = x + cardWidth - dp(7f)
            val textSize = min(dp(12.5f), cardHeight * .34f).coerceAtLeast(dp(7f))
            mapTitlePaint.textSize = textSize
            mapTitlePaint.color = Color.argb(245, 247, 250, 255)
            val label = atlasNames[i]
            val clippedLabel = if (mapTitlePaint.measureText(label) <= textRight - textLeft) label else {
                var end = label.length
                while (end > 2 && mapTitlePaint.measureText(label.substring(0, end) + "…") > textRight - textLeft) end--
                label.substring(0, end.coerceAtLeast(1)) + "…"
            }
            val baseline = y + (cardHeight - (mapTitlePaint.ascent() + mapTitlePaint.descent())) * .5f
            canvas.drawText(clippedLabel, textLeft, baseline, mapTitlePaint)
        }
    }

    private fun drawWorldIcon(canvas: Canvas, id: Int, cx: Float, cy: Float, width: Float, height: Float, accent: Int, time: Float) {
        val s = min(width, height) * .42f
        val ground = withAlpha(accent, 230)
        mapIconPaint.color = ground
        mapLinePaint.strokeWidth = maxOf(dp(1f), height * .035f)
        when (id) {
            Worlds.LAVA_ID -> {
                triangle(canvas, cx - s * 1.05f, cy + s * .58f, cx - s * .35f, cy - s * .94f, cx + s * .16f, cy + s * .58f, Color.rgb(93, 34, 35))
                triangle(canvas, cx - s * .26f, cy + s * .58f, cx + s * .38f, cy - s * 1.18f, cx + s * 1.06f, cy + s * .58f, Color.rgb(124, 37, 34))
                triangle(canvas, cx + s * .20f, cy - s * .72f, cx + s * .38f, cy - s * 1.18f, cx + s * .56f, cy - s * .70f, Color.rgb(255, 216, 163))
                mapIconPaint.color = Color.rgb(255, 101, 34)
                canvas.drawRect(cx + s * .32f, cy - s * .11f, cx + s * .45f, cy + s * .62f, mapIconPaint)
                canvas.drawCircle(cx + s * .66f, cy - s * .39f + sin(time * 3f) * s * .06f, s * .10f, mapIconPaint)
            }
            Worlds.WINTER_ID -> {
                triangle(canvas, cx - s * 1.15f, cy + s * .7f, cx - s * .58f, cy - s * .98f, cx + s * .06f, cy + s * .7f, Color.rgb(103, 156, 194))
                triangle(canvas, cx - s * .34f, cy + s * .7f, cx + s * .34f, cy - s * 1.2f, cx + s * 1.1f, cy + s * .7f, Color.rgb(139, 190, 219))
                triangle(canvas, cx - s * .83f, cy - s * .55f, cx - s * .58f, cy - s * .98f, cx - s * .40f, cy - s * .53f, Color.WHITE)
                triangle(canvas, cx + s * .02f, cy - s * .65f, cx + s * .34f, cy - s * 1.2f, cx + s * .60f, cy - s * .60f, Color.WHITE)
            }
            Worlds.JUNGLE_ID -> {
                mapIconPaint.color = Color.rgb(113, 75, 44)
                canvas.drawRect(cx - s * .12f, cy - s * .05f, cx + s * .11f, cy + s * .80f, mapIconPaint)
                mapIconPaint.color = Color.rgb(38, 153, 75)
                canvas.drawCircle(cx - s * .36f, cy - s * .28f, s * .51f, mapIconPaint)
                canvas.drawCircle(cx + s * .25f, cy - s * .33f, s * .59f, mapIconPaint)
                canvas.drawCircle(cx + s * .57f, cy - s * .06f, s * .36f, mapIconPaint)
            }
            Worlds.NEON_CITY_ID -> {
                val building = Color.rgb(29, 71, 110)
                mapIconPaint.color = building
                canvas.drawRect(cx - s * 1.05f, cy - s * .10f, cx - s * .45f, cy + s * .80f, mapIconPaint)
                canvas.drawRect(cx - s * .32f, cy - s * .78f, cx + s * .27f, cy + s * .80f, mapIconPaint)
                canvas.drawRect(cx + s * .43f, cy - s * .43f, cx + s * 1.06f, cy + s * .80f, mapIconPaint)
                mapIconPaint.color = Color.rgb(72, 232, 255)
                for (i in 0..2) canvas.drawRect(cx - s * .16f, cy - s * .56f + i * s * .35f, cx - s * .02f, cy - s * .43f + i * s * .35f, mapIconPaint)
            }
            Worlds.GALAXY_ID -> {
                mapLinePaint.color = Color.argb(220, 94, 220, 255)
                mapRect.set(cx - s * 1.18f, cy - s * .44f, cx + s * 1.18f, cy + s * .44f)
                canvas.drawOval(mapRect, mapLinePaint)
                mapIconPaint.color = Color.rgb(129, 97, 255)
                canvas.drawCircle(cx, cy, s * .61f, mapIconPaint)
                mapIconPaint.color = Color.rgb(91, 228, 255)
                canvas.drawCircle(cx - s * .18f, cy - s * .17f, s * .23f, mapIconPaint)
                mapIconPaint.color = Color.rgb(255, 177, 93)
                canvas.drawCircle(cx + s * .83f, cy - s * .22f, s * .14f, mapIconPaint)
            }
            Worlds.RAILWAY_ID -> {
                mapLinePaint.color = Color.rgb(156, 195, 218)
                mapLinePaint.strokeWidth = maxOf(dp(1f), height * .04f)
                canvas.drawLine(cx - s, cy + s * .42f, cx + s, cy + s * .42f, mapLinePaint)
                canvas.drawLine(cx - s, cy + s * .72f, cx + s, cy + s * .72f, mapLinePaint)
                for (i in 0..3) {
                    val x = cx - s * .75f + i * s * .5f
                    canvas.drawLine(x, cy + s * .35f, x, cy + s * .78f, mapLinePaint)
                }
                mapIconPaint.color = Color.rgb(245, 126, 49)
                canvas.drawRect(cx - s * .75f, cy - s * .9f, cx - s * .56f, cy + s * .38f, mapIconPaint)
                canvas.drawRect(cx + s * .58f, cy - s * .7f, cx + s * .76f, cy + s * .38f, mapIconPaint)
            }
            Worlds.BEACH_ID -> {
                mapIconPaint.color = Color.rgb(255, 194, 77)
                canvas.drawCircle(cx + s * .60f, cy - s * .56f, s * .30f, mapIconPaint)
                mapLinePaint.color = Color.rgb(93, 225, 239)
                mapLinePaint.strokeWidth = maxOf(dp(1f), height * .045f)
                for (i in 0..2) canvas.drawLine(cx - s, cy + s * .1f + i * s * .32f, cx + s, cy + s * .1f + i * s * .32f, mapLinePaint)
                mapIconPaint.color = Color.rgb(132, 84, 44)
                canvas.drawRect(cx - s * .38f, cy - s * .22f, cx - s * .27f, cy + s * .63f, mapIconPaint)
                triangle(canvas, cx - s * .95f, cy - s * .13f, cx - s * .32f, cy - s * .76f, cx + s * .22f, cy - s * .12f, Color.rgb(57, 187, 102))
            }
            Worlds.NIGHT_ID -> {
                mapIconPaint.color = Color.rgb(239, 228, 178)
                canvas.drawCircle(cx + s * .56f, cy - s * .52f, s * .38f, mapIconPaint)
                mapIconPaint.color = Color.rgb(14, 23, 47)
                canvas.drawCircle(cx + s * .72f, cy - s * .62f, s * .34f, mapIconPaint)
                mapLinePaint.color = Color.rgb(59, 81, 107)
                mapLinePaint.strokeWidth = maxOf(dp(1f), height * .035f)
                for (i in 0..2) {
                    val x = cx - s * .85f + i * s * .78f
                    canvas.drawLine(x, cy + s * .65f, x, cy - s * .15f, mapLinePaint)
                    triangle(canvas, x - s * .38f, cy - s * .13f, x, cy - s * .75f, x + s * .35f, cy - s * .13f, Color.rgb(15, 34, 53))
                }
            }
            Worlds.HACKER_ID -> {
                mapIconPaint.color = Color.rgb(13, 63, 45)
                canvas.drawRect(cx - s, cy - s * .85f, cx + s, cy + s * .80f, mapIconPaint)
                for (column in 0..5) {
                    val x = cx - s * .83f + column * s * .32f
                    val y = cy - s * .75f + ((column * 13 + time.toInt() * 3) % 4) * s * .27f
                    mapIconPaint.color = if (column % 2 == 0) Color.rgb(86, 255, 145) else Color.rgb(27, 183, 103)
                    canvas.drawRect(x, y, x + s * .09f, y + s * .22f, mapIconPaint)
                    canvas.drawRect(x, y + s * .34f, x + s * .09f, y + s * .48f, mapIconPaint)
                }
            }
            Worlds.AIR_ID -> {
                mapIconPaint.color = Color.rgb(239, 248, 255)
                canvas.drawCircle(cx - s * .35f, cy - s * .10f, s * .40f, mapIconPaint)
                canvas.drawCircle(cx + s * .05f, cy - s * .35f, s * .55f, mapIconPaint)
                canvas.drawCircle(cx + s * .48f, cy - s * .06f, s * .37f, mapIconPaint)
                canvas.drawRect(cx - s * .55f, cy - s * .10f, cx + s * .67f, cy + s * .38f, mapIconPaint)
                mapLinePaint.color = Color.rgb(101, 205, 241)
                mapLinePaint.strokeWidth = maxOf(dp(1f), height * .035f)
                canvas.drawLine(cx - s, cy + s * .66f, cx + s, cy + s * .66f, mapLinePaint)
            }
            Worlds.NEXUS_ID -> {
                mapLinePaint.color = Color.rgb(63, 247, 231)
                mapLinePaint.strokeWidth = maxOf(dp(1.5f), height * .06f)
                mapRect.set(cx - s * .86f, cy - s * .84f, cx - s * .02f, cy + s * .65f)
                canvas.drawRoundRect(mapRect, s * .38f, s * .38f, mapLinePaint)
                mapLinePaint.color = Color.rgb(236, 75, 247)
                mapRect.set(cx + s * .08f, cy - s * .60f, cx + s * .90f, cy + s * .84f)
                canvas.drawRoundRect(mapRect, s * .38f, s * .38f, mapLinePaint)
                mapIconPaint.color = Color.rgb(255, 255, 255)
                canvas.drawCircle(cx, cy, s * .12f, mapIconPaint)
            }
            else -> {
                mapIconPaint.color = Color.rgb(255, 132, 203)
                canvas.drawCircle(cx - s * .48f, cy - s * .17f, s * .46f, mapIconPaint)
                mapIconPaint.color = Color.rgb(255, 208, 83)
                canvas.drawCircle(cx + s * .33f, cy - s * .1f, s * .51f, mapIconPaint)
            }
        }
    }

    private fun triangle(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, color: Int) {
        mapPath.reset()
        mapPath.moveTo(x1, y1)
        mapPath.lineTo(x2, y2)
        mapPath.lineTo(x3, y3)
        mapPath.close()
        mapIconPaint.color = color
        canvas.drawPath(mapPath, mapIconPaint)
    }

    private fun withAlpha(color: Int, alpha: Int): Int = Color.argb(
        alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color),
    )

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
    private fun Float.sign(): Float = if (this < 0f) -1f else 1f
}

/** Small pure state object so startup milestones can never make the bar run backwards. */
internal class StartupLoadProgress {
    var targetPercent: Int = 0
        private set

    fun advanceTo(percent: Int) {
        targetPercent = maxOf(targetPercent, percent.coerceIn(0, 100))
    }
}
