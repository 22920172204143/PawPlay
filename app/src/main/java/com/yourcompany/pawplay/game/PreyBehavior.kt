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
        steps: Int = 300
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

    private fun randomCurvePath(start: PointF, bounds: RectF, steps: Int): List<PointF> {
        val target = BoundaryPhysics.randomPointAway(start, bounds, bounds.width() * 0.3f)
        val ctrl1 = BoundaryPhysics.randomPointInBounds(bounds)
        val ctrl2 = BoundaryPhysics.randomPointInBounds(bounds)
        return cubicBezierSample(start, ctrl1, ctrl2, target, steps)
    }

    private fun swimmingPath(start: PointF, bounds: RectF, steps: Int): List<PointF> {
        val target = BoundaryPhysics.randomPointAway(start, bounds, bounds.width() * 0.25f)
        val midX = (start.x + target.x) / 2
        val midY = (start.y + target.y) / 2
        val offset = (bounds.height() * 0.15f * (Math.random() - 0.5)).toFloat()
        val ctrl1 = PointF(midX - offset, midY + offset)
        val ctrl2 = PointF(midX + offset, midY - offset)
        return cubicBezierSample(start, ctrl1, ctrl2, target, steps)
    }

    private fun crawlingPath(start: PointF, bounds: RectF, steps: Int): List<PointF> {
        val points = mutableListOf<PointF>()
        var current = PointF(start.x, start.y)
        val segments = 4 + (Math.random() * 3).toInt()
        val stepsPerSegment = steps / segments

        repeat(segments) {
            val next = BoundaryPhysics.randomPointAway(current, bounds, 80f)
            for (i in 0..stepsPerSegment) {
                val t = i.toFloat() / stepsPerSegment
                points.add(PointF(
                    current.x + (next.x - current.x) * t,
                    current.y + (next.y - current.y) * t
                ))
            }
            current = PointF(next.x, next.y)
        }
        return points
    }

    private fun flyingPath(start: PointF, bounds: RectF, steps: Int): List<PointF> {
        val target = BoundaryPhysics.randomPointAway(start, bounds, bounds.width() * 0.3f)
        val points = mutableListOf<PointF>()
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val baseX = start.x + (target.x - start.x) * t
            val baseY = start.y + (target.y - start.y) * t
            val wave = sin(t * 4 * PI).toFloat() * 30f
            points.add(PointF(baseX, baseY + wave))
        }
        return points
    }

    private fun runningPath(start: PointF, bounds: RectF, steps: Int): List<PointF> {
        val points = mutableListOf<PointF>()
        var current = PointF(start.x, start.y)
        val sprints = 3 + (Math.random() * 2).toInt()
        val stepsPerSprint = steps / sprints

        repeat(sprints) {
            val target = BoundaryPhysics.randomPointAway(current, bounds, 120f)
            val ctrl = BoundaryPhysics.randomPointInBounds(bounds)
            val sprintPoints = quadBezierSample(current, ctrl, target, stepsPerSprint)
            points.addAll(sprintPoints)

            val pauseSteps = (5..15).random()
            repeat(pauseSteps) { points.add(PointF(target.x, target.y)) }
            current = PointF(target.x, target.y)
        }
        return points
    }

    private fun danglingPath(start: PointF, bounds: RectF, steps: Int): List<PointF> {
        val points = mutableListOf<PointF>()
        val topY = bounds.top + 20f
        val bottomY = bounds.top + bounds.height() * 0.7f

        val anchorX = start.x + ((Math.random() - 0.5) * bounds.width() * 0.5).toFloat()
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val y = topY + (bottomY - topY) * t
            val swing = sin(t * 6 * PI).toFloat() * 40f
            points.add(PointF(anchorX + swing, y))
        }
        val pauseSteps = 20
        repeat(pauseSteps) {
            points.add(PointF(anchorX, bottomY))
        }
        val halfSteps = (steps / 2).coerceAtLeast(1)
        for (i in 0..halfSteps) {
            val t = i.toFloat() / halfSteps
            val y = bottomY - (bottomY - topY) * t
            points.add(PointF(anchorX, y))
        }
        return points
    }

    private fun driftingPath(start: PointF, bounds: RectF, steps: Int): List<PointF> {
        val points = mutableListOf<PointF>()
        val endY = bounds.bottom - 30f
        val drift = ((Math.random() - 0.5) * bounds.width() * 0.4).toFloat()

        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = start.x + drift * t + sin(t * 3 * PI).toFloat() * 50f
            val y = start.y + (endY - start.y) * t
            points.add(PointF(x.coerceIn(bounds.left + 20f, bounds.right - 20f), y))
        }
        return points
    }

    private fun bouncingPath(start: PointF, bounds: RectF, steps: Int): List<PointF> {
        val points = mutableListOf<PointF>()
        var x = start.x
        var y = start.y
        var vx = ((Math.random() - 0.5) * 8).toFloat()
        var vy = 2f
        val gravity = 0.15f
        val bounceFactor = -0.75f
        val radius = 30f

        for (i in 0..steps) {
            vy += gravity
            x += vx
            y += vy

            if (y + radius > bounds.bottom) {
                y = bounds.bottom - radius
                vy *= bounceFactor
            }
            if (y - radius < bounds.top) {
                y = bounds.top + radius
                vy *= bounceFactor
            }
            if (x - radius < bounds.left) {
                x = bounds.left + radius
                vx = -vx
            }
            if (x + radius > bounds.right) {
                x = bounds.right - radius
                vx = -vx
            }
            points.add(PointF(x, y))
        }
        return points
    }

    fun cubicBezierSample(
        p0: PointF, p1: PointF, p2: PointF, p3: PointF, steps: Int
    ): List<PointF> {
        return (0..steps).map { i ->
            val t = i.toFloat() / steps
            val u = 1 - t
            val x = u * u * u * p0.x + 3 * u * u * t * p1.x + 3 * u * t * t * p2.x + t * t * t * p3.x
            val y = u * u * u * p0.y + 3 * u * u * t * p1.y + 3 * u * t * t * p2.y + t * t * t * p3.y
            PointF(x, y)
        }
    }

    private fun quadBezierSample(
        p0: PointF, p1: PointF, p2: PointF, steps: Int
    ): List<PointF> {
        return (0..steps).map { i ->
            val t = i.toFloat() / steps
            val u = 1 - t
            val x = u * u * p0.x + 2 * u * t * p1.x + t * t * p2.x
            val y = u * u * p0.y + 2 * u * t * p1.y + t * t * p2.y
            PointF(x, y)
        }
    }
}
