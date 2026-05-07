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
import com.sehzadi.launcher.data.MemoryStore
import com.sehzadi.launcher.data.SettingsStore
import com.sehzadi.launcher.core.ActionExecutor
import com.sehzadi.launcher.core.IntentRouter
import com.sehzadi.launcher.customization.ThemeEngine
import com.sehzadi.launcher.health.WellnessManager
import com.sehzadi.launcher.permissions.PermissionManager
import com.sehzadi.launcher.services.CameraService
import com.sehzadi.launcher.services.GalleryService
import com.sehzadi.launcher.services.ScreenAIService
import com.sehzadi.launcher.services.StockService
import com.sehzadi.launcher.services.TtsService
import com.sehzadi.launcher.services.UsageMonitorService
import com.sehzadi.launcher.services.WidgetService
import com.sehzadi.launcher.ai.models.DeviceCapabilityDetector
import com.sehzadi.launcher.ai.models.HybridAIEngine
import com.sehzadi.launcher.ai.models.ModelManager
import com.sehzadi.launcher.ai.models.ProactiveAIService
import com.sehzadi.launcher.services.SoundManager
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
    fun provideGeminiService(storageManager: StorageManager): GeminiService {
        return GeminiService(storageManager)
    }

    @Provides
    @Singleton
    fun provideGroqService(storageManager: StorageManager): GroqService {
        return GroqService(storageManager)
    }

    @Provides
    @Singleton
    fun provideHuggingFaceService(
        @ApplicationContext context: Context,
        storageManager: StorageManager
    ): HuggingFaceService {
        return HuggingFaceService(context, storageManager)
    }

    @Provides
    @Singleton
    fun provideTavilyService(storageManager: StorageManager): TavilyService {
        return TavilyService(storageManager)
    }

    @Provides
    @Singleton
    fun provideNotionService(storageManager: StorageManager): NotionService {
        return NotionService(storageManager)
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

    // New services

    @Provides
    @Singleton
    fun provideTtsService(@ApplicationContext context: Context): TtsService {
        return TtsService(context)
    }

    @Provides
    @Singleton
    fun provideGalleryService(@ApplicationContext context: Context): GalleryService {
        return GalleryService(context)
    }

    @Provides
    @Singleton
    fun provideCameraService(
        @ApplicationContext context: Context,
        galleryService: GalleryService
    ): CameraService {
        return CameraService(context, galleryService)
    }

    @Provides
    @Singleton
    fun provideStockService(
        geminiService: GeminiService,
        tavilyService: TavilyService
    ): StockService {
        return StockService(geminiService, tavilyService)
    }

    @Provides
    @Singleton
    fun provideScreenAIService(
        @ApplicationContext context: Context,
        geminiService: GeminiService
    ): ScreenAIService {
        return ScreenAIService(context, geminiService)
    }

    @Provides
    @Singleton
    fun provideWidgetService(@ApplicationContext context: Context): WidgetService {
        return WidgetService(context)
    }

    @Provides
    @Singleton
    fun provideUsageMonitorService(
        @ApplicationContext context: Context,
        systemMonitor: SystemMonitor,
        geminiService: GeminiService
    ): UsageMonitorService {
        return UsageMonitorService(context, systemMonitor, geminiService)
    }

    @Provides
    @Singleton
    fun provideIntentRouter(): IntentRouter {
        return IntentRouter()
    }

    @Provides
    @Singleton
    fun provideActionExecutor(
        appManager: AppManager,
        communicationManager: CommunicationManager,
        cameraService: CameraService,
        stockService: StockService,
        galleryService: GalleryService,
        widgetService: WidgetService,
        screenAIService: ScreenAIService,
        usageMonitorService: UsageMonitorService,
        ttsService: TtsService,
        huggingFaceService: HuggingFaceService,
        tavilyService: TavilyService,
        groqService: GroqService,
        notionService: NotionService,
        storageManager: StorageManager
    ): ActionExecutor {
        return ActionExecutor(
            appManager, communicationManager, cameraService, stockService,
            galleryService, widgetService, screenAIService, usageMonitorService,
            ttsService, huggingFaceService, tavilyService, groqService,
            notionService, storageManager
        )
    }

    @Provides
    @Singleton
    fun provideMemoryStore(@ApplicationContext context: Context): MemoryStore {
        return MemoryStore(context)
    }

    @Provides
    @Singleton
    fun provideSettingsStore(@ApplicationContext context: Context): SettingsStore {
        return SettingsStore(context)
    }

    @Provides
    @Singleton
    fun provideSoundManager(@ApplicationContext context: Context): SoundManager {
        return SoundManager(context)
    }

    @Provides
    @Singleton
    fun provideDeviceCapabilityDetector(@ApplicationContext context: Context): DeviceCapabilityDetector {
        return DeviceCapabilityDetector(context)
    }

    @Provides
    @Singleton
    fun provideModelManager(
        @ApplicationContext context: Context,
        capabilityDetector: DeviceCapabilityDetector
    ): ModelManager {
        return ModelManager(context, capabilityDetector)
    }

    @Provides
    @Singleton
    fun provideHybridAIEngine(
        @ApplicationContext context: Context,
        modelManager: ModelManager,
        aiEngine: AIEngine
    ): HybridAIEngine {
        return HybridAIEngine(context, modelManager, aiEngine)
    }

    @Provides
    @Singleton
    fun provideProactiveAIService(
        @ApplicationContext context: Context,
        ttsService: TtsService
    ): ProactiveAIService {
        return ProactiveAIService(context, ttsService)
    }
}
