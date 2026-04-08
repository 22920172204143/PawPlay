/**
 * @file Slot.kt
 * @brief Runtime slot that binds a texture region to a bone for rendering.
 */
package com.yourcompany.pawplay.game.dragonbones

class Slot(
    val name: String,
    val bone: Bone,
    val zOrder: Int
) {
    var displayName: String? = null
    var displayTransform: DBTransform = DBTransform()
    var visible: Boolean = true
}
