package com.example.platform

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import kotlin.math.roundToInt

data class DisplayStatus(
    val brightnessPercent: Int,
    val isAutoBrightness: Boolean,
    val screenTimeoutSeconds: Int,
    val hasWriteSettingsPermission: Boolean
)

class DisplayAndBrightnessManager(private val context: Context) {

    private val resolver: ContentResolver = context.contentResolver

    fun hasWriteSettingsPermission(): Boolean {
        return Settings.System.canWrite(context)
    }

    fun openWriteSettingsPermission(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    fun openDisplaySettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openNightDisplaySettings(): Boolean {
        return try {
            val intent = Intent("android.settings.NIGHT_DISPLAY_SETTINGS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            openDisplaySettings()
        }
    }

    fun getDisplayStatus(): DisplayStatus {
        val rawBrightness = try {
            Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Exception) {
            128
        }
        val brightnessPercent = ((rawBrightness / 255.0) * 100).roundToInt().coerceIn(0, 100)

        val mode = try {
            Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
        } catch (e: Exception) {
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        }
        val isAuto = (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)

        val timeoutMs = try {
            Settings.System.getInt(resolver, Settings.System.SCREEN_OFF_TIMEOUT)
        } catch (e: Exception) {
            30000
        }
        val timeoutSec = timeoutMs / 1000

        return DisplayStatus(
            brightnessPercent = brightnessPercent,
            isAutoBrightness = isAuto,
            screenTimeoutSeconds = timeoutSec,
            hasWriteSettingsPermission = hasWriteSettingsPermission()
        )
    }

    fun setBrightness(percent: Int): String {
        val targetPercent = percent.coerceIn(0, 100)
        val rawValue = ((targetPercent / 100.0) * 255).roundToInt().coerceIn(0, 255)

        if (!hasWriteSettingsPermission()) {
            openWriteSettingsPermission()
            return "Modify System Settings permission is required to adjust screen brightness directly. DEV AI has opened the permission screen. Once granted, please retry."
        }

        return try {
            // Disable auto brightness first so manual brightness applies
            Settings.System.putInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS,
                rawValue
            )
            "Screen brightness successfully set to $targetPercent% (raw value: $rawValue/255)."
        } catch (e: Exception) {
            "Failed to adjust screen brightness: ${e.localizedMessage}"
        }
    }

    fun setAutoBrightness(enable: Boolean): String {
        if (!hasWriteSettingsPermission()) {
            openWriteSettingsPermission()
            return "Modify System Settings permission is required to toggle auto brightness. Opening settings..."
        }

        return try {
            val mode = if (enable) {
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            } else {
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            }
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, mode)
            "Adaptive / Auto Brightness is now ${if (enable) "ENABLED" else "DISABLED"}."
        } catch (e: Exception) {
            "Failed to update auto brightness: ${e.localizedMessage}"
        }
    }

    fun setScreenTimeout(seconds: Int): String {
        if (!hasWriteSettingsPermission()) {
            openWriteSettingsPermission()
            return "Modify System Settings permission is required to change screen timeout. Opening settings..."
        }

        val safeSec = seconds.coerceAtLeast(15)
        val timeoutMs = safeSec * 1000

        return try {
            Settings.System.putInt(resolver, Settings.System.SCREEN_OFF_TIMEOUT, timeoutMs)
            "Screen timeout successfully updated to $safeSec seconds (${safeSec / 60}m ${safeSec % 60}s)."
        } catch (e: Exception) {
            "Failed to update screen timeout: ${e.localizedMessage}"
        }
    }
}
