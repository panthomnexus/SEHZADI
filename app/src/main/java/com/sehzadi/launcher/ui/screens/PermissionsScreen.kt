package com.sehzadi.launcher.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sehzadi.launcher.ui.theme.*

data class PermissionItem(
    val key: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val permissions: List<String>,
    val isSpecial: Boolean = false
)

@Composable
fun PermissionsScreen(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val permissionItems = remember {
        listOf(
            PermissionItem(
                key = "microphone",
                title = "Microphone",
                description = "Voice commands, wake word detection, aur speech-to-text ke liye zaroori hai.",
                icon = Icons.Default.Mic,
                permissions = listOf(Manifest.permission.RECORD_AUDIO)
            ),
            PermissionItem(
                key = "camera",
                title = "Camera",
                description = "Photo capture aur video record karne ke liye zaroori hai.",
                icon = Icons.Default.CameraAlt,
                permissions = listOf(Manifest.permission.CAMERA)
            ),
            PermissionItem(
                key = "contacts",
                title = "Contacts",
                description = "Voice se call lagane aur contacts dhundhne ke liye zaroori hai.",
                icon = Icons.Default.Contacts,
                permissions = listOf(Manifest.permission.READ_CONTACTS)
            ),
            PermissionItem(
                key = "phone",
                title = "Phone",
                description = "Call karne aur incoming calls ko manage karne ke liye.",
                icon = Icons.Default.Phone,
                permissions = listOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_CALL_LOG)
            ),
            PermissionItem(
                key = "sms",
                title = "SMS",
                description = "Voice se message bhejne aur padhne ke liye.",
                icon = Icons.Default.Sms,
                permissions = listOf(Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS)
            ),
            PermissionItem(
                key = "storage",
                title = "Storage / Media",
                description = "Gallery images, photos, aur AI-generated images save karne ke liye.",
                icon = Icons.Default.Storage,
                permissions = if (Build.VERSION.SDK_INT >= 33) {
                    listOf(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            ),
            PermissionItem(
                key = "overlay",
                title = "Overlay (Draw Over Apps)",
                description = "Floating widgets (live clock, notes, system stats) dikhane ke liye.",
                icon = Icons.Default.Layers,
                permissions = emptyList(),
                isSpecial = true
            ),
            PermissionItem(
                key = "notifications",
                title = "Notifications",
                description = "AI alerts, health warnings, aur system notifications ke liye.",
                icon = Icons.Default.Notifications,
                permissions = if (Build.VERSION.SDK_INT >= 33) {
                    listOf(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    emptyList()
                }
            )
        )
    }

    // Entry animation
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
                        "PERMISSIONS",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OrbitronFont,
                        color = NeonCyan,
                        letterSpacing = 3.sp
                    )
                    Text(
                        "Grant permissions for full experience",
                        fontSize = 12.sp,
                        fontFamily = RajdhaniFont,
                        color = TextDim
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                itemsIndexed(permissionItems) { index, item ->
                    val itemScale = remember { Animatable(0.9f) }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(index * 80L)
                        itemScale.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 400f))
                    }

                    PermissionCard(
                        item = item,
                        modifier = Modifier.scale(itemScale.value),
                        onGrant = {
                            if (item.isSpecial && item.key == "overlay") {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } else {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionCard(
    item: PermissionItem,
    modifier: Modifier = Modifier,
    onGrant: () -> Unit
) {
    val context = LocalContext.current

    val isGranted = remember(item) {
        if (item.isSpecial && item.key == "overlay") {
            Settings.canDrawOverlays(context)
        } else if (item.permissions.isEmpty()) {
            true
        } else {
            item.permissions.all { perm ->
                ContextCompat.checkSelfPermission(context, perm) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                NeonGreen.copy(alpha = 0.08f)
            else
                DarkCard.copy(alpha = 0.8f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isGranted)
                            NeonGreen.copy(alpha = 0.2f)
                        else
                            NeonCyan.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    item.icon,
                    contentDescription = item.title,
                    tint = if (isGranted) NeonGreen else NeonCyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title,
                        fontSize = 15.sp,
                        fontFamily = RajdhaniFont,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite
                    )
                    if (isGranted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            "Granted",
                            tint = NeonGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    item.description,
                    fontSize = 12.sp,
                    fontFamily = RajdhaniFont,
                    color = TextDim,
                    lineHeight = 16.sp
                )
            }

            if (!isGranted) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onGrant,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan.copy(alpha = 0.2f),
                        contentColor = NeonCyan
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Grant", fontSize = 12.sp, fontFamily = RajdhaniFont)
                }
            }
        }
    }
}
