/**
 * @file DBSkeletonParser.kt
 * @brief Parses DragonBones _ske.json skeleton export into DBSkeletonData.
 */
package com.yourcompany.pawplay.game.dragonbones

import org.json.JSONObject

object DBSkeletonParser {

    /**
     * @brief Parse a DragonBones skeleton JSON string
     * @param[in] json raw JSON content of _ske.json
     * @return DBSkeletonData containing all armature definitions
     */
    fun parse(json: String): DBSkeletonData {
        val root = JSONObject(json)
        val name = root.optString("name", "")
        val version = root.optString("version", "5.5")
        val frameRate = root.optInt("frameRate", 24)

        val armatures = mutableListOf<DBArmatureData>()
        val armArr = root.optJSONArray("armature")
        if (armArr != null) {
            for (i in 0 until armArr.length()) {
                armatures.add(parseArmature(armArr.getJSONObject(i), frameRate))
            }
        }

        return DBSkeletonData(name, version, frameRate, armatures)
    }

    private fun parseArmature(obj: JSONObject, defaultFR: Int): DBArmatureData {
        val name = obj.optString("name", "Armature")
        val fr = obj.optInt("frameRate", defaultFR)

        val bones = mutableListOf<DBBoneData>()
        val boneArr = obj.optJSONArray("bone")
        if (boneArr != null) {
            for (i in 0 until boneArr.length()) {
                bones.add(parseBone(boneArr.getJSONObject(i)))
            }
        }

        val slots = mutableListOf<DBSlotData>()
        val slotArr = obj.optJSONArray("slot")
        if (slotArr != null) {
            for (i in 0 until slotArr.length()) {
                val s = slotArr.getJSONObject(i)
                slots.add(
                    DBSlotData(
                        name = s.getString("name"),
                        parent = s.getString("parent"),
                        displayIndex = s.optInt("displayIndex", 0),
                        zOrder = s.optInt("z", i)
                    )
                )
            }
        }

        val skins = mutableListOf<DBSkinData>()
        val skinArr = obj.optJSONArray("skin")
        if (skinArr != null) {
            for (i in 0 until skinArr.length()) {
                skins.add(parseSkin(skinArr.getJSONObject(i)))
            }
        }

        val animations = mutableListOf<DBAnimationData>()
        val animArr = obj.optJSONArray("animation")
        if (animArr != null) {
            for (i in 0 until animArr.length()) {
                animations.add(parseAnimation(animArr.getJSONObject(i)))
            }
        }

        return DBArmatureData(name, fr, bones, slots, skins, animations)
    }

    private fun parseBone(obj: JSONObject): DBBoneData {
        val t = obj.optJSONObject("transform")
        return DBBoneData(
            name = obj.getString("name"),
            parent = if (obj.has("parent")) obj.getString("parent") else null,
            transform = parseTransform(t)
        )
    }

    private fun parseSkin(obj: JSONObject): DBSkinData {
        val name = obj.optString("name", "")
        val slotsMap = mutableMapOf<String, DBSkinSlotData>()

        val slotObj = obj.optJSONObject("slot") ?: return DBSkinData(name, slotsMap)

        val keys = slotObj.keys()
        while (keys.hasNext()) {
            val slotName = keys.next()
            val dispArr = slotObj.getJSONArray(slotName)
            val displays = mutableListOf<DBDisplayData>()
            for (i in 0 until dispArr.length()) {
                val d = dispArr.getJSONObject(i)
                displays.add(
                    DBDisplayData(
                        name = d.optString("name", ""),
                        type = d.optString("type", "image"),
                        transform = parseTransform(d.optJSONObject("transform"))
                    )
                )
            }
            slotsMap[slotName] = DBSkinSlotData(slotName, displays)
        }

        return DBSkinData(name, slotsMap)
    }

    private fun parseAnimation(obj: JSONObject): DBAnimationData {
        val name = obj.optString("name", "")
        val duration = obj.optInt("duration", 1)
        val playTimes = obj.optInt("playTimes", 0)

        val timelines = mutableListOf<DBBoneTimeline>()
        val boneArr = obj.optJSONArray("bone")
        if (boneArr != null) {
            for (i in 0 until boneArr.length()) {
                timelines.add(parseBoneTimeline(boneArr.getJSONObject(i)))
            }
        }

        return DBAnimationData(name, duration, playTimes, timelines)
    }

    private fun parseBoneTimeline(obj: JSONObject): DBBoneTimeline {
        val boneName = obj.getString("name")
        val translate = parseKeyFrames(obj.optJSONArray("translateFrame"))
        val rotate = parseKeyFrames(obj.optJSONArray("rotateFrame"))
        val scale = parseKeyFrames(obj.optJSONArray("scaleFrame"))
        return DBBoneTimeline(boneName, translate, rotate, scale)
    }

    private fun parseKeyFrames(arr: org.json.JSONArray?): List<DBKeyFrame> {
        if (arr == null) return emptyList()
        val frames = mutableListOf<DBKeyFrame>()
        for (i in 0 until arr.length()) {
            val f = arr.getJSONObject(i)
            frames.add(
                DBKeyFrame(
                    duration = f.optInt("duration", 1),
                    tweenEasing = if (f.has("tweenEasing")) f.optDouble("tweenEasing", 0.0).toFloat() else null,
                    x = f.optDouble("x", 0.0).toFloat(),
                    y = f.optDouble("y", 0.0).toFloat(),
                    rotate = f.optDouble("rotate", 0.0).toFloat(),
                    scaleX = f.optDouble("scaleX", 1.0).toFloat(),
                    scaleY = f.optDouble("scaleY", 1.0).toFloat()
                )
            )
        }
        return frames
    }

    private fun parseTransform(obj: JSONObject?): DBTransform {
        if (obj == null) return DBTransform()
        return DBTransform(
            x = obj.optDouble("x", 0.0).toFloat(),
            y = obj.optDouble("y", 0.0).toFloat(),
            rotation = obj.optDouble("skX", 0.0).toFloat(),
            scaleX = obj.optDouble("scX", 1.0).toFloat(),
            scaleY = obj.optDouble("scY", 1.0).toFloat()
        )
    }
}
