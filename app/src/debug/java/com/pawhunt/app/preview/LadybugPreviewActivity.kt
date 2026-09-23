package com.pawhunt.app.preview

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.SurfaceView
import android.view.WindowManager
import android.view.View
import android.view.ContextThemeWrapper
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

/** Separate debug application: opening the APK immediately shows the model being reviewed. */
class LadybugPreviewActivity : Activity() {
    private var renderer: LadybugRenderer? = null
    private lateinit var status: TextView
    private lateinit var play: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.rgb(20, 25, 22)
        window.navigationBarColor = Color.rgb(20, 25, 22)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(20, 25, 22))
        }
        layout.addView(TextView(this).apply {
            text = "PawPlay · 小瓢虫"
            textSize = 22f
            setTextColor(Color.rgb(222, 234, 212))
            setPadding(dp(20), dp(18), dp(20), dp(6))
        })
        status = TextView(this).apply {
            text = "正在加载模型…"
            textSize = 12f
            setTextColor(Color.rgb(165, 185, 164))
            setPadding(dp(20), 0, dp(20), dp(12))
        }
        layout.addView(status)
        val surface = SurfaceView(this)
        layout.addView(surface, LinearLayout.LayoutParams(-1, 0, 1f))
        val controls = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(10), dp(8), 0)
        }
        fun button(label: String, action: () -> Unit): Button = Button(this).apply {
            text = label
            textSize = 12f
            isAllCaps = false
            setOnClickListener { action() }
            controls.addView(this, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
        button("俯视") { renderer?.mode = LadybugRenderer.Mode.FRONT }
        button("旋转观察") {
            renderer?.let { it.mode = LadybugRenderer.Mode.VOLUME; it.animated = true }
            play.text = "暂停"
        }
        button("参考尺寸") { renderer?.mode = LadybugRenderer.Mode.REFERENCE_SIZE }
        play = button("播放") {
            renderer?.let { it.animated = !it.animated; play.text = if (it.animated) "暂停" else "播放" }
        }
        layout.addView(controls)
        layout.addView(Spinner(ContextThemeWrapper(this, android.R.style.Theme_Material)).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item,
                listOf("自然变化", "缓慢开合", "短促连拍", "展翅停留", "轻收停顿"))
            var initialSelection = true
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (initialSelection) { initialSelection = false; return }
                    renderer?.selectMotion(listOf("auto", "gentle", "flutter", "spread", "settle")[position])
                    play.text = "暂停"
                }
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }, LinearLayout.LayoutParams(-1, dp(40)))
        val openingLabel = TextView(this).apply {
            text = "翅壳开合"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(dp(20), dp(10), 0, 0)
        }
        layout.addView(openingLabel)
        layout.addView(SeekBar(this).apply {
            max = 110
            progress = 26
            setPadding(dp(20), 0, dp(20), 0)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    renderer?.let { it.animated = false; it.opening = value / 2f }
                    play.text = "播放"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }, LinearLayout.LayoutParams(-1, dp(42)))
        layout.addView(TextView(this).apply {
            text = "头部与动作细化：可观察自然节奏，也可单独选择拍动方式。"
            textSize = 11f
            setTextColor(Color.rgb(137, 153, 140))
            setPadding(dp(20), dp(4), dp(20), dp(16))
        })
        setContentView(layout)
        try {
            renderer = LadybugRenderer(this, surface) { status.text = it }
        } catch (failure: Throwable) {
            Log.e("PawPlay3D", "Preview initialization failed", failure)
            status.text = "模型加载失败：${failure.message ?: failure.javaClass.simpleName}"
        }
    }

    override fun onResume() { super.onResume(); renderer?.start() }
    override fun onPause() { renderer?.stop(); super.onPause() }
    override fun onDestroy() { renderer?.close(); renderer = null; super.onDestroy() }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
