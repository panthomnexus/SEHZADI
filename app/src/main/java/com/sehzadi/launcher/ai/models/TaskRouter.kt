package com.sehzadi.launcher.ai.models

import com.sehzadi.launcher.core.ParsedIntent
import javax.inject.Inject
import javax.inject.Singleton

data class TaskRoute(
    val parsedIntent: ParsedIntent,
    val complexity: TaskComplexity,
    val preferredSource: AISource,
    val requiresInternet: Boolean = false
)

@Singleton
class TaskRouter @Inject constructor(
    private val modelManager: ModelManager,
    private val hybridAIEngine: HybridAIEngine
) {
    fun routeTask(intent: ParsedIntent): TaskRoute {
        val intentType = intent.intent.lowercase()

        return when (intentType) {
            // Simple tasks → Lite/Rule Engine (works offline)
            "open_app", "call", "message", "sms", "whatsapp",
            "photo", "take_photo", "video", "record_video",
            "gallery", "clock", "live_clock", "live_notes",
            "toggle_wifi", "toggle_bluetooth", "permissions",
            "system_stats", "save_memory", "save_note" -> {
                TaskRoute(
                    parsedIntent = intent,
                    complexity = TaskComplexity.SIMPLE,
                    preferredSource = getSimpleSource()
                )
            }

            // Moderate tasks → Balanced/Cloud
            "chat", "weather", "system_help", "fix_phone",
            "read_messages", "screen_assist" -> {
                TaskRoute(
                    parsedIntent = intent,
                    complexity = TaskComplexity.MODERATE,
                    preferredSource = getModerateSource()
                )
            }

            // Complex tasks → Pro/Cloud
            "generate_code", "code" -> {
                TaskRoute(
                    parsedIntent = intent,
                    complexity = TaskComplexity.COMPLEX,
                    preferredSource = getComplexSource()
                )
            }

            // Cloud-only tasks
            "stock", "stock_analysis", "generate_image", "image",
            "web_search", "search" -> {
                TaskRoute(
                    parsedIntent = intent,
                    complexity = TaskComplexity.CLOUD_ONLY,
                    preferredSource = AISource.CLOUD_GEMINI,
                    requiresInternet = true
                )
            }

            else -> {
                TaskRoute(
                    parsedIntent = intent,
                    complexity = TaskComplexity.MODERATE,
                    preferredSource = getModerateSource()
                )
            }
        }
    }

    private fun getSimpleSource(): AISource {
        val tier = modelManager.getActiveModelTier()
        return when {
            tier != null -> when (tier) {
                ModelTier.PRO -> AISource.LOCAL_PRO
                ModelTier.BALANCED -> AISource.LOCAL_BALANCED
                ModelTier.LITE -> AISource.LOCAL_LITE
            }
            else -> AISource.RULE_ENGINE
        }
    }

    private fun getModerateSource(): AISource {
        val tier = modelManager.getActiveModelTier()
        return when {
            tier != null && tier.priority >= ModelTier.BALANCED.priority -> {
                if (tier == ModelTier.PRO) AISource.LOCAL_PRO else AISource.LOCAL_BALANCED
            }
            hybridAIEngine.isOnline() -> AISource.CLOUD_GEMINI
            tier == ModelTier.LITE -> AISource.LOCAL_LITE
            else -> AISource.RULE_ENGINE
        }
    }

    private fun getComplexSource(): AISource {
        val tier = modelManager.getActiveModelTier()
        return when {
            tier == ModelTier.PRO -> AISource.LOCAL_PRO
            hybridAIEngine.isOnline() -> AISource.CLOUD_GEMINI
            tier != null -> when (tier) {
                ModelTier.PRO -> AISource.LOCAL_PRO
                ModelTier.BALANCED -> AISource.LOCAL_BALANCED
                ModelTier.LITE -> AISource.LOCAL_LITE
            }
            else -> AISource.RULE_ENGINE
        }
    }

    fun getRouteDescription(route: TaskRoute): String {
        val sourceLabel = when (route.preferredSource) {
            AISource.LOCAL_PRO -> "Local Pro Model"
            AISource.LOCAL_BALANCED -> "Local Balanced Model"
            AISource.LOCAL_LITE -> "Local Lite Model"
            AISource.CLOUD_GEMINI -> "Cloud AI (Gemini)"
            AISource.CLOUD_GROQ -> "Cloud AI (Groq)"
            AISource.RULE_ENGINE -> "Offline Rule Engine"
        }
        return "${route.complexity.name} task → $sourceLabel"
    }
}
