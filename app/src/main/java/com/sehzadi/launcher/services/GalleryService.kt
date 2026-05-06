package com.sehzadi.launcher.services

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class GalleryImage(
    val path: String,
    val name: String,
    val timestamp: Long,
    val isAiGenerated: Boolean = false
)

@Singleton
class GalleryService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _images = MutableStateFlow<List<GalleryImage>>(emptyList())
    val images: StateFlow<List<GalleryImage>> = _images.asStateFlow()

    private val _showGallery = MutableStateFlow(false)
    val showGallery: StateFlow<Boolean> = _showGallery.asStateFlow()

    init {
        loadImages()
    }

    fun loadImages() {
        val allImages = mutableListOf<GalleryImage>()

        val photosDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "sehzadi_photos")
        if (photosDir.exists()) {
            photosDir.listFiles()?.filter { it.extension in listOf("jpg", "jpeg", "png") }?.forEach { file ->
                allImages.add(
                    GalleryImage(
                        path = file.absolutePath,
                        name = file.name,
                        timestamp = file.lastModified(),
                        isAiGenerated = false
                    )
                )
            }
        }

        val aiDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "sehzadi_generated")
        if (aiDir.exists()) {
            aiDir.listFiles()?.filter { it.extension in listOf("jpg", "jpeg", "png") }?.forEach { file ->
                allImages.add(
                    GalleryImage(
                        path = file.absolutePath,
                        name = file.name,
                        timestamp = file.lastModified(),
                        isAiGenerated = true
                    )
                )
            }
        }

        _images.value = allImages.sortedByDescending { it.timestamp }
    }

    fun saveImagePath(path: String) {
        val file = File(path)
        if (file.exists()) {
            val isAi = path.contains("sehzadi_generated")
            val current = _images.value.toMutableList()
            current.add(0, GalleryImage(path, file.name, System.currentTimeMillis(), isAi))
            _images.value = current
        }
    }

    fun deleteImage(path: String) {
        val file = File(path)
        if (file.exists()) file.delete()
        _images.value = _images.value.filter { it.path != path }
    }

    fun openGallery() {
        loadImages()
        _showGallery.value = true
    }

    fun closeGallery() {
        _showGallery.value = false
    }
}
