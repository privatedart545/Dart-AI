package com.example.platform

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build

class FlashlightManager(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private var cameraIdWithFlash: String? = null
    private var isTorchOn: Boolean = false

    init {
        findCameraWithFlash()
    }

    private fun findCameraWithFlash() {
        if (cameraManager == null) return
        try {
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraIdWithFlash = id
                    break
                }
            }
            if (cameraIdWithFlash == null && cameraManager.cameraIdList.isNotEmpty()) {
                cameraIdWithFlash = cameraManager.cameraIdList.firstOrNull()
            }
        } catch (_: Exception) {
            cameraIdWithFlash = null
        }
    }

    fun turnOn(): Result<Boolean> {
        val id = cameraIdWithFlash ?: return Result.failure(Exception("No camera flash hardware detected on this device."))
        return try {
            cameraManager?.setTorchMode(id, true)
            isTorchOn = true
            Result.success(true)
        } catch (e: CameraAccessException) {
            Result.failure(Exception("Unable to access camera torch: ${e.localizedMessage}"))
        } catch (e: Exception) {
            Result.failure(Exception("Flashlight error: ${e.localizedMessage}"))
        }
    }

    fun turnOff(): Result<Boolean> {
        val id = cameraIdWithFlash ?: return Result.failure(Exception("No camera flash hardware detected."))
        return try {
            cameraManager?.setTorchMode(id, false)
            isTorchOn = false
            Result.success(false)
        } catch (e: CameraAccessException) {
            Result.failure(Exception("Unable to turn off torch: ${e.localizedMessage}"))
        } catch (e: Exception) {
            Result.failure(Exception("Flashlight error: ${e.localizedMessage}"))
        }
    }

    fun toggle(): Result<Boolean> {
        return if (isTorchOn) turnOff() else turnOn()
    }

    fun getStatus(): Boolean {
        return isTorchOn
    }
}
