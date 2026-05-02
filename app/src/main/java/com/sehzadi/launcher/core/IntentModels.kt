package com.sehzadi.launcher.core

data class ParsedIntent(
    val intent: String,
    val text: String = "",
    val entities: Map<String, String> = emptyMap()
)

sealed class Action {
    data class OpenApp(val appName: String) : Action()
    data class CallContact(val contactName: String) : Action()
    data class SendMessage(val contactName: String, val message: String) : Action()
    data class SendWhatsApp(val contactName: String, val message: String) : Action()
    object TakePhoto : Action()
    object RecordVideo : Action()
    data class AnalyzeStock(val ticker: String) : Action()
    object ShowGallery : Action()
    object ShowLiveClock : Action()
    object ShowSystemStats : Action()
    object ShowSystemHelp : Action()
    data class ScreenAssist(val query: String) : Action()
    data class GenerateImage(val prompt: String) : Action()
    data class WebSearch(val query: String) : Action()
    data class SaveNote(val title: String, val content: String) : Action()
    data class GenerateCode(val prompt: String, val language: String) : Action()
    object ToggleWifi : Action()
    object ToggleBluetooth : Action()
    object ShowWeather : Action()
    data class ReadMessages(val contactName: String) : Action()
    object ShowLiveNotes : Action()
    data class SaveMemory(val key: String, val value: String) : Action()
    object ShowPermissions : Action()
    object Unknown : Action()
}
