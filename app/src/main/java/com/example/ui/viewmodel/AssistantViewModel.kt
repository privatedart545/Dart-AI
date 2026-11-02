package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.agent.AgentOrchestrator
import com.example.agent.OrchestratorEvent
import com.example.agent.ToolExecutionReport
import com.example.agent.ToolExecutor
import com.example.agent.ToolRegistry
import com.example.data.api.ApiMessage
import com.example.data.api.OpenRouterClient
import com.example.data.db.ChatMessageEntity
import com.example.data.db.ChatSessionEntity
import com.example.data.db.DevAiDatabase
import com.example.data.preferences.DevAiPreferences
import com.example.platform.AlarmAndCalendarManager
import com.example.platform.AppLaunchManager
import com.example.platform.AppLockManager
import com.example.platform.AppUsageAuditorManager
import com.example.platform.AudioControlManager
import com.example.platform.BatteryHealthManager
import com.example.platform.ClipboardAndMessagingManager
import com.example.platform.ContactsAndPhoneManager
import com.example.platform.CustomRoutineEngine
import com.example.platform.DailyBriefingManager
import com.example.platform.DeepDocumentManager
import com.example.platform.DeviceTelemetryManager
import com.example.platform.DisplayAndBrightnessManager
import com.example.platform.FlashlightManager
import com.example.platform.MathCalculatorManager
import com.example.platform.MediaControlManager
import com.example.platform.NotificationReaderManager
import com.example.platform.OfflineSurvivalManager
import com.example.platform.RoutinesAndDiagnosticsManager
import com.example.platform.SafFileManager
import com.example.platform.ScheduledAutomationManager
import com.example.platform.SecurityVaultManager
import com.example.platform.SystemSettingsManager
import com.example.platform.WakeWordManager
import com.example.platform.WebSearchManager
import com.example.voice.VoiceAssistantManager
import com.example.voice.VoiceState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class UiToolState(
    val toolCallId: String,
    val toolName: String,
    val arguments: String,
    val isRunning: Boolean,
    val resultText: String? = null,
    val isSuccess: Boolean? = null
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    val preferences = DevAiPreferences(context)
    private val database = DevAiDatabase.getInstance(context)
    val chatDao = database.chatDao()

    // Platform managers
    val flashlightManager = FlashlightManager(context)
    val appLaunchManager = AppLaunchManager(context)
    val safFileManager = SafFileManager(context)
    val audioControlManager = AudioControlManager(context)
    val systemSettingsManager = SystemSettingsManager(context)
    val deviceTelemetryManager = DeviceTelemetryManager(context)
    val deepDocumentManager = DeepDocumentManager(context, safFileManager)
    val alarmAndCalendarManager = AlarmAndCalendarManager(context)
    val clipboardAndMessagingManager = ClipboardAndMessagingManager(context)
    val routinesAndDiagnosticsManager = RoutinesAndDiagnosticsManager(
        context,
        flashlightManager,
        audioControlManager,
        deviceTelemetryManager,
        safFileManager
    )
    val webSearchManager = WebSearchManager()
    val mathCalculatorManager = MathCalculatorManager()
    val contactsAndPhoneManager = ContactsAndPhoneManager(context)
    val securityVaultManager = SecurityVaultManager(context)
    val dailyBriefingManager = DailyBriefingManager(deviceTelemetryManager, audioControlManager)
    val appUsageAuditorManager = AppUsageAuditorManager(context)
    val notificationReaderManager = NotificationReaderManager(context)
    val scheduledAutomationManager = ScheduledAutomationManager(context)
    val displayAndBrightnessManager = DisplayAndBrightnessManager(context)
    val appLockManager = AppLockManager(context, preferences)
    val wakeWordManager = WakeWordManager(context, preferences)
    val batteryHealthManager = BatteryHealthManager(context)
    val mediaControlManager = MediaControlManager(context)
    val offlineSurvivalManager = OfflineSurvivalManager(
        flashlightManager,
        audioControlManager,
        deviceTelemetryManager,
        systemSettingsManager,
        alarmAndCalendarManager,
        mathCalculatorManager,
        contactsAndPhoneManager,
        dailyBriefingManager
    )

    // Voice manager
    val voiceManager = VoiceAssistantManager(context)
    val voiceState: StateFlow<VoiceState> = voiceManager.voiceState

    val customRoutineEngine = CustomRoutineEngine(
        context,
        preferences,
        voiceManager,
        flashlightManager,
        displayAndBrightnessManager,
        audioControlManager,
        mediaControlManager
    )

    // API & Agent
    val openRouterClient = OpenRouterClient()
    val toolRegistry = ToolRegistry(
        flashlightManager = flashlightManager,
        appLaunchManager = appLaunchManager,
        safFileManager = safFileManager,
        audioControlManager = audioControlManager,
        systemSettingsManager = systemSettingsManager,
        deviceTelemetryManager = deviceTelemetryManager,
        deepDocumentManager = deepDocumentManager,
        alarmAndCalendarManager = alarmAndCalendarManager,
        clipboardAndMessagingManager = clipboardAndMessagingManager,
        routinesAndDiagnosticsManager = routinesAndDiagnosticsManager,
        webSearchManager = webSearchManager,
        mathCalculatorManager = mathCalculatorManager,
        contactsAndPhoneManager = contactsAndPhoneManager,
        securityVaultManager = securityVaultManager,
        dailyBriefingManager = dailyBriefingManager,
        appUsageAuditorManager = appUsageAuditorManager,
        notificationReaderManager = notificationReaderManager,
        scheduledAutomationManager = scheduledAutomationManager,
        voiceAssistantManager = voiceManager,
        displayAndBrightnessManager = displayAndBrightnessManager,
        appLockManager = appLockManager,
        wakeWordManager = wakeWordManager,
        batteryHealthManager = batteryHealthManager,
        customRoutineEngine = customRoutineEngine,
        mediaControlManager = mediaControlManager,
        exportChatAction = { exportChatHistoryToMarkdown() }
    )
    private val toolExecutor = ToolExecutor(toolRegistry, context)
    private val orchestrator = AgentOrchestrator(
        openRouterClient,
        toolRegistry,
        toolExecutor,
        preferences
    )

    // Current Session & Messages
    private val _currentSessionId = MutableStateFlow("default_session")
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    val currentMessages: StateFlow<List<ChatMessageEntity>> = chatDao
        .getMessagesForSession("default_session")
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Theme & Features State
    private val _selectedTheme = MutableStateFlow(preferences.selectedTheme)
    val selectedTheme: StateFlow<String> = _selectedTheme.asStateFlow()

    private val _isDarkMode = MutableStateFlow(preferences.isDarkMode)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _shakeToWakeEnabled = MutableStateFlow(preferences.shakeToWakeEnabled)
    val shakeToWakeEnabled: StateFlow<Boolean> = _shakeToWakeEnabled.asStateFlow()

    private val _hasCompletedTutorial = MutableStateFlow(preferences.hasCompletedTutorial)
    val hasCompletedTutorial: StateFlow<Boolean> = _hasCompletedTutorial.asStateFlow()

    private val _clipboardPreview = MutableStateFlow<String?>(null)
    val clipboardPreview: StateFlow<String?> = _clipboardPreview.asStateFlow()

    private val _attachedImageUri = MutableStateFlow<Uri?>(null)
    val attachedImageUri: StateFlow<Uri?> = _attachedImageUri.asStateFlow()

    // UI States
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _activeToolState = MutableStateFlow<UiToolState?>(null)
    val activeToolState: StateFlow<UiToolState?> = _activeToolState.asStateFlow()

    private val _apiTestStatus = MutableStateFlow<String?>(null)
    val apiTestStatus: StateFlow<String?> = _apiTestStatus.asStateFlow()

    private val _isTestingApi = MutableStateFlow(false)
    val isTestingApi: StateFlow<Boolean> = _isTestingApi.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isVoiceModalOpen = MutableStateFlow(false)
    val isVoiceModalOpen: StateFlow<Boolean> = _isVoiceModalOpen.asStateFlow()

    init {
        initSession()
        listenToOrchestratorEvents()
        listenToVoiceResults()

        appLaunchManager.appLockChecker = { packageName ->
            appLockManager.isAppLocked(packageName)
        }

        wakeWordManager.onWakeWordTriggered = {
            openVoiceModal()
        }

        if (preferences.wakeWordEnabled) {
            wakeWordManager.startListening()
        }
    }

    private fun initSession() {
        viewModelScope.launch {
            val session = chatDao.getSessionById("default_session")
            if (session == null) {
                chatDao.insertSession(
                    ChatSessionEntity(
                        id = "default_session",
                        title = "Main Conversation"
                    )
                )
            }
        }
    }

    private fun listenToOrchestratorEvents() {
        viewModelScope.launch {
            orchestrator.events.collect { event ->
                when (event) {
                    is OrchestratorEvent.ToolStarted -> {
                        _activeToolState.value = UiToolState(
                            toolCallId = event.toolCallId,
                            toolName = event.toolName,
                            arguments = event.argsJson,
                            isRunning = true
                        )
                    }
                    is OrchestratorEvent.ToolCompleted -> {
                        _activeToolState.value = UiToolState(
                            toolCallId = event.report.toolCallId,
                            toolName = event.report.toolName,
                            arguments = event.report.argumentsJson,
                            isRunning = false,
                            resultText = event.report.resultText,
                            isSuccess = event.report.isSuccess
                        )
                    }
                    is OrchestratorEvent.AgentReasoning -> {
                        // Ongoing reasoning round
                    }
                }
            }
        }
    }

    private fun listenToVoiceResults() {
        viewModelScope.launch {
            voiceState.collect { state ->
                if (state is VoiceState.SpeechReady) {
                    val spokenText = state.fullText
                    if (spokenText.isNotBlank()) {
                        sendMessage(spokenText, triggeredByVoice = true)
                    }
                }
            }
        }
    }

    fun openVoiceModal() {
        _isVoiceModalOpen.value = true
        voiceManager.startListening()
    }

    fun closeVoiceModal() {
        _isVoiceModalOpen.value = false
        voiceManager.stopListening()
        voiceManager.stopSpeaking()
        voiceManager.resetState()
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    fun toggleDarkMode() {
        val next = !_isDarkMode.value
        _isDarkMode.value = next
        preferences.isDarkMode = next
    }

    fun attachImage(uri: Uri?) {
        _attachedImageUri.value = uri
    }

    fun clearAttachedImage() {
        _attachedImageUri.value = null
    }

    private fun isExplicitLocalCommand(input: String): Boolean {
        val lower = input.lowercase()
        return lower.contains("flashlight") || lower.contains("torch") ||
                lower.contains("mute") || lower.contains("unmute") ||
                lower.startsWith("calculate ") || lower.startsWith("calc ") ||
                lower.startsWith("dial ") || lower.contains("briefing")
    }

    fun sendMessage(prompt: String, triggeredByVoice: Boolean = false) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty() || _isGenerating.value) return

        val attachedUri = _attachedImageUri.value
        val effectivePrompt = if (attachedUri != null) {
            "📷 [Attached Image / Vision Scan]: $trimmed"
        } else trimmed

        _errorMessage.value = null
        _isGenerating.value = true
        _activeToolState.value = null

        val sessionId = _currentSessionId.value

        viewModelScope.launch {
            try {
                // Check Offline Survival Engine
                val offline = offlineSurvivalManager.handleOfflineIntent(trimmed)
                val isOfflineEligible = offline.handled && (preferences.openRouterApiKey.isBlank() || isExplicitLocalCommand(trimmed))

                if (isOfflineEligible) {
                    chatDao.insertMessage(
                        ChatMessageEntity(
                            sessionId = sessionId,
                            role = "user",
                            content = effectivePrompt
                        )
                    )
                    chatDao.insertMessage(
                        ChatMessageEntity(
                            sessionId = sessionId,
                            role = "assistant",
                            content = offline.responseText
                        )
                    )
                    if (preferences.autoSpeak && (triggeredByVoice || _isVoiceModalOpen.value)) {
                        voiceManager.speak(offline.spokenText)
                    }
                    _attachedImageUri.value = null
                    return@launch
                }

                // 1. Insert user message in database
                chatDao.insertMessage(
                    ChatMessageEntity(
                        sessionId = sessionId,
                        role = "user",
                        content = effectivePrompt
                    )
                )
                _attachedImageUri.value = null

                // 2. Fetch recent messages for LLM context
                val historyEntities = chatDao.getMessagesListForSession(sessionId)
                val apiHistory = historyEntities.takeLast(10).mapNotNull {
                    when (it.role) {
                        "user" -> ApiMessage(role = "user", content = it.content)
                        "assistant" -> ApiMessage(role = "assistant", content = it.content)
                        "tool" -> ApiMessage(
                            role = "tool",
                            name = it.toolName,
                            toolCallId = it.toolCallId,
                            content = it.toolResult
                        )
                        else -> null
                    }
                }

                // 3. Process turn through Agent Orchestrator
                val result = orchestrator.processUserTurn(trimmed, apiHistory)

                if (result.isSuccess) {
                    val agentResult = result.getOrThrow()

                    // Record tool execution messages in database if any
                    for (report in agentResult.toolReports) {
                        chatDao.insertMessage(
                            ChatMessageEntity(
                                sessionId = sessionId,
                                role = "tool",
                                content = null,
                                toolCallId = report.toolCallId,
                                toolName = report.toolName,
                                toolArguments = report.argumentsJson,
                                toolResult = report.resultText,
                                toolStatus = if (report.isSuccess) "SUCCESS" else "FAILED"
                            )
                        )
                    }

                    // Record assistant final message
                    chatDao.insertMessage(
                        ChatMessageEntity(
                            sessionId = sessionId,
                            role = "assistant",
                            content = agentResult.replyText
                        )
                    )

                    // Auto-speak if enabled
                    if (preferences.autoSpeak && (triggeredByVoice || _isVoiceModalOpen.value)) {
                        voiceManager.speak(agentResult.replyText)
                    }
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Unknown agent error"
                    _errorMessage.value = err
                    chatDao.insertMessage(
                        ChatMessageEntity(
                            sessionId = sessionId,
                            role = "assistant",
                            content = "⚠️ $err"
                        )
                    )
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isGenerating.value = false
                _activeToolState.value = null
            }
        }
    }

    fun testOpenRouterConnection() {
        val key = preferences.openRouterApiKey
        val model = preferences.modelId
        _isTestingApi.value = true
        _apiTestStatus.value = "Connecting to OpenRouter..."

        viewModelScope.launch {
            val res = openRouterClient.testConnection(key, model)
            _isTestingApi.value = false
            if (res.isSuccess) {
                _apiTestStatus.value = "✅ Connected to Nemotron 3 Ultra: ${res.getOrThrow()}"
            } else {
                _apiTestStatus.value = "❌ ${res.exceptionOrNull()?.message ?: "Connection failed"}"
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatDao.clearMessagesForSession(_currentSessionId.value)
            _errorMessage.value = null
            _activeToolState.value = null
        }
    }

    fun onFolderSelected(uri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            preferences.addAuthorizedFolder(uri.toString())
        } catch (_: Exception) {
            preferences.addAuthorizedFolder(uri.toString())
        }
    }

    fun removeFolder(uriString: String) {
        preferences.removeAuthorizedFolder(uriString)
    }

    fun setTheme(theme: String) {
        preferences.selectedTheme = theme
        _selectedTheme.value = theme
    }

    fun setShakeToWake(enabled: Boolean) {
        preferences.shakeToWakeEnabled = enabled
        _shakeToWakeEnabled.value = enabled
    }

    fun completeTutorial() {
        preferences.hasCompletedTutorial = true
        _hasCompletedTutorial.value = true
    }

    fun resetTutorial() {
        preferences.hasCompletedTutorial = false
        _hasCompletedTutorial.value = false
    }

    fun checkClipboard() {
        val text = clipboardAndMessagingManager.getClipboardText()
        if (!text.isNullOrBlank() && text.length <= 1000) {
            _clipboardPreview.value = text.trim()
        } else {
            _clipboardPreview.value = null
        }
    }

    fun clearClipboardPreview() {
        _clipboardPreview.value = null
    }

    suspend fun exportChatHistoryToMarkdown(): Result<String> {
        val sessionId = _currentSessionId.value
        val messages = chatDao.getMessagesListForSession(sessionId)
        if (messages.isEmpty()) {
            return Result.failure(Exception("No messages to export in the current session."))
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
        val dateDisplay = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val fileName = "Dart_AI_Chat_Export_${sdf.format(Date())}.md"

        val sb = StringBuilder()
        sb.append("# Dart AI Conversation Export\n")
        sb.append("**Assistant:** Dart AI\n")
        sb.append("**Developer:** DEV L KAKKOTH\n")
        sb.append("**Tagline:** Your Personal AI. Your Phone. Your Control.\n")
        sb.append("**Export Date:** $dateDisplay\n\n")
        sb.append("---\n\n")

        for (msg in messages) {
            when (msg.role) {
                "user" -> {
                    sb.append("### 👤 User\n")
                    sb.append("${msg.content}\n\n")
                }
                "assistant" -> {
                    sb.append("### 🤖 Dart AI\n")
                    sb.append("${msg.content}\n\n")
                }
                "tool" -> {
                    sb.append("<details><summary>🔧 Tool: ${msg.toolName} (${msg.toolStatus})</summary>\n\n")
                    if (!msg.toolArguments.isNullOrBlank()) {
                        sb.append("**Arguments:**\n```json\n${msg.toolArguments}\n```\n\n")
                    }
                    if (!msg.toolResult.isNullOrBlank()) {
                        sb.append("**Result:**\n```\n${msg.toolResult}\n```\n\n")
                    }
                    sb.append("</details>\n\n")
                }
            }
        }
        sb.append("---\n*Exported by Dart AI - Developed by DEV L KAKKOTH*\n")

        return safFileManager.createFile(fileName, sb.toString())
    }

    override fun onCleared() {
        super.onCleared()
        customRoutineEngine.stopTriggerMonitoring()
        wakeWordManager.stopListening()
        appLockManager.cancelAuthentication()
        voiceManager.destroy()
    }
}
