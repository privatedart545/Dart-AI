package com.example.platform

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import java.util.Locale

class NotificationReaderManager(private val context: Context) {

    fun isNotificationAccessGranted(): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(context.packageName)
    }

    fun openNotificationListenerSettings(): Result<String> {
        return try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened Android Notification Access Settings. Enable 'DEV AI Notification Access' so DEV AI can read messages when you ask.")
        } catch (e: Exception) {
            Result.failure(Exception("Unable to open notification listener settings: ${e.localizedMessage}"))
        }
    }

    fun checkNotifications(appQuery: String? = null, keywordQuery: String? = null): Result<String> {
        if (!isNotificationAccessGranted()) {
            return Result.failure(
                Exception(
                    "Notification access is not granted yet. Please allow DEV AI in Android Notification Access settings. You can ask me 'Open notification settings' to grant permission."
                )
            )
        }

        val allNotifs = DevAiNotificationListenerService.getActiveNotificationsList(context)
        if (allNotifs.isEmpty()) {
            val appDesc = if (!appQuery.isNullOrBlank()) " from $appQuery" else ""
            return Result.success("No active notifications detected$appDesc.")
        }

        val cleanApp = appQuery?.trim()?.lowercase(Locale.ROOT)
        val cleanKeyword = keywordQuery?.trim()?.lowercase(Locale.ROOT)

        val filtered = allNotifs.filter { notif ->
            val matchApp = cleanApp.isNullOrBlank() ||
                    notif.appName.lowercase(Locale.ROOT).contains(cleanApp) ||
                    notif.packageName.lowercase(Locale.ROOT).contains(cleanApp)

            val matchKeyword = cleanKeyword.isNullOrBlank() ||
                    notif.title.lowercase(Locale.ROOT).contains(cleanKeyword) ||
                    notif.text.lowercase(Locale.ROOT).contains(cleanKeyword) ||
                    (notif.subText != null && notif.subText.lowercase(Locale.ROOT).contains(cleanKeyword))

            matchApp && matchKeyword
        }

        if (filtered.isEmpty()) {
            val target = appQuery ?: keywordQuery ?: "requested criteria"
            return Result.success("No message or notification detected from $target.")
        }

        val sb = StringBuilder()
        val count = filtered.size
        val targetName = appQuery?.replaceFirstChar { it.uppercase() } ?: "Applications"
        sb.append("Found $count notification(s)${if (!appQuery.isNullOrBlank()) " from $targetName" else ""}:\n")

        for ((index, item) in filtered.take(8).withIndex()) {
            val titlePart = if (item.title.isNotBlank()) "• **${item.title}**" else "• **${item.appName}**"
            val textPart = if (item.text.isNotBlank()) ": \"${item.text}\"" else ""
            val subPart = if (!item.subText.isNullOrBlank()) " (${item.subText})" else ""
            sb.append("${index + 1}. $titlePart$textPart$subPart [${item.formattedTime}]\n")
        }

        return Result.success(sb.toString().trim())
    }
}
