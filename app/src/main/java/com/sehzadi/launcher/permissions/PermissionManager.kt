package com.sehzadi.launcher.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class PermissionInfo(
    val permission: String,
    val name: String,
    val description: String,
    val isGranted: Boolean,
    val isRequired: Boolean = true
)

@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getRequiredPermissions(): List<PermissionInfo> {
        return listOf(
            PermissionInfo(
                Manifest.permission.RECORD_AUDIO,
                "Microphone",
                "Voice commands aur wake word detection ke liye zaroori hai",
                isGranted = isGranted(Manifest.permission.RECORD_AUDIO)
            ),
            PermissionInfo(
                Manifest.permission.READ_CONTACTS,
                "Contacts",
                "Call aur message bhejne ke liye contacts access chahiye",
                isGranted = isGranted(Manifest.permission.READ_CONTACTS)
            ),
            PermissionInfo(
                Manifest.permission.CALL_PHONE,
                "Phone",
                "Voice se call lagane ke liye phone permission chahiye",
                isGranted = isGranted(Manifest.permission.CALL_PHONE)
            ),
            PermissionInfo(
                Manifest.permission.SEND_SMS,
                "SMS",
                "Message bhejne ke liye SMS permission chahiye",
                isGranted = isGranted(Manifest.permission.SEND_SMS)
            ),
            PermissionInfo(
                Manifest.permission.CAMERA,
                "Camera",
                "Photo aur video capture karne ke liye camera access chahiye",
                isGranted = isGranted(Manifest.permission.CAMERA)
            ),
            PermissionInfo(
                Manifest.permission.READ_PHONE_STATE,
                "Phone State",
                "Incoming calls detect karne ke liye zaroori hai",
                isGranted = isGranted(Manifest.permission.READ_PHONE_STATE)
            ),
            PermissionInfo(
                Manifest.permission.READ_CALL_LOG,
                "Call Log",
                "Call history dekhne ke liye",
                isGranted = isGranted(Manifest.permission.READ_CALL_LOG),
                isRequired = false
            )
        ) + if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                PermissionInfo(
                    Manifest.permission.POST_NOTIFICATIONS,
                    "Notifications",
                    "Alerts aur voice listener notification ke liye",
                    isGranted = isGranted(Manifest.permission.POST_NOTIFICATIONS)
                )
            )
        } else emptyList()
    }

    fun getMissingPermissions(): List<String> {
        return getRequiredPermissions()
            .filter { it.isRequired && !it.isGranted }
            .map { it.permission }
    }

    fun areAllRequiredPermissionsGranted(): Boolean {
        return getMissingPermissions().isEmpty()
    }

    private fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
