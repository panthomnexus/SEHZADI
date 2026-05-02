package com.sehzadi.launcher.core

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntentRouter @Inject constructor() {

    fun route(intent: ParsedIntent): Action {
        return when (intent.intent.lowercase()) {
            "open_app" -> Action.OpenApp(intent.entities["app"] ?: intent.text)
            "call" -> Action.CallContact(intent.entities["name"] ?: intent.text)
            "message", "sms" -> Action.SendMessage(
                contactName = intent.entities["name"] ?: "",
                message = intent.entities["text"] ?: intent.text
            )
            "whatsapp" -> Action.SendWhatsApp(
                contactName = intent.entities["name"] ?: "",
                message = intent.entities["text"] ?: intent.text
            )
            "photo", "take_photo" -> Action.TakePhoto
            "video", "record_video" -> Action.RecordVideo
            "stock", "stock_analysis" -> Action.AnalyzeStock(intent.entities["ticker"] ?: intent.text)
            "gallery" -> Action.ShowGallery
            "clock", "live_clock" -> Action.ShowLiveClock
            "system_stats" -> Action.ShowSystemStats
            "system_help", "fix_phone" -> Action.ShowSystemHelp
            "screen_assist" -> Action.ScreenAssist(intent.text)
            "generate_image", "image" -> Action.GenerateImage(intent.entities["prompt"] ?: intent.text)
            "web_search", "search" -> Action.WebSearch(intent.entities["query"] ?: intent.text)
            "save_note", "note" -> Action.SaveNote(
                title = intent.entities["title"] ?: "Note",
                content = intent.entities["content"] ?: intent.text
            )
            "generate_code", "code" -> Action.GenerateCode(
                prompt = intent.entities["prompt"] ?: intent.text,
                language = intent.entities["language"] ?: "python"
            )
            "toggle_wifi", "wifi" -> Action.ToggleWifi
            "toggle_bluetooth", "bluetooth" -> Action.ToggleBluetooth
            "weather" -> Action.ShowWeather
            "read_messages" -> Action.ReadMessages(intent.entities["name"] ?: "")
            "live_notes" -> Action.ShowLiveNotes
            else -> Action.Unknown
        }
    }
}
