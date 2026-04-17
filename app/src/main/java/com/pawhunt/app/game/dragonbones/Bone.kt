/**
 * @file Bone.kt
 * @brief Runtime bone node with local/world transform and hierarchy.
 */
package com.pawhunt.app.game.dragonbones

import android.graphics.Matrix
import kotlin.math.cos
import kotlin.math.sin

class Bone(val name: String) {

    var parent: Bone? = null
    val children = mutableListOf<Bone>()

    var localX = 0f
    var localY = 0f
    var localRotation = 0f
    var localScaleX = 1f
    var localScaleY = 1f

    var animX = 0f
    var animY = 0f
    var animRotation = 0f
    var animScaleX = 1f
    var animScaleY = 1f

    var overrideRotation: Float? = null
    var overrideScaleX: Float? = null
    var overrideScaleY: Float? = null

    val worldMatrix = Matrix()

    private val tempMatrix = Matrix()

    /**
     * @brief Apply bind-pose from skeleton data
     * @param[in] data bind-pose transform
     * @return none
     */
    fun applyBindPose(data: DBTransform) {
        localX = data.x
        localY = data.y
        localRotation = data.rotation
        localScaleX = data.scaleX
        localScaleY = data.scaleY
    }

    /**
     * @brief Reset per-frame animation deltas
     * @return none
     */
    fun resetAnim() {
        animX = 0f
        animY = 0f
        animRotation = 0f
        animScaleX = 1f
        animScaleY = 1f
    }

    /**
     * @brief Compute worldMatrix by composing local+anim+override on top of parent
     * @return none
     */
    fun updateWorldTransform() {
        val totalRotDeg = localRotation + animRotation + (overrideRotation ?: 0f)
        val totalSX = localScaleX * animScaleX * (overrideScaleX ?: 1f)
        val totalSY = localScaleY * animScaleY * (overrideScaleY ?: 1f)
        val tx = localX + animX
        val ty = localY + animY

        val radians = Math.toRadians(totalRotDeg.toDouble())
        val cosR = cos(radians).toFloat()
        val sinR = sin(radians).toFloat()

        tempMatrix.setValues(
            floatArrayOf(
                cosR * totalSX, -sinR * totalSY, tx,
                sinR * totalSX, cosR * totalSY, ty,
                0f, 0f, 1f
            )
        )

        val p = parent
        if (p != null) {
            worldMatrix.set(p.worldMatrix)
            worldMatrix.preConcat(tempMatrix)
        } else {
            worldMatrix.set(tempMatrix)
        }

        for (child in children) {
            child.updateWorldTransform()
        }
    }
}
