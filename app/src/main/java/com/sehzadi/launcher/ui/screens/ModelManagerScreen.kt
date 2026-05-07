package com.sehzadi.launcher.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sehzadi.launcher.ai.models.*
import com.sehzadi.launcher.ui.theme.*

@Composable
fun ModelManagerScreen(
    models: List<ModelState>,
    deviceCapability: DeviceCapability?,
    activeModel: ModelState?,
    onDownload: (String) -> Unit,
    onLoad: (String) -> Unit,
    onUnload: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val entryAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entryAlpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(alpha = entryAlpha.value)
            .background(
                Brush.verticalGradient(
                    listOf(DarkBackground, Color(0xFF050510))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = NeonCyan)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "AI MODEL MANAGER",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OrbitronFont,
                        color = NeonCyan,
                        letterSpacing = 3.sp
                    )
                    Text(
                        activeModel?.let { "Active: ${it.model.name}" } ?: "No model loaded",
                        fontSize = 12.sp,
                        fontFamily = RajdhaniFont,
                        color = if (activeModel != null) NeonGreen else TextDim
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Device Info Card
            deviceCapability?.let { cap ->
                DeviceInfoCard(cap)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Models list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                itemsIndexed(models) { index, modelState ->
                    val itemScale = remember { Animatable(0.9f) }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(index * 100L)
                        itemScale.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 400f))
                    }

                    ModelCard(
                        modelState = modelState,
                        isActive = activeModel?.model?.id == modelState.model.id,
                        deviceCapability = deviceCapability,
                        modifier = Modifier.scale(itemScale.value),
                        onDownload = { onDownload(modelState.model.id) },
                        onLoad = { onLoad(modelState.model.id) },
                        onUnload = { onUnload(modelState.model.id) },
                        onDelete = { onDelete(modelState.model.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceInfoCard(capability: DeviceCapability) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("DEVICE STATUS", fontSize = 12.sp, fontFamily = OrbitronFont, color = NeonCyan, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DeviceStat("RAM", "${capability.totalRamMb / 1024}GB", "${capability.availableRamMb / 1024}GB free")
                DeviceStat("Storage", "", "${capability.availableStorageMb / 1024}GB free")
                DeviceStat("CPU", "${capability.cpuCores} cores", "")
                DeviceStat("Max Tier", capability.maxTier.label, "")
            }

            if (capability.warnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                capability.warnings.forEach { warning ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFFFAA00), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(warning, fontSize = 11.sp, fontFamily = RajdhaniFont, color = Color(0xFFFFAA00))
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceStat(label: String, value: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, fontFamily = OrbitronFont, color = TextDim)
        if (value.isNotEmpty()) {
            Text(value, fontSize = 14.sp, fontFamily = JetBrainsMonoFont, fontWeight = FontWeight.Bold, color = TextWhite)
        }
        if (subtitle.isNotEmpty()) {
            Text(subtitle, fontSize = 10.sp, fontFamily = JetBrainsMonoFont, color = NeonGreen)
        }
    }
}

@Composable
fun ModelCard(
    modelState: ModelState,
    isActive: Boolean,
    deviceCapability: DeviceCapability?,
    modifier: Modifier = Modifier,
    onDownload: () -> Unit,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onDelete: () -> Unit
) {
    val model = modelState.model
    val tierColor = when (model.tier) {
        ModelTier.LITE -> NeonGreen
        ModelTier.BALANCED -> NeonCyan
        ModelTier.PRO -> NeonPurple
    }

    val canInstall = deviceCapability?.let { cap ->
        model.tier.priority <= cap.maxTier.priority
    } ?: true

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isActive) Modifier.border(1.dp, tierColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) tierColor.copy(alpha = 0.08f) else DarkCard.copy(alpha = 0.8f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(tierColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when (model.tier) {
                            ModelTier.LITE -> Icons.Default.FlashOn
                            ModelTier.BALANCED -> Icons.Default.Balance
                            ModelTier.PRO -> Icons.Default.Star
                        },
                        model.tier.label,
                        tint = tierColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(model.name, fontSize = 16.sp, fontFamily = RajdhaniFont, fontWeight = FontWeight.Bold, color = TextWhite)
                    Text("${model.tier.label} • ${formatSize(model.sizeMb)} • v${model.version}",
                        fontSize = 12.sp, fontFamily = JetBrainsMonoFont, color = TextDim)
                }
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(tierColor.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("ACTIVE", fontSize = 10.sp, fontFamily = OrbitronFont, fontWeight = FontWeight.Bold, color = tierColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(model.description, fontSize = 12.sp, fontFamily = RajdhaniFont, color = TextDim, lineHeight = 16.sp)

            // Capabilities
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                model.capabilities.take(4).forEach { cap ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(tierColor.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(cap, fontSize = 9.sp, fontFamily = RajdhaniFont, color = tierColor)
                    }
                }
                if (model.capabilities.size > 4) {
                    Text("+${model.capabilities.size - 4}", fontSize = 9.sp, color = TextDim)
                }
            }

            // Download progress
            if (modelState.status == ModelStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(12.dp))
                @Suppress("DEPRECATION")
                LinearProgressIndicator(
                    progress = modelState.downloadProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = tierColor,
                    trackColor = DarkCard
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${(modelState.downloadProgress * 100).toInt()}% downloading...",
                    fontSize = 11.sp, color = tierColor
                )
            }

            // Loading indicator
            if (modelState.status == ModelStatus.LOADING) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = tierColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Loading model...", fontSize = 12.sp, color = tierColor)
                }
            }

            // Error
            if (modelState.status == ModelStatus.ERROR) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    modelState.errorMessage ?: "Error occurred",
                    fontSize = 11.sp, color = Color(0xFFFF4444)
                )
            }

            // Warning for heavy models
            if (!canInstall && modelState.status == ModelStatus.NOT_DOWNLOADED) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFFFAA00), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Device not powerful enough for this model",
                        fontSize = 11.sp, color = Color(0xFFFFAA00))
                }
            }

            if (model.tier == ModelTier.PRO && modelState.status == ModelStatus.NOT_DOWNLOADED && canInstall) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFFFAA00), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("This model may heat your device and consume battery.",
                        fontSize = 11.sp, color = Color(0xFFFFAA00))
                }
            }

            // Action buttons
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                when (modelState.status) {
                    ModelStatus.NOT_DOWNLOADED -> {
                        if (canInstall) {
                            Button(
                                onClick = onDownload,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = tierColor.copy(alpha = 0.2f),
                                    contentColor = tierColor
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download (${formatSize(model.sizeMb)})", fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = {},
                                enabled = false,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Not Compatible", fontSize = 12.sp)
                            }
                        }
                    }
                    ModelStatus.DOWNLOADED -> {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF4444)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onLoad,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = tierColor,
                                contentColor = DarkBackground
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Load", fontSize = 12.sp)
                        }
                    }
                    ModelStatus.LOADED -> {
                        Button(
                            onClick = onUnload,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF6600).copy(alpha = 0.2f),
                                contentColor = Color(0xFFFF6600)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Stop, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Unload", fontSize = 12.sp)
                        }
                    }
                    ModelStatus.DOWNLOADING, ModelStatus.LOADING -> {
                        // Show progress, no action
                    }
                    ModelStatus.ERROR -> {
                        Button(
                            onClick = onDownload,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF4444).copy(alpha = 0.2f),
                                contentColor = Color(0xFFFF4444)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Retry", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Delete confirmation
            if (showDeleteConfirm) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFF4444).copy(alpha = 0.1f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Delete this model?", fontSize = 12.sp, color = Color(0xFFFF4444), modifier = Modifier.weight(1f))
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel", fontSize = 12.sp, color = TextDim)
                    }
                    TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                        Text("Delete", fontSize = 12.sp, color = Color(0xFFFF4444))
                    }
                }
            }
        }
    }
}

private fun formatSize(sizeMb: Long): String {
    return if (sizeMb >= 1024) {
        "%.1f GB".format(sizeMb / 1024.0)
    } else {
        "${sizeMb} MB"
    }
}
