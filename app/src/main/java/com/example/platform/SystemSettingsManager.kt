package com.example.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

data class SystemSettingItem(
    val id: String,
    val title: String,
    val description: String,
    val action: String,
    val category: String
)

class SystemSettingsManager(private val context: Context) {

    val availableSettings: List<SystemSettingItem> = listOf(
        SystemSettingItem("wifi", "Wi-Fi Settings", "Network connections and Wi-Fi networks", Settings.ACTION_WIFI_SETTINGS, "Connectivity"),
        SystemSettingItem("bluetooth", "Bluetooth Settings", "Paired devices and wireless gadgets", Settings.ACTION_BLUETOOTH_SETTINGS, "Connectivity"),
        SystemSettingItem("network", "Network & Internet", "Mobile data, SIM cards, airplane mode", Settings.ACTION_WIRELESS_SETTINGS, "Connectivity"),
        SystemSettingItem("nfc", "NFC & Contactless", "Contactless payments and tags", Settings.ACTION_NFC_SETTINGS, "Connectivity"),
        SystemSettingItem("airplane", "Airplane Mode", "Toggle radio transmissions", Settings.ACTION_AIRPLANE_MODE_SETTINGS, "Connectivity"),
        SystemSettingItem("hotspot", "Tethering & Hotspot", "Portable Wi-Fi and USB hotspot", Settings.ACTION_WIRELESS_SETTINGS, "Connectivity"),
        
        SystemSettingItem("display", "Display & Brightness", "Screen timeout, dark theme, font size", Settings.ACTION_DISPLAY_SETTINGS, "Personalization"),
        SystemSettingItem("sound", "Sound & Vibration", "Ringtone, media volume, haptics", Settings.ACTION_SOUND_SETTINGS, "Personalization"),
        SystemSettingItem("notifications", "Notifications", "App alerts, notification history, DND", Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS, "Personalization"),
        SystemSettingItem("language", "Language & Keyboard", "Input methods, speech, spell check", Settings.ACTION_INPUT_METHOD_SETTINGS, "Personalization"),
        
        SystemSettingItem("battery", "Battery & Power", "Battery usage, battery saver, health", Intent.ACTION_POWER_USAGE_SUMMARY, "System"),
        SystemSettingItem("storage", "Storage Manager", "Free up space, downloads, cached files", Settings.ACTION_INTERNAL_STORAGE_SETTINGS, "System"),
        SystemSettingItem("apps", "Installed Applications", "Manage app permissions, storage, cache", Settings.ACTION_APPLICATION_SETTINGS, "System"),
        SystemSettingItem("device_info", "About Phone", "Android version, model, serial, updates", Settings.ACTION_DEVICE_INFO_SETTINGS, "System"),
        SystemSettingItem("date_time", "Date & Time", "Time zone, 24-hour format, automatic time", Settings.ACTION_DATE_SETTINGS, "System"),
        SystemSettingItem("developer", "Developer Options", "USB debugging, animations, memory logs", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, "System"),
        
        SystemSettingItem("location", "Location & GPS", "GPS satellites, location accuracy, access", Settings.ACTION_LOCATION_SOURCE_SETTINGS, "Privacy & Security"),
        SystemSettingItem("security", "Security & Lock Screen", "Screen lock, encryption, find my device", Settings.ACTION_SECURITY_SETTINGS, "Privacy & Security"),
        SystemSettingItem("biometrics", "Biometrics & Fingerprint", "Face unlock, fingerprint enrollment", Settings.ACTION_SECURITY_SETTINGS, "Privacy & Security"),
        SystemSettingItem("privacy", "Privacy & Permissions", "Permission manager, microphone/camera access", Settings.ACTION_PRIVACY_SETTINGS, "Privacy & Security"),
        SystemSettingItem("accessibility", "Accessibility", "TalkBack, magnification, high contrast", Settings.ACTION_ACCESSIBILITY_SETTINGS, "Accessibility")
    )

