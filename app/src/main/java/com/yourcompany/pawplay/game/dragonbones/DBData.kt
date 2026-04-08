/**
 * @file DBData.kt
 * @brief DragonBones data model classes parsed from _ske.json and _tex.json exports.
 */
package com.yourcompany.pawplay.game.dragonbones

import android.graphics.Rect

data class DBTransform(
    val x: Float = 0f,
    val y: Float = 0f,
    val rotation: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f
)

data class DBBoneData(
    val name: String,
    val parent: String? = null,
    val transform: DBTransform = DBTransform()
)

data class DBSlotData(
    val name: String,
    val parent: String,
    val displayIndex: Int = 0,
    val zOrder: Int = 0
)

data class DBDisplayData(
    val name: String,
    val type: String = "image",
    val transform: DBTransform = DBTransform()
)

data class DBSkinSlotData(
    val slotName: String,
    val displays: List<DBDisplayData>
)

data class DBSkinData(
    val name: String,
    val slots: Map<String, DBSkinSlotData>
)

data class DBKeyFrame(
    val duration: Int = 1,
    val tweenEasing: Float? = 0f,
    val x: Float = 0f,
    val y: Float = 0f,
    val rotate: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f
)

data class DBBoneTimeline(
    val boneName: String,
    val translateFrames: List<DBKeyFrame> = emptyList(),
    val rotateFrames: List<DBKeyFrame> = emptyList(),
    val scaleFrames: List<DBKeyFrame> = emptyList()
)

data class DBAnimationData(
    val name: String,
    val duration: Int,
    val playTimes: Int = 0,
    val boneTimelines: List<DBBoneTimeline> = emptyList()
)

data class DBArmatureData(
    val name: String,
    val frameRate: Int = 24,
    val bones: List<DBBoneData>,
    val slots: List<DBSlotData>,
    val skins: List<DBSkinData>,
    val animations: List<DBAnimationData>
)

data class DBSkeletonData(
    val name: String,
    val version: String,
    val frameRate: Int,
    val armatures: List<DBArmatureData>
)

data class DBAtlasRegion(
    val name: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val frameX: Int = 0,
    val frameY: Int = 0,
    val frameWidth: Int = 0,
    val frameHeight: Int = 0
) {
    val rect: Rect get() = Rect(x, y, x + width, y + height)
}

data class DBAtlasData(
    val name: String,
    val imagePath: String,
    val width: Int,
    val height: Int,
    val regions: Map<String, DBAtlasRegion>
)
