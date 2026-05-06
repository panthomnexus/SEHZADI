# SEHZADI - Futuristic AI Launcher + HUD System

A fully functional Android launcher with JARVIS-style HUD interface, AI assistant with voice control, and real-time system monitoring.

## Features

### Core Launcher
- Load and display all installed apps
- Open apps on click
- App drawer with search + category filters
- Gesture controls (swipe up for drawer, double tap for voice)

### HUD Interface (JARVIS Style)
- Animated circular HUD with rotating rings and pulse effects
- Real-time system stats: Battery, RAM, CPU, Storage, Network speed
- Live updating - no static values
- Neon glow UI with multiple themes

### AI Core System
- **Gemini** - Main AI brain for intelligent conversations
- **Groq** - Fast responses and code generation
- **HuggingFace** - Image generation (Stable Diffusion XL)
- **Tavily** - Deep web search
- **Notion** - Persistent storage and notes sync

### Voice Assistant
- Wake word: **"Hacknuma"**
- Speech-to-text (Hindi + English + Hinglish)
- Text-to-speech responses
- Session-based continuous listening
- Natural language understanding

### Communication Control
- Incoming call detection with caller name
- Voice-controlled call handling (receive/reject)
- SMS and WhatsApp message sending
- Contact search and disambiguation

### Security
- App Lock (PIN + biometric)
- Hidden apps (secure)
- Encrypted storage for API keys

### Notes System
- Local notes with persistent storage
- Optional Notion cloud sync

### Customization
- 5 HUD themes (Neon Cyan, Neon Purple, Matrix Green, Iron Red, Gold Arc)
- Dynamic color system

### Health & Wellness
- Screen time tracking
- Continuous usage detection
- Late night usage warnings
- Break reminders

### System Control
- Real-time battery, RAM, CPU, storage monitoring
- Network speed tracking
- Quick status bar

## Architecture

```
com.sehzadi.launcher/
├── ai/                    # AI Engine + Services
│   ├── AIEngine.kt        # Main AI processing + tool calling
│   └── services/          # Gemini, Groq, HuggingFace, Tavily, Notion
├── apps/                  # App management
│   └── AppManager.kt      # Load, search, categorize, hide, lock apps
├── voice/                 # Voice system
│   ├── VoiceEngine.kt     # STT, TTS, wake word detection
│   └── VoiceListenerService.kt  # Background voice service
├── system/                # System monitoring
│   ├── SystemMonitor.kt   # Battery, RAM, CPU, storage, network
│   └── BootReceiver.kt    # Auto-start on boot
├── communication/         # Call & message handling
│   ├── CommunicationManager.kt
│   ├── IncomingCallReceiver.kt
│   └── SmsReceiver.kt
├── storage/               # Data persistence
│   └── StorageManager.kt  # Encrypted prefs, notes, history
├── health/                # Wellness monitoring
│   └── WellnessManager.kt
├── customization/         # Themes & customization
│   └── ThemeEngine.kt
├── permissions/           # Permission management
│   └── PermissionManager.kt
├── di/                    # Dependency injection
│   └── AppModule.kt
└── ui/                    # Compose UI
    ├── screens/           # All screens
    └── theme/             # Theme colors & styles
```

## Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34

### API Keys Setup
Create `local.properties` in root directory:
```properties
sdk.dir=/path/to/android/sdk

GEMINI_API_KEY=your_gemini_api_key
GROQ_API_KEY=your_groq_api_key
HUGGINGFACE_API_KEY=your_huggingface_api_key
TAVILY_API_KEY=your_tavily_api_key
NOTION_API_KEY=your_notion_api_key
NOTION_DATABASE_ID=your_notion_database_id
```

### Get API Keys
- **Gemini**: https://aistudio.google.com/app/apikey
- **Groq**: https://console.groq.com/keys
- **HuggingFace**: https://huggingface.co/settings/tokens
- **Tavily**: https://app.tavily.com/home
- **Notion**: https://www.notion.so/my-integrations

### Build
```bash
./gradlew assembleDebug
```

APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

### Install
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Permissions Required
- Microphone (voice commands)
- Contacts (call/message)
- Phone (call control)
- SMS (send/read messages)
- Camera (photo/video)
- Storage (save media)
- Notifications (alerts)

## Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose
- **DI**: Hilt (Dagger)
- **Networking**: Retrofit + OkHttp
- **Storage**: SharedPreferences + EncryptedSharedPreferences
- **Image Loading**: Coil
- **Architecture**: MVVM + Clean Architecture

## License
MIT License
