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
class GroqService @Inject constructor() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiKey: String get() = BuildConfig.GROQ_API_KEY
    private val baseUrl = "https://api.groq.com/openai/v1/chat/completions"

    suspend fun chat(message: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext "Groq API key not configured."

        val requestBody = JSONObject().apply {
            put("model", "llama3-70b-8192")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are SEHZADI AI, a futuristic assistant. Be concise, helpful, and respond in the user's language.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", message)
                })
            })
            put("temperature", 0.7)
            put("max_tokens", 2048)
        }

        val request = Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        val json = JSONObject(body)

        if (json.has("error")) {
            throw Exception(json.getJSONObject("error").getString("message"))
        }

        json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }

    suspend fun generateCode(prompt: String, language: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext "Groq API key not configured."

        val requestBody = JSONObject().apply {
            put("model", "llama3-70b-8192")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are a code generation expert. Generate clean, working $language code. Only output code, no explanations unless asked.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.3)
            put("max_tokens", 4096)
        }

        val request = Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        val json = JSONObject(body)

        json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }
}
