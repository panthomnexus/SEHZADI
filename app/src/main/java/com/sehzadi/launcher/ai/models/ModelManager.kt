package com.sehzadi.launcher.ai.models

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelManager @Inject constructor(
    private val context: Context,
    private val capabilityDetector: DeviceCapabilityDetector
) {
    private val modelsDir = File(context.filesDir, "ai_models")
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val _models = MutableStateFlow<List<ModelState>>(emptyList())
    val models: StateFlow<List<ModelState>> = _models.asStateFlow()

    private val _activeModel = MutableStateFlow<ModelState?>(null)
    val activeModel: StateFlow<ModelState?> = _activeModel.asStateFlow()

    private val _deviceCapability = MutableStateFlow<DeviceCapability?>(null)
    val deviceCapability: StateFlow<DeviceCapability?> = _deviceCapability.asStateFlow()

    init {
        modelsDir.mkdirs()
        initializeModels()
    }

    private fun initializeModels() {
        val capability = capabilityDetector.detect()
        _deviceCapability.value = capability

        val availableModels = listOf(
            ModelInfo(
                id = "lite_v1",
                name = "SEHZADI Lite",
                tier = ModelTier.LITE,
                sizeMb = 512,
                description = "Fast intent detection, basic chat, low battery usage. Perfect for quick commands like app opening, calls, messages.",
                minRamMb = 3000,
                capabilities = listOf("Intent Detection", "Basic Chat", "App Control", "Call/SMS", "Notes"),
                downloadUrl = "https://huggingface.co/models/sehzadi-lite",
                version = "1.0.0"
            ),
            ModelInfo(
                id = "balanced_v1",
                name = "SEHZADI Balanced",
                tier = ModelTier.BALANCED,
                sizeMb = 1536,
                description = "Better reasoning, medium performance. Good for daily conversations, analysis, and suggestions.",
                minRamMb = 5000,
                capabilities = listOf("Intent Detection", "Conversation", "Analysis", "Suggestions", "Context Memory", "Code Help"),
                downloadUrl = "https://huggingface.co/models/sehzadi-balanced",
                version = "1.0.0"
            ),
            ModelInfo(
                id = "pro_v1",
                name = "SEHZADI Pro",
                tier = ModelTier.PRO,
                sizeMb = 3584,
                description = "Deep reasoning, advanced assistant behavior. Full power for complex queries, planning, and deep analysis.",
                minRamMb = 7000,
                capabilities = listOf("Intent Detection", "Deep Reasoning", "Complex Analysis", "Planning", "Creative Writing", "Code Generation", "Multi-turn Context"),
                downloadUrl = "https://huggingface.co/models/sehzadi-pro",
                version = "1.0.0"
            )
        )

        _models.value = availableModels.map { model ->
            val modelFile = File(modelsDir, "${model.id}.bin")
            if (modelFile.exists()) {
                ModelState(model, ModelStatus.DOWNLOADED, 1f, filePath = modelFile.absolutePath)
            } else {
                ModelState(model)
            }
        }
    }

    suspend fun downloadModel(modelId: String) = withContext(Dispatchers.IO) {
        val index = _models.value.indexOfFirst { it.model.id == modelId }
        if (index == -1) return@withContext

        val modelState = _models.value[index]
        val model = modelState.model

        // Check device capability
        val (canInstall, message) = capabilityDetector.canInstallModel(model)
        if (!canInstall) {
            updateModelState(index, modelState.copy(status = ModelStatus.ERROR, errorMessage = message))
            return@withContext
        }

        // Start download
        updateModelState(index, modelState.copy(status = ModelStatus.DOWNLOADING, downloadProgress = 0f))

        try {
            val modelFile = File(modelsDir, "${model.id}.bin")

            // Simulate download with progress (actual model download would be from real URL)
            // For production, this would download from HuggingFace or custom server
            simulateModelDownload(modelFile, model.sizeMb, index, modelState)

            updateModelState(index, modelState.copy(
                status = ModelStatus.DOWNLOADED,
                downloadProgress = 1f,
                filePath = modelFile.absolutePath
            ))
        } catch (e: Exception) {
            updateModelState(index, modelState.copy(
                status = ModelStatus.ERROR,
                errorMessage = "Download failed: ${e.message}"
            ))
        }
    }

    private suspend fun simulateModelDownload(file: File, sizeMb: Long, index: Int, state: ModelState) {
        // Create a file that represents the model
        // In production, this would be actual model weights download
        val totalBytes = sizeMb * 1024 * 1024
        val chunkSize = 1024 * 1024L // 1MB chunks

        FileOutputStream(file).use { fos ->
            var written = 0L
            val buffer = ByteArray(8192)

            // Write model header
            val header = "SEHZADI_MODEL_v1|${state.model.id}|${state.model.tier.name}|${state.model.version}\n"
            fos.write(header.toByteArray())
            written += header.length

            // Write model data (fill with structured data to represent model weights)
            while (written < totalBytes) {
                val remaining = (totalBytes - written).coerceAtMost(buffer.size.toLong())
                // Write pseudo-random model weight data
                for (i in 0 until remaining.toInt()) {
                    buffer[i] = ((written + i) % 256).toByte()
                }
                fos.write(buffer, 0, remaining.toInt())
                written += remaining

                // Update progress
                val progress = (written.toFloat() / totalBytes).coerceIn(0f, 1f)
                updateModelState(index, state.copy(
                    status = ModelStatus.DOWNLOADING,
                    downloadProgress = progress
                ))

                // Small delay to simulate network download speed
                kotlinx.coroutines.delay(10)
            }
        }
    }

    suspend fun loadModel(modelId: String) = withContext(Dispatchers.IO) {
        val index = _models.value.indexOfFirst { it.model.id == modelId }
        if (index == -1) return@withContext

        val modelState = _models.value[index]
        if (modelState.status != ModelStatus.DOWNLOADED) return@withContext

        // Unload current model first
        _activeModel.value?.let { current ->
            unloadModel(current.model.id)
        }

        updateModelState(index, modelState.copy(status = ModelStatus.LOADING))

        // Simulate model loading (in production, load into inference engine)
        kotlinx.coroutines.delay(1500)

        val loadedState = modelState.copy(status = ModelStatus.LOADED)
        updateModelState(index, loadedState)
        _activeModel.value = loadedState
    }

    suspend fun unloadModel(modelId: String) {
        val index = _models.value.indexOfFirst { it.model.id == modelId }
        if (index == -1) return

        val modelState = _models.value[index]
        if (modelState.status == ModelStatus.LOADED) {
            updateModelState(index, modelState.copy(status = ModelStatus.DOWNLOADED))
            if (_activeModel.value?.model?.id == modelId) {
                _activeModel.value = null
            }
        }
    }

    suspend fun deleteModel(modelId: String) = withContext(Dispatchers.IO) {
        val index = _models.value.indexOfFirst { it.model.id == modelId }
        if (index == -1) return@withContext

        val modelState = _models.value[index]

        // Unload first if loaded
        if (modelState.status == ModelStatus.LOADED) {
            unloadModel(modelId)
        }

        // Delete file
        modelState.filePath?.let { File(it).delete() }
        File(modelsDir, "${modelId}.bin").delete()

        updateModelState(index, modelState.copy(
            status = ModelStatus.NOT_DOWNLOADED,
            downloadProgress = 0f,
            filePath = null
        ))
    }

    fun getActiveModelTier(): ModelTier? = _activeModel.value?.model?.tier

    fun isModelLoaded(): Boolean = _activeModel.value?.status == ModelStatus.LOADED

    fun getDownloadedModels(): List<ModelState> =
        _models.value.filter { it.status == ModelStatus.DOWNLOADED || it.status == ModelStatus.LOADED }

    fun refreshCapability() {
        _deviceCapability.value = capabilityDetector.detect()
    }

    private fun updateModelState(index: Int, newState: ModelState) {
        val current = _models.value.toMutableList()
        if (index in current.indices) {
            current[index] = newState
            _models.value = current
        }
    }

    private fun verifyChecksum(file: File, expectedChecksum: String): Boolean {
        if (expectedChecksum.isBlank()) return true
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        return hash == expectedChecksum
    }
}
