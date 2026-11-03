package com.example.platform

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings
import java.util.Calendar
import java.util.Locale

data class AppUsageStat(
    val appName: String,
    val packageName: String,
    val foregroundTimeMillis: Long,
    val formattedDuration: String,
    val percentageOfTotal: Int
)

data class DigitalWellbeingAudit(
    val isPermissionGranted: Boolean,
    val totalScreenTimeMillis: Long,
    val formattedTotalScreenTime: String,
    val topApps: List<AppUsageStat>,
    val auditSummary: String,
    val recommendation: String
)

class AppUsageAuditorManager(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    fun isUsagePermissionGranted(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings(): Result<String> {
        return try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened Android Usage Access Settings. Please enable access for Dart AI to unlock real-time App Usage and Digital Wellbeing auditing.")
        } catch (e: Exception) {
            Result.failure(Exception("Unable to open Usage Settings: ${e.localizedMessage}"))
        }
    }

    fun auditDigitalWellbeing(days: Int = 1): DigitalWellbeingAudit {
        if (!isUsagePermissionGranted() || usageStatsManager == null) {
            return DigitalWellbeingAudit(
                isPermissionGranted = false,
                totalScreenTimeMillis = 0L,
                formattedTotalScreenTime = "Permission required",
                topApps = emptyList(),
                auditSummary = "Usage Access permission is not yet granted. Dart AI needs this permission to inspect screen time and app usage stats.",
                recommendation = "Call 'open_usage_access_settings' to grant permission in Android Settings."
            )
        }

        val cal = Calendar.getInstance()
        val endTime = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -days.coerceAtLeast(1))
        val startTime = cal.timeInMillis

        val usageStatsList: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: emptyList()

        // Aggregate by package name
        val aggregated = mutableMapOf<String, Long>()
        for (stat in usageStatsList) {
            val time = stat.totalTimeInForeground
            if (time > 0) {
                aggregated[stat.packageName] = (aggregated[stat.packageName] ?: 0L) + time
            }
        }

        // Filter out system launchers and current app
        val userAppsUsage = aggregated.filterKeys { pkg ->
            pkg != context.packageName && !pkg.contains("launcher") && !pkg.contains("systemui")
        }

        val totalScreenTime = userAppsUsage.values.sum()
        val sortedApps = userAppsUsage.toList()
            .sortedByDescending { it.second }
            .take(10)

        val appUsageStats = sortedApps.map { (pkg, time) ->
            val appName = try {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                pkg.substringAfterLast('.')
            }
            val percentage = if (totalScreenTime > 0) ((time.toDouble() / totalScreenTime) * 100).toInt() else 0
            AppUsageStat(
                appName = appName,
                packageName = pkg,
                foregroundTimeMillis = time,
                formattedDuration = formatDuration(time),
                percentageOfTotal = percentage
            )
        }

        val totalFormatted = formatDuration(totalScreenTime)
        val hours = totalScreenTime / (1000 * 60 * 60)

        val (summary, recommendation) = when {
            hours >= 6 -> {
                Pair(
                    "⚠️ High Screen Time Alert: You have spent $totalFormatted on your phone today.",
                    "Your usage is notably above average. Consider enabling Dart AI Focus Mode, activating Smart DND, or setting app timers for your top distracting apps."
                )
            }
            hours >= 3 -> {
                Pair(
                    "📊 Moderate Screen Time: You have used your phone for $totalFormatted today.",
                    "Healthy usage balance! Make sure to take 5-minute eye breaks every 45 minutes of continuous screen usage."
                )
            }
            else -> {
                Pair(
                    "✨ Excellent Digital Wellbeing: Only $totalFormatted of screen time recorded today.",
                    "Great work staying present in the physical world! Your digital habits are in the optimal wellness zone."
                )
            }
        }

        return DigitalWellbeingAudit(
            isPermissionGranted = true,
            totalScreenTimeMillis = totalScreenTime,
            formattedTotalScreenTime = totalFormatted,
            topApps = appUsageStats,
            auditSummary = summary,
            recommendation = recommendation
        )
    }

    private fun formatDuration(millis: Long): String {
        val totalMinutes = millis / (1000 * 60)
        val hrs = totalMinutes / 60
        val mins = totalMinutes % 60
        return when {
            hrs > 0 && mins > 0 -> "${hrs}h ${mins}m"
            hrs > 0 -> "${hrs}h"
            mins > 0 -> "${mins}m"
            else -> "< 1m"
        }
    }
}
