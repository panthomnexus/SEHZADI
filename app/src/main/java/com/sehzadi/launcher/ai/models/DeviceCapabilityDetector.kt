package com.sehzadi.launcher.ai.models

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceCapabilityDetector @Inject constructor(
    private val context: Context
) {
    fun detect(): DeviceCapability {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availableRamMb = memInfo.availMem / (1024 * 1024)

        val stat = StatFs(Environment.getDataDirectory().path)
        val availableStorageMb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024)

        val cpuCores = Runtime.getRuntime().availableProcessors()

        val maxTier = when {
            totalRamMb >= 8000 -> ModelTier.PRO
            totalRamMb >= 6000 -> ModelTier.BALANCED
            else -> ModelTier.LITE
        }

        val warnings = mutableListOf<String>()

        if (totalRamMb < 4000) {
            warnings.add("Low RAM detected (${totalRamMb}MB). Only Lite model recommended.")
        }
        if (availableStorageMb < 2000) {
            warnings.add("Low storage (${availableStorageMb}MB free). Clear space before downloading models.")
        }
        if (availableRamMb < 1500) {
            warnings.add("RAM usage high. Close other apps before loading AI model.")
        }

        return DeviceCapability(
            totalRamMb = totalRamMb,
            availableRamMb = availableRamMb,
            availableStorageMb = availableStorageMb,
            cpuCores = cpuCores,
            maxTier = maxTier,
            warnings = warnings
        )
    }

    fun canInstallModel(model: ModelInfo): Pair<Boolean, String> {
        val cap = detect()

        if (model.sizeMb > cap.availableStorageMb) {
            return Pair(false, "Not enough storage. Need ${model.sizeMb}MB, available ${cap.availableStorageMb}MB.")
        }

        if (model.minRamMb > cap.totalRamMb) {
            return Pair(false, "Not enough RAM. Model needs ${model.minRamMb}MB, device has ${cap.totalRamMb}MB.")
        }

        if (model.tier.priority > cap.maxTier.priority) {
            return Pair(false, "Device not powerful enough for ${model.tier.label} model. Max supported: ${cap.maxTier.label}.")
        }

        return Pair(true, "Ready to download.")
    }

    fun getModelWarning(model: ModelInfo): String? {
        val cap = detect()
        return when (model.tier) {
            ModelTier.PRO -> "This model may heat your device and consume battery. Recommended for 8GB+ RAM devices."
            ModelTier.BALANCED -> if (cap.totalRamMb < 6000) "This model may slow down your device." else null
            ModelTier.LITE -> null
        }
    }
}
