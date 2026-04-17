/**
 * @file ArmatureRenderer.kt
 * @brief Renders an Armature's slots to Android Canvas using texture atlas regions.
 */
package com.pawhunt.app.game.dragonbones

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin

class ArmatureRenderer(
    private val atlas: DBAtlasData,
    private val texture: Bitmap
) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val slotMatrix = Matrix()
    private val srcRect = Rect()
    private val dstRect = RectF()

    /**
     * @brief Draw the armature at the given position and scale onto canvas
     * @param[in] canvas target Canvas
     * @param[in] armature the Armature to render
     * @param[in] cx center X position on canvas
     * @param[in] cy center Y position on canvas
     * @param[in] scale uniform scale factor (pixel size / design size)
     * @return none
     */
    fun draw(canvas: Canvas, armature: Armature, cx: Float, cy: Float, scale: Float) {
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(scale, scale)

        for (slot in armature.slots) {
            if (!slot.visible) continue
            val dispName = slot.displayName ?: continue
            val region = atlas.regions[dispName] ?: continue

            srcRect.set(region.x, region.y, region.x + region.width, region.y + region.height)

            val frameWidth = if (region.frameWidth > 0) region.frameWidth.toFloat() else region.width.toFloat()
            val frameHeight = if (region.frameHeight > 0) region.frameHeight.toFloat() else region.height.toFloat()
            val left = -frameWidth / 2f - region.frameX
            val top = -frameHeight / 2f - region.frameY
            dstRect.set(left, top, left + region.width, top + region.height)

            slotMatrix.set(slot.bone.worldMatrix)

            val dt = slot.displayTransform
            if (dt.x != 0f || dt.y != 0f || dt.rotation != 0f || dt.scaleX != 1f || dt.scaleY != 1f) {
                val rad = Math.toRadians(dt.rotation.toDouble())
                val cr = cos(rad).toFloat()
                val sr = sin(rad).toFloat()
                val m = Matrix()
                m.setValues(
                    floatArrayOf(
                        cr * dt.scaleX, -sr * dt.scaleY, dt.x,
                        sr * dt.scaleX, cr * dt.scaleY, dt.y,
                        0f, 0f, 1f
                    )
                )
                slotMatrix.preConcat(m)
            }

            canvas.save()
            canvas.concat(slotMatrix)
            canvas.drawBitmap(texture, srcRect, dstRect, paint)
            canvas.restore()
        }

        canvas.restore()
    }
}
