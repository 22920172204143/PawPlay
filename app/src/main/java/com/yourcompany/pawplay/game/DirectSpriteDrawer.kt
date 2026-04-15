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
import android.graphics.RectF
import com.yourcompany.pawplay.game.dragonbones.Bone
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

object DirectSpriteDrawer {

    private const val BUG_FACING_OFFSET_DEG = 162f

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
    private val wingGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        alpha = 90
        colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
            0.65f, 0f, 0f, 0f, 0f,
            0f, 1.2f, 0f, 0f, 55f,
            0f, 0f, 1.25f, 0f, 85f,
            0f, 0f, 0f, 0.36f, 0f
        )))
    }
    private val bodyGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(60, 255, 186, 48)
        style = Paint.Style.FILL
    }
    private val bodyCoreGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(75, 255, 124, 24)
        style = Paint.Style.FILL
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
            BoneDef("body", "root", rotation = 0f),
            BoneDef("head", "body", x = -38f, y = -12f),
            BoneDef("ant_l", "head", x = -6f, y = -14f, rotation = -74f),
            BoneDef("ant_r", "head", x = 8f, y = -16f, rotation = -28f),
            BoneDef("wing_back_l", "body", x = -2f, y = -50f, rotation = -88f),
            BoneDef("wing_back_r", "body", x = 22f, y = -30f, rotation = -12f),
            BoneDef("wing_front", "body", x = 30f, y = -4f, rotation = 12f)
        )

        val drawOrder = listOf(
            PartDef("wing_back_l", "wing.png", 60f, 86f,
                pivotX = 0.10f, pivotY = 0.90f),
            PartDef("wing_back_r", "wing.png", 96f, 58f,
                pivotX = 0.09f, pivotY = 0.64f),
            PartDef("body", "body.png", 82f, 92f, pivotX = 0.52f, pivotY = 0.54f),
            PartDef("wing_front", "wing.png", 70f, 34f,
                pivotX = 0.08f, pivotY = 0.52f),
            PartDef("head", "head.png", 40f, 40f),
            PartDef("ant_l", "antenna.png", 42f, 30f,
                pivotX = 0.86f, pivotY = 0.86f),
            PartDef("ant_r", "antenna.png", 34f, 26f,
                pivotX = 0.84f, pivotY = 0.84f)
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
                model.bitmaps[fileName] = cropTransparentMargins(bmp)
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

        for ((_, bone) in model.bones) {
            bone.resetAnim()
        }

        val wingFreq = 22f + speed * 4f
        model.wingPhase += dt * wingFreq
        val wingBase = sin(model.wingPhase.toDouble()).toFloat()
        val wingAngle = wingBase * 28f
        val wingScale = 0.9f + abs(wingBase) * 0.16f

        val antFreq = 4f + speed * 0.6f
        model.antennaPhase += dt * antFreq
        val antAngle = sin(model.antennaPhase.toDouble()).toFloat() * 10f
        val antAngle2 = sin((model.antennaPhase * 1.3).toDouble()).toFloat() * 7f

        val bodyBob = sin((model.wingPhase * 0.5).toDouble()).toFloat() * 2f
        val facingHeading = heading + BUG_FACING_OFFSET_DEG
        val turnAmount = (headLead * 0.35f).coerceIn(-18f, 18f)

        model.bones["root"]?.overrideRotation = facingHeading
        model.bones["body"]?.let {
            it.overrideScaleX = scaleX
            it.overrideScaleY = scaleY
            it.animY = bodyBob
            it.overrideRotation = turnAmount * 0.10f
        }
        model.bones["head"]?.overrideRotation = turnAmount * 0.55f
        model.bones["wing_back_l"]?.let {
            it.overrideRotation = wingAngle - 88f - turnAmount * 0.28f
            it.overrideScaleX = wingScale * 0.96f
            it.overrideScaleY = 1.02f + abs(wingBase) * 0.03f
        }
        model.bones["wing_back_r"]?.let {
            it.overrideRotation = wingAngle * 0.68f - 12f - turnAmount * 0.10f
            it.overrideScaleX = 0.9f + abs(wingBase) * 0.12f
        }
        model.bones["wing_front"]?.let {
            it.overrideRotation = wingAngle * 0.46f + 14f + turnAmount * 0.22f
            it.overrideScaleX = 0.78f + abs(wingBase) * 0.10f
        }
        model.bones["ant_l"]?.overrideRotation = antAngle
        model.bones["ant_r"]?.overrideRotation = antAngle2 - 6f

        model.rootBone?.updateWorldTransform()

        val drawScale = size / model.designSize

        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(drawScale, drawScale)

        drawShadow(canvas, facingHeading)
        drawGlow(canvas, model)

        for (part in model.drawOrder) {
            drawPart(canvas, model, part)
        }

        canvas.restore()
    }

    private fun drawShadow(canvas: Canvas, heading: Float) {
        canvas.save()
        canvas.translate(4f, 26f)
        canvas.rotate(heading - 16f)
        canvas.scale(1.0f, 0.38f)
        shadowRect.set(-34f, -22f, 36f, 20f)
        canvas.drawOval(shadowRect, shadowOvalPaint)
        canvas.restore()
    }

    private fun drawGlow(canvas: Canvas, model: SpriteModel) {
        val bodyBone = model.bones["body"] ?: return
        val wingBack = model.bones["wing_back_l"] ?: return
        val wingFront = model.bones["wing_front"] ?: return
        val wingBmp = model.bitmaps["wing.png"] ?: return

        canvas.save()
        canvas.concat(bodyBone.worldMatrix)
        canvas.drawCircle(2f, 0f, 34f, bodyGlowPaint)
        canvas.drawCircle(6f, 2f, 18f, bodyCoreGlowPaint)
        canvas.restore()

        canvas.save()
        canvas.concat(wingBack.worldMatrix)
        dstRect.set(-10f, -20f, 106f, 22f)
        canvas.drawBitmap(wingBmp, null, dstRect, wingGlowPaint)
        canvas.restore()

        canvas.save()
        canvas.concat(wingFront.worldMatrix)
        dstRect.set(-8f, -12f, 72f, 16f)
        canvas.drawBitmap(wingBmp, null, dstRect, wingGlowPaint)
        canvas.restore()
    }

    private fun drawPart(canvas: Canvas, model: SpriteModel, part: PartDef) {
        val bone = model.bones[part.boneName] ?: return
        val bitmap = model.bitmaps[part.assetFile] ?: return

        val scale = min(part.designW / bitmap.width.toFloat(), part.designH / bitmap.height.toFloat())
        val drawW = bitmap.width * scale
        val drawH = bitmap.height * scale
        val left = -drawW * part.pivotX
        val top = -drawH * part.pivotY
        dstRect.set(left, top, left + drawW, top + drawH)

        canvas.save()
        canvas.concat(bone.worldMatrix)
        if (part.mirrorY) {
            canvas.scale(1f, -1f)
        }
        canvas.drawBitmap(bitmap, null, dstRect, basePaint)
        canvas.restore()
    }

    private fun cropTransparentMargins(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1

        for (y in 0 until height) {
            for (x in 0 until width) {
                val alpha = (pixels[y * width + x] ushr 24) and 0xFF
                if (alpha > 12) {
                    if (x < minX) minX = x
                    if (y < minY) minY = y
                    if (x > maxX) maxX = x
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return source
        }

        val padding = 2
        val croppedMinX = (minX - padding).coerceAtLeast(0)
        val croppedMinY = (minY - padding).coerceAtLeast(0)
        val croppedMaxX = (maxX + padding).coerceAtMost(width - 1)
        val croppedMaxY = (maxY + padding).coerceAtMost(height - 1)

        if (croppedMinX == 0 && croppedMinY == 0 && croppedMaxX == width - 1 && croppedMaxY == height - 1) {
            return source
        }

        val cropped = Bitmap.createBitmap(
            source,
            croppedMinX,
            croppedMinY,
            croppedMaxX - croppedMinX + 1,
            croppedMaxY - croppedMinY + 1
        )
        source.recycle()
        return cropped
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
