package com.sehzadi.launcher.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.sehzadi.launcher.services.GalleryImage
import com.sehzadi.launcher.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GalleryScreen(
    images: List<GalleryImage>,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedImage by remember { mutableStateOf<GalleryImage?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = NeonCyan)
                }
                Text(
                    "GALLERY",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OrbitronFont,
                    color = NeonCyan,
                    letterSpacing = 4.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "${images.size} images",
                    fontSize = 12.sp,
                    fontFamily = JetBrainsMonoFont,
                    color = TextDim
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (images.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PhotoLibrary, "Empty", tint = TextDim, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No images yet", fontSize = 16.sp, fontFamily = RajdhaniFont, color = TextDim)
                        Text("Take a photo or generate an image", fontSize = 12.sp, fontFamily = RajdhaniFont, color = TextDim.copy(alpha = 0.5f))
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(images) { image ->
                        GalleryThumbnail(
                            image = image,
                            onClick = { selectedImage = image }
                        )
                    }
                }
            }
        }

        // Full image viewer
        selectedImage?.let { image ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground.copy(alpha = 0.95f))
                    .clickable { selectedImage = null },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(
                        model = File(image.path),
                        contentDescription = image.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        contentScale = ContentScale.Fit
                    )

                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                try {
                                    val file = File(image.path)
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/*"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Image").apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    })
                                } catch (_: Exception) {}
                            },
                            containerColor = NeonCyan,
                            contentColor = DarkBackground
                        ) {
                            Icon(Icons.Default.Share, "Share")
                        }

                        FloatingActionButton(
                            onClick = {
                                onDelete(image.path)
                                selectedImage = null
                            },
                            containerColor = Color(0xFFFF4444),
                            contentColor = Color.White
                        ) {
                            Icon(Icons.Default.Delete, "Delete")
                        }

                        FloatingActionButton(
                            onClick = { selectedImage = null },
                            containerColor = DarkCard,
                            contentColor = TextWhite
                        ) {
                            Icon(Icons.Default.Close, "Close")
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 32.dp)
                    ) {
                        Text(image.name, fontSize = 12.sp, color = TextDim)
                        if (image.isAiGenerated) {
                            Text("AI Generated", fontSize = 10.sp, color = NeonPurple)
                        }
                        Text(
                            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                                .format(Date(image.timestamp)),
                            fontSize = 10.sp,
                            color = TextDim.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GalleryThumbnail(
    image: GalleryImage,
    onClick: () -> Unit
) {
    val scale = remember { Animatable(0.8f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 300f))
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(scale.value)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = File(image.path),
            contentDescription = image.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (image.isAiGenerated) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(NeonPurple),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, "AI", tint = Color.White, modifier = Modifier.size(10.dp))
            }
        }
    }
}
