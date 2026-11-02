package com.example.platform

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

data class CapturedNotification(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val subText: String?,
    val timestampMillis: Long,
    val formattedTime: String
)

class DevAiNotificationListenerService : NotificationListenerService() {

    companion object {
        var isConnected: Boolean = false
            private set

        private val recentNotifications = CopyOnWriteArrayList<CapturedNotification>()
        private var instance: DevAiNotificationListenerService? = null

        fun getActiveNotificationsList(context: Context): List<CapturedNotification> {
            val list = mutableListOf<CapturedNotification>()
            val activeSbn = try {
                instance?.activeNotifications
            } catch (_: Exception) {
                null
            }

            if (activeSbn != null && activeSbn.isNotEmpty()) {
                val pm = context.packageManager
                for (sbn in activeSbn) {
                    val notif = parseSbn(sbn, pm)
                    if (notif != null) list.add(notif)
                }
            } else {
                // Return recent cached items if active buffer is empty
                list.addAll(recentNotifications.takeLast(30).reversed())
            }
            return list
        }

        private fun parseSbn(sbn: StatusBarNotification, pm: PackageManager): CapturedNotification? {
            val extras = sbn.notification.extras ?: return null
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString() ?: ""
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

            if (title.isBlank() && text.isBlank()) return null

            val appName = try {
                val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                sbn.packageName.substringAfterLast('.')
            }

            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return CapturedNotification(
                id = "${sbn.packageName}_${sbn.id}_${sbn.postTime}",
                packageName = sbn.packageName,
                appName = appName,
                title = title,
                text = text,
                subText = subText,
                timestampMillis = sbn.postTime,
                formattedTime = sdf.format(Date(sbn.postTime))
            )
        }

        fun getActiveMediaControllers(context: Context): List<android.media.session.MediaController> {
            val service = instance ?: return emptyList()
            return try {
                val msm = service.getSystemService(Context.MEDIA_SESSION_SERVICE) as? android.media.session.MediaSessionManager
                val component = android.content.ComponentName(service, DevAiNotificationListenerService::class.java)
                msm?.getActiveSessions(component) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        instance = this
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        if (instance == this) instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val parsed = parseSbn(sbn, packageManager) ?: return
        // Maintain sliding window of 50 recent notifications
        recentNotifications.removeAll { it.id == parsed.id }
        recentNotifications.add(parsed)
        if (recentNotifications.size > 50) {
            recentNotifications.removeAt(0)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val idPrefix = "${sbn.packageName}_${sbn.id}"
        recentNotifications.removeAll { it.id.startsWith(idPrefix) }
    }
}
