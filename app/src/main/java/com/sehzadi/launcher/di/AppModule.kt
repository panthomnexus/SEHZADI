package com.sehzadi.launcher.di

import android.content.Context
import com.sehzadi.launcher.ai.AIEngine
import com.sehzadi.launcher.ai.services.GeminiService
import com.sehzadi.launcher.ai.services.GroqService
import com.sehzadi.launcher.ai.services.HuggingFaceService
import com.sehzadi.launcher.ai.services.NotionService
import com.sehzadi.launcher.ai.services.TavilyService
import com.sehzadi.launcher.apps.AppManager
import com.sehzadi.launcher.communication.CommunicationManager
import com.sehzadi.launcher.customization.ThemeEngine
import com.sehzadi.launcher.health.WellnessManager
import com.sehzadi.launcher.permissions.PermissionManager
import com.sehzadi.launcher.storage.StorageManager
import com.sehzadi.launcher.system.SystemMonitor
import com.sehzadi.launcher.voice.VoiceEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideStorageManager(@ApplicationContext context: Context): StorageManager {
        return StorageManager(context)
    }

    @Provides
    @Singleton
    fun provideAppManager(@ApplicationContext context: Context): AppManager {
        return AppManager(context)
    }

    @Provides
    @Singleton
    fun provideSystemMonitor(@ApplicationContext context: Context): SystemMonitor {
        return SystemMonitor(context)
    }

    @Provides
    @Singleton
    fun provideVoiceEngine(@ApplicationContext context: Context): VoiceEngine {
        return VoiceEngine(context)
    }

    @Provides
    @Singleton
    fun provideGeminiService(): GeminiService {
        return GeminiService()
    }

    @Provides
    @Singleton
    fun provideGroqService(): GroqService {
        return GroqService()
    }

    @Provides
    @Singleton
    fun provideHuggingFaceService(@ApplicationContext context: Context): HuggingFaceService {
        return HuggingFaceService(context)
    }

    @Provides
    @Singleton
    fun provideTavilyService(): TavilyService {
        return TavilyService()
    }

    @Provides
    @Singleton
    fun provideNotionService(): NotionService {
        return NotionService()
    }

    @Provides
    @Singleton
    fun provideAIEngine(
        geminiService: GeminiService,
        groqService: GroqService,
        huggingFaceService: HuggingFaceService,
        tavilyService: TavilyService,
        notionService: NotionService,
        appManager: AppManager
    ): AIEngine {
        return AIEngine(geminiService, groqService, huggingFaceService, tavilyService, notionService, appManager)
    }

    @Provides
    @Singleton
    fun provideCommunicationManager(@ApplicationContext context: Context): CommunicationManager {
        return CommunicationManager(context)
    }

    @Provides
    @Singleton
    fun provideThemeEngine(storageManager: StorageManager): ThemeEngine {
        return ThemeEngine(storageManager)
    }

    @Provides
    @Singleton
    fun providePermissionManager(@ApplicationContext context: Context): PermissionManager {
        return PermissionManager(context)
    }

    @Provides
    @Singleton
    fun provideWellnessManager(
        @ApplicationContext context: Context,
        storageManager: StorageManager
    ): WellnessManager {
        return WellnessManager(context, storageManager)
    }
}
