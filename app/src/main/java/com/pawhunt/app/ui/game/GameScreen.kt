package com.pawhunt.app.ui.game

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.pawhunt.app.game.GameSurfaceView
import com.pawhunt.app.model.Toy
import com.pawhunt.app.service.HapticManager
import com.pawhunt.app.service.SoundManager

@Composable
fun GameScreen(
    allToys: List<Toy>,
    speedMultiplier: Float,
    soundManager: SoundManager?,
    hapticManager: HapticManager?
) {
    var gameSurfaceView by remember { mutableStateOf<GameSurfaceView?>(null) }

    AndroidView(
        factory = { ctx ->
            GameSurfaceView(ctx).also { view ->
                gameSurfaceView = view
                view.renderer.speedMultiplier = speedMultiplier
                view.renderer.allToys = allToys
                view.renderer.soundManager = soundManager

                view.renderer.onHit = { _ ->
                    hapticManager?.playHitFeedback()
                }
            }
        },
        update = { view ->
            view.renderer.speedMultiplier = speedMultiplier
            view.renderer.allToys = allToys

            if (view.renderer.preys.isEmpty()) {
                view.renderer.setupSinglePrey()
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? android.app.Activity
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            gameSurfaceView?.stopGame()
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}
