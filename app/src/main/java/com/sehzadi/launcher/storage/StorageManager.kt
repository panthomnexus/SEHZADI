package com.sehzadi.launcher.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    private val prefs: SharedPreferences = context.getSharedPreferences("sehzadi_prefs", Context.MODE_PRIVATE)

    private val securePrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "sehzadi_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            prefs
        }
    }

    // API Key management
    fun saveApiKey(key: String, value: String) {
        securePrefs.edit().putString("api_key_$key", value).apply()
    }

    fun getApiKey(key: String): String {
        return securePrefs.getString("api_key_$key", "") ?: ""
    }

    // Hidden apps
    fun saveHiddenApps(apps: Set<String>) {
        prefs.edit().putStringSet("hidden_apps", apps).apply()
    }

    fun getHiddenApps(): Set<String> {
        return prefs.getStringSet("hidden_apps", emptySet()) ?: emptySet()
    }

    // Locked apps
    fun saveLockedApps(apps: Set<String>) {
        prefs.edit().putStringSet("locked_apps", apps).apply()
    }

    fun getLockedApps(): Set<String> {
        return prefs.getStringSet("locked_apps", emptySet()) ?: emptySet()
    }

    // App lock PIN
    fun saveAppLockPin(pin: String) {
        securePrefs.edit().putString("app_lock_pin", pin).apply()
    }

    fun getAppLockPin(): String {
        return securePrefs.getString("app_lock_pin", "") ?: ""
    }

    // Notes (local)
    fun saveNote(id: String, title: String, content: String) {
        val notes = getNotes().toMutableMap()
        notes[id] = Pair(title, content)
        val json = gson.toJson(notes)
        prefs.edit().putString("local_notes", json).apply()
    }

    fun getNotes(): Map<String, Pair<String, String>> {
        val json = prefs.getString("local_notes", null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, Pair<String, String>>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun deleteNote(id: String) {
        val notes = getNotes().toMutableMap()
        notes.remove(id)
        val json = gson.toJson(notes)
        prefs.edit().putString("local_notes", json).apply()
    }

    // Chat history
    fun saveChatHistory(history: List<Pair<String, String>>) {
        val json = gson.toJson(history)
        prefs.edit().putString("chat_history", json).apply()
    }

    fun getChatHistory(): List<Pair<String, String>> {
        val json = prefs.getString("chat_history", null) ?: return emptyList()
        val type = object : TypeToken<List<Pair<String, String>>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // User preferences / learning data
    fun incrementAppUsage(packageName: String) {
        val count = prefs.getInt("usage_$packageName", 0)
        prefs.edit().putInt("usage_$packageName", count + 1).apply()
    }

    fun getAppUsageCount(packageName: String): Int {
        return prefs.getInt("usage_$packageName", 0)
    }

    fun saveUserPreference(key: String, value: String) {
        prefs.edit().putString("user_pref_$key", value).apply()
    }

    fun getUserPreference(key: String, default: String = ""): String {
        return prefs.getString("user_pref_$key", default) ?: default
    }

    // Theme settings
    fun saveTheme(themeId: String) {
        prefs.edit().putString("current_theme", themeId).apply()
    }

    fun getTheme(): String {
        return prefs.getString("current_theme", "neon_cyan") ?: "neon_cyan"
    }

    // Screen time tracking
    fun addScreenTime(durationMs: Long) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val current = prefs.getLong("screen_time_$today", 0)
        prefs.edit().putLong("screen_time_$today", current + durationMs).apply()
    }

    fun getTodayScreenTime(): Long {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        return prefs.getLong("screen_time_$today", 0)
    }
}
