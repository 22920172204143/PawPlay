package com.yourcompany.pawplay.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

object BackgroundRenderer {

    private var cachedBgId = -1
    private var cachedBitmap: Bitmap? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    fun drawBackground(canvas: Canvas, bgId: Int, bounds: RectF) {
        if (bounds.width() <= 0f || bounds.height() <= 0f) return

        val w = bounds.width().toInt()
        val h = bounds.height().toInt()

        if (cachedBgId != bgId || cachedBitmap == null ||
            cachedBitmap!!.width != w || cachedBitmap!!.height != h
        ) {
            cachedBitmap?.recycle()
            cachedBitmap = generateBackground(bgId, w, h)
            cachedBgId = bgId
        }

        cachedBitmap?.let { canvas.drawBitmap(it, 0f, 0f, bitmapPaint) }
    }

    private fun generateBackground(bgId: Int, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        when (bgId) {
            1 -> drawGrass(c, w, h)
            2 -> drawWoodFloor(c, w, h)
            3 -> drawWater(c, w, h)
            4 -> drawNightSky(c, w, h)
            5 -> drawCityFloor(c, w, h)
            6 -> drawPinkBlanket(c, w, h)
            else -> drawGrass(c, w, h)
        }
        drawVignette(c, w, h)
        return bmp
    }

    private fun drawGrass(c: Canvas, w: Int, h: Int) {
        paint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            Color.rgb(55, 120, 40), Color.rgb(35, 85, 25),
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        val rng = Random(42)
        paint.strokeWidth = 1.5f
        paint.style = Paint.Style.STROKE
        for (i in 0 until (w * h / 200)) {
            val x = rng.nextFloat() * w
            val y = rng.nextFloat() * h
            val len = 8f + rng.nextFloat() * 15f
            val angle = -70f + rng.nextFloat() * 20f
            val shade = rng.nextInt(40)
            paint.color = Color.rgb(30 + shade, 90 + shade, 15 + shade)
            val rad = Math.toRadians(angle.toDouble())
            c.drawLine(x, y, x + (len * cos(rad)).toFloat(), y + (len * sin(rad)).toFloat(), paint)
        }
        paint.style = Paint.Style.FILL

        paint.color = Color.argb(15, 0, 0, 0)
        for (i in 0 until 40) {
            val x = rng.nextFloat() * w
            val y = rng.nextFloat() * h
            val r = 20f + rng.nextFloat() * 60f
            c.drawCircle(x, y, r, paint)
        }
    }

    private fun drawWoodFloor(c: Canvas, w: Int, h: Int) {
        paint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            Color.rgb(130, 90, 55), Color.rgb(95, 65, 38),
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        val rng = Random(77)
        val plankH = h / 6f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        for (row in 0..6) {
            val y = row * plankH
            paint.color = Color.argb(40, 0, 0, 0)
            c.drawLine(0f, y, w.toFloat(), y, paint)

            val offset = if (row % 2 == 0) 0f else w * 0.33f
            for (col in 0..4) {
                val x = offset + col * w * 0.5f
                if (x < w) {
                    c.drawLine(x, y, x, y + plankH, paint)
                }
            }
        }
        paint.style = Paint.Style.FILL

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.7f
        for (i in 0 until (w * h / 300)) {
            val x = rng.nextFloat() * w
            val y = rng.nextFloat() * h
            val len = 15f + rng.nextFloat() * 40f
            val shade = rng.nextInt(25)
            paint.color = Color.argb(30, 60 + shade, 40 + shade, 20 + shade)
            c.drawLine(x, y, x + len, y + rng.nextFloat() * 2f - 1f, paint)
        }
        paint.style = Paint.Style.FILL

        for (i in 0 until 15) {
            val x = rng.nextFloat() * w
            val y = rng.nextFloat() * h
            val r = 3f + rng.nextFloat() * 8f
            paint.color = Color.argb(20, 50, 30, 10)
            c.drawCircle(x, y, r, paint)
        }
    }

    private fun drawWater(c: Canvas, w: Int, h: Int) {
        paint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            Color.rgb(0, 100, 180), Color.rgb(0, 40, 100),
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        val rng = Random(55)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        for (i in 0 until 60) {
            val y = rng.nextFloat() * h
            val x = rng.nextFloat() * w
            val len = 30f + rng.nextFloat() * 80f
            val alpha = 15 + rng.nextInt(25)
            paint.color = Color.argb(alpha, 100, 180, 255)
            c.drawLine(x, y, x + len, y + rng.nextFloat() * 4f - 2f, paint)
        }
        paint.style = Paint.Style.FILL

        for (i in 0 until 20) {
            val x = rng.nextFloat() * w
            val y = rng.nextFloat() * h
            paint.color = Color.argb(20 + rng.nextInt(20), 150, 210, 255)
            c.drawOval(x - 15f, y - 3f, x + 15f + rng.nextFloat() * 30f, y + 3f, paint)
        }

        for (i in 0 until 30) {
            val x = rng.nextFloat() * w
            val y = rng.nextFloat() * h
            val r = 10f + rng.nextFloat() * 40f
            paint.shader = RadialGradient(
                x, y, r,
                Color.argb(12, 200, 230, 255), Color.argb(0, 200, 230, 255),
                Shader.TileMode.CLAMP
            )
            c.drawCircle(x, y, r, paint)
            paint.shader = null
        }
    }

