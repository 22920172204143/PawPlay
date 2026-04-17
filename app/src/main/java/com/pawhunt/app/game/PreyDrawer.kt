package com.pawhunt.app.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Four-layer prey renderer:
 *   Layer 1 — Shadow (lagged silhouette, not a plain circle)
 *   Layer 2 — Body (main shape with gradients & texture)
 *   Layer 3 — Glow / Bloom (edge halo for dark-background pop)
 *   Layer 4 — Appendages (antennae, wings, tails — animated independently)
 *
 * Each prey also receives `scaleX` / `scaleY` from PreyEntity for squash-and-stretch.
 */
object PreyDrawer {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val path = Path()

    fun drawPrey(
        canvas: Canvas, toyId: Int,
        cx: Float, cy: Float, size: Float,
        phase: Float,
        scaleX: Float = 1f, scaleY: Float = 1f,
        heading: Float = 0f, bodyWobble: Float = 0f
    ) {
        val renderHeading = heading + modelHeadingOffset(toyId)
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(renderHeading + bodyWobble)
        canvas.scale(scaleX, scaleY)
        when (toyId) {
            1 -> ladybug(canvas, size, phase)
            2 -> fish(canvas, size, phase)
            3 -> cockroach(canvas, size, phase)
            4 -> butterfly(canvas, size, phase)
            5 -> mouse(canvas, size, phase)
            6 -> spider(canvas, size, phase)
            7 -> bee(canvas, size, phase)
            8 -> feather(canvas, size, phase)
            9 -> bird(canvas, size, phase)
            10 -> yarnBall(canvas, size, phase)
            else -> ladybug(canvas, size, phase)
        }
        canvas.restore()
    }

