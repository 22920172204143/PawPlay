package com.pawhunt.app.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class TrailParticleSystem {

    private data class Star(
        val x: Float, val y: Float,
        val size: Float, val rotation: Float,
        val r: Int, val g: Int, val b: Int,
        val maxLife: Float,
        var life: Float
    )

    private val stars = mutableListOf<Star>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val path = Path()

    private var emitCooldown = 0f

    private fun emit(x: Float, y: Float, @Suppress("UNUSED_PARAMETER") color: Int) {
        val size = 14f + Math.random().toFloat() * 14f
        val rotation = Math.random().toFloat() * 360f
        val maxLife = 1.5f + Math.random().toFloat() * 1.0f

        val variant = Math.random()
        val r: Int; val g: Int; val b: Int
        if (variant < 0.5) {
            r = 255; g = 200 + (Math.random() * 55).toInt(); b = 40 + (Math.random() * 50).toInt()
        } else {
            r = 240 + (Math.random() * 15).toInt(); g = 170 + (Math.random() * 60).toInt(); b = 20 + (Math.random() * 30).toInt()
        }

        stars.add(Star(
            x + (Math.random().toFloat() - 0.5f) * 40f,
            y + (Math.random().toFloat() - 0.5f) * 40f,
            size, rotation, r, g, b, maxLife, maxLife
        ))

        if (stars.size > 60) {
            stars.removeAt(0)
        }
    }

    fun update(deltaSeconds: Float) {
        val iter = stars.iterator()
        while (iter.hasNext()) {
            val s = iter.next()
            s.life -= deltaSeconds
            if (s.life <= 0f) iter.remove()
        }
    }

    fun tryEmit(x: Float, y: Float, color: Int, deltaSeconds: Float, isMoving: Boolean) {
        if (!isMoving) return
        emitCooldown -= deltaSeconds
        if (emitCooldown <= 0f) {
            emit(x, y, color)
            emitCooldown = 0.12f + Math.random().toFloat() * 0.10f
        }
    }

    fun draw(canvas: Canvas) {
        for (s in stars) {
            val progress = s.life / s.maxLife
            val alpha = (progress * 240).toInt().coerceIn(0, 255)
            val scale = 0.3f + 0.7f * progress
            val sz = s.size * scale
            paint.color = Color.argb(alpha, s.r, s.g, s.b)
            drawStar(canvas, s.x, s.y, sz, sz * 0.4f, 5, s.rotation, paint)
        }
    }

    private fun drawStar(
        c: Canvas, cx: Float, cy: Float,
        outer: Float, inner: Float, points: Int,
        rotation: Float, p: Paint
    ) {
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
        c.drawPath(path, p)
    }

    fun clear() {
        stars.clear()
    }
}
