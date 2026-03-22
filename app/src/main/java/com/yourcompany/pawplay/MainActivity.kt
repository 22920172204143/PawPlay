package com.yourcompany.pawplay

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.yourcompany.pawplay.model.GameSettingsRepository
import com.yourcompany.pawplay.navigation.AppNavGraph
import com.yourcompany.pawplay.service.AdManager
import com.yourcompany.pawplay.service.BillingManager
import com.yourcompany.pawplay.service.HapticManager
import com.yourcompany.pawplay.service.SoundManager
import com.yourcompany.pawplay.theme.PawPlayTheme

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepo: GameSettingsRepository
    private var billingManager: BillingManager? = null
    private var adManager: AdManager? = null
    private var soundManager: SoundManager? = null
    private var hapticManager: HapticManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsRepo = GameSettingsRepository(applicationContext)
        soundManager = SoundManager(this)
        hapticManager = HapticManager(this)

        try {
            billingManager = BillingManager(this).also { it.init() }
        } catch (_: Exception) {
            billingManager = null
        }

        try {
            adManager = AdManager(this).also { it.initialize() }
        } catch (_: Exception) {
            adManager = null
        }

        setContent {
            PawPlayTheme {
                val keepScreenOn by settingsRepo.keepScreenOn.collectAsState(initial = true)

                DisposableEffect(keepScreenOn) {
                    if (keepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    onDispose {}
                }

                AppNavGraph(
                    settingsRepo = settingsRepo,
                    billingManager = billingManager,
                    adManager = adManager,
                    soundManager = soundManager,
                    hapticManager = hapticManager
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager?.release()
        billingManager?.destroy()
        adManager?.destroy()
    }
}
