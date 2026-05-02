package com.sehzadi.launcher.ai

import com.sehzadi.launcher.ai.services.GeminiService
import com.sehzadi.launcher.ai.services.GroqService
import com.sehzadi.launcher.ai.services.HuggingFaceService
import com.sehzadi.launcher.ai.services.TavilyService
import com.sehzadi.launcher.ai.services.NotionService
import com.sehzadi.launcher.apps.AppManager
import com.sehzadi.launcher.core.ParsedIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class AIResponse(
    val text: String,
    val action: AIAction? = null,
    val imageUrl: String? = null,
    val searchResults: List<SearchResult>? = null
)

data class SearchResult(
    val title: String,
    val url: String,
    val content: String
)

sealed class AIAction {
    data class OpenApp(val packageName: String, val appName: String) : AIAction()
    data class SearchApps(val query: String) : AIAction()
    data class GenerateImage(val prompt: String) : AIAction()
    data class WebSearch(val query: String) : AIAction()
    data class SaveNote(val title: String, val content: String) : AIAction()
    data class GenerateCode(val prompt: String, val language: String) : AIAction()
    data class GetWeather(val city: String) : AIAction()
    data class MakeCall(val contactName: String) : AIAction()
    data class SendMessage(val contactName: String, val message: String) : AIAction()
    data object TakePhoto : AIAction()
    data object ToggleWifi : AIAction()
    data object ToggleBluetooth : AIAction()
    data object ShowSystemStats : AIAction()
}

