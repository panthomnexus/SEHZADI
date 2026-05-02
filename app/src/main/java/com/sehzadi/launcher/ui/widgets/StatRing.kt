package com.sehzadi.launcher.ui.widgets

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sehzadi.launcher.ui.theme.NeonCyan
import com.sehzadi.launcher.ui.theme.NeonGreen
import com.sehzadi.launcher.ui.theme.TextDim
import com.sehzadi.launcher.ui.theme.TextWhite

@Composable
fun StatRing(
    label: String,
    value: String,
    progress: Float,
    color: Color = NeonCyan,
    size: Dp = 80.dp,
    strokeWidth: Dp = 6.dp,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "statProgress"
    )

    val glowAlpha by rememberInfiniteTransition(label = "ringGlow").animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringAlpha"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ringSize = Size(this.size.width, this.size.height)
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f

            // Background ring
            drawArc(
                color = color.copy(alpha = 0.1f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(ringSize.width - strokePx, ringSize.height - strokePx),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Progress ring
            drawArc(
                color = color.copy(alpha = glowAlpha),
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(ringSize.width - strokePx, ringSize.height - strokePx),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Text(
                label,
                fontSize = 9.sp,
                color = TextDim
            )
        }
    }
}

@Composable
fun StatRingRow(
    batteryPct: Int,
    ramUsedMb: Long,
    ramTotalMb: Long,
    networkStatus: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatRing(
            label = "Battery",
            value = "$batteryPct%",
            progress = batteryPct / 100f,
            color = when {
                batteryPct <= 15 -> Color(0xFFFF4444)
                batteryPct <= 30 -> Color(0xFFFFAA00)
                else -> NeonGreen
            }
        )
        StatRing(
            label = "RAM",
            value = "${ramUsedMb / 1024}/${ramTotalMb / 1024}G",
            progress = if (ramTotalMb > 0) ramUsedMb.toFloat() / ramTotalMb else 0f,
            color = NeonCyan
        )
        StatRing(
            label = "Network",
            value = networkStatus,
            progress = if (networkStatus == "Online") 1f else 0f,
            color = if (networkStatus == "Online") NeonGreen else Color(0xFFFF4444)
        )
    }
}
