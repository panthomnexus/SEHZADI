package com.sehzadi.launcher.communication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {

    companion object {
        var onSmsReceived: ((String, String) -> Unit)? = null
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages?.forEach { sms ->
            val sender = sms.originatingAddress ?: "Unknown"
            val body = sms.messageBody ?: ""
            onSmsReceived?.invoke(sender, body)
        }
    }
}
