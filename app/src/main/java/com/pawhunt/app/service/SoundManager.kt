/**
 * @file SoundManager.kt
 * @brief Game audio manager: SoundPool for short SFX and prey-type looping sounds.
 */
package com.pawhunt.app.service

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class SoundManager(private val context: Context) {

    private val soundPool: SoundPool
    private val popSoundId: Int
    private val hitSoundId: Int

    private val soundMap = mutableMapOf<String, Int>()
    private val uiOpenId: Int
    private val uiCloseId: Int

    private var preyLoopStreamId = 0
    private var catCallStreamId = 0
    private var catCallPending = false
    private var catCallSoundId = 0

    @Volatile
    var enabled = true

    /** Per-toy sound key mapping (toy name → res/raw file name) */
    private val toySoundKeys = mapOf(
        "Ladybug"   to "sfx_hit_bug",
        "Cockroach" to "sfx_hit_roach",
        "Fish"      to "sfx_hit_fish",
        "Mouse"     to "sfx_hit_mouse",
        "Bird"      to "sfx_hit_bird",
        "Spider"    to "sfx_hit_spider",
        "Bee"       to "sfx_hit_bee",
        "Butterfly" to "sfx_hit_butterfly"
    )

    /** Per-toy loop volume (default 0.5, boost quiet sources) */
    private val toyLoopVolume = mapOf(
        "Mouse"     to 0.9f,
        "Butterfly" to 0.8f
    )
    private val defaultLoopVolume = 0.5f

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attrs)
            .build()

        popSoundId = loadSoundSafe("pop")
        hitSoundId = loadSoundSafe("sfx_hit")
        uiOpenId = loadSoundSafe("sfx_ui_open")
        uiCloseId = loadSoundSafe("sfx_ui_close")

        val allKeys = toySoundKeys.values.toSet() + "sfx_cat_call"
        for (key in allKeys) {
            val id = loadSoundSafe(key)
            if (id != 0) soundMap[key] = id
        }
        catCallSoundId = soundMap["sfx_cat_call"] ?: 0

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0 && sampleId == catCallSoundId && catCallPending) {
                catCallPending = false
                catCallStreamId = soundPool.play(catCallSoundId, 0.7f, 0.7f, 1, -1, 1f)
            }
        }
    }

    private fun loadSoundSafe(name: String): Int {
        val resId = context.resources.getIdentifier(name, "raw", context.packageName)
        return if (resId != 0) {
            soundPool.load(context, resId, 1)
        } else {
            0
        }
    }

    private fun toyToSoundId(toyName: String): Int {
        val key = toySoundKeys[toyName] ?: return 0
        return soundMap[key] ?: 0
    }

    @Suppress("UNUSED_PARAMETER")
    fun update(dt: Float) {
        // reserved for future per-frame audio logic
    }

    fun startCatCall() {
        stopCatCall()
        if (!enabled) return
        if (catCallSoundId == 0) {
            catCallPending = true
            return
        }
        val stream = soundPool.play(catCallSoundId, 0.7f, 0.7f, 1, -1, 1f)
        if (stream == 0) {
            catCallPending = true
        } else {
            catCallStreamId = stream
            catCallPending = false
        }
    }

    fun stopCatCall() {
        catCallPending = false
        if (catCallStreamId != 0) {
            soundPool.stop(catCallStreamId)
            catCallStreamId = 0
        }
    }

    /**
     * Start looping the prey's sound. Stops any previous loop atomically.
     */
    fun startPreyLoop(toyName: String) {
        stopPreyLoop()
        if (!enabled) return
        val id = toyToSoundId(toyName)
        if (id == 0) return
        val vol = toyLoopVolume[toyName] ?: defaultLoopVolume
        preyLoopStreamId = soundPool.play(id, vol, vol, 1, -1, 1f)
    }

    fun stopPreyLoop() {
        if (preyLoopStreamId != 0) {
            soundPool.stop(preyLoopStreamId)
            preyLoopStreamId = 0
        }
    }

    /**
     * Called when prey is caught. Stops the loop and plays the unified hit SFX.
     */
    fun playHit() {
        stopPreyLoop()
        if (!enabled || hitSoundId == 0) return
        soundPool.play(hitSoundId, 0.7f, 0.7f, 1, 0, 1f)
    }

    /** Pause all audio (screen off / app background) */
    fun pauseAll() {
        soundPool.autoPause()
    }

    /** Resume all audio (screen on / app foreground) */
    fun resumeAll() {
        soundPool.autoResume()
    }

    fun playPop() {
        if (!enabled || popSoundId == 0) return
        soundPool.play(popSoundId, 0.6f, 0.6f, 1, 0, 1.2f)
    }

    fun playUIOpen() {
        if (!enabled || uiOpenId == 0) return
        soundPool.play(uiOpenId, 0.5f, 0.5f, 1, 0, 1f)
    }

    fun playUIClose() {
        if (!enabled || uiCloseId == 0) return
        soundPool.play(uiCloseId, 0.5f, 0.5f, 1, 0, 1f)
    }

    fun release() {
        stopCatCall()
        stopPreyLoop()
        soundPool.release()
    }
}
