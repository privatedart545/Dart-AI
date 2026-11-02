package com.example.platform

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings

data class VolumeStatus(
    val mediaPercent: Int,
    val ringPercent: Int,
    val notificationPercent: Int,
    val alarmPercent: Int,
    val ringerMode: String,
    val isDndActive: Boolean,
    val hasDndPermission: Boolean
)

class AudioControlManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    fun isDndPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager?.isNotificationPolicyAccessGranted == true
        } else true
    }

    fun openDndAccessSettings(): Result<String> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Result.success("Opened Do Not Disturb permission settings. Please grant access so DEV AI can control DND.")
            } else {
                Result.success("DND policy access not required on this Android version.")
            }
        } catch (e: Exception) {
            Result.failure(Exception("Unable to open DND settings: ${e.localizedMessage}"))
        }
    }

    fun getVolumeLevels(): VolumeStatus {
        val am = audioManager ?: return VolumeStatus(0, 0, 0, 0, "Unknown", false, false)

        fun streamPercent(stream: Int): Int {
            val max = am.getStreamMaxVolume(stream)
            if (max <= 0) return 0
            val current = am.getStreamVolume(stream)
            return ((current.toFloat() / max.toFloat()) * 100).toInt()
        }

        val modeStr = when (am.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> "Silent"
            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
            AudioManager.RINGER_MODE_NORMAL -> "Normal"
            else -> "Standard"
        }

        val isDnd = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val filter = notificationManager?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL
            filter != NotificationManager.INTERRUPTION_FILTER_ALL && filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        } else false

        return VolumeStatus(
            mediaPercent = streamPercent(AudioManager.STREAM_MUSIC),
            ringPercent = streamPercent(AudioManager.STREAM_RING),
            notificationPercent = streamPercent(AudioManager.STREAM_NOTIFICATION),
            alarmPercent = streamPercent(AudioManager.STREAM_ALARM),
            ringerMode = modeStr,
            isDndActive = isDnd,
            hasDndPermission = isDndPermissionGranted()
        )
    }

    fun setMediaVolume(percent: Int): Result<String> {
        val am = audioManager ?: return Result.failure(Exception("Audio service unavailable."))
        return try {
            val clamped = percent.coerceIn(0, 100)
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val targetLevel = ((clamped / 100.0) * max).toInt()
            am.setStreamVolume(AudioManager.STREAM_MUSIC, targetLevel, AudioManager.FLAG_SHOW_UI)
            Result.success("Media volume adjusted to $clamped% (Level $targetLevel/$max).")
        } catch (e: Exception) {
            Result.failure(Exception("Could not change volume: ${e.localizedMessage}"))
        }
    }

    fun setRingerMode(mode: String): Result<String> {
        val am = audioManager ?: return Result.failure(Exception("Audio service unavailable."))
        return try {
            when (mode.lowercase()) {
                "silent" -> {
                    am.ringerMode = AudioManager.RINGER_MODE_SILENT
                    Result.success("Ringer mode set to SILENT.")
                }
                "vibrate" -> {
                    am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    Result.success("Ringer mode set to VIBRATE.")
                }
                "normal" -> {
                    am.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    Result.success("Ringer mode set to NORMAL ring.")
                }
                else -> Result.failure(Exception("Unknown ringer mode '$mode'. Use 'silent', 'vibrate', or 'normal'."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Could not set ringer mode: ${e.localizedMessage}"))
        }
    }

    fun setSmartDnd(enable: Boolean, priorityOnly: Boolean = false): Result<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return Result.success("Do Not Disturb mode toggle not supported on this version.")
        }
        val nm = notificationManager ?: return Result.failure(Exception("Notification manager unavailable."))

        if (!isDndPermissionGranted()) {
            openDndAccessSettings()
            return Result.failure(
                Exception("Do Not Disturb permission is required. Opened DND access settings so you can authorize DEV AI.")
            )
        }

        return try {
            if (enable) {
                val filter = if (priorityOnly) {
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY
                } else {
                    NotificationManager.INTERRUPTION_FILTER_NONE
                }
                nm.setInterruptionFilter(filter)
                audioManager?.ringerMode = AudioManager.RINGER_MODE_SILENT
                Result.success("Smart Do Not Disturb activated (${if (priorityOnly) "Priority Only" else "Total Silence"}).")
            } else {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                audioManager?.ringerMode = AudioManager.RINGER_MODE_NORMAL
                Result.success("Smart Do Not Disturb turned OFF. All alerts restored.")
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to change DND: ${e.localizedMessage}"))
        }
    }

    fun applyAudioProfile(profileName: String): Result<String> {
        val am = audioManager ?: return Result.failure(Exception("Audio service unavailable."))
        return try {
            when (profileName.lowercase()) {
                "meeting" -> {
                    am.ringerMode = AudioManager.RINGER_MODE_SILENT
                    setMediaVolume(0)
                    if (isDndPermissionGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                    }
                    Result.success("💼 Applied 'Meeting' profile: Silent ringer, media muted, priority alerts only.")
                }
                "focus", "study" -> {
                    am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    setMediaVolume(20)
                    if (isDndPermissionGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                    }
                    Result.success("🎯 Applied 'Focus' profile: Vibrate mode, low media (20%), priority alerts only.")
                }
                "sleep", "bedtime" -> {
                    am.ringerMode = AudioManager.RINGER_MODE_SILENT
                    setMediaVolume(0)
                    if (isDndPermissionGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                    }
                    Result.success("🌙 Applied 'Sleep' profile: Total silence, media muted, Do Not Disturb enabled.")
                }
                "outdoor", "loud" -> {
                    am.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    val maxRing = am.getStreamMaxVolume(AudioManager.STREAM_RING)
                    am.setStreamVolume(AudioManager.STREAM_RING, maxRing, AudioManager.FLAG_SHOW_UI)
                    setMediaVolume(85)
                    if (isDndPermissionGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    }
                    Result.success("📢 Applied 'Outdoor' profile: Maximum ringer volume, high media (85%), DND disabled.")
                }
                "normal", "standard" -> {
                    am.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    setMediaVolume(50)
                    if (isDndPermissionGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    }
                    Result.success("🔔 Applied 'Normal' profile: Standard ring mode, media 50%, all notifications enabled.")
                }
                "vibrate" -> {
                    am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    setMediaVolume(30)
                    Result.success("📳 Applied 'Vibrate' profile: Ringer set to vibrate, media volume 30%.")
                }
                else -> Result.failure(Exception("Unknown audio profile '$profileName'. Available profiles: 'normal', 'meeting', 'focus', 'sleep', 'outdoor', 'vibrate'."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to apply audio profile: ${e.localizedMessage}"))
        }
    }

    fun mute(): Result<String> = setMediaVolume(0)
    fun unmute(): Result<String> = setMediaVolume(50)
}
