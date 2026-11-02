package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.AssistantViewModel
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AssistantViewModel,
    onBack: () -> Unit,
    onPickFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preferences = viewModel.preferences
    val scope = rememberCoroutineScope()

    var apiKeyInput by remember { mutableStateOf(preferences.openRouterApiKey) }
    var modelIdInput by remember { mutableStateOf(preferences.modelId) }
    var autoSpeakEnabled by remember { mutableStateOf(preferences.autoSpeak) }
    var speechRate by remember { mutableFloatStateOf(preferences.speechRate) }
    var speechPitch by remember { mutableFloatStateOf(preferences.speechPitch) }
    var showApiKey by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var exportStatusMessage by remember { mutableStateOf<String?>(null) }

    val apiTestStatus by viewModel.apiTestStatus.collectAsState()
    val isTestingApi by viewModel.isTestingApi.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val shakeToWakeEnabled by viewModel.shakeToWakeEnabled.collectAsState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentWindowInsets = WindowInsets.statusBars,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Dart AI Settings",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. OpenRouter & Nemotron 3 Ultra Configuration
            SectionCard(title = "AI ENGINE & OPENROUTER", icon = Icons.Default.Key) {
                Text(
                    text = "Configure your OpenRouter API key for NVIDIA Nemotron 3 Ultra reasoning.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = {
                        apiKeyInput = it
                        preferences.openRouterApiKey = it
                    },
                    label = { Text("OpenRouter API Key") },
                    placeholder = { Text("sk-or-v1-...") },
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle API Key Visibility",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_api_key_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = modelIdInput,
                    onValueChange = {
                        modelIdInput = it
                        preferences.modelId = it
                    },
                    label = { Text("Model ID") },
                    placeholder = { Text("nvidia/nemotron-3-ultra-550b-a55b") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_model_id_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.testOpenRouterConnection() },
                    enabled = !isTestingApi && apiKeyInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_test_connection_button")
                ) {
                    if (isTestingApi) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Testing Connection...")
                    } else {
                        Text("Test Connection", fontWeight = FontWeight.Bold)
                    }
                }

                if (apiTestStatus != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = apiTestStatus ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (apiTestStatus?.startsWith("✅") == true) StatusSuccess else StatusError,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // 2. Theme & Appearance
            SectionCard(title = "THEME & APPEARANCE", icon = Icons.Default.Palette) {
                // Dark / Light Mode Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isDarkMode) "Dark Theme" else "Light Theme",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = if (isDarkMode) "Cyber neon dark visual aesthetics" else "Crisp daylight high-contrast palette",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("settings_dark_mode_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Select Accent Cyber Palette:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                val themes = listOf(
                    Triple("cyan", "Cyber Cyan", androidx.compose.ui.graphics.Color(0xFF00E5FF)),
                    Triple("emerald", "Matrix Emerald", androidx.compose.ui.graphics.Color(0xFF00FF66)),
                    Triple("magenta", "Vapor Magenta", androidx.compose.ui.graphics.Color(0xFFFF007F)),
                    Triple("gold", "Solar Gold", androidx.compose.ui.graphics.Color(0xFFFFB700))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    themes.forEach { (themeKey, label, color) ->
                        val isSelected = selectedTheme.equals(themeKey, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setTheme(themeKey) },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                )
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.25f),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) color else MaterialTheme.colorScheme.outline,
                                selectedBorderColor = color,
                                enabled = true,
                                selected = isSelected
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_chip_$themeKey")
                        )
                    }
                }
            }

            // 3. Voice & Speech Synthesis
            SectionCard(title = "VOICE & SPEECH SYNTHESIS", icon = Icons.Default.Mic) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Speak Voice Responses",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "Speak back with TTS audio whenever you initiate with voice.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    Switch(
                        checked = autoSpeakEnabled,
                        onCheckedChange = {
                            autoSpeakEnabled = it
                            preferences.autoSpeak = it
                        },
                        modifier = Modifier.testTag("settings_auto_speak_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Voice Personality Presets
                Text(
                    text = "Voice Profile Presets:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        Triple("Cyber Dart", 1.0f, 1.1f),
                        Triple("Deep Cortex", 0.75f, 0.95f),
                        Triple("Quantum Speed", 1.25f, 1.35f),
                        Triple("Standard", 1.0f, 1.0f)
                    )
                    presets.forEach { (name, pitch, rate) ->
                        OutlinedButton(
                            onClick = {
                                speechPitch = pitch
                                speechRate = rate
                                preferences.speechPitch = pitch
                                preferences.speechRate = rate
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                        ) {
                            Text(name, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Speech Rate: ${String.format("%.1fx", speechRate)}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Slider(
                    value = speechRate,
                    onValueChange = {
                        speechRate = it
                        preferences.speechRate = it
                    },
                    valueRange = 0.5f..2.0f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Speech Pitch: ${String.format("%.1fx", speechPitch)}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Slider(
                    value = speechPitch,
                    onValueChange = {
                        speechPitch = it
                        preferences.speechPitch = it
                    },
                    valueRange = 0.5f..1.5f,
                    steps = 4,
                    colors = SliderDefaults.colors(
                        thumbColor = PurpleAccent,
                        activeTrackColor = PurpleAccent
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                var showVoicePicker by remember { mutableStateOf(false) }
                val availableVoices = remember { viewModel.voiceManager.getAvailableVoices() }
                var selectedVoiceName by remember { mutableStateOf(viewModel.preferences.selectedVoiceName) }

                OutlinedButton(
                    onClick = { showVoicePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("select_custom_voice_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedVoiceName.isNotBlank()) "Voice: ${selectedVoiceName.take(24)}" else "Select Custom TTS Voice (${availableVoices.size} available)"
                    )
                }

                if (showVoicePicker) {
                    AlertDialog(
                        onDismissRequest = { showVoicePicker = false },
                        title = { Text("Select Custom Voice", color = MaterialTheme.colorScheme.onSurface) },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                if (availableVoices.isEmpty()) {
                                    Text("No alternate TTS voices detected. System default voice is active.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    availableVoices.forEach { voice ->
                                        val isCurrent = selectedVoiceName.equals(voice.name, ignoreCase = true)
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.voiceManager.setCustomVoice(voice.name)
                                                selectedVoiceName = voice.name
                                                showVoicePicker = false
                                            },
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                            colors = if (isCurrent) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) else ButtonDefaults.outlinedButtonColors()
                                        ) {
                                            Text(
                                                text = "${voice.displayName}${if (isCurrent) "  ✓" else ""}",
                                                fontSize = 11.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showVoicePicker = false }) {
                                Text("Close")
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                }
            }

            // 4. Gestures & Hardware Sensors
            SectionCard(title = "GESTURES & SENSORS", icon = Icons.Default.Vibration) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Shake-to-Wake Assistant",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "Shake your phone firmly anytime to immediately trigger DEV AI voice modal.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    Switch(
                        checked = shakeToWakeEnabled,
                        onCheckedChange = { viewModel.setShakeToWake(it) },
                        modifier = Modifier.testTag("settings_shake_to_wake_switch")
                    )
                }
            }

            // 5. Custom Wake Word
            var wakeWordEnabled by remember { mutableStateOf(viewModel.preferences.wakeWordEnabled) }
            var wakePhrase by remember { mutableStateOf(viewModel.preferences.wakeWordPhrase) }
            var customWakePhraseInput by remember { mutableStateOf("") }

            SectionCard(title = "CUSTOM WAKE WORD", icon = Icons.Default.Hearing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hands-Free Wake Word",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = if (wakeWordEnabled) "Actively listening for \"$wakePhrase\"..." else "Speak wake word to trigger DEV AI hands-free.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (wakeWordEnabled) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    Switch(
                        checked = wakeWordEnabled,
                        onCheckedChange = {
                            wakeWordEnabled = it
                            viewModel.wakeWordManager.toggleWakeWordListening(it)
                        },
                        modifier = Modifier.testTag("settings_wake_word_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Quick Wake Phrases:",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presetPhrases = listOf("Hey Dev", "Dev AI", "Computer", "Jarvis")
                    presetPhrases.forEach { preset ->
                        val isSelected = wakePhrase.equals(preset, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                wakePhrase = preset
                                viewModel.wakeWordManager.setWakeWordPhrase(preset)
                            },
                            label = { Text(preset, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customWakePhraseInput,
                        onValueChange = { customWakePhraseInput = it },
                        placeholder = { Text("Or enter custom phrase...", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (customWakePhraseInput.isNotBlank()) {
                                wakePhrase = customWakePhraseInput.trim()
                                viewModel.wakeWordManager.setWakeWordPhrase(wakePhrase)
                                customWakePhraseInput = ""
                            }
                        },
                        enabled = customWakePhraseInput.isNotBlank(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Set", fontSize = 12.sp)
                    }
                }
            }

            // 6. App Lock & Biometric Protection
            var appLockEnabled by remember { mutableStateOf(viewModel.preferences.appLockEnabled) }
            var showAppLockDialog by remember { mutableStateOf(false) }
            val lockedApps = remember(appLockEnabled, showAppLockDialog) { viewModel.appLockManager.getLockedApps() }

            SectionCard(title = "APP LOCK & BIOMETRICS", icon = Icons.Default.Lock) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DEV AI App Lock Guard",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = if (appLockEnabled) "${lockedApps.size} apps secured with biometric lock" else "Lock sensitive apps with fingerprint / PIN",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (appLockEnabled) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    Switch(
                        checked = appLockEnabled,
                        onCheckedChange = {
                            appLockEnabled = it
                            viewModel.appLockManager.setAppLockFeatureEnabled(it)
                        },
                        modifier = Modifier.testTag("settings_app_lock_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showAppLockDialog = true },
                    modifier = Modifier.fillMaxWidth().testTag("manage_locked_apps_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage Locked Apps (${lockedApps.size} Locked)")
                }

                if (showAppLockDialog) {
                    val allInstalled = remember { viewModel.appLaunchManager.listInstalledApps() }
                    AlertDialog(
                        onDismissRequest = { showAppLockDialog = false },
                        title = { Text("Secure Apps with Biometrics", color = MaterialTheme.colorScheme.onSurface) },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = "Tap switch to lock or unlock an application with DEV AI Biometric Security:",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                allInstalled.forEach { app ->
                                    val isLocked = viewModel.appLockManager.isAppLocked(app.packageName)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(app.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Text(app.packageName, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(
                                            checked = isLocked,
                                            onCheckedChange = { lock ->
                                                if (lock) {
                                                    viewModel.appLockManager.lockApp(app.packageName)
                                                } else {
                                                    viewModel.appLockManager.unlockApp(app.packageName)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showAppLockDialog = false }) {
                                Text("Done", fontWeight = FontWeight.Bold)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                }
            }

            // 7. Display & Brightness Controls
            val displayManager = viewModel.displayAndBrightnessManager
            val displayStatus = remember { displayManager.getDisplayStatus() }
            var currentBrightness by remember { mutableFloatStateOf(displayStatus.brightnessPercent.toFloat()) }
            var isAutoBrightness by remember { mutableStateOf(displayStatus.isAutoBrightness) }
            val hasWriteSettings = displayManager.hasWriteSettingsPermission()

            SectionCard(title = "DISPLAY & BRIGHTNESS", icon = Icons.Default.BrightnessMedium) {
                Text(
                    text = "Screen Brightness: ${currentBrightness.toInt()}%",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
                )
                Slider(
                    value = currentBrightness,
                    onValueChange = {
                        currentBrightness = it
                        displayManager.setBrightness(it.toInt())
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Adaptive / Auto Brightness",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        )
                        Text(
                            text = if (isAutoBrightness) "Adjusts to ambient lighting automatically" else "Manual brightness mode",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    Switch(
                        checked = isAutoBrightness,
                        onCheckedChange = {
                            isAutoBrightness = it
                            displayManager.setAutoBrightness(it)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Screen Timeout:",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(15 to "15s", 30 to "30s", 60 to "1m", 120 to "2m", 300 to "5m").forEach { (sec, label) ->
                        FilterChip(
                            selected = displayStatus.screenTimeoutSeconds == sec,
                            onClick = { displayManager.setScreenTimeout(sec) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { displayManager.openDisplaySettings() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Display Settings", fontSize = 11.sp)
                    }
                    if (!hasWriteSettings) {
                        Button(
                            onClick = { displayManager.openWriteSettingsPermission() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                        ) {
                            Text("Grant Permission", fontSize = 11.sp)
                        }
                    }
                }
            }

            // 8. Advanced Permissions & Quick Settings Tile
            SectionCard(title = "SPECIAL ACCESS & QUICK TILE", icon = Icons.Default.Tune) {
                Text(
                    text = "Unlock full capabilities: notification reading, screen time auditing, and quick settings tile.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(10.dp))

                val hasNotif = viewModel.notificationReaderManager.isNotificationAccessGranted()
                val hasUsage = viewModel.appUsageAuditorManager.isUsagePermissionGranted()
                val hasDnd = viewModel.audioControlManager.isDndPermissionGranted()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Notification Listener Access", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface))
                        Text(if (hasNotif) "Active (Slack/WhatsApp reader enabled)" else "Disabled (Grant access to read messages)", style = MaterialTheme.typography.labelSmall.copy(color = if (hasNotif) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant))
                    }
                    OutlinedButton(
                        onClick = { viewModel.notificationReaderManager.openNotificationListenerSettings() },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(if (hasNotif) "Manage" else "Enable", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("App Usage & Screen Time Access", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface))
                        Text(if (hasUsage) "Active (Digital wellbeing auditing enabled)" else "Disabled (Grant access to audit screen time)", style = MaterialTheme.typography.labelSmall.copy(color = if (hasUsage) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant))
                    }
                    OutlinedButton(
                        onClick = { viewModel.appUsageAuditorManager.openUsageAccessSettings() },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(if (hasUsage) "Manage" else "Enable", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Do Not Disturb Policy Access", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface))
                        Text(if (hasDnd) "Active (Smart DND & Profiles enabled)" else "Disabled (Grant access to control DND)", style = MaterialTheme.typography.labelSmall.copy(color = if (hasDnd) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant))
                    }
                    OutlinedButton(
                        onClick = { viewModel.audioControlManager.openDndAccessSettings() },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(if (hasDnd) "Manage" else "Enable", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("⚡ Quick Settings Tile Ready", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                        Text("Swipe down your Android status bar, tap the Edit/Pencil icon, and drag the 'DEV AI Assistant' tile to your quick toggles for one-tap voice assistance anywhere!", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                    }
                }
            }

            // 6. Authorized Folders (SAF)
            SectionCard(title = "AUTHORIZED FOLDERS (SAF)", icon = Icons.Default.Folder) {
                Text(
                    text = "Under modern Android privacy guidelines, DEV AI only accesses files inside folders you explicitly designate.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(10.dp))

                val folders = viewModel.safFileManager.getAuthorizedFolders()
                if (folders.isEmpty()) {
                    Text(
                        text = "No authorized folders yet. Authorize a folder (e.g. Documents, Downloads) to enable file search, file moving, and creation.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    )
                } else {
                    folders.forEach { (uriStr, folderName) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = folderName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                            IconButton(
                                onClick = { viewModel.safFileManager.removeAuthorizedFolder(Uri.parse(uriStr)) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = StatusError,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onPickFolder,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_authorize_folder_button")
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Authorize New Folder")
                }
            }

            // 6. Data Management & Walkthrough
            SectionCard(title = "DATA & TUTORIAL", icon = Icons.Default.Tune) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val res = viewModel.exportChatHistoryToMarkdown()
                            exportStatusMessage = if (res.isSuccess) {
                                res.getOrThrow()
                            } else {
                                "Export Failed: ${res.exceptionOrNull()?.message}"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Chat to Markdown File")
                }

                if (exportStatusMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = exportStatusMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = StatusSuccess,
                            fontSize = 11.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showClearHistoryDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = StatusError, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Chat History", color = StatusError)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.resetTutorial() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Replay Full 10-Step Feature Tutorial", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            // 7. Battery Health & Degradation Tracker
            var batteryReport by remember { mutableStateOf(viewModel.batteryHealthManager.getBatteryHealthReport()) }
            SectionCard(title = "BATTERY HEALTH & CHARGE CYCLES", icon = Icons.Default.BatteryChargingFull) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${batteryReport.percentage}%",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = if (batteryReport.percentage <= 20) StatusError else CyanPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (batteryReport.isCharging) "⚡ ${batteryReport.chargingSource}" else "On Battery",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            Text(
                                text = "Estimated Health: ${batteryReport.estimatedHealthPercentage}% (${batteryReport.degradationRating})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        IconButton(
                            onClick = { batteryReport = viewModel.batteryHealthManager.getBatteryHealthReport() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Battery Telemetry", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Temperature:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "${String.format(Locale.US, "%.1f", batteryReport.temperatureC)}°C / ${String.format(Locale.US, "%.1f", batteryReport.temperatureF)}°F",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Thermal State:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    batteryReport.thermalStatus.take(24),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (batteryReport.temperatureC > 40f) StatusError else StatusSuccess,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Voltage & Chemistry:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${batteryReport.voltageMv} mV (${batteryReport.technology})", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Charge Cycles:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(batteryReport.cycleCount?.toString() ?: "~140 cycles (est.)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Charging Profile:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(batteryReport.chargingSpeed, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.sendMessage("Run a complete battery health, degradation, and temperature diagnostic")
                                onBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Run AI Diagnostic", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }

                        OutlinedButton(
                            onClick = { viewModel.batteryHealthManager.openBatterySettings() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Battery Settings", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // 8. Custom Routines ("If This Then That")
            var routines by remember { mutableStateOf(viewModel.customRoutineEngine.getRoutines()) }
            var showCreateRoutineDialog by remember { mutableStateOf(false) }

            SectionCard(title = "CUSTOM ROUTINES (IFTTT ENGINE)", icon = Icons.Default.AutoMode) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Automate device actions based on battery level, charger events, or motion triggers.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    routines.forEach { routine ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = routine.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "IF ${routine.triggerType} [${routine.triggerParam}] ➜ THEN ${routine.actionType}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.primary,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }

                                    Switch(
                                        checked = routine.isEnabled,
                                        onCheckedChange = { enabled ->
                                            viewModel.customRoutineEngine.toggleRoutine(routine.id, enabled)
                                            routines = viewModel.customRoutineEngine.getRoutines()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = CyanPrimary,
                                            checkedTrackColor = CyanPrimary.copy(alpha = 0.4f)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(
                                        onClick = {
                                            viewModel.customRoutineEngine.executeRoutine(routine, "Manual Test")
                                        }
                                    ) {
                                        Text("⚡ Test Fire", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }

                                    TextButton(
                                        onClick = {
                                            viewModel.customRoutineEngine.deleteRoutine(routine.id)
                                            routines = viewModel.customRoutineEngine.getRoutines()
                                        }
                                    ) {
                                        Text("Delete", fontSize = 11.sp, color = StatusError)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { showCreateRoutineDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create New Custom Routine", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 9. System-Wide Media Controller
            var mediaTrack by remember { mutableStateOf(viewModel.mediaControlManager.getCurrentMediaTrack()) }
            var mediaVolume by remember { mutableFloatStateOf(viewModel.mediaControlManager.getMediaVolumePercent().toFloat()) }

            SectionCard(title = "SYSTEM-WIDE MEDIA CONTROLLER", icon = Icons.Default.PlayArrow) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mediaTrack?.title ?: "No active media session",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            Text(
                                text = mediaTrack?.let { "${it.artist} • ${it.appName}" } ?: "Open Spotify, YouTube Music, or Podcasts",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                maxLines = 1
                            )
                        }

                        IconButton(
                            onClick = {
                                mediaTrack = viewModel.mediaControlManager.getCurrentMediaTrack()
                                mediaVolume = viewModel.mediaControlManager.getMediaVolumePercent().toFloat()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Media", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Media Control Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.mediaControlManager.mediaPrevious()
                                mediaTrack = viewModel.mediaControlManager.getCurrentMediaTrack()
                            }
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Track", modifier = Modifier.size(32.dp))
                        }

                        IconButton(
                            onClick = {
                                viewModel.mediaControlManager.mediaPlayPause()
                                mediaTrack = viewModel.mediaControlManager.getCurrentMediaTrack()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = if (mediaTrack?.isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play Pause",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                viewModel.mediaControlManager.mediaNext()
                                mediaTrack = viewModel.mediaControlManager.getCurrentMediaTrack()
                            }
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next Track", modifier = Modifier.size(32.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Media Volume Slider
                    Text(
                        text = "Media Volume: ${mediaVolume.toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Slider(
                        value = mediaVolume,
                        onValueChange = {
                            mediaVolume = it
                            viewModel.mediaControlManager.setMediaVolume(it.toInt())
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanPrimary,
                            activeTrackColor = CyanPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = { viewModel.mediaControlManager.openNotificationListenerSettings() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Media & Notification Access", fontSize = 11.sp)
                    }
                }
            }

            if (showCreateRoutineDialog) {
                var routineName by remember { mutableStateOf("Low Battery Voice Alert") }
                var selectedTrigger by remember { mutableStateOf("battery_below") }
                var triggerParam by remember { mutableStateOf("20") }
                var selectedAction by remember { mutableStateOf("speak_text") }
                var actionParam by remember { mutableStateOf("Warning! Battery is low.") }

                AlertDialog(
                    onDismissRequest = { showCreateRoutineDialog = false },
                    title = { Text("Create IFTTT Routine", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            OutlinedTextField(
                                value = routineName,
                                onValueChange = { routineName = it },
                                label = { Text("Routine Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("WHEN (Trigger):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CyanPrimary)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = selectedTrigger == "battery_below",
                                    onClick = { selectedTrigger = "battery_below"; triggerParam = "20" },
                                    label = { Text("Battery < %", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = selectedTrigger == "power_connected",
                                    onClick = { selectedTrigger = "power_connected"; triggerParam = "" },
                                    label = { Text("Plugged In", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = selectedTrigger == "shake_device",
                                    onClick = { selectedTrigger = "shake_device"; triggerParam = "" },
                                    label = { Text("Shake", fontSize = 10.sp) }
                                )
                            }
                            if (selectedTrigger.contains("battery")) {
                                OutlinedTextField(
                                    value = triggerParam,
                                    onValueChange = { triggerParam = it },
                                    label = { Text("Battery Threshold (%)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("THEN (Action):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CyanPrimary)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = selectedAction == "speak_text",
                                    onClick = { selectedAction = "speak_text"; actionParam = "Battery low, plug in now!" },
                                    label = { Text("Speak", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = selectedAction == "toggle_flashlight",
                                    onClick = { selectedAction = "toggle_flashlight"; actionParam = "toggle" },
                                    label = { Text("Flashlight", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = selectedAction == "set_brightness",
                                    onClick = { selectedAction = "set_brightness"; actionParam = "10" },
                                    label = { Text("Dim Screen", fontSize = 10.sp) }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = actionParam,
                                onValueChange = { actionParam = it },
                                label = { Text("Action Parameter / Speech") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.customRoutineEngine.createRoutine(
                                    routineName,
                                    selectedTrigger,
                                    triggerParam,
                                    selectedAction,
                                    actionParam
                                )
                                routines = viewModel.customRoutineEngine.getRoutines()
                                showCreateRoutineDialog = false
                            }
                        ) {
                            Text("Create")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateRoutineDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // 10. Official About Section
            SectionCard(title = "ABOUT DEV AI", icon = Icons.Default.Info) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DEV AI",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"Your Personal AI. Your Phone. Your Control.\"",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Developed by DEV L KAKKOTH",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = PurpleAccent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Autonomous personal Android assistant powered by OpenRouter and NVIDIA Nemotron 3 Ultra.\nBuilt with Kotlin, Jetpack Compose, Material 3, and native Android platform bridges.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Chat History?", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("This will permanently remove stored conversation messages from your local database.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearChatHistory()
                        showClearHistoryDialog = false
                    }
                ) {
                    Text("Clear All", color = StatusError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