@Singleton
class AIEngine @Inject constructor(
    private val geminiService: GeminiService,
    private val groqService: GroqService,
    private val huggingFaceService: HuggingFaceService,
    private val tavilyService: TavilyService,
    private val notionService: NotionService,
    private val appManager: AppManager
) {
    private val conversationHistory = mutableListOf<Pair<String, String>>()

    suspend fun processCommand(input: String): AIResponse = withContext(Dispatchers.IO) {
        val action = detectAction(input)

        when (action) {
            is AIAction.OpenApp -> {
                val app = appManager.findAppByName(action.appName)
                if (app != null) {
                    appManager.launchApp(app.packageName)
                    AIResponse("Opening ${app.appName}...", action)
                } else {
                    AIResponse("App '${action.appName}' not found. Available apps search karo?")
                }
            }
            is AIAction.SearchApps -> {
                val results = appManager.searchApps(action.query)
                val appList = results.joinToString(", ") { it.appName }
                AIResponse("Found: $appList", action)
            }
            is AIAction.GenerateImage -> {
                try {
                    val imageUrl = huggingFaceService.generateImage(action.prompt)
                    AIResponse("Image generated!", action, imageUrl = imageUrl)
                } catch (e: Exception) {
                    AIResponse("Image generation failed: ${e.message}")
                }
            }
            is AIAction.WebSearch -> {
                try {
                    val results = tavilyService.search(action.query)
                    val summary = results.joinToString("\n\n") { "**${it.title}**\n${it.content}" }
                    AIResponse(summary, action, searchResults = results)
                } catch (e: Exception) {
                    getAIResponse(input)
                }
            }
            is AIAction.SaveNote -> {
                try {
                    notionService.saveNote(action.title, action.content)
                    AIResponse("Note saved: '${action.title}'", action)
                } catch (e: Exception) {
                    AIResponse("Note save failed: ${e.message}")
                }
            }
            is AIAction.GenerateCode -> {
                try {
                    val code = groqService.generateCode(action.prompt, action.language)
                    AIResponse(code, action)
                } catch (e: Exception) {
                    getAIResponse("Generate code: $input")
                }
            }
            else -> getAIResponse(input)
        }
    }

    private suspend fun getAIResponse(input: String): AIResponse {
        return try {
            val context = buildContext()
            val response = geminiService.chat(input, context)
            addToHistory(input, response)
            AIResponse(response)
        } catch (e: Exception) {
            try {
                val response = groqService.chat(input)
                addToHistory(input, response)
                AIResponse(response)
            } catch (e2: Exception) {
                val localResponse = getLocalFallbackResponse(input)
                AIResponse(localResponse)
            }
        }
    }

    private fun detectAction(input: String): AIAction? {
        val lower = input.lowercase()

        // App opening patterns (Hindi + English)
        val openPatterns = listOf(
            "open ", "launch ", "start ",
            "khol", "chalu kar", "open karo", "start karo"
        )
        for (pattern in openPatterns) {
            if (lower.contains(pattern)) {
                val appName = extractAppName(lower, pattern)
                if (appName.isNotBlank()) {
                    return AIAction.OpenApp("", appName)
                }
            }
        }

        // Image generation
        if (lower.contains("image bana") || lower.contains("generate image") ||
            lower.contains("photo bana") || lower.contains("picture bana") ||
            lower.contains("create image")) {
            val prompt = lower.replace(Regex("(image|photo|picture) (bana|banao|generate|create)"), "").trim()
            return AIAction.GenerateImage(prompt.ifBlank { input })
        }

        // Web search
        if (lower.contains("search") || lower.contains("find") ||
            lower.contains("dhundh") || lower.contains("kya hai") ||
            lower.contains("bata") || lower.contains("research")) {
            return AIAction.WebSearch(input)
        }

        // Note saving
        if (lower.contains("note") || lower.contains("save") ||
            lower.contains("likh") || lower.contains("yaad rakh")) {
            return AIAction.SaveNote("Note", input)
        }

        // Code generation
        if (lower.contains("code") || lower.contains("program") ||
            lower.contains("function") || lower.contains("script")) {
            val language = detectLanguage(lower)
            return AIAction.GenerateCode(input, language)
        }

        // Call
        if (lower.contains("call") || lower.contains("phone") ||
            lower.contains("call lagao") || lower.contains("call karo")) {
            val name = extractContactName(lower)
            return AIAction.MakeCall(name)
        }

        // Message
        if (lower.contains("message") || lower.contains("msg") ||
            lower.contains("bhej") || lower.contains("send")) {
            val name = extractContactName(lower)
            return AIAction.SendMessage(name, input)
        }

        // System controls
        if (lower.contains("wifi")) return AIAction.ToggleWifi
        if (lower.contains("bluetooth")) return AIAction.ToggleBluetooth
        if (lower.contains("photo khinch") || lower.contains("take photo") ||
            lower.contains("camera")) return AIAction.TakePhoto
        if (lower.contains("system") || lower.contains("stats") ||
            lower.contains("battery") || lower.contains("ram")) return AIAction.ShowSystemStats

        return null
    }

    private fun extractAppName(input: String, pattern: String): String {
        val idx = input.indexOf(pattern)
        if (idx < 0) return ""
        return input.substring(idx + pattern.length).trim()
            .replace(Regex("(karo|kar do|kro|please|plz)"), "").trim()
    }

    private fun extractContactName(input: String): String {
        val patterns = listOf("ko ", "to ", "call ", "message ", "msg ")
        for (p in patterns) {
            val idx = input.indexOf(p)
            if (idx >= 0) {
                val remaining = input.substring(idx + p.length).trim()
                return remaining.split(" ").firstOrNull() ?: remaining
            }
        }
        return input
    }

    private fun detectLanguage(input: String): String {
        return when {
            input.contains("python") -> "python"
            input.contains("javascript") || input.contains("js") -> "javascript"
            input.contains("kotlin") -> "kotlin"
            input.contains("java") -> "java"
            input.contains("html") -> "html"
            input.contains("css") -> "css"
            input.contains("sql") -> "sql"
            else -> "python"
        }
    }

    private fun buildContext(): String {
        val recentHistory = conversationHistory.takeLast(10)
        return recentHistory.joinToString("\n") { "User: ${it.first}\nAssistant: ${it.second}" }
    }

    private fun addToHistory(input: String, response: String) {
        conversationHistory.add(Pair(input, response))
        if (conversationHistory.size > 50) {
            conversationHistory.removeAt(0)
        }
    }

    fun clearHistory() {
        conversationHistory.clear()
    }

    fun getHistory(): List<Pair<String, String>> = conversationHistory.toList()

    suspend fun parseUserIntent(input: String): ParsedIntent = withContext(Dispatchers.IO) {
        val lower = input.lowercase()

        // Fast local intent detection first (no API call needed)
        val localIntent = detectLocalIntent(lower, input)
        if (localIntent != null) return@withContext localIntent

        // If local detection fails, use AI to parse intent
        try {
            val prompt = """Parse this user command and return JSON with "intent" and "entities".
                |Possible intents: open_app, call, message, whatsapp, photo, video, stock, gallery, clock, system_stats, system_help, screen_assist, generate_image, web_search, save_note, generate_code, toggle_wifi, toggle_bluetooth, weather, read_messages, live_notes, chat
                |
                |Entities can include: app, name, text, ticker, prompt, query, title, content, language
                |
                |User said: "$input"
                |
                |Return ONLY valid JSON like: {"intent": "open_app", "entities": {"app": "WhatsApp"}}""".trimMargin()

            val response = groqService.chat(prompt)
            val jsonStr = response.trim().let {
                if (it.startsWith("{")) it
                else it.substringAfter("{").let { s -> "{$s" }.substringBeforeLast("}").let { s -> "$s}" }
            }
            val json = JSONObject(jsonStr)
            val intent = json.optString("intent", "chat")
            val entitiesJson = json.optJSONObject("entities")
            val entities = mutableMapOf<String, String>()
            entitiesJson?.keys()?.forEach { key ->
                entities[key] = entitiesJson.optString(key, "")
            }
            ParsedIntent(intent = intent, text = input, entities = entities)
        } catch (e: Exception) {
            ParsedIntent(intent = "chat", text = input)
        }
    }

    private fun detectLocalIntent(lower: String, original: String): ParsedIntent? {
        // Open app patterns
        val openPatterns = listOf("open ", "launch ", "start ", "khol", "chalu kar", "open karo")
        for (pattern in openPatterns) {
            if (lower.contains(pattern)) {
                val appName = extractAppName(lower, pattern)
                if (appName.isNotBlank()) {
                    return ParsedIntent("open_app", original, mapOf("app" to appName))
                }
            }
        }

        // Call patterns
        if (lower.contains("call lagao") || lower.contains("call karo") ||
            lower.contains("ko call") || (lower.contains("call ") && !lower.contains("incoming"))) {
            val name = extractContactName(lower)
            return ParsedIntent("call", original, mapOf("name" to name))
        }

        // WhatsApp
        if (lower.contains("whatsapp") && (lower.contains("bhej") || lower.contains("send"))) {
            val name = extractContactName(lower)
            val msg = lower.substringAfter("'", "").substringBefore("'", original)
            return ParsedIntent("whatsapp", original, mapOf("name" to name, "text" to msg))
        }

        // SMS
        if (lower.contains("message bhej") || lower.contains("sms") || lower.contains("msg bhej")) {
            val name = extractContactName(lower)
            val msg = lower.substringAfter("'", "").substringBefore("'", original)
            return ParsedIntent("message", original, mapOf("name" to name, "text" to msg))
        }

        // Photo/Camera
        if (lower.contains("photo") || lower.contains("camera") || lower.contains("pic le") || lower.contains("photo khinch") || lower.contains("photo le")) {
            return ParsedIntent("photo", original)
        }

        // Video
        if (lower.contains("video record") || lower.contains("video bana")) {
            return ParsedIntent("video", original)
        }

        // Stock
        if (lower.contains("stock") || lower.contains("share price") || lower.contains("analysis kar")) {
            val ticker = lower.replace(Regex(".*(stock|share|analysis|price|kar|ka|ke).*"), "").trim()
                .split(" ").firstOrNull { it.length > 1 } ?: original.split(" ").last()
            return ParsedIntent("stock", original, mapOf("ticker" to ticker))
        }

        // Gallery
        if (lower.contains("gallery") || lower.contains("photos dikha") || lower.contains("images dikha")) {
            return ParsedIntent("gallery", original)
        }

        // Clock widget
        if (lower.contains("live clock") || lower.contains("clock bana") || lower.contains("clock dikha")) {
            return ParsedIntent("clock", original)
        }

        // System help / fix
        if (lower.contains("phone slow") || lower.contains("slow hai") || lower.contains("fix karo") ||
            lower.contains("problem") || lower.contains("hang")) {
            return ParsedIntent("system_help", original)
        }

        // Screen assist
        if (lower.contains("ye kaise") || lower.contains("screen") || lower.contains("guide") || lower.contains("help me")) {
            return ParsedIntent("screen_assist", original, mapOf("query" to original))
        }

        // Image generation
        if (lower.contains("image bana") || lower.contains("generate image") || lower.contains("picture bana")) {
            val prompt = lower.replace(Regex("(image|photo|picture) (bana|banao|generate|create)"), "").trim()
            return ParsedIntent("generate_image", original, mapOf("prompt" to prompt.ifBlank { original }))
        }

        // Web search
        if (lower.contains("search") || lower.contains("dhundh") || lower.contains("kya hai") || lower.contains("research")) {
            return ParsedIntent("web_search", original, mapOf("query" to original))
        }

        // Notes
        if (lower.contains("note likh") || lower.contains("note save") || lower.contains("yaad rakh")) {
            return ParsedIntent("save_note", original, mapOf("title" to "Note", "content" to original))
        }

        // Memory / Remember
        if (lower.contains("remember") || lower.contains("yaad kar")) {
            val after = original.substringAfter("remember", original).substringAfter("yaad kar", original).trim()
            val key = after.substringBefore(":", "preference").trim()
            val value = after.substringAfter(":", after).trim()
            return ParsedIntent("save_memory", original, mapOf("key" to key, "value" to value))
        }

        // Permissions
        if (lower.contains("permission")) {
            return ParsedIntent("permissions", original)
        }

        // Code generation
        if (lower.contains("code likh") || lower.contains("code bana") || lower.contains("program bana")) {
            val lang = detectLanguage(lower)
            return ParsedIntent("generate_code", original, mapOf("prompt" to original, "language" to lang))
        }

        // System controls
        if (lower.contains("wifi")) return ParsedIntent("toggle_wifi", original)
        if (lower.contains("bluetooth")) return ParsedIntent("toggle_bluetooth", original)
        if (lower.contains("weather") || lower.contains("mausam")) return ParsedIntent("weather", original)

        // System stats
        if (lower.contains("system") || lower.contains("stats") || lower.contains("ram") || lower.contains("battery kitni")) {
            return ParsedIntent("system_stats", original)
        }

        return null // Let AI handle it
    }

    private fun getLocalFallbackResponse(input: String): String {
        val lower = input.lowercase()
        return when {
            lower.contains("weather") || lower.contains("mausam") ->
                "Weather command detected. API key set karo Settings mein — phir live weather milega."
            lower.contains("open") ->
                "App open command detected. Main launcher manager se route karunga."
            lower.contains("call") ->
                "Call command detected. Contact confirm hone ke baad call lagunga."
            lower.contains("message") || lower.contains("sms") || lower.contains("whatsapp") ->
                "Message command detected. Contact resolve karke message bhejunga."
            lower.contains("photo") || lower.contains("camera") ->
                "Camera command detected. Photo capture karke gallery mein save karunga."
            lower.contains("clock") ->
                "Live clock widget command detected. HUD pe dikhaunga."
            lower.contains("stock") ->
                "Stock analysis command detected. API key configure karo for live data."
            lower.contains("help") || lower.contains("kaise") ->
                "Main aapki help karne ke liye ready hoon. Koi bhi command bolo — app kholna, call karna, photo lena, ya AI se baat karna."
            lower.contains("hello") || lower.contains("hi") || lower.contains("namaste") ->
                "Namaste! Main SEHZADI hoon — aapka AI assistant. Kya karu aapke liye?"
            lower.contains("thank") || lower.contains("shukriya") || lower.contains("dhanyavaad") ->
                "Shukriya! Kuch aur help chahiye toh batao."
            conversationHistory.isNotEmpty() ->
                "Main aapki preferences yaad rakh raha hoon. API keys Settings mein configure karo for full AI power."
            else ->
                "Main ready hoon. API keys set karo Settings mein for Gemini/Groq AI responses. Tab tak basic commands kaam karenge."
        }
    }
}
