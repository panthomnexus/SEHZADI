package com.sehzadi.launcher.communication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class IncomingCallReceiver : BroadcastReceiver() {

    companion object {
        var onIncomingCall: ((String) -> Unit)? = null
        var onCallEnded: (() -> Unit)? = null
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: "Unknown"

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                onIncomingCall?.invoke(number)
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                onCallEnded?.invoke()
            }
        }
    }
}
