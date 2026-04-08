/**
 * @file SpritePreyDrawer.kt
 * @brief Sprite-based prey drawer using DragonBones skeletal animation.
 *        Falls back to code-drawn PreyDrawer when no skeletal asset exists.
 */
package com.yourcompany.pawplay.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import com.yourcompany.pawplay.game.dragonbones.Armature
import com.yourcompany.pawplay.game.dragonbones.ArmatureRenderer
import com.yourcompany.pawplay.game.dragonbones.DBAtlasParser
import com.yourcompany.pawplay.game.dragonbones.DBSkeletonParser
import kotlin.math.sin

object SpritePreyDrawer {

    private data class LoadedModel(
        val armature: Armature,
        val renderer: ArmatureRenderer,
        val designSize: Float,
        var wingPhase: Float = 0f,
        var antennaPhase: Float = 0f
    )

    private val models = mutableMapOf<Int, LoadedModel>()
    private var initialized = false

    private val toyAssetMap = mapOf(
        1 to "bug",
        2 to "fish",
        3 to "cockroach",
        4 to "butterfly",
        5 to "mouse",
        6 to "spider",
        7 to "bee",
        8 to "feather",
        9 to "bird",
        10 to "yarn"
    )

    /**
     * @brief Initialize by loading all available DragonBones assets from assets/dragonbones/
     * @param[in] context Android context for asset access
     * @return none
     */
    fun init(context: Context) {
        if (initialized) return
        initialized = true

        for ((toyId, assetName) in toyAssetMap) {
            tryLoadModel(context, toyId, assetName)
        }
    }

    private fun tryLoadModel(context: Context, toyId: Int, assetName: String) {
        try {
            val basePath = "dragonbones/$assetName"
            val skePath = "$basePath/${assetName}_ske.json"
            val texJsonPath = "$basePath/${assetName}_tex.json"
            val texPngPath = "$basePath/${assetName}_tex.png"

            val skeJson = context.assets.open(skePath).bufferedReader().use { it.readText() }
            val texJson = context.assets.open(texJsonPath).bufferedReader().use { it.readText() }
            val texture: Bitmap = context.assets.open(texPngPath).use {
                BitmapFactory.decodeStream(it)
            } ?: return

            val skeleton = DBSkeletonParser.parse(skeJson)
            val atlas = DBAtlasParser.parse(texJson)

            if (skeleton.armatures.isEmpty()) return

            val armData = skeleton.armatures[0]
            val armature = Armature()
            armature.build(armData)

            if (armature.hasAnimation("idle")) {
                armature.playAnimation("idle", loop = true)
            }

            val renderer = ArmatureRenderer(atlas, texture)
            val designSize = 200f

            models[toyId] = LoadedModel(armature, renderer, designSize)
        } catch (_: Exception) {
            // Asset not found or parse error — this toyId will use code fallback
        }
    }

    /**
     * @brief Check if a toyId has a loaded skeletal model
     * @param[in] toyId toy identifier
     * @return true if skeletal model is available
     */
    fun hasModel(toyId: Int): Boolean = models.containsKey(toyId)

    /**
     * @brief Draw a prey using its skeletal model
     * @param[in] canvas target Canvas
     * @param[in] toyId toy identifier
     * @param[in] cx center X position
     * @param[in] cy center Y position
     * @param[in] size display size in pixels
     * @param[in] dt delta time for animation advance
     * @param[in] heading facing direction in degrees
     * @param[in] speed current movement speed for animation rate scaling
     * @param[in] scaleX squash-stretch X
     * @param[in] scaleY squash-stretch Y
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

        model.armature.setOverrideRotation("root", heading)

        if (model.armature.bones.containsKey("head")) {
            model.armature.setOverrideRotation("head", headLead)
        }

        model.armature.setOverrideScale("body", scaleX, scaleY)

        val wingFreq = 15f + speed * 2f
        model.wingPhase += dt * wingFreq
        val wingAngle = sin(model.wingPhase.toDouble()).toFloat() * 25f

        val arm = model.armature
        if (arm.bones.containsKey("wing_l")) {
            arm.setOverrideRotation("wing_l", wingAngle)
        }
        if (arm.bones.containsKey("wing_r")) {
            arm.setOverrideRotation("wing_r", -wingAngle)
        }

        val antFreq = 2.5f + speed * 0.3f
        model.antennaPhase += dt * antFreq
        val antAngle = sin(model.antennaPhase.toDouble()).toFloat() * 12f

        if (arm.bones.containsKey("ant_l")) {
            arm.setOverrideRotation("ant_l", antAngle)
        }
        if (arm.bones.containsKey("ant_r")) {
            arm.setOverrideRotation("ant_r", -antAngle * 0.8f)
        }

        if (arm.bones.containsKey("tail")) {
            arm.setOverrideRotation("tail", sin((model.wingPhase * 0.4f).toDouble()).toFloat() * 15f)
        }

        val animSpeed = (0.5f + speed * 0.15f).coerceIn(0.3f, 3f)
        model.armature.update(dt * animSpeed)

        val drawScale = size / model.designSize
        model.renderer.draw(canvas, model.armature, cx, cy, drawScale)
    }

    /**
     * @brief Release all loaded bitmap resources
     * @return none
     */
    fun release() {
        models.clear()
        initialized = false
    }
}
