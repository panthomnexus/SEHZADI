package com.sehzadi.launcher.communication

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.TelecomManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class ContactInfo(
    val name: String,
    val phoneNumber: String,
    val id: String
)

@Singleton
class CommunicationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun findContacts(name: String): List<ContactInfo> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<ContactInfo>()
        val cursor: Cursor? = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID
            ),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"),
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                contacts.add(
                    ContactInfo(
                        name = it.getString(0) ?: "Unknown",
                        phoneNumber = it.getString(1) ?: "",
                        id = it.getString(2) ?: ""
                    )
                )
            }
        }

        contacts.distinctBy { it.phoneNumber }
    }

    fun getContactNameByNumber(number: String): String? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null, null, null
        )

        return cursor?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }

    fun makeCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun sendSms(phoneNumber: String, message: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun sendWhatsApp(phoneNumber: String, message: String) {
        try {
            val formattedNumber = phoneNumber.replace("+", "").replace(" ", "")
            val uri = Uri.parse("https://wa.me/$formattedNumber?text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            sendSms(phoneNumber, message)
        }
    }

    fun answerCall() {
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            telecomManager.acceptRingingCall()
        } catch (e: SecurityException) { /* Permission not granted */ }
    }

    fun rejectCall() {
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            telecomManager.endCall()
        } catch (e: SecurityException) { /* Permission not granted */ }
    }
}
