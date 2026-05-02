package com.sehzadi.launcher.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

    val systemStats by viewModel.systemStats.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
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

        // Bottom navigation
        BottomNavBar(
            selectedPage = pagerState.currentPage,
            onPageSelected = { page ->
                scope.launch { pagerState.animateScrollToPage(page) }
            },
            onVoiceClick = { showVoiceOverlay = true },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // App Drawer overlay
        AnimatedVisibility(
            visible = showAppDrawer,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            AppDrawerScreen(
                onDismiss = { showAppDrawer = false }
            )
        }

        // Voice overlay
        AnimatedVisibility(
            visible = showVoiceOverlay,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            VoiceOverlayScreen(
                onDismiss = { showVoiceOverlay = false }
            )
        }
    }
}

@Composable
fun BottomNavBar(
    selectedPage: Int,
    onPageSelected: (Int) -> Unit,
    onVoiceClick: () -> Unit,
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
    }
}
