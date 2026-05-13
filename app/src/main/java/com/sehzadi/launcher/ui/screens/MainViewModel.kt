package com.sehzadi.launcher.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sehzadi.launcher.ai.AIEngine
import com.sehzadi.launcher.ai.AIResponse
import com.sehzadi.launcher.apps.AppInfo
import com.sehzadi.launcher.apps.AppManager
import com.sehzadi.launcher.communication.CommunicationManager
import com.sehzadi.launcher.core.ActionExecutor
import com.sehzadi.launcher.core.IntentRouter
import com.sehzadi.launcher.customization.HudTheme
import com.sehzadi.launcher.data.MemoryStore
import com.sehzadi.launcher.data.SettingsStore
import com.sehzadi.launcher.customization.ThemeEngine
import com.sehzadi.launcher.health.WellnessManager
import com.sehzadi.launcher.permissions.PermissionManager
import com.sehzadi.launcher.services.GalleryImage
import com.sehzadi.launcher.services.GalleryService
import com.sehzadi.launcher.services.SoundManager
import com.sehzadi.launcher.services.TtsService
import com.sehzadi.launcher.services.UsageMonitorService
import com.sehzadi.launcher.services.WidgetService
import com.sehzadi.launcher.services.WidgetType
import com.sehzadi.launcher.ai.models.DeviceCapability
import com.sehzadi.launcher.ai.models.HybridAIEngine
import com.sehzadi.launcher.ai.models.ModelManager
import com.sehzadi.launcher.ai.models.ModelState
import com.sehzadi.launcher.ai.models.ProactiveAIService
import com.sehzadi.launcher.ai.models.ProactiveSuggestion
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
    private val wellnessManager: WellnessManager,
    private val intentRouter: IntentRouter,
    private val actionExecutor: ActionExecutor,
    private val ttsService: TtsService,
    val galleryService: GalleryService,
    val widgetService: WidgetService,
    private val usageMonitorService: UsageMonitorService,
    private val memoryStore: MemoryStore,
    private val settingsStore: SettingsStore,
    val modelManager: ModelManager,
    private val hybridAIEngine: HybridAIEngine,
    val proactiveAIService: ProactiveAIService,
    val soundManager: SoundManager
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

    val galleryImages: StateFlow<List<GalleryImage>> = galleryService.images
    val showGallery: StateFlow<Boolean> = galleryService.showGallery
    val activeWidget: StateFlow<WidgetType> = widgetService.activeWidget
    val showWidget: StateFlow<Boolean> = widgetService.showWidget

    // Model Manager
    val aiModels: StateFlow<List<ModelState>> = modelManager.models
    val activeModel: StateFlow<ModelState?> = modelManager.activeModel
    val deviceCapability: StateFlow<DeviceCapability?> = modelManager.deviceCapability

    // Proactive AI
    val proactiveSuggestions: StateFlow<List<ProactiveSuggestion>> = proactiveAIService.suggestions

    // AI Source info
    val aiSourceInfo: String get() = hybridAIEngine.getCurrentSource()

    fun initialize() {
        viewModelScope.launch {
            try {
                appManager.loadInstalledApps()
            } catch (_: Exception) { }

            try {
                themeEngine.initialize()
            } catch (_: Exception) { }

            try {
                voiceEngine.initialize()
            } catch (_: Exception) { }

            try {
                wellnessManager.startSession()
            } catch (_: Exception) { }

            try {
                soundManager.initialize()
                soundManager.playPowerUp()
            } catch (_: Exception) { }

            actionExecutor.onResultCallback = { text, imageUrl ->
                viewModelScope.launch {
                    _chatMessages.value = _chatMessages.value + ChatMessage(
                        text = text,
                        isUser = false,
                        imageUrl = imageUrl
                    )
                }
            }

            voiceEngine.setOnCommandReceived { command ->
                viewModelScope.launch {
                    sendMessage(command)
                }
            }

            // Auto-start wake word listening (only if mic permission granted)
            try {
                voiceEngine.startWakeWordListening()
            } catch (_: Exception) { }
        }
    }

    fun sendMessage(text: String) {
        viewModelScope.launch {
            soundManager.playTap()
            _chatMessages.value = _chatMessages.value + ChatMessage(text, isUser = true)
            _isAIProcessing.value = true

            try {
                val parsedIntent = aiEngine.parseUserIntent(text)

                if (parsedIntent.intent == "chat") {
                    // Route through HybridAIEngine for proper cloud/local/rule routing
                    val routeResult = hybridAIEngine.processInput(text)
                    soundManager.playAIResponse()
                    _chatMessages.value = _chatMessages.value + ChatMessage(
                        text = routeResult.response,
                        isUser = false
                    )
                    if (voiceEngine.voiceState.value != com.sehzadi.launcher.voice.VoiceState.IDLE) {
                        voiceEngine.speak(routeResult.response)
                    }
                } else {
                    val action = intentRouter.route(parsedIntent)
                    soundManager.playScan()
                    actionExecutor.execute(action)

                    // Also speak the action confirmation via voice if in voice mode
                    if (voiceEngine.voiceState.value != com.sehzadi.launcher.voice.VoiceState.IDLE) {
                        voiceEngine.speak("${parsedIntent.intent} command process ho raha hai.")
                    }
                }
            } catch (e: Exception) {
                soundManager.playError()
                val errorMsg = "Maaf karo, kuch gadbad hui: ${e.message}"
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    text = errorMsg,
                    isUser = false
                )
                if (voiceEngine.voiceState.value != com.sehzadi.launcher.voice.VoiceState.IDLE) {
                    voiceEngine.speak(errorMsg)
                }
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
        soundManager.playWakeWord()
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

    fun dismissGallery() {
        galleryService.closeGallery()
    }

    fun deleteGalleryImage(path: String) {
        galleryService.deleteImage(path)
    }

    fun dismissWidget() {
        widgetService.dismissWidget()
    }

    fun saveMemory(key: String, value: String) {
        viewModelScope.launch {
            memoryStore.save(key, value)
        }
    }

    fun executeQuickAction(action: String) {
        viewModelScope.launch {
            when (action.lowercase()) {
                "call" -> sendMessage("call")
                "message" -> sendMessage("message bhejo")
                "photo" -> sendMessage("photo le lo")
                "search" -> sendMessage("web search")
                "memory" -> sendMessage("mera memory dikhao")
                "clock" -> sendMessage("live clock bana do")
                "gallery" -> sendMessage("gallery dikha do")
                "stock" -> sendMessage("stock analysis")
                "settings" -> {} // handled by UI
                "permissions" -> {} // handled by UI
            }
        }
    }

    // Model Manager functions
    fun downloadModel(modelId: String) {
        viewModelScope.launch {
            modelManager.downloadModel(modelId)
        }
    }

    fun loadModel(modelId: String) {
        viewModelScope.launch {
            modelManager.loadModel(modelId)
        }
    }

    fun unloadModel(modelId: String) {
        viewModelScope.launch {
            modelManager.unloadModel(modelId)
        }
    }

    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            modelManager.deleteModel(modelId)
        }
    }

    fun dismissSuggestion(id: String) {
        proactiveAIService.dismissSuggestion(id)
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.destroy()
        ttsService.destroy()
        soundManager.destroy()
    }
}
