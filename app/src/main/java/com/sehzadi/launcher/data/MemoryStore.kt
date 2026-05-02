package com.sehzadi.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.memoryDataStore by preferencesDataStore(name = "sehzadi_memory")

data class MemoryItem(
    val key: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class MemoryStore @Inject constructor(
    private val context: Context
) {
    private val memoryKey = stringPreferencesKey("memory_json")

    val memories: Flow<List<MemoryItem>> = context.memoryDataStore.data.map { prefs ->
        val raw = prefs[memoryKey].orEmpty()
        if (raw.isBlank()) emptyList()
        else decodeMemories(raw)
    }

    suspend fun save(key: String, value: String) {
        context.memoryDataStore.edit { prefs ->
            val current = decodeMemories(prefs[memoryKey].orEmpty())
            val updated = (current + MemoryItem(key, value)).takeLast(200)
            prefs[memoryKey] = encodeMemories(updated)
        }
    }

    suspend fun getAll(): List<MemoryItem> {
        var result = emptyList<MemoryItem>()
        context.memoryDataStore.data.collect { prefs ->
            result = decodeMemories(prefs[memoryKey].orEmpty())
        }
        return result
    }

    suspend fun clear() {
        context.memoryDataStore.edit { prefs -> prefs.remove(memoryKey) }
    }

    private fun decodeMemories(raw: String): List<MemoryItem> {
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                MemoryItem(
                    key = obj.optString("key", ""),
                    value = obj.optString("value", ""),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun encodeMemories(items: List<MemoryItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("key", item.key)
                put("value", item.value)
                put("timestamp", item.timestamp)
            }
            arr.put(obj)
        }
        return arr.toString()
    }
}
