package com.pawhunt.app.game

import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.abs

object BoundaryPhysics {

    private const val OFF_SCREEN_MARGIN = 200f

    /**
     * @brief Returns bounds expanded outward, allowing prey to roam partially off-screen
     * @param[in] bounds visible screen bounds
     * @return expanded RectF
     */
    fun extendedBounds(bounds: RectF): RectF {
        return RectF(
            bounds.left - OFF_SCREEN_MARGIN,
            bounds.top - OFF_SCREEN_MARGIN,
            bounds.right + OFF_SCREEN_MARGIN,
            bounds.bottom + OFF_SCREEN_MARGIN
        )
    }

    /**
     * @brief Soft clamp — only pulls back when prey exceeds the extended (off-screen) zone
     * @param[in] position prey position to clamp
     * @param[in] radius prey collision radius
     * @param[in] bounds visible screen bounds
     */
    fun softClamp(position: PointF, radius: Float, bounds: RectF) {
        val ext = extendedBounds(bounds)
        if (ext.width() <= 0f || ext.height() <= 0f) return
        val minX = ext.left + radius
        val maxX = ext.right - radius
        val minY = ext.top + radius
        val maxY = ext.bottom - radius
        if (minX < maxX) position.x = position.x.coerceIn(minX, maxX)
        if (minY < maxY) position.y = position.y.coerceIn(minY, maxY)
    }

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

    /**
     * @brief Generate a random point that may be partially off-screen (within extended bounds)
     * @param[in] bounds visible screen bounds
     * @return random PointF, possibly outside visible area
     */
    fun randomPointInExtendedBounds(bounds: RectF): PointF {
        val ext = extendedBounds(bounds)
        val x = ext.left + Math.random().toFloat() * ext.width()
        val y = ext.top + Math.random().toFloat() * ext.height()
        return PointF(x, y)
    }

    /**
     * @brief Pick a random point along the screen edges (or just outside)
     * @param[in] bounds visible screen bounds
     * @return PointF on or near one of the 4 edges
     */
    fun randomEdgePoint(bounds: RectF): PointF {
        val overshoot = 80f
        return when ((Math.random() * 4).toInt()) {
            0 -> PointF(
                bounds.left + Math.random().toFloat() * bounds.width(),
                bounds.top - overshoot
            )
            1 -> PointF(
                bounds.left + Math.random().toFloat() * bounds.width(),
                bounds.bottom + overshoot
            )
            2 -> PointF(
                bounds.left - overshoot,
                bounds.top + Math.random().toFloat() * bounds.height()
            )
            else -> PointF(
                bounds.right + overshoot,
                bounds.top + Math.random().toFloat() * bounds.height()
            )
        }
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
