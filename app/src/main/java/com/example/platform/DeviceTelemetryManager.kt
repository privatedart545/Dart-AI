package com.example.platform

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BatteryInfo(
    val percentage: Int,
    val isCharging: Boolean,
    val chargingSource: String,
    val powerSaveMode: Boolean
)

data class StorageInfo(
    val totalGb: Double,
    val freeGb: Double,
    val usedGb: Double,
    val usedPercent: Int
)

class DeviceTelemetryManager(private val context: Context) {

    fun getBatteryStatus(): BatteryInfo {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, filter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 0

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val source = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable"
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Wall Charger"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Charging"
            else -> if (isCharging) "Charging" else "On Battery"
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        val isPowerSave = powerManager?.isPowerSaveMode ?: false

        return BatteryInfo(
            percentage = percent,
            isCharging = isCharging,
            chargingSource = source,
            powerSaveMode = isPowerSave
        )
    }

    fun getDeviceStorageInfo(): StorageInfo {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = totalBytes - freeBytes

            val bytesInGb = 1024.0 * 1024.0 * 1024.0
            val totalGb = String.format(Locale.US, "%.1f", totalBytes / bytesInGb).toDoubleOrNull() ?: 0.0
            val freeGb = String.format(Locale.US, "%.1f", freeBytes / bytesInGb).toDoubleOrNull() ?: 0.0
            val usedGb = String.format(Locale.US, "%.1f", usedBytes / bytesInGb).toDoubleOrNull() ?: 0.0
            val usedPercent = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt() else 0

            StorageInfo(totalGb, freeGb, usedGb, usedPercent)
        } catch (_: Exception) {
            StorageInfo(0.0, 0.0, 0.0, 0)
        }
    }

    fun getNetworkStatus(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "Network Service Unavailable"

        val activeNetwork = cm.activeNetwork ?: return "Offline (No active internet connection)"
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return "Offline"

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Online via Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Online via Mobile Cellular Data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Online via Ethernet"
            else -> "Connected to network"
        }
    }

    fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("h:mm:ss a z", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        return sdf.format(Date())
    }
}