    private fun modelHeadingOffset(toyId: Int): Float {
        return when (toyId) {
            // The glowing bug/ladybug art is authored diagonally,
            // with its head pointing roughly to the upper-left.
            1 -> 135f
            // These models are authored facing upward in local space,
            // so rotate them 90 degrees to match movement heading.
            3, 4, 7 -> 90f
            else -> 0f
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  1 — GLOWING FIREFLY BUG
    //
    //  Round orange body with yellow spiral decorations & segment lines.
    //  Head at top-left with large dark eyes and segmented antennae
    //  tipped with glowing cyan orbs. Four translucent cyan leaf-shaped
    //  dragonfly wings that flap. Golden stars float near antennae.
    // ════════════════════════════════════════════════════════════════

    private fun ladybug(c: Canvas, s: Float, ph: Float) {
        val r = s * 0.32f
        val headR = r * 0.45f
        val headX = -r * 0.55f
        val headY = -r * 0.75f

        // Shadow
        drawShapeShadow(c, 0f, r * 1.1f, r * 1.15f, r * 0.22f, 90)

        // Body glow — warm orange
        drawGlow(c, 0f, 0f, r * 2.0f, Color.rgb(240, 160, 30), ph)

        // Wing glow — cyan, behind the body
        val wingGlowX = r * 0.4f
        val wingGlowY = -r * 0.5f
        fill.shader = RadialGradient(wingGlowX, wingGlowY, r * 1.6f,
            intArrayOf(Color.argb(50, 0, 240, 220), Color.argb(15, 0, 220, 200), Color.argb(0, 0, 200, 180)),
            floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(wingGlowX, wingGlowY, r * 1.6f, fill)
        fill.shader = null

        // ── WINGS (4 translucent cyan/mint leaf-shaped wings) ──
        val wingFlap = sin(ph * 5.5f).toFloat()
        val upperAngle = 8f + wingFlap * 14f
        val lowerAngle = -5f + wingFlap * 10f
        val wingAlpha = 110

        // Upper-left wing
        c.save()
        c.rotate(upperAngle, r * 0.15f, -r * 0.35f)
        drawLeafWing(c, r * 0.15f, -r * 0.35f, r * 1.15f, r * 0.42f, -55f, wingAlpha, ph)
        c.restore()

        // Upper-right wing
        c.save()
        c.rotate(-upperAngle - 5f, r * 0.25f, -r * 0.25f)
        drawLeafWing(c, r * 0.25f, -r * 0.25f, r * 1.05f, r * 0.38f, 35f, wingAlpha, ph)
        c.restore()

        // Lower-left wing
        c.save()
        c.rotate(lowerAngle + 15f, r * 0.05f, -r * 0.1f)
        drawLeafWing(c, r * 0.05f, -r * 0.1f, r * 0.85f, r * 0.32f, -35f, (wingAlpha * 0.8f).toInt(), ph)
        c.restore()

        // Lower-right wing
        c.save()
        c.rotate(-lowerAngle - 10f, r * 0.2f, r * 0.0f)
        drawLeafWing(c, r * 0.2f, 0f, r * 0.78f, r * 0.30f, 55f, (wingAlpha * 0.8f).toInt(), ph)
        c.restore()

        // ── BODY — round rich deep orange with spiral decorations ──
        fill.shader = RadialGradient(
            -r * 0.2f, -r * 0.15f, r * 1.5f,
            Color.rgb(195, 100, 10), Color.rgb(130, 48, 0), Shader.TileMode.CLAMP
        )
        c.drawCircle(0f, 0f, r, fill)
        fill.shader = null

        // Subtle underside shading to separate the body from dark textured backgrounds.
        fill.shader = RadialGradient(
            r * 0.22f, r * 0.32f, r * 0.95f,
            intArrayOf(Color.argb(78, 75, 22, 0), Color.argb(20, 50, 12, 0), Color.argb(0, 0, 0, 0)),
            floatArrayOf(0f, 0.62f, 1f), Shader.TileMode.CLAMP
        )
        c.drawOval(-r * 0.8f, -r * 0.08f, r * 0.98f, r * 0.92f, fill)
        fill.shader = null

        // Body segment lines
        stroke.color = Color.argb(80, 160, 80, 10)
        stroke.strokeWidth = r * 0.025f
        c.drawLine(-r * 0.7f, -r * 0.2f, r * 0.7f, -r * 0.15f, stroke)
        c.drawLine(-r * 0.75f, r * 0.15f, r * 0.65f, r * 0.2f, stroke)
        c.drawLine(-r * 0.6f, r * 0.48f, r * 0.5f, r * 0.52f, stroke)

        // Yellow spiral decorations on body
        stroke.color = Color.argb(140, 255, 210, 40)
        stroke.strokeWidth = r * 0.045f
        stroke.strokeCap = Paint.Cap.ROUND
        drawSpiral(c, -r * 0.15f, r * 0.1f, r * 0.25f, 1.8f)
        drawSpiral(c, r * 0.25f, -r * 0.3f, r * 0.18f, -1.5f)
        drawSpiral(c, -r * 0.35f, -r * 0.35f, r * 0.13f, 1.2f)

        // Body highlight
        fill.shader = RadialGradient(
            -r * 0.25f, -r * 0.3f, r * 0.55f,
            Color.argb(90, 255, 230, 160), Color.argb(0, 255, 230, 160), Shader.TileMode.CLAMP
        )
        c.drawCircle(-r * 0.25f, -r * 0.3f, r * 0.55f, fill)
        fill.shader = null

        // ── HEAD — smaller deep orange circle at upper-left ──
        fill.shader = RadialGradient(
            headX - headR * 0.15f, headY - headR * 0.1f, headR * 1.5f,
            Color.rgb(210, 125, 18), Color.rgb(155, 75, 5), Shader.TileMode.CLAMP
        )
        c.drawCircle(headX, headY, headR, fill)
        fill.shader = null

        // Soft occlusion where the head tucks into the thorax.
        fill.shader = RadialGradient(
            headX + headR * 0.6f, headY + headR * 0.55f, headR * 0.95f,
            intArrayOf(Color.argb(72, 70, 24, 0), Color.argb(18, 40, 10, 0), Color.argb(0, 0, 0, 0)),
            floatArrayOf(0f, 0.58f, 1f), Shader.TileMode.CLAMP
        )
        c.drawOval(headX - headR * 0.15f, headY + headR * 0.05f, headX + headR * 1.2f, headY + headR * 1.15f, fill)
        fill.shader = null

        // Head spiral decoration
        stroke.color = Color.argb(120, 255, 220, 50)
        stroke.strokeWidth = r * 0.035f
        drawSpiral(c, headX + headR * 0.1f, headY, headR * 0.3f, 1.5f)

        // Eyes — large dark navy spheres
        val eyeR = headR * 0.28f
        val eyeLX = headX - headR * 0.35f
        val eyeRX = headX + headR * 0.2f
        val eyeY = headY + headR * 0.1f

        fill.shader = RadialGradient(eyeLX - eyeR * 0.2f, eyeY - eyeR * 0.2f, eyeR * 1.2f,
            Color.rgb(30, 30, 80), Color.rgb(10, 10, 40), Shader.TileMode.CLAMP)
        c.drawCircle(eyeLX, eyeY, eyeR, fill)
        fill.shader = null

        fill.shader = RadialGradient(eyeRX - eyeR * 0.2f, eyeY - eyeR * 0.2f, eyeR * 1.2f,
            Color.rgb(30, 30, 80), Color.rgb(10, 10, 40), Shader.TileMode.CLAMP)
        c.drawCircle(eyeRX, eyeY, eyeR, fill)
        fill.shader = null

        // Eye highlights
        fill.color = Color.argb(200, 255, 255, 255)
        c.drawCircle(eyeLX - eyeR * 0.3f, eyeY - eyeR * 0.25f, eyeR * 0.25f, fill)
        c.drawCircle(eyeRX - eyeR * 0.3f, eyeY - eyeR * 0.25f, eyeR * 0.25f, fill)

        // ── ANTENNAE — segmented orange with cyan tip orbs ──
        val antWobble = sin(ph * 5.5f).toFloat() * r * 0.06f
        val antWobble2 = sin(ph * 5.5f + 0.8f).toFloat() * r * 0.05f
        drawSegmentedAntenna(c, headX - headR * 0.2f, headY - headR * 0.7f,
            headX - r * 0.7f, headY - r * 1.35f, antWobble, r)
        drawSegmentedAntenna(c, headX + headR * 0.2f, headY - headR * 0.8f,
            headX + r * 0.3f, headY - r * 1.4f, antWobble2, r)

        // Glowing cyan orbs at antenna tips
        val tipLX = headX - r * 0.7f + antWobble * 0.8f
        val tipLY = headY - r * 1.35f
        val tipRX = headX + r * 0.3f + antWobble2 * 0.8f
        val tipRY = headY - r * 1.4f
        val orbR = r * 0.1f

        fill.shader = RadialGradient(tipLX, tipLY, orbR * 3f,
            Color.argb(60, 0, 255, 230), Color.argb(0, 0, 255, 230), Shader.TileMode.CLAMP)
        c.drawCircle(tipLX, tipLY, orbR * 3f, fill)
        fill.shader = null
        fill.shader = RadialGradient(tipLX, tipLY, orbR,
            Color.rgb(120, 255, 240), Color.rgb(0, 210, 200), Shader.TileMode.CLAMP)
        c.drawCircle(tipLX, tipLY, orbR, fill)
        fill.shader = null
        fill.color = Color.argb(200, 220, 255, 255)
        c.drawCircle(tipLX - orbR * 0.25f, tipLY - orbR * 0.25f, orbR * 0.3f, fill)

        fill.shader = RadialGradient(tipRX, tipRY, orbR * 3f,
            Color.argb(60, 0, 255, 230), Color.argb(0, 0, 255, 230), Shader.TileMode.CLAMP)
        c.drawCircle(tipRX, tipRY, orbR * 3f, fill)
        fill.shader = null
        fill.shader = RadialGradient(tipRX, tipRY, orbR,
            Color.rgb(120, 255, 240), Color.rgb(0, 210, 200), Shader.TileMode.CLAMP)
        c.drawCircle(tipRX, tipRY, orbR, fill)
        fill.shader = null
        fill.color = Color.argb(200, 220, 255, 255)
        c.drawCircle(tipRX - orbR * 0.25f, tipRY - orbR * 0.25f, orbR * 0.3f, fill)

        // Golden 4-point stars near antenna tips
        val starPulse = 0.6f + 0.4f * sin(ph * 4f).toFloat()
        val starSz = r * 0.12f * starPulse
        fill.color = Color.rgb(255, 210, 50)
        drawStarShape(c, tipLX + r * 0.12f, tipLY + r * 0.08f, starSz, starSz * 0.35f, 4, ph * 2.5f)
        drawStarShape(c, tipLX - r * 0.08f, tipLY + r * 0.15f, starSz * 0.7f, starSz * 0.25f, 4, ph * 3f)
        drawStarShape(c, tipRX + r * 0.1f, tipRY - r * 0.06f, starSz * 0.8f, starSz * 0.3f, 4, ph * 2f)
        drawStarShape(c, tipRX - r * 0.05f, tipRY + r * 0.12f, starSz * 0.65f, starSz * 0.22f, 4, ph * 3.5f)
    }

    private fun drawLeafWing(
        c: Canvas, ox: Float, oy: Float,
        length: Float, width: Float, angle: Float,
        alpha: Int, ph: Float
    ) {
        c.save()
        c.translate(ox, oy)
        c.rotate(angle)

        fill.shader = LinearGradient(0f, 0f, length, 0f,
            Color.argb(alpha, 80, 255, 230), Color.argb(alpha - 30, 40, 220, 200), Shader.TileMode.CLAMP)
        path.reset()
        path.moveTo(0f, 0f)
        path.cubicTo(length * 0.3f, -width * 0.9f, length * 0.7f, -width * 0.7f, length, 0f)
        path.cubicTo(length * 0.7f, width * 0.7f, length * 0.3f, width * 0.9f, 0f, 0f)
        c.drawPath(path, fill)
        fill.shader = null

        // Inner depth near the rear half of the wing to keep it readable without hard outlining.
        fill.shader = LinearGradient(
            length * 0.25f, 0f, length, 0f,
            Color.argb(0, 0, 0, 0), Color.argb(alpha / 7, 18, 110, 104), Shader.TileMode.CLAMP
        )
        c.drawOval(length * 0.18f, -width * 0.52f, length * 0.96f, width * 0.52f, fill)
        fill.shader = null

        // Wing vein center line
        stroke.color = Color.argb((alpha * 0.58f).toInt(), 168, 255, 245)
        stroke.strokeWidth = 1.8f
        c.drawLine(0f, 0f, length * 0.9f, 0f, stroke)

        // Diagonal veins
        stroke.strokeWidth = 1.0f
        stroke.color = Color.argb((alpha * 0.38f).toInt(), 135, 238, 228)
        for (i in 1..3) {
            val vx = length * i * 0.22f
            c.drawLine(vx, 0f, vx + length * 0.12f, -width * 0.4f, stroke)
            c.drawLine(vx, 0f, vx + length * 0.12f, width * 0.4f, stroke)
        }

        // Highlight shimmer
        val shimmer = 0.5f + 0.5f * sin(ph * 3f + angle * 0.05f).toFloat()
        fill.color = Color.argb((shimmer * 40).toInt(), 255, 255, 255)
        c.drawOval(length * 0.15f, -width * 0.3f, length * 0.55f, width * 0.1f, fill)

        c.restore()
    }

    private fun drawSegmentedAntenna(
        c: Canvas, startX: Float, startY: Float,
        endX: Float, endY: Float, wobble: Float, r: Float
    ) {
        val segments = 7
        val segR = r * 0.028f
        stroke.color = Color.rgb(210, 140, 40)
        stroke.strokeWidth = r * 0.04f
        stroke.strokeCap = Paint.Cap.ROUND

        path.reset()
        path.moveTo(startX, startY)
        val mx = (startX + endX) / 2 + wobble * 1.5f
        val my = (startY + endY) / 2
        path.quadTo(mx, my, endX + wobble * 0.8f, endY)
        c.drawPath(path, stroke)

        for (i in 0..segments) {
            val t = i.toFloat() / segments
            val u = 1f - t
            val px = u * u * startX + 2 * u * t * mx + t * t * (endX + wobble * 0.8f)
            val py = u * u * startY + 2 * u * t * my + t * t * endY
            fill.color = Color.rgb(225, 155, 45)
            c.drawCircle(px, py, segR, fill)
        }
    }

    private fun drawSpiral(c: Canvas, cx: Float, cy: Float, radius: Float, direction: Float) {
        path.reset()
        val steps = 20
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val angle = t * 2.5f * PI.toFloat() * direction
            val sr = radius * t
            val px = cx + sr * cos(angle)
            val py = cy + sr * sin(angle)
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        c.drawPath(path, stroke)
    }

    // ════════════════════════════════════════════════════════════════
    //  2 — FISH (organic tropical fish with flowing fins)
    // ════════════════════════════════════════════════════════════════

    private fun fish(c: Canvas, s: Float, ph: Float) {
        val w = s * 0.42f; val h = s * 0.26f
        drawShapeShadow(c, 0f, h + s * 0.08f, w * 0.8f, h * 0.18f, 60)
        drawGlow(c, 0f, 0f, s * 0.65f, Color.rgb(40, 160, 255), ph)

        val tailSwing = sin(ph * 6f).toFloat() * s * 0.05f
        val bodyFlex = sin(ph * 6f + 0.5f).toFloat() * s * 0.015f

        // Flowing tail fin
        fill.shader = RadialGradient(-w * 1.1f, tailSwing, w * 0.5f,
            Color.rgb(20, 100, 220), Color.rgb(5, 50, 140), Shader.TileMode.CLAMP)
        path.reset()
        path.moveTo(-w * 0.7f, bodyFlex)
        path.cubicTo(-w * 0.9f, -h * 0.4f + bodyFlex, -w * 1.1f, -h * 0.8f + tailSwing, -w * 1.25f, -h * 0.3f + tailSwing)
        path.cubicTo(-w * 1.15f, tailSwing * 0.5f, -w * 1.15f, tailSwing * 0.5f, -w * 0.7f, bodyFlex)
        c.drawPath(path, fill)
        path.reset()
        path.moveTo(-w * 0.7f, bodyFlex)
        path.cubicTo(-w * 0.9f, h * 0.4f + bodyFlex, -w * 1.1f, h * 0.8f + tailSwing, -w * 1.25f, h * 0.3f + tailSwing)
        path.cubicTo(-w * 1.15f, tailSwing * 0.5f, -w * 1.15f, tailSwing * 0.5f, -w * 0.7f, bodyFlex)
        c.drawPath(path, fill)
        fill.shader = null

        // Main body — smooth flowing shape
        val midFlex = sin(ph * 6f + 1f).toFloat() * s * 0.008f
        fill.shader = RadialGradient(w * 0.1f, -h * 0.15f, w * 1.2f,
            intArrayOf(Color.rgb(110, 230, 255), Color.rgb(50, 180, 245), Color.rgb(15, 80, 200), Color.rgb(10, 50, 160)),
            floatArrayOf(0f, 0.3f, 0.7f, 1f), Shader.TileMode.CLAMP)
        path.reset()
        path.moveTo(-w * 0.75f, bodyFlex)
        path.cubicTo(-w * 0.5f, -h * 1.3f + midFlex, w * 0.55f, -h * 1.3f - midFlex, w * 0.8f, -h * 0.1f)
        path.cubicTo(w * 0.85f, 0f, w * 0.85f, 0f, w * 0.8f, h * 0.1f)
        path.cubicTo(w * 0.55f, h * 1.3f + midFlex, -w * 0.5f, h * 1.3f - midFlex, -w * 0.75f, bodyFlex)
        c.drawPath(path, fill)
        fill.shader = null

        // Dorsal fin
        val dorsalWave = sin(ph * 4f).toFloat() * h * 0.1f
        fill.shader = LinearGradient(0f, -h * 1.1f, 0f, -h * 0.3f,
            Color.rgb(60, 200, 255), Color.argb(80, 30, 140, 220), Shader.TileMode.CLAMP)
        path.reset()
        path.moveTo(-w * 0.1f, -h * 0.85f + midFlex)
        path.cubicTo(0f, -h * 1.15f + dorsalWave, w * 0.25f, -h * 1.1f + dorsalWave, w * 0.35f, -h * 0.7f)
        path.lineTo(-w * 0.1f, -h * 0.85f + midFlex)
        c.drawPath(path, fill)
        fill.shader = null

        // Belly highlight
        fill.shader = RadialGradient(0f, h * 0.3f, w * 0.6f,
            Color.argb(60, 200, 240, 255), Color.argb(0, 200, 240, 255), Shader.TileMode.CLAMP)
        c.drawOval(-w * 0.5f, h * 0.1f, w * 0.4f, h * 0.7f, fill)
        fill.shader = null

        // Scales shimmer
        fill.color = Color.argb(22, 180, 240, 255)
        for (i in 0..4) {
            val sx = -w * 0.35f + i * w * 0.18f
            val sy = sin(ph * 2f + i).toFloat() * h * 0.05f
            c.drawOval(sx - w * 0.06f, -h * 0.15f + sy, sx + w * 0.06f, h * 0.15f + sy, fill)
        }

        // Eye — slight forward/back movement
        val headBob = sin(ph * 6f + 1.5f).toFloat() * s * 0.005f
        fill.shader = RadialGradient(w * 0.52f + headBob, -h * 0.18f, s * 0.06f,
            Color.rgb(240, 245, 255), Color.rgb(200, 220, 240), Shader.TileMode.CLAMP)
        c.drawCircle(w * 0.52f + headBob, -h * 0.18f, s * 0.055f, fill)
        fill.shader = null
        fill.shader = RadialGradient(w * 0.54f + headBob, -h * 0.2f, s * 0.03f,
            Color.rgb(8, 8, 50), Color.rgb(20, 20, 80), Shader.TileMode.CLAMP)
        c.drawCircle(w * 0.54f + headBob, -h * 0.2f, s * 0.028f, fill)
        fill.shader = null
        fill.color = Color.argb(200, 255, 255, 255)
        c.drawCircle(w * 0.5f + headBob, -h * 0.24f, s * 0.012f, fill)

        // Pectoral fin
        val pectWave = sin(ph * 5f).toFloat() * 10f
        fill.color = Color.argb(60, 80, 200, 255)
        c.save(); c.rotate(pectWave, w * 0.2f, h * 0.3f)
        path.reset()
        path.moveTo(w * 0.2f, h * 0.3f)
        path.cubicTo(w * 0.35f, h * 0.6f, w * 0.1f, h * 0.8f, w * 0.0f, h * 0.4f)
        path.close()
        c.drawPath(path, fill)
        c.restore()
    }

    // ════════════════════════════════════════════════════════════════
    //  3 — COCKROACH
    // ════════════════════════════════════════════════════════════════

    private fun cockroach(c: Canvas, s: Float, ph: Float) {
        val bw = s * 0.2f; val bh = s * 0.34f
        drawShapeShadow(c, 0f, bh * 0.55f, bw * 1.2f, bh * 0.15f, 70)
        drawGlow(c, 0f, 0f, s * 0.5f, Color.rgb(180, 130, 70), ph)

        val lw = sin(ph * 8f).toFloat() * s * 0.035f
        stroke.color = Color.rgb(85, 55, 30); stroke.strokeWidth = 2.5f; stroke.strokeCap = Paint.Cap.ROUND
        for (i in -1..1) {
            val ly = i * bh * 0.3f
            val d = if (i % 2 == 0) 1 else -1
            val rootInset = bw * 0.32f
            val spreadX = s * (0.16f + (1 - kotlin.math.abs(i)) * 0.02f)
            val spreadY = when (i) {
                -1 -> -bh * 0.24f
                0 -> 0f
                else -> bh * 0.24f
            }
            c.drawLine(-rootInset, ly, -bw - spreadX, ly + spreadY + lw * d, stroke)
            c.drawLine(rootInset, ly, bw + spreadX, ly + spreadY - lw * d, stroke)
        }

        fill.shader = RadialGradient(0f, -bh * 0.1f, bw * 1.5f,
            Color.rgb(155, 105, 58), Color.rgb(78, 48, 22), Shader.TileMode.CLAMP)
        c.drawOval(-bw, -bh * 0.85f, bw, bh * 0.5f, fill); fill.shader = null

        fill.color = Color.argb(40, 220, 190, 140)
        c.drawOval(-bw * 0.3f, -bh * 0.65f, bw * 0.3f, bh * 0.15f, fill)

        fill.color = Color.rgb(52, 32, 16); c.drawCircle(0f, -bh * 0.82f, bw * 0.48f, fill)
        fill.color = Color.rgb(25, 14, 6)
        c.drawCircle(-bw * 0.13f, -bh * 0.87f, s * 0.02f, fill)
        c.drawCircle(bw * 0.13f, -bh * 0.87f, s * 0.02f, fill)

        stroke.color = Color.rgb(95, 65, 35); stroke.strokeWidth = 1.8f
        c.drawLine(-bw * 0.12f, -bh, -bw * 0.38f, -bh * 1.38f, stroke)
        c.drawLine(bw * 0.12f, -bh, bw * 0.38f, -bh * 1.38f, stroke)
    }

    // ════════════════════════════════════════════════════════════════
    //  4 — BUTTERFLY
    // ════════════════════════════════════════════════════════════════

    private fun butterfly(c: Canvas, s: Float, ph: Float) {
        drawShapeShadow(c, 0f, s * 0.28f, s * 0.35f, s * 0.06f, 50)
        drawGlow(c, 0f, 0f, s * 0.65f, Color.rgb(230, 100, 255), ph)

        val flap = sin(ph * 4.5f).toFloat()
        val sx = 0.5f + 0.5f * flap

        c.save(); c.scale(-sx, 1f)
        drawWing(c, s, Color.rgb(235, 75, 255), Color.rgb(255, 140, 220))
        c.restore()
        c.save(); c.scale(sx, 1f)
        drawWing(c, s, Color.rgb(235, 75, 255), Color.rgb(255, 140, 220))
        c.restore()

        fill.color = Color.rgb(48, 24, 65)
        c.drawRoundRect(-s * 0.022f, -s * 0.3f, s * 0.022f, s * 0.28f, s * 0.015f, s * 0.015f, fill)

        stroke.color = Color.rgb(48, 24, 65); stroke.strokeWidth = 2f
        c.drawLine(0f, -s * 0.3f, -s * 0.12f, -s * 0.46f, stroke)
        c.drawLine(0f, -s * 0.3f, s * 0.12f, -s * 0.46f, stroke)
        fill.color = Color.rgb(255, 220, 60)
        c.drawCircle(-s * 0.12f, -s * 0.46f, s * 0.025f, fill)
        c.drawCircle(s * 0.12f, -s * 0.46f, s * 0.025f, fill)
    }

    private fun drawWing(c: Canvas, s: Float, c1: Int, c2: Int) {
        fill.shader = RadialGradient(s * 0.18f, -s * 0.08f, s * 0.35f, c1, c2, Shader.TileMode.CLAMP)
        path.reset()
        path.moveTo(0f, -s * 0.08f)
        path.cubicTo(s * 0.12f, -s * 0.48f, s * 0.48f, -s * 0.42f, s * 0.38f, -s * 0.04f)
        path.cubicTo(s * 0.48f, s * 0.14f, s * 0.12f, s * 0.32f, 0f, s * 0.14f)
        path.close(); c.drawPath(path, fill); fill.shader = null
        fill.color = Color.argb(85, 255, 255, 235)
        c.drawCircle(s * 0.22f, -s * 0.14f, s * 0.06f, fill)
        c.drawCircle(s * 0.18f, s * 0.07f, s * 0.045f, fill)
    }

    // ════════════════════════════════════════════════════════════════
    //  5 — MOUSE (soft furry mouse with organic shapes)
    // ════════════════════════════════════════════════════════════════

    private fun mouse(c: Canvas, s: Float, ph: Float) {
        val scurry = sin(ph * 12f).toFloat() * s * 0.006f
        c.save()
        c.translate(0f, scurry)

        drawShapeShadow(c, 0f, s * 0.14f - scurry, s * 0.3f, s * 0.06f, 65)
        drawGlow(c, 0f, 0f, s * 0.45f, Color.rgb(180, 175, 195), ph)

        // Tail — smooth curved with taper
        val tw = sin(ph * 5f).toFloat() * s * 0.06f
        stroke.strokeCap = Paint.Cap.ROUND
        for (i in 0..4) {
            val t = i / 4f
            stroke.color = Color.argb(180 - (t * 100).toInt(), 215, 190, 190)
            stroke.strokeWidth = (3.5f - t * 2f).coerceAtLeast(0.8f)
            val x0 = -s * 0.22f - t * s * 0.12f
            val y0 = s * 0.04f + tw * t
            val x1 = -s * 0.22f - (t + 0.25f) * s * 0.12f
            val y1 = s * 0.04f - tw * (t + 0.25f) * 0.5f
            c.drawLine(x0, y0, x1, y1, stroke)
        }

        // Body — smooth rounded with fur-like gradient
        fill.shader = RadialGradient(-s * 0.02f, -s * 0.02f, s * 0.28f,
            intArrayOf(Color.rgb(200, 198, 210), Color.rgb(175, 172, 185), Color.rgb(145, 142, 158)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        path.reset()
        path.moveTo(-s * 0.22f, s * 0.02f)
        path.cubicTo(-s * 0.25f, -s * 0.12f, -s * 0.18f, -s * 0.2f, 0f, -s * 0.18f)
        path.cubicTo(s * 0.12f, -s * 0.18f, s * 0.2f, -s * 0.1f, s * 0.2f, 0f)
        path.cubicTo(s * 0.2f, s * 0.1f, s * 0.1f, s * 0.15f, 0f, s * 0.14f)
        path.cubicTo(-s * 0.15f, s * 0.14f, -s * 0.22f, s * 0.1f, -s * 0.22f, s * 0.02f)
        c.drawPath(path, fill); fill.shader = null

        // Belly highlight
        fill.shader = RadialGradient(0f, s * 0.05f, s * 0.12f,
            Color.argb(50, 230, 228, 240), Color.argb(0, 230, 228, 240), Shader.TileMode.CLAMP)
        c.drawOval(-s * 0.1f, 0f, s * 0.1f, s * 0.1f, fill); fill.shader = null

        // Head — smooth teardrop, slight nod when moving
        val headNod = sin(ph * 10f).toFloat() * s * 0.008f
        fill.shader = RadialGradient(s * 0.2f + headNod, -s * 0.05f, s * 0.14f,
            intArrayOf(Color.rgb(195, 193, 208), Color.rgb(170, 168, 183), Color.rgb(148, 145, 162)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        path.reset()
        path.moveTo(s * 0.12f + headNod, -s * 0.06f)
        path.cubicTo(s * 0.12f + headNod, -s * 0.16f, s * 0.22f + headNod, -s * 0.18f, s * 0.28f + headNod, -s * 0.12f)
        path.cubicTo(s * 0.32f + headNod, -s * 0.06f, s * 0.32f + headNod, s * 0.02f, s * 0.28f + headNod, s * 0.06f)
        path.cubicTo(s * 0.22f + headNod, s * 0.1f, s * 0.12f + headNod, s * 0.06f, s * 0.12f + headNod, -s * 0.06f)
        c.drawPath(path, fill); fill.shader = null

        // Ears — rounded with pink inner
        val earTwitch = sin(ph * 6f).toFloat() * 3f
        c.save(); c.rotate(earTwitch, s * 0.16f + headNod, -s * 0.14f)
        fill.shader = RadialGradient(s * 0.14f + headNod, -s * 0.2f, s * 0.06f,
            Color.rgb(190, 188, 200), Color.rgb(165, 162, 178), Shader.TileMode.CLAMP)
        c.drawOval(s * 0.08f + headNod, -s * 0.28f, s * 0.2f + headNod, -s * 0.14f, fill); fill.shader = null
        fill.shader = RadialGradient(s * 0.14f + headNod, -s * 0.2f, s * 0.04f,
            Color.rgb(240, 210, 210), Color.rgb(225, 195, 200), Shader.TileMode.CLAMP)
        c.drawOval(s * 0.1f + headNod, -s * 0.25f, s * 0.18f + headNod, -s * 0.16f, fill); fill.shader = null
        c.restore()

        c.save(); c.rotate(-earTwitch, s * 0.26f + headNod, -s * 0.12f)
        fill.shader = RadialGradient(s * 0.26f + headNod, -s * 0.18f, s * 0.06f,
            Color.rgb(190, 188, 200), Color.rgb(165, 162, 178), Shader.TileMode.CLAMP)
        c.drawOval(s * 0.2f + headNod, -s * 0.26f, s * 0.32f + headNod, -s * 0.12f, fill); fill.shader = null
        fill.shader = RadialGradient(s * 0.26f + headNod, -s * 0.18f, s * 0.04f,
            Color.rgb(240, 210, 210), Color.rgb(225, 195, 200), Shader.TileMode.CLAMP)
        c.drawOval(s * 0.22f + headNod, -s * 0.23f, s * 0.3f + headNod, -s * 0.14f, fill); fill.shader = null
        c.restore()

        // Eye
        fill.shader = RadialGradient(s * 0.24f + headNod, -s * 0.05f, s * 0.025f,
            Color.rgb(15, 12, 30), Color.rgb(8, 6, 18), Shader.TileMode.CLAMP)
        c.drawCircle(s * 0.24f + headNod, -s * 0.05f, s * 0.023f, fill); fill.shader = null
        fill.color = Color.argb(200, 255, 255, 255)
        c.drawCircle(s * 0.235f + headNod, -s * 0.06f, s * 0.008f, fill)

        // Nose — tiny pink
        fill.shader = RadialGradient(s * 0.3f + headNod, -s * 0.01f, s * 0.02f,
            Color.rgb(255, 180, 175), Color.rgb(240, 155, 155), Shader.TileMode.CLAMP)
        c.drawCircle(s * 0.3f + headNod, -s * 0.01f, s * 0.016f, fill); fill.shader = null

        // Whiskers — thin and soft
        stroke.color = Color.argb(120, 150, 148, 162); stroke.strokeWidth = 0.8f
        val ww = sin(ph * 4f).toFloat() * s * 0.01f
        c.drawLine(s * 0.29f + headNod, -s * 0.03f, s * 0.42f + headNod, -s * 0.08f + ww, stroke)
        c.drawLine(s * 0.29f + headNod, -s * 0.005f, s * 0.43f + headNod, -s * 0.005f + ww, stroke)
        c.drawLine(s * 0.29f + headNod, s * 0.02f, s * 0.42f + headNod, s * 0.06f + ww, stroke)

        c.restore()
    }

    // ════════════════════════════════════════════════════════════════
    //  6 — SPIDER
    // ════════════════════════════════════════════════════════════════

    private fun spider(c: Canvas, s: Float, ph: Float) {
        drawShapeShadow(c, 0f, s * 0.2f, s * 0.16f, s * 0.04f, 55)
        drawGlow(c, 0f, 0f, s * 0.45f, Color.rgb(120, 80, 150), ph)

        stroke.color = Color.rgb(55, 55, 60); stroke.strokeWidth = 1f
        c.drawLine(0f, -s * 0.55f, 0f, -s * 0.14f, stroke)

        stroke.color = Color.rgb(32, 30, 38); stroke.strokeWidth = 2.8f; stroke.strokeCap = Paint.Cap.ROUND
        val lw = sin(ph * 6f).toFloat() * s * 0.025f
        for (i in 0..3) {
            val ba = -65f + i * 20f; val rad = Math.toRadians(ba.toDouble())
            val m = s * 0.22f; val t = s * 0.4f; val w = lw * (if (i % 2 == 0) 1 else -1)
            val my = -s * 0.05f + i * s * 0.038f
            c.drawLine(0f, my, (-m * cos(rad) + w).toFloat(), (-m * sin(rad)).toFloat(), stroke)
            c.drawLine((-m * cos(rad) + w).toFloat(), (-m * sin(rad)).toFloat(),
                (-t * cos(rad) + w * 1.3f).toFloat(), (-t * sin(rad) + s * 0.14f).toFloat(), stroke)
            c.drawLine(0f, my, (m * cos(rad) - w).toFloat(), (-m * sin(rad)).toFloat(), stroke)
            c.drawLine((m * cos(rad) - w).toFloat(), (-m * sin(rad)).toFloat(),
                (t * cos(rad) - w * 1.3f).toFloat(), (-t * sin(rad) + s * 0.14f).toFloat(), stroke)
        }

        fill.shader = RadialGradient(0f, 0f, s * 0.16f,
            Color.rgb(48, 42, 58), Color.rgb(20, 18, 25), Shader.TileMode.CLAMP)
        c.drawCircle(0f, 0f, s * 0.15f, fill); fill.shader = null
        fill.color = Color.rgb(32, 30, 38); c.drawOval(-s * 0.09f, -s * 0.24f, s * 0.09f, -s * 0.09f, fill)
        fill.color = Color.rgb(220, 28, 28); c.drawCircle(0f, s * 0.04f, s * 0.045f, fill)
    }

    // ════════════════════════════════════════════════════════════════
    //  7 — BEE
    // ════════════════════════════════════════════════════════════════

    private fun bee(c: Canvas, s: Float, ph: Float) {
        drawShapeShadow(c, 0f, s * 0.25f, s * 0.18f, s * 0.05f, 55)
        drawGlow(c, 0f, 0f, s * 0.5f, Color.rgb(255, 235, 60), ph)

        val wa = sin(ph * 15f).toFloat() * 22f
        fill.color = Color.argb(90, 220, 240, 255)
        c.save(); c.rotate(wa, -s * 0.08f, s * 0.02f)
        c.drawOval(-s * 0.28f, -s * 0.08f, 0f, s * 0.28f, fill); c.restore()
        c.save(); c.rotate(-wa, s * 0.08f, s * 0.02f)
        c.drawOval(0f, -s * 0.08f, s * 0.28f, s * 0.28f, fill); c.restore()

        fill.shader = RadialGradient(0f, 0f, s * 0.2f,
            Color.rgb(255, 225, 45), Color.rgb(230, 180, 0), Shader.TileMode.CLAMP)
        c.drawOval(-s * 0.15f, -s * 0.24f, s * 0.15f, s * 0.24f, fill); fill.shader = null
        fill.color = Color.rgb(30, 24, 18)
        for (j in 0..2) { val y = -s * 0.09f + j * s * 0.1f; c.drawRect(-s * 0.16f, y, s * 0.16f, y + s * 0.04f, fill) }

        fill.color = Color.rgb(32, 26, 22); c.drawCircle(0f, -s * 0.28f, s * 0.075f, fill)
        fill.color = Color.WHITE
        c.drawCircle(-s * 0.028f, -s * 0.3f, s * 0.02f, fill)
        c.drawCircle(s * 0.028f, -s * 0.3f, s * 0.02f, fill)
        stroke.color = Color.rgb(30, 25, 20); stroke.strokeWidth = 1.5f
        c.drawLine(-s * 0.018f, -s * 0.35f, -s * 0.075f, -s * 0.44f, stroke)
        c.drawLine(s * 0.018f, -s * 0.35f, s * 0.075f, -s * 0.44f, stroke)
    }

    // ════════════════════════════════════════════════════════════════
    //  8 — FEATHER
    // ════════════════════════════════════════════════════════════════

    private fun feather(c: Canvas, s: Float, ph: Float) {
        c.save(); c.rotate(sin(ph * 1.8f).toFloat() * 22f)
        drawShapeShadow(c, 0f, s * 0.35f, s * 0.12f, s * 0.04f, 40)
        drawGlow(c, 0f, 0f, s * 0.4f, Color.rgb(235, 235, 250), ph)

        fill.shader = LinearGradient(-s * 0.08f, -s * 0.42f, s * 0.08f, s * 0.42f,
            Color.rgb(250, 250, 255), Color.rgb(218, 218, 238), Shader.TileMode.CLAMP)
        path.reset(); path.moveTo(0f, -s * 0.44f)
        path.cubicTo(s * 0.22f, -s * 0.2f, s * 0.18f, s * 0.14f, 0f, s * 0.44f)
        path.cubicTo(-s * 0.18f, s * 0.14f, -s * 0.22f, -s * 0.2f, 0f, -s * 0.44f)
        c.drawPath(path, fill); fill.shader = null

        stroke.color = Color.rgb(198, 198, 218); stroke.strokeWidth = 1.5f
        c.drawLine(0f, -s * 0.4f, 0f, s * 0.4f, stroke)
        stroke.strokeWidth = 0.7f
        for (i in 1..7) {
            val y = -s * 0.34f + i * s * 0.085f; val xo = s * 0.11f * (1f - i / 8f)
            c.drawLine(0f, y, -xo, y - s * 0.04f, stroke)
            c.drawLine(0f, y, xo, y - s * 0.04f, stroke)
        }
        c.restore()
    }

    // ════════════════════════════════════════════════════════════════
    //  9 — BIRD (soft round hummingbird with flowing wings)
    // ════════════════════════════════════════════════════════════════

    private fun bird(c: Canvas, s: Float, ph: Float) {
        drawShapeShadow(c, 0f, s * 0.22f, s * 0.25f, s * 0.06f, 55)
        drawGlow(c, 0f, 0f, s * 0.55f, Color.rgb(50, 220, 190), ph)

        val flap = sin(ph * 7f).toFloat()
        val leftWingAngle = -12f + flap * 16f
        val rightWingAngle = 12f - flap * 16f

        // Left wing — organic feathered shape
        c.save(); c.rotate(leftWingAngle, -s * 0.04f, -s * 0.02f)
        fill.shader = LinearGradient(-s * 0.45f, -s * 0.35f, 0f, 0f,
            Color.rgb(30, 200, 180), Color.argb(160, 20, 170, 155), Shader.TileMode.CLAMP)
        path.reset(); path.moveTo(-s * 0.04f, -s * 0.02f)
        path.cubicTo(-s * 0.15f, -s * 0.32f, -s * 0.42f, -s * 0.38f, -s * 0.48f, -s * 0.15f)
        path.cubicTo(-s * 0.45f, -s * 0.02f, -s * 0.25f, s * 0.06f, -s * 0.04f, -s * 0.02f)
        c.drawPath(path, fill); fill.shader = null
        c.restore()

        // Right wing
        c.save(); c.rotate(rightWingAngle, s * 0.04f, -s * 0.02f)
        fill.shader = LinearGradient(s * 0.45f, -s * 0.35f, 0f, 0f,
            Color.rgb(30, 200, 180), Color.argb(160, 20, 170, 155), Shader.TileMode.CLAMP)
        path.reset(); path.moveTo(s * 0.04f, -s * 0.02f)
        path.cubicTo(s * 0.15f, -s * 0.32f, s * 0.42f, -s * 0.38f, s * 0.48f, -s * 0.15f)
        path.cubicTo(s * 0.45f, -s * 0.02f, s * 0.25f, s * 0.06f, s * 0.04f, -s * 0.02f)
        c.drawPath(path, fill); fill.shader = null
        c.restore()

        // Tail feathers
        fill.shader = RadialGradient(-s * 0.3f, s * 0.08f, s * 0.2f,
            Color.rgb(0, 160, 140), Color.rgb(0, 100, 90), Shader.TileMode.CLAMP)
        path.reset(); path.moveTo(-s * 0.15f, s * 0.06f)
        path.cubicTo(-s * 0.28f, s * 0.02f, -s * 0.38f, s * 0.12f, -s * 0.35f, s * 0.06f)
        path.cubicTo(-s * 0.32f, s * 0.0f, -s * 0.25f, s * 0.08f, -s * 0.15f, s * 0.06f)
        c.drawPath(path, fill); fill.shader = null

        // Body — plump soft oval with breathing motion
        val breathe = sin(ph * 3f).toFloat() * s * 0.008f
        fill.shader = RadialGradient(s * 0.02f, -s * 0.03f, s * 0.22f,
            intArrayOf(Color.rgb(80, 250, 225), Color.rgb(35, 210, 190), Color.rgb(10, 155, 135)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        c.drawOval(-s * 0.18f, -s * 0.15f - breathe, s * 0.16f, s * 0.16f + breathe, fill)
        fill.shader = null

        // Breast highlight
        fill.shader = RadialGradient(-s * 0.02f, s * 0.02f, s * 0.12f,
            Color.argb(80, 200, 255, 240), Color.argb(0, 200, 255, 240), Shader.TileMode.CLAMP)
        c.drawOval(-s * 0.1f, -s * 0.02f, s * 0.08f, s * 0.12f, fill)
        fill.shader = null

        // Head — bobs forward/back with movement
        val headBob = sin(ph * 7f).toFloat() * s * 0.012f
        val headDip = sin(ph * 3.5f).toFloat() * s * 0.006f
        val hx = s * 0.13f + headBob
        val hy = -s * 0.06f + headDip
        fill.shader = RadialGradient(hx, hy, s * 0.12f,
            Color.rgb(70, 245, 220), Color.rgb(20, 185, 165), Shader.TileMode.CLAMP)
        c.drawCircle(hx, hy, s * 0.1f, fill)
        fill.shader = null

        // Eye
        fill.shader = RadialGradient(hx + s * 0.03f, hy - s * 0.03f, s * 0.04f,
            Color.WHITE, Color.rgb(220, 230, 235), Shader.TileMode.CLAMP)
        c.drawCircle(hx + s * 0.03f, hy - s * 0.03f, s * 0.035f, fill); fill.shader = null
        fill.shader = RadialGradient(hx + s * 0.04f, hy - s * 0.03f, s * 0.02f,
            Color.rgb(10, 10, 35), Color.rgb(5, 5, 20), Shader.TileMode.CLAMP)
        c.drawCircle(hx + s * 0.04f, hy - s * 0.03f, s * 0.018f, fill); fill.shader = null
        fill.color = Color.argb(210, 255, 255, 255)
        c.drawCircle(hx + s * 0.025f, hy - s * 0.04f, s * 0.008f, fill)

        // Beak — follows head position
        fill.shader = LinearGradient(hx + s * 0.09f, hy, hx + s * 0.21f, hy + s * 0.01f,
            Color.rgb(255, 190, 40), Color.rgb(240, 140, 10), Shader.TileMode.CLAMP)
        path.reset()
        path.moveTo(hx + s * 0.09f, hy - s * 0.005f)
        path.cubicTo(hx + s * 0.15f, hy - s * 0.025f, hx + s * 0.2f, hy, hx + s * 0.21f, hy + s * 0.015f)
        path.cubicTo(hx + s * 0.2f, hy + s * 0.03f, hx + s * 0.15f, hy + s * 0.05f, hx + s * 0.09f, hy + s * 0.035f)
        path.close()
        c.drawPath(path, fill); fill.shader = null
    }

    // ════════════════════════════════════════════════════════════════
    //  10 — YARN BALL
    // ════════════════════════════════════════════════════════════════

    private fun yarnBall(c: Canvas, s: Float, ph: Float) {
        val r = s * 0.27f
        drawShapeShadow(c, 0f, r + s * 0.04f, r * 0.9f, r * 0.15f, 65)
        drawGlow(c, 0f, 0f, r * 1.25f, Color.rgb(235, 110, 20), ph)

        c.save(); c.rotate(ph * 38f)
        fill.shader = RadialGradient(-r * 0.3f, -r * 0.3f, r * 1.6f,
            intArrayOf(Color.rgb(250, 138, 38), Color.rgb(214, 92, 8), Color.rgb(155, 48, 0)),
            floatArrayOf(0f, 0.58f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(0f, 0f, r, fill); fill.shader = null

        fill.shader = RadialGradient(r * 0.18f, r * 0.22f, r * 0.92f,
            Color.argb(58, 120, 30, 0), Color.argb(14, 70, 12, 0), Shader.TileMode.CLAMP)
        c.drawCircle(0f, 0f, r, fill); fill.shader = null

        stroke.color = Color.rgb(198, 88, 10); stroke.strokeWidth = 3.1f
        for (i in 0..8) {
            val a1 = (i * 51f + 10f) * PI.toFloat() / 180f; val a2 = a1 + PI.toFloat() * 0.55f
            path.reset(); path.moveTo(r * 0.6f * cos(a1), r * 0.6f * sin(a1))
            path.cubicTo(r * 0.2f * cos(a1 + 0.4f), r * 0.2f * sin(a1 + 0.4f),
                -r * 0.2f * cos(a2 - 0.4f), -r * 0.2f * sin(a2 - 0.4f),
                r * 0.6f * cos(a2), r * 0.6f * sin(a2))
            c.drawPath(path, stroke)
        }
        stroke.color = Color.argb(165, 255, 170, 72); stroke.strokeWidth = 1.6f
        for (i in 0..5) {
            val y = -r * 0.48f + i * r * 0.18f
            path.reset()
            path.moveTo(-r * 0.72f, y)
            path.cubicTo(-r * 0.25f, y - r * 0.16f, r * 0.22f, y + r * 0.16f, r * 0.74f, y - r * 0.02f)
            c.drawPath(path, stroke)
        }
        fill.shader = RadialGradient(-r * 0.22f, -r * 0.24f, r * 0.28f,
            Color.argb(76, 255, 230, 185), Color.argb(0, 255, 230, 185), Shader.TileMode.CLAMP)
        c.drawCircle(-r * 0.2f, -r * 0.22f, r * 0.26f, fill); fill.shader = null
        c.restore()

        stroke.color = Color.rgb(214, 98, 12); stroke.strokeWidth = 3f
        val tw = sin(ph * 3f).toFloat() * s * 0.045f
        path.reset(); path.moveTo(r * 0.7f, r * 0.25f)
        path.cubicTo(r * 1.15f, tw, r * 1.45f, r * 0.38f + tw, r * 1.6f, r * 0.22f)
        c.drawPath(path, stroke)
    }

    // ════════════════════════════════════════════════════════════════
    //  SHARED LAYER UTILITIES
    // ════════════════════════════════════════════════════════════════

    private fun drawShapeShadow(c: Canvas, cx: Float, cy: Float, rx: Float, ry: Float, alpha: Int) {
        fill.shader = RadialGradient(cx, cy, rx,
            intArrayOf(Color.argb(alpha, 0, 0, 0), Color.argb(alpha / 2, 0, 0, 0), Color.argb(0, 0, 0, 0)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        c.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, fill)
        fill.shader = null
    }

    private fun drawGlow(c: Canvas, cx: Float, cy: Float, radius: Float, color: Int, phase: Float) {
        val pulse = 1f + 0.15f * sin(phase * 2.8f).toFloat()
        val r = radius * pulse
        val rd = Color.red(color); val g = Color.green(color); val b = Color.blue(color)
        // Outer soft glow
        fill.shader = RadialGradient(cx, cy, r * 1.3f,
            intArrayOf(Color.argb(60, rd, g, b), Color.argb(20, rd, g, b), Color.argb(0, rd, g, b)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r * 1.3f, fill)
        fill.shader = null
        // Inner bright glow
        fill.shader = RadialGradient(cx, cy, r,
            intArrayOf(Color.argb(180, rd, g, b), Color.argb(70, rd, g, b), Color.argb(0, rd, g, b)),
            floatArrayOf(0f, 0.3f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, fill)
        fill.shader = null
    }

    private fun drawStarShape(c: Canvas, cx: Float, cy: Float, outer: Float, inner: Float, points: Int, rotation: Float) {
        path.reset()
        val step = PI.toFloat() / points
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) outer else inner
            val angle = i * step - PI.toFloat() / 2 + rotation
            val x = cx + r * cos(angle)
            val y = cy + r * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        c.drawPath(path, fill)
    }
}
