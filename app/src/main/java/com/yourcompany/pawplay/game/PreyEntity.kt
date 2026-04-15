package com.yourcompany.pawplay.game

import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.RectF
import com.yourcompany.pawplay.model.BehaviorType
import com.yourcompany.pawplay.model.Toy
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sin

class PreyEntity(
    val toy: Toy,
    private val bounds: RectF,
    var speedMultiplier: Float = 1.0f
) {

    enum class State { WAITING, SPRINTING, CRAWLING, SMOOTH_MOVING }

    var position = BoundaryPhysics.randomPointInBounds(bounds)
    var isHit = false

    var state = State.WAITING
        private set
    var scaleX = 1f
        private set
    var scaleY = 1f
        private set
    var animPhase = 0f
        private set

    var heading = 0f
        private set
    var bodyWobble = 0f
        private set
    var headLead = 0f
        private set

    private var prevX = position.x
    private var prevY = position.y
    private var smoothHeading = 0f
    private var targetHeading = 0f
    var currentSpeed = 0f
        private set

    private var path: List<PointF> = emptyList()
    private var pathProgress = 0f
    private var stateTimer = 0f
    private var stateDuration = 0f
    private var moveTimer = 0f

    private val isBeetleType get() = toy.behaviorType == BehaviorType.CRAWLING ||
            toy.behaviorType == BehaviorType.RUNNING ||
            toy.behaviorType == BehaviorType.BOUNCING

    private val radius get() = toy.displaySize / 2f

    init {
        buildNewPath()
        enterState(if (isBeetleType) State.WAITING else State.SMOOTH_MOVING)
    }

    fun buildNewPath() {
        path = PreyBehavior.generatePath(toy.behaviorType, position, bounds)
        pathProgress = 0f
    }

    private fun enterState(next: State) {
        state = next
        stateTimer = 0f
        moveTimer = 0f
        stateDuration = when (next) {
            State.WAITING -> 1.0f + Math.random().toFloat() * 1.5f
            State.SPRINTING -> 0.4f + Math.random().toFloat() * 0.8f
            State.CRAWLING -> 0.8f + Math.random().toFloat() * 1.0f
            State.SMOOTH_MOVING -> 999f
        }
    }

    fun update(deltaSeconds: Float) {
        if (isHit) return
        animPhase += deltaSeconds
        stateTimer += deltaSeconds

        prevX = position.x
        prevY = position.y

        when (state) {
            State.WAITING -> {
                currentSpeed = 0f
                applySquash(0f)
                if (stateTimer >= stateDuration) {
                    buildNewPath()
                    enterState(State.SPRINTING)
                }
            }
            State.SPRINTING -> {
                val sprintSpeed = toy.baseSpeed * speedMultiplier * 2.5f
                currentSpeed = sprintSpeed
                advanceAlongPath(sprintSpeed, deltaSeconds)
                applySquash(sprintSpeed)
                if (stateTimer >= stateDuration) {
                    enterState(State.CRAWLING)
                }
            }
            State.CRAWLING -> {
                val crawlSpeed = toy.baseSpeed * speedMultiplier * 0.6f
                currentSpeed = crawlSpeed
                advanceAlongPath(crawlSpeed, deltaSeconds)
                applySquash(crawlSpeed)
                if (stateTimer >= stateDuration) {
                    enterState(State.WAITING)
                }
            }
            State.SMOOTH_MOVING -> {
                val speed = toy.baseSpeed * speedMultiplier
                val easeIn = (moveTimer * 2f).coerceAtMost(1f)
                moveTimer += deltaSeconds
                currentSpeed = speed * easeIn
                advanceAlongPath(currentSpeed, deltaSeconds)
                applySquash(currentSpeed)
                if (pathProgress >= path.size - 1) {
                    buildNewPath()
                }
            }
        }

        updateHeading(deltaSeconds)
    }

    private fun updateHeading(dt: Float) {
        val dx = position.x - prevX
        val dy = position.y - prevY
        val dist = hypot(dx, dy)

        if (dist > 0.5f) {
            targetHeading = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        }

        var diff = targetHeading - smoothHeading
        while (diff > 180f) diff -= 360f
        while (diff < -180f) diff += 360f
        val lerpSpeed = (8f * dt).coerceAtMost(1f)
        smoothHeading += diff * lerpSpeed
        heading = smoothHeading

        headLead = diff * 0.3f

        val wobbleIntensity = (currentSpeed / 6f).coerceIn(0f, 1f)
        bodyWobble = sin(animPhase * 8f).toFloat() * 4f * wobbleIntensity
    }

    private fun advanceAlongPath(speed: Float, dt: Float) {
        if (path.isEmpty()) {
            buildNewPath()
            return
        }
        val advance = speed * dt * 6f
        pathProgress += advance

        if (pathProgress >= path.size - 1) {
            if (isBeetleType) {
                enterState(State.WAITING)
            } else {
                buildNewPath()
            }
            return
        }

        val idx = pathProgress.toInt().coerceIn(0, path.size - 1)
        val nextIdx = (idx + 1).coerceIn(0, path.size - 1)
        val frac = pathProgress - idx
        val p1 = path[idx]; val p2 = path[nextIdx]
        position.x = p1.x + (p2.x - p1.x) * frac
        position.y = p1.y + (p2.y - p1.y) * frac
        BoundaryPhysics.softClamp(position, radius, bounds)
    }

    private fun applySquash(speed: Float) {
        val intensity = (speed / 8f).coerceIn(0f, 1f)
        scaleX = 1f + intensity * 0.15f
        scaleY = 1f - intensity * 0.1f
    }

    fun draw(canvas: Canvas, deltaSeconds: Float = 0.016f) {
        if (isHit) return
        if (SpritePreyDrawer.hasModel(toy.id)) {
            SpritePreyDrawer.drawPrey(
                canvas, toy.id,
                position.x, position.y, toy.displaySize,
                deltaSeconds,
                heading, currentSpeed, scaleX, scaleY, headLead
            )
        } else if (DirectSpriteDrawer.hasModel(toy.id)) {
            DirectSpriteDrawer.drawPrey(
                canvas, toy.id,
                position.x, position.y, toy.displaySize,
                deltaSeconds,
                heading, currentSpeed, scaleX, scaleY, headLead
            )
        } else {
            PreyDrawer.drawPrey(
                canvas, toy.id,
                position.x, position.y, toy.displaySize,
                animPhase, scaleX, scaleY,
                heading, bodyWobble
            )
        }
    }

    fun checkHit(touchX: Float, touchY: Float): Boolean {
        if (isHit) return false
        val hitRadius = toy.displaySize * 0.8f
        return hypot(touchX - position.x, touchY - position.y) <= hitRadius
    }

    fun respawn() {
        position = if (Math.random() < 0.5) {
            BoundaryPhysics.randomEdgePoint(bounds)
        } else {
            BoundaryPhysics.randomPointAway(position, bounds, 250f)
        }
        isHit = false
        buildNewPath()
        enterState(if (isBeetleType) State.WAITING else State.SMOOTH_MOVING)
    }
}
