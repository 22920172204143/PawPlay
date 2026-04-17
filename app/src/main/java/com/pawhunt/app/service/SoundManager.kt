/**
 * @file SoundManager.kt
 * @brief Game audio manager: SoundPool for short SFX, MediaPlayer for ambient loops.
 */
package com.pawhunt.app.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.pawhunt.app.model.BehaviorType

class SoundManager(private val context: Context) {

    private val soundPool: SoundPool
    private val hitSoundId: Int
    private val popSoundId: Int

    private val hitSoundMap = mutableMapOf<String, Int>()
    private val uiOpenId: Int
    private val uiCloseId: Int

    private var ambientPlayer: MediaPlayer? = null
    private var currentAmbientRes = 0

    private var hitCooldown = 0f

    @Volatile
    var enabled = true

    private val ambientResNames = arrayOf(
        "amb_mulch",       // 0 Mulch  – cricket / garden
        "amb_forest",      // 1 Grass  – bird chirps
        "amb_stone",       // 2 Stone  – bubbles
        "amb_wood",        // 3 Wood   – mouse squeaks
        "amb_cute"         // 4 Cute   – bubbles (playful)
    )

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attrs)
            .build()

        hitSoundId = loadSoundSafe("hit")
        popSoundId = loadSoundSafe("pop")
        uiOpenId = loadSoundSafe("sfx_ui_open")
        uiCloseId = loadSoundSafe("sfx_ui_close")

        for (key in arrayOf("sfx_hit_bug", "sfx_hit_fish", "sfx_hit_bird", "sfx_hit_mouse", "sfx_hit_default")) {
            val id = loadSoundSafe(key)
            if (id != 0) hitSoundMap[key] = id
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

    /**
     * @brief Advance internal cooldown timer
     * @param[in] dt delta seconds
     * @return none
     */
    fun update(dt: Float) {
        if (hitCooldown > 0f) {
            hitCooldown -= dt
        }
    }

    /**
     * @brief Play hit sound based on prey behavior type, with 0.1s cooldown
     * @param[in] behaviorType the prey's behavior type
     * @return none
     */
    fun playHit(behaviorType: BehaviorType) {
        if (!enabled || hitCooldown > 0f) return
        hitCooldown = 0.1f

        val key = when (behaviorType) {
            BehaviorType.CRAWLING -> "sfx_hit_bug"
            BehaviorType.RUNNING -> "sfx_hit_mouse"
            BehaviorType.BOUNCING -> "sfx_hit_default"
            BehaviorType.SWIMMING -> "sfx_hit_fish"
            BehaviorType.FLYING -> "sfx_hit_bird"
            BehaviorType.DANGLING, BehaviorType.DRIFTING -> "sfx_hit_default"
            BehaviorType.RANDOM_CURVE -> "sfx_hit_default"
        }

        val id = hitSoundMap[key] ?: hitSoundMap["sfx_hit_default"] ?: hitSoundId
        if (id != 0) {
            soundPool.play(id, 0.85f, 0.85f, 1, 0, 1f)
        }
    }

    /**
     * @brief Play generic hit sound (legacy, no cooldown)
     * @return none
     */
    fun playHit() {
        if (!enabled) return
        if (hitSoundId != 0) {
            soundPool.play(hitSoundId, 0.8f, 0.8f, 1, 0, 1f)
        } else if (popSoundId != 0) {
            soundPool.play(popSoundId, 0.8f, 0.8f, 1, 0, 1f)
        }
    }

    /**
     * @brief Play pop sound effect
     * @return none
     */
    fun playPop() {
        if (!enabled || popSoundId == 0) return
        soundPool.play(popSoundId, 0.6f, 0.6f, 1, 0, 1.2f)
    }

    /**
     * @brief Play UI panel open sound
     * @return none
     */
    fun playUIOpen() {
        if (!enabled || uiOpenId == 0) return
        soundPool.play(uiOpenId, 0.5f, 0.5f, 1, 0, 1f)
    }

    /**
     * @brief Play UI panel close sound
     * @return none
     */
    fun playUIClose() {
        if (!enabled || uiCloseId == 0) return
        soundPool.play(uiCloseId, 0.5f, 0.5f, 1, 0, 1f)
    }

    /**
     * @brief Switch ambient background sound based on background index
     * @param[in] bgIndex background index (0..4)
     * @return none
     */
    fun playAmbient(bgIndex: Int) {
        if (!enabled) return
        val resName = ambientResNames.getOrNull(bgIndex) ?: return
        val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
        if (resId == 0 || resId == currentAmbientRes) return

        stopAmbient()
        currentAmbientRes = resId

        try {
            ambientPlayer = MediaPlayer.create(context, resId)?.apply {
                isLooping = true
                setVolume(0.3f, 0.3f)
                start()
            }
        } catch (_: Exception) {
            ambientPlayer = null
        }
    }

    /**
     * @brief Stop ambient background sound
     * @return none
     */
    fun stopAmbient() {
        try {
            ambientPlayer?.stop()
            ambientPlayer?.release()
        } catch (_: Exception) { }
        ambientPlayer = null
        currentAmbientRes = 0
    }

    /**
     * @brief Release all audio resources
     * @return none
     */
    fun release() {
        soundPool.release()
        stopAmbient()
    }
}
