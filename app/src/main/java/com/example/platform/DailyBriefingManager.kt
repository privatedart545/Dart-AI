package com.example.platform

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyBriefingManager(
    private val telemetryManager: DeviceTelemetryManager,
    private val audioControlManager: AudioControlManager
) {

    private val timeFormat = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.getDefault())

    private val developerQuotes = listOf(
        "\"Focus your mind, master your phone, control your destiny.\" — DEV L KAKKOTH",
        "\"Technology is only as powerful as the intent behind it.\" — DEV L KAKKOTH",
        "\"Your Personal AI. Your Phone. Your Control.\" — DEV L KAKKOTH",
        "\"Every line of code is an opportunity to empower someone.\" — DEV L KAKKOTH"
    )

    fun generateBriefing(): String {
        val now = timeFormat.format(Date())
        val battery = telemetryManager.getBatteryStatus()
        val storage = telemetryManager.getDeviceStorageInfo()
        val volume = audioControlManager.getVolumeLevels()
        val network = telemetryManager.getNetworkStatus()
        val quote = developerQuotes.random()

        val chargingState = if (battery.isCharging) "Charging via ${battery.chargingSource}" else "On Battery"

        return buildString {
            appendLine("🌅 **Dart AI Daily Executive Briefing**")
            appendLine("⏰ **Current Time:** $now")
            appendLine("🔋 **Battery:** ${battery.percentage}% ($chargingState)")
            appendLine("💾 **Storage:** ${storage.freeGb} GB free of ${storage.totalGb} GB (${storage.usedPercent}% used)")
            appendLine("📶 **Network:** $network")
            appendLine("🔊 **Media Volume:** ${volume.mediaPercent}%")
            appendLine("\n✨ **Daily Thought:**\n$quote")
        }
    }
}
