/**
 * @file DirectSpriteDrawer.kt
 * @brief Renders prey using individual PNG sprites with programmatic bone-based animation.
 *        Bone hierarchy and animations are defined in code — no external tool required.
 * @version 1.0
 * @date 2025-03-22
 * @copyright Copyright (c) Tuya Inc.
 */
package com.yourcompany.pawplay.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import com.yourcompany.pawplay.game.dragonbones.Bone
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object DirectSpriteDrawer {

    private data class PartDef(
        val boneName: String,
        val assetFile: String,
        val designW: Float,
        val designH: Float,
        val pivotX: Float = 0.5f,
        val pivotY: Float = 0.5f,
        val mirrorY: Boolean = false
    )

    private data class BoneDef(
        val name: String,
        val parent: String? = null,
        val x: Float = 0f,
        val y: Float = 0f,
        val rotation: Float = 0f
    )

    private class SpriteModel(
        val designSize: Float,
        val boneDefs: List<BoneDef>,
        val drawOrder: List<PartDef>
    ) {
        val bones = mutableMapOf<String, Bone>()
        var rootBone: Bone? = null
        val bitmaps = mutableMapOf<String, Bitmap>()
        var wingPhase = 0f
        var antennaPhase = 0f

        fun buildHierarchy() {
            bones.clear()
            for (bd in boneDefs) {
                val bone = Bone(bd.name)
                bone.localX = bd.x
                bone.localY = bd.y
                bone.localRotation = bd.rotation
                bones[bd.name] = bone
            }
            for (bd in boneDefs) {
                val bone = bones[bd.name] ?: continue
                if (bd.parent != null) {
                    bones[bd.parent]?.let { parent ->
                        bone.parent = parent
                        parent.children.add(bone)
                    }
                } else {
                    rootBone = bone
                }
            }
        }
    }

    private val models = mutableMapOf<Int, SpriteModel>()
    private var initialized = false

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val shadowOvalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(55, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        alpha = 80
        colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
            1.6f, 0f, 0f, 0f, 80f,
            1.0f, 1.3f, 0f, 0f, 50f,
            0f, 0f, 0.4f, 0f, 0f,
            0f, 0f, 0f, 0.45f, 0f
        )))
    }

    private val shadowRect = RectF()
    private val dstRect = RectF()

    /**
     * @brief Initialize and load all available sprite models
     * @param[in] context Android context for asset access
     * @return none
     */
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        loadBugModel(context)
    }

    /**
     * @brief Check if a sprite model exists for the given toy ID
     * @param[in] toyId toy identifier
     * @return true if model is available
     */
    fun hasModel(toyId: Int) = models.containsKey(toyId)

    private fun loadBugModel(context: Context) {
        val boneDefs = listOf(
            BoneDef("root"),
            BoneDef("body", "root"),
            BoneDef("head", "body", x = 42f, y = 2f),
            BoneDef("ant_l", "head", x = 22f, y = -10f, rotation = -35f),
            BoneDef("ant_r", "head", x = 22f, y = 10f, rotation = 35f),
            BoneDef("wing_l", "body", x = -15f, y = -12f, rotation = -15f),
            BoneDef("wing_r", "body", x = -15f, y = 12f, rotation = 15f)
        )

        val drawOrder = listOf(
            PartDef("wing_l", "wing.png", 95f, 40f,
                pivotX = 0.05f, pivotY = 0.5f),
            PartDef("wing_r", "wing.png", 95f, 40f,
                pivotX = 0.05f, pivotY = 0.5f, mirrorY = true),
            PartDef("body", "body.png", 85f, 85f),
            PartDef("head", "head.png", 50f, 50f),
            PartDef("ant_l", "antenna.png", 55f, 28f,
                pivotX = 0.9f, pivotY = 0.7f),
            PartDef("ant_r", "antenna.png", 55f, 28f,
                pivotX = 0.9f, pivotY = 0.3f, mirrorY = true)
        )

        val model = SpriteModel(200f, boneDefs, drawOrder)
        model.buildHierarchy()

        val assetDir = "sprites/bug"
        val fileNames = setOf("body.png", "head.png", "wing.png", "antenna.png")

        for (fileName in fileNames) {
            try {
                val bmp = context.assets.open("$assetDir/$fileName").use {
                    BitmapFactory.decodeStream(it)
                } ?: continue
                model.bitmaps[fileName] = bmp
            } catch (_: Exception) {
                // Asset not available
            }
        }

        if (model.bitmaps.size == fileNames.size) {
            models[1] = model
        }
    }

    /**
     * @brief Draw a sprite-based prey with full bone animation
     * @param[in] canvas target Canvas
     * @param[in] toyId toy identifier
     * @param[in] cx center X position on screen
     * @param[in] cy center Y position on screen
     * @param[in] size display size in pixels
     * @param[in] dt delta time in seconds
     * @param[in] heading facing direction in degrees
     * @param[in] speed current movement speed
     * @param[in] scaleX squash-stretch X
     * @param[in] scaleY squash-stretch Y
     * @param[in] headLead head rotation offset in degrees
     * @return none
     */
    fun drawPrey(
        canvas: Canvas, toyId: Int,
        cx: Float, cy: Float, size: Float,
        dt: Float,
        heading: Float = 0f,
        speed: Float = 0f,
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        headLead: Float = 0f
    ) {
        val model = models[toyId] ?: return

        val wingFreq = 22f + speed * 4f
        model.wingPhase += dt * wingFreq
        val wingBase = sin(model.wingPhase.toDouble()).toFloat()
        val wingAngle = wingBase * 30f
        val wingScale = 0.85f + abs(wingBase) * 0.15f

        val antFreq = 4f + speed * 0.6f
        model.antennaPhase += dt * antFreq
        val antAngle = sin(model.antennaPhase.toDouble()).toFloat() * 15f
        val antAngle2 = sin((model.antennaPhase * 1.3).toDouble()).toFloat() * 12f

        val bodyBob = sin((model.wingPhase * 0.5).toDouble()).toFloat() * 2f

        model.bones["root"]?.overrideRotation = heading
        model.bones["body"]?.let {
            it.overrideScaleX = scaleX
            it.overrideScaleY = scaleY
            it.animY = bodyBob
        }
        model.bones["head"]?.overrideRotation = headLead * 0.4f
        model.bones["wing_l"]?.let {
            it.overrideRotation = wingAngle
            it.overrideScaleX = wingScale
        }
        model.bones["wing_r"]?.let {
            it.overrideRotation = -wingAngle
            it.overrideScaleX = wingScale
        }
        model.bones["ant_l"]?.overrideRotation = antAngle
        model.bones["ant_r"]?.overrideRotation = -antAngle2

        for ((_, bone) in model.bones) {
            bone.resetAnim()
        }
        model.rootBone?.updateWorldTransform()

        val drawScale = size / model.designSize

        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(drawScale, drawScale)

        drawShadow(canvas)
        drawGlow(canvas, model)

        for (part in model.drawOrder) {
            drawPart(canvas, model, part)
        }

        canvas.restore()
    }

    private fun drawShadow(canvas: Canvas) {
        canvas.save()
        canvas.translate(0f, 22f)
        canvas.scale(1f, 0.45f)
        shadowRect.set(-42f, -42f, 42f, 42f)
        canvas.drawOval(shadowRect, shadowOvalPaint)
        canvas.restore()
    }

    private fun drawGlow(canvas: Canvas, model: SpriteModel) {
        val bodyBmp = model.bitmaps["body.png"] ?: return
        val bodyBone = model.bones["body"] ?: return

        canvas.save()
        canvas.concat(bodyBone.worldMatrix)
        val gs = 1.4f
        dstRect.set(-50f * gs, -50f * gs, 50f * gs, 50f * gs)
        canvas.drawBitmap(bodyBmp, null, dstRect, glowPaint)
        canvas.restore()
    }

    private fun drawPart(canvas: Canvas, model: SpriteModel, part: PartDef) {
        val bone = model.bones[part.boneName] ?: return
        val bitmap = model.bitmaps[part.assetFile] ?: return

        val left = -part.designW * part.pivotX
        val top = -part.designH * part.pivotY
        dstRect.set(left, top, left + part.designW, top + part.designH)

        canvas.save()
        canvas.concat(bone.worldMatrix)
        if (part.mirrorY) {
            canvas.scale(1f, -1f)
        }
        canvas.drawBitmap(bitmap, null, dstRect, basePaint)
        canvas.restore()
    }

    

    /**
     * @brief Release all loaded bitmap resources
     * @return none
     */
    fun release() {
        for ((_, model) in models) {
            for ((_, bmp) in model.bitmaps) {
                bmp.recycle()
            }
        }
        models.clear()
        initialized = false
    }
}
