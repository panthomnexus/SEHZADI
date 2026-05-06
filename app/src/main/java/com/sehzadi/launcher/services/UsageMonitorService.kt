package com.sehzadi.launcher.services

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.sehzadi.launcher.system.SystemMonitor
import com.sehzadi.launcher.ai.services.GeminiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageMonitorService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val systemMonitor: SystemMonitor,
    private val geminiService: GeminiService
) {
    suspend fun diagnoseSystem(): String = withContext(Dispatchers.IO) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val totalRam = memInfo.totalMem / (1024 * 1024)
        val availRam = memInfo.availMem / (1024 * 1024)
        val usedRam = totalRam - availRam
        val ramPercent = (usedRam * 100 / totalRam).toInt()

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val runningApps = activityManager.runningAppProcesses?.size ?: 0

        val systemReport = """
            RAM: ${usedRam}MB / ${totalRam}MB (${ramPercent}%)
            Battery: ${batteryLevel}%
            Running processes: $runningApps
            Low memory: ${memInfo.lowMemory}
        """.trimIndent()

        try {
            val prompt = """System report:
                |$systemReport
                |
                |User ne pucha "phone slow hai" ya system help chahiye.
                |Analyze this and provide:
                |1. Problem identified
                |2. Solution (clear steps)
                |3. Quick optimization tips
                |Respond in Hinglish. Be practical.""".trimMargin()

            geminiService.chat(prompt)
        } catch (e: Exception) {
            buildString {
                appendLine("System Status:")
                appendLine("RAM: ${ramPercent}% used (${usedRam}MB / ${totalRam}MB)")
                appendLine("Battery: ${batteryLevel}%")
                appendLine("Running apps: $runningApps")
                if (ramPercent > 80) {
                    appendLine("RAM zyada use ho raha hai. Kuch apps band karo.")
                }
                if (batteryLevel < 20) {
                    appendLine("Battery low hai. Charging pe lagao.")
                }
                if (memInfo.lowMemory) {
                    appendLine("Phone mein memory bahut kam hai. Background apps clear karo.")
                }
            }
        }
    }

    fun checkUsageWarning(screenTimeMinutes: Long): String? {
        return when {
            screenTimeMinutes > 180 -> "Aap 3 ghante se zyada phone use kar rahe ho. Thoda break le lo."
            screenTimeMinutes > 120 -> "2 ghante ho gaye. Aankhon ko rest do."
            screenTimeMinutes > 90 -> "1.5 ghante se phone use kar rahe ho. Break ka time hai."
            else -> null
        }
    }

    fun isLateNight(): Boolean {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return hour in 23..23 || hour in 0..5
    }
}
