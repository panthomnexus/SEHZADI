package com.sehzadi.launcher.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val galleryService: GalleryService
) {
    private var currentPhotoPath: String? = null

    fun capturePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val photoFile = createImageFile()
        currentPhotoPath = photoFile.absolutePath

        try {
            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            ContextCompat.startActivity(context, intent, null)
            galleryService.saveImagePath(photoFile.absolutePath)
        } catch (e: Exception) {
            val fallbackIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                ContextCompat.startActivity(context, fallbackIntent, null)
            } catch (_: Exception) {}
        }
    }

    fun recordVideo() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            ContextCompat.startActivity(context, intent, null)
        } catch (_: Exception) {}
    }

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "sehzadi_photos"
        )
        storageDir.mkdirs()
        return File(storageDir, "SEHZADI_${timestamp}.jpg")
    }

    fun getLastPhotoPath(): String? = currentPhotoPath
}
