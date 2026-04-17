package com.pawhunt.app.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hit burst effect: radial star explosion with velocity + damping,
 * warm center flash, multi-color star particles.
 */
class HitEffect(
    private val center: PointF,
    private val baseColor: androidx.compose.ui.graphics.Color
) {

    private data class StarParticle(
        var x: Float, var y: Float,
        var vx: Float, var vy: Float,
        val size: Float, val rotation: Float,
        val r: Int, val g: Int, val b: Int,
        val maxLife: Float,
        var life: Float
    )

    private val particles = mutableListOf<StarParticle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val path = Path()

    private val damping = 0.92f
    var isFinished = false
        private set

    private var flashAlpha = 255f
    private var age = 0f
    private val flashDuration = 0.15f
    private val totalDuration = 1.2f

    private val ar = (baseColor.red * 255).toInt().coerceIn(0, 255)
    private val ag = (baseColor.green * 255).toInt().coerceIn(0, 255)
    private val ab = (baseColor.blue * 255).toInt().coerceIn(0, 255)

    init {
        val count = 18 + (Math.random() * 8).toInt()
        for (i in 0 until count) {
            val angle = Math.random().toFloat() * 2f * PI.toFloat()
            val speed = 200f + Math.random().toFloat() * 350f
            val sz = 8f + Math.random().toFloat() * 14f
            val life = 0.6f + Math.random().toFloat() * 0.5f

            val variant = Math.random()
            val r: Int; val g: Int; val b: Int
            when {
                variant < 0.3 -> { r = 255; g = 230; b = 50 }
                variant < 0.5 -> { r = 255; g = 160; b = 30 }
                variant < 0.7 -> { r = 80; g = 200; b = 255 }
                variant < 0.85 -> { r = 100; g = 255; b = 150 }
                else -> { r = ar; g = ag; b = ab }
            }

            particles.add(StarParticle(
                center.x, center.y,
                cos(angle) * speed, sin(angle) * speed,
                sz, Math.random().toFloat() * 360f,
                r, g, b, life, life
            ))
        }
    }

    fun update(deltaSeconds: Float) {
        age += deltaSeconds
        flashAlpha = ((1f - age / flashDuration) * 255f).coerceIn(0f, 255f)

        for (p in particles) {
            p.vx *= damping; p.vy *= damping
            p.x += p.vx * deltaSeconds
            p.y += p.vy * deltaSeconds
            p.life -= deltaSeconds
        }

        if (age >= totalDuration) isFinished = true
    }

    fun draw(canvas: Canvas) {
        if (flashAlpha > 5f) {
            val r = 60f + (1f - age / flashDuration).coerceIn(0f, 1f) * 80f
            glowPaint.shader = RadialGradient(center.x, center.y, r,
                Color.argb(flashAlpha.toInt(), 255, 240, 180),
                Color.argb(0, 255, 240, 180),
                Shader.TileMode.CLAMP)
            canvas.drawCircle(center.x, center.y, r, glowPaint)
            glowPaint.shader = null
        }

        for (p in particles) {
            if (p.life <= 0f) continue
            val progress = p.life / p.maxLife
            val alpha = (progress * 240).toInt().coerceIn(0, 255)
            val scale = 0.4f + 0.6f * progress
            val sz = p.size * scale
            paint.color = Color.argb(alpha, p.r, p.g, p.b)
            drawStar(canvas, p.x, p.y, sz, sz * 0.35f, 4, p.rotation)
        }
    }

    private fun drawStar(c: Canvas, cx: Float, cy: Float, outer: Float, inner: Float, points: Int, rotation: Float) {
        path.reset()
        val step = PI.toFloat() / points
        val rotRad = rotation * PI.toFloat() / 180f
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) outer else inner
            val angle = i * step - PI.toFloat() / 2 + rotRad
            val x = cx + r * cos(angle)
            val y = cy + r * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        c.drawPath(path, paint)
    }
}
