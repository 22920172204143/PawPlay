/**
 * @file DBAtlasParser.kt
 * @brief Parses DragonBones _tex.json texture atlas into DBAtlasData.
 */
package com.pawhunt.app.game.dragonbones

import org.json.JSONObject

object DBAtlasParser {

    /**
     * @brief Parse a DragonBones texture atlas JSON string
     * @param[in] json raw JSON content of _tex.json
     * @return DBAtlasData with region mapping
     */
    fun parse(json: String): DBAtlasData {
        val root = JSONObject(json)
        val name = root.optString("name", "")
        val imagePath = root.optString("imagePath", "")
        val width = root.optInt("width", 0)
        val height = root.optInt("height", 0)

        val regions = mutableMapOf<String, DBAtlasRegion>()
        val subArr = root.optJSONArray("SubTexture")
        if (subArr != null) {
            for (i in 0 until subArr.length()) {
                val s = subArr.getJSONObject(i)
                val rName = s.getString("name")
                regions[rName] = DBAtlasRegion(
                    name = rName,
                    x = s.optInt("x", 0),
                    y = s.optInt("y", 0),
                    width = s.optInt("width", 0),
                    height = s.optInt("height", 0),
                    frameX = s.optInt("frameX", 0),
                    frameY = s.optInt("frameY", 0),
                    frameWidth = s.optInt("frameWidth", 0),
                    frameHeight = s.optInt("frameHeight", 0)
                )
            }
        }

        return DBAtlasData(name, imagePath, width, height, regions)
    }
}
