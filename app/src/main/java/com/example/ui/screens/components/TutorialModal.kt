package com.example.ui.screens.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class TutorialStep(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val description: String,
    val badge: String,
    val bulletPoints: List<String>
)

@Composable
fun TutorialModal(
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    val steps = remember {
        listOf(
            TutorialStep(
                title = "Welcome to Dart AI",
                subtitle = "Your Personal AI. Your Phone. Your Control.",
                icon = Icons.Default.AutoAwesome,
                badge = "DEVELOPED BY DEV L KAKKOTH",
                description = "Dart AI is an autonomous personal Android AI assistant developed by DEV L KAKKOTH. Powered by OpenRouter NVIDIA Nemotron 3 Ultra, it seamlessly bridges deep AI reasoning with direct Android system control.",
                bulletPoints = listOf(
                    "State-of-the-art Nemotron 3 Ultra reasoning engine",
                    "Native Android OS and hardware integrations",
                    "Real actions on your phone, not a simulated mock"
                )
            ),
            TutorialStep(
                title = "Battery Health & Degradation",
                subtitle = "Real-time battery longevity & charge telemetry",
                icon = Icons.Default.BatteryChargingFull,
                badge = "BATTERY HEALTH AUDITOR",
                description = "Track battery degradation score, charge cycle counts, live voltage in mV, instantaneous charging/discharging current in mA, temperature, and intelligent charging threshold recommendations.",
                bulletPoints = listOf(
                    "\"How is my battery health and cycle count?\"",
                    "Degradation rating and remaining capacity health %",
                    "Overheat alerts and 20%-80% longevity rule guidance"
                )
            ),
            TutorialStep(
                title = "Custom \"IFTTT\" Routines",
                subtitle = "Automate device actions with natural language",
                icon = Icons.Default.AltRoute,
                badge = "NATURAL AUTOMATION ENGINE",
                description = "Teach Dart AI custom triggers and actions! Build rules like 'When battery drops below 20%, turn off flashlight and announce low power' or 'When plugged in, set media volume to 80%'.",
                bulletPoints = listOf(
                    "Triggers: battery levels, power plugged/unplugged, shake gestures",
                    "Scheduled delayed automations (\"Turn off flashlight in 10 minutes\")",
                    "Background broadcast monitoring with active toggle control"
                )
            ),
            TutorialStep(
                title = "System Media Controller",
                subtitle = "Full playback control across all music & video apps",
                icon = Icons.Default.PlayCircle,
                badge = "GLOBAL MEDIA PLAYBACK",
                description = "Control Spotify, YouTube Music, Apple Music, and podcast apps hands-free! Dart AI can play, pause, skip, go to previous track, and inspect now-playing song title and artist.",
                bulletPoints = listOf(
                    "\"Pause music\", \"Play next song\", \"Resume playback\"",
                    "\"What song is currently playing?\"",
                    "Global MediaSessionManager integration with fallback controls"
                )
            ),
            TutorialStep(
                title = "Biometric Lock & Wake Word",
                subtitle = "App privacy shield and hands-free activation",
                icon = Icons.Default.Fingerprint,
                badge = "SECURITY & WAKE WORDS",
                description = "Lock sensitive apps with biometric fingerprint or device PIN so nobody can open them without authorization. Plus, set custom wake words like 'Hey Dart' for continuous hands-free listening.",
                bulletPoints = listOf(
                    "\"Lock WhatsApp and Gallery with App Lock\"",
                    "Set custom wake words (\"Hey Dart\", \"Dart AI\", \"Assistant\")",
                    "BiometricPrompt + Keyguard PIN fallback protection"
                )
            ),
            TutorialStep(
                title = "Digital Wellbeing & Smart DND",
                subtitle = "App screen time audits and audio profile toggles",
                icon = Icons.Default.NotificationsActive,
                badge = "FOCUS & WELLBEING",
                description = "Audit your daily screen time across apps, activate Smart Do Not Disturb during focus hours, toggle between Silent/Vibrate/Normal sound modes, and inspect incoming notifications (e.g., Slack).",
                bulletPoints = listOf(
                    "\"Audit my screen time today\"",
                    "\"Set sound profile to silent / vibrate\"",
                    "\"Check my recent Slack notifications\""
                )
            ),
            TutorialStep(
                title = "Alarm & Calendar Reminders",
                subtitle = "Turn on existing alarms, change times & add events",
                icon = Icons.Default.Alarm,
                badge = "PRODUCTIVITY SCHEDULER",
                description = "Dart AI doesn't just create new alarms—it can turn on existing alarms, adjust times, inspect upcoming schedules, and directly insert calendar events without opening the calendar app.",
                bulletPoints = listOf(
                    "\"Turn on my 7:00 AM alarm\"",
                    "\"Change my alarm to 7:30 AM\"",
                    "\"Schedule calendar meeting tomorrow at 3 PM\""
                )
            ),
            TutorialStep(
                title = "File Management & Name Dialing",
                subtitle = "Move files, create notes, and call contacts by name",
                icon = Icons.Default.Description,
                badge = "SMART FILE & CONTACT TOOLS",
                description = "Organize folders by moving files, create files inside subdirectories, and dial contacts by speaking their name (e.g. 'Call John') rather than remembering phone numbers.",
                bulletPoints = listOf(
                    "\"Move invoice.pdf from Downloads to Accounting\"",
                    "\"Create meeting_notes.md in Projects folder\"",
                    "\"Call Mom\" or \"Call Sarah on phone\""
                )
            ),
            TutorialStep(
                title = "Voice Assistant & Settings Hub",
                subtitle = "Speak naturally with instant audio playback",
                icon = Icons.Default.Mic,
                badge = "SPEECH & 1-TAP SHORTCUTS",
                description = "Tap the glowing microphone or shake your phone to speak. Choose from custom voices (Male, Female, Fast, Deep), adjust speech pitch/rate, and launch over 20 Android settings instantly.",
                bulletPoints = listOf(
                    "Hands-free voice loop with auto-speak responses",
                    "Custom voice selection and speed slider in Settings",
                    "1-tap System Settings Hub for Wi-Fi, Bluetooth, Power Saver"
                )
            ),
            TutorialStep(
                title = "Themes, Dark/Light Mode & Gestures",
                subtitle = "Customizable cyberpunk and daylight aesthetics",
                icon = Icons.Default.Vibration,
                badge = "CUSTOMIZE YOUR EXPERIENCE",
                description = "Toggle between Dark Mode and Light Mode, pick from 4 cyber color themes (Cyan, Emerald, Magenta, Gold), and shake your phone to wake Dart AI instantly from anywhere!",
                bulletPoints = listOf(
                    "Dark / Light theme toggle on the top bar and settings",
                    "4 dynamic cyberpunk color accent styles",
                    "Hardware accelerometer shake-to-wake gesture"
                )
            )
        )
    }

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val currentStep = steps[currentStepIndex]
    val isLastStep = currentStepIndex == steps.size - 1

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                )
                .testTag("tutorial_modal_dialog"),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top header: Step indicator & Skip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STEP ${currentStepIndex + 1} OF ${steps.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    TextButton(
                        onClick = onComplete,
                        modifier = Modifier.testTag("tutorial_skip_button")
                    ) {
                        Text(
                            text = "Skip Tour",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Animated step content with scroll for smaller displays
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        slideInHorizontally { width -> width / 3 } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width / 3 } + fadeOut()
                    },
                    label = "tutorial_slide"
                ) { step ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Icon Circle
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = step.badge,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Title & Subtitle
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = step.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Description
                        Text(
                            text = step.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bullet Points
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            step.bulletPoints.forEach { point ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = point,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress indicator bar
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.indices.forEach { index ->
                        val isSelected = index == currentStepIndex
                        Box(
                            modifier = Modifier
                                .height(5.dp)
                                .width(if (isSelected) 18.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Buttons: Back / Next or Finish
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (currentStepIndex > 0) {
                        OutlinedButton(
                            onClick = { currentStepIndex-- },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("tutorial_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back", fontSize = 13.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (isLastStep) {
                                onComplete()
                            } else {
                                currentStepIndex++
                            }
                        },
                        modifier = Modifier
                            .weight(if (currentStepIndex > 0) 1.5f else 1f)
                            .height(46.dp)
                            .testTag(if (isLastStep) "tutorial_finish_button" else "tutorial_next_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = if (isLastStep) "Get Started" else "Next",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (isLastStep) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
