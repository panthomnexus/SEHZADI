package com.sehzadi.launcher.ai.services

import com.sehzadi.launcher.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HuggingFaceService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val apiKey: String get() = BuildConfig.HUGGINGFACE_API_KEY
    private val modelUrl = "https://api-inference.huggingface.co/models/stabilityai/stable-diffusion-xl-base-1.0"

    suspend fun generateImage(prompt: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw Exception("HuggingFace API key not configured")

        val requestBody = JSONObject().apply {
            put("inputs", prompt)
            put("parameters", JSONObject().apply {
                put("num_inference_steps", 30)
                put("guidance_scale", 7.5)
            })
        }

        val request = Request.Builder()
            .url(modelUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            throw Exception("Image generation failed: $errorBody")
        }

        val imageBytes = response.body?.bytes() ?: throw Exception("Empty response")

        // Save image to local storage
        val imagesDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "sehzadi_generated")
        imagesDir.mkdirs()
        val imageFile = File(imagesDir, "generated_${System.currentTimeMillis()}.png")

        FileOutputStream(imageFile).use { fos ->
            fos.write(imageBytes)
        }

        imageFile.absolutePath
    }
}
