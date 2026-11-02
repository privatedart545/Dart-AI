package com.example.platform

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class InstalledAppItem(
    val name: String,
    val packageName: String,
    val isSystemApp: Boolean
)

data class AppDetails(
    val name: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val firstInstallTime: String,
    val lastUpdateTime: String,
    val isSystemApp: Boolean
)

class AppLaunchManager(
    private val context: Context,
    var appLockChecker: ((String) -> Boolean)? = null
) {

    private val packageManager: PackageManager = context.packageManager

    fun listInstalledApps(filter: String? = null): List<InstalledAppItem> {
        val launchIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList = packageManager.queryIntentActivities(launchIntent, 0)
        val apps = resolveInfoList.mapNotNull { resolveInfo ->
            val appName = resolveInfo.loadLabel(packageManager)?.toString()?.trim()
            val packageName = resolveInfo.activityInfo?.packageName
            if (!appName.isNullOrBlank() && !packageName.isNullOrBlank()) {
                val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                InstalledAppItem(appName, packageName, isSystem)
            } else null
        }.distinctBy { it.packageName }

        return if (!filter.isNullOrBlank()) {
            val q = filter.lowercase(Locale.ROOT)
            apps.filter { it.name.lowercase(Locale.ROOT).contains(q) || it.packageName.lowercase(Locale.ROOT).contains(q) }
        } else {
            apps.sortedBy { it.name }
        }
    }

    fun openApp(query: String): Result<String> {
        val trimmed = query.trim()
        val allApps = listInstalledApps()

        // 1. Direct match by packageName
        var targetPackage = allApps.firstOrNull { it.packageName.equals(trimmed, ignoreCase = true) }?.packageName

        // 2. Exact match by label
        if (targetPackage == null) {
            targetPackage = allApps.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }?.packageName
        }

        // 3. Substring match by label
        if (targetPackage == null) {
            val q = trimmed.lowercase(Locale.ROOT)
            targetPackage = allApps.firstOrNull { it.name.lowercase(Locale.ROOT).contains(q) }?.packageName
        }

        if (targetPackage == null) {
            return Result.failure(
                Exception("Could not find an installed app matching '$query'. Available launchable apps include: ${allApps.take(6).joinToString { it.name }}...")
            )
        }

        val matchedName = allApps.firstOrNull { it.packageName == targetPackage }?.name ?: targetPackage
        if (appLockChecker?.invoke(targetPackage) == true) {
            return Result.failure(
                Exception("SECURITY LOCK: App '$matchedName' ($targetPackage) is locked with DEV AI Biometric Security. Unlock it using fingerprint or PIN to proceed.")
            )
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
            ?: return Result.failure(Exception("App '$targetPackage' is installed but cannot be launched directly."))

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return Result.success("Successfully launched $matchedName ($targetPackage).")
    }

    fun getAppInfo(query: String): Result<AppDetails> {
        val trimmed = query.trim()
        val allApps = listInstalledApps()
        val targetPackage = allApps.firstOrNull {
            it.packageName.equals(trimmed, ignoreCase = true) ||
            it.name.equals(trimmed, ignoreCase = true) ||
            it.name.lowercase(Locale.ROOT).contains(trimmed.lowercase(Locale.ROOT))
        }?.packageName ?: trimmed

        return try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(targetPackage, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(targetPackage, 0)
            }
            val appName = packageInfo.applicationInfo?.loadLabel(packageManager)?.toString() ?: targetPackage
            val isSystem = (packageInfo.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            val details = AppDetails(
                name = appName,
                packageName = packageInfo.packageName,
                versionName = packageInfo.versionName ?: "Unknown",
                versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                },
                firstInstallTime = sdf.format(Date(packageInfo.firstInstallTime)),
                lastUpdateTime = sdf.format(Date(packageInfo.lastUpdateTime)),
                isSystemApp = isSystem
            )
            Result.success(details)
        } catch (e: Exception) {
            Result.failure(Exception("Unable to find info for package '$targetPackage': ${e.localizedMessage}"))
        }
    }

    fun openAppSettings(query: String): Result<String> {
        val trimmed = query.trim()
        val allApps = listInstalledApps()
        val targetPackage = allApps.firstOrNull {
            it.packageName.equals(trimmed, ignoreCase = true) ||
            it.name.equals(trimmed, ignoreCase = true) ||
            it.name.lowercase(Locale.ROOT).contains(trimmed.lowercase(Locale.ROOT))
        }?.packageName ?: trimmed

        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", targetPackage, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened system settings for $targetPackage.")
        } catch (e: Exception) {
            Result.failure(Exception("Unable to open app settings: ${e.localizedMessage}"))
        }
    }

    fun deepLinkApp(
        appName: String,
        action: String = "search",
        query: String? = null,
        directUriString: String? = null
    ): Result<String> {
        val cleanQuery = query?.trim() ?: ""
        val app = appName.trim().lowercase(Locale.ROOT)

        val uri: Uri = when {
            !directUriString.isNullOrBlank() -> Uri.parse(directUriString)
            app.contains("youtube") -> {
                if (action == "search" || cleanQuery.isNotBlank()) {
                    Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(cleanQuery)}")
                } else {
                    Uri.parse("https://www.youtube.com")
                }
            }
            app.contains("spotify") -> {
                if (cleanQuery.isNotBlank()) {
                    Uri.parse("spotify:search:${Uri.encode(cleanQuery)}")
                } else {
                    Uri.parse("spotify:")
                }
            }
            app.contains("whatsapp") -> {
                val phone = cleanQuery.filter { it.isDigit() }
                if (phone.isNotBlank()) {
                    Uri.parse("https://api.whatsapp.com/send?phone=$phone")
                } else {
                    Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(cleanQuery)}")
                }
            }
            app.contains("map") || app.contains("navigation") -> {
                if (action == "navigate" && cleanQuery.isNotBlank()) {
                    Uri.parse("google.navigation:q=${Uri.encode(cleanQuery)}")
                } else if (cleanQuery.isNotBlank()) {
                    Uri.parse("geo:0,0?q=${Uri.encode(cleanQuery)}")
                } else {
                    Uri.parse("geo:0,0")
                }
            }
            app.contains("slack") -> {
                Uri.parse("slack://open")
            }
            app.contains("twitter") || app.contains(" x") -> {
                if (cleanQuery.isNotBlank()) {
                    Uri.parse("https://x.com/search?q=${Uri.encode(cleanQuery)}")
                } else {
                    Uri.parse("https://x.com")
                }
            }
            app.contains("gmail") || app.contains("email") -> {
                if (cleanQuery.contains("@")) {
                    Uri.parse("mailto:$cleanQuery")
                } else {
                    Uri.parse("mailto:")
                }
            }
            app.contains("reddit") -> {
                if (cleanQuery.isNotBlank()) {
                    Uri.parse("https://reddit.com/r/${Uri.encode(cleanQuery)}")
                } else {
                    Uri.parse("https://reddit.com")
                }
            }
            app.contains("github") -> {
                if (cleanQuery.isNotBlank()) {
                    Uri.parse("https://github.com/search?q=${Uri.encode(cleanQuery)}")
                } else {
                    Uri.parse("https://github.com")
                }
            }
            app.contains("telegram") -> {
                if (cleanQuery.isNotBlank()) {
                    Uri.parse("https://t.me/${cleanQuery.removePrefix("@")}")
                } else {
                    Uri.parse("tg:")
                }
            }
            else -> {
                if (cleanQuery.startsWith("http://") || cleanQuery.startsWith("https://")) {
                    Uri.parse(cleanQuery)
                } else {
                    return openApp(appName)
                }
            }
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Deep linked into $appName with action '$action' ($uri).")
        } catch (e: Exception) {
            // Fallback to opening regular app
            val fallback = openApp(appName)
            if (fallback.isSuccess) {
                Result.success("Deep link handler launched app directly ($appName).")
            } else {
                Result.failure(Exception("Deep link failed for $appName: ${e.localizedMessage}"))
            }
        }
    }
}
