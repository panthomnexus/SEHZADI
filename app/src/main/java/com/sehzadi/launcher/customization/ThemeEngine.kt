package com.sehzadi.launcher.customization

import androidx.compose.ui.graphics.Color
import com.sehzadi.launcher.storage.StorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class HudTheme(
    val id: String,
    val name: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val glowColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val textColor: Color
)

@Singleton
class ThemeEngine @Inject constructor(
    private val storageManager: StorageManager
) {
    private val themes = listOf(
        HudTheme(
            id = "neon_cyan",
            name = "Neon Cyan",
            primaryColor = Color(0xFF00F0FF),
            secondaryColor = Color(0xFF0066FF),
            accentColor = Color(0xFF00FF88),
            glowColor = Color(0xFF00F0FF),
            backgroundColor = Color(0xFF0A0A1A),
            surfaceColor = Color(0xFF111128),
            textColor = Color(0xFFE0E0FF)
        ),
        HudTheme(
            id = "neon_purple",
            name = "Neon Purple",
            primaryColor = Color(0xFF8B00FF),
            secondaryColor = Color(0xFFFF00AA),
            accentColor = Color(0xFF00F0FF),
            glowColor = Color(0xFF8B00FF),
            backgroundColor = Color(0xFF0A001A),
            surfaceColor = Color(0xFF1A0028),
            textColor = Color(0xFFE0D0FF)
        ),
        HudTheme(
            id = "neon_green",
            name = "Matrix Green",
            primaryColor = Color(0xFF00FF00),
            secondaryColor = Color(0xFF00CC00),
            accentColor = Color(0xFF88FF88),
            glowColor = Color(0xFF00FF00),
            backgroundColor = Color(0xFF000A00),
            surfaceColor = Color(0xFF001A00),
            textColor = Color(0xFFCCFFCC)
        ),
        HudTheme(
            id = "neon_red",
            name = "Iron Red",
            primaryColor = Color(0xFFFF0044),
            secondaryColor = Color(0xFFFF6600),
            accentColor = Color(0xFFFFAA00),
            glowColor = Color(0xFFFF0044),
            backgroundColor = Color(0xFF1A0000),
            surfaceColor = Color(0xFF280000),
            textColor = Color(0xFFFFCCCC)
        ),
        HudTheme(
            id = "neon_gold",
            name = "Gold Arc",
            primaryColor = Color(0xFFFFD700),
            secondaryColor = Color(0xFFFFA500),
            accentColor = Color(0xFFFFFF00),
            glowColor = Color(0xFFFFD700),
            backgroundColor = Color(0xFF1A1400),
            surfaceColor = Color(0xFF282000),
            textColor = Color(0xFFFFF8DC)
        )
    )

    private val _currentTheme = MutableStateFlow(themes.first())
    val currentTheme: StateFlow<HudTheme> = _currentTheme.asStateFlow()

    fun initialize() {
        val savedThemeId = storageManager.getTheme()
        _currentTheme.value = themes.find { it.id == savedThemeId } ?: themes.first()
    }

    fun setTheme(themeId: String) {
        val theme = themes.find { it.id == themeId } ?: return
        _currentTheme.value = theme
        storageManager.saveTheme(themeId)
    }

    fun getAvailableThemes(): List<HudTheme> = themes

    fun getCurrentTheme(): HudTheme = _currentTheme.value
}
