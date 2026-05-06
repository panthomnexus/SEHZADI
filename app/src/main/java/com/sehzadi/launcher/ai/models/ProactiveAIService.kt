package com.sehzadi.launcher.ai.models

import android.content.Context
import android.os.BatteryManager
import com.sehzadi.launcher.services.TtsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class ProactiveSuggestion(
    val id: String,
    val message: String,
    val type: SuggestionType,
    val priority: Int = 0,
    val actionLabel: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class SuggestionType {
    HEALTH_WARNING,
    USAGE_TIP,
    TASK_REMINDER,
    BATTERY_ALERT,
    BEHAVIOR_GUIDE,
    GOOD_MORNING,
    GOOD_NIGHT,
    APP_SUGGESTION
}

@Singleton
class ProactiveAIService @Inject constructor(
    private val context: Context,
    private val ttsService: TtsService
) {
    private val _suggestions = MutableStateFlow<List<ProactiveSuggestion>>(emptyList())
    val suggestions: StateFlow<List<ProactiveSuggestion>> = _suggestions.asStateFlow()

    private var lastHealthCheck = 0L
    private var sessionStartTime = System.currentTimeMillis()
    private var totalScreenTimeMinutes = 0

    fun checkAndSuggest() {
        val now = System.currentTimeMillis()
        if (now - lastHealthCheck < 60_000) return // Check max once per minute
        lastHealthCheck = now

        val newSuggestions = mutableListOf<ProactiveSuggestion>()

        // Battery check
        checkBattery()?.let { newSuggestions.add(it) }

        // Usage duration check
        checkUsageDuration()?.let { newSuggestions.add(it) }

        // Time-based suggestions
        checkTimeBasedSuggestion()?.let { newSuggestions.add(it) }

        if (newSuggestions.isNotEmpty()) {
            _suggestions.value = (_suggestions.value + newSuggestions).takeLast(10)
        }
    }

    private fun checkBattery(): ProactiveSuggestion? {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: return null

        return when {
            batteryPct <= 5 -> ProactiveSuggestion(
                id = "battery_critical",
                message = "Battery sirf $batteryPct% hai! Phone band ho sakta hai. Turant charging lagao.",
                type = SuggestionType.BATTERY_ALERT,
                priority = 10
            )
            batteryPct <= 15 -> ProactiveSuggestion(
                id = "battery_low",
                message = "Battery low hai ($batteryPct%). Charging lagao aur heavy apps band karo.",
                type = SuggestionType.BATTERY_ALERT,
                priority = 8
            )
            batteryPct <= 25 -> ProactiveSuggestion(
                id = "battery_warn",
                message = "Battery $batteryPct% bachi hai. Brightness kam karo aur background apps close karo.",
                type = SuggestionType.BATTERY_ALERT,
                priority = 5
            )
            else -> null
        }
    }

    private fun checkUsageDuration(): ProactiveSuggestion? {
        val sessionMinutes = ((System.currentTimeMillis() - sessionStartTime) / 60_000).toInt()

        return when {
            sessionMinutes >= 120 -> ProactiveSuggestion(
                id = "usage_2hr",
                message = "Aap 2 ghante se zyada phone use kar rahe ho. Aankhon ko aaram do — 20 second door dekho (20-20-20 rule).",
                type = SuggestionType.HEALTH_WARNING,
                priority = 9,
                actionLabel = "Break Reminder Set Karo"
            )
            sessionMinutes >= 90 -> ProactiveSuggestion(
                id = "usage_90min",
                message = "90 minute ho gaye phone pe. Thoda break le lo — paani piyo, stretch karo.",
                type = SuggestionType.HEALTH_WARNING,
                priority = 7
            )
            sessionMinutes >= 60 -> ProactiveSuggestion(
                id = "usage_60min",
                message = "1 ghanta ho gaya. Thoda rest lo — aankhen relax karo.",
                type = SuggestionType.USAGE_TIP,
                priority = 4
            )
            else -> null
        }
    }

    private fun checkTimeBasedSuggestion(): ProactiveSuggestion? {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        return when (hour) {
            in 23..23, in 0..4 -> ProactiveSuggestion(
                id = "night_mode",
                message = "Raat bahut ho gayi hai. Phone rakh do aur so jao — kal fresh feel karoge.",
                type = SuggestionType.GOOD_NIGHT,
                priority = 8,
                actionLabel = "Do Not Disturb On Karo"
            )
            in 5..7 -> ProactiveSuggestion(
                id = "morning",
                message = "Suprabhat! Aaj ka din acha ho. Kya plan hai aaj ka?",
                type = SuggestionType.GOOD_MORNING,
                priority = 2
            )
            else -> null
        }
    }

    fun onAppUsed(packageName: String) {
        // Track app usage for future suggestions
        totalScreenTimeMinutes++
    }

    fun speakSuggestion(suggestion: ProactiveSuggestion) {
        ttsService.speak(suggestion.message)
    }

    fun dismissSuggestion(id: String) {
        _suggestions.value = _suggestions.value.filter { it.id != id }
    }

    fun clearAll() {
        _suggestions.value = emptyList()
    }

    fun resetSession() {
        sessionStartTime = System.currentTimeMillis()
    }
}
