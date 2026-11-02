package com.example.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.data.preferences.DevAiPreferences
import com.example.voice.VoiceAssistantManager
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class CustomRoutine(
    val id: String,
    val name: String,
    val triggerType: String, // "battery_below", "battery_above", "power_connected", "power_disconnected", "shake_device", "time_schedule"
    val triggerParam: String, // e.g. "20", "85", "22:00"
    val actionType: String,  // "speak_text", "toggle_flashlight", "set_brightness", "set_audio_profile", "open_battery_saver", "media_play_pause", "send_notification", "launch_app"
    val actionParam: String, // payload / speech text / level
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    var lastTriggeredAt: Long = 0L,
    var triggerCount: Int = 0
)

class CustomRoutineEngine(
    private val context: Context,
    private val preferences: DevAiPreferences,
    private val voiceAssistantManager: VoiceAssistantManager? = null,
    private val flashlightManager: FlashlightManager? = null,
    private val displayAndBrightnessManager: DisplayAndBrightnessManager? = null,
    private val audioControlManager: AudioControlManager? = null,
    private val mediaControlManager: MediaControlManager? = null
) {

    private val PREF_KEY_ROUTINES = "dev_ai_custom_routines_json"
    private var isReceiverRegistered = false
    private var lastBatteryLevel = -1

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    onTriggerFired("power_connected", "")
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    onTriggerFired("power_disconnected", "")
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                    if (level >= 0 && scale > 0) {
                        val percent = ((level.toFloat() / scale.toFloat()) * 100).toInt()
                        if (percent != lastBatteryLevel) {
                            lastBatteryLevel = percent
                            checkBatteryTriggers(percent)
                        }
                    }
                }
            }
        }
    }

    init {
        createNotificationChannel()
        ensureDefaultRoutines()
        startTriggerMonitoring()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = NotificationChannel(
                "dev_ai_routines_channel",
                "DEV AI Custom Routines",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Triggers and alerts from DEV AI IFTTT custom automation routines"
            }
            nm?.createNotificationChannel(channel)
        }
    }

    private fun ensureDefaultRoutines() {
        val existing = getRoutines()
        if (existing.isEmpty()) {
            val defaults = listOf(
                CustomRoutine(
                    id = "routine_low_battery_guard",
                    name = "Low Battery Voice Warning",
                    triggerType = "battery_below",
                    triggerParam = "20",
                    actionType = "speak_text",
                    actionParam = "DEV AI Battery Alert: Battery dropped below 20%. Connect charger or activate Battery Saver.",
                    isEnabled = true
                ),
                CustomRoutine(
                    id = "routine_optimal_charge_alert",
                    name = "85% Optimal Charge Guard",
                    triggerType = "battery_above",
                    triggerParam = "85",
                    actionType = "speak_text",
                    actionParam = "DEV AI Battery Guard: Charge reached 85%. Unplugging now extends cell life.",
                    isEnabled = true
                ),
                CustomRoutine(
                    id = "routine_power_connected_chime",
                    name = "Power Connected Chime",
                    triggerType = "power_connected",
                    triggerParam = "",
                    actionType = "send_notification",
                    actionParam = "Power source connected. Fast charging initiated.",
                    isEnabled = true
                )
            )
            saveRoutines(defaults)
        }
    }

    fun startTriggerMonitoring() {
        if (!isReceiverRegistered) {
            try {
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_POWER_CONNECTED)
                    addAction(Intent.ACTION_POWER_DISCONNECTED)
                    addAction(Intent.ACTION_BATTERY_CHANGED)
                }
                context.registerReceiver(batteryReceiver, filter)
                isReceiverRegistered = true
            } catch (_: Exception) {}
        }
    }

    fun stopTriggerMonitoring() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(batteryReceiver)
            } catch (_: Exception) {}
            isReceiverRegistered = false
        }
    }

    fun getRoutines(): List<CustomRoutine> {
        val sharedPrefs = context.getSharedPreferences("dev_ai_user_preferences", Context.MODE_PRIVATE)
        val jsonStr = sharedPrefs.getString(PREF_KEY_ROUTINES, null) ?: return emptyList()
        val list = mutableListOf<CustomRoutine>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CustomRoutine(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        triggerType = obj.getString("triggerType"),
                        triggerParam = obj.optString("triggerParam", ""),
                        actionType = obj.getString("actionType"),
                        actionParam = obj.optString("actionParam", ""),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        lastTriggeredAt = obj.optLong("lastTriggeredAt", 0L),
                        triggerCount = obj.optInt("triggerCount", 0)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun saveRoutines(routines: List<CustomRoutine>) {
        val sharedPrefs = context.getSharedPreferences("dev_ai_user_preferences", Context.MODE_PRIVATE)
        val array = JSONArray()
        for (r in routines) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("name", r.name)
                put("triggerType", r.triggerType)
                put("triggerParam", r.triggerParam)
                put("actionType", r.actionType)
                put("actionParam", r.actionParam)
                put("isEnabled", r.isEnabled)
                put("createdAt", r.createdAt)
                put("lastTriggeredAt", r.lastTriggeredAt)
                put("triggerCount", r.triggerCount)
            }
            array.put(obj)
        }
        sharedPrefs.edit().putString(PREF_KEY_ROUTINES, array.toString()).apply()
    }

    fun createRoutine(
        name: String,
        triggerType: String,
        triggerParam: String,
        actionType: String,
        actionParam: String
    ): CustomRoutine {
        val id = "routine_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5)}"
        val routine = CustomRoutine(
            id = id,
            name = name.trim().ifEmpty { "Custom DEV AI Routine" },
            triggerType = triggerType.trim().lowercase(),
            triggerParam = triggerParam.trim(),
            actionType = actionType.trim().lowercase(),
            actionParam = actionParam.trim(),
            isEnabled = true
        )
        val list = getRoutines().toMutableList()
        list.add(0, routine)
        saveRoutines(list)
        return routine
    }

    fun toggleRoutine(id: String, enabled: Boolean): Boolean {
        val list = getRoutines().toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index != -1) {
            val updated = list[index].copy(isEnabled = enabled)
            list[index] = updated
            saveRoutines(list)
            return true
        }
        return false
    }

    fun deleteRoutine(id: String): Boolean {
        val list = getRoutines().toMutableList()
        val removed = list.removeAll { it.id == id }
        if (removed) {
            saveRoutines(list)
        }
        return removed
    }

    fun checkBatteryTriggers(currentPercent: Int) {
        val routines = getRoutines().filter { it.isEnabled }
        val now = System.currentTimeMillis()

        for (r in routines) {
            // Debounce: don't trigger the same routine within 10 minutes
            if (now - r.lastTriggeredAt < 10 * 60 * 1000) continue

            if (r.triggerType == "battery_below") {
                val threshold = r.triggerParam.toIntOrNull() ?: 20
                if (currentPercent <= threshold) {
                    executeRoutine(r, "Battery dropped to $currentPercent%")
                }
            } else if (r.triggerType == "battery_above") {
                val threshold = r.triggerParam.toIntOrNull() ?: 80
                if (currentPercent >= threshold) {
                    executeRoutine(r, "Battery reached $currentPercent%")
                }
            }
        }
    }

    fun onTriggerFired(triggerType: String, param: String = "") {
        val routines = getRoutines().filter { it.isEnabled && it.triggerType.equals(triggerType, ignoreCase = true) }
        val now = System.currentTimeMillis()
        for (r in routines) {
            // Debounce 10 seconds for power / shake
            if (now - r.lastTriggeredAt < 10 * 1000) continue
            executeRoutine(r, "Triggered by $triggerType")
        }
    }

    fun executeRoutine(routine: CustomRoutine, reason: String): String {
        routine.lastTriggeredAt = System.currentTimeMillis()
        routine.triggerCount++

        val list = getRoutines().toMutableList()
        val idx = list.indexOfFirst { it.id == routine.id }
        if (idx != -1) {
            list[idx] = routine
            saveRoutines(list)
        }

        val resultMsg = executeAction(routine.actionType, routine.actionParam, routine.name)
        postNotification(routine.name, "$reason -> $resultMsg")
        return resultMsg
    }

    private fun executeAction(actionType: String, actionParam: String, routineName: String): String {
        return try {
            when (actionType.lowercase()) {
                "speak_text" -> {
                    val speech = actionParam.ifEmpty { "DEV AI routine $routineName executed." }
                    voiceAssistantManager?.speak(speech)
                    "Spoke vocal announcement: \"$speech\""
                }
                "toggle_flashlight" -> {
                    when (actionParam.lowercase()) {
                        "on" -> {
                            flashlightManager?.turnOn()
                            "Turned flashlight ON"
                        }
                        "off" -> {
                            flashlightManager?.turnOff()
                            "Turned flashlight OFF"
                        }
                        else -> {
                            val res = flashlightManager?.toggle()
                            val isOn = res?.getOrNull() ?: false
                            "Toggled flashlight ${if (isOn) "ON" else "OFF"}"
                        }
                    }
                }
                "set_brightness" -> {
                    val percent = actionParam.toIntOrNull() ?: 50
                    displayAndBrightnessManager?.setBrightness(percent)
                    "Set screen brightness to $percent%"
                }
                "set_audio_profile" -> {
                    val profile = actionParam.ifEmpty { "meeting" }
                    val res = audioControlManager?.applyAudioProfile(profile)
                    res?.getOrNull() ?: "Applied audio profile: $profile"
                }
                "open_battery_saver" -> {
                    val intent = Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    "Opened Android Battery Saver Settings"
                }
                "media_play_pause" -> {
                    when (actionParam.lowercase()) {
                        "play" -> mediaControlManager?.mediaPlay() ?: "Play command dispatched"
                        "pause" -> mediaControlManager?.mediaPause() ?: "Pause command dispatched"
                        else -> mediaControlManager?.mediaPlayPause() ?: "Play/Pause toggled"
                    }
                }
                "send_notification" -> {
                    postNotification("DEV AI Routine: $routineName", actionParam.ifEmpty { "Routine executed successfully." })
                    "Posted notification: $actionParam"
                }
                "launch_app" -> {
                    val intent = context.packageManager.getLaunchIntentForPackage(actionParam)
                    if (intent != null) {
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                        "Launched application: $actionParam"
                    } else {
                        "App package $actionParam is not installed."
                    }
                }
                else -> {
                    "Executed routine action: $actionType ($actionParam)"
                }
            }
        } catch (e: Exception) {
            "Failed executing action: ${e.message}"
        }
    }

    private fun postNotification(title: String, text: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val notif = NotificationCompat.Builder(context, "dev_ai_routines_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify((System.currentTimeMillis() % 100000).toInt(), notif)
    }

    fun formatRoutinesSummary(): String {
        val list = getRoutines()
        if (list.isEmpty()) {
            return "No custom routines configured yet. You can create one anytime by telling DEV AI (e.g., *'If battery is below 20%, speak low battery warning'*)."
        }

        val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        val builder = StringBuilder("### ⚡ DEV AI Custom Routines (\"If This Then That\")\n")
        builder.append("Active trigger monitoring: **ENABLED**\n\n")

        list.forEachIndexed { i, r ->
            val status = if (r.isEnabled) "🟢 Active" else "⚪ Disabled"
            val lastTrigger = if (r.lastTriggeredAt > 0) sdf.format(Date(r.lastTriggeredAt)) else "Never"
            builder.append("**${i + 1}. ${r.name}** ($status)\n")
            builder.append("   - **IF (Trigger)**: `${r.triggerType}` ${if (r.triggerParam.isNotEmpty()) "[${r.triggerParam}]" else ""}\n")
            builder.append("   - **THEN (Action)**: `${r.actionType}` ${if (r.actionParam.isNotEmpty()) "\"${r.actionParam}\"" else ""}\n")
            builder.append("   - **Stats**: Fired ${r.triggerCount} times (Last: $lastTrigger) | ID: `${r.id}`\n\n")
        }

        return builder.toString().trim()
    }
}
