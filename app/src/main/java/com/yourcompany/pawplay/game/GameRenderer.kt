package com.yourcompany.pawplay.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
import com.yourcompany.pawplay.model.Toy
import com.yourcompany.pawplay.service.SoundManager

class GameRenderer(private val surfaceHolder: SurfaceHolder) {

    val preys = mutableListOf<PreyEntity>()
    val hitEffects = mutableListOf<HitEffect>()
    val trailSystem = TrailParticleSystem()

    var bounds = RectF()
        private set

    var speedMultiplier = 1.0f
    var allToys: List<Toy> = emptyList()
    var soundManager: SoundManager? = null

    var onHit: ((PointF) -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentToyIndex = 0
    private var pendingSetup = false

    private var catchCount = 0
    private var currentBgIndex = 0

    private val activeTouches = mutableMapOf<Int, TouchInfo>()
    @Volatile private var lastDt = 0.016f

    private data class TouchInfo(
        val x: Float, val y: Float,
        var holdTime: Float = 0f
    )

    fun setBounds(width: Int, height: Int) {
        bounds = RectF(0f, 0f, width.toFloat(), height.toFloat())
        if (pendingSetup) {
            setupSinglePrey()
        }
    }

    fun setupSinglePrey() {
        if (bounds.width() <= 0f || bounds.height() <= 0f) {
            pendingSetup = true
            return
        }
        pendingSetup = false
        if (allToys.isEmpty()) return

        val toy = allToys[currentToyIndex % allToys.size]
        synchronized(preys) {
            preys.clear()
            val prey = PreyEntity(toy, bounds, speedMultiplier)
            prey.buildNewPath()
            preys.add(prey)
        }
        if (catchCount == 0) {
            soundManager?.playAmbient(currentBgIndex)
        }
    }

    private fun spawnNextPrey() {
        currentToyIndex++
        catchCount++
        if (catchCount % 5 == 0) {
            currentBgIndex = (currentBgIndex + 1) % BackgroundRenderer.BG_COUNT
            trailSystem.clear()
            soundManager?.playAmbient(currentBgIndex)
        }
        setupSinglePrey()
    }

    fun update(deltaSeconds: Float) {
        lastDt = deltaSeconds
        soundManager?.update(deltaSeconds)
        if (bounds.width() <= 0f || bounds.height() <= 0f) return

        synchronized(preys) {
            for (prey in preys) {
                prey.speedMultiplier = speedMultiplier
                prey.update(deltaSeconds)

                val isMoving = prey.state != PreyEntity.State.WAITING && !prey.isHit
                trailSystem.tryEmit(
                    prey.position.x, prey.position.y,
                    prey.toy.primaryColor.hashCode(),
                    deltaSeconds, isMoving
                )
            }
        }

        trailSystem.update(deltaSeconds)

        synchronized(hitEffects) {
            for (effect in hitEffects) {
                effect.update(deltaSeconds)
            }
        }

        updateEscapeLogic(deltaSeconds)
    }

    private fun updateEscapeLogic(deltaSeconds: Float) {
        synchronized(activeTouches) {
            val iter = activeTouches.entries.iterator()
            while (iter.hasNext()) {
                val entry = iter.next()
                entry.value.holdTime += deltaSeconds
                if (entry.value.holdTime >= 3f) {
                    synchronized(preys) {
                        for (prey in preys) {
                            if (!prey.isHit && prey.checkHit(entry.value.x, entry.value.y)) {
                                prey.respawn()
                            }
                        }
                    }
                    iter.remove()
                }
            }
        }
    }

    fun draw() {
        val canvas: Canvas
        try {
            canvas = surfaceHolder.lockCanvas() ?: return
        } catch (_: Exception) {
            return
        }
        try {
            drawBackground(canvas)
            trailSystem.draw(canvas)
            synchronized(preys) {
                for (prey in preys) {
                    prey.draw(canvas, lastDt)
                }
            }
            synchronized(hitEffects) {
                val iterator = hitEffects.iterator()
                while (iterator.hasNext()) {
                    val effect = iterator.next()
                    effect.draw(canvas)
                    if (effect.isFinished) iterator.remove()
                }
            }
        } finally {
            try { surfaceHolder.unlockCanvasAndPost(canvas) } catch (_: Exception) {}
        }
    }

    private fun drawBackground(canvas: Canvas) {
        BackgroundRenderer.drawBackground(canvas, currentBgIndex, bounds)
    }

    fun onTouchDown(pointerId: Int, x: Float, y: Float) {
        synchronized(activeTouches) {
            activeTouches[pointerId] = TouchInfo(x, y)
        }

        synchronized(preys) {
            val hit = preys.firstOrNull { !it.isHit && it.checkHit(x, y) }
            if (hit != null) {
                hit.isHit = true
                val pos = PointF(hit.position.x, hit.position.y)
                synchronized(hitEffects) {
                    hitEffects.add(HitEffect(pos, hit.toy.primaryColor))
                }
                soundManager?.playHit(hit.toy.behaviorType)
                onHit?.invoke(pos)
                mainHandler.postDelayed({ spawnNextPrey() }, 600L)
            }
        }
    }

    fun onTouchUp(pointerId: Int) {
        synchronized(activeTouches) {
            activeTouches.remove(pointerId)
        }
    }

    fun onTouchMove(pointerId: Int, x: Float, y: Float) {
        synchronized(activeTouches) {
            activeTouches[pointerId]?.let {
                activeTouches[pointerId] = it.copy(x = x, y = y, holdTime = it.holdTime)
            }
        }
    }
}
