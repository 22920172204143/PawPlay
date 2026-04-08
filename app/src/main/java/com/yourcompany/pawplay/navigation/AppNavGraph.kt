package com.yourcompany.pawplay.navigation

import androidx.compose.runtime.Composable
import com.yourcompany.pawplay.model.ToyLibrary
import com.yourcompany.pawplay.service.HapticManager
import com.yourcompany.pawplay.service.SoundManager
import com.yourcompany.pawplay.ui.game.GameScreen

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
