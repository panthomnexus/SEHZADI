package com.sehzadi.launcher.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class VoiceState {
    IDLE,
    LISTENING_WAKE_WORD,
    ACTIVATED,
    LISTENING_COMMAND,
    PROCESSING,
    SPEAKING
}

@Singleton
class VoiceEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var onCommandReceived: ((String) -> Unit)? = null
    private var isSessionActive = false

    companion object {
        val WAKE_WORDS = listOf("sehzadi", "hacknuma")
    }

    fun initialize() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale("hi", "IN")
                textToSpeech?.setSpeechRate(1.0f)
                textToSpeech?.setPitch(1.0f)

                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                        _voiceState.value = VoiceState.SPEAKING
                    }
                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        if (isSessionActive) {
                            _voiceState.value = VoiceState.ACTIVATED
                            startListeningForCommand()
                        } else {
                            _voiceState.value = VoiceState.IDLE
                        }
                    }
                    @Deprecated("Deprecated")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        _voiceState.value = VoiceState.IDLE
                    }
                })
            }
        }
    }

    fun setOnCommandReceived(callback: (String) -> Unit) {
        onCommandReceived = callback
    }

    fun startWakeWordListening() {
        _voiceState.value = VoiceState.LISTENING_WAKE_WORD
        startListeningInternal(isWakeWord = true)
    }

    fun startListeningForCommand() {
        if (_isSpeaking.value) return
        _voiceState.value = VoiceState.LISTENING_COMMAND
        startListeningInternal(isWakeWord = false)
    }

    fun activateSession() {
        isSessionActive = true
        _voiceState.value = VoiceState.ACTIVATED
        speak("Haan, bolo. Main SEHZADI hoon, sun rahi hoon.")
    }

    fun deactivateSession() {
        isSessionActive = false
        stopListening()
        _voiceState.value = VoiceState.IDLE
    }

    private fun startListeningInternal(isWakeWord: Boolean) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return

        stopListening()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                _recognizedText.value = text

                if (isWakeWord) {
                    val lower = text.lowercase()
                    if (WAKE_WORDS.any { lower.contains(it) }) {
                        activateSession()
                    } else {
                        startWakeWordListening()
                    }
                } else {
                    if (text.isNotBlank()) {
                        _voiceState.value = VoiceState.PROCESSING
                        onCommandReceived?.invoke(text)
                    } else if (isSessionActive) {
                        startListeningForCommand()
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { _recognizedText.value = it }
            }

            override fun onError(error: Int) {
                _isListening.value = false
                if (isSessionActive && !_isSpeaking.value) {
                    startListeningForCommand()
                } else if (isWakeWord) {
                    startWakeWordListening()
                }
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { _isListening.value = false }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            if (!isWakeWord) {
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            }
        }

        speechRecognizer?.startListening(intent)
    }

    fun speak(text: String) {
        _voiceState.value = VoiceState.SPEAKING
        _isSpeaking.value = true
        stopListening()

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "sehzadi_${System.currentTimeMillis()}")
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) { /* ignore */ }
        _isListening.value = false
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        _isSpeaking.value = false
    }

    fun destroy() {
        stopListening()
        stopSpeaking()
        textToSpeech?.shutdown()
        isSessionActive = false
    }
}
