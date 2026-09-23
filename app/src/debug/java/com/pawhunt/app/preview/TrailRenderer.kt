package com.pawhunt.app.preview

import android.opengl.Matrix
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.gltfio.FilamentAsset

/** Camera-facing vector meshes; positions remain in the scene, outside the bug rig. */
class TrailRenderer(private val engine: Engine, asset: FilamentAsset, capacity: Int, styles: Set<String>) {
    private data class Slot(val transform: Int, val material: MaterialInstance, val style: String, val index: Int)
    private val billboard=FloatArray(16)
    private val transform=FloatArray(16)
    private val slots=styles.flatMap { style -> (0 until capacity).map { index ->
        val name="trail_${style}_${index.toString().padStart(2,'0')}"
        val entity=asset.entities.firstOrNull { asset.getName(it)==name } ?: error("Missing trail node: $name")
        Slot(engine.transformManager.getInstance(entity),
            engine.renderableManager.getMaterialInstanceAt(engine.renderableManager.getInstance(entity),0),style,index)
    } }

    fun update(trail: BugTrail, camera: Camera) {
        camera.getModelMatrix(billboard)
        for(slot in slots) {
            val p=trail.particles[slot.index]
            if(!p.active || p.style!=slot.style) {
                Matrix.setIdentityM(transform,0)
                Matrix.scaleM(transform,0,0f,0f,0f)
            } else {
                billboard.copyInto(transform)
                transform[12]=p.x.toFloat();transform[13]=p.y.toFloat();transform[14]=p.z.toFloat()
                Matrix.rotateM(transform,0,Math.toDegrees(p.angle).toFloat(),0f,0f,1f)
                val size=p.size.toFloat();Matrix.scaleM(transform,0,size,size,size)
                slot.material.setParameter("baseColorFactor",p.color[0].toFloat(),p.color[1].toFloat(),p.color[2].toFloat(),p.alpha.toFloat())
            }
            engine.transformManager.setTransform(slot.transform,transform)
        }
    }
}