    private fun drawNightSky(c: Canvas, w: Int, h: Int) {
        paint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            Color.rgb(10, 10, 40), Color.rgb(25, 25, 80),
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        val rng = Random(99)
        for (i in 0 until 150) {
            val x = rng.nextFloat() * w
            val y = rng.nextFloat() * h
            val r = 0.5f + rng.nextFloat() * 2f
            val brightness = 150 + rng.nextInt(105)
            paint.color = Color.argb(brightness, 255, 255, 240)
            c.drawCircle(x, y, r, paint)
            if (rng.nextFloat() < 0.15f) {
                paint.color = Color.argb(30, 255, 255, 200)
                c.drawCircle(x, y, r * 4f, paint)
            }
        }

        paint.shader = RadialGradient(
            w * 0.2f, h * 0.15f, w * 0.08f,
            Color.argb(60, 255, 255, 200), Color.argb(0, 255, 255, 200),
            Shader.TileMode.CLAMP
        )
        c.drawCircle(w * 0.2f, h * 0.15f, w * 0.08f, paint)
        paint.shader = null
        paint.color = Color.rgb(220, 220, 200)
        c.drawCircle(w * 0.2f, h * 0.15f, w * 0.035f, paint)
        paint.color = Color.argb(40, 200, 200, 180)
        c.drawCircle(w * 0.2f - w * 0.008f, h * 0.15f - w * 0.005f, w * 0.012f, paint)

        for (i in 0 until 5) {
            val cx = rng.nextFloat() * w
            val cy = rng.nextFloat() * h * 0.7f
            val r = 30f + rng.nextFloat() * 80f
            paint.color = Color.argb(8, 100, 100, 150)
            c.drawOval(cx - r, cy - r * 0.4f, cx + r, cy + r * 0.4f, paint)
        }
    }

    private fun drawCityFloor(c: Canvas, w: Int, h: Int) {
        paint.shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            Color.rgb(85, 85, 85), Color.rgb(65, 65, 65),
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        val rng = Random(33)
        for (i in 0 until (w * h / 100)) {
            val x = rng.nextFloat() * w
            val y = rng.nextFloat() * h
            val brightness = 55 + rng.nextInt(40)
            paint.color = Color.argb(rng.nextInt(60) + 20, brightness, brightness, brightness)
            c.drawPoint(x, y, paint)
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.5f
        paint.color = Color.argb(20, 0, 0, 0)
        val tileSize = 80f
        var y = 0f
        while (y < h) {
            c.drawLine(0f, y, w.toFloat(), y, paint)
            y += tileSize
        }
        var x = 0f
        while (x < w) {
            c.drawLine(x, 0f, x, h.toFloat(), paint)
            x += tileSize
        }
        paint.style = Paint.Style.FILL

        for (i in 0 until 10) {
            val cx = rng.nextFloat() * w
            val cy = rng.nextFloat() * h
            val r = 5f + rng.nextFloat() * 20f
            paint.color = Color.argb(15, 0, 0, 0)
            c.drawCircle(cx, cy, r, paint)
        }
    }

    private fun drawPinkBlanket(c: Canvas, w: Int, h: Int) {
        paint.shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            Color.rgb(245, 170, 190), Color.rgb(230, 140, 170),
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        val rng = Random(88)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        val spacing = 12f
        var y = 0f
        var toggle = true
        while (y < h) {
            var x = 0f
            while (x < w) {
                val shade = rng.nextInt(20)
                paint.color = Color.argb(30, 200 + shade, 100, 130 + shade)
                if (toggle) {
                    c.drawLine(x, y, x + spacing, y + spacing, paint)
                } else {
                    c.drawLine(x + spacing, y, x, y + spacing, paint)
                }
                x += spacing
            }
            y += spacing
            toggle = !toggle
        }
        paint.style = Paint.Style.FILL

        for (i in 0 until 40) {
            val x = rng.nextFloat() * w
            val fy = rng.nextFloat() * h
            val r = 15f + rng.nextFloat() * 40f
            paint.color = Color.argb(10, 255, 200, 210)
            c.drawCircle(x, fy, r, paint)
        }

        for (i in 0 until 8) {
            val fx = rng.nextFloat() * w
            val fy = rng.nextFloat() * h
            paint.color = Color.argb(15, 100, 50, 70)
            c.drawOval(fx - 20f, fy - 8f, fx + 20f + rng.nextFloat() * 30f, fy + 8f, paint)
        }
    }

    private fun drawVignette(c: Canvas, w: Int, h: Int) {
        val cx = w / 2f
        val cy = h / 2f
        val radius = sqrt((cx * cx + cy * cy).toDouble()).toFloat()
        paint.shader = RadialGradient(
            cx, cy, radius,
            intArrayOf(Color.argb(0, 0, 0, 0), Color.argb(0, 0, 0, 0), Color.argb(80, 0, 0, 0)),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null
    }

    fun release() {
        cachedBitmap?.recycle()
        cachedBitmap = null
        cachedBgId = -1
    }
}
