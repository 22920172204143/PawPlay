package com.yourcompany.pawplay.navigation

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yourcompany.pawplay.model.BackgroundLibrary
import com.yourcompany.pawplay.model.GameSettingsRepository
import com.yourcompany.pawplay.model.ToyLibrary
import com.yourcompany.pawplay.service.AdManager
import com.yourcompany.pawplay.service.BillingManager
import com.yourcompany.pawplay.service.HapticManager
import com.yourcompany.pawplay.service.SoundManager
import com.yourcompany.pawplay.ui.game.GameScreen
import com.yourcompany.pawplay.ui.home.HomeScreen
import com.yourcompany.pawplay.ui.library.ContentLibraryScreen
import com.yourcompany.pawplay.ui.paywall.PaywallScreen
import com.yourcompany.pawplay.ui.settings.SettingsScreen
import com.yourcompany.pawplay.ui.settings.SettingsState
import kotlinx.coroutines.launch

object Routes {
    const val HOME = "home"
    const val GAME = "game"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val PAYWALL = "paywall"
}

@Composable
fun AppNavGraph(
    settingsRepo: GameSettingsRepository,
    billingManager: BillingManager?,
    adManager: AdManager?,
    soundManager: SoundManager?,
    hapticManager: HapticManager?,
    navController: NavHostController = rememberNavController()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? Activity

    val soundEnabled by settingsRepo.soundEnabled.collectAsState(initial = true)
    val musicEnabled by settingsRepo.musicEnabled.collectAsState(initial = true)
    val speedMultiplier by settingsRepo.speedMultiplier.collectAsState(initial = 1.0f)
    val preyCount by settingsRepo.preyCount.collectAsState(initial = 1)
    val keepScreenOn by settingsRepo.keepScreenOn.collectAsState(initial = true)
    val hapticEnabled by settingsRepo.hapticEnabled.collectAsState(initial = true)
    val selectedToyId by settingsRepo.selectedToyId.collectAsState(initial = 4)
    val selectedBgId by settingsRepo.selectedBgId.collectAsState(initial = 1)
    val isPro by settingsRepo.isPro.collectAsState(initial = false)

    soundManager?.enabled = soundEnabled
    hapticManager?.enabled = hapticEnabled

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                isPro = billingManager?.isPro ?: kotlinx.coroutines.flow.MutableStateFlow(isPro),
                adManager = if (!isPro) adManager else null,
                onPlay = { navController.navigate(Routes.GAME) },
                onLibrary = { navController.navigate(Routes.LIBRARY) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onPaywall = { navController.navigate(Routes.PAYWALL) }
            )
        }

        composable(Routes.GAME) {
            val selectedToys = remember(selectedToyId, preyCount) {
                val baseToy = ToyLibrary.allToys.find { it.id == selectedToyId }
                    ?: ToyLibrary.allToys.first()

                if (preyCount <= 1) {
                    listOf(baseToy)
                } else {
                    val available = if (isPro) ToyLibrary.allToys else ToyLibrary.freeToys
                    val extras = available.filter { it.id != baseToy.id }.shuffled()
                        .take(preyCount - 1)
                    listOf(baseToy) + extras
                }
            }

            val selectedBg = remember(selectedBgId) {
                BackgroundLibrary.allBackgrounds.find { it.id == selectedBgId }
                    ?: BackgroundLibrary.allBackgrounds.first()
            }

            GameScreen(
                selectedToys = selectedToys,
                selectedBackground = selectedBg,
                speedMultiplier = speedMultiplier,
                soundManager = soundManager,
                hapticManager = hapticManager,
                onBack = {
                    scope.launch {
                        val exitCount = settingsRepo.incrementGameExitCount()
                        if (!isPro && adManager != null && activity != null) {
                            adManager.showInterstitialIfReady(activity, exitCount)
                        }
                    }
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.LIBRARY) {
            ContentLibraryScreen(
                isPro = isPro,
                selectedToyId = selectedToyId,
                selectedBgId = selectedBgId,
                onSelectToy = { id -> scope.launch { settingsRepo.setSelectedToyId(id) } },
                onSelectBg = { id -> scope.launch { settingsRepo.setSelectedBgId(id) } },
                onBack = { navController.popBackStack() },
                onPaywall = { navController.navigate(Routes.PAYWALL) }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                state = SettingsState(
                    soundEnabled = soundEnabled,
                    musicEnabled = musicEnabled,
                    speedMultiplier = speedMultiplier,
                    preyCount = preyCount,
                    keepScreenOn = keepScreenOn,
                    hapticEnabled = hapticEnabled
                ),
                onSoundToggle = { scope.launch { settingsRepo.setSoundEnabled(it) } },
                onMusicToggle = { scope.launch { settingsRepo.setMusicEnabled(it) } },
                onSpeedChange = { scope.launch { settingsRepo.setSpeedMultiplier(it) } },
                onPreyCountChange = { scope.launch { settingsRepo.setPreyCount(it) } },
                onKeepScreenToggle = { scope.launch { settingsRepo.setKeepScreenOn(it) } },
                onHapticToggle = { scope.launch { settingsRepo.setHapticEnabled(it) } },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PAYWALL) {
            PaywallScreen(
                proPrice = billingManager?.proPrice
                    ?: kotlinx.coroutines.flow.MutableStateFlow("¥38"),
                yearlyPrice = billingManager?.yearlyPrice
                    ?: kotlinx.coroutines.flow.MutableStateFlow("¥68/year"),
                onBuyPro = { billingManager?.launchProPurchase() },
                onBuyYearly = { billingManager?.launchYearlyPurchase() },
                onRestore = { billingManager?.restorePurchases() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
