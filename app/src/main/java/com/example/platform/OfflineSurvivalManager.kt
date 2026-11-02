package com.example.platform

class OfflineSurvivalManager(
    private val flashlightManager: FlashlightManager,
    private val audioControlManager: AudioControlManager,
    private val telemetryManager: DeviceTelemetryManager,
    private val systemSettingsManager: SystemSettingsManager,
    private val alarmManager: AlarmAndCalendarManager,
    private val mathCalculatorManager: MathCalculatorManager,
    private val contactsAndPhoneManager: ContactsAndPhoneManager,
    private val dailyBriefingManager: DailyBriefingManager
) {

    data class OfflineResult(
        val handled: Boolean,
        val responseText: String,
        val spokenText: String = responseText
    )

    fun handleOfflineIntent(rawInput: String): OfflineResult {
        val input = rawInput.trim().lowercase()

        // 1. Flashlight
        if (input.contains("flashlight") || input.contains("torch")) {
            return if (input.contains("on") || input.contains("enable") || input.contains("start")) {
                val res = flashlightManager.turnOn()
                OfflineResult(true, "⚡ [Offline Survival Mode]: " + if (res.isSuccess) "Flashlight turned ON." else (res.exceptionOrNull()?.message ?: "Failed"))
            } else if (input.contains("off") || input.contains("disable") || input.contains("stop")) {
                val res = flashlightManager.turnOff()
                OfflineResult(true, "⚡ [Offline Survival Mode]: " + if (res.isSuccess) "Flashlight turned OFF." else (res.exceptionOrNull()?.message ?: "Failed"))
            } else {
                val res = flashlightManager.toggle()
                val statusText = if (flashlightManager.getStatus()) "Flashlight is now ON." else "Flashlight is now OFF."
                OfflineResult(true, "⚡ [Offline Survival Mode]: $statusText")
            }
        }

        // 2. Battery & Telemetry
        if (input.contains("battery") || input.contains("charge") || input.contains("power level")) {
            val bat = telemetryManager.getBatteryStatus()
            val chargingState = if (bat.isCharging) "Charging (${bat.chargingSource})" else "Discharging"
            val text = "⚡ [Offline Survival Mode]: Battery is at ${bat.percentage}% ($chargingState)."
            return OfflineResult(true, text)
        }

        // 3. Storage
        if (input.contains("storage") || input.contains("free space") || input.contains("disk space")) {
            val storage = telemetryManager.getDeviceStorageInfo()
            val text = "⚡ [Offline Survival Mode]: ${storage.freeGb} GB available of ${storage.totalGb} GB (${storage.usedPercent}% used)."
            return OfflineResult(true, text)
        }

        // 4. Volume controls
        if (input.contains("volume") || input.contains("mute") || input.contains("unmute") || input.contains("sound")) {
            if (input.contains("mute")) {
                val res = audioControlManager.mute()
                return OfflineResult(true, "⚡ [Offline Survival Mode]: " + (res.getOrNull() ?: "Media volume muted."))
            } else if (input.contains("unmute")) {
                val res = audioControlManager.unmute()
                return OfflineResult(true, "⚡ [Offline Survival Mode]: " + (res.getOrNull() ?: "Media volume unmuted."))
            } else if (input.contains("max") || input.contains("100")) {
                val res = audioControlManager.setMediaVolume(100)
                return OfflineResult(true, "⚡ [Offline Survival Mode]: " + (res.getOrNull() ?: "Media volume set to 100%."))
            } else if (input.contains("half") || input.contains("50")) {
                val res = audioControlManager.setMediaVolume(50)
                return OfflineResult(true, "⚡ [Offline Survival Mode]: " + (res.getOrNull() ?: "Media volume set to 50%."))
            }
        }

        // 5. Alarms & Timers
        if (input.contains("show alarm") || input == "alarms") {
            val res = alarmManager.showAlarms()
            return OfflineResult(true, "⚡ [Offline Survival Mode]: " + (res.getOrNull() ?: "Opening Clock alarms."))
        }

        if (input.contains("alarm") || input.contains("wake me")) {
            val hourMatch = Regex("(\\d{1,2})(:(\\d{2}))?\\s*(am|pm)?").find(input)
            if (hourMatch != null) {
                var h = hourMatch.groupValues[1].toIntOrNull() ?: 7
                val m = hourMatch.groupValues[3].toIntOrNull() ?: 0
                val ampm = hourMatch.groupValues[4]
                if (ampm == "pm" && h < 12) h += 12
                if (ampm == "am" && h == 12) h = 0
                val res = alarmManager.setAlarm(h, m, "DEV AI Alarm")
                return OfflineResult(true, "⚡ [Offline Survival Mode]: " + (res.getOrNull() ?: "Alarm set for $h:$m."))
            }
        }

        if (input.contains("timer")) {
            val num = Regex("(\\d+)\\s*(minute|second|min|sec)").find(input)
            val count = num?.groupValues?.get(1)?.toIntOrNull() ?: 5
            val unit = num?.groupValues?.get(2) ?: "min"
            val totalSeconds = if (unit.startsWith("sec")) count else count * 60
            val res = alarmManager.setTimer(totalSeconds, "DEV AI Timer")
            return OfflineResult(true, "⚡ [Offline Survival Mode]: " + (res.getOrNull() ?: "Timer started for $count $unit."))
        }

        // 6. System Settings
        if (input.contains("settings") || input.contains("wifi") || input.contains("bluetooth") || input.contains("display") || input.contains("airplane")) {
            for (setting in systemSettingsManager.availableSettings) {
                if (input.contains(setting.id) || input.contains(setting.title.lowercase())) {
                    val res = systemSettingsManager.openSettingById(setting.id)
                    return OfflineResult(true, "⚡ [Offline Survival Mode]: " + (res.getOrNull() ?: "Opened ${setting.title}."))
                }
            }
        }

        // 7. Math calculations
        if (input.startsWith("calculate ") || input.startsWith("calc ") || input.startsWith("math ") || input.matches(Regex("^[0-9+\\-*/^. ()]+$"))) {
            val expr = input.removePrefix("calculate ").removePrefix("calc ").removePrefix("math ")
            val res = mathCalculatorManager.evaluate(expr)
            if (res.isSuccess) {
                return OfflineResult(true, "⚡ [Offline Survival Mode]: " + res.getOrThrow())
            }
        }

        // 8. Phone dialing (by Name or Number!)
        if (input.startsWith("dial ") || input.startsWith("call ")) {
            val target = input.removePrefix("dial ").removePrefix("call ").trim()
            val res = contactsAndPhoneManager.dialContactByName(target)
            return OfflineResult(true, "⚡ [Offline Survival Mode]: " + (res.getOrNull() ?: res.exceptionOrNull()?.message ?: "Calling $target."))
        }

        // 9. Daily Briefing
        if (input.contains("briefing") || input.contains("status report") || input.contains("morning update")) {
            val text = dailyBriefingManager.generateBriefing()
            return OfflineResult(true, "⚡ [Offline Survival Mode]:\n$text")
        }

        return OfflineResult(false, "")
    }
}
