package com.sehzadi.launcher.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sehzadi.launcher.services.WidgetType
import com.sehzadi.launcher.system.SystemStats
import com.sehzadi.launcher.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun FloatingWidgetOverlay(
    widgetType: WidgetType,
    systemStats: SystemStats,
    notes: Map<String, Pair<String, String>>,
    onDismiss: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(200f) }

    val entryAnimation = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entryAnimation.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }

    val glowAlpha by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
    ) {
        Card(
            modifier = Modifier
                .width(200.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                DarkCard.copy(alpha = 0.95f),
                                DarkBackground.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .padding(1.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(15.dp))
                        .background(DarkCard.copy(alpha = 0.9f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                when (widgetType) {
                                    WidgetType.LIVE_CLOCK -> "CLOCK"
                                    WidgetType.SYSTEM_STATS -> "SYSTEM"
                                    WidgetType.NOTES -> "NOTES"
                                    WidgetType.NONE -> ""
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = OrbitronFont,
                                color = NeonCyan.copy(alpha = glowAlpha),
                                letterSpacing = 2.sp
                            )
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Close, "Close", tint = TextDim, modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        when (widgetType) {
                            WidgetType.LIVE_CLOCK -> LiveClockContent()
                            WidgetType.SYSTEM_STATS -> SystemStatsContent(systemStats)
                            WidgetType.NOTES -> NotesContent(notes)
                            WidgetType.NONE -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveClockContent() {
    var time by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            date = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(time, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = NeonCyan, fontFamily = JetBrainsMonoFont)
        Text(date, fontSize = 12.sp, fontFamily = RajdhaniFont, color = TextDim)
    }
}

@Composable
fun SystemStatsContent(stats: SystemStats) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        WidgetStatRow("BAT", "${stats.batteryLevel}%", stats.batteryLevel / 100f)
        WidgetStatRow("RAM", "${stats.ramUsagePercent.toInt()}%", stats.ramUsagePercent / 100f)
        WidgetStatRow("CPU", "${stats.cpuUsagePercent.toInt()}%", stats.cpuUsagePercent / 100f)
        WidgetStatRow("STO", "${stats.storageUsagePercent.toInt()}%", stats.storageUsagePercent / 100f)
    }
}

@Composable
fun WidgetStatRow(label: String, value: String, progress: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 9.sp, fontFamily = OrbitronFont, color = TextDim, modifier = Modifier.width(28.dp))
        LinearProgressIndicator(
            progress = progress.coerceIn(0f, 1f),
            modifier = Modifier
                .weight(1f)
                .height(3.dp),
            color = when {
                progress > 0.8f -> Color(0xFFFF4444)
                progress > 0.6f -> Color(0xFFFFAA00)
                else -> NeonCyan
            },
            trackColor = DarkBackground
        )
        Text(value, fontSize = 9.sp, fontFamily = JetBrainsMonoFont, color = NeonCyan, modifier = Modifier.width(32.dp))
    }
}

@Composable
fun NotesContent(notes: Map<String, Pair<String, String>>) {
    if (notes.isEmpty()) {
        Text("No notes", fontSize = 12.sp, fontFamily = RajdhaniFont, color = TextDim)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            notes.entries.take(3).forEach { (_, pair) ->
                Text(pair.first, fontSize = 11.sp, fontFamily = RajdhaniFont, fontWeight = FontWeight.Bold, color = TextWhite, maxLines = 1)
                Text(pair.second.take(40), fontSize = 9.sp, fontFamily = RajdhaniFont, color = TextDim, maxLines = 1)
            }
        }
    }
}
