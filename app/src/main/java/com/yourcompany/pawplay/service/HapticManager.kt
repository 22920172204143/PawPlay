package com.yourcompany.pawplay.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticManager(context: Context) {

    @Volatile
    var enabled = true

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        mgr.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun playHitFeedback() {
        if (!enabled) return
        if (!vibrator.hasVibrator()) return

        val effect = VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(effect)
    }

    fun playLightTap() {
        if (!enabled) return
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            vibrator.vibrate(effect)
        } else {
            val effect = VibrationEffect.createOneShot(15, 80)
            vibrator.vibrate(effect)
        }
    }
}
