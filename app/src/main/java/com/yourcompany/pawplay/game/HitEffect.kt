package com.yourcompany.pawplay.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class HitEffect(
    private val origin: PointF,
    private val color: androidx.compose.ui.graphics.Color
) {

    private data class StarParticle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var outerR: Float,
        var alpha: Float,
        var decay: Float,
        var shrink: Float,
        var rotation: Float,
        var rotationSpeed: Float,
        var colorVariant: Int
    )

    private val particles: List<StarParticle>
    private var elapsed = 0f
    private val duration = 1.0f

    val isFinished get() = elapsed >= duration

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val path = Path()

    private val baseR = (color.red * 255).toInt()
    private val baseG = (color.green * 255).toInt()
    private val baseB = (color.blue * 255).toInt()

    init {
        val count = 8 + (Math.random() * 6).toInt()
        particles = List(count) { i ->
            val angle = Math.random() * 2 * PI
            val speed = 60f + Math.random().toFloat() * 180f
            val isGold = i < count / 2
            StarParticle(
                x = origin.x,
                y = origin.y,
                vx = (cos(angle) * speed).toFloat(),
                vy = (sin(angle) * speed).toFloat(),
                outerR = 6f + Math.random().toFloat() * 10f,
                alpha = 1f,
                decay = 0.8f + Math.random().toFloat() * 0.6f,
                shrink = 0.3f + Math.random().toFloat() * 0.4f,
                rotation = Math.random().toFloat() * 360f,
                rotationSpeed = 120f + Math.random().toFloat() * 240f,
                colorVariant = if (isGold) 0 else 1
            )
        }
    }

    fun update(deltaSeconds: Float) {
        if (isFinished) return
        elapsed += deltaSeconds

        for (p in particles) {
            p.x += p.vx * deltaSeconds
            p.y += p.vy * deltaSeconds
            p.vy += 80f * deltaSeconds
            p.vx *= (1f - 1.0f * deltaSeconds)
            p.alpha = (p.alpha - p.decay * deltaSeconds).coerceAtLeast(0f)
            p.outerR = (p.outerR - p.shrink * deltaSeconds * 4f).coerceAtLeast(0f)
            p.rotation += p.rotationSpeed * deltaSeconds
        }
    }

    fun draw(canvas: Canvas) {
        if (isFinished) return

        val ringProgress = elapsed / duration
        if (ringProgress < 0.4f) {
            val ringAlpha = (1f - ringProgress * 2.5f)
            val ringRadius = ringProgress * 180f

            fillPaint.color = Color.argb(
                (ringAlpha * 40).toInt().coerceIn(0, 255),
                baseR.coerceIn(0, 255),
                baseG.coerceIn(0, 255),
                baseB.coerceIn(0, 255)
            )
            canvas.drawCircle(origin.x, origin.y, ringRadius, fillPaint)

            strokePaint.color = Color.argb(
                (ringAlpha * 150).toInt().coerceIn(0, 255), 255, 255, 255
            )
            strokePaint.strokeWidth = 2.5f
            canvas.drawCircle(origin.x, origin.y, ringRadius, strokePaint)
        }

        for (p in particles) {
            if (p.alpha <= 0.01f || p.outerR <= 0.5f) continue

            val r: Int
            val g: Int
            val b: Int
            if (p.colorVariant == 0) {
                r = 255; g = 210; b = 50
            } else {
                r = baseR.coerceIn(0, 255)
                g = baseG.coerceIn(0, 255)
                b = baseB.coerceIn(0, 255)
            }

            val a = (p.alpha * 255).toInt().coerceIn(0, 255)
            drawStar(canvas, p.x, p.y, p.outerR, p.outerR * 0.4f, Color.argb(a, r, g, b), p.rotation)

            if (p.outerR > 4f) {
                fillPaint.color = Color.argb((a * 0.3f).toInt().coerceIn(0, 255), r, g, b)
                canvas.drawCircle(p.x, p.y, p.outerR * 1.5f, fillPaint)
            }
        }

        if (ringProgress < 0.15f) {
            val flashAlpha = (1f - ringProgress / 0.15f) * 0.4f
            fillPaint.color = Color.argb(
                (flashAlpha * 255).toInt().coerceIn(0, 255), 255, 255, 230
            )
            canvas.drawCircle(origin.x, origin.y, 30f + ringProgress * 100f, fillPaint)
        }
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, outerR: Float, innerR: Float, color: Int, rotation: Float) {
        fillPaint.color = color
        path.reset()
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outerR else innerR
            val angle = (i * 36f + rotation - 90f) * PI.toFloat() / 180f
            val x = cx + r * cos(angle)
            val y = cy + r * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, fillPaint)
    }
}
