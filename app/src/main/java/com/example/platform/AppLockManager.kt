package com.example.platform

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import com.example.data.preferences.DevAiPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors

data class LockedAppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean
)

data class BiometricLockState(
    val isPromptActive: Boolean = false,
    val targetAppOrAction: String = "",
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false
)

class AppLockManager(
    private val context: Context,
    private val preferences: DevAiPreferences
) {
    private val packageManager: PackageManager = context.packageManager
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

    private val _lockState = MutableStateFlow(BiometricLockState())
    val lockState: StateFlow<BiometricLockState> = _lockState.asStateFlow()

    private var cancellationSignal: CancellationSignal? = null

    fun isAppLockFeatureEnabled(): Boolean = preferences.appLockEnabled

    fun setAppLockFeatureEnabled(enabled: Boolean) {
        preferences.appLockEnabled = enabled
    }

    fun isBiometricHardwareAvailable(): Boolean {
        return if (keyguardManager?.isDeviceSecure == true) {
            true
        } else {
            // Check if biometric sensor exists
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        }
    }

    fun isAppLocked(packageName: String): Boolean {
        if (!preferences.appLockEnabled) return false
        val cleanPkg = packageName.trim().lowercase()
        return preferences.getLockedPackages().any { it.equals(cleanPkg, ignoreCase = true) }
    }

    fun getLockedApps(): List<LockedAppInfo> {
        val lockedPackages = preferences.getLockedPackages()
        return lockedPackages.map { pkg ->
            val appName = try {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                pkg
            }
            val isSystem = try {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (e: Exception) {
                false
            }
            LockedAppInfo(packageName = pkg, appName = appName, isSystemApp = isSystem)
        }.sortedBy { it.appName }
    }

    fun lockApp(targetQuery: String): String {
        val resolved = resolvePackageName(targetQuery)
            ?: return "App matching '$targetQuery' was not found on this device. Please check the app name."

        preferences.addLockedPackage(resolved.packageName)
        preferences.appLockEnabled = true
        return "App '${resolved.appName}' (${resolved.packageName}) is now LOCKED. DEV AI biometric security will protect it."
    }

    fun unlockApp(targetQuery: String): String {
        val resolved = resolvePackageName(targetQuery)
        val targetPkg = resolved?.packageName ?: targetQuery.trim().lowercase()

        preferences.removeLockedPackage(targetPkg)
        val name = resolved?.appName ?: targetPkg
        return "App '$name' has been removed from the locked apps list."
    }

    fun requestBiometricAuth(
        activity: Activity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            cancellationSignal?.cancel()
            val signal = CancellationSignal().also { cancellationSignal = it }

            val executor = Executors.newSingleThreadExecutor()
            val prompt = BiometricPrompt.Builder(activity)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription("Verify your identity with fingerprint, face, or device PIN to continue.")
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        setDeviceCredentialAllowed(true)
                    } else {
                        setNegativeButton("Cancel", executor) { _, _ ->
                            activity.runOnUiThread { onError("Authentication cancelled by user.") }
                        }
                    }
                }
                .build()

            prompt.authenticate(
                signal,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        activity.runOnUiThread {
                            _lockState.value = BiometricLockState(isAuthenticated = true)
                            onSuccess()
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        super.onAuthenticationError(errorCode, errString)
                        activity.runOnUiThread {
                            val msg = errString?.toString() ?: "Authentication error code $errorCode"
                            _lockState.value = BiometricLockState(errorMessage = msg)
                            onError(msg)
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        activity.runOnUiThread {
                            _lockState.value = BiometricLockState(errorMessage = "Biometric not recognized. Please retry.")
                        }
                    }
                }
            )
        } else {
            // Pre-Android P fallback via KeyguardManager
            if (keyguardManager?.isDeviceSecure == true) {
                val intent = keyguardManager.createConfirmDeviceCredentialIntent(title, subtitle)
                if (intent != null) {
                    activity.startActivity(intent)
                    onSuccess()
                } else {
                    onSuccess()
                }
            } else {
                onSuccess()
            }
        }
    }

    fun cancelAuthentication() {
        cancellationSignal?.cancel()
        cancellationSignal = null
        _lockState.value = BiometricLockState()
    }

    private fun resolvePackageName(query: String): LockedAppInfo? {
        val cleanQuery = query.trim().lowercase()
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        // Exact package name match
        val exactPkg = installedApps.firstOrNull { it.packageName.equals(cleanQuery, ignoreCase = true) }
        if (exactPkg != null) {
            return LockedAppInfo(
                packageName = exactPkg.packageName,
                appName = packageManager.getApplicationLabel(exactPkg).toString(),
                isSystemApp = (exactPkg.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            )
        }

        // Exact app label match
        val exactLabel = installedApps.firstOrNull {
            packageManager.getApplicationLabel(it).toString().equals(cleanQuery, ignoreCase = true)
        }
        if (exactLabel != null) {
            return LockedAppInfo(
                packageName = exactLabel.packageName,
                appName = packageManager.getApplicationLabel(exactLabel).toString(),
                isSystemApp = (exactLabel.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            )
        }

        // Fuzzy contains match
        val fuzzy = installedApps.firstOrNull {
            packageManager.getApplicationLabel(it).toString().lowercase().contains(cleanQuery)
        }
        if (fuzzy != null) {
            return LockedAppInfo(
                packageName = fuzzy.packageName,
                appName = packageManager.getApplicationLabel(fuzzy).toString(),
                isSystemApp = (fuzzy.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            )
        }

        return null
    }
}
