package com.yourcompany.pawplay.game

class GameLoop(private val renderer: GameRenderer) : Thread("GameLoop") {

    @Volatile
    var running = false

    private val targetFps = 60L
    private val targetNanos = 1_000_000_000L / targetFps

    override fun run() {
        var lastTime = System.nanoTime()

        while (running) {
            val now = System.nanoTime()
            val deltaNanos = now - lastTime
            lastTime = now

            val deltaSeconds = (deltaNanos / 1_000_000_000f).coerceAtMost(0.05f)

            try {
                renderer.update(deltaSeconds)
                renderer.draw()
            } catch (_: Exception) {
                // skip frame on transient errors
            }

            val elapsed = System.nanoTime() - now
            val sleepNanos = targetNanos - elapsed
            if (sleepNanos > 0) {
                try {
                    sleep(sleepNanos / 1_000_000, (sleepNanos % 1_000_000).toInt())
                } catch (_: InterruptedException) {
                    break
                }
            }
        }
    }

    fun startLoop() {
        running = true
        start()
    }

    fun stopLoop() {
        running = false
        try {
            join(500)
        } catch (_: InterruptedException) {
            // ignored
        }
    }
}
