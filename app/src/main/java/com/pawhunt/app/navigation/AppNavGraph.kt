package com.pawhunt.app.navigation

import androidx.compose.runtime.Composable
import com.pawhunt.app.model.ToyLibrary
import com.pawhunt.app.service.HapticManager
import com.pawhunt.app.service.SoundManager
import com.pawhunt.app.ui.game.GameScreen

@Composable
fun AppNavGraph(
    soundManager: SoundManager?,
    hapticManager: HapticManager?
) {
    GameScreen(
        allToys = ToyLibrary.allToys,
        speedMultiplier = 1.0f,
        soundManager = soundManager,
        hapticManager = hapticManager
    )
}
