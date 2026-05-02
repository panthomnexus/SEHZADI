package com.sehzadi.launcher.ai

import com.sehzadi.launcher.ai.services.GeminiService
import com.sehzadi.launcher.ai.services.GroqService
import com.sehzadi.launcher.ai.services.HuggingFaceService
import com.sehzadi.launcher.ai.services.TavilyService
import com.sehzadi.launcher.ai.services.NotionService
import com.sehzadi.launcher.apps.AppManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
                AIResponse("Sorry, AI services are currently unavailable. Error: ${e2.message}")
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
}
