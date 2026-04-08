package com.yourcompany.pawplay.game

import android.graphics.PointF
import android.graphics.RectF
import com.yourcompany.pawplay.model.BehaviorType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object PreyBehavior {

    fun generatePath(
        behaviorType: BehaviorType,
        start: PointF,
        bounds: RectF,
        steps: Int = 400
    ): List<PointF> {
        if (bounds.width() <= 0f || bounds.height() <= 0f) {
            return listOf(PointF(start.x, start.y))
        }
        return when (behaviorType) {
            BehaviorType.RANDOM_CURVE -> randomCurvePath(start, bounds, steps)
            BehaviorType.SWIMMING -> swimmingPath(start, bounds, steps)
            BehaviorType.CRAWLING -> crawlingPath(start, bounds, steps)
            BehaviorType.FLYING -> flyingPath(start, bounds, steps)
            BehaviorType.RUNNING -> runningPath(start, bounds, steps)
            BehaviorType.DANGLING -> danglingPath(start, bounds, steps)
            BehaviorType.DRIFTING -> driftingPath(start, bounds, steps)
            BehaviorType.BOUNCING -> bouncingPath(start, bounds, steps)
        }
    }

    /**
     * @brief Pick a waypoint: ~30% chance off-screen (edge or extended), ~70% on screen
     */
    private fun randomWaypoint(current: PointF, bounds: RectF, minDist: Float): PointF {
        return when {
            Math.random() < 0.15 -> BoundaryPhysics.randomEdgePoint(bounds)
            Math.random() < 0.25 -> BoundaryPhysics.randomPointInExtendedBounds(bounds)
            else -> BoundaryPhysics.randomPointAway(current, bounds, minDist)
        }
    }

    private fun randomCurvePath(start: PointF, bounds: RectF, steps: Int): List<PointF> {
        val points = mutableListOf<PointF>()
        var current = PointF(start.x, start.y)
        val segments = 3 + (Math.random() * 2).toInt()
        val stepsPerSeg = steps / segments

        repeat(segments) {
            val target = randomWaypoint(current, bounds, bounds.width() * 0.35f)
            val ctrl1 = BoundaryPhysics.randomPointInExtendedBounds(bounds)
            val ctrl2 = BoundaryPhysics.randomPointInBounds(bounds)
            points.addAll(cubicBezierSample(current, ctrl1, ctrl2, target, stepsPerSeg))
            current = PointF(target.x, target.y)
        }
        return points
    }

    private fun swimmingPath(start: PointF, bounds: RectF, steps: Int): List<PointF> {
        val points = mutableListOf<PointF>()
        var current = PointF(start.x, start.y)
        val segments = 2 + (Math.random() * 2).toInt()
        val stepsPerSeg = steps / segments

        repeat(segments) {
            val target = randomWaypoint(current, bounds, bounds.width() * 0.3f)
            val midX = (current.x + target.x) / 2
            val midY = (current.y + target.y) / 2
            val offset = (bounds.height() * 0.2f * (Math.random() - 0.5)).toFloat()
            val ctrl1 = PointF(midX - offset, midY + offset)
            val ctrl2 = PointF(midX + offset, midY - offset)
            points.addAll(cubicBezierSample(current, ctrl1, ctrl2, target, stepsPerSeg))
            current = PointF(target.x, target.y)
        }
        return points
    }

    private fun crawlingPath(start: PointF, bounds: RectF, steps: Int): List<PointF> {
        val points = mutableListOf<PointF>()
        var current = PointF(start.x, start.y)
        val segments = 5 + (Math.random() * 4).toInt()
        val stepsPerSeg = steps / segments

        repeat(segments) { i ->
            val next = if (i == segments / 2 && Math.random() < 0.4) {
                BoundaryPhysics.randomEdgePoint(bounds)
            } else {
                randomWaypoint(current, bounds, bounds.width() * 0.15f)
            }
            val ctrl = PointF(
                (current.x + next.x) / 2 + ((Math.random() - 0.5) * 100).toFloat(),
                (current.y + next.y) / 2 + ((Math.random() - 0.5) * 100).toFloat()
            )
            points.addAll(quadBezierSample(current, ctrl, next, stepsPerSeg))
            current = PointF(next.x, next.y)
        }
        return points
    }

    private fun flyingPath(start: PointF, bounds: RectF, steps: Int): List<PointF> {
        val points = mutableListOf<PointF>()
        var current = PointF(start.x, start.y)
        val segments = 3 + (Math.random() * 2).toInt()
        val stepsPerSeg = steps / segments

        repeat(segments) {
            val target = randomWaypoint(current, bounds, bounds.width() * 0.35f)
            for (i in 0..stepsPerSeg) {
                val t = i.toFloat() / stepsPerSeg
                val baseX = current.x + (target.x - current.x) * t
                val baseY = current.y + (target.y - current.y) * t
                val wave = sin(t * 3 * PI).toFloat() * 40f
                points.add(PointF(baseX + wave * 0.3f, baseY + wave))
            }
            current = PointF(target.x, target.y)
        }
        return points
    }

    private fun runningPath(start: PointF, bounds: RectF, steps: Int): List<PointF> {
        val points = mutableListOf<PointF>()
        var current = PointF(start.x, start.y)
        val sprints = 3 + (Math.random() * 3).toInt()
        val stepsPerSprint = steps / sprints

        repeat(sprints) {
            val target = randomWaypoint(current, bounds, bounds.width() * 0.3f)
            val ctrl = BoundaryPhysics.randomPointInBounds(bounds)
            points.addAll(quadBezierSample(current, ctrl, target, stepsPerSprint))
            current = PointF(target.x, target.y)
        }
        return points
    }

    private fun danglingPath(@Suppress("UNUSED_PARAMETER") start: PointF, bounds: RectF, steps: Int): List<PointF> {
        val points = mutableListOf<PointF>()
        val topY = bounds.top - 60f
        val bottomY = bounds.top + bounds.height() * 0.65f

        val anchorX = (bounds.left + bounds.width() * 0.1f +
            Math.random().toFloat() * bounds.width() * 0.8f)
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val y = topY + (bottomY - topY) * t
            val swing = sin(t * 5 * PI).toFloat() * 50f
            points.add(PointF(anchorX + swing, y))
        }
        val pauseSteps = 30
        repeat(pauseSteps) { points.add(PointF(anchorX, bottomY)) }
        val halfSteps = (steps / 2).coerceAtLeast(1)
        for (i in 0..halfSteps) {
            val t = i.toFloat() / halfSteps
            val y = bottomY - (bottomY - topY) * t
            val swing = sin(t * 3 * PI).toFloat() * 30f
            points.add(PointF(anchorX + swing, y))
        }
        return points
    }

    private fun driftingPath(start: PointF, bounds: RectF, steps: Int): List<PointF> {
        val points = mutableListOf<PointF>()
        val endY = bounds.bottom + 60f
        val drift = ((Math.random() - 0.5) * bounds.width() * 0.6).toFloat()

        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = start.x + drift * t + sin(t * 4 * PI).toFloat() * 55f
            val y = start.y + (endY - start.y) * t
            points.add(PointF(x, y))
        }
        return points
    }

    private fun bouncingPath(start: PointF, bounds: RectF, steps: Int): List<PointF> {
        val points = mutableListOf<PointF>()
        val ext = BoundaryPhysics.extendedBounds(bounds)
        var x = start.x; var y = start.y
        var vx = ((Math.random() - 0.5) * 6).toFloat()
        var vy = 1.5f
        val gravity = 0.1f
        val bounceFactor = -0.78f
        val radius = 40f

        for (i in 0..steps) {
            vy += gravity; x += vx; y += vy
            if (y + radius > ext.bottom) { y = ext.bottom - radius; vy *= bounceFactor }
            if (y - radius < ext.top) { y = ext.top + radius; vy *= bounceFactor }
            if (x - radius < ext.left) { x = ext.left + radius; vx = -vx }
            if (x + radius > ext.right) { x = ext.right - radius; vx = -vx }
            points.add(PointF(x, y))
        }
        return points
    }

    fun cubicBezierSample(
        p0: PointF, p1: PointF, p2: PointF, p3: PointF, steps: Int
    ): List<PointF> {
        val n = steps.coerceAtLeast(1)
        return (0..n).map { i ->
            val t = i.toFloat() / n; val u = 1 - t
            PointF(
                u * u * u * p0.x + 3 * u * u * t * p1.x + 3 * u * t * t * p2.x + t * t * t * p3.x,
                u * u * u * p0.y + 3 * u * u * t * p1.y + 3 * u * t * t * p2.y + t * t * t * p3.y
            )
        }
    }

    private fun quadBezierSample(
        p0: PointF, p1: PointF, p2: PointF, steps: Int
    ): List<PointF> {
        val n = steps.coerceAtLeast(1)
        return (0..n).map { i ->
            val t = i.toFloat() / n; val u = 1 - t
            PointF(
                u * u * p0.x + 2 * u * t * p1.x + t * t * p2.x,
                u * u * p0.y + 2 * u * t * p1.y + t * t * p2.y
            )
        }
    }
}
