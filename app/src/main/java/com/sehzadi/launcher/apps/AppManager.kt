package com.sehzadi.launcher.apps

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.UserManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val category: String = "Other",
    val isHidden: Boolean = false,
    val isLocked: Boolean = false,
    val usageCount: Long = 0
)

@Singleton
class AppManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _hiddenApps = MutableStateFlow<Set<String>>(emptySet())
    val hiddenApps: StateFlow<Set<String>> = _hiddenApps.asStateFlow()

    private val _lockedApps = MutableStateFlow<Set<String>>(emptySet())
    val lockedApps: StateFlow<Set<String>> = _lockedApps.asStateFlow()

    private val packageManager: PackageManager = context.packageManager

    suspend fun loadInstalledApps() = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList = packageManager.queryIntentActivities(intent, 0)
        val apps = resolveInfoList.mapNotNull { resolveInfo ->
            val pkgName = resolveInfo.activityInfo.packageName
            if (pkgName == context.packageName) return@mapNotNull null
            AppInfo(
                packageName = pkgName,
                appName = resolveInfo.loadLabel(packageManager).toString(),
                icon = resolveInfo.loadIcon(packageManager),
                category = categorizeApp(pkgName),
                isHidden = _hiddenApps.value.contains(pkgName),
                isLocked = _lockedApps.value.contains(pkgName)
            )
        }.sortedBy { it.appName.lowercase() }

        _installedApps.value = apps
    }

    fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    }

    fun searchApps(query: String): List<AppInfo> {
        val lowerQuery = query.lowercase()
        return _installedApps.value.filter { app ->
            !app.isHidden && app.appName.lowercase().contains(lowerQuery)
        }
    }

    fun getAppsByCategory(category: String): List<AppInfo> {
        return _installedApps.value.filter {
            !it.isHidden && it.category == category
        }
    }

    fun getCategories(): List<String> {
        return _installedApps.value
            .filter { !it.isHidden }
            .map { it.category }
            .distinct()
            .sorted()
    }

    fun hideApp(packageName: String) {
        _hiddenApps.value = _hiddenApps.value + packageName
        refreshAppStates()
    }

    fun unhideApp(packageName: String) {
        _hiddenApps.value = _hiddenApps.value - packageName
        refreshAppStates()
    }

    fun lockApp(packageName: String) {
        _lockedApps.value = _lockedApps.value + packageName
        refreshAppStates()
    }

    fun unlockApp(packageName: String) {
        _lockedApps.value = _lockedApps.value - packageName
        refreshAppStates()
    }

    fun findAppByName(name: String): AppInfo? {
        val lowerName = name.lowercase()
        return _installedApps.value.firstOrNull { app ->
            app.appName.lowercase().contains(lowerName)
        }
    }

    private fun refreshAppStates() {
        _installedApps.value = _installedApps.value.map { app ->
            app.copy(
                isHidden = _hiddenApps.value.contains(app.packageName),
                isLocked = _lockedApps.value.contains(app.packageName)
            )
        }
    }

    private fun categorizeApp(packageName: String): String {
        return when {
            packageName.contains("game", ignoreCase = true) ||
            packageName.contains("play", ignoreCase = true) -> "Games"

            packageName.contains("social", ignoreCase = true) ||
            packageName.contains("facebook", ignoreCase = true) ||
            packageName.contains("instagram", ignoreCase = true) ||
            packageName.contains("twitter", ignoreCase = true) ||
            packageName.contains("whatsapp", ignoreCase = true) ||
            packageName.contains("telegram", ignoreCase = true) -> "Social"

            packageName.contains("camera", ignoreCase = true) ||
            packageName.contains("photo", ignoreCase = true) ||
            packageName.contains("gallery", ignoreCase = true) -> "Media"

            packageName.contains("music", ignoreCase = true) ||
            packageName.contains("spotify", ignoreCase = true) ||
            packageName.contains("audio", ignoreCase = true) -> "Music"

            packageName.contains("chrome", ignoreCase = true) ||
            packageName.contains("browser", ignoreCase = true) ||
            packageName.contains("firefox", ignoreCase = true) -> "Browser"

            packageName.contains("mail", ignoreCase = true) ||
            packageName.contains("gmail", ignoreCase = true) ||
            packageName.contains("outlook", ignoreCase = true) -> "Email"

            packageName.contains("maps", ignoreCase = true) ||
            packageName.contains("navigation", ignoreCase = true) -> "Navigation"

            packageName.contains("settings", ignoreCase = true) ||
            packageName.contains("system", ignoreCase = true) -> "System"

            packageName.contains("shop", ignoreCase = true) ||
            packageName.contains("amazon", ignoreCase = true) ||
            packageName.contains("flipkart", ignoreCase = true) -> "Shopping"

            packageName.contains("bank", ignoreCase = true) ||
            packageName.contains("pay", ignoreCase = true) ||
            packageName.contains("money", ignoreCase = true) -> "Finance"

            else -> "Other"
        }
    }
}
