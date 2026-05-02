package com.sehzadi.launcher.health

import android.app.usage.UsageStatsManager
import android.content.Context
import com.sehzadi.launcher.storage.StorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class WellnessData(
    val todayScreenTimeMinutes: Long = 0,
    val currentSessionMinutes: Long = 0,
    val unlockCount: Int = 0,
    val isLateNight: Boolean = false,
    val shouldTakeBreak: Boolean = false,
    val suggestion: String? = null
)

@Singleton
class WellnessManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageManager: StorageManager
) {
    private val _wellnessData = MutableStateFlow(WellnessData())
    val wellnessData: StateFlow<WellnessData> = _wellnessData.asStateFlow()

    private var sessionStartTime = System.currentTimeMillis()

    fun startSession() {
        sessionStartTime = System.currentTimeMillis()
    }

    fun updateWellnessData() {
        val now = System.currentTimeMillis()
        val sessionDuration = (now - sessionStartTime) / (1000 * 60)
        val totalScreenTime = storageManager.getTodayScreenTime() / (1000 * 60)

        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val isLateNight = hour >= 23 || hour < 5

        val shouldBreak = sessionDuration > 30

        val suggestion = when {
            isLateNight && sessionDuration > 15 ->
                "Raat ho gayi hai. Phone rakh do aur so jao. Health important hai."
            shouldBreak ->
                "Aap ${sessionDuration} minute se continuously phone use kar rahe ho. Thoda break le lo."
            totalScreenTime > 180 ->
                "Aaj ka screen time ${totalScreenTime} minutes ho gaya. Bahar jao, fresh air lo."
            else -> null
        }

        _wellnessData.value = WellnessData(
            todayScreenTimeMinutes = totalScreenTime,
            currentSessionMinutes = sessionDuration,
            isLateNight = isLateNight,
            shouldTakeBreak = shouldBreak,
            suggestion = suggestion
        )
    }

    fun getTodayUsageStats(): Map<String, Long> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )

        return stats
            .filter { it.totalTimeInForeground > 0 }
            .associate { it.packageName to (it.totalTimeInForeground / (1000 * 60)) }
            .toSortedMap(compareByDescending { stats.find { s -> s.packageName == it }?.totalTimeInForeground ?: 0 })
    }

    fun recordScreenTime(durationMs: Long) {
        storageManager.addScreenTime(durationMs)
    }
}
