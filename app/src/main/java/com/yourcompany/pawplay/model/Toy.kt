package com.yourcompany.pawplay.model

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
        Toy(1, "Laser Dot", true, BehaviorType.RANDOM_CURVE, 4f, 80f, Color(0xFFFF1744), "🔴"),
        Toy(2, "Fish", true, BehaviorType.SWIMMING, 2f, 160f, Color(0xFF2979FF), "🐟"),
        Toy(3, "Cockroach", true, BehaviorType.CRAWLING, 2.5f, 140f, Color(0xFF6D4C41), "🪳"),
        Toy(4, "Butterfly", true, BehaviorType.FLYING, 1.8f, 180f, Color(0xFFAA00FF), "🦋"),
        Toy(5, "Mouse", false, BehaviorType.RUNNING, 3f, 150f, Color(0xFF78909C), "🐭"),
        Toy(6, "Spider", false, BehaviorType.DANGLING, 2f, 140f, Color(0xFF212121), "🕷️"),
        Toy(7, "Bee", false, BehaviorType.FLYING, 3.5f, 130f, Color(0xFFFFD600), "🐝"),
        Toy(8, "Feather", false, BehaviorType.DRIFTING, 1f, 150f, Color(0xFFE0E0E0), "🪶"),
        Toy(9, "Bird", false, BehaviorType.FLYING, 3.8f, 140f, Color(0xFF00BFA5), "🐦"),
        Toy(10, "Yarn Ball", false, BehaviorType.BOUNCING, 2.5f, 130f, Color(0xFFFF6D00), "🧶")
    )

    val freeToys get() = allToys.filter { it.isFree }
}
