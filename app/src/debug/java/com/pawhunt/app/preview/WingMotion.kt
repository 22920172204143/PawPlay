package com.pawhunt.app.preview

import kotlin.math.floor
import kotlin.math.sin

/** Fixed-step motion shared with the browser preview; no rendering or Android dependencies. */
class WingMotion(private val config: Config, rest: Double = 13.0) {
    data class Mode(val id: String, val label: String, val duration: Pair<Double, Double>,
                    val frequency: Pair<Double, Double>, val center: Pair<Double, Double>,
                    val amplitude: Pair<Double, Double>, val openFraction: Double,
                    val crestHold: Double, val asymmetry: Double)
    data class Config(val seed: Long, val transitionSeconds: Double, val maxOpening: Double,
                      val modes: List<Mode>)
    class Pose(var left: Double, var right: Double, var antennaLeft: Double = 0.0,
               var antennaRight: Double = 0.0, var label: String = "")

    companion object {
        private const val STEP = 1.0 / 120
        private fun mix(a: Double, b: Double, t: Double) = a + (b - a) * t
        private fun smooth(value: Double): Double {
            val x = value.coerceIn(0.0, 1.0)
            return x * x * (3 - 2 * x)
        }
        private fun stroke(phase: Double, open: Double, hold: Double): Double {
            val p = phase - floor(phase)
            val end = open + hold
            return when {
                p < open -> smooth(p / open) * 2 - 1
                p < end -> 1.0
                else -> 1 - smooth((p - end) / (1 - end)) * 2
            }
        }
    }

    private var randomState = config.seed
    private var selected = "auto"
    private var active = -1
    private var age = 0.0
    private var duration = 0.0
    private var phase = 0.0
    private var time = 0.0
    private var accumulator = 0.0
    private var startFrequency = 1.0
    private var endFrequency = 1.0
    // frequency, center, amplitude, opening fraction, crest hold, left/right phase skew.
    private val values = doubleArrayOf(1.0, rest, 0.0, .5, .08, 0.0)
    private val from = values.copyOf()
    private val to = values.copyOf()
    val pose = Pose(rest, rest)

    init { choose(0) }

    private fun random(): Double {
        randomState = randomState * 48271 % 2147483647
        return randomState.toDouble() / 2147483647
    }
    private fun range(pair: Pair<Double, Double>) = mix(pair.first, pair.second, random())
    private fun choose(index: Int) {
        active = index; age = 0.0
        values.copyInto(from)
        val mode = config.modes[index]
        duration = range(mode.duration)
        startFrequency = range(mode.frequency); endFrequency = range(mode.frequency)
        to[0] = startFrequency; to[1] = range(mode.center); to[2] = range(mode.amplitude)
        to[3] = mode.openFraction; to[4] = mode.crestHold; to[5] = mode.asymmetry
        pose.label = mode.label
    }

    fun select(id: String) {
        val index = config.modes.indexOfFirst { it.id == id }
        require(id == "auto" || index >= 0) { "Unknown motion: $id" }
        selected = id; choose(if (id == "auto") 0 else index)
    }
    fun setRestOpening(degrees: Double) {
        val opening = degrees.coerceIn(0.0, config.maxOpening)
        values[0] = 1.0; values[1] = opening; values[2] = 0.0
        values[3] = .5; values[4] = .08; values[5] = 0.0
        phase = 0.0; accumulator = 0.0
        pose.left = opening; pose.right = opening; pose.antennaLeft = 0.0; pose.antennaRight = 0.0
        choose(if (selected == "auto") 0 else config.modes.indexOfFirst { it.id == selected })
    }
    fun advance(seconds: Double): Pose {
        require(seconds.isFinite() && seconds >= 0) { "Invalid delta" }
        accumulator += seconds.coerceAtMost(1.0)
        while (accumulator + 1e-10 >= STEP) { tick(); accumulator -= STEP }
        return pose
    }
    private fun tick() {
        if (age >= duration) {
            var next = active
            if (selected == "auto") {
                next = (random() * (config.modes.size - 1)).toInt()
                if (next >= active) next++
            }
            choose(next)
        }
        age += STEP; time += STEP
        to[0] = mix(startFrequency, endFrequency, smooth(age / duration))
        val blend = smooth(age / config.transitionSeconds)
        for (i in values.indices) values[i] = mix(from[i], to[i], blend)
        val v = values
        phase = (phase + v[0] * STEP) % 1
        val amplitude = v[2] * (.95 + .05 * sin(time * 1.73))
        val skew = sin(time * .87) * v[5]
        pose.left = (v[1] + amplitude * stroke(phase, v[3], v[4])).coerceIn(0.0, config.maxOpening)
        pose.right = (v[1] + amplitude * (.95 + .05 * sin(time * .61)) * stroke(phase + skew, v[3], v[4])).coerceIn(0.0, config.maxOpening)
        val antennaStrength = (amplitude / 2).coerceIn(0.0, 1.0)
        pose.antennaLeft = sin(time * 2.1) * 1.7 * antennaStrength
        pose.antennaRight = sin(time * 1.7 + .8) * 1.2 * antennaStrength
    }
}
