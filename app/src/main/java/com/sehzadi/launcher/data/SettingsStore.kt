package com.sehzadi.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "sehzadi_settings")

@Singleton
class SettingsStore @Inject constructor(
    private val context: Context
) {
    private val wakeWordKey = booleanPreferencesKey("wake_word_enabled")
    private val ttsKey = booleanPreferencesKey("tts_enabled")
    private val healthAlertsKey = booleanPreferencesKey("health_alerts_enabled")
    private val notionSyncKey = booleanPreferencesKey("notion_sync_enabled")

    val wakeWordEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[wakeWordKey] ?: true }

    val ttsEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[ttsKey] ?: true }

    val healthAlertsEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[healthAlertsKey] ?: true }

    val notionSyncEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[notionSyncKey] ?: false }

    suspend fun setWakeWordEnabled(v: Boolean) =
        context.settingsDataStore.edit { it[wakeWordKey] = v }

    suspend fun setTtsEnabled(v: Boolean) =
        context.settingsDataStore.edit { it[ttsKey] = v }

    suspend fun setHealthAlertsEnabled(v: Boolean) =
        context.settingsDataStore.edit { it[healthAlertsKey] = v }

    suspend fun setNotionSyncEnabled(v: Boolean) =
        context.settingsDataStore.edit { it[notionSyncKey] = v }
}
