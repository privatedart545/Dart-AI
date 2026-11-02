package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.data.preferences.DevAiPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class VoiceOption(
    val name: String,
    val displayName: String,
    val locale: String,
    val isNetworkRequired: Boolean
)

sealed class VoiceState {
    object Idle : VoiceState()
    object Listening : VoiceState()
    data class Transcribing(val partialText: String) : VoiceState()
    data class SpeechReady(val fullText: String) : VoiceState()
    data class Speaking(val text: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}

class VoiceAssistantManager(private val context: Context) {

    private val preferences = DevAiPreferences(context)
    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized: Boolean = false

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                applyConfiguredVoice()
                textToSpeech?.setSpeechRate(preferences.speechRate)
                textToSpeech?.setPitch(preferences.speechPitch)
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        if (_voiceState.value is VoiceState.Speaking) {
                            _voiceState.value = VoiceState.Idle
                        }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        if (_voiceState.value is VoiceState.Speaking) {
                            _voiceState.value = VoiceState.Idle
                        }
                    }
                })
            }
        }
    }

    private fun applyConfiguredVoice() {
        val tts = textToSpeech ?: return
        val savedVoiceName = preferences.selectedVoiceName
        if (savedVoiceName.isNotBlank()) {
            val matchedVoice = tts.voices?.firstOrNull { it.name.equals(savedVoiceName, ignoreCase = true) }
            if (matchedVoice != null) {
                tts.voice = matchedVoice
                return
            }
        }
        tts.language = Locale.getDefault()
    }

    fun getAvailableVoices(): List<VoiceOption> {
        val tts = textToSpeech ?: return emptyList()
        val voices = tts.voices ?: return emptyList()
        return voices.map { v ->
            val prettyName = v.name
                .replace("#", " ")
                .replace("_", " ")
                .replace("-", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
            VoiceOption(
                name = v.name,
                displayName = "$prettyName (${v.locale.displayLanguage})",
                locale = v.locale.toLanguageTag(),
                isNetworkRequired = v.isNetworkConnectionRequired
            )
        }.sortedBy { it.displayName }
    }

    fun setCustomVoice(voiceName: String): Result<String> {
        val tts = textToSpeech ?: return Result.failure(Exception("TextToSpeech engine is not initialized yet."))
        val voices = tts.voices
        if (voices.isNullOrEmpty()) {
            return Result.failure(Exception("No voices available on this device."))
        }
        val target = voices.firstOrNull { 
            it.name.equals(voiceName.trim(), ignoreCase = true) || 
            it.name.contains(voiceName.trim(), ignoreCase = true) 
        } ?: return Result.failure(Exception("Voice '$voiceName' not found. Available voices: ${voices.take(5).joinToString { it.name }}"))

        tts.voice = target
        preferences.selectedVoiceName = target.name
        preferences.selectedVoiceLocale = target.locale.toLanguageTag()
        return Result.success("Voice successfully changed to '${target.name}' (${target.locale.displayName}).")
    }

    fun startListening() {
        stopSpeaking()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _voiceState.value = VoiceState.Error("Speech recognition is not available on this device.")
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _voiceState.value = VoiceState.Listening
                    }

                    override fun onBeginningOfSpeech() {
                        _voiceState.value = VoiceState.Listening
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                            SpeechRecognizer.ERROR_CLIENT -> "Client error occurred."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                            SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition."
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout."
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please try again."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy."
                            SpeechRecognizer.ERROR_SERVER -> "Server error."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
                            else -> "Voice recognition error ($error)."
                        }
                        _voiceState.value = VoiceState.Error(errorMsg)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            _voiceState.value = VoiceState.SpeechReady(text)
                        } else {
                            _voiceState.value = VoiceState.Idle
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            _voiceState.value = VoiceState.Transcribing(text)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
            _voiceState.value = VoiceState.Listening
        } catch (e: Exception) {
            _voiceState.value = VoiceState.Error("Failed to start voice listener: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
    }

    fun speak(text: String) {
        if (!isTtsInitialized || text.isBlank()) return
        stopSpeaking()
        textToSpeech?.setSpeechRate(preferences.speechRate)
        textToSpeech?.setPitch(preferences.speechPitch)

        // Clean out markdown symbols for natural TTS speech
        val cleanSpeech = text
            .replace(Regex("[*#`_~]"), "")
            .replace(Regex("\\bhttps?://\\S+"), "link")

        _voiceState.value = VoiceState.Speaking(cleanSpeech)
        textToSpeech?.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, null, "dev_ai_utterance_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
            if (_voiceState.value is VoiceState.Speaking) {
                _voiceState.value = VoiceState.Idle
            }
        } catch (_: Exception) {}
    }

    fun resetState() {
        _voiceState.value = VoiceState.Idle
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (_: Exception) {}
    }
}
