package com.example.myapplication.ui.theme

import androidx.compose.ui.graphics.Color

data class AppThemeColors(
    val background: Color,
    val surface: Color,
    val cardBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val primaryButtonBg: Color,
    val primaryButtonText: Color
)

fun getThemeColors(isDarkMode: Boolean): AppThemeColors {
    return if (isDarkMode) {
        AppThemeColors(
            background = Color.Black,
            surface = Color(0xFF121212),
            cardBackground = Color.Black,
            textPrimary = Color.White,
            textSecondary = Color(0xFFAAAAAA),
            border = Color(0xFF444444),
            primaryButtonBg = Color.White,
            primaryButtonText = Color.Black
        )
    } else {
        AppThemeColors(
            background = Color(0xFFF2F5F9),
            surface = Color.White,
            cardBackground = Color.White,
            textPrimary = Color.Black,
            textSecondary = Color(0xFF555555),
            border = Color(0xFFC5CEDA),
            primaryButtonBg = Color.Black,
            primaryButtonText = Color.White
        )
    }
}
