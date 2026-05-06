package com.sehzadi.launcher.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sehzadi.launcher.ui.theme.*

data class ApiKeyConfig(
    val key: String,
    val label: String,
    val hint: String,
    val url: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    onOpenPermissions: () -> Unit = {},
    onOpenModelManager: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showRestartDialog by remember { mutableStateOf(false) }
    var hasChanges by remember { mutableStateOf(false) }

    val apiKeys = listOf(
        ApiKeyConfig("gemini", "Gemini API Key", "Main AI brain", "https://aistudio.google.com/app/apikey"),
        ApiKeyConfig("groq", "Groq API Key", "Fast responses + code", "https://console.groq.com/keys"),
        ApiKeyConfig("huggingface", "HuggingFace API Key", "Image generation", "https://huggingface.co/settings/tokens"),
        ApiKeyConfig("tavily", "Tavily API Key", "Deep web search", "https://app.tavily.com/home"),
        ApiKeyConfig("notion", "Notion API Key", "Notes sync", "https://www.notion.so/my-integrations"),
        ApiKeyConfig("notion_database_id", "Notion Database ID", "Notes database", "https://www.notion.so")
    )

    val keyValues = remember {
        mutableStateMapOf<String, String>().apply {
            apiKeys.forEach { config ->
                put(config.key, viewModel.getApiKey(config.key))
            }
        }
    }

    // Restart confirmation dialog
    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            containerColor = DarkCard,
            titleContentColor = NeonCyan,
            textContentColor = TextWhite,
            title = { Text("Restart Required") },
            text = { Text("API keys saved successfully! App ko restart karna padega taaki changes apply ho sakein.") },
            confirmButton = {
                Button(
                    onClick = {
                        // Restart the app
                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        context.startActivity(intent)
                        Runtime.getRuntime().exit(0)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground)
                ) {
                    Text("Restart Now")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showRestartDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDim)
                ) {
                    Text("Later")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = NeonCyan)
                }
                Text(
                    "SETTINGS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OrbitronFont,
                    color = NeonCyan,
                    letterSpacing = 4.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // API Keys Section
            Text(
                "API KEYS",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OrbitronFont,
                color = NeonCyan.copy(alpha = 0.7f),
                letterSpacing = 3.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
            Text(
                "Enter your API keys below. Get keys from the links provided.",
                fontSize = 12.sp,
                fontFamily = RajdhaniFont,
                color = TextDim,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            apiKeys.forEach { config ->
                ApiKeyInputCard(
                    config = config,
                    value = keyValues[config.key] ?: "",
                    onValueChange = { newValue ->
                        keyValues[config.key] = newValue
                        hasChanges = true
                    },
                    onGetKeyClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(config.url))
                        context.startActivity(intent)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Controls Section
            Text(
                "CONTROLS",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OrbitronFont,
                color = NeonCyan.copy(alpha = 0.7f),
                letterSpacing = 3.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            var wakeWordEnabled by remember { mutableStateOf(true) }
            var ttsEnabled by remember { mutableStateOf(true) }

            SettingsToggleCard(
                title = "Wake Word (Hacknuma)",
                description = "Voice se activate karo — 'Hacknuma' bolo",
                icon = Icons.Default.Mic,
                checked = wakeWordEnabled,
                onCheckedChange = { wakeWordEnabled = it }
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsToggleCard(
                title = "Text-to-Speech (TTS)",
                description = "AI responses bolke sunaye",
                icon = Icons.Default.VolumeUp,
                checked = ttsEnabled,
                onCheckedChange = { ttsEnabled = it }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Permissions button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPermissions() },
                colors = CardDefaults.cardColors(containerColor = DarkCard.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, "Permissions", tint = NeonPurple, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Manage Permissions", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = RajdhaniFont, color = TextWhite)
                        Text("Mic, Camera, Contacts, Phone, Storage", fontSize = 11.sp, fontFamily = RajdhaniFont, color = TextDim)
                    }
                    Icon(Icons.Default.ChevronRight, "Open", tint = TextDim)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // AI Model Manager button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenModelManager() },
                colors = CardDefaults.cardColors(containerColor = DarkCard.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Memory, "AI Models", tint = NeonCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI Model Manager", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = RajdhaniFont, color = TextWhite)
                        Text("Download, load, manage on-device AI models", fontSize = 11.sp, fontFamily = RajdhaniFont, color = TextDim)
                    }
                    Icon(Icons.Default.ChevronRight, "Open", tint = TextDim)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Theme Section
            Text(
                "THEMES",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OrbitronFont,
                color = NeonCyan.copy(alpha = 0.7f),
                letterSpacing = 3.sp,
                modifier = Modifier.padding(start = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            val themes = viewModel.getAvailableThemes()
            val currentTheme by viewModel.currentTheme.collectAsState()
            themes.forEach { theme ->
                ThemeCard(
                    theme = theme,
                    isSelected = theme.id == currentTheme.id,
                    onClick = { viewModel.setTheme(theme.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save button
            Button(
                onClick = {
                    apiKeys.forEach { config ->
                        val value = keyValues[config.key] ?: ""
                        viewModel.saveApiKey(config.key, value)
                    }
                    if (hasChanges) {
                        showRestartDialog = true
                        hasChanges = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Save, "Save", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("SAVE & RESTART", fontWeight = FontWeight.Bold, fontFamily = OrbitronFont, letterSpacing = 2.sp)
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
fun ApiKeyInputCard(
    config: ApiKeyConfig,
    value: String,
    onValueChange: (String) -> Unit,
    onGetKeyClick: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(config.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = RajdhaniFont, color = TextWhite)
                    Text(config.hint, fontSize = 11.sp, fontFamily = RajdhaniFont, color = TextDim)
                }
                TextButton(onClick = onGetKeyClick) {
                    Icon(Icons.Default.OpenInNew, "Get Key", tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Get Key", fontSize = 12.sp, color = NeonCyan)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter ${config.label}...", color = TextDim.copy(alpha = 0.5f), fontSize = 13.sp) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide" else "Show",
                            tint = TextDim
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = NeonCyan.copy(alpha = 0.2f),
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    cursorColor = NeonCyan
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (value.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, "Configured", tint = NeonGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Configured", fontSize = 11.sp, color = NeonGreen)
                }
            }
        }
    }
}

@Composable
fun ThemeCard(
    theme: com.sehzadi.launcher.customization.HudTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) theme.primaryColor.copy(alpha = 0.15f) else DarkCard.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, theme.primaryColor) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(listOf(theme.primaryColor, theme.secondaryColor))
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                theme.name,
                fontSize = 14.sp,
                fontFamily = RajdhaniFont,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) theme.primaryColor else TextWhite
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, "Selected", tint = theme.primaryColor, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun SettingsToggleCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, title, tint = NeonCyan, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = RajdhaniFont, color = TextWhite)
                Text(description, fontSize = 11.sp, fontFamily = RajdhaniFont, color = TextDim)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NeonCyan,
                    checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                    uncheckedThumbColor = TextDim,
                    uncheckedTrackColor = DarkCard
                )
            )
        }
    }
}
