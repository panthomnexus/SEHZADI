package com.sehzadi.launcher.ai.models

data class ModelInfo(
    val id: String,
    val name: String,
    val tier: ModelTier,
    val sizeMb: Long,
    val description: String,
    val minRamMb: Long,
    val capabilities: List<String>,
    val downloadUrl: String,
    val version: String = "1.0",
    val checksum: String = ""
)

enum class ModelTier(val label: String, val priority: Int) {
    LITE("Lite", 1),
    BALANCED("Balanced", 2),
    PRO("Pro", 3)
}

enum class ModelStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    LOADING,
    LOADED,
    ERROR
}

data class ModelState(
    val model: ModelInfo,
    val status: ModelStatus = ModelStatus.NOT_DOWNLOADED,
    val downloadProgress: Float = 0f,
    val errorMessage: String? = null,
    val filePath: String? = null
)

data class DeviceCapability(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val availableStorageMb: Long,
    val cpuCores: Int,
    val maxTier: ModelTier,
    val warnings: List<String>
)
