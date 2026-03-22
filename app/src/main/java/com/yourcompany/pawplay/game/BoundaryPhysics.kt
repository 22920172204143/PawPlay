package com.yourcompany.pawplay.game

import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.abs

object BoundaryPhysics {

    fun clamp(position: PointF, radius: Float, bounds: RectF) {
        if (bounds.width() <= 0f || bounds.height() <= 0f) return
        val minX = bounds.left + radius
        val maxX = bounds.right - radius
        val minY = bounds.top + radius
        val maxY = bounds.bottom - radius
        if (minX < maxX) position.x = position.x.coerceIn(minX, maxX)
        if (minY < maxY) position.y = position.y.coerceIn(minY, maxY)
    }

    fun reflectVelocity(
        position: PointF,
        velocity: PointF,
        radius: Float,
        bounds: RectF
    ) {
        if (position.x - radius <= bounds.left || position.x + radius >= bounds.right) {
            velocity.x = -velocity.x
        }
        if (position.y - radius <= bounds.top || position.y + radius >= bounds.bottom) {
            velocity.y = -velocity.y
        }
        clamp(position, radius, bounds)
    }

    fun isNearBoundary(position: PointF, radius: Float, bounds: RectF, threshold: Float): Boolean {
        return position.x - radius < bounds.left + threshold ||
                position.x + radius > bounds.right - threshold ||
                position.y - radius < bounds.top + threshold ||
                position.y + radius > bounds.bottom - threshold
    }

    fun randomPointInBounds(bounds: RectF, margin: Float = 50f): PointF {
        if (bounds.width() <= 0f || bounds.height() <= 0f) return PointF(0f, 0f)
        val safeMargin = margin.coerceAtMost(bounds.width() / 3f).coerceAtMost(bounds.height() / 3f)
        val x = (bounds.left + safeMargin) + Math.random().toFloat() * (bounds.width() - 2 * safeMargin).coerceAtLeast(1f)
        val y = (bounds.top + safeMargin) + Math.random().toFloat() * (bounds.height() - 2 * safeMargin).coerceAtLeast(1f)
        return PointF(x, y)
    }

    fun randomPointAway(current: PointF, bounds: RectF, minDist: Float, margin: Float = 50f): PointF {
        repeat(20) {
            val p = randomPointInBounds(bounds, margin)
            val dx = p.x - current.x
            val dy = p.y - current.y
            if (abs(dx) + abs(dy) > minDist) return p
        }
        return randomPointInBounds(bounds, margin)
    }
}
