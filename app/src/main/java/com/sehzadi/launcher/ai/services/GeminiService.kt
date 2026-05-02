package com.sehzadi.launcher.ai.services

import com.sehzadi.launcher.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiService @Inject constructor() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiKey: String get() = BuildConfig.GEMINI_API_KEY
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent"

    suspend fun chat(message: String, context: String = ""): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext "Gemini API key not configured. Please add it in Settings."

        val systemPrompt = """You are SEHZADI AI, a futuristic intelligent assistant built into an Android launcher.
            |You are helpful, witty, and respond in the language the user uses (Hindi, English, or Hinglish).
            |You can control device functions, open apps, search the web, generate images, and more.
            |Be concise but informative. Add personality to your responses.
            |Previous conversation context: $context""".trimMargin()

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "$systemPrompt\n\nUser: $message")
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 2048)
            })
        }

        val request = Request.Builder()
            .url("$baseUrl?key=$apiKey")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response from Gemini")

        val jsonResponse = JSONObject(body)
        if (jsonResponse.has("error")) {
            throw Exception(jsonResponse.getJSONObject("error").getString("message"))
        }

        jsonResponse
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
    }
}
