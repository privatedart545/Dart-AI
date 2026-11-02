package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences

class DevAiPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("dev_ai_user_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_OPENROUTER_API_KEY = "openrouter_api_key"
        private const val KEY_MODEL_ID = "model_id"
        private const val DEFAULT_MODEL = "nvidia/nemotron-3-ultra-550b-a55b"
        private const val KEY_AUTO_SPEAK = "auto_speak_voice"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_SPEECH_PITCH = "speech_pitch"
        private const val KEY_SAF_FOLDER_URIS = "saf_authorized_folder_uris"
        private const val KEY_TUTORIAL_COMPLETED = "has_completed_tutorial"
        private const val KEY_THEME = "selected_theme"
        private const val KEY_SHAKE_TO_WAKE = "shake_to_wake_enabled"
        private const val KEY_DARK_MODE = "dark_mode_enabled"
        private const val KEY_SELECTED_VOICE_NAME = "selected_voice_name"
        private const val KEY_SELECTED_VOICE_LOCALE = "selected_voice_locale"
    }

    var selectedVoiceName: String
        get() = prefs.getString(KEY_SELECTED_VOICE_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SELECTED_VOICE_NAME, value).apply()

    var selectedVoiceLocale: String
        get() = prefs.getString(KEY_SELECTED_VOICE_LOCALE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SELECTED_VOICE_LOCALE, value).apply()

    var openRouterApiKey: String
        get() = prefs.getString(KEY_OPENROUTER_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OPENROUTER_API_KEY, value.trim()).apply()

    var modelId: String
        get() = prefs.getString(KEY_MODEL_ID, DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) = prefs.edit().putString(KEY_MODEL_ID, value.trim().ifEmpty { DEFAULT_MODEL }).apply()

    var autoSpeak: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SPEAK, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SPEAK, value).apply()

    var speechRate: Float
        get() = prefs.getFloat(KEY_SPEECH_RATE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_RATE, value).apply()

    var speechPitch: Float
        get() = prefs.getFloat(KEY_SPEECH_PITCH, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_PITCH, value).apply()

    var hasCompletedTutorial: Boolean
        get() = prefs.getBoolean(KEY_TUTORIAL_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_TUTORIAL_COMPLETED, value).apply()

    var selectedTheme: String
        get() = prefs.getString(KEY_THEME, "cyan") ?: "cyan"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    var shakeToWakeEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHAKE_TO_WAKE, true)
        set(value) = prefs.edit().putBoolean(KEY_SHAKE_TO_WAKE, value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    fun getAuthorizedFolders(): Set<String> {
        return prefs.getStringSet(KEY_SAF_FOLDER_URIS, emptySet()) ?: emptySet()
    }

    fun addAuthorizedFolder(uriString: String) {
        val current = getAuthorizedFolders().toMutableSet()
        current.add(uriString)
        prefs.edit().putStringSet(KEY_SAF_FOLDER_URIS, current).apply()
    }

    fun removeAuthorizedFolder(uriString: String) {
        val current = getAuthorizedFolders().toMutableSet()
        current.remove(uriString)
        prefs.edit().putStringSet(KEY_SAF_FOLDER_URIS, current).apply()
    }
}
