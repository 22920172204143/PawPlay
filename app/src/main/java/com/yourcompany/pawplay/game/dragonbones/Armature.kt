/**
 * @file Armature.kt
 * @brief Skeletal tree container with animation state machine, keyframe interpolation,
 *        and procedural override support.
 */
package com.yourcompany.pawplay.game.dragonbones

class Armature {

    val bones = mutableMapOf<String, Bone>()
    val slots = mutableListOf<Slot>()
    var rootBone: Bone? = null

    private var animations = mapOf<String, DBAnimationData>()
    private var currentAnim: DBAnimationData? = null
    private var animTime = 0f
    private var animDurationSec = 1f
    private var animLoop = true
    private var animPlaying = false
    private var frameRate = 24

    /**
     * @brief Build bone hierarchy and slots from parsed armature data
     * @param[in] data parsed DBArmatureData
     * @return none
     */
    fun build(data: DBArmatureData) {
        frameRate = data.frameRate
        bones.clear()
        slots.clear()

        for (bd in data.bones) {
            val bone = Bone(bd.name)
            bone.applyBindPose(bd.transform)
            bones[bd.name] = bone
        }

        for (bd in data.bones) {
            val bone = bones[bd.name] ?: continue
            if (bd.parent != null) {
                val parentBone = bones[bd.parent]
                if (parentBone != null) {
                    bone.parent = parentBone
                    parentBone.children.add(bone)
                }
            } else {
                rootBone = bone
            }
        }

        for (sd in data.slots) {
            val bone = bones[sd.parent] ?: continue
            slots.add(Slot(sd.name, bone, sd.zOrder))
        }
        slots.sortBy { it.zOrder }

        if (data.skins.isNotEmpty()) {
            val skin = data.skins[0]
            for (slot in slots) {
                val skinSlot = skin.slots[slot.name]
                if (skinSlot != null && skinSlot.displays.isNotEmpty()) {
                    slot.displayName = skinSlot.displays[0].name
                    slot.displayTransform = skinSlot.displays[0].transform
                }
            }
        }

        animations = data.animations.associateBy { it.name }
    }

    /**
     * @brief Start playing an animation by name
     * @param[in] name animation name
     * @param[in] loop whether to loop
     * @return none
     */
    fun playAnimation(name: String, loop: Boolean = true) {
        val anim = animations[name] ?: return
        currentAnim = anim
        animTime = 0f
        animDurationSec = anim.duration.toFloat() / frameRate.coerceAtLeast(1)
        animLoop = loop
        animPlaying = true
    }

    /**
     * @brief Check if a named animation exists
     * @param[in] name animation name
     * @return true if the animation is available
     */
    fun hasAnimation(name: String): Boolean = animations.containsKey(name)

    /**
     * @brief Override rotation on a specific bone (applied on top of animation)
     * @param[in] boneName target bone
     * @param[in] degrees rotation override in degrees
     * @return none
     */
    fun setOverrideRotation(boneName: String, degrees: Float) {
        bones[boneName]?.overrideRotation = degrees
    }

    /**
     * @brief Clear rotation override on a bone
     * @param[in] boneName target bone
     * @return none
     */
    fun clearOverrideRotation(boneName: String) {
        bones[boneName]?.overrideRotation = null
    }

    /**
     * @brief Override scale on a specific bone
     * @param[in] boneName target bone
     * @param[in] sx scale X
     * @param[in] sy scale Y
     * @return none
     */
    fun setOverrideScale(boneName: String, sx: Float, sy: Float) {
        bones[boneName]?.let {
            it.overrideScaleX = sx
            it.overrideScaleY = sy
        }
    }

    /**
     * @brief Advance animation time and update all bone transforms
     * @param[in] dt delta time in seconds
     * @return none
     */
    fun update(dt: Float) {
        if (animPlaying && currentAnim != null) {
            animTime += dt
            if (animTime >= animDurationSec) {
                if (animLoop) {
                    animTime %= animDurationSec
                } else {
                    animTime = animDurationSec
                    animPlaying = false
                }
            }
        }

        for ((_, bone) in bones) {
            bone.resetAnim()
        }

        currentAnim?.let { applyAnimation(it) }

        rootBone?.updateWorldTransform()
    }

