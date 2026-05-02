package com.sehzadi.launcher.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sehzadi.launcher.ai.AIEngine
import com.sehzadi.launcher.ai.AIResponse
import com.sehzadi.launcher.apps.AppInfo
import com.sehzadi.launcher.apps.AppManager
import com.sehzadi.launcher.communication.CommunicationManager
import com.sehzadi.launcher.customization.HudTheme
import com.sehzadi.launcher.customization.ThemeEngine
import com.sehzadi.launcher.health.WellnessManager
import com.sehzadi.launcher.permissions.PermissionManager
import com.sehzadi.launcher.storage.StorageManager
import com.sehzadi.launcher.system.SystemMonitor
import com.sehzadi.launcher.system.SystemStats
import com.sehzadi.launcher.voice.VoiceEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val appManager: AppManager,
    private val systemMonitor: SystemMonitor,
    private val aiEngine: AIEngine,
    val voiceEngine: VoiceEngine,
    private val communicationManager: CommunicationManager,
    private val themeEngine: ThemeEngine,
    private val storageManager: StorageManager,
    private val permissionManager: PermissionManager,
    private val wellnessManager: WellnessManager
) : ViewModel() {

    val installedApps: StateFlow<List<AppInfo>> = appManager.installedApps

    val systemStats: StateFlow<SystemStats> = systemMonitor.getSystemStatsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SystemStats())

    val currentTheme: StateFlow<HudTheme> = themeEngine.currentTheme

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAIProcessing = MutableStateFlow(false)
    val isAIProcessing: StateFlow<Boolean> = _isAIProcessing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<AppInfo>>(emptyList())
    val searchResults: StateFlow<List<AppInfo>> = _searchResults.asStateFlow()

    fun initialize() {
        viewModelScope.launch {
            appManager.loadInstalledApps()
            themeEngine.initialize()
            voiceEngine.initialize()
            wellnessManager.startSession()

            voiceEngine.setOnCommandReceived { command ->
                viewModelScope.launch {
                    sendMessage(command)
                }
            }
        }
    }

    fun sendMessage(text: String) {
        viewModelScope.launch {
            _chatMessages.value = _chatMessages.value + ChatMessage(text, isUser = true)
            _isAIProcessing.value = true

            try {
                val response = aiEngine.processCommand(text)
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    text = response.text,
                    isUser = false,
                    imageUrl = response.imageUrl
                )

                if (voiceEngine.voiceState.value != com.sehzadi.launcher.voice.VoiceState.IDLE) {
                    voiceEngine.speak(response.text)
                }
            } catch (e: Exception) {
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    text = "Error: ${e.message}",
                    isUser = false
                )
            } finally {
                _isAIProcessing.value = false
            }
        }
    }

    fun searchApps(query: String) {
        _searchQuery.value = query
        _searchResults.value = if (query.isBlank()) {
            emptyList()
        } else {
            appManager.searchApps(query)
        }
    }

    fun launchApp(packageName: String) {
        appManager.launchApp(packageName)
        storageManager.incrementAppUsage(packageName)
    }

    fun getCategories(): List<String> = appManager.getCategories()

    fun getAppsByCategory(category: String): List<AppInfo> = appManager.getAppsByCategory(category)

    fun setTheme(themeId: String) {
        themeEngine.setTheme(themeId)
    }

    fun getAvailableThemes() = themeEngine.getAvailableThemes()

    fun hideApp(packageName: String) = appManager.hideApp(packageName)
    fun unhideApp(packageName: String) = appManager.unhideApp(packageName)
    fun lockApp(packageName: String) = appManager.lockApp(packageName)
    fun unlockApp(packageName: String) = appManager.unlockApp(packageName)

    fun saveNote(title: String, content: String) {
        val id = "note_${System.currentTimeMillis()}"
        storageManager.saveNote(id, title, content)
    }

    fun getNotes() = storageManager.getNotes()

    fun startVoiceListening() {
        voiceEngine.startListeningForCommand()
    }

    fun stopVoiceListening() {
        voiceEngine.deactivateSession()
    }

    fun saveApiKey(key: String, value: String) {
        storageManager.saveApiKey(key, value)
    }

    fun getApiKey(key: String): String {
        return storageManager.getApiKey(key)
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.destroy()
    }
}
