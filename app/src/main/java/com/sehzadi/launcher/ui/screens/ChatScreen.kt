package com.sehzadi.launcher.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sehzadi.launcher.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val messages by viewModel.chatMessages.collectAsState()
    val isProcessing by viewModel.isAIProcessing.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(NeonCyan, NeonBlue))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, "AI", tint = DarkBackground, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("SEHZADI AI", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeonCyan, letterSpacing = 2.sp)
                Text(
                    if (isProcessing) "Thinking..." else "Online",
                    fontSize = 11.sp,
                    color = if (isProcessing) NeonPink else NeonGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    WelcomeMessage()
                }
            }
            items(messages) { message ->
                ChatBubble(message = message)
            }
            if (isProcessing) {
                item {
                    TypingIndicator()
                }
            }
        }

        // Input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 90.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask SEHZADI...", color = TextDim) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = NeonCyan.copy(alpha = 0.3f),
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    cursorColor = NeonCyan
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                leadingIcon = {
                    IconButton(onClick = { viewModel.startVoiceListening() }) {
                        Icon(Icons.Default.Mic, "Voice", tint = NeonCyan)
                    }
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText.trim())
                        inputText = ""
                    }
                },
                containerColor = NeonCyan,
                contentColor = DarkBackground,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Send, "Send", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Entry animation for each bubble
    val scale = remember { Animatable(0.85f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 400f))
    }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(300))
    }

    // Glow effect for AI responses
    val glowAlpha by rememberInfiniteTransition(label = "glow_${message.timestamp}").animateFloat(
        initialValue = 0.0f,
        targetValue = if (!message.isUser) 0.3f else 0.0f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse),
        label = "bubbleGlow"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale.value, scaleY = scale.value, alpha = alpha.value),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser)
                    NeonCyan.copy(alpha = 0.15f)
                else
                    DarkCard.copy(alpha = 0.8f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = TextWhite,
                    lineHeight = 20.sp
                )

                message.imageUrl?.let { url ->
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = url,
                        contentDescription = "Generated image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeFormat.format(Date(message.timestamp)),
                    fontSize = 10.sp,
                    color = TextDim.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun WelcomeMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SEHZADI", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = NeonCyan, letterSpacing = 6.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("AI ASSISTANT", fontSize = 12.sp, color = TextDim, letterSpacing = 4.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Try:", fontSize = 12.sp, color = TextDim)
        Spacer(modifier = Modifier.height(8.dp))

        listOf(
            "\"WhatsApp open karo\"",
            "\"Image bana do sunset ka\"",
            "\"Aaj ka weather kya hai?\"",
            "\"Code likh do Python mein\"",
            "\"Mera note save karo\""
        ).forEach { suggestion ->
            Text(
                text = suggestion,
                fontSize = 13.sp,
                color = NeonCyan.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dot1 by infiniteTransition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(600, 0), RepeatMode.Reverse), label = "d1")
    val dot2 by infiniteTransition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(600, 200), RepeatMode.Reverse), label = "d2")
    val dot3 by infiniteTransition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(600, 400), RepeatMode.Reverse), label = "d3")

    Row(
        modifier = Modifier.padding(start = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(dot1, dot2, dot3).forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(NeonCyan.copy(alpha = alpha))
            )
        }
    }
}
