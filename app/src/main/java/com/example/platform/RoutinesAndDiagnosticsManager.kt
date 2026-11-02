package com.example.platform

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RoutinesAndDiagnosticsManager(
    private val context: Context,
    private val flashlightManager: FlashlightManager,
    private val audioControlManager: AudioControlManager,
    private val deviceTelemetryManager: DeviceTelemetryManager,
    private val safFileManager: SafFileManager
) {

    fun executeRoutine(routineName: String): Result<String> {
        val name = routineName.lowercase().trim()
        val steps = mutableListOf<String>()

        when {
            name.contains("bedtime") || name.contains("night") || name.contains("sleep") -> {
                // 1. Turn off flashlight
                flashlightManager.turnOff()
                steps.add("• Flashlight turned OFF")

                // 2. Mute media volume
                audioControlManager.setMediaVolume(0)
                steps.add("• Media volume muted (0%)")

                // 3. Battery check
                val bat = deviceTelemetryManager.getBatteryStatus()
                val batAdvice = if (!bat.isCharging && bat.percentage < 40) {
                    "⚠️ Battery at ${bat.percentage}%. Please plug in charger before sleeping!"
                } else {
                    "Battery at ${bat.percentage}% (${if (bat.isCharging) "Charging" else "Unplugged"})."
                }
                steps.add("• $batAdvice")

                return Result.success("🌙 Bedtime Routine Executed:\n${steps.joinToString("\n")}\nSleep well!")
            }

            name.contains("study") || name.contains("focus") || name.contains("work") -> {
                // 1. Lower media volume to 10%
                audioControlManager.setMediaVolume(10)
                steps.add("• Media volume lowered to 10% for focus")

                // 2. Log session start in notes
                val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                safFileManager.createFile("DEV_AI_Focus_Log.txt", "Focus Session Started at $now\n")
                steps.add("• Created timestamped session entry in DEV_AI_Focus_Log.txt")

                // 3. Telemetry check
                val net = deviceTelemetryManager.getNetworkStatus()
                steps.add("• Connection: $net")

                return Result.success("🎯 Focus Mode Activated:\n${steps.joinToString("\n")}\nProductive session ahead!")
            }

            name.contains("morning") || name.contains("wakeup") -> {
                // 1. Set media volume to 50%
                audioControlManager.setMediaVolume(50)
                steps.add("• Media volume set to 50%")

                // 2. Get date & time
                val time = deviceTelemetryManager.getCurrentTime()
                val date = deviceTelemetryManager.getCurrentDate()
                steps.add("• Time: $time on $date")

                // 3. Battery status
                val bat = deviceTelemetryManager.getBatteryStatus()
                steps.add("• Battery: ${bat.percentage}% (${if (bat.isCharging) "Charging" else "On Battery"})")

                return Result.success("☀️ Good Morning Routine:\n${steps.joinToString("\n")}\nHave an incredible day!")
            }

            else -> {
                return Result.failure(
                    Exception("Unknown routine '$routineName'. Available routines: 'bedtime', 'focus', 'morning'.")
                )
            }
        }
    }

    fun runDiagnostics(): Result<String> {
        val reports = mutableListOf<String>()

        // 1. System OS
        reports.add("🖥️ Device OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}, ${Build.MANUFACTURER} ${Build.MODEL})")

        // 2. RAM / Memory
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        if (actManager != null) {
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            val totalMb = memInfo.totalMem / (1024 * 1024)
            val availMb = memInfo.availMem / (1024 * 1024)
            val usedMb = totalMb - availMb
            val usedPct = ((usedMb.toDouble() / totalMb.toDouble()) * 100).toInt()
            reports.add("🧠 RAM Usage: $usedMb MB / $totalMb MB ($usedPct% used, $availMb MB free)")
        }

        // 3. Storage
        val storage = deviceTelemetryManager.getDeviceStorageInfo()
        reports.add("💾 Internal Storage: ${storage.usedGb} GB / ${storage.totalGb} GB (${storage.usedPercent}% used, ${storage.freeGb} GB free)")

        // 4. Battery
        val bat = deviceTelemetryManager.getBatteryStatus()
        val chg = if (bat.isCharging) "Charging via ${bat.chargingSource}" else "Discharging"
        reports.add("🔋 Battery: ${bat.percentage}% ($chg, Power Saver: ${if (bat.powerSaveMode) "ON" else "OFF"})")

        // 5. Network
        val net = deviceTelemetryManager.getNetworkStatus()
        reports.add("🌐 Connectivity: $net")

        // 6. Authorized Folders
        val folders = safFileManager.getAuthorizedFolders()
        reports.add("📁 Storage Access Framework: ${folders.size} authorized folder(s)")

        return Result.success("🛠️ DEV AI Hardware & System Diagnostics:\n\n${reports.joinToString("\n")}")
    }
}
