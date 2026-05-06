package com.sehzadi.launcher.core

import com.sehzadi.launcher.apps.AppManager
import com.sehzadi.launcher.communication.CommunicationManager
import com.sehzadi.launcher.services.CameraService
import com.sehzadi.launcher.services.StockService
import com.sehzadi.launcher.services.GalleryService
import com.sehzadi.launcher.services.WidgetService
import com.sehzadi.launcher.services.ScreenAIService
import com.sehzadi.launcher.services.UsageMonitorService
import com.sehzadi.launcher.services.TtsService
import com.sehzadi.launcher.ai.services.HuggingFaceService
import com.sehzadi.launcher.ai.services.TavilyService
import com.sehzadi.launcher.ai.services.GroqService
import com.sehzadi.launcher.ai.services.NotionService
import com.sehzadi.launcher.storage.StorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionExecutor @Inject constructor(
    private val appManager: AppManager,
    private val communicationManager: CommunicationManager,
    private val cameraService: CameraService,
    private val stockService: StockService,
    private val galleryService: GalleryService,
    private val widgetService: WidgetService,
    private val screenAIService: ScreenAIService,
    private val usageMonitorService: UsageMonitorService,
    private val ttsService: TtsService,
    private val huggingFaceService: HuggingFaceService,
    private val tavilyService: TavilyService,
    private val groqService: GroqService,
    private val notionService: NotionService,
    private val storageManager: StorageManager
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    var onResultCallback: ((String, String?) -> Unit)? = null

    fun execute(action: Action) {
        scope.launch {
            executeAction(action)
        }
    }

    private suspend fun executeAction(action: Action) {
        when (action) {
            is Action.OpenApp -> {
                val app = appManager.findAppByName(action.appName)
                if (app != null) {
                    appManager.launchApp(app.packageName)
                    ttsService.speak("${app.appName} open kar diya.")
                    onResultCallback?.invoke("${app.appName} opened.", null)
                } else {
                    ttsService.speak("${action.appName} nahi mila.")
                    onResultCallback?.invoke("App '${action.appName}' not found.", null)
                }
            }

            is Action.CallContact -> {
                val contacts = communicationManager.findContacts(action.contactName)
                when {
                    contacts.isEmpty() -> {
                        ttsService.speak("${action.contactName} naam ka contact nahi mila.")
                        onResultCallback?.invoke("Contact '${action.contactName}' not found.", null)
                    }
                    contacts.size > 1 -> {
                        val names = contacts.take(3).joinToString(", ") { it.name }
                        ttsService.speak("Kaun sa contact? $names")
                        onResultCallback?.invoke("Multiple contacts found: $names. Please specify.", null)
                    }
                    else -> {
                        ttsService.speak("${contacts.first().name} ko call kar raha hoon.")
                        communicationManager.makeCall(contacts.first().phoneNumber)
                        onResultCallback?.invoke("Calling ${contacts.first().name}...", null)
                    }
                }
            }

            is Action.SendMessage -> {
                val contacts = communicationManager.findContacts(action.contactName)
                when {
                    contacts.isEmpty() -> {
                        ttsService.speak("${action.contactName} contact nahi mila.")
                        onResultCallback?.invoke("Contact not found.", null)
                    }
                    contacts.size > 1 -> {
                        val names = contacts.take(3).joinToString(", ") { it.name }
                        ttsService.speak("Kaun sa contact? $names")
                        onResultCallback?.invoke("Multiple contacts: $names", null)
                    }
                    else -> {
                        communicationManager.sendSms(contacts.first().phoneNumber, action.message)
                        ttsService.speak("${contacts.first().name} ko message bhej diya.")
                        onResultCallback?.invoke("Message sent to ${contacts.first().name}.", null)
                    }
                }
            }

            is Action.SendWhatsApp -> {
                val contacts = communicationManager.findContacts(action.contactName)
                when {
                    contacts.isEmpty() -> {
                        ttsService.speak("${action.contactName} contact nahi mila.")
                        onResultCallback?.invoke("Contact not found.", null)
                    }
                    else -> {
                        communicationManager.sendWhatsApp(contacts.first().phoneNumber, action.message)
                        ttsService.speak("WhatsApp message bhej raha hoon.")
                        onResultCallback?.invoke("WhatsApp message to ${contacts.first().name}.", null)
                    }
                }
            }

            Action.TakePhoto -> {
                cameraService.capturePhoto()
                ttsService.speak("Photo le raha hoon.")
                onResultCallback?.invoke("Camera opened for photo.", null)
            }

            Action.RecordVideo -> {
                cameraService.recordVideo()
                ttsService.speak("Video record shuru kar raha hoon.")
                onResultCallback?.invoke("Camera opened for video.", null)
            }

            is Action.AnalyzeStock -> {
                ttsService.speak("${action.ticker} ka analysis kar raha hoon.")
                val result = stockService.analyzeTicker(action.ticker)
                ttsService.speak(result.take(200))
                onResultCallback?.invoke(result, null)
            }

            Action.ShowGallery -> {
                galleryService.openGallery()
                onResultCallback?.invoke("Gallery opened.", null)
            }

            Action.ShowLiveClock -> {
                widgetService.showLiveClock()
                ttsService.speak("Live clock widget add kar diya.")
                onResultCallback?.invoke("Live clock widget shown.", null)
            }

            Action.ShowSystemStats -> {
                widgetService.showSystemStats()
                ttsService.speak("System stats dikhaa raha hoon.")
                onResultCallback?.invoke("System stats shown.", null)
            }

            Action.ShowSystemHelp -> {
                ttsService.speak("System check kar raha hoon.")
                val report = usageMonitorService.diagnoseSystem()
                ttsService.speak(report.take(200))
                onResultCallback?.invoke(report, null)
            }

            is Action.ScreenAssist -> {
                ttsService.speak("Screen analyze kar raha hoon.")
                val guidance = screenAIService.analyzeScreenAndGuide(action.query)
                ttsService.speak(guidance.take(200))
                onResultCallback?.invoke(guidance, null)
            }

            is Action.GenerateImage -> {
                ttsService.speak("Image generate kar raha hoon.")
                try {
                    val path = huggingFaceService.generateImage(action.prompt)
                    galleryService.saveImagePath(path)
                    ttsService.speak("Image ban gayi hai.")
                    onResultCallback?.invoke("Image generated!", path)
                } catch (e: Exception) {
                    ttsService.speak("Image generate nahi ho paayi.")
                    onResultCallback?.invoke("Error: ${e.message}", null)
                }
            }

            is Action.WebSearch -> {
                ttsService.speak("Search kar raha hoon.")
                try {
                    val results = tavilyService.search(action.query, 3)
                    val summary = results.joinToString("\n\n") { "**${it.title}**\n${it.content.take(150)}" }
                    ttsService.speak("Search results mil gaye.")
                    onResultCallback?.invoke(summary, null)
                } catch (e: Exception) {
                    onResultCallback?.invoke("Search failed: ${e.message}", null)
                }
            }

            is Action.SaveNote -> {
                storageManager.saveNote("note_${System.currentTimeMillis()}", action.title, action.content)
                ttsService.speak("Note save ho gaya.")
                onResultCallback?.invoke("Note saved: ${action.title}", null)
                try { notionService.saveNote(action.title, action.content) } catch (_: Exception) {}
            }

            is Action.GenerateCode -> {
                ttsService.speak("Code generate kar raha hoon.")
                try {
                    val code = groqService.generateCode(action.prompt, action.language)
                    ttsService.speak("Code ready hai.")
                    onResultCallback?.invoke(code, null)
                } catch (e: Exception) {
                    onResultCallback?.invoke("Code generation failed: ${e.message}", null)
                }
            }

            Action.ToggleWifi, Action.ToggleBluetooth -> {
                ttsService.speak("Settings open kar raha hoon.")
                onResultCallback?.invoke("Opening system settings.", null)
            }

            Action.ShowWeather -> {
                try {
                    val weather = tavilyService.search("current weather forecast today", 1)
                    val info = weather.firstOrNull()?.content ?: "Weather info not available."
                    ttsService.speak(info.take(200))
                    onResultCallback?.invoke(info, null)
                } catch (e: Exception) {
                    onResultCallback?.invoke("Weather unavailable.", null)
                }
            }

            is Action.ReadMessages -> {
                ttsService.speak("Messages feature coming soon.")
                onResultCallback?.invoke("Reading messages...", null)
            }

            Action.ShowLiveNotes -> {
                widgetService.showLiveNotes()
                ttsService.speak("Notes widget dikha raha hoon.")
                onResultCallback?.invoke("Notes widget shown.", null)
            }

            is Action.SaveMemory -> {
                storageManager.saveNote(action.key, action.key, action.value)
                ttsService.speak("Yaad rakh liya: ${action.key}")
                onResultCallback?.invoke("Memory saved: ${action.key} = ${action.value}", null)
            }

            Action.ShowPermissions -> {
                ttsService.speak("Permissions screen dikha raha hoon.")
                onResultCallback?.invoke("Opening permissions manager.", null)
            }

            Action.Unknown -> {
                ttsService.speak("Samajh nahi aaya. Dobara bol do.")
                onResultCallback?.invoke("Command not recognized.", null)
            }
        }
    }
}
