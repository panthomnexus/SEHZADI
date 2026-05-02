package com.sehzadi.launcher.ai.services

import com.sehzadi.launcher.ai.SearchResult
import com.sehzadi.launcher.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TavilyService @Inject constructor(
    private val storageManager: StorageManager
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiKey: String get() = storageManager.getApiKey("tavily")
    private val baseUrl = "https://api.tavily.com/search"

    suspend fun search(query: String, maxResults: Int = 5): List<SearchResult> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw Exception("Tavily API key not configured")

        val requestBody = JSONObject().apply {
            put("api_key", apiKey)
            put("query", query)
            put("search_depth", "advanced")
            put("max_results", maxResults)
            put("include_answer", true)
        }

        val request = Request.Builder()
            .url(baseUrl)
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        val json = JSONObject(body)

        val results = mutableListOf<SearchResult>()
        val jsonResults = json.getJSONArray("results")

        for (i in 0 until jsonResults.length()) {
            val result = jsonResults.getJSONObject(i)
            results.add(
                SearchResult(
                    title = result.optString("title", ""),
                    url = result.optString("url", ""),
                    content = result.optString("content", "")
                )
            )
        }

        results
    }
}
