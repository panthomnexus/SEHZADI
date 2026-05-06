package com.sehzadi.launcher.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sehzadi.launcher.voice.VoiceListenerService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            context?.let {
                val serviceIntent = Intent(it, VoiceListenerService::class.java)
                it.startForegroundService(serviceIntent)
            }
        }
    }
}
