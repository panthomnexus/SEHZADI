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
class NotionService @Inject constructor() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiKey: String get() = BuildConfig.NOTION_API_KEY
    private val databaseId: String get() = BuildConfig.NOTION_DATABASE_ID
    private val baseUrl = "https://api.notion.com/v1"

    suspend fun saveNote(title: String, content: String): Boolean = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || databaseId.isBlank()) {
            throw Exception("Notion API key or Database ID not configured")
        }

        val requestBody = JSONObject().apply {
            put("parent", JSONObject().apply {
                put("database_id", databaseId)
            })
            put("properties", JSONObject().apply {
                put("Name", JSONObject().apply {
                    put("title", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", JSONObject().apply {
                                put("content", title)
                            })
                        })
                    })
                })
            })
            put("children", JSONArray().apply {
                put(JSONObject().apply {
                    put("object", "block")
                    put("type", "paragraph")
                    put("paragraph", JSONObject().apply {
                        put("rich_text", JSONArray().apply {
                            put(JSONObject().apply {
                                put("type", "text")
                                put("text", JSONObject().apply {
                                    put("content", content)
                                })
                            })
                        })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url("$baseUrl/pages")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Notion-Version", "2022-06-28")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        response.isSuccessful
    }

    suspend fun getNotes(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || databaseId.isBlank()) return@withContext emptyList()

        val requestBody = JSONObject().apply {
            put("sorts", JSONArray().apply {
                put(JSONObject().apply {
                    put("timestamp", "created_time")
                    put("direction", "descending")
                })
            })
            put("page_size", 20)
        }

        val request = Request.Builder()
            .url("$baseUrl/databases/$databaseId/query")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Notion-Version", "2022-06-28")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext emptyList()
        val json = JSONObject(body)

        val notes = mutableListOf<Pair<String, String>>()
        val results = json.getJSONArray("results")

        for (i in 0 until results.length()) {
            val page = results.getJSONObject(i)
            val properties = page.getJSONObject("properties")
            val titleArray = properties.getJSONObject("Name").getJSONArray("title")
            val title = if (titleArray.length() > 0) {
                titleArray.getJSONObject(0).getJSONObject("text").getString("content")
            } else "Untitled"

            notes.add(Pair(title, page.getString("id")))
        }

        notes
    }
}
