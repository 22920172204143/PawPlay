package com.yourcompany.pawplay.game

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

    var onTouchCallback: ((Float, Float) -> Unit)? = null

    init {
        holder.addCallback(this)
        isFocusable = true
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
                val x = event.getX(idx)
                val y = event.getY(idx)
                renderer.onTouch(x, y)
                onTouchCallback?.invoke(x, y)
            }
        }
        return true
    }
}
