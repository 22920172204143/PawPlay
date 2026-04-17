package com.pawhunt.app.model

import androidx.compose.ui.graphics.Color

enum class BehaviorType {
    RANDOM_CURVE,
    SWIMMING,
    CRAWLING,
    FLYING,
    RUNNING,
    DANGLING,
    DRIFTING,
    BOUNCING
}

data class Toy(
    val id: Int,
    val name: String,
    val isFree: Boolean,
    val behaviorType: BehaviorType,
    val baseSpeed: Float,
    val displaySize: Float,
    val primaryColor: Color,
    val emoji: String
)

object ToyLibrary {

    val allToys = listOf(
        Toy(1, "Ladybug", true, BehaviorType.CRAWLING, 4.5f, 400f, Color(0xFFFF2070), "🐞"),
        Toy(2, "Fish", true, BehaviorType.SWIMMING, 4.8f, 380f, Color(0xFF2979FF), "🐟"),
        Toy(3, "Cockroach", true, BehaviorType.CRAWLING, 5.5f, 360f, Color(0xFF6D4C41), "🪳"),
        Toy(4, "Butterfly", true, BehaviorType.FLYING, 3.8f, 420f, Color(0xFFAA00FF), "🦋"),
        Toy(5, "Mouse", true, BehaviorType.RUNNING, 5f, 360f, Color(0xFF78909C), "🐭"),
        Toy(6, "Spider", true, BehaviorType.DANGLING, 4f, 360f, Color(0xFF212121), "🕷️"),
        Toy(7, "Bee", true, BehaviorType.FLYING, 5.5f, 340f, Color(0xFFFFD600), "🐝"),
        Toy(9, "Bird", true, BehaviorType.FLYING, 6f, 360f, Color(0xFF00BFA5), "🐦")
    )

    val freeToys get() = allToys.filter { it.isFree }
}
