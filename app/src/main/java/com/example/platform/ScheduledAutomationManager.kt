package com.example.platform

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class ScheduledAutomation(
    val id: String,
    val title: String,
    val actionType: String, // "routine", "audio_profile", "flashlight", "alert"
    val payload: String,
    val scheduledTimeMillis: Long,
    val formattedTime: String
)

class DevAiAutomationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("EXTRA_ID") ?: return
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "DEV AI Scheduled Automation"
        val actionType = intent.getStringExtra("EXTRA_ACTION_TYPE") ?: "alert"
        val payload = intent.getStringExtra("EXTRA_PAYLOAD") ?: ""

        // Post notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "dev_ai_automations_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "DEV AI Automations",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for scheduled automations and routines"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("DEV AI: $title")
            .setContentText("Triggered automation: $actionType ($payload)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id.hashCode(), notification)

        // Remove from active map
        ScheduledAutomationManager.activeAutomations.remove(id)
    }
}

class ScheduledAutomationManager(private val context: Context) {

    companion object {
        val activeAutomations = ConcurrentHashMap<String, ScheduledAutomation>()
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun scheduleAutomation(
        title: String,
        actionType: String,
        payload: String,
        delayMinutes: Int
    ): Result<String> {
        val am = alarmManager ?: return Result.failure(Exception("AlarmManager system service is unavailable."))
        val delayMillis = delayMinutes.toLong() * 60 * 1000
        val triggerAtMillis = System.currentTimeMillis() + delayMillis
        val id = "automation_${System.currentTimeMillis()}"

        val sdf = SimpleDateFormat("HH:mm, MMM d", Locale.getDefault())
        val formattedTime = sdf.format(Date(triggerAtMillis))

        val intent = Intent(context, DevAiAutomationReceiver::class.java).apply {
            action = "com.example.ACTION_EXECUTE_AUTOMATION"
            putExtra("EXTRA_ID", id)
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_ACTION_TYPE", actionType)
            putExtra("EXTRA_PAYLOAD", payload)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(context, id.hashCode(), intent, flags)

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }

            val automation = ScheduledAutomation(
                id = id,
                title = title,
                actionType = actionType,
                payload = payload,
                scheduledTimeMillis = triggerAtMillis,
                formattedTime = formattedTime
            )
            activeAutomations[id] = automation

            Result.success("Scheduled automation '$title' ($actionType: $payload) to execute in $delayMinutes minute(s) at $formattedTime [ID: $id].")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to schedule automation: ${e.localizedMessage}"))
        }
    }

    fun listScheduledAutomations(): List<ScheduledAutomation> {
        return activeAutomations.values.sortedBy { it.scheduledTimeMillis }
    }

    fun cancelAutomation(id: String): Result<String> {
        val item = activeAutomations.remove(id) ?: return Result.failure(Exception("No active automation found with ID '$id'."))
        val intent = Intent(context, DevAiAutomationReceiver::class.java).apply {
            action = "com.example.ACTION_EXECUTE_AUTOMATION"
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, id.hashCode(), intent, flags)
        alarmManager?.cancel(pendingIntent)
        return Result.success("Successfully cancelled scheduled automation '${item.title}' [ID: $id].")
    }
}
