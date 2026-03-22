package com.yourcompany.pawplay.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.yourcompany.pawplay.game.GameSurfaceView
import com.yourcompany.pawplay.model.GameBackground
import com.yourcompany.pawplay.model.Toy
import com.yourcompany.pawplay.service.HapticManager
import com.yourcompany.pawplay.service.SoundManager
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    selectedToys: List<Toy>,
    selectedBackground: GameBackground,
    speedMultiplier: Float,
    soundManager: SoundManager?,
    hapticManager: HapticManager?,
    onBack: () -> Unit
) {
    var showToolbar by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(soundManager?.enabled ?: true) }
    var gameSurfaceView by remember { mutableStateOf<GameSurfaceView?>(null) }

    LaunchedEffect(showToolbar) {
        if (showToolbar) {
            delay(4000)
            showToolbar = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                showToolbar = !showToolbar
            }
    ) {
        AndroidView(
            factory = { ctx ->
                GameSurfaceView(ctx).also { view ->
                    gameSurfaceView = view
                    view.renderer.speedMultiplier = speedMultiplier
                    view.renderer.background = selectedBackground

                    view.renderer.onHit = { _ ->
                        soundManager?.playHit()
                        hapticManager?.playHitFeedback()
                    }

                    view.onTouchCallback = { _, _ ->
                        if (showToolbar) showToolbar = false
                    }
                }
            },
            update = { view ->
                view.renderer.speedMultiplier = speedMultiplier
                view.renderer.background = selectedBackground

                if (view.renderer.preys.isEmpty() ||
                    view.renderer.preys.size != selectedToys.size
                ) {
                    view.renderer.setupPreys(selectedToys)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = showToolbar,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = {
                        soundEnabled = !soundEnabled
                        soundManager?.enabled = soundEnabled
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Toggle sound",
                        tint = Color.White
                    )
                }
            }
        }
    }

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
