package com.yourcompany.pawplay.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
import com.yourcompany.pawplay.model.GameBackground
import com.yourcompany.pawplay.model.Toy

class GameRenderer(private val surfaceHolder: SurfaceHolder) {

    val preys = mutableListOf<PreyEntity>()
    val hitEffects = mutableListOf<HitEffect>()

    var bounds = RectF()
        private set

    var speedMultiplier = 1.0f
    var background: GameBackground? = null

    var onHit: ((PointF) -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    fun setBounds(width: Int, height: Int) {
        bounds = RectF(0f, 0f, width.toFloat(), height.toFloat())
        pendingToys?.let { setupPreys(it) }
    }

    private var pendingToys: List<Toy>? = null

    fun setupPreys(toys: List<Toy>) {
        if (bounds.width() <= 0f || bounds.height() <= 0f) {
            pendingToys = toys
            return
        }
        pendingToys = null
        synchronized(preys) {
            preys.clear()
            for (toy in toys) {
                val prey = PreyEntity(toy, bounds, speedMultiplier)
                prey.buildNewPath()
                preys.add(prey)
            }
        }
    }

    fun update(deltaSeconds: Float) {
        if (bounds.width() <= 0f || bounds.height() <= 0f) return
        synchronized(preys) {
            for (prey in preys) {
                prey.speedMultiplier = speedMultiplier
                prey.update(deltaSeconds)
            }
        }
        synchronized(hitEffects) {
            for (effect in hitEffects) {
                effect.update(deltaSeconds)
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
            synchronized(preys) {
                for (prey in preys) {
                    prey.draw(canvas)
                }
            }
            synchronized(hitEffects) {
                val iterator = hitEffects.iterator()
                while (iterator.hasNext()) {
                    val effect = iterator.next()
                    effect.draw(canvas)
                    if (effect.isFinished) {
                        iterator.remove()
                    }
                }
            }
        } finally {
            try {
                surfaceHolder.unlockCanvasAndPost(canvas)
            } catch (_: Exception) {
                // surface destroyed
            }
        }
    }

    private fun drawBackground(canvas: Canvas) {
        val bg = background
        if (bg != null) {
            BackgroundRenderer.drawBackground(canvas, bg.id, bounds)
        } else {
            canvas.drawColor(Color.parseColor("#2E2E2E"))
        }
    }

    fun onTouch(x: Float, y: Float) {
        synchronized(preys) {
            val hit = preys.firstOrNull { !it.isHit && it.checkHit(x, y) }
            if (hit != null) {
                hit.isHit = true
                val pos = PointF(hit.position.x, hit.position.y)

                synchronized(hitEffects) {
                    hitEffects.add(HitEffect(pos, hit.toy.primaryColor))
                }

                onHit?.invoke(pos)

                val delay = (300L..800L).random()
                mainHandler.postDelayed({
                    hit.respawn()
                }, delay)
            }
        }
    }
}
