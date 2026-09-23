package com.pawhunt.app.preview

import android.content.Context
import android.opengl.Matrix
import android.util.Log
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceView
import com.google.android.filament.Camera
import com.google.android.filament.ColorGrading
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.Renderer
import com.google.android.filament.SwapChain
import com.google.android.filament.ToneMapper
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** All Filament ownership and frame updates stay on the Activity's main thread. */
class LadybugRenderer(
    private val context: Context,
    surface: SurfaceView,
    private val onStatus: (String) -> Unit
) : Choreographer.FrameCallback, AutoCloseable {
    enum class Mode { FRONT, VOLUME, REFERENCE_SIZE }

    companion object {
        private const val TAG = "PawPlay3D"
        init { Gltfio.init() }
    }

    private val engine = Engine.create()
    private val renderer = engine.createRenderer()
    private val scene = engine.createScene()
    private val cameraEntity = EntityManager.get().create()
    private val camera = engine.createCamera(cameraEntity)
    private val view = engine.createView()
    private val grading = ColorGrading.Builder().toneMapper(ToneMapper.Linear()).build(engine)
    private val provider = UbershaderProvider(engine)
    private val assetLoader = AssetLoader(engine, provider, EntityManager.get())
    private val resourceLoader = ResourceLoader(engine)
    private val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
    private val choreographer = Choreographer.getInstance()
    private val assets = mutableListOf<FilamentAsset>()
    private var swapChain: SwapChain? = null
    private var running = false
    private var closed = false
    private var lastFrame = 0L
    private var elapsed = 0.0
    private var aspect = 1.0
    private var statsStart = 0L
    private var renderedFrames = 0
    private var joints = emptyList<Joint>()
    private var restOpening = 13f
    private lateinit var motion: WingMotion
    private val matrix = FloatArray(16)

    var mode = Mode.FRONT
    var animated = false
    var opening = 13f
        set(value) {
            field = value
            if (!animated && ::motion.isInitialized) motion.setRestOpening(value.toDouble())
        }

    fun selectMotion(id: String) { motion.select(id); animated = true }

    private data class Joint(val name: String, val instance: Int, val base: FloatArray)

    init {
        try {
            val profile = context.assets.open("profiles/reference_ladybug.json").bufferedReader().use {
                JSONObject(it.readText())
            }
            restOpening = profile.getDouble("shell_open_degrees").toFloat()
            opening = restOpening
            val motionProfile = profile.getJSONObject("motion")
            fun pair(json: JSONObject, key: String): Pair<Double, Double> {
                val values = json.getJSONArray(key)
                return values.getDouble(0) to values.getDouble(1)
            }
            val modes = motionProfile.getJSONArray("modes")
            motion = WingMotion(WingMotion.Config(
                motionProfile.getLong("seed"), motionProfile.getDouble("transition_seconds"),
                motionProfile.getDouble("max_open_degrees"),
                List(modes.length()) { i ->
                    val m = modes.getJSONObject(i)
                    WingMotion.Mode(m.getString("id"), m.getString("label"), pair(m, "duration"),
                        pair(m, "frequency"), pair(m, "center"), pair(m, "amplitude"),
                        m.getDouble("open_fraction"), m.getDouble("crest_hold"), m.getDouble("asymmetry"))
                }), restOpening.toDouble())
            camera.setExposure(1f)
            view.scene = scene
            view.camera = camera
            view.colorGrading = grading
            view.antiAliasing = View.AntiAliasing.FXAA
            renderer.clearOptions = Renderer.ClearOptions().apply {
                clear = true
                clearColor = floatArrayOf(.012f, .018f, .014f, 1f)
            }
            loadAsset("models/reference_stage.glb")
            val bug = loadAsset("models/reference_ladybug.glb")
            val names = listOf("shell_left_hinge", "shell_right_hinge", "antenna_left_pivot", "antenna_right_pivot")
            joints = names.map { name ->
                val entity = bug.entities.firstOrNull { bug.getName(it) == name }
                    ?: error("Missing model node: $name")
                val instance = engine.transformManager.getInstance(entity)
                val base = FloatArray(16)
                engine.transformManager.getTransform(instance, base)
                Joint(name, instance, base)
            }
            uiHelper.renderCallback = object : UiHelper.RendererCallback {
                override fun onNativeWindowChanged(surface: Surface) {
                    swapChain?.let { engine.destroySwapChain(it) }
                    swapChain = engine.createSwapChain(surface, uiHelper.swapChainFlags)
                }
                override fun onDetachedFromSurface() {
                    swapChain?.let {
                        engine.destroySwapChain(it)
                        engine.flushAndWait()
                        swapChain = null
                    }
                }
                override fun onResized(width: Int, height: Int) {
                    if (width == 0 || height == 0) return
                    engine.flushAndWait()
                    aspect = width.toDouble() / height
                    view.viewport = Viewport(0, 0, width, height)
                    updateCamera()
                }
            }
            uiHelper.attachTo(surface)
            Log.i(TAG, "model_loaded revision=${profile.getInt("revision")} parts=${bug.entities.size}")
            onStatus("模型已加载 · 头部与动作细化")
        } catch (failure: Throwable) {
            close()
            throw failure
        }
    }

    private fun loadAsset(path: String): FilamentAsset {
        val bytes = context.assets.open(path).use { it.readBytes() }
        val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
        buffer.put(bytes).rewind()
        val asset = assetLoader.createAsset(buffer) ?: error("Cannot load $path")
        assets.add(asset)
        resourceLoader.loadResources(asset)
        scene.addEntities(asset.entities)
        asset.releaseSourceData()
        return asset
    }

    fun start() {
        if (running || closed) return
        running = true
        lastFrame = 0L
        statsStart = 0L
        renderedFrames = 0
        choreographer.postFrameCallback(this)
    }

    fun stop() {
        running = false
        choreographer.removeFrameCallback(this)
        lastFrame = 0L
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running || closed) return
        choreographer.postFrameCallback(this)
        val delta = if (lastFrame == 0L) 0.0 else ((frameTimeNanos - lastFrame) / 1e9).coerceAtMost(.05)
        lastFrame = frameTimeNanos
        if (animated) {
            elapsed += delta
            val pose = motion.advance(delta)
            opening = ((pose.left + pose.right) / 2).toFloat()
        }
        for (joint in joints) {
            val degrees = when (joint.name) {
                "shell_left_hinge" -> -(motion.pose.left.toFloat() - restOpening)
                "shell_right_hinge" -> motion.pose.right.toFloat() - restOpening
                "antenna_left_pivot" -> motion.pose.antennaLeft.toFloat()
                else -> motion.pose.antennaRight.toFloat()
            }
            joint.base.copyInto(matrix)
            Matrix.rotateM(matrix, 0, degrees, 0f, 1f, 0f)
            engine.transformManager.setTransform(joint.instance, matrix)
        }
        updateCamera()
        val chain = swapChain ?: return
        if (uiHelper.isReadyToRender && renderer.beginFrame(chain, frameTimeNanos)) {
            renderer.render(view)
            renderer.endFrame()
            renderedFrames++
        }
        if (statsStart == 0L) statsStart = frameTimeNanos
        val statsSeconds = (frameTimeNanos - statsStart) / 1e9
        if (statsSeconds >= 1.0) {
            val fps = (renderedFrames / statsSeconds).toInt()
            onStatus("${if (animated) motion.pose.label else "动作已暂停"} · $fps 帧/秒 · 尚待视觉验收")
            statsStart = frameTimeNanos
            renderedFrames = 0
        }
    }

    private fun updateCamera() {
        val extent = if (mode == Mode.REFERENCE_SIZE) 4.58 else 1.30
        val halfW = extent * max(1.0, aspect)
        val halfH = extent * max(1.0, 1.0 / aspect)
        camera.setProjection(Camera.Projection.ORTHO, -halfW, halfW, -halfH, halfH, .01, 100.0)
        if (mode == Mode.VOLUME) {
            val angle = elapsed * .55
            camera.lookAt(sin(angle) * 3, 3.2, cos(angle) * 3, 0.0, .08, 0.0, 0.0, 1.0, 0.0)
        } else {
            camera.lookAt(0.0, 5.0, .00001, 0.0, .08, 0.0, 0.0, 0.0, -1.0)
        }
    }

    override fun close() {
        if (closed) return
        stop()
        uiHelper.detach()
        for (asset in assets) {
            scene.removeEntities(asset.entities)
            assetLoader.destroyAsset(asset)
        }
        assets.clear()
        resourceLoader.destroy()
        assetLoader.destroy()
        provider.destroyMaterials()
        provider.destroy()
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroyCameraComponent(cameraEntity)
        EntityManager.get().destroy(cameraEntity)
        engine.destroyColorGrading(grading)
        engine.destroyRenderer(renderer)
        engine.destroy()
        closed = true
        Log.i(TAG, "renderer_closed")
    }
}
