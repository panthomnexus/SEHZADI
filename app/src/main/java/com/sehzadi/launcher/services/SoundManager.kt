package com.sehzadi.launcher.services

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.sehzadi.launcher.R
import javax.inject.Inject
import javax.inject.Singleton

enum class SoundEffect {
    TAP,
    AI_RESPONSE,
    NOTIFICATION,
    WAKE_WORD,
    SUCCESS,
    ERROR,
    SCAN,
    POWER_UP,
    TYPING,
    SWIPE
}

@Singleton
class SoundManager @Inject constructor(
    private val context: Context
) {
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<SoundEffect, Int>()
    private var isInitialized = false
    private var isMuted = false

    fun initialize() {
        if (isInitialized) return

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool?.let { pool ->
            soundMap[SoundEffect.TAP] = pool.load(context, R.raw.sfx_tap, 1)
            soundMap[SoundEffect.AI_RESPONSE] = pool.load(context, R.raw.sfx_ai_response, 1)
            soundMap[SoundEffect.NOTIFICATION] = pool.load(context, R.raw.sfx_notification, 1)
            soundMap[SoundEffect.WAKE_WORD] = pool.load(context, R.raw.sfx_wake_word, 1)
            soundMap[SoundEffect.SUCCESS] = pool.load(context, R.raw.sfx_success, 1)
            soundMap[SoundEffect.ERROR] = pool.load(context, R.raw.sfx_error, 1)
            soundMap[SoundEffect.SCAN] = pool.load(context, R.raw.sfx_scan, 1)
            soundMap[SoundEffect.POWER_UP] = pool.load(context, R.raw.sfx_power_up, 1)
            soundMap[SoundEffect.TYPING] = pool.load(context, R.raw.sfx_typing, 1)
            soundMap[SoundEffect.SWIPE] = pool.load(context, R.raw.sfx_swipe, 1)
        }

        isInitialized = true
    }

    fun play(effect: SoundEffect, volume: Float = 0.7f) {
        if (isMuted || !isInitialized) return
        soundMap[effect]?.let { soundId ->
            soundPool?.play(soundId, volume, volume, 1, 0, 1.0f)
        }
    }

    fun playTap() = play(SoundEffect.TAP, 0.5f)
    fun playAIResponse() = play(SoundEffect.AI_RESPONSE, 0.6f)
    fun playNotification() = play(SoundEffect.NOTIFICATION, 0.7f)
    fun playWakeWord() = play(SoundEffect.WAKE_WORD, 0.8f)
    fun playSuccess() = play(SoundEffect.SUCCESS, 0.6f)
    fun playError() = play(SoundEffect.ERROR, 0.6f)
    fun playScan() = play(SoundEffect.SCAN, 0.5f)
    fun playPowerUp() = play(SoundEffect.POWER_UP, 0.7f)
    fun playTyping() = play(SoundEffect.TYPING, 0.3f)
    fun playSwipe() = play(SoundEffect.SWIPE, 0.4f)

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        return isMuted
    }

    fun destroy() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
        isInitialized = false
    }
}
