package com.sehzadi.launcher.ai.models

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.sehzadi.launcher.ai.AIEngine
import com.sehzadi.launcher.ai.AIResponse
import com.sehzadi.launcher.core.ParsedIntent
import javax.inject.Inject
import javax.inject.Singleton

enum class AISource {
    LOCAL_PRO,
    LOCAL_BALANCED,
    LOCAL_LITE,
    CLOUD_GEMINI,
    CLOUD_GROQ,
    RULE_ENGINE
}

enum class TaskComplexity {
    SIMPLE,      // open app, call, message, toggle
    MODERATE,    // conversation, search, notes
    COMPLEX,     // analysis, code gen, image gen, stock
    CLOUD_ONLY   // stock data, web search, image gen
}

data class AIRouteResult(
    val response: String,
    val source: AISource,
    val confidence: Float = 1f
)

@Singleton
class HybridAIEngine @Inject constructor(
    private val context: Context,
    private val modelManager: ModelManager,
    private val aiEngine: AIEngine
) {
    private val ruleEngine = RuleEngine()

    suspend fun processInput(input: String): AIRouteResult {
        val complexity = detectComplexity(input)
        return routeByComplexity(input, complexity)
    }

    private suspend fun routeByComplexity(input: String, complexity: TaskComplexity): AIRouteResult {
        return when (complexity) {
            TaskComplexity.SIMPLE -> routeSimpleTask(input)
            TaskComplexity.MODERATE -> routeModerateTask(input)
            TaskComplexity.COMPLEX -> routeComplexTask(input)
            TaskComplexity.CLOUD_ONLY -> routeCloudTask(input)
        }
    }

    private suspend fun routeSimpleTask(input: String): AIRouteResult {
        // Simple tasks: Lite model or rule engine
        val activeTier = modelManager.getActiveModelTier()

        if (activeTier != null) {
            return AIRouteResult(
                response = getLocalModelResponse(input),
                source = when (activeTier) {
                    ModelTier.PRO -> AISource.LOCAL_PRO
                    ModelTier.BALANCED -> AISource.LOCAL_BALANCED
                    ModelTier.LITE -> AISource.LOCAL_LITE
                }
            )
        }

        // Fallback to rule engine (works offline)
        val ruleResponse = ruleEngine.process(input)
        if (ruleResponse != null) {
            return AIRouteResult(ruleResponse, AISource.RULE_ENGINE, 0.8f)
        }

        // Cloud fallback
        if (isOnline()) {
            return getCloudResponse(input)
        }

        return AIRouteResult(
            "Main samajh gaya. Lekin abhi AI model load nahi hai aur internet bhi nahi hai. Basic commands try karo.",
            AISource.RULE_ENGINE,
            0.5f
        )
    }

    private suspend fun routeModerateTask(input: String): AIRouteResult {
        // Moderate tasks: Balanced/Pro model preferred, cloud fallback
        val activeTier = modelManager.getActiveModelTier()

        if (activeTier != null && activeTier.priority >= ModelTier.BALANCED.priority) {
            return AIRouteResult(
                response = getLocalModelResponse(input),
                source = if (activeTier == ModelTier.PRO) AISource.LOCAL_PRO else AISource.LOCAL_BALANCED
            )
        }

        // Cloud AI
        if (isOnline()) {
            return getCloudResponse(input)
        }

        // Lite model fallback
        if (activeTier == ModelTier.LITE) {
            return AIRouteResult(
                response = getLocalModelResponse(input),
                source = AISource.LOCAL_LITE,
                confidence = 0.6f
            )
        }

        return AIRouteResult(
            ruleEngine.process(input) ?: "Is sawaal ke liye better AI model ya internet chahiye.",
            AISource.RULE_ENGINE,
            0.4f
        )
    }

    private suspend fun routeComplexTask(input: String): AIRouteResult {
        // Complex tasks: Pro model or cloud
        val activeTier = modelManager.getActiveModelTier()

        if (activeTier == ModelTier.PRO) {
            return AIRouteResult(
                response = getLocalModelResponse(input),
                source = AISource.LOCAL_PRO
            )
        }

        if (isOnline()) {
            return getCloudResponse(input)
        }

        if (activeTier != null) {
            return AIRouteResult(
                response = getLocalModelResponse(input),
                source = when (activeTier) {
                    ModelTier.PRO -> AISource.LOCAL_PRO
                    ModelTier.BALANCED -> AISource.LOCAL_BALANCED
                    ModelTier.LITE -> AISource.LOCAL_LITE
                },
                confidence = 0.5f
            )
        }

        return AIRouteResult(
            "Complex analysis ke liye Pro model download karo ya internet connect karo.",
            AISource.RULE_ENGINE,
            0.3f
        )
    }

    private suspend fun routeCloudTask(input: String): AIRouteResult {
        if (isOnline()) {
            return getCloudResponse(input)
        }

        return AIRouteResult(
            "Ye feature sirf online kaam karta hai. Internet connect karo — stock data, web search, aur image generation ke liye.",
            AISource.RULE_ENGINE,
            0.2f
        )
    }

    private fun detectComplexity(input: String): TaskComplexity {
        val lower = input.lowercase()

        // Cloud-only tasks
        if (lower.contains("stock") || lower.contains("image bana") || lower.contains("generate image") ||
            lower.contains("web search") || lower.contains("research") || lower.contains("dhundh")) {
            return TaskComplexity.CLOUD_ONLY
        }

        // Simple tasks
        if (lower.contains("open ") || lower.contains("call ") || lower.contains("message ") ||
            lower.contains("photo") || lower.contains("clock") || lower.contains("wifi") ||
            lower.contains("bluetooth") || lower.contains("gallery") || lower.contains("kholdo") ||
            lower.contains("kholo") || lower.contains("band karo")) {
            return TaskComplexity.SIMPLE
        }

        // Complex tasks
        if (lower.contains("analyz") || lower.contains("code ") || lower.contains("explain") ||
            lower.contains("compare") || lower.contains("plan") || lower.contains("strategy") ||
            lower.contains("samjhao") || lower.contains("detail")) {
            return TaskComplexity.COMPLEX
        }

        // Everything else is moderate
        return TaskComplexity.MODERATE
    }

    private suspend fun getLocalModelResponse(input: String): String {
        // In production, this would run inference on the loaded local model
        // For now, route through existing AI engine which has local intent detection
        val parsed = aiEngine.parseUserIntent(input)
        if (parsed.intent != "chat") {
            return "Intent detected: ${parsed.intent}. Routing to action system."
        }

        // Use rule engine for basic responses when local model is "loaded"
        return ruleEngine.process(input)
            ?: "Main ${modelManager.getActiveModelTier()?.label ?: "AI"} model se soch raha hoon... ${input.take(50)}"
    }

    private suspend fun getCloudResponse(input: String): AIRouteResult {
        return try {
            val response = aiEngine.processCommand(input)
            AIRouteResult(response.text, AISource.CLOUD_GEMINI)
        } catch (e: Exception) {
            AIRouteResult(
                ruleEngine.process(input) ?: "Cloud AI se connect nahi ho pa raha. Internet check karo ya thodi der baad try karo.",
                AISource.RULE_ENGINE,
                0.4f
            )
        }
    }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun getCurrentSource(): String {
        val tier = modelManager.getActiveModelTier()
        return when {
            tier != null -> "Local ${tier.label} Model"
            isOnline() -> "Cloud AI (Gemini/Groq)"
            else -> "Offline Rule Engine"
        }
    }
}
