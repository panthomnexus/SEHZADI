package com.sehzadi.launcher.system

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

data class SystemStats(
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false,
    val ramUsagePercent: Float = 0f,
    val ramUsedMB: Long = 0,
    val ramTotalMB: Long = 0,
    val cpuUsagePercent: Float = 0f,
    val storageUsedGB: Float = 0f,
    val storageTotalGB: Float = 0f,
    val storageUsagePercent: Float = 0f,
    val downloadSpeedKbps: Float = 0f,
    val uploadSpeedKbps: Float = 0f,
    val isNetworkConnected: Boolean = false,
    val networkType: String = "Unknown"
)

@Singleton
class SystemMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var prevRxBytes = TrafficStats.getTotalRxBytes()
    private var prevTxBytes = TrafficStats.getTotalTxBytes()
    private var prevTimestamp = System.currentTimeMillis()

    fun getSystemStatsFlow(intervalMs: Long = 2000): Flow<SystemStats> = flow {
        while (true) {
            val stats = getSystemStats()
            emit(stats)
            delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getSystemStats(): SystemStats = withContext(Dispatchers.IO) {
        val battery = getBatteryInfo()
        val ram = getRamInfo()
        val cpu = getCpuUsage()
        val storage = getStorageInfo()
        val network = getNetworkInfo()
        val speed = getNetworkSpeed()

        SystemStats(
            batteryLevel = battery.first,
            isCharging = battery.second,
            ramUsagePercent = ram.first,
            ramUsedMB = ram.second,
            ramTotalMB = ram.third,
            cpuUsagePercent = cpu,
            storageUsedGB = storage.first,
            storageTotalGB = storage.second,
            storageUsagePercent = if (storage.second > 0) (storage.first / storage.second) * 100f else 0f,
            downloadSpeedKbps = speed.first,
            uploadSpeedKbps = speed.second,
            isNetworkConnected = network.first,
            networkType = network.second
        )
    }

    private fun getBatteryInfo(): Pair<Int, Boolean> {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == BatteryManager.BATTERY_STATUS_FULL

        val batteryPercent = if (scale > 0) (level * 100) / scale else 0
        return Pair(batteryPercent, isCharging)
    }

    private fun getRamInfo(): Triple<Float, Long, Long> {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val totalMB = memInfo.totalMem / (1024 * 1024)
        val availMB = memInfo.availMem / (1024 * 1024)
        val usedMB = totalMB - availMB
        val usagePercent = if (totalMB > 0) (usedMB.toFloat() / totalMB.toFloat()) * 100f else 0f

        return Triple(usagePercent, usedMB, totalMB)
    }

    private fun getCpuUsage(): Float {
        return try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val line = reader.readLine()
            reader.close()

            val parts = line.split("\\s+".toRegex())
            if (parts.size >= 8) {
                val user = parts[1].toLong()
                val nice = parts[2].toLong()
                val system = parts[3].toLong()
                val idle = parts[4].toLong()
                val iowait = parts[5].toLong()
                val irq = parts[6].toLong()
                val softirq = parts[7].toLong()

                val total = user + nice + system + idle + iowait + irq + softirq
                val active = total - idle - iowait

                if (total > 0) (active.toFloat() / total.toFloat()) * 100f else 0f
            } else {
                0f
            }
        } catch (e: Exception) {
            0f
        }
    }

    private fun getStorageInfo(): Pair<Float, Float> {
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.blockSizeLong * stat.blockCountLong
        val availBytes = stat.blockSizeLong * stat.availableBlocksLong
        val usedBytes = totalBytes - availBytes

        val totalGB = totalBytes / (1024f * 1024f * 1024f)
        val usedGB = usedBytes / (1024f * 1024f * 1024f)

        return Pair(usedGB, totalGB)
    }

    private fun getNetworkInfo(): Pair<Boolean, String> {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        val isConnected = capabilities != null
        val type = when {
            capabilities == null -> "Disconnected"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Unknown"
        }

        return Pair(isConnected, type)
    }

    private fun getNetworkSpeed(): Pair<Float, Float> {
        val currentRx = TrafficStats.getTotalRxBytes()
        val currentTx = TrafficStats.getTotalTxBytes()
        val currentTime = System.currentTimeMillis()

        val timeDelta = (currentTime - prevTimestamp) / 1000f
        if (timeDelta <= 0) return Pair(0f, 0f)

        val rxDelta = currentRx - prevRxBytes
        val txDelta = currentTx - prevTxBytes

        val downloadKbps = (rxDelta / 1024f) / timeDelta
        val uploadKbps = (txDelta / 1024f) / timeDelta

        prevRxBytes = currentRx
        prevTxBytes = currentTx
        prevTimestamp = currentTime

        return Pair(downloadKbps, uploadKbps)
    }
}
