package com.yourcompany.pawplay.game

import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.RectF
import com.yourcompany.pawplay.model.Toy
import kotlin.math.hypot

class PreyEntity(
    val toy: Toy,
    private val bounds: RectF,
    var speedMultiplier: Float = 1.0f
) {

    var position = PointF(bounds.centerX(), bounds.centerY())
    var isHit = false

    private var path: List<PointF> = emptyList()
    private var pathProgress = 0f
    private var pauseTimer = 0f
    private var isPaused = false
    private var animPhase = 0f

    private val effectiveSpeed get() = toy.baseSpeed * speedMultiplier
    private val radius get() = toy.displaySize / 2f

    init {
        buildNewPath()
    }

    fun buildNewPath() {
        path = PreyBehavior.generatePath(toy.behaviorType, position, bounds)
        pathProgress = 0f
        schedulePause()
    }

    private fun schedulePause() {
        isPaused = false
        pauseTimer = 0f
    }

    fun update(deltaSeconds: Float) {
        if (isHit) return
        animPhase += deltaSeconds

        if (isPaused) {
            pauseTimer -= deltaSeconds
            if (pauseTimer <= 0f) {
                isPaused = false
            }
            return
        }

        if (path.isEmpty() || pathProgress >= path.size - 1) {
            buildNewPath()
            return
        }

        val advance = effectiveSpeed * deltaSeconds * 6f
        pathProgress += advance

        val idx = pathProgress.toInt().coerceIn(0, path.size - 1)
        val nextIdx = (idx + 1).coerceIn(0, path.size - 1)
        val frac = pathProgress - idx
        val p1 = path[idx]
        val p2 = path[nextIdx]

        position.x = p1.x + (p2.x - p1.x) * frac
        position.y = p1.y + (p2.y - p1.y) * frac

        BoundaryPhysics.clamp(position, radius, bounds)

        if (Math.random() < 0.002 && !isPaused) {
            isPaused = true
            pauseTimer = 0.2f + Math.random().toFloat() * 0.6f
        }
    }

    fun draw(canvas: Canvas) {
        if (isHit) return
        PreyDrawer.drawPrey(canvas, toy.id, position.x, position.y, toy.displaySize, animPhase)
    }

    fun checkHit(touchX: Float, touchY: Float): Boolean {
        if (isHit) return false
        val hitRadius = toy.displaySize * 0.65f
        return hypot(touchX - position.x, touchY - position.y) <= hitRadius
    }

    fun respawn() {
        position = BoundaryPhysics.randomPointAway(position, bounds, 150f)
        isHit = false
        buildNewPath()
    }
}
