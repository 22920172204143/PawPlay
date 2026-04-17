package com.pawhunt.app.game

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    val renderer = GameRenderer(holder)
    private var gameLoop: GameLoop? = null

    init {
        holder.addCallback(this)
        isFocusable = true
        BackgroundRenderer.init(context)
        DirectSpriteDrawer.init(context)
        SpritePreyDrawer.init(context)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        renderer.setBounds(width, height)
        startGame()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        renderer.setBounds(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopGame()
    }

    fun startGame() {
        if (gameLoop?.running == true) return
        gameLoop = GameLoop(renderer).apply { startLoop() }
    }

    fun stopGame() {
        gameLoop?.stopLoop()
        gameLoop = null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                val pointerId = event.getPointerId(idx)
                renderer.onTouchDown(pointerId, event.getX(idx), event.getY(idx))
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pid = event.getPointerId(i)
                    renderer.onTouchMove(pid, event.getX(i), event.getY(i))
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val idx = event.actionIndex
                renderer.onTouchUp(event.getPointerId(idx))
            }
            MotionEvent.ACTION_CANCEL -> {
                for (i in 0 until event.pointerCount) {
                    renderer.onTouchUp(event.getPointerId(i))
                }
            }
        }
        return true
    }
}
