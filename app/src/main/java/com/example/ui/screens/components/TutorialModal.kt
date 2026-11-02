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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
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
                title = "Voice & Hands-Free Mode",
                subtitle = "Speak naturally with instant audio playback",
                icon = Icons.Default.Mic,
                badge = "SPEECH & TTS",
                description = "Tap the glowing microphone or shake your phone to speak. Dart AI transcribes your voice in real time, executes device tools, and speaks answers back with customizable pitch and speech rate.",
                bulletPoints = listOf(
                    "Hands-free voice loop with auto-speak responses",
                    "Quick-switch button between Voice and Keyboard typing",
                    "Adjustable speech rate and pitch in Settings"
                )
            ),
            TutorialStep(
                title = "System Settings Hub",
                subtitle = "Instant 1-tap shortcuts to every phone setting",
                icon = Icons.Default.Settings,
                badge = "SYSTEM CONTROL",
                description = "Never dig through convoluted menus again! Dart AI gives you a unified Settings Hub to launch over 20 Android settings in a fraction of a second.",
                bulletPoints = listOf(
                    "Wi-Fi, Bluetooth, Display, and Sound settings",
                    "Battery Saver, Storage, and App permissions",
                    "Developer Options, Biometrics, and Privacy controls"
                )
            ),
            TutorialStep(
                title = "Hardware & Telemetry",
                subtitle = "Flashlight, Volume, Battery & Storage",
                icon = Icons.Default.PhoneAndroid,
                badge = "DEVICE TELEMETRY",
                description = "Directly operate camera flashlight, adjust volume across media/alarm streams, inspect live battery temperature and percentage, and query internal storage and RAM usage.",
                bulletPoints = listOf(
                    "\"Turn on flashlight\" / \"Set media volume to 80%\"",
                    "\"Check battery temperature and health\"",
                    "\"How much free storage and RAM is left?\""
                )
            ),
            TutorialStep(
                title = "Vision & SAF File Intelligence",
                subtitle = "Search, read, and create files on your phone",
                icon = Icons.Default.Description,
                badge = "STORAGE ACCESS FRAMEWORK",
                description = "Authorize folders using Android's privacy-first Storage Access Framework. Dart AI can search inside documents, create text notes and source code files, and analyze images from Camera or Gallery.",
                bulletPoints = listOf(
                    "\"Create a file named todo.txt with project tasks\"",
                    "\"Search inside documents for invoice keywords\"",
                    "Pick photos and camera captures for visual inspection"
                )
            ),
            TutorialStep(
                title = "Offline Emergency Survival Mode",
                subtitle = "Core phone actions work without internet",
                icon = Icons.Default.AutoAwesome,
                badge = "EMERGENCY READY",
                description = "Stranded without internet or mobile data? Dart AI automatically detects local device intents and operates your flashlight, volume, battery telemetry, alarms, timers, and math calculations completely offline!",
                bulletPoints = listOf(
                    "Instant local hardware execution without API key or network",
                    "Emergency flashlight toggle and battery status checks",
                    "Offline countdown timers and alarms"
                )
            ),
            TutorialStep(
                title = "Live Web Search & Article Reader",
                subtitle = "Live summaries from the web",
                icon = Icons.Default.Language,
                badge = "LIVE WEB KNOWLEDGE",
                description = "Ask Dart AI to look up live facts, articles, and topic summaries. It queries real-time web services and synthesizes key takeaways directly in your conversation.",
                bulletPoints = listOf(
                    "\"Search the web for latest quantum computing breakthroughs\"",
                    "\"Who won the latest chess championship?\"",
                    "Clean summarized web answers with sources"
                )
            ),
            TutorialStep(
                title = "Math Sandbox & Formula Rendering",
                subtitle = "High-precision calculations and LaTeX formatting",
                icon = Icons.Default.Calculate,
                badge = "CALCULATION ENGINE",
                description = "Solve arithmetic, powers, square roots, and trigonometric equations in real time with beautiful mathematical LaTeX formatting and syntax-highlighted code blocks with 1-tap copy.",
                bulletPoints = listOf(
                    "Evaluate complex formulas like sqrt(144) + 2^5",
                    "LaTeX formula rendering for physics and calculus",
                    "Syntax-highlighted code blocks with copy-to-clipboard"
                )
            ),
            TutorialStep(
                title = "Security Vault & Contacts Dialer",
                subtitle = "Private encrypted notes and fast phone dialing",
                icon = Icons.Default.Lock,
                badge = "SECURITY & COMMUNICATIONS",
                description = "Store sensitive notes, thoughts, and codes in the private Dart AI Vault. Plus, ask Dart AI to dial phone numbers or launch device contacts in one tap.",
                bulletPoints = listOf(
                    "\"Save private note: Server backup keys in vault\"",
                    "\"Dial 555-0199\" or \"Open contacts\"",
                    "Daily executive briefing with battery, storage, and quotes"
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
