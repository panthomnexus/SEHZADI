package com.sehzadi.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.sehzadi.launcher.ui.theme.*
import com.sehzadi.launcher.ui.screens.MainScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SehzadiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var showSplash by remember { mutableStateOf(true) }

                    if (showSplash) {
                        SplashContent(onFinished = { showSplash = false })
                    } else {
                        MainScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun SplashContent(onFinished: () -> Unit) {
    val scaleAnim = remember { Animatable(0.5f) }
    val alphaAnim = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, tween(600))
        delay(200)
        textAlpha.animateTo(1f, tween(500))
        delay(1500)
        onFinished()
    }

    // Lottie animation
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.neon_glow_ring)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        DarkBackground,
                        Color(0xFF050510),
                        DarkBackground
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scaleAnim.value)
                .alpha(alphaAnim.value)
        ) {
            // Animated Lottie ring behind logo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.fillMaxSize()
                )

                // AI Pulse inside
                val pulseComposition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(R.raw.ai_pulse_animation)
                )
                val pulseProgress by animateLottieCompositionAsState(
                    composition = pulseComposition,
                    iterations = LottieConstants.IterateForever
                )
                LottieAnimation(
                    composition = pulseComposition,
                    progress = { pulseProgress },
                    modifier = Modifier.size(100.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SEHZADI",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OrbitronFont,
                color = NeonCyan,
                letterSpacing = 8.sp,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "AI LAUNCHER",
                fontSize = 14.sp,
                fontFamily = OrbitronFont,
                color = NeonCyan.copy(alpha = 0.5f),
                letterSpacing = 6.sp,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Loading indicator
            val loadingComposition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.loading_spinner)
            )
            val loadingProgress by animateLottieCompositionAsState(
                composition = loadingComposition,
                iterations = LottieConstants.IterateForever
            )
            LottieAnimation(
                composition = loadingComposition,
                progress = { loadingProgress },
                modifier = Modifier
                    .size(40.dp)
                    .alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "INITIALIZING SYSTEMS...",
                fontSize = 10.sp,
                fontFamily = JetBrainsMonoFont,
                color = NeonCyan.copy(alpha = 0.4f),
                letterSpacing = 3.sp,
                modifier = Modifier.alpha(textAlpha.value)
            )
        }
    }
}
