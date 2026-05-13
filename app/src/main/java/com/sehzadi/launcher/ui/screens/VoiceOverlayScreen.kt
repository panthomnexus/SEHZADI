package com.sehzadi.launcher.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.sehzadi.launcher.R
import com.sehzadi.launcher.ui.theme.*
import com.sehzadi.launcher.voice.VoiceState

@Composable
fun VoiceOverlayScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val voiceState by viewModel.voiceEngine.voiceState.collectAsState()
    val recognizedText by viewModel.voiceEngine.recognizedText.collectAsState()
    val isListening by viewModel.voiceEngine.isListening.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "voice")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            tween(1000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground.copy(alpha = 0.95f))
            .clickable { /* prevent click through */ },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Close button
            IconButton(
                onClick = {
                    viewModel.stopVoiceListening()
                    onDismiss()
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Close, "Close", tint = TextDim)
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Voice status
            Text(
                text = when (voiceState) {
                    VoiceState.IDLE -> "Tap to speak"
                    VoiceState.LISTENING_WAKE_WORD -> "Say 'SEHZADI'..."
                    VoiceState.ACTIVATED -> "Activated! Speak now..."
                    VoiceState.LISTENING_COMMAND -> "Listening..."
                    VoiceState.PROCESSING -> "Processing..."
                    VoiceState.SPEAKING -> "Speaking..."
                },
                fontSize = 16.sp,
                fontFamily = OrbitronFont,
                color = NeonCyan,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Voice wave Lottie animation
            if (isListening) {
                val voiceWaveComposition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(R.raw.voice_wave_animation)
                )
                val voiceWaveProgress by animateLottieCompositionAsState(
                    composition = voiceWaveComposition,
                    iterations = LottieConstants.IterateForever
                )
                LottieAnimation(
                    composition = voiceWaveComposition,
                    progress = { voiceWaveProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Mic button with pulse
            Box(contentAlignment = Alignment.Center) {
                if (isListening) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        NeonCyan.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                FloatingActionButton(
                    onClick = {
                        when (voiceState) {
                            VoiceState.IDLE, VoiceState.LISTENING_WAKE_WORD -> {
                                viewModel.voiceEngine.activateSession()
                            }
                            VoiceState.ACTIVATED, VoiceState.LISTENING_COMMAND -> {
                                viewModel.voiceEngine.deactivateSession()
                            }
                            VoiceState.SPEAKING -> {
                                viewModel.voiceEngine.stopSpeaking()
                                viewModel.voiceEngine.startListeningForCommand()
                            }
                            else -> {}
                        }
                    },
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    containerColor = if (isListening) NeonCyan else DarkCard,
                    contentColor = if (isListening) DarkBackground else NeonCyan
                ) {
                    Icon(
                        when (voiceState) {
                            VoiceState.SPEAKING -> Icons.Default.VolumeUp
                            VoiceState.PROCESSING -> Icons.Default.HourglassBottom
                            else -> Icons.Default.Mic
                        },
                        contentDescription = "Voice",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Recognized text
            if (recognizedText.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard.copy(alpha = 0.8f)),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = recognizedText,
                        fontSize = 16.sp,
                        fontFamily = RajdhaniFont,
                        color = TextWhite,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Hints
            Text(
                text = "Say 'SEHZADI' to activate anytime",
                fontSize = 12.sp,
                fontFamily = RajdhaniFont,
                color = TextDim.copy(alpha = 0.5f)
            )
            Text(
                text = "Double tap screen for voice mode",
                fontSize = 12.sp,
                fontFamily = RajdhaniFont,
                color = TextDim.copy(alpha = 0.5f)
            )
        }
    }
}