    private fun applyAnimation(anim: DBAnimationData) {
        val totalFrames = anim.duration.coerceAtLeast(1)
        val currentFrame = (animTime / animDurationSec * totalFrames).coerceIn(0f, totalFrames.toFloat())

        for (timeline in anim.boneTimelines) {
            val bone = bones[timeline.boneName] ?: continue

            if (timeline.translateFrames.isNotEmpty()) {
                val (x, y) = interpolateTranslate(timeline.translateFrames, currentFrame)
                bone.animX = x
                bone.animY = y
            }
            if (timeline.rotateFrames.isNotEmpty()) {
                bone.animRotation = interpolateRotation(timeline.rotateFrames, currentFrame)
            }
            if (timeline.scaleFrames.isNotEmpty()) {
                val (sx, sy) = interpolateScale(timeline.scaleFrames, currentFrame)
                bone.animScaleX = sx
                bone.animScaleY = sy
            }
        }
    }

    private fun interpolateTranslate(frames: List<DBKeyFrame>, time: Float): Pair<Float, Float> {
        if (frames.size == 1) return Pair(frames[0].x, frames[0].y)

        var elapsed = 0f
        for (i in frames.indices) {
            val frame = frames[i]
            val nextElapsed = elapsed + frame.duration
            if (time < nextElapsed || i == frames.lastIndex) {
                val next = frames.getOrElse(i + 1) { frames[0] }
                val t = if (frame.duration > 0) ((time - elapsed) / frame.duration).coerceIn(0f, 1f) else 0f
                val eased = if (frame.tweenEasing != null) ease(t) else 0f
                val finalT = if (frame.tweenEasing != null) eased else 0f
                return Pair(
                    frame.x + (next.x - frame.x) * finalT,
                    frame.y + (next.y - frame.y) * finalT
                )
            }
            elapsed = nextElapsed
        }
        return Pair(frames.last().x, frames.last().y)
    }

    private fun interpolateRotation(frames: List<DBKeyFrame>, time: Float): Float {
        if (frames.size == 1) return frames[0].rotate

        var elapsed = 0f
        for (i in frames.indices) {
            val frame = frames[i]
            val nextElapsed = elapsed + frame.duration
            if (time < nextElapsed || i == frames.lastIndex) {
                val next = frames.getOrElse(i + 1) { frames[0] }
                val t = if (frame.duration > 0) ((time - elapsed) / frame.duration).coerceIn(0f, 1f) else 0f
                val finalT = if (frame.tweenEasing != null) ease(t) else 0f
                var diff = next.rotate - frame.rotate
                while (diff > 180f) diff -= 360f
                while (diff < -180f) diff += 360f
                return frame.rotate + diff * finalT
            }
            elapsed = nextElapsed
        }
        return frames.last().rotate
    }

    private fun interpolateScale(frames: List<DBKeyFrame>, time: Float): Pair<Float, Float> {
        if (frames.size == 1) return Pair(frames[0].scaleX, frames[0].scaleY)

        var elapsed = 0f
        for (i in frames.indices) {
            val frame = frames[i]
            val nextElapsed = elapsed + frame.duration
            if (time < nextElapsed || i == frames.lastIndex) {
                val next = frames.getOrElse(i + 1) { frames[0] }
                val t = if (frame.duration > 0) ((time - elapsed) / frame.duration).coerceIn(0f, 1f) else 0f
                val finalT = if (frame.tweenEasing != null) ease(t) else 0f
                return Pair(
                    frame.scaleX + (next.scaleX - frame.scaleX) * finalT,
                    frame.scaleY + (next.scaleY - frame.scaleY) * finalT
                )
            }
            elapsed = nextElapsed
        }
        return Pair(frames.last().scaleX, frames.last().scaleY)
    }

    private fun ease(t: Float): Float = t * t * (3f - 2f * t)
}
