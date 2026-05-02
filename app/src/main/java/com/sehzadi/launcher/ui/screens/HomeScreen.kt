package com.sehzadi.launcher.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.sehzadi.launcher.apps.AppInfo
import com.sehzadi.launcher.system.SystemStats
import com.sehzadi.launcher.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onAppDrawerOpen: () -> Unit,
    systemStats: SystemStats
) {
    val apps by viewModel.installedApps.collectAsState()

    val currentTime = remember { mutableStateOf("") }
    val currentDate = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            currentTime.value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)
            currentDate.value = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(now.time)
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // Clock
        Text(
            text = currentTime.value,
            fontSize = 72.sp,
            fontWeight = FontWeight.Thin,
            color = NeonCyan,
            letterSpacing = 4.sp
        )
        Text(
            text = currentDate.value,
            fontSize = 16.sp,
            color = TextDim,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick stats bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = DarkCard.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickStat(
                icon = Icons.Default.BatteryChargingFull,
                value = "${systemStats.batteryLevel}%",
                color = if (systemStats.batteryLevel > 20) NeonGreen else Color(0xFFFF4444)
            )
            QuickStat(
                icon = Icons.Default.Memory,
                value = "${systemStats.ramUsagePercent.toInt()}%",
                color = NeonCyan
            )
            QuickStat(
                icon = Icons.Default.Wifi,
                value = systemStats.networkType,
                color = if (systemStats.isNetworkConnected) NeonBlue else Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Frequent apps (top 8)
        val frequentApps = apps.filter { !it.isHidden }.take(8)
        if (frequentApps.isNotEmpty()) {
            Text(
                text = "APPS",
                fontSize = 11.sp,
                color = TextDim,
                letterSpacing = 3.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(frequentApps) { app ->
                    AppIcon(
                        app = app,
                        onClick = { viewModel.launchApp(app.packageName) }
                    )
                }
            }
        }

        // Swipe up hint
        Column(
            modifier = Modifier.padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = "Swipe up",
                tint = TextDim.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Swipe up for all apps",
                fontSize = 10.sp,
                color = TextDim.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun QuickStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AppIcon(
    app: AppInfo,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DarkCard),
            contentAlignment = Alignment.Center
        ) {
            app.icon?.let { drawable ->
                Image(
                    bitmap = drawable.toBitmap(48, 48).asImageBitmap(),
                    contentDescription = app.appName,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.appName,
            fontSize = 10.sp,
            color = TextWhite.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(60.dp)
        )
    }
}
