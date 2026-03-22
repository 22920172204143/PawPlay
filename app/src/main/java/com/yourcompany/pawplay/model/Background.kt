package com.yourcompany.pawplay.model

import androidx.compose.ui.graphics.Color

data class GameBackground(
    val id: Int,
    val name: String,
    val isFree: Boolean,
    val baseColor: Color,
    val accentColor: Color
)

object BackgroundLibrary {

    val allBackgrounds = listOf(
        GameBackground(1, "Grass", true, Color(0xFF4CAF50), Color(0xFF388E3C)),
        GameBackground(2, "Wood Floor", true, Color(0xFF8D6E63), Color(0xFF6D4C41)),
        GameBackground(3, "Water", false, Color(0xFF0288D1), Color(0xFF01579B)),
        GameBackground(4, "Night Sky", false, Color(0xFF1A237E), Color(0xFF0D47A1)),
        GameBackground(5, "City Floor", false, Color(0xFF616161), Color(0xFF424242)),
        GameBackground(6, "Pink Blanket", false, Color(0xFFF48FB1), Color(0xFFEC407A))
    )

    val freeBackgrounds get() = allBackgrounds.filter { it.isFree }
}
