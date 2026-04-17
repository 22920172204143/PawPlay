package com.pawhunt.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pawhunt.app.navigation.AppNavGraph
import com.pawhunt.app.service.HapticManager
import com.pawhunt.app.service.SoundManager
import com.pawhunt.app.theme.PawPlayTheme

class MainActivity : ComponentActivity() {

    private var soundManager: SoundManager? = null
    private var hapticManager: HapticManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        soundManager = SoundManager(this)
        hapticManager = HapticManager(this)

        setContent {
            PawPlayTheme {
                AppNavGraph(
                    soundManager = soundManager,
                    hapticManager = hapticManager
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager?.release()
    }
}
