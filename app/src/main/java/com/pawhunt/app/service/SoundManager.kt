/**
 * @file SoundManager.kt
 * @brief Game audio manager: SoundPool for short SFX and prey-type looping sounds.
 */
package com.pawhunt.app.service

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.pawhunt.app.model.BehaviorType

class SoundManager(private val context: Context) {

    private val soundPool: SoundPool
    private val hitSoundId: Int
    private val popSoundId: Int

    private val hitSoundMap = mutableMapOf<String, Int>()
    private val uiOpenId: Int
    private val uiCloseId: Int

    private var hitCooldown = 0f
    private var preyLoopStreamId = 0
    private var catCallStreamId = 0
    private val catCallId: Int

    @Volatile
    var enabled = true

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

        catCallId = loadSoundSafe("sfx_cat_call")
    }

    private fun loadSoundSafe(name: String): Int {
        val resId = context.resources.getIdentifier(name, "raw", context.packageName)
        return if (resId != 0) {
            soundPool.load(context, resId, 1)
        } else {
            0
        }
    }

    private fun behaviorToKey(behaviorType: BehaviorType): String = when (behaviorType) {
        BehaviorType.CRAWLING -> "sfx_hit_bug"
        BehaviorType.RUNNING -> "sfx_hit_mouse"
        BehaviorType.BOUNCING -> "sfx_hit_default"
        BehaviorType.SWIMMING -> "sfx_hit_fish"
        BehaviorType.FLYING -> "sfx_hit_bird"
        BehaviorType.DANGLING, BehaviorType.DRIFTING -> "sfx_hit_default"
        BehaviorType.RANDOM_CURVE -> "sfx_hit_default"
    }

    fun update(dt: Float) {
        if (hitCooldown > 0f) {
            hitCooldown -= dt
        }
    }

    /**
     * Start looping the cat-call sound to attract the cat.
     */
    fun startCatCall() {
        stopCatCall()
        if (!enabled || catCallId == 0) return
        catCallStreamId = soundPool.play(catCallId, 0.7f, 0.7f, 1, -1, 1f)
    }

    /**
     * Stop the cat-call loop.
     */
    fun stopCatCall() {
        if (catCallStreamId != 0) {
            soundPool.stop(catCallStreamId)
            catCallStreamId = 0
        }
    }

    /**
     * Start looping the prey's sound when it appears on screen.
     */
    fun startPreyLoop(behaviorType: BehaviorType) {
        stopPreyLoop()
        if (!enabled) return
        val id = hitSoundMap[behaviorToKey(behaviorType)] ?: return
        preyLoopStreamId = soundPool.play(id, 0.4f, 0.4f, 1, -1, 1f)
    }

    /**
     * Stop the currently looping prey sound.
     */
    fun stopPreyLoop() {
        if (preyLoopStreamId != 0) {
            soundPool.stop(preyLoopStreamId)
            preyLoopStreamId = 0
        }
    }

    /**
     * Play hit sound when prey is caught: stop loop, then play one-shot.
     */
    fun playHit(behaviorType: BehaviorType) {
        stopPreyLoop()
        if (!enabled || hitCooldown > 0f) return
        hitCooldown = 0.1f

        val id = hitSoundMap[behaviorToKey(behaviorType)] ?: hitSoundMap["sfx_hit_default"] ?: hitSoundId
        if (id != 0) {
            soundPool.play(id, 0.85f, 0.85f, 1, 0, 1f)
        }
    }

    fun playHit() {
        stopPreyLoop()
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
