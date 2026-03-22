package com.yourcompany.pawplay.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object PreyDrawer {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val p = Path()

    fun drawPrey(canvas: Canvas, toyId: Int, cx: Float, cy: Float, size: Float, phase: Float) {
        drawGroundGlow(canvas, cx, cy, size, glowColor(toyId), phase)
        drawGroundShadow(canvas, cx, cy + size * 0.42f, size)
        when (toyId) {
            1 -> laserDot(canvas, cx, cy, size, phase)
            2 -> fish(canvas, cx, cy, size, phase)
            3 -> cockroach(canvas, cx, cy, size, phase)
            4 -> butterfly(canvas, cx, cy, size, phase)
            5 -> mouse(canvas, cx, cy, size, phase)
            6 -> spider(canvas, cx, cy, size, phase)
            7 -> bee(canvas, cx, cy, size, phase)
            8 -> feather(canvas, cx, cy, size, phase)
            9 -> bird(canvas, cx, cy, size, phase)
            10 -> yarnBall(canvas, cx, cy, size, phase)
            else -> laserDot(canvas, cx, cy, size, phase)
        }
    }

    private fun glowColor(id: Int) = when (id) {
        1 -> Color.rgb(255, 60, 60)
        2 -> Color.rgb(50, 140, 255)
        3 -> Color.rgb(160, 120, 70)
        4 -> Color.rgb(200, 80, 255)
        5 -> Color.rgb(160, 160, 180)
        6 -> Color.rgb(100, 60, 120)
        7 -> Color.rgb(255, 220, 50)
        8 -> Color.rgb(220, 220, 240)
        9 -> Color.rgb(50, 210, 180)
        10 -> Color.rgb(255, 130, 30)
        else -> Color.WHITE
    }

    private fun drawGroundGlow(c: Canvas, cx: Float, cy: Float, size: Float, color: Int, phase: Float) {
        val pulse = 1f + 0.12f * sin(phase * 3f).toFloat()
        val r = size * 0.9f * pulse
        glow.shader = RadialGradient(cx, cy, r,
            intArrayOf(Color.argb(100, Color.red(color), Color.green(color), Color.blue(color)),
                Color.argb(40, Color.red(color), Color.green(color), Color.blue(color)),
                Color.argb(0, Color.red(color), Color.green(color), Color.blue(color))),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, glow)
        glow.shader = null
    }

    private fun drawGroundShadow(c: Canvas, cx: Float, cy: Float, size: Float) {
        glow.shader = RadialGradient(cx, cy, size * 0.5f,
            intArrayOf(Color.argb(70, 0, 0, 0), Color.argb(0, 0, 0, 0)), null, Shader.TileMode.CLAMP)
        c.drawOval(cx - size * 0.4f, cy - size * 0.12f, cx + size * 0.4f, cy + size * 0.12f, glow)
        glow.shader = null
    }

    private fun highlight(c: Canvas, cx: Float, cy: Float, r: Float) {
        fill.color = Color.argb(70, 255, 255, 255)
        c.drawCircle(cx, cy, r, fill)
    }

    // ── Laser Dot ──
    private fun laserDot(c: Canvas, cx: Float, cy: Float, s: Float, ph: Float) {
        val r = s * 0.28f * (1f + 0.18f * sin(ph * 5f).toFloat())
        fill.shader = RadialGradient(cx - r * 0.2f, cy - r * 0.2f, r * 1.8f,
            intArrayOf(Color.WHITE, Color.rgb(255, 60, 60), Color.rgb(180, 0, 0)),
            floatArrayOf(0f, 0.35f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, fill)
        fill.shader = null
        highlight(c, cx - r * 0.3f, cy - r * 0.3f, r * 0.35f)
    }

    // ── Fish ──
    private fun fish(c: Canvas, cx: Float, cy: Float, s: Float, ph: Float) {
        c.save(); c.translate(cx, cy)
        val w = s * 0.48f; val h = s * 0.28f
        val tw = sin(ph * 4f).toFloat() * s * 0.04f

        fill.color = Color.rgb(0, 80, 200)
        p.reset()
        p.moveTo(-w * 0.9f, 0f)
        p.lineTo(-w * 1.2f, -h * 0.5f + tw)
        p.lineTo(-w * 1.0f, 0f)
        p.lineTo(-w * 1.2f, h * 0.5f + tw)
        p.close()
        c.drawPath(p, fill)

        fill.shader = LinearGradient(0f, -h, 0f, h,
            Color.rgb(60, 180, 255), Color.rgb(10, 80, 200), Shader.TileMode.CLAMP)
        p.reset()
        p.moveTo(-w, 0f)
        p.cubicTo(-w * 0.5f, -h * 1.1f, w * 0.5f, -h * 1.1f, w, 0f)
        p.cubicTo(w * 0.5f, h * 1.1f, -w * 0.5f, h * 1.1f, -w, 0f)
        c.drawPath(p, fill)
        fill.shader = null

        fill.color = Color.rgb(80, 210, 255)
        p.reset()
        p.moveTo(w * 0.1f, -h * 0.2f)
        p.lineTo(w * 0.2f, -h * 0.7f)
        p.lineTo(w * 0.4f, -h * 0.15f)
        p.close()
        c.drawPath(p, fill)

        fill.shader = LinearGradient(-w * 0.3f, -h * 0.6f, w * 0.3f, 0f,
            Color.argb(90, 180, 230, 255), Color.argb(0, 180, 230, 255), Shader.TileMode.CLAMP)
        c.drawOval(-w * 0.4f, -h * 0.7f, w * 0.3f, -h * 0.05f, fill)
        fill.shader = null

        stroke.color = Color.rgb(20, 60, 160); stroke.strokeWidth = 1.5f
        for (i in 0..4) { val sx = -w * 0.35f + i * w * 0.16f; c.drawLine(sx, -h * 0.25f, sx, h * 0.25f, stroke) }

        fill.color = Color.WHITE; c.drawCircle(w * 0.5f, -h * 0.2f, s * 0.055f, fill)
        fill.color = Color.rgb(10, 10, 50); c.drawCircle(w * 0.53f, -h * 0.2f, s * 0.03f, fill)
        highlight(c, w * 0.48f, -h * 0.25f, s * 0.018f)
        c.restore()
    }

    // ── Cockroach ──
    private fun cockroach(c: Canvas, cx: Float, cy: Float, s: Float, ph: Float) {
        c.save(); c.translate(cx, cy)
        val bw = s * 0.22f; val bh = s * 0.38f
        val lw = sin(ph * 8f).toFloat() * s * 0.04f

        stroke.color = Color.rgb(90, 55, 30); stroke.strokeWidth = 2.5f
        for (i in -1..1) {
            val ly = i * bh * 0.35f; val d = if (i % 2 == 0) 1 else -1
            c.drawLine(-bw, ly, -bw - s * 0.25f, ly + lw * d, stroke)
            c.drawLine(bw, ly, bw + s * 0.25f, ly - lw * d, stroke)
        }

        fill.shader = LinearGradient(-bw, -bh, bw, bh,
            Color.rgb(130, 85, 45), Color.rgb(65, 38, 18), Shader.TileMode.CLAMP)
        c.drawOval(-bw, -bh, bw, bh * 0.6f, fill)
        fill.shader = null

        fill.shader = RadialGradient(0f, -bh * 0.15f, bw * 1.2f,
            Color.rgb(145, 95, 50), Color.rgb(80, 48, 22), Shader.TileMode.CLAMP)
        c.drawOval(-bw * 0.95f, -bh * 0.85f, bw * 0.95f, bh * 0.5f, fill)
        fill.shader = null

        fill.color = Color.argb(40, 200, 170, 120)
        c.drawOval(-bw * 0.3f, -bh * 0.7f, bw * 0.3f, bh * 0.2f, fill)

        fill.color = Color.rgb(55, 32, 15)
        c.drawCircle(0f, -bh * 0.85f, bw * 0.5f, fill)

        fill.color = Color.rgb(30, 15, 5)
        c.drawCircle(-bw * 0.15f, -bh * 0.9f, s * 0.02f, fill)
        c.drawCircle(bw * 0.15f, -bh * 0.9f, s * 0.02f, fill)

        stroke.color = Color.rgb(100, 65, 35); stroke.strokeWidth = 1.8f
        c.drawLine(-bw * 0.1f, -bh * 1.05f, -bw * 0.4f, -bh * 1.4f, stroke)
        c.drawLine(bw * 0.1f, -bh * 1.05f, bw * 0.4f, -bh * 1.4f, stroke)

        c.restore()
    }

    // ── Butterfly ──
    private fun butterfly(c: Canvas, cx: Float, cy: Float, s: Float, ph: Float) {
        c.save(); c.translate(cx, cy)
        val flap = sin(ph * 4.5f).toFloat()
        val scaleX = 0.65f + 0.35f * flap

        c.save(); c.scale(-scaleX, 1f); wing(c, s, Color.rgb(220, 60, 255), Color.rgb(255, 120, 200)); c.restore()
        c.save(); c.scale(scaleX, 1f); wing(c, s, Color.rgb(220, 60, 255), Color.rgb(255, 120, 200)); c.restore()

        fill.color = Color.rgb(50, 25, 65)
        c.drawRoundRect(-s * 0.025f, -s * 0.32f, s * 0.025f, s * 0.28f, s * 0.02f, s * 0.02f, fill)

        stroke.color = Color.rgb(50, 25, 65); stroke.strokeWidth = 2f
        c.drawLine(0f, -s * 0.32f, -s * 0.12f, -s * 0.48f, stroke)
        c.drawLine(0f, -s * 0.32f, s * 0.12f, -s * 0.48f, stroke)
        fill.color = Color.rgb(255, 210, 60)
        c.drawCircle(-s * 0.12f, -s * 0.48f, s * 0.025f, fill)
        c.drawCircle(s * 0.12f, -s * 0.48f, s * 0.025f, fill)
        c.restore()
    }

    private fun wing(c: Canvas, s: Float, c1: Int, c2: Int) {
        fill.shader = RadialGradient(s * 0.18f, -s * 0.08f, s * 0.38f, c1, c2, Shader.TileMode.CLAMP)
        p.reset()
        p.moveTo(0f, -s * 0.08f)
        p.cubicTo(s * 0.12f, -s * 0.5f, s * 0.5f, -s * 0.45f, s * 0.42f, -s * 0.02f)
        p.cubicTo(s * 0.5f, s * 0.15f, s * 0.12f, s * 0.35f, 0f, s * 0.15f)
        p.close()
        c.drawPath(p, fill)
        fill.shader = null

        fill.color = Color.argb(90, 255, 255, 220)
        c.drawCircle(s * 0.22f, -s * 0.14f, s * 0.06f, fill)
        c.drawCircle(s * 0.18f, s * 0.08f, s * 0.045f, fill)

        fill.color = Color.argb(50, 255, 200, 255)
        c.drawCircle(s * 0.3f, -s * 0.06f, s * 0.035f, fill)

        stroke.color = Color.argb(50, 80, 0, 120); stroke.strokeWidth = 1f
        c.drawLine(s * 0.05f, -s * 0.05f, s * 0.35f, -s * 0.2f, stroke)
        c.drawLine(s * 0.05f, s * 0.05f, s * 0.35f, s * 0.1f, stroke)
    }

    // ── Mouse ──
    private fun mouse(c: Canvas, cx: Float, cy: Float, s: Float, ph: Float) {
        c.save(); c.translate(cx, cy)
        val tw = sin(ph * 5f).toFloat() * s * 0.06f

        stroke.color = Color.rgb(210, 180, 180); stroke.strokeWidth = 3f
        p.reset()
        p.moveTo(-s * 0.25f, s * 0.05f)
        p.cubicTo(-s * 0.4f, tw, -s * 0.5f, -tw, -s * 0.55f, tw * 0.5f)
        c.drawPath(p, stroke)

        fill.shader = RadialGradient(0f, -s * 0.05f, s * 0.35f,
            Color.rgb(185, 185, 195), Color.rgb(140, 140, 155), Shader.TileMode.CLAMP)
        c.drawOval(-s * 0.25f, -s * 0.2f, s * 0.2f, s * 0.15f, fill)
        fill.shader = null

        fill.color = Color.rgb(175, 175, 185)
        c.drawCircle(s * 0.18f, -s * 0.06f, s * 0.12f, fill)

        fill.color = Color.rgb(220, 185, 185)
        c.drawOval(s * 0.06f, -s * 0.28f, s * 0.18f, -s * 0.14f, fill)
        c.drawOval(s * 0.2f, -s * 0.28f, s * 0.32f, -s * 0.14f, fill)
        fill.color = Color.rgb(245, 210, 210)
        c.drawOval(s * 0.09f, -s * 0.25f, s * 0.15f, -s * 0.17f, fill)
        c.drawOval(s * 0.23f, -s * 0.25f, s * 0.29f, -s * 0.17f, fill)

        fill.color = Color.rgb(10, 10, 20)
        c.drawCircle(s * 0.26f, -s * 0.04f, s * 0.022f, fill)
        fill.color = Color.rgb(255, 170, 170)
        c.drawCircle(s * 0.3f, -s * 0.01f, s * 0.018f, fill)

        stroke.color = Color.rgb(130, 130, 140); stroke.strokeWidth = 1.3f
        c.drawLine(s * 0.29f, -s * 0.04f, s * 0.42f, -s * 0.1f, stroke)
        c.drawLine(s * 0.29f, -s * 0.01f, s * 0.42f, -s * 0.01f, stroke)
        c.drawLine(s * 0.29f, s * 0.02f, s * 0.42f, s * 0.06f, stroke)

        highlight(c, s * 0.08f, -s * 0.12f, s * 0.06f)
        c.restore()
    }

    // ── Spider ──
    private fun spider(c: Canvas, cx: Float, cy: Float, s: Float, ph: Float) {
        c.save(); c.translate(cx, cy)
        val lw = sin(ph * 6f).toFloat() * s * 0.025f

        stroke.color = Color.rgb(60, 60, 60); stroke.strokeWidth = 1f
        c.drawLine(0f, -s * 0.6f, 0f, -s * 0.15f, stroke)

        stroke.color = Color.rgb(35, 30, 40); stroke.strokeWidth = 2.8f
        for (i in 0..3) {
            val baseAngle = -70f + i * 22f
            val rad = Math.toRadians(baseAngle.toDouble())
            val mid = s * 0.22f; val tip = s * 0.42f
            val w = lw * (if (i % 2 == 0) 1 else -1)
            val my = -s * 0.06f + i * s * 0.04f
            c.drawLine(0f, my, (-mid * cos(rad) + w).toFloat(), (-mid * sin(rad)).toFloat(), stroke)
            c.drawLine((-mid * cos(rad) + w).toFloat(), (-mid * sin(rad)).toFloat(),
                (-tip * cos(rad) + w * 1.4f).toFloat(), (-tip * sin(rad) + s * 0.15f).toFloat(), stroke)
            c.drawLine(0f, my, (mid * cos(rad) - w).toFloat(), (-mid * sin(rad)).toFloat(), stroke)
            c.drawLine((mid * cos(rad) - w).toFloat(), (-mid * sin(rad)).toFloat(),
                (tip * cos(rad) - w * 1.4f).toFloat(), (-tip * sin(rad) + s * 0.15f).toFloat(), stroke)
        }

        fill.shader = RadialGradient(0f, 0f, s * 0.18f,
            Color.rgb(45, 40, 50), Color.rgb(20, 18, 25), Shader.TileMode.CLAMP)
        c.drawCircle(0f, 0f, s * 0.16f, fill)
        fill.shader = null
        fill.color = Color.rgb(35, 30, 40)
        c.drawOval(-s * 0.09f, -s * 0.26f, s * 0.09f, -s * 0.1f, fill)

        fill.color = Color.rgb(200, 20, 20)
        c.drawCircle(0f, s * 0.04f, s * 0.045f, fill)
        fill.color = Color.rgb(80, 15, 15)
        c.drawCircle(-s * 0.03f, -s * 0.04f, s * 0.028f, fill)
        c.drawCircle(s * 0.03f, -s * 0.04f, s * 0.028f, fill)

        highlight(c, -s * 0.05f, -s * 0.05f, s * 0.04f)
        c.restore()
    }

    // ── Bee ──
    private fun bee(c: Canvas, cx: Float, cy: Float, s: Float, ph: Float) {
        c.save(); c.translate(cx, cy)
        val wingA = sin(ph * 14f).toFloat() * 18f

        fill.color = Color.argb(90, 210, 230, 255)
        c.save(); c.rotate(wingA, -s * 0.1f, -s * 0.2f)
        c.drawOval(-s * 0.3f, -s * 0.42f, -s * 0.01f, -s * 0.06f, fill); c.restore()
        c.save(); c.rotate(-wingA, s * 0.1f, -s * 0.2f)
        c.drawOval(s * 0.01f, -s * 0.42f, s * 0.3f, -s * 0.06f, fill); c.restore()

        fill.shader = RadialGradient(0f, 0f, s * 0.22f,
            Color.rgb(255, 215, 30), Color.rgb(220, 170, 0), Shader.TileMode.CLAMP)
        c.drawOval(-s * 0.16f, -s * 0.24f, s * 0.16f, s * 0.24f, fill)
        fill.shader = null

        fill.color = Color.rgb(30, 25, 20)
        for (i in 0..2) { val y = -s * 0.1f + i * s * 0.1f; c.drawRect(-s * 0.17f, y, s * 0.17f, y + s * 0.04f, fill) }

        fill.color = Color.rgb(35, 30, 25)
        c.drawCircle(0f, -s * 0.28f, s * 0.08f, fill)
        fill.color = Color.WHITE
        c.drawCircle(-s * 0.03f, -s * 0.3f, s * 0.02f, fill)
        c.drawCircle(s * 0.03f, -s * 0.3f, s * 0.02f, fill)

        stroke.color = Color.rgb(35, 30, 25); stroke.strokeWidth = 1.5f
        c.drawLine(-s * 0.02f, -s * 0.35f, -s * 0.08f, -s * 0.45f, stroke)
        c.drawLine(s * 0.02f, -s * 0.35f, s * 0.08f, -s * 0.45f, stroke)

        highlight(c, -s * 0.05f, -s * 0.1f, s * 0.06f)
        c.restore()
    }

    // ── Feather ──
    private fun feather(c: Canvas, cx: Float, cy: Float, s: Float, ph: Float) {
        c.save(); c.translate(cx, cy); c.rotate(sin(ph * 2f).toFloat() * 18f)

        fill.shader = LinearGradient(-s * 0.1f, -s * 0.45f, s * 0.1f, s * 0.45f,
            Color.rgb(245, 245, 255), Color.rgb(210, 210, 230), Shader.TileMode.CLAMP)
        p.reset()
        p.moveTo(0f, -s * 0.45f)
        p.cubicTo(s * 0.22f, -s * 0.2f, s * 0.18f, s * 0.15f, 0f, s * 0.45f)
        p.cubicTo(-s * 0.18f, s * 0.15f, -s * 0.22f, -s * 0.2f, 0f, -s * 0.45f)
        c.drawPath(p, fill)
        fill.shader = null

        stroke.color = Color.rgb(190, 190, 210); stroke.strokeWidth = 1.5f
        c.drawLine(0f, -s * 0.4f, 0f, s * 0.4f, stroke)
        stroke.strokeWidth = 0.8f
        for (i in 1..7) {
            val y = -s * 0.35f + i * s * 0.09f; val xo = s * 0.1f * (1f - i / 8f)
            c.drawLine(0f, y, -xo, y - s * 0.04f, stroke)
            c.drawLine(0f, y, xo, y - s * 0.04f, stroke)
        }

        fill.color = Color.argb(40, 255, 255, 255)
        c.drawOval(-s * 0.08f, -s * 0.3f, s * 0.04f, s * 0.05f, fill)
        c.restore()
    }

    // ── Bird ──
    private fun bird(c: Canvas, cx: Float, cy: Float, s: Float, ph: Float) {
        c.save(); c.translate(cx, cy)
        val wAngle = sin(ph * 6f).toFloat() * 28f

        fill.shader = RadialGradient(0f, 0f, s * 0.25f,
            Color.rgb(30, 220, 190), Color.rgb(0, 150, 130), Shader.TileMode.CLAMP)
        c.drawOval(-s * 0.22f, -s * 0.14f, s * 0.18f, s * 0.18f, fill)
        fill.shader = null

        fill.color = Color.rgb(0, 170, 145)
        c.save(); c.rotate(wAngle, -s * 0.08f, 0f)
        p.reset(); p.moveTo(-s * 0.08f, 0f)
        p.cubicTo(-s * 0.2f, -s * 0.38f, -s * 0.48f, -s * 0.28f, -s * 0.42f, 0f); p.close()
        c.drawPath(p, fill); c.restore()
        c.save(); c.rotate(-wAngle, s * 0.04f, 0f)
        p.reset(); p.moveTo(s * 0.04f, 0f)
        p.cubicTo(s * 0.16f, -s * 0.38f, s * 0.44f, -s * 0.28f, s * 0.38f, 0f); p.close()
        c.drawPath(p, fill); c.restore()

        fill.color = Color.rgb(40, 230, 200)
        c.drawCircle(s * 0.13f, -s * 0.06f, s * 0.1f, fill)
        fill.color = Color.WHITE; c.drawCircle(s * 0.16f, -s * 0.08f, s * 0.035f, fill)
        fill.color = Color.rgb(10, 10, 30); c.drawCircle(s * 0.17f, -s * 0.08f, s * 0.018f, fill)
        highlight(c, s * 0.15f, -s * 0.1f, s * 0.012f)

        fill.color = Color.rgb(255, 160, 0)
        p.reset(); p.moveTo(s * 0.22f, -s * 0.04f); p.lineTo(s * 0.34f, -s * 0.06f)
        p.lineTo(s * 0.22f, 0f); p.close()
        c.drawPath(p, fill)

        fill.color = Color.rgb(0, 150, 130)
        p.reset(); p.moveTo(-s * 0.22f, s * 0.04f); p.lineTo(-s * 0.38f, s * 0.1f)
        p.lineTo(-s * 0.35f, 0f); p.close()
        c.drawPath(p, fill)
        c.restore()
    }

    // ── Yarn Ball ──
    private fun yarnBall(c: Canvas, cx: Float, cy: Float, s: Float, ph: Float) {
        c.save(); c.translate(cx, cy); c.rotate(ph * 40f)
        val r = s * 0.28f

        fill.shader = RadialGradient(-r * 0.3f, -r * 0.3f, r * 1.6f,
            Color.rgb(255, 140, 40), Color.rgb(190, 70, 0), Shader.TileMode.CLAMP)
        c.drawCircle(0f, 0f, r, fill)
        fill.shader = null

        stroke.color = Color.rgb(230, 110, 15); stroke.strokeWidth = 2f
        for (i in 0..6) {
            val a1 = (i * 51f + 10f) * PI.toFloat() / 180f
            val a2 = a1 + PI.toFloat() * 0.55f
            p.reset()
            p.moveTo(r * 0.65f * cos(a1), r * 0.65f * sin(a1))
            p.cubicTo(r * 0.25f * cos(a1 + 0.4f), r * 0.25f * sin(a1 + 0.4f),
                -r * 0.25f * cos(a2 - 0.4f), -r * 0.25f * sin(a2 - 0.4f),
                r * 0.65f * cos(a2), r * 0.65f * sin(a2))
            c.drawPath(p, stroke)
        }

        highlight(c, -r * 0.25f, -r * 0.25f, r * 0.22f)
        c.restore()

        stroke.color = Color.rgb(255, 130, 25); stroke.strokeWidth = 2.8f
        val tw = sin(ph * 3f).toFloat() * s * 0.05f
        p.reset()
        p.moveTo(cx + r * 0.7f, cy + r * 0.3f)
        p.cubicTo(cx + r * 1.2f, cy + tw, cx + r * 1.5f, cy + r * 0.4f + tw, cx + r * 1.7f, cy + r * 0.2f)
        c.drawPath(p, stroke)
    }
}