    private fun openIntent(intent: Intent, label: String): Result<String> {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Result.success("Opened $label.")
            } else {
                // Fallback to general settings
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
                Result.success("Opened Android Settings for $label.")
            }
        } catch (e: Exception) {
            Result.failure(Exception("Unable to open $label: ${e.localizedMessage}"))
        }
    }

    fun openSettingById(settingId: String): Result<String> {
        val target = availableSettings.firstOrNull { 
            it.id.equals(settingId.trim(), ignoreCase = true) || 
            it.title.contains(settingId.trim(), ignoreCase = true) 
        }
        return if (target != null) {
            openIntent(Intent(target.action), target.title)
        } else {
            // General fallback
            openIntent(Intent(Settings.ACTION_SETTINGS), "Android Settings")
        }
    }

    fun openWifiSettings(): Result<String> = openIntent(Intent(Settings.ACTION_WIFI_SETTINGS), "Wi-Fi Settings")
    fun openBluetoothSettings(): Result<String> = openIntent(Intent(Settings.ACTION_BLUETOOTH_SETTINGS), "Bluetooth Settings")
    fun openDisplaySettings(): Result<String> = openIntent(Intent(Settings.ACTION_DISPLAY_SETTINGS), "Display Settings")
    fun openSoundSettings(): Result<String> = openIntent(Intent(Settings.ACTION_SOUND_SETTINGS), "Sound & Vibration Settings")
    fun openBatterySettings(): Result<String> = openIntent(Intent(Intent.ACTION_POWER_USAGE_SUMMARY), "Battery Settings")
    fun openLocationSettings(): Result<String> = openIntent(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), "Location Settings")
    fun openAccessibilitySettings(): Result<String> = openIntent(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), "Accessibility Settings")
    fun openNotificationSettings(): Result<String> = openIntent(Intent(Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS), "Notification Settings")
    fun openStorageSettings(): Result<String> = openIntent(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS), "Storage Settings")
    fun openAppSettings(): Result<String> = openIntent(Intent(Settings.ACTION_APPLICATION_SETTINGS), "Apps Settings")
    fun openSecuritySettings(): Result<String> = openIntent(Intent(Settings.ACTION_SECURITY_SETTINGS), "Security Settings")
    fun openDateTimeSettings(): Result<String> = openIntent(Intent(Settings.ACTION_DATE_SETTINGS), "Date & Time Settings")
    fun openDeveloperSettings(): Result<String> = openIntent(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS), "Developer Options")
    fun openDeviceInfoSettings(): Result<String> = openIntent(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS), "Device Info Settings")
    fun openPrivacySettings(): Result<String> = openIntent(Intent(Settings.ACTION_PRIVACY_SETTINGS), "Privacy Settings")

    fun openPowerSaverSettings(): Result<String> {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
        } else {
            Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
        }
        return openIntent(intent, "Battery Saver & Power Optimization")
    }

    fun openWifiPanel(): Result<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            openIntent(Intent(Settings.Panel.ACTION_WIFI), "Quick Wi-Fi Panel")
        } else {
            openWifiSettings()
        }
    }

    fun openBluetoothPanel(): Result<String> {
        return openIntent(Intent(Settings.ACTION_BLUETOOTH_SETTINGS), "Bluetooth Settings")
    }

    fun openVolumePanel(): Result<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            openIntent(Intent(Settings.Panel.ACTION_VOLUME), "Quick Volume Control Panel")
        } else {
            openSoundSettings()
        }
    }

    fun openHotspotSettings(): Result<String> {
        val intent = Intent().apply {
            setClassName("com.android.settings", "com.android.settings.TetherSettings")
        }
        val res = openIntent(intent, "Hotspot & Tethering")
        return if (res.isSuccess) res else openIntent(Intent(Settings.ACTION_WIRELESS_SETTINGS), "Wireless & Tethering Settings")
    }

    fun openUrl(url: String): Result<String> {
        return try {
            val validUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened URL: $validUrl")
        } catch (e: Exception) {
            Result.failure(Exception("Cannot open URL '$url': ${e.localizedMessage}"))
        }
    }

    fun shareText(text: String, title: String? = null): Result<String> {
        return try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, title ?: "Share via Dart AI").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(shareIntent)
            Result.success("Opened Android Share Sheet.")
        } catch (e: Exception) {
            Result.failure(Exception("Cannot share text: ${e.localizedMessage}"))
        }
    }
}
