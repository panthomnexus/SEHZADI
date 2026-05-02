package com.sehzadi.launcher.ui.widgets

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sehzadi.launcher.ui.theme.DarkCard
import com.sehzadi.launcher.ui.theme.NeonCyan

@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    glowColor: Color = NeonCyan,
    cornerRadius: Dp = 16.dp,
    enableGlow: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val glowAlpha by rememberInfiniteTransition(label = "neonGlow").animateFloat(
        initialValue = 0.15f,
        targetValue = if (enableGlow) 0.4f else 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        glowColor.copy(alpha = glowAlpha),
                        glowColor.copy(alpha = glowAlpha * 0.3f),
                        glowColor.copy(alpha = glowAlpha * 0.6f)
                    )
                ),
                shape = shape
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkCard.copy(alpha = 0.9f),
                        DarkCard.copy(alpha = 0.7f)
                    )
                )
            )
            .padding(16.dp),
        content = content
    )
}

@Composable
fun GlassmorphismCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    Surface(
        modifier = modifier.clip(shape),
        color = Color(0xFF111128).copy(alpha = 0.7f),
        tonalElevation = 4.dp,
        shape = shape
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp),
            content = content
        )
    }
}
