package com.sehzadi.launcher.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val NeonCyan = Color(0xFF00F0FF)
val NeonBlue = Color(0xFF0066FF)
val NeonPurple = Color(0xFF8B00FF)
val NeonPink = Color(0xFFFF00AA)
val NeonGreen = Color(0xFF00FF88)
val DarkBackground = Color(0xFF0A0A1A)
val DarkSurface = Color(0xFF111128)
val DarkCard = Color(0xFF1A1A3E)
val TextWhite = Color(0xFFE0E0FF)
val TextDim = Color(0xFF8888AA)

private val SehzadiColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = NeonBlue,
    tertiary = NeonPurple,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkCard,
    onPrimary = DarkBackground,
    onSecondary = DarkBackground,
    onTertiary = DarkBackground,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = TextDim,
    error = Color(0xFFFF4444),
    outline = NeonCyan.copy(alpha = 0.3f)
)

@Composable
fun SehzadiTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBackground.toArgb()
            window.navigationBarColor = DarkBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = SehzadiColorScheme,
        content = content
    )
}
