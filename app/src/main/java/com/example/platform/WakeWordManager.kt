package com.example.platform

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.data.preferences.DevAiPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class WakeWordManager(
    private val context: Context,
    private val preferences: DevAiPreferences
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isCurrentlyListening = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastDetectedPhrase = MutableStateFlow<String?>(null)
    val lastDetectedPhrase: StateFlow<String?> = _lastDetectedPhrase.asStateFlow()

    private val _configuredPhrase = MutableStateFlow(preferences.wakeWordPhrase)
    val configuredPhrase: StateFlow<String> = _configuredPhrase.asStateFlow()

    var onWakeWordTriggered: ((String) -> Unit)? = null

    init {
        _configuredPhrase.value = preferences.wakeWordPhrase
    }

    fun getWakeWordPhrase(): String = preferences.wakeWordPhrase

    fun setWakeWordPhrase(phrase: String): String {
        val clean = phrase.trim().ifEmpty { "Hey Dev" }
        preferences.wakeWordPhrase = clean
        _configuredPhrase.value = clean
        return "Custom wake word successfully updated to: '$clean'."
    }

    fun isEnabled(): Boolean = preferences.wakeWordEnabled

    fun toggleWakeWordListening(enable: Boolean): String {
        preferences.wakeWordEnabled = enable
        if (enable) {
            startListening()
            return "Wake word detection ENABLED. DEV AI is now listening for '${preferences.wakeWordPhrase}'."
        } else {
            stopListening()
            return "Wake word detection DISABLED."
        }
    }

    fun startListening() {
        if (isCurrentlyListening) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return
        }

        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                }

                speechRecognizer?.startListening(intent)
                isCurrentlyListening = true
                _isListening.value = true
            } catch (e: Exception) {
                isCurrentlyListening = false
                _isListening.value = false
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                isCurrentlyListening = false
                _isListening.value = false
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            // Restart if user has wake word enabled
            if (preferences.wakeWordEnabled && isCurrentlyListening) {
                mainHandler.postDelayed({
                    if (preferences.wakeWordEnabled) {
                        try {
                            speechRecognizer?.cancel()
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                            }
                            speechRecognizer?.startListening(intent)
                        } catch (e: Exception) {
                            // Retry later
                        }
                    }
                }, 750)
            } else {
                isCurrentlyListening = false
                _isListening.value = false
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            checkMatchesForWakeWord(matches)

            // Loop listening if still enabled
            if (preferences.wakeWordEnabled) {
                mainHandler.postDelayed({
                    if (preferences.wakeWordEnabled) {
                        try {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                            }
                            speechRecognizer?.startListening(intent)
                        } catch (e: Exception) {
                            // Handled on next cycle
                        }
                    }
                }, 300)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            checkMatchesForWakeWord(matches)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun checkMatchesForWakeWord(matches: ArrayList<String>?) {
        if (matches.isNullOrEmpty()) return
        val targetPhrase = preferences.wakeWordPhrase.lowercase().trim()
        val defaultVariants = listOf("hey dev", "dev ai", "hey assistant", "ok dev")

        for (match in matches) {
            val lower = match.lowercase().trim()
            val isMatched = lower.contains(targetPhrase) ||
                    (targetPhrase == "hey dev" && defaultVariants.any { lower.contains(it) })

            if (isMatched) {
                triggerWakeWord(match)
                break
            }
        }
    }

    private fun triggerWakeWord(detectedText: String) {
        _lastDetectedPhrase.value = detectedText
        vibrateFeedback()
        mainHandler.post {
            onWakeWordTriggered?.invoke(detectedText)
        }
    }

    private fun vibrateFeedback() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(50)
                }
            }
        } catch (e: Exception) {
            // Ignore vibration error
        }
    }

    fun getStatusSummary(): String {
        val enabled = preferences.wakeWordEnabled
        val phrase = preferences.wakeWordPhrase
        return "Wake Word Detection: ${if (enabled) "ACTIVE (Listening)" else "INACTIVE"}\n" +
                "Configured Phrase: \"$phrase\"\n" +
                "Default Fallbacks: \"Hey Dev\", \"Dev AI\"\n" +
                "Speech Engine: Android SpeechRecognizer"
    }
}
