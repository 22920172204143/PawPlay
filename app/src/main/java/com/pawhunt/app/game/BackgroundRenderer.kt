package com.pawhunt.app.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.pawhunt.app.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

object BackgroundRenderer {

    const val BG_COUNT = 5

    private var cachedBgIndex = -99
    private var cachedBitmap: Bitmap? = null
    private var tiles = arrayOfNulls<Bitmap>(BG_COUNT)
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val tileResIds = intArrayOf(
        R.drawable.bg_mulch,
        R.drawable.bg_grass,
        R.drawable.bg_stone,
        R.drawable.bg_wood,
        0
    )

    private val darkenScales = arrayOf(
        floatArrayOf(0.35f, 0.33f, 0.30f),
        floatArrayOf(0.55f, 0.65f, 0.38f),
        floatArrayOf(0.50f, 0.45f, 0.42f),
        floatArrayOf(0.42f, 0.38f, 0.32f),
        floatArrayOf(1f, 1f, 1f)
    )

    fun init(context: Context) {
        val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 }
        for (i in tileResIds.indices) {
            if (tileResIds[i] != 0 && tiles[i] == null) {
                tiles[i] = BitmapFactory.decodeResource(context.resources, tileResIds[i], opts)
            }
        }
    }

    fun drawBackground(canvas: Canvas, bgIndex: Int, bounds: RectF) {
        if (bounds.width() <= 0f || bounds.height() <= 0f) return
        val w = bounds.width().toInt()
        val h = bounds.height().toInt()
        val idx = bgIndex.mod(BG_COUNT)

        if (cachedBgIndex != idx || cachedBitmap == null ||
            cachedBitmap!!.width != w || cachedBitmap!!.height != h
        ) {
            cachedBitmap?.recycle()
            cachedBitmap = generateBackground(idx, w, h)
            cachedBgIndex = idx
        }
        cachedBitmap?.let { canvas.drawBitmap(it, 0f, 0f, paint) }
    }

    private fun generateBackground(idx: Int, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        if (idx == 4) {
            drawCutePattern(c, w, h)
        } else {
            drawTiled(c, w, h, idx)
        }
        drawVignette(c, w, h)
        return bmp
    }

    private fun drawTiled(c: Canvas, w: Int, h: Int, idx: Int) {
        val tile = tiles.getOrNull(idx)
        if (tile == null || tile.isRecycled) {
            c.drawColor(Color.rgb(30, 25, 20))
            return
        }

        val ds = darkenScales[idx]
        val tilePaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                setScale(ds[0], ds[1], ds[2], 1f)
            })
        }

        val tw = tile.width; val th = tile.height
        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                c.drawBitmap(tile, x.toFloat(), y.toFloat(), tilePaint)
                x += tw
            }
            y += th
        }
    }

    /**
     * @brief Programmatically generated cute pastel pattern with paw prints and stars
     */
    private fun drawCutePattern(c: Canvas, w: Int, h: Int) {
        val rng = Random(123)
        val wf = w.toFloat(); val hf = h.toFloat()

        c.drawColor(Color.rgb(245, 240, 230))

        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val path = Path()

        val cellSize = 160f
        var row = 0
        var gy = -cellSize / 2
        while (gy < hf + cellSize) {
            var gx = if (row % 2 == 0) 0f else cellSize / 2
            while (gx < wf + cellSize) {
                val cx = gx + rng.nextFloat() * 20f - 10f
                val cy = gy + rng.nextFloat() * 20f - 10f
                val variant = rng.nextInt(4)
                val alpha = 55 + rng.nextInt(40)

                when (variant) {
                    0 -> {
                        p.color = Color.argb(alpha, 240, 150, 170)
                        drawPawPrint(c, p, cx, cy, 18f + rng.nextFloat() * 8f, rng.nextFloat() * 360f)
                    }
                    1 -> {
                        p.color = Color.argb(alpha, 245, 200, 80)
                        drawStarShape(c, p, path, cx, cy, 12f + rng.nextFloat() * 8f, 5, rng.nextFloat() * 360f)
                    }
                    2 -> {
                        p.color = Color.argb(alpha, 140, 185, 240)
                        drawHeart(c, p, path, cx, cy, 14f + rng.nextFloat() * 6f, rng.nextFloat() * 30f - 15f)
                    }
                    3 -> {
                        p.color = Color.argb(alpha, 200, 180, 220)
                        c.drawCircle(cx, cy, 5f + rng.nextFloat() * 8f, p)
                    }
                }
                gx += cellSize
            }
            gy += cellSize * 0.85f
            row++
        }
    }

    private fun drawPawPrint(c: Canvas, p: Paint, cx: Float, cy: Float, size: Float, rot: Float) {
        c.save()
        c.translate(cx, cy)
        c.rotate(rot)
        c.drawOval(-size * 0.5f, -size * 0.2f, size * 0.5f, size * 0.6f, p)
        val toeR = size * 0.2f
        c.drawCircle(-size * 0.35f, -size * 0.35f, toeR, p)
        c.drawCircle(0f, -size * 0.5f, toeR, p)
        c.drawCircle(size * 0.35f, -size * 0.35f, toeR, p)
        c.restore()
    }

    private fun drawStarShape(c: Canvas, p: Paint, path: Path, cx: Float, cy: Float, r: Float, pts: Int, rot: Float) {
        path.reset()
        val step = PI.toFloat() / pts
        val rotRad = rot * PI.toFloat() / 180f
        for (i in 0 until pts * 2) {
            val rad = if (i % 2 == 0) r else r * 0.4f
            val angle = i * step - PI.toFloat() / 2 + rotRad
            val x = cx + rad * cos(angle)
            val y = cy + rad * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        c.drawPath(path, p)
    }

    private fun drawHeart(c: Canvas, p: Paint, path: Path, cx: Float, cy: Float, size: Float, rot: Float) {
        c.save()
        c.translate(cx, cy)
        c.rotate(rot)
        path.reset()
        path.moveTo(0f, size * 0.3f)
        path.cubicTo(-size, -size * 0.3f, -size * 0.3f, -size, 0f, -size * 0.4f)
        path.cubicTo(size * 0.3f, -size, size, -size * 0.3f, 0f, size * 0.3f)
        c.drawPath(path, p)
        c.restore()
    }

    private fun drawVignette(c: Canvas, w: Int, h: Int) {
        val cx = w / 2f; val cy = h / 2f
        val r = sqrt((cx * cx + cy * cy).toDouble()).toFloat()
        overlayPaint.shader = RadialGradient(cx, cy, r,
            intArrayOf(Color.argb(0, 0, 0, 0), Color.argb(0, 0, 0, 0), Color.argb(130, 0, 0, 0)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), overlayPaint)
        overlayPaint.shader = null
    }

    fun release() {
        cachedBitmap?.recycle()
        cachedBitmap = null
        for (i in tiles.indices) {
            tiles[i]?.recycle()
            tiles[i] = null
        }
        cachedBgIndex = -99
    }
}
