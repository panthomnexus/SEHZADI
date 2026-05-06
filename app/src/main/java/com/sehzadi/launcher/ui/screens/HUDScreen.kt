package com.sehzadi.launcher.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sehzadi.launcher.customization.HudTheme
import com.sehzadi.launcher.system.SystemStats
import com.sehzadi.launcher.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HUDScreen(
    systemStats: SystemStats,
    theme: HudTheme
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val currentTime = remember { mutableStateOf("") }
    val currentDate = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            currentTime.value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now.time)
            currentDate.value = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(now.time)
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Time display
        Text(
            text = currentTime.value,
            fontSize = 48.sp,
            fontWeight = FontWeight.Thin,
            color = theme.primaryColor,
            letterSpacing = 8.sp
        )

        Text(
            text = currentDate.value,
            fontSize = 14.sp,
            color = theme.textColor.copy(alpha = 0.7f),
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Central HUD Circle
        Box(
            modifier = Modifier.size(280.dp),
            contentAlignment = Alignment.Center
        ) {
            // Animated rings
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawHUDRings(rotation, pulseAlpha, theme)
            }

            // Center stats
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SEHZADI",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.primaryColor,
                    letterSpacing = 4.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${systemStats.batteryLevel}%",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Light,
                    color = if (systemStats.batteryLevel > 20) theme.primaryColor else Color(0xFFFF4444)
                )
                Text(
                    text = if (systemStats.isCharging) "CHARGING" else "BATTERY",
                    fontSize = 10.sp,
                    color = theme.textColor.copy(alpha = 0.5f),
                    letterSpacing = 3.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HUDStatCard(
                icon = Icons.Default.Memory,
                label = "RAM",
                value = "${systemStats.ramUsagePercent.toInt()}%",
                subValue = "${systemStats.ramUsedMB}/${systemStats.ramTotalMB} MB",
                progress = systemStats.ramUsagePercent / 100f,
                theme = theme,
                modifier = Modifier.weight(1f)
            )
            HUDStatCard(
                icon = Icons.Default.Speed,
                label = "CPU",
                value = "${systemStats.cpuUsagePercent.toInt()}%",
                subValue = "Processing",
                progress = systemStats.cpuUsagePercent / 100f,
                theme = theme,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HUDStatCard(
                icon = Icons.Default.Storage,
                label = "STORAGE",
                value = "${systemStats.storageUsagePercent.toInt()}%",
                subValue = "%.1f/%.1f GB".format(systemStats.storageUsedGB, systemStats.storageTotalGB),
                progress = systemStats.storageUsagePercent / 100f,
                theme = theme,
                modifier = Modifier.weight(1f)
            )
            HUDStatCard(
                icon = Icons.Default.NetworkCheck,
                label = "NETWORK",
                value = systemStats.networkType,
                subValue = "%.0f KB/s".format(systemStats.downloadSpeedKbps),
                progress = if (systemStats.isNetworkConnected) 1f else 0f,
                theme = theme,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Network speed detail
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = theme.surfaceColor.copy(alpha = 0.8f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ArrowDownward, "Download", tint = theme.accentColor, modifier = Modifier.size(20.dp))
                    Text("%.1f KB/s".format(systemStats.downloadSpeedKbps), color = theme.textColor, fontSize = 14.sp)
                    Text("Download", color = theme.textColor.copy(alpha = 0.5f), fontSize = 10.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ArrowUpward, "Upload", tint = theme.secondaryColor, modifier = Modifier.size(20.dp))
                    Text("%.1f KB/s".format(systemStats.uploadSpeedKbps), color = theme.textColor, fontSize = 14.sp)
                    Text("Upload", color = theme.textColor.copy(alpha = 0.5f), fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun HUDStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    subValue: String,
    progress: Float,
    theme: HudTheme,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = theme.surfaceColor.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = theme.primaryColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = theme.textColor.copy(alpha = 0.5f),
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = theme.textColor
            )
            Text(
                text = subValue,
                fontSize = 10.sp,
                color = theme.textColor.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = when {
                    progress > 0.8f -> Color(0xFFFF4444)
                    progress > 0.6f -> Color(0xFFFFAA00)
                    else -> theme.primaryColor
                },
                trackColor = theme.backgroundColor
            )
        }
    }
}

fun DrawScope.drawHUDRings(rotation: Float, pulseAlpha: Float, theme: HudTheme) {
    val center = Offset(size.width / 2, size.height / 2)
    val maxRadius = size.minDimension / 2

    // Outer ring - rotating
    rotate(rotation) {
        drawArc(
            color = theme.primaryColor.copy(alpha = pulseAlpha * 0.5f),
            startAngle = 0f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
            size = Size(maxRadius * 2, maxRadius * 2),
            style = Stroke(width = 2f)
        )
    }

    // Second ring - counter rotating
    rotate(-rotation * 0.7f) {
        drawArc(
            color = theme.secondaryColor.copy(alpha = pulseAlpha * 0.4f),
            startAngle = 45f,
            sweepAngle = 200f,
            useCenter = false,
            topLeft = Offset(center.x - maxRadius * 0.85f, center.y - maxRadius * 0.85f),
            size = Size(maxRadius * 1.7f, maxRadius * 1.7f),
            style = Stroke(width = 1.5f)
        )
    }

    // Inner ring - slow rotation
    rotate(rotation * 0.3f) {
        drawArc(
            color = theme.accentColor.copy(alpha = pulseAlpha * 0.3f),
            startAngle = 90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(center.x - maxRadius * 0.7f, center.y - maxRadius * 0.7f),
            size = Size(maxRadius * 1.4f, maxRadius * 1.4f),
            style = Stroke(width = 1f)
        )
    }

    // Center glow circle
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                theme.primaryColor.copy(alpha = pulseAlpha * 0.15f),
                Color.Transparent
            ),
            center = center,
            radius = maxRadius * 0.5f
        ),
        radius = maxRadius * 0.5f,
        center = center
    )

    // Static circle border
    drawCircle(
        color = theme.primaryColor.copy(alpha = 0.2f),
        radius = maxRadius * 0.55f,
        center = center,
        style = Stroke(width = 1f)
    )
}
