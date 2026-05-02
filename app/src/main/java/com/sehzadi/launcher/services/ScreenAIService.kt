package com.sehzadi.launcher.services

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import com.sehzadi.launcher.ai.services.GeminiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenAIService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geminiService: GeminiService
) {
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _lastGuidance = MutableStateFlow("")
    val lastGuidance: StateFlow<String> = _lastGuidance.asStateFlow()

    suspend fun analyzeScreenAndGuide(query: String): String = withContext(Dispatchers.IO) {
        _isAnalyzing.value = true
        try {
            val prompt = """User is looking at their phone screen and asking: "$query"
                |
                |Provide step-by-step guidance to help them. Be specific and practical.
                |Consider common Android scenarios like:
                |- App settings navigation
                |- System settings
                |- Troubleshooting errors
                |- Feature discovery
                |
                |Respond in Hinglish (Hindi + English mix). Keep steps numbered and clear.""".trimMargin()

            val guidance = geminiService.chat(prompt)
            _lastGuidance.value = guidance
            guidance
        } catch (e: Exception) {
            val error = "Screen assist available nahi hai abhi: ${e.message}"
            _lastGuidance.value = error
            error
        } finally {
            _isAnalyzing.value = false
        }
    }
}
