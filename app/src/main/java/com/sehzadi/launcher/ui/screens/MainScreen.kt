package com.sehzadi.launcher.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sehzadi.launcher.services.WidgetType
import com.sehzadi.launcher.ui.theme.DarkBackground
import com.sehzadi.launcher.ui.theme.NeonCyan
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(initialPage = 1) { 3 }
    val scope = rememberCoroutineScope()
    var showAppDrawer by remember { mutableStateOf(false) }
    var showVoiceOverlay by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showPermissions by remember { mutableStateOf(false) }
    var showModelManager by remember { mutableStateOf(false) }

    val systemStats by viewModel.systemStats.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()
    val showGallery by viewModel.showGallery.collectAsState()
    val galleryImages by viewModel.galleryImages.collectAsState()
    val showWidget by viewModel.showWidget.collectAsState()
    val activeWidget by viewModel.activeWidget.collectAsState()
    val aiModels by viewModel.aiModels.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()
    val deviceCapability by viewModel.deviceCapability.collectAsState()

    // Entry animation
    val entryScale = remember { Animatable(0.9f) }
    val entryAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        viewModel.initialize()
        entryAlpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        entryScale.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .scale(entryScale.value)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        DarkBackground.copy(alpha = 0.95f),
                        Color(0xFF050510)
                    )
                )
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (dragAmount.y < -50 && abs(dragAmount.x) < abs(dragAmount.y)) {
                        showAppDrawer = true
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        showVoiceOverlay = !showVoiceOverlay
                    }
                )
            }
    ) {
        // Main pager: Home | HUD | Chat
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> HUDScreen(systemStats = systemStats, theme = currentTheme)
                1 -> HomeScreen(
                    onAppDrawerOpen = { showAppDrawer = true },
                    systemStats = systemStats
                )
                2 -> ChatScreen()
            }
        }

        // Floating widget overlay
        if (showWidget && activeWidget != WidgetType.NONE) {
            FloatingWidgetOverlay(
                widgetType = activeWidget,
                systemStats = systemStats,
                notes = viewModel.getNotes(),
                onDismiss = { viewModel.dismissWidget() }
            )
        }

        // Bottom navigation
        BottomNavBar(
            selectedPage = pagerState.currentPage,
            onPageSelected = { page ->
                scope.launch { pagerState.animateScrollToPage(page) }
            },
            onVoiceClick = { showVoiceOverlay = true },
            onSettingsClick = { showSettings = true },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // App Drawer overlay
        AnimatedVisibility(
            visible = showAppDrawer,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(200))
        ) {
            AppDrawerScreen(
                onDismiss = { showAppDrawer = false }
            )
        }

        // Voice overlay
        AnimatedVisibility(
            visible = showVoiceOverlay,
            enter = scaleIn(
                animationSpec = tween(400, easing = FastOutSlowInEasing),
                initialScale = 0.8f
            ) + fadeIn(tween(300)),
            exit = scaleOut(
                animationSpec = tween(300),
                targetScale = 0.8f
            ) + fadeOut(tween(200))
        ) {
            VoiceOverlayScreen(
                onDismiss = { showVoiceOverlay = false }
            )
        }

        // Settings overlay
        AnimatedVisibility(
            visible = showSettings,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeOut(tween(200))
        ) {
            SettingsScreen(
                onDismiss = { showSettings = false },
                onOpenPermissions = {
                    showSettings = false
                    showPermissions = true
                },
                onOpenModelManager = {
                    showSettings = false
                    showModelManager = true
                }
            )
        }

        // Gallery overlay
        AnimatedVisibility(
            visible = showGallery,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeOut(tween(200))
        ) {
            GalleryScreen(
                images = galleryImages,
                onDismiss = { viewModel.dismissGallery() },
                onDelete = { path -> viewModel.deleteGalleryImage(path) }
            )
        }

        // Permissions overlay
        AnimatedVisibility(
            visible = showPermissions,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeOut(tween(200))
        ) {
            PermissionsScreen(
                onDismiss = { showPermissions = false }
            )
        }

        // Model Manager overlay
        AnimatedVisibility(
            visible = showModelManager,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeOut(tween(200))
        ) {
            ModelManagerScreen(
                models = aiModels,
                deviceCapability = deviceCapability,
                activeModel = activeModel,
                onDownload = { viewModel.downloadModel(it) },
                onLoad = { viewModel.loadModel(it) },
                onUnload = { viewModel.unloadModel(it) },
                onDelete = { viewModel.deleteModel(it) },
                onDismiss = { showModelManager = false }
            )
        }
    }
}

@Composable
fun BottomNavBar(
    selectedPage: Int,
    onPageSelected: (Int) -> Unit,
    onVoiceClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp),
        containerColor = Color(0xFF111128).copy(alpha = 0.9f),
        contentColor = NeonCyan,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = selectedPage == 0,
            onClick = { onPageSelected(0) },
            icon = { Icon(Icons.Default.Dashboard, "HUD") },
            label = { Text("HUD") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonCyan,
                unselectedIconColor = Color.Gray,
                indicatorColor = NeonCyan.copy(alpha = 0.1f)
            )
        )
        NavigationBarItem(
            selected = selectedPage == 1,
            onClick = { onPageSelected(1) },
            icon = { Icon(Icons.Default.Apps, "Home") },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonCyan,
                unselectedIconColor = Color.Gray,
                indicatorColor = NeonCyan.copy(alpha = 0.1f)
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = onVoiceClick,
            icon = { Icon(Icons.Default.Mic, "Voice") },
            label = { Text("Voice") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonCyan,
                unselectedIconColor = Color.Gray,
                indicatorColor = NeonCyan.copy(alpha = 0.1f)
            )
        )
        NavigationBarItem(
            selected = selectedPage == 2,
            onClick = { onPageSelected(2) },
            icon = { Icon(Icons.Default.Chat, "Chat") },
            label = { Text("Chat") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonCyan,
                unselectedIconColor = Color.Gray,
                indicatorColor = NeonCyan.copy(alpha = 0.1f)
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = onSettingsClick,
            icon = { Icon(Icons.Default.Settings, "Settings") },
            label = { Text("Settings") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonCyan,
                unselectedIconColor = Color.Gray,
                indicatorColor = NeonCyan.copy(alpha = 0.1f)
            )
        )
    }
}
