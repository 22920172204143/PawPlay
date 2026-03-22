package com.yourcompany.pawplay.service

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class SoundManager(context: Context) {

    private val soundPool: SoundPool
    private val hitSoundId: Int
    private val popSoundId: Int

    @Volatile
    var enabled = true

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(attrs)
            .build()

        hitSoundId = loadSoundSafe(context, "hit")
        popSoundId = loadSoundSafe(context, "pop")
    }

    private fun loadSoundSafe(context: Context, name: String): Int {
        val resId = context.resources.getIdentifier(name, "raw", context.packageName)
        return if (resId != 0) {
            soundPool.load(context, resId, 1)
        } else {
            0
        }
    }

    fun playHit() {
        if (!enabled) return
        if (hitSoundId != 0) {
            soundPool.play(hitSoundId, 0.8f, 0.8f, 1, 0, 1f)
        } else if (popSoundId != 0) {
            soundPool.play(popSoundId, 0.8f, 0.8f, 1, 0, 1f)
        }
    }

    fun playPop() {
        if (!enabled || popSoundId == 0) return
        soundPool.play(popSoundId, 0.6f, 0.6f, 1, 0, 1.2f)
    }

    fun release() {
        soundPool.release()
    }
}
